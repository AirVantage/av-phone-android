package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;

public interface ISystemClient {

	public abstract net.airvantage.model.System getSystem(String serialNumber) throws IOException, AirVantageException;

	public abstract net.airvantage.model.System createSystem(String serialNumber, String mqttPassword,
			String applicationUid) throws IOException, AirVantageException;

}