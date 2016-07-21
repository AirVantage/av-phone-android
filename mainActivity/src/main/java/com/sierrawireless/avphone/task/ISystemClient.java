package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvSystem;

public interface ISystemClient {

    AvSystem getSystem(String serialNumber) throws IOException, AirVantageException;

    AvSystem createSystem(String serialNumber, String imei, String type, String mqttPassword, String applicationUid)
            throws IOException, AirVantageException;

}
