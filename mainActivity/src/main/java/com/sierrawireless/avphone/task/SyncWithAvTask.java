package com.sierrawireless.avphone.task;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.DeviceInfo;
import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.ObjectsManager;
import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import net.airvantage.model.User;
import net.airvantage.model.alert.v1.AlertRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncWithAvTask extends AvPhoneTask<SyncWithAvParams, SyncProgress, SyncWithAvResult> {

    private IApplicationClient applicationClient;

    private ISystemClient systemClient;

    private IAlertRuleClient alertRuleClient;

    private List<SyncWithAvListener> syncListeners = new ArrayList<>();

    private IUserClient userClient;

    @SuppressLint("StaticFieldLeak")
    private Context context;


    SyncWithAvTask(IApplicationClient applicationClient, ISystemClient systemClient,
                   IAlertRuleClient alertRuleClient, IUserClient userClient, Context context) {
        this.applicationClient = applicationClient;
        this.systemClient = systemClient;
        this.alertRuleClient = alertRuleClient;
        this.userClient = userClient;
        this.context = context;
    }

    public void addProgressListener(SyncWithAvListener listener) {
        this.syncListeners.add(listener);
    }

    @Override
    @SuppressLint("DefaultLocale")
    protected SyncWithAvResult doInBackground(SyncWithAvParams... params) {

        try {

            publishProgress(SyncProgress.CHECKING_RIGHTS);

            final List<String> missingRights = userClient.checkRights();
            if (!missingRights.isEmpty()) {
                return new SyncWithAvResult(new AvError(AvError.MISSING_RIGHTS, missingRights));
            }

            String systemType;
            final SyncWithAvParams syncParams = params[0];
            final User user = userClient.getUser();
            final String imei = syncParams.imei;
            final String iccid = syncParams.iccid;
            final String deviceName = syncParams.deviceName;
            final String mqttPassword = syncParams.mqttPassword;
            ObjectsManager objectsManager = ObjectsManager.getInstance();

            systemType = objectsManager.getSavedObjectName();

            // For emulator and iOs compatibility sake, using generated serial.
            final String serialNumber =  DeviceInfo.generateSerial(user.uid);

            // Save Device serial in context
            if (context instanceof MainActivity) {
                final MainActivity mainActivity = (MainActivity) context;
                mainActivity.setSystemSerial(serialNumber);
            }

            publishProgress(SyncProgress.CHECKING_APPLICATION);

            Application application = this.applicationClient.ensureApplicationExists();

            publishProgress(SyncProgress.CHECKING_SYSTEM);

            net.airvantage.model.AvSystem system = this.systemClient.getSystem(serialNumber, systemType);
            if (system == null) {

                publishProgress(SyncProgress.CREATING_SYSTEM);

                system = systemClient.createSystem(serialNumber, iccid, systemType, mqttPassword, application.uid, deviceName, user.name, imei);
            }

            publishProgress(SyncProgress.CHECKING_ALERT_RULE);

            AlertRule alertRule = this.alertRuleClient.getAlertRule(serialNumber, application.uid);
            if (alertRule == null) {

                publishProgress(SyncProgress.CREATING_ALERT_RULE);

                this.alertRuleClient.createAlertRule(application.uid);
            }

            publishProgress(SyncProgress.UPDATING_APPLICATION);

            this.applicationClient.setApplicationData(application.uid, objectsManager.getSavecObject().datas, objectsManager.getSavecObject().name);

            if (!hasApplication(system, application)) {

                publishProgress(SyncProgress.ADDING_APPLICATION);

                this.applicationClient.addApplication(system, application);
            }

            publishProgress(SyncProgress.DONE);

            return new SyncWithAvResult(system, user);

        } catch (AirVantageException e) {
            publishProgress(SyncProgress.DONE);
            return new SyncWithAvResult(e.getError());
        } catch (IOException e) {
            Crashlytics.logException(e);
            Log.e(MainActivity.class.getName(), "Error when trying to synchronize with server", e);
            publishProgress(SyncProgress.DONE);
            return new SyncWithAvResult(new AvError("unkown.error"));
        }

    }

    @Override
    protected void onPostExecute(SyncWithAvResult result) {
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

    public void showResult(SyncWithAvResult result, IMessageDisplayer displayer, Activity context) {

        if (result.isError()) {
            AvError error = result.getError();
            displayTaskError(error, displayer, context, userClient);
        } else {
            displayer.showSuccess(R.string.sync_success);
        }
    }

    protected Context getContext() {
        return this.context;
    }

}
