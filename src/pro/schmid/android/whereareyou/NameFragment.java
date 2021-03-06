package pro.schmid.android.whereareyou;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Dialog used to ask the user's name.
 */
public class NameFragment extends DialogFragment {

	public interface NameDialogListener {
		public void onNameDialogPositiveClick(NameFragment dialog);
	}

	private NameDialogListener mListener;
	private String mUsername;
	private View mRoot;

	public static NameFragment newInstance() {
		return new NameFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Verify that the host activity implements the callback interface
		try {
			mListener = (NameDialogListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement NameDialogListener");
		}
	}

	public String getUsername() {
		return mUsername;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Build the dialog and set up the button click handlers
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		mRoot = inflater.inflate(R.layout.name_dialog, null);

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		builder.setView(mRoot).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {
				EditText username = (EditText) mRoot.findViewById(R.id.username);
				mUsername = username.getText().toString();

				// Send the positive button event back to the host activity
				mListener.onNameDialogPositiveClick(NameFragment.this);
			}
		});
		return builder.create();
	}

}
