package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AlertRule;

public interface IAlertRuleClient {

    public AlertRule getAlertRule(String serialNumber) throws IOException, AirVantageException;

    public AlertRule createAlertRule(String serialNumber, String systemUid, String applicationUid) throws IOException, AirVantageException;

}
