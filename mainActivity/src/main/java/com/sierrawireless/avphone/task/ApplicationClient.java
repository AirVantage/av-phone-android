package com.sierrawireless.avphone.task;

import android.util.Log;

import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.AvPhoneObjectData;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.ApplicationData;
import net.airvantage.model.AvSystem;
import net.airvantage.model.Protocol;
import net.airvantage.model.User;
import net.airvantage.utils.IAirVantageClient;
import net.airvantage.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationClient implements IApplicationClient {
    private static final String TAG = "ApplicationClient";

    private IAirVantageClient client;
    private User currentUser = null;

    ApplicationClient(IAirVantageClient client) {
        this.client = client;
    }

    @Override
    public Application ensureApplicationExists() throws IOException, AirVantageException {
        Application application = getApplication();

        if (application == null) {
            Log.d(TAG, "ensureApplicationExists: Create new application");
            application = createApplication();
            setApplicationCommunication(application.uid);
        }
        Log.d(TAG, "ensureApplicationExists: application is " + application.uid);
        return application;
    }

    @Override
    public void setApplicationData(String applicationUid, ArrayList<AvPhoneObjectData> customData)
            throws IOException, AirVantageException {
        List<ApplicationData> data = AvPhoneApplication.createApplicationData(customData);
        client.setApplicationData(applicationUid, data);
    }

    protected Application getApplication() throws IOException, AirVantageException {
        List<Application> applications = client.getApplications(AvPhoneApplication.appType(getCurrentUsername()));
        return Utils.first(applications);
    }

    private String getCurrentUsername() {
        if (currentUser == null) {
            try {
                currentUser = client.getCurrentUser();
            } catch (final IOException | AirVantageException e) {
                return "";
            }
        }
        return currentUser.email;
    }

    @Override
    public Application createApplication() throws IOException, AirVantageException {
        Application application = AvPhoneApplication.createApplication(getCurrentUsername());
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

    private AvSystem systemWithApplication(AvSystem system, Application appToAdd) {

        AvSystem res = new AvSystem();
        res.uid = system.uid;
        res.type = system.type;
        res.applications = new ArrayList<>();

        boolean appAlreadyLinked = false;

        if (system.applications != null) {
            for (Application systemApp : system.applications) {
                Application resApp = new Application();
                resApp.uid = systemApp.uid;
                res.applications.add(resApp);
                if (resApp.uid.equals(appToAdd.uid)) {
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
