package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v1.AlertRule;

public interface IAlertRuleClient {

    AlertRule getAlertRule(String serialNumber) throws IOException, AirVantageException;

    void createAlertRule() throws IOException, AirVantageException;

    
}
