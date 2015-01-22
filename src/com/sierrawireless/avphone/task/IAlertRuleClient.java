package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AlertRule;

public interface IAlertRuleClient {

    AlertRule getAlertRule(String serialNumber) throws IOException, AirVantageException;

    AlertRule createAlertRule() throws IOException, AirVantageException;

    
}
