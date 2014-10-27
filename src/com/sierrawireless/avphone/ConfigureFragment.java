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

import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.task.AlertRuleClient;
import com.sierrawireless.avphone.task.ApplicationClient;
import com.sierrawireless.avphone.task.IAlertRuleClient;
import com.sierrawireless.avphone.task.IApplicationClient;
import com.sierrawireless.avphone.task.ISystemClient;
import com.sierrawireless.avphone.task.SyncWithAvTask;
import com.sierrawireless.avphone.task.SystemClient;

public class ConfigureFragment extends Fragment {

    public static final String PHONE_UNIQUE_ID = Build.SERIAL;

    private Button syncBt;

    private EditText customData1EditText;
    private EditText customData2EditText;
    private EditText customData3EditText;
    private EditText customData4EditText;
    private EditText customData5EditText;
    private EditText customData6EditText;

    private View view;

    private PreferenceUtils prefUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_configure, container, false);

        // phone identifier
        ((TextView) view.findViewById(R.id.phoneid_value)).setText(PHONE_UNIQUE_ID);

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
            Intent intent = new Intent(getActivity(), AuthorizationActivity.class);
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
                syncWithAv(token);
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

    private void syncWithAv(String token) {

        AvPhonePrefs prefs = prefUtils.getAvPhonePrefs();

        AirVantageClient client = new AirVantageClient(prefs.serverHost, token);

        IApplicationClient appClient = new ApplicationClient(client);
        ISystemClient systemClient = new SystemClient(client);
        IAlertRuleClient alertRuleClient = new AlertRuleClient(client);

        SyncWithAvTask syncTask = new SyncWithAvTask(appClient, systemClient, alertRuleClient);

        // try to get the IMEI for GSM phones
        String imei = null;
        TelephonyManager telManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            imei = telManager.getDeviceId();
        }

        syncTask.execute(PHONE_UNIQUE_ID, imei, prefs.password, getCustomDataLabels());
        try {

            AvError error = syncTask.get();

            if (error != null) {
                if (error.systemAlreadyExists()) {
                    toast("Error : A system already exists with this serial number (maybe in another company.)");
                } else if (error.applicationAlreadyUsed()) {
                    toast("Error : An application with type " + AvPhoneApplication.appType(PHONE_UNIQUE_ID)
                            + " already exists (maybe in another company)");
                } else if (error.tooManyAlerRules()){
                    toast("Error : There are too many alert rules registered in your company.");
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
