package net.airvantage.model;

import java.util.Collection;
import java.util.Map;


public class AvSystem {
    public String uid;
    public String name;
    public String commStatus;
    public Long lastCommDate;
    public String type;
    public String state;
    public String activityState;
    public Long lastSyncDate;
    public String syncStatus;
    public Gateway gateway;
    public Data data;
    public Collection<Application> applications;
    public Map<String, MqttCommunication> communication;

    public static class Data {
        public Double rssi;
        public String rssiLevel;
        public String networkServiceType;
        public Double latitude;
        public Double longitude;
    }

    public static class Gateway {
        public String uid;
        public String imei;
        public String macAddress;
        public String serialNumber;
        public String type;
    }

}
