package com.sierrawireless.avphone.auth

import java.util.Date

import com.sierrawireless.avphone.activity.AuthorizationActivity

import android.app.Activity
import android.content.Intent

object AuthUtils {

    fun activityResultAsAuthentication(requestCode: Int, resultCode: Int, data: Intent): Authentication? {
        var res: Authentication? = null
        when (requestCode) {
            AuthorizationActivity.REQUEST_AUTHORIZATION -> {
                if (resultCode == Activity.RESULT_OK) {
                    val accessToken = data.getStringExtra(AuthorizationActivity.AUTHENTICATION_TOKEN)
                    val expiresAtMs = data.getLongExtra(AuthorizationActivity.AUTHENTICATION_EXPIRATION_DATE, 0)
                    res = Authentication()
                    res.accessToken = accessToken
                    res.expirationDate = Date(expiresAtMs)
                }
            }
        }
        return res
    }


}
