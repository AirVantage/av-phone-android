package com.sierrawireless.avphone;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.NewData;

public class RunFragment extends AvPhoneFragment implements MonitorServiceListener, CustomLabelsListener {

    private static final String LOGTAG = RunFragment.class.getName();

    private DataViewUpdater viewUpdater;

    private View view;

    private MonitorServiceManager monitorServiceManager;

    private CustomLabelsManager customLabelsManager;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof MonitorServiceManager) {
            this.setMonitorServiceManager((MonitorServiceManager) activity);
        }

        if (activity instanceof CustomLabelsManager) {
           this.setCustomLabelsManager((CustomLabelsManager) activity);
        }
    }

    protected void setMonitorServiceManager(MonitorServiceManager manager) {
        this.monitorServiceManager = manager;
        this.monitorServiceManager.setMonitoringServiceListener(this);
    }
    
    protected void setCustomLabelsManager(CustomLabelsManager manager) {
        this.customLabelsManager = manager;
        this.customLabelsManager.setCustomLabelsListener(this);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_run, container, false);
        viewUpdater = new DataViewUpdater(view);

        CustomDataLabels customLabels = PreferenceUtils.getCustomDataLabels(getActivity());
        setCustomDataLabels(customLabels);

        // register service listener
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(NewData.NEW_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(LogMessage.LOG_EVENT));

        boolean isServiceRunning = monitorServiceManager.isServiceRunning();

        Switch serviceSwitch = (Switch) view.findViewById(R.id.service_switch);
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
            this.onServiceStarted(monitorServiceManager.getMonitoringService());
        }
        
        // Alarm button
        Switch alarmButton = (Switch) view.findViewById(R.id.alarm_switch);
        alarmButton.setOnCheckedChangeListener(onAlarmClick);

        // Info message
        TextView infoMesageView = (TextView) view.findViewById(R.id.run_info_message);
        String infoMessage = getString(R.string.run_info_message, DeviceInfo.getUniqueId(getActivity()));
        infoMesageView.setText(infoMessage);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isServiceRunning = monitorServiceManager.isServiceRunning();
        Switch serviceSwitch = getServiceSwitch();
        serviceSwitch.setChecked(isServiceRunning);

    }

    public Switch getServiceSwitch() {
        return (Switch) view.findViewById(R.id.service_switch);
    }
    
    private void startMonitoringService() {
        AvPhonePrefs avPrefs = PreferenceUtils.getAvPhonePrefs(getActivity());
        if (!avPrefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(getActivity());
            Switch serviceSwitch = getServiceSwitch();
            serviceSwitch.setChecked(false);
        } else {
            this.monitorServiceManager.startMonitoringService();
        }

    }

    private void stopMonitoringService() {
        this.monitorServiceManager.stopMonitoringService();
    }

    protected void setCustomDataLabels(CustomDataLabels customDataLabels) {
        TextView labelView = (TextView) view.findViewById(R.id.run_custom1_label);
        labelView.setText(customDataLabels.customUp1Label);

        labelView = (TextView) view.findViewById(R.id.run_custom2_label);
        labelView.setText(customDataLabels.customUp2Label);

        labelView = (TextView) view.findViewById(R.id.run_custom3_label);
        labelView.setText(customDataLabels.customDown1Label);

        labelView = (TextView) view.findViewById(R.id.run_custom4_label);
        labelView.setText(customDataLabels.customDown2Label);

        labelView = (TextView) view.findViewById(R.id.run_custom5_label);
        labelView.setText(customDataLabels.customStr1Label);

        labelView = (TextView) view.findViewById(R.id.run_custom6_label);
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
        return (TextView) view.findViewById(R.id.run_error_message);
    }

    @Override
    public void onServiceStarted(MonitoringService service) {
        viewUpdater.onStart(service.getStartedSince(), service.getLastData(), service.getLastLog(),
                service.getLastRun());
    }

    @Override
    public void onServiceStopped(MonitoringService service) {
        viewUpdater.onStop();
    }
    
    @Override
    public void onCustomLabelsChanged() {
        // The activity can be null if the change is done while the fragment is not active.
        // This can wait for the activity to be resumed.
        if (getActivity() != null) {
            CustomDataLabels customLabels = PreferenceUtils.getCustomDataLabels(getActivity());
            setCustomDataLabels(customLabels);
        }
    }

}
