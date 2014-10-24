package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import android.os.AsyncTask;
import android.util.Log;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class RegisterSystemTask extends AsyncTask<Object, Integer, AvError> {

    private IApplicationClient applicationClient;

    private ISystemClient systemClient;

    private IAlertRuleClient alertRuleClient;

    public RegisterSystemTask(IApplicationClient applicationClient, ISystemClient systemClient,
            IAlertRuleClient alertRuleClient) {
        this.applicationClient = applicationClient;
        this.systemClient = systemClient;
        this.alertRuleClient = alertRuleClient;
    }

    @Override
    protected AvError doInBackground(Object... params) {

        try {

            String serialNumber = (String) params[0];
            String imei = (String) params[1];
            String mqttPassword = (String) params[2];
            CustomDataLabels customData = (CustomDataLabels) params[3];

            Application application = this.applicationClient.ensureApplicationExists(serialNumber);

            net.airvantage.model.AvSystem system = this.systemClient.getSystem(serialNumber);
            if (system == null) {
                system = systemClient.createSystem(serialNumber, imei, mqttPassword, application.uid);
            }

            net.airvantage.model.AlertRule alertRule = this.alertRuleClient.getAlertRule(serialNumber);
            if (alertRule == null) {
                this.alertRuleClient.createAlertRule(serialNumber, system.uid, application.uid);
            }

            this.applicationClient.setApplicationData(application.uid, customData);

            return null;
        } catch (AirVantageException e) {
            return e.getError();
        } catch (IOException e) {
            Log.e(MainActivity.class.getName(), "Error when trying to get current user", e);
            return new AvError("unkown.error");
        }

    }

}
