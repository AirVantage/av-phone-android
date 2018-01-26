package com.sierrawireless.avphone.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.DeviceInfo;
import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.ObjectsManager;
import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.CustomDataLabels;
import com.sierrawireless.avphone.tools.MyPreference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import net.airvantage.model.AirVantageException;
import net.airvantage.model.Application;
import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;
import net.airvantage.model.alert.v1.AlertRule;

public class SyncWithAvTask extends AsyncTask<SyncWithAvParams, SyncProgress, SyncWithAvResult> {
    private static final String TAG = "SyncWithAvTask";

    private IApplicationClient applicationClient;

    private ISystemClient systemClient;

    private IAlertRuleClient alertRuleClient;

    private List<SyncWithAvListener> syncListeners = new ArrayList<SyncWithAvListener>();

    private IUserClient userClient;

    private Context context;
    private ObjectsManager objectsManager;

    public SyncWithAvTask(IApplicationClient applicationClient, ISystemClient systemClient,
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
            final CustomDataLabels customData = syncParams.customData;
            final MainActivity activity = syncParams.activity;
            objectsManager = ObjectsManager.getInstance();

            systemType = objectsManager.getSavedObjectName();

            // For emulator and iOs compatibility sake, using generated serial.
            final String serialNumber =  DeviceInfo.generateSerial(user.uid, systemType);

            // Save Device serial in context
            if (context instanceof MainActivity) {
                final MainActivity mainActivity = (MainActivity) context;
                mainActivity.setSystemSerial(serialNumber);
            }

            if (syncParams.delete == true) {
                publishProgress(SyncProgress.CHECKING_SYSTEM);
                net.airvantage.model.AvSystem system = this.systemClient.getSystem(serialNumber, systemType);
                if (system != null) {
                    publishProgress(SyncProgress.DELETING_SYSTEM);
                    systemClient.deleteSystem(system);
                }
                publishProgress(SyncProgress.DONE);
                return new SyncWithAvResult(null, user);

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

            AlertRule alertRule = this.alertRuleClient.getAlertRule(serialNumber);
            if (alertRule == null) {

                publishProgress(SyncProgress.CREATING_ALERT_RULE);

                this.alertRuleClient.createAlertRule();
            }

            publishProgress(SyncProgress.UPDATING_APPLICATION);

            this.applicationClient.setApplicationData(application.uid, objectsManager.getSavecObject().datas);

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
            if (error.missingRights()) {
                String message = missingRightsMessage(error, context);
                displayer.showErrorMessage(Html.fromHtml(message));
            } else if (error.systemAlreadyExists()) {
                displayer.showError(R.string.sync_error_system_exists);
            } else if (error.gatewayAlreadyExists()) {
                displayer.showError(R.string.sync_error_gateway_exists);
            } else if (error.applicationAlreadyUsed()) {
                final User user = userClient.getUser();
                if (user == null) {
                    displayer.showError(R.string.sync_error_no_user_data);
                } else {
                    displayer.showError(R.string.sync_error_app_exists, AvPhoneApplication.appType(user.name));
                }
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

    protected Context getContext() {
        return this.context;
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
