package com.sierrawireless.avphone;

import net.airvantage.utils.AvPhonePrefs;
import net.airvantage.utils.PreferenceUtils;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.MonitoringService.ServiceBinder;
import com.sierrawireless.avphone.service.NewData;

public class RunFragment extends Fragment implements OnSharedPreferenceChangeListener {

    private static final String LOGTAG = RunFragment.class.getName();

    private DataViewUpdater viewUpdater;

    private AlarmManager alarmManager;

    private View view;

    private PreferenceUtils prefUtils;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        // TODO Auto-generated method stub
        super.onHiddenChanged(hidden);
    }
    
    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onInflate(activity, attrs, savedInstanceState);
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_run, container, false);
        viewUpdater = new DataViewUpdater(view);

        prefUtils = new PreferenceUtils(this);
        prefUtils.addListener(this);

        CustomDataLabels customLabels = prefUtils.getCustomDataLabels();
        setCustomDataLabels(customLabels);

        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

        // register service listener
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(NewData.NEW_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(LogMessage.LOG_EVENT));

        // Preferences

        // Start/stop switch
        boolean isServiceRunning = isServiceRunning();

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
            connectToService();
        }

        // Alarm button
        ToggleButton alarmButton = (ToggleButton) view.findViewById(R.id.alarm_button);
        alarmButton.setOnClickListener(onAlarmClick);

        return view;
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startMonitoringService() {
        Intent intent = new Intent(getActivity(), MonitoringService.class);
        intent.putExtra(MonitoringService.DEVICE_ID, ConfigureFragment.PHONE_UNIQUE_ID);

        AvPhonePrefs avPrefs = prefUtils.getAvPhonePrefs();

        if (!avPrefs.checkCredentials()) {

            prefUtils.showMissingPrefsDialog();

            Switch serviceSwitch = (Switch) view.findViewById(R.id.service_switch);
            serviceSwitch.setChecked(false);
            return;
        }

        intent.putExtra(MonitoringService.SERVER_HOST, avPrefs.serverHost);
        intent.putExtra(MonitoringService.PASSWORD, avPrefs.password);

        PendingIntent pendingIntent = PendingIntent.getService(getActivity(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        // registering our pending intent with alarm manager
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, Integer.valueOf(avPrefs.period) * 60 * 1000,
                pendingIntent);

        this.connectToService();
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

    private void stopMonitoringService() {

        Intent intent = new Intent(getActivity(), MonitoringService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getActivity(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);
        getActivity().stopService(intent);

        disconnectFromService();

        viewUpdater.onStop();
    }

    // Service binding

    private void connectToService() {
        bound = getActivity().bindService(new Intent(getActivity(), MonitoringService.class), connection,
                Context.BIND_AUTO_CREATE);
    }

    private void disconnectFromService() {
        if (bound) {
            getActivity().unbindService(connection);
            bound = false;
        }
    }

    boolean bound = false;
    MonitoringService service;
    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.d(LOGTAG, "Connected to the monitoring service");
            service = ((ServiceBinder) binder).getService();
            viewUpdater.onStart(service.getStartedSince(), service.getLastData(), service.getLastLog(),
                    service.getLastRun());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(LOGTAG, "Disconnected from the monitoring service");
            bound = false;
        }

    };

    // Preferences

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (isServiceRunning()) {
            // restart
            stopMonitoringService();
            startMonitoringService();
        }
        
        setCustomDataLabels(prefUtils.getCustomDataLabels());
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromService();
    }

    // Alarm button

    OnClickListener onAlarmClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(LOGTAG, "On alarm button click");
            if (bound && service != null) {
                service.sendAlarmEvent(((ToggleButton) v).isChecked());
            }
        }
    };


}
