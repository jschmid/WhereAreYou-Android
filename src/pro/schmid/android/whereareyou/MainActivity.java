package pro.schmid.android.whereareyou;

import java.util.List;

import pro.schmid.android.androidonfire.Firebase;
import pro.schmid.android.androidonfire.FirebaseEngine;
import pro.schmid.android.androidonfire.callbacks.FirebaseLoaded;
import pro.schmid.android.whereareyou.NameFragment.NameDialogListener;
import pro.schmid.android.whereareyou.TutorialDialog.TutorialDialogListener;
import pro.schmid.android.whereareyou.utils.Constants;
import pro.schmid.android.whereareyou.utils.Utils;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * Main activity to show the people of a room on a map.
 * Connects to Firebase, get the user's username, launch the map manager.
 */
public class MainActivity extends FragmentActivity implements NameDialogListener, TutorialDialogListener {

	private FirebaseMapManager mFirebaseMapManager;
	private FirebaseEngine mEngine;
	private Firebase mCurrentFirebase;
	private String mUsername;
	private String mRoomFromIntent;

	private GoogleMap mMap;

	private SharedPreferences mPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		setProgressBarIndeterminateVisibility(true);

		if (!Utils.isOnline(this)) {
			showUserIsOffline();
			return;
		}

		// Check that Google Play Services is available
		int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (googlePlayServicesAvailable != ConnectionResult.SUCCESS) {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, this, -1);
			errorDialog.show();
			return;
		}

		// Check if the user is coming from a link he clicked (using an existing room)
		// or if we have to create a new room.
		Uri data = getIntent().getData();
		if (data != null) {
			List<String> params = data.getPathSegments();
			mRoomFromIntent = params.get(0);
		}

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Get the username.
		// Once the username is here, it will trigger the map manager.
		getUserName();
	}

	@Override
	protected void onStart() {
		super.onStart();

		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			mMap.setMyLocationEnabled(true);
			mMap.setOnMyLocationChangeListener(mMyLocationListener);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();

		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mEngine != null) {
			mEngine.onDestroy();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem findItem = menu.findItem(R.id.menu_share_group);
		findItem.setVisible(mCurrentFirebase != null);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_share_group:
				shareGroup();
				return true;

			case R.id.menu_prefs:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;

			case R.id.menu_about:
				intent = new Intent(this, AboutActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Share the group using the Android Intent
	 */
	private void shareGroup() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		String text = getString(R.string.share_text, Constants.BASE_URL + mCurrentFirebase.name());
		sendIntent.putExtra(Intent.EXTRA_TEXT, text);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}

	/**
	 * Start the application once everything is settled
	 */
	private void startApplication() {
		loadFirebase();
	}

	/**
	 * Load the Firebase engine.
	 */
	private void loadFirebase() {
		mEngine = FirebaseEngine.getInstance();
		mEngine.setLoadedListener(new FirebaseLoaded() {

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void firebaseLoaded() {

				setProgressBarIndeterminateVisibility(false);

				// Get the root Firebase
				Firebase firebase = mEngine.newFirebase(Constants.FIREBASE_URL).child(Constants.PROTOCOL_VERSION);

				// Check if we have to create a new room or join an existing one.
				if (mRoomFromIntent != null) {
					mCurrentFirebase = firebase.child(mRoomFromIntent);
				} else {
					mCurrentFirebase = firebase.push();
					showTutorial();
				}

				// Create the manager
				mFirebaseMapManager = new FirebaseMapManager(MainActivity.this, mMap, mCurrentFirebase, mUsername);

				// Set the user location is available already
				Location myLocation = mMap.getMyLocation();
				if (myLocation != null) {
					mFirebaseMapManager.setMyLocation(myLocation);
				}

				// Change the actionbar now we can share the room
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
					invalidateOptionsMenu();
				}
			}
		});
		mEngine.loadEngine(this);
	}

	/**
	 * Show a dialog telling the user that he has now to share his room to see his friends.
	 */
	protected void showTutorial() {

		// Do not show the tutorial if it was shown a certain amount of times
		int times = mPreferences.getInt(Constants.PREF_SHOW_TUTORIAL, 0);
		if (times >= Constants.SHOW_TUTORIAL_TIMES) {
			return;
		}
		mPreferences.edit().putInt(Constants.PREF_SHOW_TUTORIAL, times + 1).commit();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog
		DialogFragment newFragment = TutorialDialog.newInstance();
		newFragment.show(ft, "dialog");
	}

	/**
	 * Get the username.
	 * First try to get it from the preferences,
	 * then get a name from the user's google account,
	 * otherwise ask him to enter a name manually.
	 * 
	 * Using the Google account allows to get the username without having to ask his name.
	 * This removes one dialog and goes straight to the application.
	 */
	private void getUserName() {
		mUsername = mPreferences.getString(Constants.PREF_NAME, null);

		if (mUsername == null) {
			// Try to get the username from the accounts
			mUsername = Utils.getAccountUsername(this);

			if (mUsername == null) {
				DialogFragment newFragment = NameFragment.newInstance();
				newFragment.show(getSupportFragmentManager(), "name");
			} else {
				setUsername(mUsername);
			}
		} else {
			startApplication();
		}
	}

	/**
	 * The user chose a username.
	 */
	@Override
	public void onNameDialogPositiveClick(NameFragment dialog) {
		mUsername = dialog.getUsername();
		setUsername(mUsername);
	}

	/**
	 * Set the username and start the application
	 * 
	 * @param username
	 */
	private void setUsername(String username) {
		mPreferences.edit().putString(Constants.PREF_NAME, mUsername).commit();
		startApplication();
	}

	/**
	 * The user clicked on the tutorial dialog, share the room.
	 */
	@Override
	public void onTutorialDialogClick() {
		shareGroup();
	}

	/**
	 * Show a dialog telling the user that he has to be online to use the app.
	 */
	private void showUserIsOffline() {
		Builder builder = new AlertDialog.Builder(this);

		builder
				.setCancelable(false)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.offline_alert_text)
				.setTitle(R.string.offline_alert_title)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int button) {
						dialog.dismiss();
						finish();
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * Set the new location when we get a new position.
	 */
	private final OnMyLocationChangeListener mMyLocationListener = new OnMyLocationChangeListener() {

		private boolean mFirstCenter = true;

		@Override
		public void onMyLocationChange(Location location) {
			if (mFirebaseMapManager != null) {
				mFirebaseMapManager.setMyLocation(location);

				if (mFirstCenter) {
					mFirstCenter = false;

					CameraUpdate newLatLngZoom = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), Constants.MAP_ZOOM);
					mMap.animateCamera(newLatLngZoom);
				}
			}
		}
	};
}
