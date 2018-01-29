package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v1.AlertRule;

public interface IAlertRuleClient {

    AlertRule getAlertRule(String serialNumber, String Application) throws IOException, AirVantageException;

    void createAlertRule(String Application) throws IOException, AirVantageException;

    
}
