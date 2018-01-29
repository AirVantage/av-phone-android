package net.airvantage.utils;

import java.io.IOException;
import java.util.List;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.AvSystem;
import net.airvantage.model.Protocol;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;

public interface IAirVantageClient {

    void createAlertRule(AlertRule alertRule, String application) throws IOException, AirVantageException;

    AlertRule getAlertRuleByName(String name, String application) throws IOException, AirVantageException;

    void setApplicationData(String applicationUid, List<ApplicationData> data) throws IOException, AirVantageException;

    List<Application> getApplications(String appType) throws IOException, AirVantageException;

    Application createApplication(Application application) throws IOException, AirVantageException;

    void setApplicationCommunication(String applicationUid, List<Protocol> protocols) throws IOException, AirVantageException;

    void updateSystem(AvSystem system) throws IOException, AirVantageException;

    void logout() throws IOException, AirVantageException;

    UserRights getUserRights() throws IOException, AirVantageException;

    User getCurrentUser() throws IOException, AirVantageException;

}
