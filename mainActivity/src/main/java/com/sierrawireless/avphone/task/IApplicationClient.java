package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvSystem;

import com.sierrawireless.avphone.model.CustomDataLabels;

public interface IApplicationClient {

    Application ensureApplicationExists() throws IOException, AirVantageException;

    void setApplicationData(String applicationUid, CustomDataLabels customData) throws IOException, AirVantageException;

    Application createApplication() throws IOException, AirVantageException;

    void setApplicationCommunication(String applicationUid) throws IOException, AirVantageException;

    void addApplication(AvSystem system, Application application) throws IOException, AirVantageException;

}