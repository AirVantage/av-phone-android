package com.sierrawireless.avphone.task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.service.LogMessage;
import com.sierrawireless.avphone.service.MqttPushClient;
import com.sierrawireless.avphone.service.NewData;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvError;
import net.airvantage.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JDamiano on 29/01/2018.
 */

public class SendDataTask extends AsyncTask<SendDataParams, Void, SendDataResult> {


    private List<SendDataListener> syncListeners = new ArrayList<>();




    public void addProgressListener(SendDataListener listener) {
        this.syncListeners.add(listener);
    }

    protected SendDataResult doInBackground(SendDataParams... params) {
        String lastLog;

        final SendDataParams sendParams = params[0];
        final MqttPushClient client = sendParams.client;
        final NewData data = sendParams.data;
        final boolean alarm = sendParams.alarm;
        final Context context = sendParams.context;
        final boolean value = sendParams.value;
        try {
            if (!client.isConnected()) {
                client.connect();
            }

            if (!alarm)
                // dispatch new data event to update the activity UI
                LocalBroadcastManager.getInstance(context).sendBroadcast(data);

            client.push(data);
            lastLog = data.size() + " data pushed to the server";
            if (!alarm)
                lastLog = data.size() + " data pushed to the server";
            else
                if (value)
                    lastLog = "Alarm on sent to server";
                else
                    lastLog = "Alarm off sent to server";

            LocalBroadcastManager.getInstance(context).sendBroadcast(new LogMessage(lastLog, alarm));
            return new SendDataResult(lastLog);
        }
        catch (Exception e) {
            Crashlytics.logException(e);
            lastLog = "ERROR: " + e.getMessage();
            LocalBroadcastManager.getInstance(context).sendBroadcast(new LogMessage(lastLog, alarm));
            return new SendDataResult(lastLog, true);
        }



    }

    @Override
    protected void onPostExecute(SendDataResult result) {
        super.onPostExecute(result);
        for (SendDataListener listener : syncListeners) {
            listener.onSend(result);
        }
    }

}
