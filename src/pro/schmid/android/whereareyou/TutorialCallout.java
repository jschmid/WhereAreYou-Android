package pro.schmid.android.whereareyou;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TutorialCallout extends DialogFragment {

	static TutorialCallout newInstance() {
		TutorialCallout f = new TutorialCallout();

		Bundle args = new Bundle();
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light_Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tutorial_callout, container, false);

		return v;
	}
}
