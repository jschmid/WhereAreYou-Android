package pro.schmid.android.whereareyou;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

			TextView title = (TextView) findViewById(R.id.title_textview);
			title.setText(getString(R.string.about_title, pInfo.versionName));
		} catch (NameNotFoundException e) {
			// Should not happen
		}

		ImageButton b;

		b = (ImageButton) findViewById(R.id.button_mail);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("message/rfc822");
				String email = getString(R.string.about_my_email);
				intent.putExtra(Intent.EXTRA_EMAIL, new String[] { email });
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_my_email_subject));
				startActivity(Intent.createChooser(intent, getString(R.string.about_send_email_with)));
			}
		});

		b = (ImageButton) findViewById(R.id.button_twitter);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_my_twitter)));
				startActivity(intent);
			}
		});

		b = (ImageButton) findViewById(R.id.button_gplus);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_my_gplus)));
				startActivity(intent);
			}
		});
	}
}
