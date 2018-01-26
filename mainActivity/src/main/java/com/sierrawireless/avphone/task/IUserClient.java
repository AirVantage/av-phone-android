package com.sierrawireless.avphone.task;

import java.util.List;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.User;

public interface IUserClient {

    List<String> checkRights() throws AirVantageException;

    User getUser();
}
