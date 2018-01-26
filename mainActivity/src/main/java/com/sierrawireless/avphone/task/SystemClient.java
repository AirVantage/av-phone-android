package com.sierrawireless.avphone.task;

import android.util.Log;

import com.sierrawireless.avphone.tools.Tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvSystem;
import net.airvantage.model.MqttCommunication;
import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.Utils;

public class SystemClient implements ISystemClient {
    private static final String TAG = "SystemClient";

    private AirVantageClient client;

    public SystemClient(AirVantageClient client) {
        this.client = client;
    }

    @Override
    public net.airvantage.model.AvSystem getSystem(final String serialNumber, String type) throws IOException, AirVantageException {
        List<net.airvantage.model.AvSystem> systems = client.getSystemsBySerialNumber(Tools.buildSerialNumber(serialNumber, type));

        return Utils.firstWhere(systems, AvSystem.hasSerialNumber(Tools.buildSerialNumber(serialNumber, type)));
    }

    @Override
    public net.airvantage.model.AvSystem createSystem(String serialNumber, String iccid, String type, String mqttPassword,
            String applicationUid, String deviceName, String userName, String imei) throws IOException, AirVantageException {
        net.airvantage.model.AvSystem system = new net.airvantage.model.AvSystem();

        Boolean exist = client.getGateway((serialNumber + "-ANDROID-" + type).toUpperCase());
        net.airvantage.model.AvSystem.Gateway gateway = new net.airvantage.model.AvSystem.Gateway();

        if (!exist) {
            gateway.serialNumber = (serialNumber + "-ANDROID-" + type).toUpperCase();
           // gateway.imei = imei + type;
            gateway.type = type;
        }else{
            gateway.serialNumber = (serialNumber + "-ANDROID-" + type).toUpperCase();
        }
        Log.d(TAG, "createSystem: Gateway exit " + exist);
        Log.d(TAG, "gateway is " + (serialNumber + "-ANDROID-" + type).toUpperCase() );
        system.name = deviceName + " de " + userName + "(" + type + ")";

        system.gateway = gateway;

        system.state = "READY";

        Application application = new Application();
        application.uid = applicationUid;
        system.applications = Arrays.asList(application);

        MqttCommunication mqtt = new MqttCommunication();
        mqtt.password = mqttPassword;

        system.communication = new HashMap<String, MqttCommunication>();
        system.communication.put("mqtt", mqtt);
        system.type = type;

        return client.createSystem(system);
    }

    @Override
    public void deleteSystem(net.airvantage.model.AvSystem system)throws IOException, AirVantageException {
        client.deleteSystem(system);
    }

}
