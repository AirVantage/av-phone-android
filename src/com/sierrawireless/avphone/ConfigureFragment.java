package com.sierrawireless.avphone;

import net.airvantage.model.AvError;
import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
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

import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.task.AlertRuleClient;
import com.sierrawireless.avphone.task.ApplicationClient;
import com.sierrawireless.avphone.task.IAlertRuleClient;
import com.sierrawireless.avphone.task.IApplicationClient;
import com.sierrawireless.avphone.task.ISystemClient;
import com.sierrawireless.avphone.task.RegisterSystemTask;
import com.sierrawireless.avphone.task.SystemClient;
import com.sierrawireless.avphone.task.UpdateDataTask;

public class ConfigureFragment extends Fragment {

    public static final String PHONE_UNIQUE_ID = Build.SERIAL;

    private static final int CONTEXT_REGISTER = 0;
    private static final int CONTEXT_UPDATE_DATA = 1;

    private Button registerBt;

    private EditText customData1EditText;
    private EditText customData2EditText;
    private EditText customData3EditText;
    private EditText customData4EditText;
    private EditText customData5EditText;
    private EditText customData6EditText;

    private View updateDataBt;

    private View view;

    private PreferenceUtils prefUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_configure, container, false);

        // phone identifier
        ((TextView) view.findViewById(R.id.phoneid_value)).setText(PHONE_UNIQUE_ID);

        // Register button
        registerBt = (Button) view.findViewById(R.id.register_bt);
        registerBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegisterClicked();
            }
        });

        updateDataBt = (Button) view.findViewById(R.id.update_data_bt);
        updateDataBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUpdateDataClicked();
            }
        });
        updateDataBt.setEnabled(false);

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
                updateDataBt.setEnabled(true);
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
            Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
            intent.putExtra(AuthorizationActivity.AUTHORIZATION_CONTEXT, CONTEXT_REGISTER);
            startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
        }
    }

    private void onUpdateDataClicked() {
        if (checkCredentials()) {
            Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
            intent.putExtra(AuthorizationActivity.AUTHORIZATION_CONTEXT, CONTEXT_UPDATE_DATA);
            startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case (AuthorizationActivity.REQUEST_AUTHORIZATION): {
            if (resultCode == Activity.RESULT_OK) {
                String token = data.getStringExtra(AuthorizationActivity.TOKEN);

                int request = data.getExtras().getInt(AuthorizationActivity.AUTHORIZATION_CONTEXT);
                if (request == ConfigureFragment.CONTEXT_REGISTER) {
                    registerSystem(token);
                } else if (request == ConfigureFragment.CONTEXT_UPDATE_DATA) {
                    updateData(token);
                }

            }
            break;
        }
        }
    }

    private void toast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private void toastError(Exception e, String label, String message) {
        Log.e(MainActivity.class.getName(), label, e);
        toast(message);
    }

    private void registerSystem(String token) {

        AvPhonePrefs prefs = prefUtils.getAvPhonePrefs();

        AirVantageClient client = new AirVantageClient(prefs.serverHost, token);

        IApplicationClient appClient = new ApplicationClient(client);
        ISystemClient systemClient = new SystemClient(client);
        IAlertRuleClient alertRuleClient = new AlertRuleClient(client);
        
        RegisterSystemTask registerTask = new RegisterSystemTask(appClient, systemClient, alertRuleClient);

        // try to get the IMEI for GSM phones
        String imei = null;
        TelephonyManager telManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            imei = telManager.getDeviceId();
        }

        registerTask.execute(PHONE_UNIQUE_ID, imei, prefs.password, getCustomDataLabels());
        try {

            AvError error = registerTask.get();

            if (error != null) {
                toast("An error occured when registering system.");
            } else {
                toast("System registered on AirVantage.");
            }
        } catch (Exception e) {
            toastError(e, "Error", "An error occured when registering system.");
        }
    }

    private void updateData(String token) {

        AvPhonePrefs prefs = prefUtils.getAvPhonePrefs();

        AirVantageClient client = new AirVantageClient(prefs.serverHost, token);

        IApplicationClient appClient = new ApplicationClient(client);

        UpdateDataTask updateDataTask = new UpdateDataTask(appClient);

        updateDataTask.execute(PHONE_UNIQUE_ID, getCustomDataLabels());
        try {

            AvError error = updateDataTask.get();

            if (error == null) {
                toast("Data updated on AirVantage.");
                updateDataBt.setEnabled(false);
            } else {
                if (error.systemAlreadyExists(error)) {
                    toast("Sorry, the system is already registered in another company.");
                } else {
                    toast("An error occured when updating data : " + error.error);
                }
            }
        } catch (Exception e) {
            toastError(e, "Error", "An error occured when updating data.");
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
