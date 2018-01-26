package com.sierrawireless.avphone.service;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.Gson;
import com.sierrawireless.avphone.DeviceInfo;
import com.sierrawireless.avphone.ObjectsManager;
import com.sierrawireless.avphone.model.AvPhoneData;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MqttPushClient {
    private static final String TAG = "MqttPushClient";

    private MqttClient client;
    private MqttConnectOptions opt;

    private Gson gson = new Gson();

    private ObjectsManager objectsManager;

    @SuppressLint("DefaultLocale")
    MqttPushClient(String clientId, String password, String serverHost, MqttCallback callback)
            throws MqttException {

        DeviceInfo.generateSerial("");
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

    boolean isConnected() {
        return client.isConnected();
    }

    void connect() throws MqttException {
        Log.d(TAG, "connecting");
        client.connect(opt);
    }

    void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    void push(NewData data) throws MqttException {
        if (client.isConnected()) {
            Log.i(TAG, "Pushing data to the server : " + data);
            String message = this.convertToJson(data);
            Log.i(TAG, "push: message " + message);

            MqttMessage msg = null;
            try {
                msg = new MqttMessage(message.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // won't happen, UTF-8 is available
            }
            if (msg != null) {
                msg.setQos(0);
            }
            this.client.publish(opt.getUserName() + "/messages/json", msg);
        }
    }

    private String convertToJson(NewData data) {
        long timestamp = System.currentTimeMillis();

        Map<String, List<DataValue>> values = new HashMap<>();

        if (data.getRssi() != null) {
           values.put("_RSSI", Collections.singletonList(new DataValue(timestamp, data.getRssi())));
        }

        if (data.getRsrp() != null) {
             values.put("_RSRP", Collections.singletonList(new DataValue(timestamp, data.getRsrp())));
        }

        if (data.getOperator() != null) {
            values.put("_NETWORK_OPERATOR", Collections.singletonList(new DataValue(timestamp, data.getOperator())));
        }


        if (data.getNetworkType() != null) {
            values.put("_NETWORK_SERVICE_TYPE",
                    Collections.singletonList(new DataValue(timestamp, data.getNetworkType())));
        }

        if (data.getLatitude() != null) {
            values.put("_LATITUDE", Collections.singletonList(new DataValue(timestamp, data.getLatitude())));
        }

        if (data.getLongitude() != null) {
            values.put("_LONGITUDE", Collections.singletonList(new DataValue(timestamp, data.getLongitude())));
        }

        if (data.getBytesReceived() != null) {
            values.put("_BYTES_RECEIVED", Collections.singletonList(new DataValue(timestamp, data.getBytesReceived())));
        }

        if (data.getBytesSent() != null) {
            values.put("_BYTES_SENT", Collections.singletonList(new DataValue(timestamp, data.getBytesSent())));
        }


        if (data.isAlarmActivated() != null) {
            values.put(AvPhoneData.ALARM,
                    Collections.singletonList(new DataValue(timestamp, data.isAlarmActivated())));
        }
        AvPhoneObject object = objectsManager.getCurrentObject();
        Integer pos = 1;
        for (AvPhoneObjectData ldata:object.datas) {
            if (ldata.isInteger()) {
                values.put(AvPhoneData.CUSTOM + pos.toString(), Collections.singletonList(new DataValue(timestamp, ldata.current)));
            }else{
                values.put(AvPhoneData.CUSTOM + pos.toString(), Collections.singletonList(new DataValue(timestamp, ldata.defaults)));
            }
            pos++;
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
