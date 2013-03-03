package pro.schmid.android.whereareyou;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.schmid.android.androidonfire.DataSnapshot;
import pro.schmid.android.androidonfire.Firebase;
import pro.schmid.android.androidonfire.callbacks.DataEvent;
import pro.schmid.android.androidonfire.callbacks.EventType;
import pro.schmid.android.whereareyou.utils.ColorUtils;
import pro.schmid.android.whereareyou.utils.Constants;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Manage the map objects coming and going through Firebase.
 * 
 * Each person is represented on the map using a marker and a circle showing the accuracy of his location.
 */
class FirebaseMapManager {
	private final Activity mActivity;
	private final SharedPreferences mapsPrefs;

	private final GoogleMap mMap;

	// Firebase references
	private final Firebase mFirebase;
	private final Firebase mMyself;
	private final Firebase mPosition;
	private final String mChildName;

	// Remember objects on the map
	private final Map<String, Marker> mMarkers = new ConcurrentHashMap<String, Marker>();
	private final Map<String, Circle> mCircles = new ConcurrentHashMap<String, Circle>();
	private final Map<String, String> mNames = new ConcurrentHashMap<String, String>();
	private final Map<String, Integer> mUsernameToColor = new ConcurrentHashMap<String, Integer>();

	/**
	 * Create the manager.
	 * Try to find if we already have a reference to this specific room.
	 * If we have one, keep the same handle to move the same map marker instead of creating a new one.
	 * Otherwise create a new one.
	 * Register listeners for the new map objects.
	 * 
	 * @param activity
	 *            The activity using the manager
	 * @param map
	 *            The Google Map used in thew Activity
	 * @param room
	 *            Reference to the room
	 * @param username
	 *            Current user's username
	 */
	public FirebaseMapManager(Activity activity, GoogleMap map, Firebase room, String username) {
		this.mActivity = activity;
		this.mFirebase = room;
		this.mMap = map;

		// The preferences contain all handles for every room used in the map
		this.mapsPrefs = this.mActivity.getSharedPreferences(Constants.MAPS_PREFS, Context.MODE_PRIVATE);

		ColorUtils.resetColor();

		// Get current handle
		String parentName = room.name();
		String myself = mapsPrefs.getString(parentName, null);
		if (myself != null) {
			this.mMyself = mFirebase.child(myself);
			this.mChildName = myself;
		} else {
			this.mMyself = mFirebase.push();
			this.mChildName = mMyself.name();
			mapsPrefs.edit().putString(parentName, this.mChildName).commit();
		}

		// Always set the name, in case the user changed it
		this.mMyself.child(Constants.NAME).set(new JsonPrimitive(username));
		this.mPosition = mMyself.child(Constants.POSITION);

		mFirebase.on(EventType.child_added, personAdded);
		mFirebase.on(EventType.child_removed, personRemoved);
	}

	/**
	 * Set the user location in Firebase
	 * 
	 * @param location
	 */
	void setMyLocation(Location location) {
		JsonObject loc = new JsonObject();
		loc.addProperty(Constants.LAT, location.getLatitude());
		loc.addProperty(Constants.LONG, location.getLongitude());
		loc.addProperty(Constants.ACCURACY, location.getAccuracy());
		loc.addProperty(Constants.DATETIME, System.currentTimeMillis());
		mPosition.set(loc);
	}

	/**
	 * Called when a new person arrives in the room.
	 * Registers a listener for his position.
	 */
	private final DataEvent personAdded = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			final String snapshotName = snapshot.name();

			// Do not capture myself
			if (mChildName.equalsIgnoreCase(snapshotName)) {
				return;
			}

			String personName = snapshot.val().getAsJsonObject().get(Constants.NAME).getAsString();
			mNames.put(snapshotName, personName);
			mUsernameToColor.put(snapshotName, ColorUtils.getCurrentColor());
			ColorUtils.incrementColor();

			// Register a listener for his position.
			Firebase positionRef = snapshot.ref().child(Constants.POSITION);
			positionRef.on(EventType.value, positionCallback);
		}
	};

	/**
	 * Called when a person is removed.
	 * Remove listeners for the person.
	 */
	private final DataEvent personRemoved = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			String name = snapshot.name();
			final Marker removed = mMarkers.remove(name);
			final Circle polygon = mCircles.get(name);

			// Remove his objects on the map
			if (removed != null || polygon != null) {
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (removed != null) {
							removed.remove();
						}

						if (polygon != null) {
							polygon.remove();
						}
					}
				});
			}

			Firebase positionRef = snapshot.ref().child(Constants.POSITION);
			positionRef.off();
		}
	};

	/**
	 * Called when a position changed.
	 * Find the related user, move his objects on the map.
	 */
	private final DataEvent positionCallback = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			JsonElement val = snapshot.val();

			if (val == null || val == JsonNull.INSTANCE) {
				return;
			}

			// Get data
			Firebase ref = snapshot.ref();
			Firebase parent = ref.parent();
			final String parentName = parent.name();

			// Get all info from the position
			JsonObject el = val.getAsJsonObject();
			double lat = el.get(Constants.LAT).getAsDouble();
			double lng = el.get(Constants.LONG).getAsDouble();
			double accuracy = el.get(Constants.ACCURACY).getAsDouble();
			long datetime = el.get(Constants.DATETIME).getAsLong();

			final String date = getStringFromTimestamp(datetime);
			final LatLng latLong = new LatLng(lat, lng);

			int colorId = mUsernameToColor.get(parentName);

			// Put or move the marker
			final Marker marker = mMarkers.get(parentName);
			if (marker != null) {

				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						marker.setPosition(latLong);
						marker.setSnippet(date);
					}
				});
			} else {

				final String personName = mNames.get(parentName);
				final MarkerOptions markerOptions = new MarkerOptions().position(latLong)
						.title(personName)
						.snippet(date)
						.icon(BitmapDescriptorFactory.defaultMarker(ColorUtils.MARKER_COLORS[colorId]));

				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Marker marker = mMap.addMarker(markerOptions);
						mMarkers.put(parentName, marker);
					}
				});
			}

			// Find the old circle if available
			final double radius = accuracy > Constants.MAX_ACCURACY ? Constants.MAX_ACCURACY : accuracy;
			final Circle oldCircle = mCircles.get(parentName);

			if (oldCircle != null) {

				// Move the old circle
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						oldCircle.setCenter(latLong);
						oldCircle.setRadius(radius);
					}
				});
			} else {

				// Create new circle
				final CircleOptions accuracyCircle = new CircleOptions()
						.center(latLong)
						.radius(radius)
						.strokeColor(ColorUtils.ACCURACY_STROKE_COLORS[colorId])
						.strokeWidth(4)
						.fillColor(ColorUtils.ACCURACY_FILL_COLORS[colorId]);

				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Circle newCircle = mMap.addCircle(accuracyCircle);
						mCircles.put(parentName, newCircle);
					}
				});
			}
		}
	};

	/**
	 * Get a string to show on the marker balloon from the date when the position was set.
	 * 
	 * @param timestamp Timestamp in milliseconds
	 * @return
	 */
	private String getStringFromTimestamp(long timestamp) {
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		String date = Constants.DATE_FORMATTER.format(calendar.getTime());
		return date;
	}
}
