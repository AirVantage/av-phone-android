package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import android.os.AsyncTask;
import android.util.Log;

import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class SyncWithAvTask extends AsyncTask<SyncWithAvParams, SyncProgress, AvError>  {

    private IApplicationClient applicationClient;

    private ISystemClient systemClient;

    private IAlertRuleClient alertRuleClient;

    private List<SyncWithAvListener> syncListeners = new ArrayList<SyncWithAvListener>();
    
    public SyncWithAvTask(IApplicationClient applicationClient, ISystemClient systemClient,
            IAlertRuleClient alertRuleClient) {
        this.applicationClient = applicationClient;
        this.systemClient = systemClient;
        this.alertRuleClient = alertRuleClient;
    }

    public void addProgressListener(SyncWithAvListener listener) {
        this.syncListeners.add(listener);
    }
    
    @Override
    protected AvError doInBackground(SyncWithAvParams... params) {

        try {

            SyncWithAvParams syncParams = params[0];
            
            String serialNumber = syncParams.deviceId;
            String imei = syncParams.imei;
            String mqttPassword = syncParams.mqttPassword;
            CustomDataLabels customData = syncParams.customData;

            publishProgress(SyncProgress.CHECKING_APPLICATION);
            
            Application application = this.applicationClient.ensureApplicationExists(serialNumber);

            publishProgress(SyncProgress.CHECKING_SYSTEM);
            
            net.airvantage.model.AvSystem system = this.systemClient.getSystem(serialNumber);
            if (system == null) {
            
                publishProgress(SyncProgress.CREATING_SYSTEM);
                
                system = systemClient.createSystem(serialNumber, imei, mqttPassword, application.uid);
            }

            publishProgress(SyncProgress.CHECKING_ALERT_RULE);
            
            net.airvantage.model.AlertRule alertRule = this.alertRuleClient.getAlertRule(serialNumber);
            if (alertRule == null) {
                
                publishProgress(SyncProgress.CREATING_ALERT_RULE);
                
                
                this.alertRuleClient.createAlertRule(serialNumber, system.uid, application.uid);
            }

            publishProgress(SyncProgress.UPDATING_APPLICATION);
            
            this.applicationClient.setApplicationData(application.uid, customData);

            if (!hasApplication(system, application)) {
            
                publishProgress(SyncProgress.ADDING_APPLICATION);
                
                this.applicationClient.addApplication(system, application);
            }
            
            publishProgress(SyncProgress.DONE);
            
            return null;
        } catch (AirVantageException e) {
            publishProgress(SyncProgress.DONE);
            return e.getError();
        } catch (IOException e) {
            Log.e(MainActivity.class.getName(), "Error when trying to synchronize with server", e);
            publishProgress(SyncProgress.DONE);
            return new AvError("unkown.error");
        }

    }

    @Override
    protected void onPostExecute(AvError result) {
        super.onPostExecute(result);
        for (SyncWithAvListener listener : syncListeners) {
            listener.onSynced(result);
        }
    }
    
    private boolean hasApplication(AvSystem system, Application application) {
        boolean found = false;
        if (system.applications != null) {
            for (Application app : system.applications) {
                if (app.uid.equals(application.uid)) {
                    found = true;
                }
            }
        }
        return found;
    }

}
