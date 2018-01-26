package com.sierrawireless.avphone;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.sierrawireless.avphone.adapter.RunListViewAdapter;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.NewData;
import com.sierrawireless.avphone.tools.Tools;

/**
 * A component in charge of listening for service events (new data, logs) and updating the view accordingly.
 */
public class DataViewUpdater extends BroadcastReceiver {

    private DateFormat hourFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRENCH);

    private final View view;
    private MainActivity activity;
    private ObjectsManager objectsManager;

    public DataViewUpdater(View view, MainActivity activity) {
        this.view = view;
        this.activity = activity;
        objectsManager = ObjectsManager.getInstance();
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
        //view.findViewById(R.id.alarm_switch).setEnabled(true);
    }

    public void onStop() {
        this.setStartedSince(null);

        // deactivate alarm button
        //view.findViewById(R.id.alarm_switch).setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void setLogMessage(String log, Long timestamp) {
        TextView logView = findView(R.id.service_log);
        if (log != null) {
            logView.setText(hourFormat.format(timestamp != null ? new Date(timestamp) : new Date()) + " - " + log);
            logView.setVisibility(View.VISIBLE);
        } else {
            logView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
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

        ListView phoneListView = (ListView)view.findViewById(R.id.phoneListView);
        ArrayList<HashMap<String, String>> listPhone = new ArrayList<>();

        String Rssi ;

        HashMap<String, String> temp;

        if (data.getRssi() != null) {
            Rssi = data.getRssi() + " dBm (RSSI)";
        } else if (data.getRsrp() != null) {
            Rssi = data.getRsrp() + " dBm (RSRP)";
        }else{
            Rssi = "Unknown";
        }
        temp = new HashMap<>();
        temp.put(Tools.NAME, "RSSI");
        temp.put(Tools.VALUE, Rssi);
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Operator");
        if (data.getOperator() == null) {
            temp.put(Tools.VALUE, "");
        }else{
            temp.put(Tools.VALUE, data.getOperator());
        }
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Bytes Sent");
        if (data.getBytesSent() == null) {
            temp.put(Tools.VALUE, "0 Mo");
        }else{
            temp.put(Tools.VALUE, ((data.getBytesSent()) / (1024F * 1024F)) + " Mo");
        }
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Bytes Received");
        if (data.getBytesReceived() == null) {
            temp.put(Tools.VALUE, "0 Mo");
        }else{
            temp.put(Tools.VALUE, ((data.getBytesReceived()) / (1024F * 1024F)) + " Mo");
        }
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Network Type");
        if (data.getNetworkType() == null) {
            temp.put(Tools.VALUE, "");
        }else{
            temp.put(Tools.VALUE, data.getNetworkType());
        }
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Latitude");
        if (data.getLatitude() == null) {
            temp.put(Tools.VALUE, "");
        }else{
            temp.put(Tools.VALUE, data.getLatitude().toString());
        }
        listPhone.add(temp);

        temp = new HashMap<>();
        temp.put(Tools.NAME, "Longitude");
        if (data.getLongitude() == null) {
            temp.put(Tools.VALUE, "");
        }else{
            temp.put(Tools.VALUE, data.getLongitude().toString());
        }
        listPhone.add(temp);
        RunListViewAdapter adapter = new RunListViewAdapter(activity, listPhone);
        phoneListView.setAdapter(adapter);
        phoneListView.invalidateViews();


        setCustomDataValues();
        
    }

    private void setCustomDataValues() {

        ListView objectListView = (ListView)view.findViewById(R.id.objectLstView);
        objectsManager = ObjectsManager.getInstance();
        AvPhoneObject object = objectsManager.getCurrentObject();
        HashMap<String,String> temp;
        ArrayList<HashMap<String, String>> listObject = new ArrayList<>();
        for (AvPhoneObjectData ldata : object.datas) {
            temp = new HashMap<>();
            temp.put(Tools.NAME, ldata.name);
            if (ldata.isInteger()) {
                temp.put(Tools.VALUE, ldata.current.toString());
            }else{
                temp.put(Tools.VALUE, ldata.defaults);
            }
            listObject.add(temp);
        }
        RunListViewAdapter adapter = new RunListViewAdapter(activity, listObject);
        objectListView.setAdapter(adapter);
        objectListView.invalidateViews();
    }

    private TextView findView(int id) {
        return (TextView) view.findViewById(id);
    }

}
