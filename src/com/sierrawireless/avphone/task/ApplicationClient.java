package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.AvSystem;
import net.airvantage.model.Protocol;
import net.airvantage.utils.IAirVantageClient;
import net.airvantage.utils.Utils;

import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class ApplicationClient implements IApplicationClient {

    private IAirVantageClient client;

    public ApplicationClient(IAirVantageClient client) {
        this.client = client;
    }

    @Override
    public Application ensureApplicationExists(String serialNumber) throws IOException, AirVantageException {
        Application application = getApplication(serialNumber);
        if (application == null) {
            application = createApplication(serialNumber);
            setApplicationCommunication(application.uid);
        }
        return application;
    }

    @Override
    public void setApplicationData(String applicationUid, CustomDataLabels customData) throws IOException,
            AirVantageException {
        List<ApplicationData> data = AvPhoneApplication.createApplicationData(customData);
        client.setApplicationData(applicationUid, data);
    }

    protected Application getApplication(String serialNumber) throws IOException, AirVantageException {
        List<Application> applications = client.getApplications(AvPhoneApplication.appType(serialNumber));
        return Utils.first(applications);
    }

    @Override
    public Application createApplication(String serialNumber) throws IOException, AirVantageException {
        Application application = AvPhoneApplication.createApplication(serialNumber);
        return client.createApplication(application);
    }

    @Override
    public void setApplicationCommunication(String applicationUid) throws IOException, AirVantageException {
        List<Protocol> protocols = AvPhoneApplication.createProtocols();
        client.setApplicationCommunication(applicationUid, protocols);
    }
    
    @Override
    public void addApplication(AvSystem system, Application application) throws IOException, AirVantageException {
        client.updateSystem(systemWithApplication(system, application));
    }

    protected AvSystem systemWithApplication(AvSystem system, Application appToAdd) {
        
        AvSystem res = new AvSystem();
        res.uid = system.uid;
        res.applications = new ArrayList<Application>();
        
        boolean appAlreadyLinked = false;
        
        if (system.applications != null) {
            for (Application systemApp : system.applications) {
                Application resApp = new Application();
                resApp.uid = systemApp.uid;
                res.applications.add(resApp);
                if (resApp.uid == appToAdd.uid) {
                    appAlreadyLinked = true;
                }
            }
        }
        if (!appAlreadyLinked) {
            Application addedApp = new Application();
            addedApp.uid = appToAdd.uid;
            res.applications.add(addedApp);
        }
        
        return res;
        
        
    }

}
