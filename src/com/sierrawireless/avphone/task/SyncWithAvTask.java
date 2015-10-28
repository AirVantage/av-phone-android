package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.CustomDataLabels;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import net.airvantage.model.UserRights;

public class SyncWithAvTask extends AsyncTask<SyncWithAvParams, SyncProgress, SyncWithAvResult> {

    private IApplicationClient applicationClient;

    private ISystemClient systemClient;

    private IAlertRuleClient alertRuleClient;

    private List<SyncWithAvListener> syncListeners = new ArrayList<SyncWithAvListener>();

    private IUserClient userClient;

    public SyncWithAvTask(IApplicationClient applicationClient, ISystemClient systemClient,
            IAlertRuleClient alertRuleClient, IUserClient userClient) {
        this.applicationClient = applicationClient;
        this.systemClient = systemClient;
        this.alertRuleClient = alertRuleClient;
        this.userClient = userClient;
    }

    public void addProgressListener(SyncWithAvListener listener) {
        this.syncListeners.add(listener);
    }

    @Override
    protected SyncWithAvResult doInBackground(SyncWithAvParams... params) {

        try {

            publishProgress(SyncProgress.CHECKING_RIGHTS);

            List<String> missingRights = userClient.checkRights();

            if (!missingRights.isEmpty()) {
                return new SyncWithAvResult(new AvError(AvError.MISSING_RIGHTS, missingRights));
            }

            SyncWithAvParams syncParams = params[0];

            String serialNumber = syncParams.deviceId;
            String imei = syncParams.imei;
            String mqttPassword = syncParams.mqttPassword;
            CustomDataLabels customData = syncParams.customData;

            publishProgress(SyncProgress.CHECKING_APPLICATION);

            Application application = this.applicationClient.ensureApplicationExists();

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

                this.alertRuleClient.createAlertRule();
            }

            publishProgress(SyncProgress.UPDATING_APPLICATION);

            this.applicationClient.setApplicationData(application.uid, customData);

            if (!hasApplication(system, application)) {

                publishProgress(SyncProgress.ADDING_APPLICATION);

                this.applicationClient.addApplication(system, application);
            }

            publishProgress(SyncProgress.DONE);

            return new SyncWithAvResult(system);

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
            if (error.missingRights()) {
                String message = missingRightsMessage(error, context);
                displayer.showErrorMessage(Html.fromHtml(message));
            } else if (error.systemAlreadyExists()) {
                displayer.showError(R.string.sync_error_system_exists);
            } else if (error.gatewayAlreadyExists()) {
                displayer.showError(R.string.sync_error_gateway_exists);
            } else if (error.applicationAlreadyUsed()) {
                displayer.showError(R.string.sync_error_app_exists,
                        AvPhoneApplication.appType(userClient.getUserName()));
            } else if (error.tooManyAlerRules()) {
                displayer.showError(R.string.sync_error_too_many_rules);
            } else if (error.cantCreateApplication()) {
                displayer.showError(R.string.sync_error_no_right_create_application);
            } else if (error.cantCreateSystem()) {
                displayer.showError(R.string.sync_error_no_right_create_system);
            } else if (error.cantCreateAlertRule()) {
                displayer.showError(R.string.sync_error_no_right_create_alert_rule);
            } else if (error.cantUpdateApplication()) {
                displayer.showError(R.string.sync_error_no_right_update_app);
            } else if (error.cantUpdateSystem()) {
                displayer.showError(R.string.sync_error_no_right_update_system);
            } else if (error.forbidden()) {
                String method = error.errorParameters.get(0);
                String url = error.errorParameters.get(1);
                displayer.showError(R.string.sync_error_forbidden, method, url);
            } else {
                displayer.showError(R.string.sync_error_unexpected, error.error);
            }
        } else {
            displayer.showSuccess(R.string.sync_success);
        }
    }

    private String missingRightsMessage(AvError error, Activity context) {
        List<String> missingRights = error.errorParameters;
        StringBuilder message = new StringBuilder();
        message.append(context.getText(R.string.auth_not_enough_rights));
        message.append("<br/>");
        message.append(context.getText(R.string.auth_missing_rights));
        message.append("<br/>");

        for (String missingRight : missingRights) {
            message.append("&#8226; ");
            message.append(UserRights.asString(missingRight, context));
            message.append("<br/>");
        }
        return message.toString();
    }

}
