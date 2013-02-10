package pro.schmid.android.whereareyou.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

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

	public static String getAccountUsername(Context context) {
		AccountManager manager = AccountManager.get(context);
		Account[] accounts = manager.getAccountsByType("com.google");
		List<String> possibleEmails = new LinkedList<String>();

		for (Account account : accounts) {
			possibleEmails.add(account.name);
		}

		if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
			String email = possibleEmails.get(0);
			String[] parts = email.split("@");
			if (parts.length > 0 && parts[0] != null) {
				return parts[0];
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}
