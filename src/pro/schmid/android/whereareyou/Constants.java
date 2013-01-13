package pro.schmid.android.whereareyou;

import java.text.DateFormat;

public class Constants {

	// Firebase structure
	public static final String NAME = "name";
	public static final String POSITION = "position";
	public static final String LAT = "lat";
	public static final String LONG = "long";
	public static final String DATETIME = "datetime";

	// General preferences
	public static final String PREF_NAME = "name";

	// Maps preferences
	public static final String MAPS_PREFS = "maps";

	public static final DateFormat DATE_FORMATTER = DateFormat.getDateTimeInstance();

	public static final String BASE_URL = "http://w.schmid.pro/";
	public static final String FIREBASE_URL = "https://whereareyou.firebaseio.com";

}
