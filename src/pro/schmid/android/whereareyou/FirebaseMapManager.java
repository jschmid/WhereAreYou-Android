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
import com.google.gson.JsonObject;

class FirebaseMapManager {
	private final Activity mActivity;
	private final GoogleMap mMap;
	private final Firebase mFirebase;
	private final Firebase mMyself;
	private final String mChildName;
	private final String mUsername;

	private final Map<String, Marker> mMarkers = new ConcurrentHashMap<String, Marker>();

	public FirebaseMapManager(Activity a, GoogleMap mMap, Firebase parent, String username) {
		this.mActivity = a;
		this.mMap = mMap;
		this.mFirebase = parent;
		this.mMyself = mFirebase.push();
		this.mUsername = username;
		this.mChildName = mMyself.name();

		// Firebase presence = mMyself.child("presence");
		// presence.setOnDisconnect(new JsonPrimitive(false));
		// presence.set(new JsonPrimitive(true));
		this.mMyself.removeOnDisconnect();

		mFirebase.on(EventType.child_added, childAdded);
		mFirebase.on(EventType.child_changed, childChanged);
		mFirebase.on(EventType.child_removed, childRemoved);
	}

	void setMyLocation(Location location) {
		JsonObject loc = new JsonObject();
		loc.addProperty(Constants.LAT, location.getLatitude());
		loc.addProperty(Constants.LONG, location.getLongitude());
		loc.addProperty(Constants.NAME, mUsername);
		mMyself.setWithPriority(loc, System.currentTimeMillis() + "");

	}

	private final DataEvent childAdded = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			final String snapshotName = snapshot.name();

			// Do not draw myself
			if (mChildName.equalsIgnoreCase(snapshotName)) {
				return;
			}

			JsonObject el = snapshot.val().getAsJsonObject();
			double lat = el.get(Constants.LAT).getAsDouble();
			double lng = el.get(Constants.LONG).getAsDouble();
			String name = el.get(Constants.NAME).getAsString();
			String priority = snapshot.getPriority();

			String date = getStringFromTimestamp(priority);
			LatLng ll = new LatLng(lat, lng);

			final MarkerOptions markerOptions = new MarkerOptions().position(ll).title(name).snippet(date);

			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Marker marker = mMap.addMarker(markerOptions);
					mMarkers.put(snapshotName, marker);
				}
			});
		}
	};

	private final DataEvent childChanged = new DataEvent() {
		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			String name = snapshot.name();
			JsonObject el = snapshot.val().getAsJsonObject();
			double lat = el.get(Constants.LAT).getAsDouble();
			double lng = el.get(Constants.LONG).getAsDouble();
			String priority = snapshot.getPriority();

			final String date = getStringFromTimestamp(priority);
			final LatLng ll = new LatLng(lat, lng);

			final Marker marker = mMarkers.get(name);

			if (marker != null) {
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						marker.setPosition(ll);
						marker.setSnippet(date);
					}
				});
			}
		}
	};

	private final DataEvent childRemoved = new DataEvent() {
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
		}
	};

	private String getStringFromTimestamp(String timestamp) {
		long seconds = Long.parseLong(timestamp);
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(seconds);
		String date = Constants.DATE_FORMATTER.format(calendar.getTime());
		return date;
	}
}
