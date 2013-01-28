package pro.schmid.android.whereareyou;

import java.util.ArrayList;
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
import pro.schmid.android.whereareyou.utils.Utils;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class FirebaseMapManager {
	private final Activity mActivity;
	private final SharedPreferences mapsPrefs;

	private final GoogleMap mMap;

	private final Firebase mFirebase;
	private final Firebase mMyself;
	private final Firebase mPosition;
	private final String mChildName;

	private final Map<String, Marker> mMarkers = new ConcurrentHashMap<String, Marker>();
	private final Map<String, Polygon> mPolygon = new ConcurrentHashMap<String, Polygon>();
	private final Map<String, String> mNames = new ConcurrentHashMap<String, String>();
	private final Map<String, Integer> mId = new ConcurrentHashMap<String, Integer>();

	public FirebaseMapManager(Activity a, GoogleMap mMap, Firebase parent, String username) {
		this.mActivity = a;
		this.mapsPrefs = this.mActivity.getSharedPreferences(Constants.MAPS_PREFS, Context.MODE_PRIVATE);

		this.mMap = mMap;

		this.mFirebase = parent;

		// Get current handle
		String parentName = parent.name();
		String myself = mapsPrefs.getString(parentName, null);
		if (myself != null) {
			this.mMyself = mFirebase.child(myself);
			this.mChildName = myself;
		} else {
			this.mMyself = mFirebase.push();
			this.mChildName = mMyself.name();
			mapsPrefs.edit().putString(parentName, this.mChildName).commit();

			this.mMyself.child(Constants.NAME).set(new JsonPrimitive(username));
		}

		this.mPosition = mMyself.child(Constants.POSITION);

		mFirebase.on(EventType.child_added, personAdded);
		mFirebase.on(EventType.child_removed, personRemoved);
	}

	void setMyLocation(Location location) {
		JsonObject loc = new JsonObject();
		loc.addProperty(Constants.LAT, location.getLatitude());
		loc.addProperty(Constants.LONG, location.getLongitude());
		loc.addProperty(Constants.ACCURACY, location.getAccuracy());
		loc.addProperty(Constants.DATETIME, System.currentTimeMillis());
		mPosition.set(loc);
	}

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
			mId.put(snapshotName, ColorUtils.getCurrentColor());
			ColorUtils.incrementColor();

			Firebase positionRef = snapshot.ref().child(Constants.POSITION);
			positionRef.on(EventType.value, positionCallback);
		}
	};

	private final DataEvent personRemoved = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			String name = snapshot.name();
			final Marker removed = mMarkers.remove(name);
			final Polygon polygon = mPolygon.get(name);

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

			JsonObject el = val.getAsJsonObject();
			double lat = el.get(Constants.LAT).getAsDouble();
			double lng = el.get(Constants.LONG).getAsDouble();
			double accuracy = el.get(Constants.ACCURACY).getAsDouble();
			long datetime = el.get(Constants.DATETIME).getAsLong();

			final String date = getStringFromTimestamp(datetime);
			final LatLng ll = new LatLng(lat, lng);

			int colorId = mId.get(parentName);

			// Put or move the marker
			final Marker marker = mMarkers.get(parentName);
			if (marker != null) {

				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						marker.setPosition(ll);
						marker.setSnippet(date);
					}
				});
			} else {

				final String personName = mNames.get(parentName);
				final MarkerOptions markerOptions = new MarkerOptions().position(ll)
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

			// Put or move the accuracy circle
			double radius = accuracy > Constants.MAX_ACCURACY ? Constants.MAX_ACCURACY : accuracy;
			ArrayList<LatLng> accuracyPoints = Utils.getCirclePoints(ll, radius);
			final Polygon polygon = mPolygon.get(parentName);

			final PolygonOptions polygonOptions = new PolygonOptions()
					.addAll(accuracyPoints)
					.strokeColor(ColorUtils.ACCURACY_STROKE_COLORS[colorId])
					.strokeWidth(4)
					.fillColor(ColorUtils.ACCURACY_FILL_COLORS[colorId])
					.geodesic(true);

			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {

					// Remove old polygon
					if (polygon != null) {
						polygon.remove();
					}

					Polygon polygon = mMap.addPolygon(polygonOptions);
					mPolygon.put(parentName, polygon);
				}
			});
		}
	};

	private String getStringFromTimestamp(long timestamp) {
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		String date = Constants.DATE_FORMATTER.format(calendar.getTime());
		return date;
	}
}
