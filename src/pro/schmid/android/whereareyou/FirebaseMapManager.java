package pro.schmid.android.whereareyou;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.schmid.android.androidonfire.DataSnapshot;
import pro.schmid.android.androidonfire.Firebase;
import pro.schmid.android.androidonfire.callbacks.DataEvent;
import pro.schmid.android.androidonfire.callbacks.EventType;
import android.app.Activity;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class FirebaseMapManager {
	private final Activity mActivity;
	private final GoogleMap mMap;
	private final Firebase mFirebase;
	private final Firebase mMyself;
	private final Firebase mPosition;
	private final String mChildName;

	private final Map<String, Marker> mMarkers = new ConcurrentHashMap<String, Marker>();
	private final Map<String, String> mNames = new ConcurrentHashMap<String, String>();

	public FirebaseMapManager(Activity a, GoogleMap mMap, Firebase parent, String username) {
		this.mActivity = a;
		this.mMap = mMap;
		this.mFirebase = parent;
		this.mMyself = mFirebase.push();
		this.mPosition = mMyself.child(Constants.POSITION);
		this.mChildName = mMyself.name();

		// Firebase presence = mMyself.child("presence");
		// presence.setOnDisconnect(new JsonPrimitive(false));
		// presence.set(new JsonPrimitive(true));
		// this.mMyself.removeOnDisconnect();

		mFirebase.on(EventType.child_added, personAdded);
		mFirebase.on(EventType.child_removed, personRemoved);

		this.mMyself.child(Constants.NAME).set(new JsonPrimitive(username));
	}

	void setMyLocation(Location location) {
		JsonObject loc = new JsonObject();
		loc.addProperty(Constants.LAT, location.getLatitude());
		loc.addProperty(Constants.LONG, location.getLongitude());
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

			Firebase positionRef = snapshot.ref().child(Constants.POSITION);
			positionRef.on(EventType.value, positionCallback);
		}
	};

	private final DataEvent personRemoved = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			String name = snapshot.name();
			final Marker removed = mMarkers.remove(name);

			if (removed != null) {
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						removed.remove();
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

			Firebase ref = snapshot.ref();
			Firebase parent = ref.parent();
			final String parentName = parent.name();

			final Marker marker = mMarkers.get(parentName);

			JsonObject el = val.getAsJsonObject();
			double lat = el.get(Constants.LAT).getAsDouble();
			double lng = el.get(Constants.LONG).getAsDouble();
			long datetime = el.get(Constants.DATETIME).getAsLong();

			final String date = getStringFromTimestamp(datetime);
			final LatLng ll = new LatLng(lat, lng);

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
				final MarkerOptions markerOptions = new MarkerOptions().position(ll).title(personName).snippet(date);

				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Marker marker = mMap.addMarker(markerOptions);
						mMarkers.put(parentName, marker);
					}
				});
			}
		}
	};

	private String getStringFromTimestamp(long timestamp) {
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		String date = Constants.DATE_FORMATTER.format(calendar.getTime());
		return date;
	}
}
