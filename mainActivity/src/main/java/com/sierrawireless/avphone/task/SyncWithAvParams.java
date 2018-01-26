package com.sierrawireless.avphone.task;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class SyncWithAvParams {
    public String deviceId;
    public String imei;
    public String mqttPassword;
    public String iccid;
    public String deviceName;
    public CustomDataLabels customData;
    public MainActivity activity;
    public boolean delete = false;
}
