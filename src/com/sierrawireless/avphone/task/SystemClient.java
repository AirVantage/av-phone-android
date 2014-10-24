package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.MqttCommunication;
import net.airvantage.utils.AirVantageClient;
import net.airvantage.utils.Utils;

public class SystemClient implements ISystemClient {

    private AirVantageClient client;

    public SystemClient(AirVantageClient client) {
        this.client = client;
    }

    @Override
    public net.airvantage.model.System getSystem(String serialNumber) throws IOException, AirVantageException {
        List<net.airvantage.model.System> systems = client.getSystems(serialNumber);
        return Utils.first(systems);
    }

    @Override
    public net.airvantage.model.System createSystem(String serialNumber, String imei, String mqttPassword,
            String applicationUid) throws IOException, AirVantageException {
        net.airvantage.model.System system = new net.airvantage.model.System();

        // FIXME(pht) this will break if the gateway already exits.
        // We should look for gateway first, and use uid if it exists.
        net.airvantage.model.System.Gateway gateway = new net.airvantage.model.System.Gateway();
        gateway.serialNumber = serialNumber;
        gateway.imei = imei;
        system.gateway = gateway;

        system.state = "READY";

        Application application = new Application();
        application.uid = applicationUid;
        system.applications = Arrays.asList(application);

        MqttCommunication mqtt = new MqttCommunication();
        mqtt.password = mqttPassword;

        system.communication = new HashMap<String, MqttCommunication>();
        system.communication.put("mqtt", mqtt);

        system.type = "Android";

        return client.createSystem(system);
    }

}
