package com.sierrawireless.avphone;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A preference to enter a server host value.
 * 
 * Besides the default text field, it provides 2 shortcut buttons for predefined values (NA and EU prod server).
 */
public class ServerDialogPreference extends DialogPreference {

    private final LinearLayout layout = new LinearLayout(this.getContext());
    private final EditText editText = new EditText(this.getContext());

    private final Button naButton = new Button(this.getContext());
    private final Button euButton = new Button(this.getContext());

    public ServerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);

        naButton.setText(context.getString(R.string.pref_server_na));
        naButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText(R.string.pref_server_na_value);
            }
        });
        euButton.setText(context.getString(R.string.pref_server_eu));
        euButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText(R.string.pref_server_eu_value);
            }
        });

        layout.setOrientation(LinearLayout.VERTICAL);
    }

    // Create the Dialog view
    @Override
    protected View onCreateDialogView() {
        layout.addView(editText);
        layout.addView(naButton);
        layout.addView(euButton);
        return layout;
    }

    // Attach persisted values to Dialog
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        editText.setText(getPersistedString("EditText"), TextView.BufferType.NORMAL);
    }

    // Persist value and disassemble views
    @Override
    protected void onDialogClosed(boolean positiveresult) {
        super.onDialogClosed(positiveresult);
        if (positiveresult && shouldPersist()) {
            persistString(editText.getText().toString());
        }

        ((ViewGroup) editText.getParent()).removeView(editText);
        ((ViewGroup) naButton.getParent()).removeView(naButton);
        ((ViewGroup) euButton.getParent()).removeView(euButton);
        ((ViewGroup) layout.getParent()).removeView(layout);

        notifyChanged();
    }
}
