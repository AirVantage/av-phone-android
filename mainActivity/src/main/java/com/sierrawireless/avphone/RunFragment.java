package com.sierrawireless.avphone;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.TextView;

import com.sierrawireless.avphone.adapter.RunListViewAdapter;
import com.sierrawireless.avphone.auth.AuthUtils;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.NewData;
import com.sierrawireless.avphone.task.IAsyncTaskFactory;
import com.sierrawireless.avphone.task.SyncWithAvListener;
import com.sierrawireless.avphone.task.SyncWithAvParams;
import com.sierrawireless.avphone.task.SyncWithAvResult;
import com.sierrawireless.avphone.task.SyncWithAvTask;
import com.sierrawireless.avphone.tools.Tools;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class RunFragment extends AvPhoneFragment implements MonitorServiceListener, CustomLabelsListener {
    private static final String TAG = "RunFragment";

    private static final String LOGTAG = RunFragment.class.getName();

    private DataViewUpdater viewUpdater;

    private View view;

    private MonitorServiceManager monitorServiceManager;

    private String systemUid;
    private String systemName;

    private IAsyncTaskFactory taskFactory;
    private String objectName;
    private ObjectsManager objectsManager;
    Button phoneBtn;
    Button objectBtn;
    ListView phoneListView;
    ListView objectListView;


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
        manager.setCustomLabelsListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: " + this);
        objectsManager = ObjectsManager.getInstance();
        objectsManager.changeCurrent(objectName);

        view = inflater.inflate(R.layout.fragment_run, container, false);
        viewUpdater = new DataViewUpdater(view, (MainActivity)getActivity());


        // register service listener
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(NewData.NEW_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(LogMessage.LOG_EVENT));

        boolean isServiceRunning = monitorServiceManager.isServiceRunning();

        SwitchCompat serviceSwitch = view.findViewById(R.id.service_switch);
        serviceSwitch.setChecked(isServiceRunning);

        if (!this.monitorServiceManager.isServiceStarted(objectName)) {
            if (this.monitorServiceManager.oneServiceStarted()) {
                //stop the service
                this.monitorServiceManager.stopMonitoringService();
            }
            //registerNewDevice();
            this.monitorServiceManager.startMonitoringService(objectName);
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
        Button alarmButton = view.findViewById(R.id.alarm_btn);
        alarmButton.setOnClickListener(onAlarmClick);

        // Make links clickable in info view.
        TextView infoMessageView = view.findViewById(R.id.run_info_message);
        infoMessageView.setLinksClickable(true);
        infoMessageView.setMovementMethod(LinkMovementMethod.getInstance());

        // Might had those before initialization
        if (systemUid != null && systemName != null) {
            setLinkToSystem(systemUid, systemName);
        }

        phoneBtn = view.findViewById(R.id.phone);
        objectBtn = view.findViewById(R.id.object);
        phoneListView = view.findViewById(R.id.phoneListView);
        objectListView = view.findViewById(R.id.objectLstView);
        objectBtn.setText(objectName);
        phoneBtn.setBackgroundColor(getResources().getColor(R.color.grey_1));
        phoneListView.setVisibility(View.VISIBLE);
        objectListView.setVisibility(View.GONE);

        phoneBtn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    phoneListView.setVisibility(View.VISIBLE);
                    objectListView.setVisibility(View.GONE);
                    phoneBtn.setSelected(true);
                    phoneBtn.setPressed(true);
                    phoneBtn.setBackgroundColor(getResources().getColor(R.color.grey_1));
                    objectBtn.setSelected(false);
                    objectBtn.setPressed(false);
                    objectBtn.setBackgroundColor(getResources().getColor(R.color.grey_4));
                }
            }
        );

        objectBtn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    phoneListView.setVisibility(View.GONE);
                    objectListView.setVisibility(View.VISIBLE);
                    phoneBtn.setSelected(false);
                    phoneBtn.setPressed(false);
                    phoneBtn.setBackgroundColor(getResources().getColor(R.color.grey_4));
                    objectBtn.setSelected(true);
                    objectBtn.setPressed(true);
                    objectBtn.setBackgroundColor(getResources().getColor(R.color.grey_1));
                }
            }
        );

        setCustomDataLabels();
        setPhoneDataLabels();


        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AuthorizationActivity.REQUEST_AUTHORIZATION) {
            Authentication auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data);
            if (auth != null) {
                authManager.onAuthentication(auth);
                syncWithAv(auth.getAccessToken());
            }
        }
    }

    private void syncWithAv(String token) {

        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(getActivity());
        final IMessageDisplayer display = this;

        final SyncWithAvTask syncAvTask = taskFactory.syncAvTask(prefs.serverHost, token);


        final SyncWithAvParams params = new SyncWithAvParams();

        params.deviceId = DeviceInfo.getUniqueId(getActivity());
        params.imei = DeviceInfo.getIMEI(getActivity());
        params.deviceName = DeviceInfo.getDeviceName();
        params.iccid = DeviceInfo.getICCID(getActivity());
        params.mqttPassword = prefs.password;
        params.customData = PreferenceUtils.getCustomDataLabels(getActivity());
        //     params.current = ((MainActivity)getActivity()).current;
        params.activity = ((MainActivity)getActivity());

        syncAvTask.execute(params);
        syncAvTask.addProgressListener(new SyncWithAvListener() {
            @Override
            public void onSynced(SyncWithAvResult result) {
                syncAvTask.showResult(result, display, getActivity());

                if (!result.isError()) {
                    syncListener.onSynced(result);
                }

            }
        });


    }

    public void setLinkToSystem(String systemUid, String systemName) {

        if (view == null || getActivity() == null) {
            // View is unavailable, bear it in mind for later
            this.systemUid = systemUid;
            this.systemName = systemName;
            return;
        }

        final TextView infoMessageView = view.findViewById(R.id.run_info_message);

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
        this.monitorServiceManager.getMonitoringService().startSendData();

    }

    private void stopMonitoringService() {
        this.monitorServiceManager.stopSendData();
        this.monitorServiceManager.getMonitoringService().stopSendData();
    }


    private void setPhoneDataLabels(){
        ArrayList<HashMap<String, String>> listPhone = new ArrayList<>();

        HashMap<String, String> temp;


        temp = new HashMap<>();
        temp.put(Tools.NAME, "RSSI");
        temp.put(Tools.VALUE, "");
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Operator");
        temp.put(Tools.VALUE, "");
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Bytes Sent");
        temp.put(Tools.VALUE, "0 Mo");
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Bytes Received");
        temp.put(Tools.VALUE, "0 Mo");
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Network Type");
        temp.put(Tools.VALUE, "");
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Latitude");
        temp.put(Tools.VALUE, "");
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Longitude");
        temp.put(Tools.VALUE, "");
        listPhone.add(temp);
        RunListViewAdapter adapter = new RunListViewAdapter(getActivity(), listPhone);
        phoneListView.setAdapter(adapter);
        phoneListView.invalidateViews();

    }

    protected void setCustomDataLabels() {
        ArrayList<HashMap<String, String>> listObject = new ArrayList<>();


        objectsManager = ObjectsManager.getInstance();
        AvPhoneObject object = objectsManager.getObjectByName(objectName);
        HashMap<String,String> temp;
        for (AvPhoneObjectData data : object.datas) {
            temp = new HashMap<>();
            temp.put(Tools.NAME, data.name);
            if (data.isInteger()) {
                temp.put(Tools.VALUE, data.current.toString());
            }else{
                temp.put(Tools.VALUE, data.defaults);
            }
            listObject.add(temp);
        }
        RunListViewAdapter adapter = new RunListViewAdapter(getActivity(), listObject);
        objectListView.setAdapter(adapter);
        objectListView.invalidateViews();
    }

    // Alarm button
    View.OnClickListener onAlarmClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(LOGTAG, "On alarm button click");

            monitorServiceManager.sendAlarmEvent();
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
        view.findViewById(R.id.alarm_log).setVisibility(View.VISIBLE);
        viewUpdater.onStart(service.getStartedSince(), service.getLastData(), service.getLastLog(),
                service.getLastRun());
    }

    @Override
    public void onServiceStopped(MonitoringService service) {
        view.findViewById(R.id.toggle_to_start).setVisibility(View.VISIBLE);
        view.findViewById(R.id.started_since).setVisibility(View.GONE);
        view.findViewById(R.id.service_log).setVisibility(View.GONE);
        view.findViewById(R.id.alarm_log).setVisibility(View.GONE);
        viewUpdater.onStop();
    }

    @Override
    public void onCustomLabelsChanged() {
        // The activity can be null if the change is done while the fragment is not active.
        // This can wait for the activity to be resumed.
        if (getActivity() != null) {
            setCustomDataLabels();
        }
    }

}
