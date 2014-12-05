package net.airvantage.utils;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.auth.Authentication;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class PreferenceUtils {

    private static String LOGTAG = PreferenceUtils.class.getName();

    public enum Server {
        NA, EU, CUSTOM
    }

    private static final String DEFAULT_COMM_PERIOD = "2";

    public static final String PREF_SERVER_KEY = "pref_server_key";

    public static final String PREF_CLIENT_ID_KEY = "pref_client_id_key";

    public static final String PREF_PASSWORD_KEY = "pref_password_key";

    public static final String PREF_PERIOD_KEY = "pref_period_key";

    public static final String PREF_ACCESS_TOKEN = "pref_access_token";

    public static final String PREF_TOKEN_EXPIRES_AT = "pref_token_expires_at";

    public static AvPhonePrefs getAvPhonePrefs(Context context) {
        AvPhonePrefs res = new AvPhonePrefs();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        res.serverHost = prefs.getString(PREF_SERVER_KEY, context.getString(R.string.pref_server_na_value));

        res.clientId = prefs.getString(PREF_CLIENT_ID_KEY, context.getString(R.string.pref_client_id_na));

        res.usesNA = (context.getString(R.string.pref_server_na_value)).equals(res.serverHost);
        res.usesEU = (context.getString(R.string.pref_server_eu_value)).equals(res.serverHost);

        res.password = prefs.getString(PREF_PASSWORD_KEY, context.getString(R.string.pref_password_default));
        res.period = prefs.getString(PREF_PERIOD_KEY, DEFAULT_COMM_PERIOD);

        return res;
    }

    public static String getPreference(Context context, int prefKeyId, int defaultValueKeyId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        String defaultValueKey = context.getString(defaultValueKeyId);
        return prefs.getString(prefKey, defaultValueKey);
    }

    public static void setPreference(Context context, String prefKey, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(prefKey, value).commit();
    }

    public static void showMissingPrefsDialog(Activity activity) {
        new AlertDialog.Builder(activity).setTitle(R.string.invalid_prefs).setMessage(R.string.prefs_missing)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                }).show();
    }

    public static CustomDataLabels getCustomDataLabels(Context context) {
        CustomDataLabels labels = new CustomDataLabels();
        labels.customUp1Label = getPreference(context, R.string.pref_custom1_label_key,
                R.string.pref_custom1_label_default);
        labels.customUp2Label = getPreference(context, R.string.pref_custom2_label_key,
                R.string.pref_custom2_label_default);
        labels.customDown1Label = getPreference(context, R.string.pref_custom3_label_key,
                R.string.pref_custom3_label_default);
        labels.customDown2Label = getPreference(context, R.string.pref_custom4_label_key,
                R.string.pref_custom4_label_default);
        labels.customStr1Label = getPreference(context, R.string.pref_custom5_label_key,
                R.string.pref_custom5_label_default);
        labels.customStr2Label = getPreference(context, R.string.pref_custom6_label_key,
                R.string.pref_custom6_label_default);

        return labels;
    }

    public static void setServer(Server server, Context context) {
        if (server == Server.NA) {
            PreferenceUtils.setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_na_value));
            PreferenceUtils.setPreference(context, PREF_CLIENT_ID_KEY, context.getString(R.string.pref_client_id_na));
        } else if (server == Server.EU) {
            PreferenceUtils.setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_eu_value));
            PreferenceUtils.setPreference(context, PREF_CLIENT_ID_KEY, context.getString(R.string.pref_client_id_eu));
        } else {
            throw new IllegalArgumentException("Should be NA or EU");
        }
    }

    public static void saveAuthentication(Context context, Authentication auth) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        prefs.edit().putString(PreferenceUtils.PREF_ACCESS_TOKEN, auth.getAccessToken())
                .putLong(PreferenceUtils.PREF_TOKEN_EXPIRES_AT, auth.getExpirationDate().getTime()).commit();
    }

    public static void resetAuthentication(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        prefs.edit().remove(PreferenceUtils.PREF_ACCESS_TOKEN).remove(PreferenceUtils.PREF_TOKEN_EXPIRES_AT).commit();

    }

    public static Authentication readAuthentication(Context context) {

        Authentication auth = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String accessToken = prefs.getString(PreferenceUtils.PREF_ACCESS_TOKEN, null);

        Long expiresAtMs = null;

        try {
            expiresAtMs = prefs.getLong(PreferenceUtils.PREF_TOKEN_EXPIRES_AT, 0);
        } catch (ClassCastException e) {
            // An earlier version might have stored the token as a string
            String expiresAtSt = null;
            try {
                expiresAtSt = prefs.getString(PreferenceUtils.PREF_TOKEN_EXPIRES_AT, null);
                if (expiresAtSt != null) {
                    expiresAtMs = Long.parseLong(expiresAtSt);
                }
            } catch (NumberFormatException nfe) {
                // The string was not even a valid one, we'll ignore it.
                Log.w(LOGTAG, "pref_token_expires_at stored as invalid String : '" + expiresAtSt + "'", nfe);
            }
        }

        if (accessToken != null && expiresAtMs != null && expiresAtMs != 0) {
            Date expiresAt = new Date(expiresAtMs);
            auth = new Authentication();
            auth.setAccessToken(accessToken);
            auth.setExpirationDate(expiresAt);
        }

        return auth;

    }

}
