package com.sierrawireless.avphone.task;

import java.util.List;

import net.airvantage.model.AirVantageException;

public interface IUserClient {

    public List<String> checkRights() throws AirVantageException;
}
