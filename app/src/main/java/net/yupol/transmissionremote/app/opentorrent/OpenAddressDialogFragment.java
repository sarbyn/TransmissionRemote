package net.yupol.transmissionremote.app.opentorrent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.yupol.transmissionremote.app.R;

public class OpenAddressDialogFragment extends DialogFragment {

    private OnOpenMagnetListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(R.layout.open_address_dialog, null);
        final EditText addressText = (EditText) view.findViewById(R.id.address_text);

        builder.setView(view)
               .setTitle(R.string.address_of_torrent_file)
               .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       listener.onOpenMagnet(addressText.getText().toString());
                   }
               })
               .setNegativeButton(android.R.string.cancel, null);

        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null && clipData.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            CharSequence text = clipData.getItemAt(0).getText();
            if (text != null) {
                addressText.setText(text);
                addressText.setSelection(0, addressText.getText().length());
            }
        }

        final AlertDialog dialog = builder.create();

        addressText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                Button openButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                openButton.setEnabled(!s.toString().isEmpty());
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                Button openButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                openButton.setEnabled(!addressText.getText().toString().isEmpty());
            }
        });

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnOpenMagnetListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + OnOpenMagnetListener.class.getSimpleName());
        }
    }

    public interface OnOpenMagnetListener {
        void onOpenMagnet(String uri);
    }
}
