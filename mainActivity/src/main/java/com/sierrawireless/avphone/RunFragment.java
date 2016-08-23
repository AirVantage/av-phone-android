package com.sierrawireless.avphone;

import android.app.Activity;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.NewData;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

public class RunFragment extends AvPhoneFragment implements CustomLabelsListener, CustomLabelsManager, MonitorServiceListener, OnSharedPreferenceChangeListener {

    private static final String LOGTAG = RunFragment.class.getName();

    private DataViewUpdater viewUpdater;

    private MonitorServiceManager monitorServiceManager;

    private CustomLabelsManager customLabelsManager;
    private CustomLabelsListener customLabelsListener;
    private SharedPreferences prefs;

    protected void setMonitorServiceManager(MonitorServiceManager manager) {
        this.monitorServiceManager = manager;
        this.monitorServiceManager.setMonitoringServiceListener(this);
    }

    protected void setCustomLabelsManager(CustomLabelsManager manager) {
        this.customLabelsManager = manager;
        this.customLabelsManager.setCustomLabelsListener(this);
    }

    public void setCustomLabelsListener(CustomLabelsListener customLabelsListener) {
        this.customLabelsListener = customLabelsListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        setContentView(R.layout.fragment_run);
        viewUpdater = new DataViewUpdater(new DataViewUpdater.IViewFinder() {
            @Override
            public View findViewById(int id) {
                return findViewById(id);
            }

            @Override
            public String getString(int resId) {
                return getApplicationContext().getString(resId);
            }
        });

        CustomDataLabels customLabels = PreferenceUtils.getCustomDataLabels(this);
        setCustomDataLabels(customLabels);

        // Register service listener
        registerReceiver(viewUpdater, new IntentFilter(NewData.NEW_DATA));
        registerReceiver(viewUpdater, new IntentFilter(LogMessage.LOG_EVENT));

        setCustomLabelsManager(this);
      //  setMonitorServiceManager(this);
        boolean isServiceRunning = monitorServiceManager.isServiceRunning();

        SwitchCompat serviceSwitch = (SwitchCompat) findViewById(R.id.service_switch);
        serviceSwitch.setChecked(isServiceRunning);

        serviceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startMonitoringService();
                } else {
                    stopMonitoringService();
                }
            }
        });

        if (isServiceRunning) {
            MonitoringService service = monitorServiceManager.getMonitoringService();
            if (service != null) {
                this.onServiceStarted(service);
            } else {
                // Activity is not yet connected to the service.
                // As a MonitorServiceListener, we will be notified when the service is available.
            }
        }

        // Alarm button
        SwitchCompat alarmButton = (SwitchCompat) findViewById(R.id.alarm_switch);
        alarmButton.setOnCheckedChangeListener(onAlarmClick);

        // Make links clickable in info view.
        TextView infoMessageView = (TextView) findViewById(R.id.run_info_message);
        infoMessageView.setLinksClickable(true);
        infoMessageView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setLinkToSystem(String systemUid, String systemName) {

        final TextView infoMessageView = (TextView) findViewById(R.id.run_info_message);
        final String infoMessage;
        if (systemUid != null && systemName != null) {

            final AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getParent());
            final String link = String.format("https://%s/monitor/systems/systemDetails?uid=%s",
                    avPhonePrefs.serverHost, systemUid);

            infoMessage = getString(R.string.run_info_message_link, link, systemName);
            infoMessageView.setText(Html.fromHtml(infoMessage));

        } else {
            infoMessage = getString(R.string.run_info_message, DeviceInfo.getUniqueId(getParent()));
            infoMessageView.setText(infoMessage);
        }

    }

    private void updateLinkToSystem() {

        if (prefs == null) {
            return;
        }

        final String systemUid = prefs.getString(MainActivity.PREFERENCE_SYSTEM_UID, null);
        final String systemName = prefs.getString(MainActivity.PREFERENCE_SYSTEM_NAME, null);
        this.setLinkToSystem(systemUid, systemName);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateLinkToSystem();
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isServiceRunning = monitorServiceManager.isServiceRunning();
        SwitchCompat serviceSwitch = getServiceSwitch();
        serviceSwitch.setChecked(isServiceRunning);

        updateLinkToSystem();
    }

    private SwitchCompat getServiceSwitch() {
        return (SwitchCompat) findViewById(R.id.service_switch);
    }

    private void startMonitoringService() {
        AvPhonePrefs avPrefs = PreferenceUtils.getAvPhonePrefs(getParent());
        if (!avPrefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(this);
            SwitchCompat serviceSwitch = getServiceSwitch();
            serviceSwitch.setChecked(false);
        } else {
            this.monitorServiceManager.startMonitoringService();
        }

    }

    private void stopMonitoringService() {
        this.monitorServiceManager.stopMonitoringService();
    }

    protected void setCustomDataLabels(CustomDataLabels customDataLabels) {
        TextView labelView = (TextView) findViewById(R.id.run_custom1_label);
        labelView.setText(customDataLabels.customUp1Label);

        labelView = (TextView) findViewById(R.id.run_custom2_label);
        labelView.setText(customDataLabels.customUp2Label);

        labelView = (TextView) findViewById(R.id.run_custom3_label);
        labelView.setText(customDataLabels.customDown1Label);

        labelView = (TextView) findViewById(R.id.run_custom4_label);
        labelView.setText(customDataLabels.customDown2Label);

        labelView = (TextView) findViewById(R.id.run_custom5_label);
        labelView.setText(customDataLabels.customStr1Label);

        labelView = (TextView) findViewById(R.id.run_custom6_label);
        labelView.setText(customDataLabels.customStr2Label);
    }

    // Alarm button

    OnCheckedChangeListener onAlarmClick = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(LOGTAG, "On alarm button click");

            monitorServiceManager.sendAlarmEvent(isChecked);

        }
    };

    protected TextView getErrorMessageView() {
        return (TextView) findViewById(R.id.run_error_message);
    }

    @Override
    public void onServiceStarted(MonitoringService service) {
        findViewById(R.id.toggle_to_start).setVisibility(View.GONE);
        findViewById(R.id.started_since).setVisibility(View.VISIBLE);
        findViewById(R.id.service_log).setVisibility(View.VISIBLE);
        viewUpdater.onStart(service.getStartedSince(), service.getLastData(), service.getLastLog(),
                service.getLastRun());
    }

    @Override
    public void onServiceStopped(MonitoringService service) {
        findViewById(R.id.toggle_to_start).setVisibility(View.VISIBLE);
        findViewById(R.id.started_since).setVisibility(View.GONE);
        findViewById(R.id.service_log).setVisibility(View.GONE);
        viewUpdater.onStop();
    }

    @Override
    public void onCustomLabelsChanged() {
        CustomDataLabels customLabels = PreferenceUtils.getCustomDataLabels(getParent());
        setCustomDataLabels(customLabels);
    }
}
