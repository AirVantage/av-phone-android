package com.sierrawireless.avphone.model;

import android.util.Log;

import com.sierrawireless.avphone.ObjectsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.Command;
import net.airvantage.model.alert.v1.Condition;
import net.airvantage.model.Data;
import net.airvantage.model.Parameter;
import net.airvantage.model.Protocol;
import net.airvantage.model.Variable;

public class AvPhoneApplication {

    private static final String TAG = "AvPhoneApplication";
    public static final String ALERT_RULE_NAME = "AV Phone raised an alert";
    static ObjectsManager objectsManager;

    public static Application createApplication(final String userName) {
        Application application = new Application();
        application.name = AvPhoneApplication.appName(userName);
        application.type = AvPhoneApplication.appType(userName);
        application.revision = "0.0.0";
        return application;
    }

    public static List<Protocol> createProtocols() {
        Protocol mqtt = new Protocol();
        mqtt.type = "MQTT";
        mqtt.commIdType = "SERIAL";
        return Arrays.asList(mqtt);
    }

    public static List<ApplicationData> createApplicationData(ArrayList<AvPhoneObjectData> customData) {

        Log.d(TAG, "createApplicationData: ************applicationData called");
        // <data>
        // <encoding type="MQTT">
        // <asset default-label="Android Phone" id="phone">
        // <setting default-label="RSSI" path="rssi" type="int"/>
        // <setting default-label="Service type" path="service" type="string"/>
        // <setting default-label="Operator" path="operator" type="string"/>
        // <setting default-label="Latitude" path="latitude"type="double"/>
        // <setting default-label="Longitude" path="longitude" type="double"/>
        // <setting default-label="Battery level" path="batterylevel" type="double"/>
        // <setting default-label="Bytes received"path="bytesreceived" type="double"/>
        // <setting default-label= "Bytes sent" path="bytessent" type="double"/>
        // <setting default-label= "Memory usage" path="memoryusage" type="double"/>
        // <setting default-label="Running applications" path="runningapps" type="int"/>
        // <setting default-label= "Active Wi-Fi" path="activewifi" type="boolean"/>
        //
        // <command default-label="Notify" path="notify" > <parameter id="message" type="string" /></command>
        // </asset>
        // </encoding>
        // </data>

        ApplicationData applicationData = new ApplicationData();
        applicationData.id = "0";
        applicationData.label = "AV Phone Demo";
        applicationData.encoding = "MQTT";
        applicationData.elementType = "node";
        applicationData.data = new ArrayList<net.airvantage.model.Data>();

        Data asset = new Data("phone", "Phone", "node");
        asset.data = new ArrayList<net.airvantage.model.Data>();
        
        asset.data.add(new Variable(AvPhoneData.ALARM, "Active alarm", "boolean"));

        Integer pos = 1;
        for (AvPhoneObjectData data:customData) {
            String type;

            if (data.isInteger()) {
                type = "int";
            } else {
                type = "string";
            }
            asset.data.add(new Variable(AvPhoneData.CUSTOM + pos.toString(), data.name, type));
            pos ++;
        }

        Command c = new Command(AvPhoneData.NOTIFY, "Notify");
        Parameter p = new Parameter("message", "string");
        c.parameters = Arrays.asList(p);

        asset.data.add(c);

        applicationData.data.add(asset);

        return Arrays.asList(applicationData);

    }

    public static String appName(final String userName) {
        objectsManager = ObjectsManager.getInstance();

        return "av_phone_" + objectsManager.getSavecObject().name + "_" + userName;
    }

    public static String appType(final String userName) {
        objectsManager = ObjectsManager.getInstance();
        return "av.phone.demo." + objectsManager.getSavecObject().name  + userName;
    }

    public static AlertRule createAlertRule() {
        AlertRule rule = new AlertRule();

        rule.active = true;
        rule.name = ALERT_RULE_NAME;
        rule.eventType = "event.system.incoming.communication";

        Condition alarmCondition = new Condition();
        alarmCondition.eventProperty = "communication.data.value";
        alarmCondition.eventPropertyKey = AvPhoneData.ALARM;
        alarmCondition.operator = "EQUALS";
        alarmCondition.value = "true";

        rule.conditions = Arrays.asList(alarmCondition);

        return rule;
    }

}
