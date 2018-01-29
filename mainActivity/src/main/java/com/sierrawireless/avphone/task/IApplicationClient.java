package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.ArrayList;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvSystem;

import com.sierrawireless.avphone.model.AvPhoneObjectData;

public interface IApplicationClient {

    Application ensureApplicationExists() throws IOException, AirVantageException;

    void setApplicationData(String applicationUid, ArrayList<AvPhoneObjectData> customData, String object) throws IOException, AirVantageException;

    Application createApplication() throws IOException, AirVantageException;

    void setApplicationCommunication(String applicationUid) throws IOException, AirVantageException;

    void addApplication(AvSystem system, Application application) throws IOException, AirVantageException;

}