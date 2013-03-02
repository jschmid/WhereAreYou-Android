package pro.schmid.android.whereareyou;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class TutorialDialog extends DialogFragment {

	public interface TutorialDialogListener {
		public void onTutorialDialogClick();
	}

	private TutorialDialogListener mListener;

	public static DialogFragment newInstance() {
		return new TutorialDialog();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Verify that the host activity implements the callback interface
		try {
			mListener = (TutorialDialogListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement TutorialDialogListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Dialog dialog = getDialog();

		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			lp.gravity = Gravity.TOP | Gravity.RIGHT;
		} else {
			lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		}

		View v = inflater.inflate(R.layout.tutorial_callout, container);

		View dialogContainer = v.findViewById(R.id.tutorial_container);
		dialogContainer.setOnClickListener(mDialogClick);

		return v;
	}

	private final OnClickListener mDialogClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			dismiss();
			mListener.onTutorialDialogClick();
		}
	};
}
