package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;

import com.sierrawireless.avphone.model.CustomDataLabels;

public interface IApplicationClient {

    Application ensureApplicationExists(String serialNumber) throws IOException, AirVantageException;

    void setApplicationData(String applicationUid, CustomDataLabels customData) throws IOException, AirVantageException;

    Application createApplication(String serialNumber) throws IOException, AirVantageException;

    void setApplicationCommunication(String applicationUid) throws IOException, AirVantageException;

}