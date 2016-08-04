package com.sierrawireless.avphone;

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

import com.sierrawireless.avphone.auth.AuthUtils;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvResult;
import com.sierrawireless.avphone.task.SyncWithAvTask;

public class ConfigureFragment extends AvPhoneFragment {

    private Button saveBt;

    private EditText customData1EditText;
    private EditText customData2EditText;
    private EditText customData3EditText;
    private EditText customData4EditText;
    private EditText customData5EditText;
    private EditText customData6EditText;

    private View view;

    private String deviceId;
    private String imei;

    private IAsyncTaskFactory taskFactory;

    public ConfigureFragment() {
        super();
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
        saveBt = (Button) view.findViewById(R.id.save_bt);
        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegisterClicked();
            }
        });

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

        res.setText(PreferenceUtils.getPreference(getActivity(), prefKeyId, labelDefaultKeyId));

        res.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                PreferenceUtils.setPreference(getActivity(), getActivity().getString(prefKeyId), s.toString());
            }
        });
        return res;
    }

    private boolean checkCredentials() {

        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());

        if (!prefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(getActivity());
            return false;
        }

        return true;

    }

    protected void onRegisterClicked() {
        if (checkCredentials()) {
            Authentication auth = authManager.getAuthentication();
            if (auth != null && !auth.isExpired()) {
                syncWithAv(auth.getAccessToken());
            } else {
                requestAuthentication();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Authentication auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data);
        if (auth != null) {
            authManager.onAuthentication(auth);
            syncWithAv(auth.getAccessToken());
        }
    }

    private void syncWithAv(String token) {

        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());

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
            public void onSynced(SyncWithAvResult result) {
                syncTask.showResult(result, display, getActivity());

                if (!result.isError()) {
                    syncListener.onSynced(result);
                }

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
