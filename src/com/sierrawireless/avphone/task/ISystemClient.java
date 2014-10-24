package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvSystem;

public interface ISystemClient {

    public abstract AvSystem getSystem(String serialNumber) throws IOException, AirVantageException;

    public abstract AvSystem createSystem(String serialNumber, String imei, String mqttPassword, String applicationUid)
            throws IOException, AirVantageException;

}