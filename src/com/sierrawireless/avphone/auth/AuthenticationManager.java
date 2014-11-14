package com.sierrawireless.avphone.auth;

import java.util.Date;

import net.airvantage.utils.PreferenceUtils;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.sierrawireless.avphone.AuthorizationActivity;
import com.sierrawireless.avphone.R;

public class AuthenticationManager implements IAuthenticationManager {

    public AuthenticationManager() {
    }

    @Override
    public void authenticate(PreferenceUtils prefUtils, Fragment context, AuthenticationListener listener) {
        Authentication auth = prefUtils.getAuthentication();
        if (auth != null) {
            listener.onAuthentication(auth);
        } else {
            Intent intent = new Intent(context.getActivity(), AuthorizationActivity.class);
            context.startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION);
        }
    }

    @Override
    public Authentication activityResultAsAuthentication(int requestCode, int resultCode, Intent data) {
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

    public Authentication getAuthentication(PreferenceUtils prefUtils) {
        return prefUtils.getAuthentication();
    }

    @Override
    public void saveAuthentication(PreferenceUtils prefUtils, Authentication auth) {
        prefUtils.setPreference(R.string.pref_access_token_key, auth.getAccessToken());
        long expiresAt = auth.getExpirationDate().getTime();
        prefUtils.setPreference(R.string.pref_token_expires_at_key, String.valueOf(expiresAt));
    }

    @Override
    public void forgetAuthentication(PreferenceUtils prefUtils) {
        prefUtils.setPreference(R.string.pref_access_token_key, null);
        prefUtils.setPreference(R.string.pref_token_expires_at_key, null);
    }

}
