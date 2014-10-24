package com.sierrawireless.avphone.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.airvantage.model.AlertRule;
import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.Command;
import net.airvantage.model.Condition;
import net.airvantage.model.Data;
import net.airvantage.model.Parameter;
import net.airvantage.model.Protocol;
import net.airvantage.model.Setting;
import net.airvantage.model.Variable;

public class AvPhoneApplication {

    private static final String PHONE_ALARM_DATA_ID = "phone.alarm";

    public static Application createApplication(String serialNumber) {
        Application application = new Application();
        application.name = AvPhoneApplication.appName(serialNumber);
        application.type = AvPhoneApplication.appType(serialNumber);
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
        /*
         * <data> <encoding type="MQTT"> <asset default-label="Android Phone" id="phone"> <setting default-label="RSSI"
         * path="rssi" type="int"/> <setting default-label="Service type" path="service" type="string"/> <setting
         * default-label="Operator" path="operator" type="string"/> <setting default-label="Latitude" path="latitude"
         * type="double"/> <setting default-label="Longitude" path="longitude" type="double"/> <setting
         * default-label="Battery level" path="batterylevel" type="double"/> <setting default-label="Bytes received"
         * path="bytesreceived" type="double"/> <setting default-label="Bytes sent" path="bytessent" type="double"/>
         * <setting default-label="Memory usage" path="memoryusage" type="double"/> <setting
         * default-label="Running applications" path="runningapps" type="int"/> <setting default-label="Active Wi-Fi"
         * path="activewifi" type="boolean"/>
         * 
         * <command default-label="Notify" path="notify" > <parameter id="message" type="string" /> </command> </asset>
         * </encoding> </data>
         */
        ApplicationData applicationData = new ApplicationData();
        applicationData.id = "0";
        applicationData.label = "AV Phone Demo";
        applicationData.encoding = "MQTT";
        applicationData.elementType = "node";
        applicationData.data = new ArrayList<net.airvantage.model.Data>();

        Data asset = new Data("phone", "Phone", "node");
        asset.data = new ArrayList<net.airvantage.model.Data>();

        asset.data.add(new Variable("phone.rssi", "RSSI", "int"));
        asset.data.add(new Variable("phone.service", "Service type", "string"));
        asset.data.add(new Variable("phone.operator", "Operator", "string"));
        asset.data.add(new Variable("phone.latitude", "Latitude", "double"));
        asset.data.add(new Variable("phone.longitude", "Longitude", "double"));
        asset.data.add(new Variable("phone.batterylevel", "Battery level", "double"));
        asset.data.add(new Variable("phone.bytesreceived", "Bytes received", "double"));
        asset.data.add(new Variable("phone.bytessent", "Bytes sent", "double"));
        asset.data.add(new Variable("phone.memoryusage", "Memory usage", "double"));
        asset.data.add(new Variable("phone.runningapps", "Running applications", "int"));
        asset.data.add(new Variable("phone.activewifi", "Active Wi-Fi", "boolean"));
        asset.data.add(new Variable(PHONE_ALARM_DATA_ID, "Active alarm", "boolean"));

        asset.data.add(new Variable("phone.custom.up.1", customData.customUp1Label, "int"));
        asset.data.add(new Variable("phone.custom.up.2", customData.customUp2Label, "int"));
        asset.data.add(new Variable("phone.custom.down.1", customData.customDown1Label, "int"));
        asset.data.add(new Variable("phone.custom.down.2", customData.customDown2Label, "int"));
        asset.data.add(new Variable("phone.custom.str.1", customData.customStr1Label, "string"));
        asset.data.add(new Variable("phone.custom.str.2", customData.customStr2Label, "string"));

        Command c = new Command("phone.notify", "Notify");
        Parameter p = new Parameter("message", "string");
        c.parameters = Arrays.asList(p);

        asset.data.add(c);

        applicationData.data.add(asset);

        return Arrays.asList(applicationData);

    }

    public static String appName(String serialNumber) {
        return "av_phone_demo_" + serialNumber;
    }

    public static String appType(String serialNumber) {
        return "av.phone.demo." + serialNumber;
    }

    public static String alertRuleName(String serialNumber) {
        return "AV Phone rule " + serialNumber;
    }
    
    public static AlertRule createAlertRule(String serialNumber, String systemUid, String applicationUid) {
        AlertRule rule = new AlertRule();
        
        rule.active = true;
        rule.name = alertRuleName(serialNumber);
        rule.eventType = "event.system.incoming.communication";
        
        Condition sysCondition = new Condition();
        sysCondition.eventProperty = "system.uid";
        sysCondition.operator = "EQUALS";
        sysCondition.value = systemUid;
        
        Condition alarmCondition = new Condition();
        alarmCondition.eventProperty = "communication.data.value";
        alarmCondition.eventPropertyKey = PHONE_ALARM_DATA_ID;
        alarmCondition.operator = "EQUALS";
        alarmCondition.value = "true";
        
        rule.conditions = Arrays.asList(sysCondition, alarmCondition);
        
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("condition_1_application", applicationUid);
        
        rule.metadata = metadata;
        
        return rule;
    }

}
