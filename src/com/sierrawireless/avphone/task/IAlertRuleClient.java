package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AlertRule;

public interface IAlertRuleClient {

    AlertRule getAlertRule(String serialNumber) throws IOException, AirVantageException;

    AlertRule createAlertRule(String serialNumber, String systemUid, String applicationUid) throws IOException, AirVantageException;

    AlertRule updateAlertRule(String alertRuleUid, String serialNumber, String systemUid, String applicationUid) throws IOException, AirVantageException;

    
}
