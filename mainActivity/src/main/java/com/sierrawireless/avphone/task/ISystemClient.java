package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvSystem;

public interface ISystemClient {

    AvSystem getSystem(String serialNumber, String type) throws IOException, AirVantageException;

    AvSystem createSystem(String serialNumber, String iccid, String type, String mqttPassword, String applicationUid, String deviceName, String userName, String imei)
            throws IOException, AirVantageException;

}
