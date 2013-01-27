package pro.schmid.android.whereareyou.utils;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

public class Utils {

	private static final double EARTH_RADIUS = 6378100.0;

	public static final ArrayList<LatLng> getCirclePoints(LatLng center, double radius) {
		ArrayList<LatLng> points = new ArrayList<LatLng>();
		// Convert to radians.
		double lat = center.latitude * Math.PI / 180.0;
		double lon = center.longitude * Math.PI / 180.0;

		for (double t = 0; t <= Math.PI * 2; t += 0.01) {
			// y
			double latPoint = lat + radius / EARTH_RADIUS * Math.sin(t);
			// x
			double lonPoint = lon + radius / EARTH_RADIUS * Math.cos(t) / Math.cos(lat);
			points.add(new LatLng(latPoint * 180.0 / Math.PI, lonPoint * 180.0 / Math.PI));
		}

		return points;
	}
}
