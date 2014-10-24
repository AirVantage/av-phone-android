package net.airvantage.utils;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AlertRule;

public interface IAirVantageClient {

    public abstract AlertRule createAlertRule(AlertRule alertRule) throws IOException, AirVantageException;

    public abstract AlertRule getAlertRule(String name) throws IOException, AirVantageException;

}