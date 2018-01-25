package com.sierrawireless.avphone.service;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.Gson;
import com.sierrawireless.avphone.DeviceInfo;
import com.sierrawireless.avphone.ObjectsManager;
import com.sierrawireless.avphone.model.AvPhoneData;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;

public class MqttPushClient {
    private static final String TAG = "MqttPushClient";

    private MqttClient client;
    private MqttConnectOptions opt;

    private Gson gson = new Gson();

    private ObjectsManager objectsManager;

    @SuppressLint("DefaultLocale")
    public MqttPushClient(String clientId, String password, String serverHost, MqttCallback callback)
            throws MqttException {

        DeviceInfo.generateSerial("", "");
        Log.d(TAG, "new client: " + clientId + " - " + password + " - " + serverHost);

        this.client = new MqttClient("tcp://" + serverHost + ":1883", MqttClient.generateClientId(),
                new MemoryPersistence());
        client.setCallback(callback);

        this.opt = new MqttConnectOptions();
        opt.setUserName(clientId.toUpperCase());
        opt.setPassword(password.toCharArray());
        opt.setKeepAliveInterval(30);
        objectsManager = ObjectsManager.getInstance();
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void connect() throws MqttSecurityException, MqttException {
        Log.d(TAG, "connecting");
        client.connect(opt);
    }

    public void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    public void push(NewData data) throws MqttException {
        if (client.isConnected()) {
            Log.i(TAG, "Pushing data to the server : " + data);
            String message = this.convertToJson(data);

            MqttMessage msg = null;
            try {
                msg = new MqttMessage(message.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // won't happen, UTF-8 is available
            }
            msg.setQos(0);

            this.client.publish(opt.getUserName() + "/messages/json", msg);
        }
    }

    private String convertToJson(NewData data) {
        long timestamp = System.currentTimeMillis();

        Map<String, List<DataValue>> values = new HashMap<String, List<DataValue>>();

        if (data.getRssi() != null) {
           // values.put(AvPhoneData.RSSI, Collections.singletonList(new DataValue(timestamp, data.getRssi())));
            values.put("_RSSI", Collections.singletonList(new DataValue(timestamp, data.getRssi())));
        }

        if (data.getRsrp() != null) {
         //   values.put(AvPhoneData.RSRP, Collections.singletonList(new DataValue(timestamp, data.getRsrp())));
            values.put("_RSRP", Collections.singletonList(new DataValue(timestamp, data.getRsrp())));
        }

        //if (data.getBatteryLevel() != null) {
        //    values.put(AvPhoneData.BATTERY,
        //            Collections.singletonList(new DataValue(timestamp, data.getBatteryLevel())));
       // }

        if (data.getOperator() != null) {
            values.put("_NETWORK_OPERATOR", Collections.singletonList(new DataValue(timestamp, data.getOperator())));
        }

//        if (data.getImei() != null) {
 //           values.put(AvPhoneData.IMEI, Collections.singletonList(new DataValue(timestamp, data.getImei())));
 //       }

        if (data.getNetworkType() != null) {
            //values.put(AvPhoneData.SERVICE, Collections.singletonList(new DataValue(timestamp, data.getNetworkType())));
            // hack for data mapping
            values.put("_NETWORK_SERVICE_TYPE",
                    Collections.singletonList(new DataValue(timestamp, data.getNetworkType())));
        }

        if (data.getLatitude() != null) {
            //values.put(AvPhoneData.LAT, Collections.singletonList(new DataValue(timestamp, data.getLatitude())));
            // hack for data mapping
            values.put("_LATITUDE", Collections.singletonList(new DataValue(timestamp, data.getLatitude())));
        }

        if (data.getLongitude() != null) {
            //values.put(AvPhoneData.LONG, Collections.singletonList(new DataValue(timestamp, data.getLongitude())));
            // hack for data mapping
            values.put("_LONGITUDE", Collections.singletonList(new DataValue(timestamp, data.getLongitude())));
        }

        if (data.getBytesReceived() != null) {
            // hack for data mapping
         //   values.put(AvPhoneData.BYTES_RECEIVED,
         //           Collections.singletonList(new DataValue(timestamp, data.getBytesReceived())));
            values.put("_BYTES_RECEIVED", Collections.singletonList(new DataValue(timestamp, data.getBytesReceived())));
        }

        if (data.getBytesSent() != null) {
            // hack for data mapping
           // values.put(AvPhoneData.BYTES_SENT, Collections.singletonList(new DataValue(timestamp, data.getBytesSent())));
            values.put("_BYTES_SENT", Collections.singletonList(new DataValue(timestamp, data.getBytesSent())));
        }

       // if (data.isWifiActive() != null) {
       //     values.put(AvPhoneData.ACTIVE_WIFI, Collections.singletonList(new DataValue(timestamp, data.isWifiActive())));
       // }

        //if (data.getRunningApps() != null) {
        //    values.put(AvPhoneData.RUNNING_APPS, Collections.singletonList(new DataValue(timestamp, data.getRunningApps())));
       // }

        //if (data.getMemoryUsage() != null) {
        //    values.put(AvPhoneData.MEMORY_USAGE, Collections.singletonList(new DataValue(timestamp, data.getMemoryUsage())));
       // }

       // if (data.getAndroidVersion() != null) {
       //     values.put(AvPhoneData.ANDROID_VERSION,
        //            Collections.singletonList(new DataValue(timestamp, data.getAndroidVersion())));
            // values.put("_FIRMWARE_VERSION", Collections.singletonList(new DataValue(timestamp,
            // data.getAndroidVersion())));
        //}

        if (data.isAlarmActivated() != null) {
            values.put(AvPhoneData.ALARM,
                    Collections.singletonList(new DataValue(timestamp, data.isAlarmActivated())));
        }
        AvPhoneObject object = objectsManager.getCurrentObject();
        for (AvPhoneObjectData ldata:object.datas) {
            if (ldata.isInteger()) {
                values.put(AvPhoneData.CUSTOM + ldata.label, Collections.singletonList(new DataValue(timestamp, ldata.current)));
            }else{
                values.put(AvPhoneData.CUSTOM + ldata.label, Collections.singletonList(new DataValue(timestamp, ldata.defaults)));
            }
        }

        return gson.toJson(Collections.singletonList(values));
    }

    class DataValue {

        DataValue(long timestamp, Object value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        long timestamp;
        Object value;
    }

}
