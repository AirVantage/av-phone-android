package com.sierrawireless.avphone;

import net.airvantage.model.AvError;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.auth.AuthenticationListener;
import com.sierrawireless.avphone.auth.IAuthenticationManager;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class ConfigureFragment extends AvPhoneFragment implements AuthenticationListener {

    
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

    private IAsyncTaskFactory taskFactory;
    private IAuthenticationManager authManager;

    public ConfigureFragment() {
        super();
    }

    protected ConfigureFragment(IAsyncTaskFactory taskFactory, IAuthenticationManager authManager) {
        super();
        assert (taskFactory != null);
        assert (authManager != null);
        this.taskFactory = taskFactory;
        this.authManager = authManager;
    }
    
    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public void setAuthenticationManager(IAuthenticationManager authManager) {
        this.authManager = authManager;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Authentication auth = authManager.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {
            authManager.saveAuthentication(prefUtils, auth);
            onAuthentication(auth);
        }
    }


    private void syncWithAv(String token) {

        AvPhonePrefs prefs = prefUtils.getAvPhonePrefs();

        final IMessageDisplayer display = this;

        final SyncWithAvTask syncTask = taskFactory.syncAvTask(prefs.serverHost, token);

        SyncWithAvParams syncParams = new SyncWithAvParams();
        syncParams.deviceId = deviceId;
        syncParams.imei = imei;
        syncParams.mqttPassword = prefs.password;
        syncParams.customData = getCustomDataLabels();

        syncTask.execute(syncParams);
        
        syncTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(AvError error) {
                syncTask.showResult(error, display, getActivity());    
        }
        });


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

    public TextView getErrorMessageView() {
        return (TextView) view.findViewById(R.id.configure_error_message);
    }

}
