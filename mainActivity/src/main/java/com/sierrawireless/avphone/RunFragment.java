package com.sierrawireless.avphone;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;
import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.NewData;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;

public class RunFragment extends AvPhoneFragment implements MonitorServiceListener, CustomLabelsListener {

    private static final String LOGTAG = RunFragment.class.getName();

    private DataViewUpdater viewUpdater;

    private View view;

    private MonitorServiceManager monitorServiceManager;

    private CustomLabelsManager customLabelsManager;
    private String systemUid;
    private String systemName;

    private IAsyncTaskFactory taskFactory;
    private String objectName;
    private ObjectsManager objectsManager;

    public void setTaskFactory(IAsyncTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public void setObjectName(String name) {
        this.objectName = name;
    }

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
        viewUpdater = new DataViewUpdater(view, (MainActivity)getActivity());

        CustomDataLabels customLabels = PreferenceUtils.getCustomDataLabels(getActivity());
        setCustomDataLabels(customLabels);

        // register service listener
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(NewData.NEW_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(LogMessage.LOG_EVENT));

        boolean isServiceRunning = monitorServiceManager.isServiceRunning();

        SwitchCompat serviceSwitch = (SwitchCompat) view.findViewById(R.id.service_switch);
        serviceSwitch.setChecked(isServiceRunning);

        if (!this.monitorServiceManager.isServiceStarted()) {
            this.monitorServiceManager.startMonitoringService();
        }

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
            }
        }


        // Alarm button
        Button alarmButton = (Button) view.findViewById(R.id.alarm_btn);
        alarmButton.setOnClickListener(onAlarmClick);

        // Make links clickable in info view.
        TextView infoMessageView = (TextView) view.findViewById(R.id.run_info_message);
        infoMessageView.setLinksClickable(true);
        infoMessageView.setMovementMethod(LinkMovementMethod.getInstance());

        // Might had those before initialization
        if (systemUid != null && systemName != null) {
            setLinkToSystem(systemUid, systemName);
        }

        return view;
    }

    public void setLinkToSystem(String systemUid, String systemName) {

        if (view == null || getActivity() == null) {
            // View is unavailable, bear it in mind for later
            this.systemUid = systemUid;
            this.systemName = systemName;
            return;
        }

        final TextView infoMessageView = (TextView) view.findViewById(R.id.run_info_message);

        String infoMessage;
        if (systemUid != null) {

            AvPhonePrefs avPhonePrefs = PreferenceUtils.getAvPhonePrefs(getActivity());
            String link = String.format("https://%s/monitor/systems/systemDetails?uid=%s", avPhonePrefs.serverHost,
                    systemUid);

            infoMessage = getString(R.string.run_info_message_link, link, systemName);
            infoMessageView.setText(Html.fromHtml(infoMessage));

        } else {
            infoMessage = getString(R.string.run_info_message, DeviceInfo.getUniqueId(getActivity()));
            infoMessageView.setText(infoMessage);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isServiceRunning = monitorServiceManager.isServiceRunning();
        SwitchCompat serviceSwitch = getServiceSwitch();
        serviceSwitch.setChecked(isServiceRunning);

        String systemUid = ((MainActivity) getActivity()).getSystemUid();
        String systemName = ((MainActivity) getActivity()).getSystemName();

        this.setLinkToSystem(systemUid, systemName);

    }

    private SwitchCompat getServiceSwitch() {
        return (SwitchCompat) view.findViewById(R.id.service_switch);
    }

    private void startMonitoringService() {
        AvPhonePrefs avPrefs = PreferenceUtils.getAvPhonePrefs(getActivity());
        if (!avPrefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(getActivity());
            SwitchCompat serviceSwitch = getServiceSwitch();
            serviceSwitch.setChecked(false);
        } else {
            this.monitorServiceManager.startSendData();
        }

    }

    private void stopMonitoringService() {
        this.monitorServiceManager.stopSendData();
    }

    protected void setCustomDataLabels(CustomDataLabels customDataLabels) {
        TextView valueView = null;
        objectsManager = ObjectsManager.getInstance();
        AvPhoneObject object = objectsManager.getObjectByName(objectName);
        for (AvPhoneObjectData data : object.datas) {
            switch (data.label) {
                case "1":
                    valueView = (TextView) view.findViewById(R.id.run_custom1_label);
                    break;
                case "2":
                    valueView = (TextView) view.findViewById(R.id.run_custom2_label);
                    break;
                case "3":
                    valueView = (TextView) view.findViewById(R.id.run_custom3_label);
                    break;
                case "4":
                    valueView = (TextView) view.findViewById(R.id.run_custom4_label);
                    break;
                case "5":
                    valueView = (TextView) view.findViewById(R.id.run_custom5_label);
                    break;
                case "6":
                    valueView = (TextView) view.findViewById(R.id.run_custom6_label);
                    break;
            }
            valueView.setText(data.name);
        }
    }

    // Alarm button
    View.OnClickListener onAlarmClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(LOGTAG, "On alarm button click");

            monitorServiceManager.sendAlarmEvent(true);
        }
    };

    protected TextView getErrorMessageView() {
        return (TextView) view.findViewById(R.id.run_error_message);
    }

    @Override
    public void onServiceStarted(MonitoringService service) {
        view.findViewById(R.id.toggle_to_start).setVisibility(View.GONE);
        view.findViewById(R.id.started_since).setVisibility(View.VISIBLE);
        view.findViewById(R.id.service_log).setVisibility(View.VISIBLE);
        viewUpdater.onStart(service.getStartedSince(), service.getLastData(), service.getLastLog(),
                service.getLastRun());
    }

    @Override
    public void onServiceStopped(MonitoringService service) {
        view.findViewById(R.id.toggle_to_start).setVisibility(View.VISIBLE);
        view.findViewById(R.id.started_since).setVisibility(View.GONE);
        view.findViewById(R.id.service_log).setVisibility(View.GONE);
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
