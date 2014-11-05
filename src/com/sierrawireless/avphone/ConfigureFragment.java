package com.sierrawireless.avphone;

import net.airvantage.model.AvError;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationListener;
import com.sierrawireless.avphone.auth.IAuthenticationManager;
import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class ConfigureFragment extends Fragment implements AuthenticationListener {

    private Button syncBt;

    private EditText customData1EditText;
    private EditText customData2EditText;
    private EditText customData3EditText;
    private EditText customData4EditText;
    private EditText customData5EditText;
    private EditText customData6EditText;

    private View view;

    private PreferenceUtils prefUtils;

    private String deviceId;
    private String imei;

    private IAuthenticationManager authManager;

    private IAsyncTaskFactory taskFactory;
    
    public ConfigureFragment() {
        super();
    }
    
    public ConfigureFragment(IAuthenticationManager authManager, IAsyncTaskFactory taskFactory) {
        super();
        this.authManager = authManager;
        this.taskFactory = taskFactory;
    }
    
    public void setAuthenticationManager(IAuthenticationManager authManger) {
        this.authManager = authManger;
    }
    
    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_configure, container, false);

        // phone identifier
        deviceId = DeviceInfo.getUniqueId(this.getActivity());
        ((TextView) view.findViewById(R.id.phoneid_value)).setText(deviceId);

        // try to get the IMEI for GSM phones
        imei = DeviceInfo.getIMEI(this.getActivity());
        
        // Register button
        syncBt = (Button) view.findViewById(R.id.sync_bt);
        syncBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegisterClicked();
            }
        });

        prefUtils = new PreferenceUtils(this);

        // Fields for custom data
        customData1EditText = buildCustomLabelEditText(view, R.id.custom1_value, R.string.pref_custom1_label_key,
                R.string.pref_custom1_label_default);
        customData2EditText = buildCustomLabelEditText(view, R.id.custom2_value, R.string.pref_custom2_label_key,
                R.string.pref_custom2_label_default);
        customData3EditText = buildCustomLabelEditText(view, R.id.custom3_value, R.string.pref_custom3_label_key,
                R.string.pref_custom3_label_default);
        customData4EditText = buildCustomLabelEditText(view, R.id.custom4_value, R.string.pref_custom4_label_key,
                R.string.pref_custom4_label_default);
        customData5EditText = buildCustomLabelEditText(view, R.id.custom5_value, R.string.pref_custom5_label_key,
                R.string.pref_custom5_label_default);
        customData6EditText = buildCustomLabelEditText(view, R.id.custom6_value, R.string.pref_custom6_label_key,
                R.string.pref_custom6_label_default);

        return view;
    }

    private EditText buildCustomLabelEditText(View view, int id, final int prefKeyId, int labelDefaultKeyId) {
        EditText res = (EditText) view.findViewById(id);

        res.setText(prefUtils.getPreference(prefKeyId, labelDefaultKeyId));

        res.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                prefUtils.setPreference(prefKeyId, s.toString());
            }
        });
        return res;
    }

    private boolean checkCredentials() {

        AvPhonePrefs prefs = prefUtils.getAvPhonePrefs();

        if (!prefs.checkCredentials()) {
            prefUtils.showMissingPrefsDialog();
            return false;
        }

        return true;

    }

    protected void onRegisterClicked() {
        if (checkCredentials()) {
            authManager.authenticate(prefUtils, this, this);
        }
    }

    @Override
    public void onAuthentication(Authentication auth) {
        syncWithAv(auth.getAccessToken());
    }

    // TODO(pht) refactor wtih HomeFragment
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Authentication auth = authManager.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {
            authManager.saveAuthentication(prefUtils, auth);
            onAuthentication(auth);
        }
    }
    
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode) {
//        case (AuthorizationActivity.REQUEST_AUTHORIZATION): {
//            if (resultCode == Activity.RESULT_OK) {
//                String token = data.getStringExtra(AuthorizationActivity.AUTHENTICATION_TOKEN);
//                syncWithAv(token);
//            }
//            break;
//        }
//        }
//    }

    private void toast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private void toastError(Exception e, String label, String message) {
        Log.e(MainActivity.class.getName(), label, e);
        toast(message);
    }

    private void syncWithAv(String token) {

        AvPhonePrefs prefs = prefUtils.getAvPhonePrefs();

        SyncWithAvTask syncTask = taskFactory.syncAvTask(prefs.serverHost, token);
        
        SyncWithAvParams syncParams = new SyncWithAvParams();
        syncParams.deviceId = deviceId;
        syncParams.imei = imei;
        syncParams.mqttPassword = prefs.password;
        syncParams.customData = getCustomDataLabels();
        
        syncTask.execute(syncParams);
        try {

            AvError error = syncTask.get();

            if (error != null) {
                if (error.systemAlreadyExists()) {
                    toast("Error : A system already exists with this serial number (maybe in another company.)");
                } else if (error.applicationAlreadyUsed()) {
                    toast("Error : An application with type " + AvPhoneApplication.appType(deviceId)
                            + " already exists (maybe in another company)");
                } else if (error.tooManyAlerRules()) {
                    toast("Error : There are too many alert rules registered in your company.");
                } else if (error.cantCreateApplication()) {
                    toast("Error : You don't have the right to create application. Contact your administrator");
                } else if (error.cantCreateSystem()) {
                    toast("Error : You don't have the right to create a system. Contact your administrator");
                } else if (error.cantCreateAlertRule()) {
                    toast("Error : You don't have the right to create an alert rule. Contact your administrator");
                } else if (error.cantUpdateApplication()) {
                    toast("Error : You don't have the right to update an application. Contact your administrator");
                } else if (error.cantUpdateSystem()) {
                    toast("Error : You don't have the right to update a system. Contact your administrator");
                } else if (error.forbidden()) {
                    String method = error.errorParameters.get(0);
                    String url = error.errorParameters.get(1);
                    toast("Error : You are not allowed to " + method + " on URL " + url);
                } else {
                    toast("Error : Unexpected error (" + error.error + ").");
                }
            } else {
                toast("Synchronized with airvantage.");
            }
        } catch (Exception e) {
            toastError(e, "Error", "An error occured when synchronizing with AirVantage.");
        }
    }

    protected CustomDataLabels getCustomDataLabels() {
        CustomDataLabels customData = new CustomDataLabels();
        customData.customUp1Label = customData1EditText.getText().toString();
        customData.customUp2Label = customData2EditText.getText().toString();
        customData.customDown1Label = customData3EditText.getText().toString();
        customData.customDown2Label = customData4EditText.getText().toString();
        customData.customStr1Label = customData5EditText.getText().toString();
        customData.customStr2Label = customData6EditText.getText().toString();
        return customData;
    }

}
