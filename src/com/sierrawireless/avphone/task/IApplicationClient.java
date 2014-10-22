package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;

import com.sierrawireless.avphone.model.CustomDataLabels;

public interface IApplicationClient {

	public abstract Application ensureApplicationExists(String serialNumber) throws IOException, AirVantageException;

	public abstract void setApplicationData(String applicationUid, CustomDataLabels customData) throws IOException,
			AirVantageException;

	public abstract Application createApplication(String serialNumber) throws IOException, AirVantageException;

	public abstract void setApplicationCommunication(String applicationUid) throws IOException, AirVantageException;

}