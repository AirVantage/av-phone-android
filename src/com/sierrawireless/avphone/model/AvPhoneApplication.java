package com.sierrawireless.avphone.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.Command;
import net.airvantage.model.Data;
import net.airvantage.model.Parameter;
import net.airvantage.model.Protocol;
import net.airvantage.model.Setting;

public class AvPhoneApplication {

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

	public static List<ApplicationData> createApplicationData(CustomData customData) {
		/*
		 * <data> <encoding type="MQTT"> <asset default-label="Android Phone"
		 * id="phone"> <setting default-label="RSSI" path="rssi" type="int"/>
		 * <setting default-label="Service type" path="service" type="string"/>
		 * <setting default-label="Operator" path="operator" type="string"/>
		 * <setting default-label="Latitude" path="latitude" type="double"/>
		 * <setting default-label="Longitude" path="longitude" type="double"/>
		 * <setting default-label="Battery level" path="batterylevel"
		 * type="double"/> <setting default-label="Bytes received"
		 * path="bytesreceived" type="double"/> <setting
		 * default-label="Bytes sent" path="bytessent" type="double"/> <setting
		 * default-label="Memory usage" path="memoryusage" type="double"/>
		 * <setting default-label="Running applications" path="runningapps"
		 * type="int"/> <setting default-label="Active Wi-Fi" path="activewifi"
		 * type="boolean"/>
		 * 
		 * <command default-label="Notify" path="notify" > <parameter
		 * id="message" type="string" /> </command> </asset> </encoding> </data>
		 */
		ApplicationData applicationData = new ApplicationData();
		applicationData.id = "0";
		applicationData.label = "AV Phone Demo";
		applicationData.encoding = "MQTT";
		applicationData.elementType = "node";
		applicationData.data = new ArrayList<net.airvantage.model.Data>();
		
		Data asset = new Data("phone", "Phone", "node");
		asset.data = new ArrayList<net.airvantage.model.Data>();
		
		asset.data.add(new Setting("phone.rssi", "RSSI", "int"));
		asset.data.add(new Setting("phone.service", "Service type", "string"));
		asset.data.add(new Setting("phone.operator", "Operator", "string"));
		asset.data.add(new Setting("phone.latitude", "Latitude", "double"));
		asset.data.add(new Setting("phone.longitude", "Longitude", "double"));
		asset.data.add(new Setting("phone.batterylevel", "Battery level", "double"));
		asset.data.add(new Setting("phone.bytesreceived", "Bytes received", "double"));
		asset.data.add(new Setting("phone.bytessent", "Bytes sent", "double"));
		asset.data.add(new Setting("phone.memoryusage", "Memory usage", "double"));
		asset.data.add(new Setting("phone.runningapps", "Running applications", "int"));
		asset.data.add(new Setting("phone.activewifi", "Active Wi-Fi", "boolean"));

		asset.data.add(new Setting("phone.custom1", customData.custom1, "int"));
		asset.data.add(new Setting("phone.custom2", customData.custom2, "int"));
		asset.data.add(new Setting("phone.custom3", customData.custom3, "boolean"));
		
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

}
