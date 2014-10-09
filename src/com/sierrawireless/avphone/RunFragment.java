package com.sierrawireless.avphone;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MonitoringService;
import com.sierrawireless.avphone.service.MonitoringService.ServiceBinder;
import com.sierrawireless.avphone.service.NewData;

public class RunFragment extends Fragment implements OnSharedPreferenceChangeListener {

    private DataViewUpdater viewUpdater;

    private SharedPreferences prefs;

    private AlarmManager alarmManager;

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_run, container, false);

        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        viewUpdater = new DataViewUpdater(view);

        // register service listener
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(NewData.NEW_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(viewUpdater,
                new IntentFilter(LogMessage.LOG_EVENT));

        // Preferences
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        boolean isServiceRunning = isServiceRunning();

        Switch serviceSwitch = (Switch) view.findViewById(R.id.service_switch);
        serviceSwitch.setChecked(isServiceRunning);

        if (isServiceRunning) {
            connectToService();
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

        String serverHost = prefs.getString(this.getString(R.string.pref_server_key), null);
        String password = prefs.getString(this.getString(R.string.pref_password_key), null);
        String period = prefs.getString(this.getString(R.string.pref_period_key), null);

        if (password == null || password.isEmpty() || serverHost == null || serverHost.isEmpty()) {
            new AlertDialog.Builder(getActivity()).setTitle(R.string.invalid_prefs).setMessage(R.string.prefs_missing)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    }).show();
            Switch serviceSwitch = (Switch) view.findViewById(R.id.service_switch);
            serviceSwitch.setChecked(false);
            return;
        }
        intent.putExtra(MonitoringService.SERVER_HOST, serverHost);
        intent.putExtra(MonitoringService.PASSWORD, password);

        PendingIntent pendingIntent = PendingIntent.getService(getActivity(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // registering our pending intent with alarm manager
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, Integer.valueOf(period) * 60 * 1000,
                pendingIntent);

        this.connectToService();
    }

    private void stopMonitoringService() {

        Intent intent = new Intent(getActivity(), MonitoringService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getActivity(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);
        getActivity().stopService(intent);

        disconnectFromService();

        viewUpdater.setStartedSince(null);
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
    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            MonitoringService service = ((ServiceBinder) binder).getService();
            viewUpdater.setStartedSince(service.getStartedSince());
            viewUpdater.setNewData(service.getLastData());
            viewUpdater.setLogMessage(service.getLastLog(), service.getLastRun());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromService();
    }

}
