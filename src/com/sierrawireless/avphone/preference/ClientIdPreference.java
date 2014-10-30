package com.sierrawireless.avphone.preference;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

import com.sierrawireless.avphone.R;

public class ClientIdPreference extends EditTextPreference {

    public ClientIdPreference(Context context) {
        super(context);
    }

    public ClientIdPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClientIdPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {

        if (dependency instanceof ServerDialogPreference) {
            String server = ((ServerDialogPreference) dependency).getText();

            // update client ID preference if NA or EU server
            if (server != null) {
                if (server.equals(getContext().getString(R.string.pref_server_na_value))) {
                    setText(getContext().getString(R.string.pref_client_id_na));
                } else if (server.equals(getContext().getString(R.string.pref_server_eu_value))) {
                    setText(getContext().getString(R.string.pref_client_id_eu));
                }
            }
        }
    }

}
