package com.sierrawireless.avphone.auth;

import java.util.Date;

import com.sierrawireless.avphone.AuthorizationActivity;

import android.app.Activity;
import android.content.Intent;

public class AuthUtils {

    public static Authentication activityResultAsAuthentication(int requestCode, int resultCode, Intent data) {
        Authentication res = null;
        switch (requestCode) {
        case (AuthorizationActivity.REQUEST_AUTHORIZATION): {
            if (resultCode == Activity.RESULT_OK) {
                String accessToken = data.getStringExtra(AuthorizationActivity.AUTHENTICATION_TOKEN);
                long expiresAtMs = data.getLongExtra(AuthorizationActivity.AUTHENTICATION_EXPIRATION_DATE, 0);
                res = new Authentication();
                res.setAccessToken(accessToken);
                res.setExpirationDate(new Date(expiresAtMs));
            }
            break;
        }
        }
        return res;
    }


}
