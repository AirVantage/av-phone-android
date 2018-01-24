package com.sierrawireless.avphone.task;

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

    private AirVantageClient client;

    public SystemClient(AirVantageClient client) {
        this.client = client;
    }

    @Override
    public net.airvantage.model.AvSystem getSystem(final String serialNumber) throws IOException, AirVantageException {
        List<net.airvantage.model.AvSystem> systems = client.getSystemsBySerialNumber(serialNumber);

        return Utils.firstWhere(systems, AvSystem.hasSerialNumber(serialNumber));
    }

    @Override
    public net.airvantage.model.AvSystem createSystem(String serialNumber, String iccid, String type, String mqttPassword,
            String applicationUid, String deviceName, String userName, String imei) throws IOException, AirVantageException {
        net.airvantage.model.AvSystem system = new net.airvantage.model.AvSystem();

        client.getGateway(serialNumber + "-" + type);

        net.airvantage.model.AvSystem.Gateway gateway = new net.airvantage.model.AvSystem.Gateway();
        gateway.serialNumber = serialNumber + "-ANDROID-" + type;
        gateway.imei = imei;
        gateway.type = type;
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

}
