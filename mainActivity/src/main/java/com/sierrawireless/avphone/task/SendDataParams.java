package com.sierrawireless.avphone.task;

import android.content.Context;

import com.sierrawireless.avphone.service.MqttPushClient;
import com.sierrawireless.avphone.service.NewData;

public class SendDataParams {
    public MqttPushClient client;
    public NewData data;
    public boolean alarm;
    public boolean value;
    public Context context;

}
