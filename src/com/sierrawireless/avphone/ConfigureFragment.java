package com.sierrawireless.avphone;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import net.airvantage.utils.AirVantageClient;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigureFragment extends Fragment implements OnSharedPreferenceChangeListener {

    public static final String PHONE_UNIQUE_ID = Build.SERIAL;

    private Button registerBt;

    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_configure, container, false);

        // phone identifier
        ((TextView) view.findViewById(R.id.phoneid_value)).setText(PHONE_UNIQUE_ID);

        // Register button
        registerBt = (Button) view.findViewById(R.id.register_bt);
        registerBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });

        // Preferences
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        return view;
    }

    protected void register() {
        // Is there a token available in the local storage?
        // Yes
        // -- Is the token still valid?
        // -- Yes
        // ---- Register the system
        // -- No
        // ---- Refresh token
        // ---- Register the system
        // No
        // -- Get a new token
        // Open authorization activity
        Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
        startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
        // -- Register the system
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case (AuthorizationActivity.REQUEST_AUTHORIZATION): {
            if (resultCode == Activity.RESULT_OK) {
                String token = data.getStringExtra(AuthorizationActivity.TOKEN);
                final String serverHost = prefs.getString(this.getString(R.string.pref_server_key), null);

                AsyncTask<String, Void, Boolean> registerTask = new AsyncTask<String, Void, Boolean>() {
                    protected Boolean doInBackground(String... params) {
                        try {
                            AirVantageClient client = new AirVantageClient(serverHost, params[0]);
                            net.airvantage.model.System system = new net.airvantage.model.System();
                            net.airvantage.model.System.Gateway gateway = new net.airvantage.model.System.Gateway();
                            gateway.serialNumber = PHONE_UNIQUE_ID;
                            system.gateway = gateway;
                            system.state = "READY";
                            client.create(system);
                            return true;
                        } catch (IOException e) {
                            Log.e(MainActivity.class.getName(), "Error when trying to get current user", e);
                            return false;
                        }
                    }
                };

                registerTask.execute(token);
                try {
                    if (registerTask.get()) {
                        Toast.makeText(getActivity(), "System registered on AirVantage.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "An error occured when registering system.", Toast.LENGTH_SHORT)
                                .show();
                    }
                } catch (InterruptedException e) {
                    Log.e(MainActivity.class.getName(), "Error", e);
                    Toast.makeText(getActivity(), "An error occured when registering system.", Toast.LENGTH_SHORT)
                            .show();
                } catch (ExecutionException e) {
                    Log.e(MainActivity.class.getName(), "Error", e);
                    Toast.makeText(getActivity(), "An error occured when registering system.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
        }
        }
    }

    // Preferences

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // TODO
    }

}
