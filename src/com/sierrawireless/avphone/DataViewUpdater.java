package com.sierrawireless.avphone;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.NewData;

/**
 * A component in charge of listening for service events (new data, logs) and updating the view accordingly.
 */
public class DataViewUpdater extends BroadcastReceiver {

    private DateFormat hourFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRENCH);

    private final View view;

    public DataViewUpdater(View view) {
        this.view = view;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent instanceof NewData) {
            setNewData((NewData) intent);
        } else if (intent instanceof LogMessage) {
            setLogMessage(((LogMessage) intent).getMessage(), System.currentTimeMillis());
        }
    }

    public void onStart(Long startedSince, NewData lastData, String logMsg, Long lastRun) {
        this.setStartedSince(startedSince);
        this.setNewData(lastData);
        this.setLogMessage(logMsg, lastRun);

        // activate alarm button
        view.findViewById(R.id.alarm_switch).setEnabled(true);
    }

    public void onStop() {
        this.setStartedSince(null);

        // deactivate alarm button
        view.findViewById(R.id.alarm_switch).setEnabled(false);
    }

    private void setLogMessage(String log, Long timestamp) {
        TextView logView = findView(R.id.service_log);
        if (log != null) {
            logView.setText(hourFormat.format(timestamp != null ? new Date(timestamp) : new Date()) + " - " + log);
            logView.setVisibility(View.VISIBLE);
        } else {
            logView.setVisibility(View.GONE);
        }
    }

    private void setStartedSince(Long startedSince) {
        TextView startedTextView = findView(R.id.started_since);
        if (startedSince != null) {
            startedTextView.setText(view.getContext().getString(R.string.started_since) + " "
                    + new SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRENCH).format(new Date(startedSince)));
            startedTextView.setVisibility(View.VISIBLE);
        } else {
            startedTextView.setVisibility(View.GONE);
        }
    }

    private void setNewData(NewData data) {
        
        if (data.getRssi() != null) {
            findView(R.id.signal_strength_value).setText(data.getRssi() + " dBm (RSSI)");
        } else if (data.getRsrp() != null) {
            findView(R.id.signal_strength_value).setText(data.getRsrp() + " dBm (RSRP)");
        }

        if (data.getBatteryLevel() != null) {
            findView(R.id.battery_value).setText((int) (data.getBatteryLevel() * 100) + "%");
        }

        if (data.getOperator() != null) {
            findView(R.id.operator_value).setText(data.getOperator());
        }

        if (data.getImei() != null) {
            findView(R.id.imei_value).setText(data.getImei());
        }

        if (data.getNetworkType() != null) {
            findView(R.id.network_type_value).setText(data.getNetworkType());
        }

        if (data.getLatitude() != null && data.getLongitude() != null) {
            findView(R.id.latitude_value).setText(data.getLatitude().toString());
            findView(R.id.longitude_value).setText(data.getLongitude().toString());
        }

        if (data.getBytesReceived() != null) {
            findView(R.id.bytes_received_value).setText(((data.getBytesReceived()) / (1024F * 1024F)) + " Mo");
        }

        if (data.getBytesSent() != null) {
            findView(R.id.bytes_sent_value).setText(((data.getBytesSent()) / (1024F * 1024F)) + " Mo");
        }

        if (data.getMemoryUsage() != null) {
            findView(R.id.memory_usage_value).setText((int) (data.getMemoryUsage() * 100) + "%");
        }

        if (data.getRunningApps() != null) {
            findView(R.id.running_apps_value).setText(data.getRunningApps().toString());
        }

        if (data.isWifiActive() != null) {
            findView(R.id.active_wifi_value).setText(data.isWifiActive() ? "On" : "Off");
        }

        if (data.getAndroidVersion() != null) {
            findView(R.id.android_version_value).setText(data.getAndroidVersion());
        }

        if (data.isAlarmActivated() != null) {
            ((Switch) view.findViewById(R.id.alarm_switch)).setChecked(data.isAlarmActivated());
        }

        setCustomDataValues(data);
        
    }

    private void setCustomDataValues(NewData data) {
        if (data.getCustomIntUp1() != null) {
            TextView valueView = (TextView) view.findViewById(R.id.run_custom1_value);
            valueView.setText(String.valueOf(data.getCustomIntUp1()));
        }

        if (data.getCustomIntUp2() != null) {
            TextView valueView = (TextView) view.findViewById(R.id.run_custom2_value);
            valueView.setText(String.valueOf(data.getCustomIntUp2()));
        }

        if (data.getCustomIntDown1() != null) {
            TextView valueView = (TextView) view.findViewById(R.id.run_custom3_value);
            valueView.setText(String.valueOf(data.getCustomIntDown1()));
        }

        if (data.getCustomIntDown2() != null) {
            TextView valueView = (TextView) view.findViewById(R.id.run_custom4_value);
            valueView.setText(String.valueOf(data.getCustomIntDown2()));
        }

        if (data.getCustomStr1() != null) {
            TextView valueView = (TextView) view.findViewById(R.id.run_custom5_value);
            valueView.setText(String.valueOf(data.getCustomStr1()));
        }

        if (data.getCustomStr2() != null) {
            TextView valueView = (TextView) view.findViewById(R.id.run_custom6_value);
            valueView.setText(String.valueOf(data.getCustomStr2()));
        }
    }

    private TextView findView(int id) {
        return (TextView) view.findViewById(id);
    }

}
