package com.sierrawireless.avphone.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.Command;
import net.airvantage.model.Condition;
import net.airvantage.model.Data;
import net.airvantage.model.Parameter;
import net.airvantage.model.Protocol;
import net.airvantage.model.Variable;

public class AvPhoneApplication {

    public static final String ALERT_RULE_NAME = "AV Phone raised an alert";

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

    public static List<ApplicationData> createApplicationData(CustomDataLabels customData) {

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

        asset.data.add(new Variable(AvPhoneData.RSSI, "RSSI", "int"));
        asset.data.add(new Variable(AvPhoneData.RSRP, "RSRP", "int"));
        asset.data.add(new Variable(AvPhoneData.SERVICE, "Service type", "string"));
        asset.data.add(new Variable(AvPhoneData.OPERATOR, "Operator", "string"));
        asset.data.add(new Variable(AvPhoneData.IMEI, "IMEI", "string"));
        asset.data.add(new Variable(AvPhoneData.LAT, "Latitude", "double"));
        asset.data.add(new Variable(AvPhoneData.LONG, "Longitude", "double"));
        asset.data.add(new Variable(AvPhoneData.BATTERY, "Battery level", "double"));
        asset.data.add(new Variable(AvPhoneData.BYTES_RECEIVED, "Bytes received", "double"));
        asset.data.add(new Variable(AvPhoneData.BYTES_SENT, "Bytes sent", "double"));
        asset.data.add(new Variable(AvPhoneData.MEMORY_USAGE, "Memory usage", "double"));
        asset.data.add(new Variable(AvPhoneData.RUNNING_APPS, "Running applications", "int"));
        asset.data.add(new Variable(AvPhoneData.ACTIVE_WIFI, "Active Wi-Fi", "boolean"));
        asset.data.add(new Variable(AvPhoneData.ANDROID_VERSION, "Android Version", "string"));

        asset.data.add(new Variable(AvPhoneData.ALARM, "Active alarm", "boolean"));

        asset.data.add(new Variable(AvPhoneData.CUSTOM_1, customData.customUp1Label, "int"));
        asset.data.add(new Variable(AvPhoneData.CUSTOM_2, customData.customUp2Label, "int"));
        asset.data.add(new Variable(AvPhoneData.CUSTOM_3, customData.customDown1Label, "int"));
        asset.data.add(new Variable(AvPhoneData.CUSTOM_4, customData.customDown2Label, "int"));
        asset.data.add(new Variable(AvPhoneData.CUSTOM_5, customData.customStr1Label, "string"));
        asset.data.add(new Variable(AvPhoneData.CUSTOM_6, customData.customStr2Label, "string"));

        Command c = new Command(AvPhoneData.NOTIFY, "Notify");
        Parameter p = new Parameter("message", "string");
        c.parameters = Arrays.asList(p);

        asset.data.add(c);

        applicationData.data.add(asset);

        return Arrays.asList(applicationData);

    }

    public static String appName(final String userName) {
        return "av_phone_demo_" + userName;
    }

    public static String appType(final String userName) {
        return "av.phone.demo." + userName;
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
