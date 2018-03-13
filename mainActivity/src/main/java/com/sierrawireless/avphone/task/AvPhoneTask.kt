package com.sierrawireless.avphone.task

import android.app.Activity
import android.os.AsyncTask
import android.os.Build
import android.text.Html
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.model.AvPhoneApplication
import net.airvantage.model.AvError
import net.airvantage.model.UserRights

abstract class AvPhoneTask<Params, Progress, Result> : AsyncTask<Params, Progress, Result>() {

     fun displayTaskError(error: AvError, displayer: IMessageDisplayer, context: Activity, userClient: IUserClient, deviceName: String) {

        if (error.missingRights()) {
            val message = missingRightsMessage(error, context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                displayer.showErrorMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY))
            }else{
                @Suppress("DEPRECATION")
                displayer.showErrorMessage(Html.fromHtml(message))
            }
        } else if (error.systemAlreadyExists()) {
            displayer.showError(R.string.sync_error_system_exists)
        } else if (error.gatewayAlreadyExists()) {
            displayer.showError(R.string.sync_error_gateway_exists)
        } else if (error.applicationAlreadyUsed()) {
            val user = userClient.user
            if (user == null) {
                displayer.showError(R.string.sync_error_no_user_data)
            } else {
                displayer.showError(R.string.sync_error_app_exists, AvPhoneApplication.appType(user.name!!, deviceName))
            }
        } else if (error.tooManyAlerRules()) {
            displayer.showError(R.string.sync_error_too_many_rules)
        } else if (error.cantCreateApplication()) {
            displayer.showError(R.string.sync_error_no_right_create_application)
        } else if (error.cantCreateSystem()) {
            displayer.showError(R.string.sync_error_no_right_create_system)
        } else if (error.cantCreateAlertRule()) {
            displayer.showError(R.string.sync_error_no_right_create_alert_rule)
        } else if (error.cantUpdateApplication()) {
            displayer.showError(R.string.sync_error_no_right_update_app)
        } else if (error.cantUpdateSystem()) {
            displayer.showError(R.string.sync_error_no_right_update_system)
        } else if (error.forbidden()) {
            val method = error.errorParameters[0]
            val url = error.errorParameters[1]
            displayer.showError(R.string.sync_error_forbidden, method, url)
        } else {
            displayer.showError(R.string.sync_error_unexpected, error.error)
        }

    }

    private fun missingRightsMessage(error: AvError, context: android.app.Activity): String {
        val missingRights = error.errorParameters
        val message = StringBuilder()
        if (missingRights.size == 1 && missingRights[0] == "No Connection" ) {
            message.append("We have no data connection")
        } else {
            message.append(context.getText(R.string.auth_not_enough_rights))
            message.append("<br/>")
            message.append(context.getText(R.string.auth_missing_rights))
            message.append("<br/>")


            for (missingRight in missingRights) {
                message.append("&#8226; ")
                message.append(UserRights.asString(missingRight, context))
                message.append("<br/>")
            }
        }
        return message.toString()
    }
}
