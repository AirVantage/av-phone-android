package com.sierrawireless.avphone.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.Html;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.AvPhoneApplication;

import net.airvantage.model.AvError;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;

import java.util.List;

public abstract class AvPhoneTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {


    void displayTaskError(AvError error, IMessageDisplayer displayer, Activity context, IUserClient userClient) {

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
                displayer.showError(R.string.sync_error_app_exists, AvPhoneApplication.INSTANCE.appType(user.name));
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

    }

    private String missingRightsMessage(AvError error, android.app.Activity context) {
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
