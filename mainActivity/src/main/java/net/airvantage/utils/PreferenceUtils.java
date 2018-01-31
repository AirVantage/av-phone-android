package net.airvantage.utils;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

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

    private static final String PREF_ACCESS_TOKEN = "pref_access_token";

    private static final String PREF_TOKEN_EXPIRES_AT = "pref_token_expires_at";

    private static Properties properties;

    public static AvPhonePrefs getAvPhonePrefs(Context context) {
        AvPhonePrefs res = new AvPhonePrefs();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        res.serverHost = prefs.getString(PREF_SERVER_KEY, context.getString(R.string.pref_server_na_value));

        res.clientId = prefs.getString(PREF_CLIENT_ID_KEY, getNaClientId(context));

        HashMap<String, Server> serverMapping = new HashMap<>();
        serverMapping.put(context.getString(R.string.pref_server_eu_value), Server.EU);
        serverMapping.put(context.getString(R.string.pref_server_na_value), Server.NA);
        serverMapping.put(context.getString(R.string.pref_server_custom_value), Server.CUSTOM);
        res.usesServer = serverMapping.get(res.serverHost);

        res.password = prefs.getString(PREF_PASSWORD_KEY, context.getString(R.string.pref_password_default));
        res.period = prefs.getString(PREF_PERIOD_KEY, DEFAULT_COMM_PERIOD);

        return res;
    }

    private static Properties getPropertiesFile(Context context) {
        if (properties == null) {
            try {
                InputStream in = context.getAssets().open("avphone.properties");
                properties = new Properties();
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    private static String getNaClientId(Context context) {
        Properties properties = getPropertiesFile(context);
        return properties.getProperty("clientid.na");
    }

    private static String getEuClientId(Context context) {
        Properties properties = getPropertiesFile(context);
        return properties.getProperty("clientid.eu");
    }

    private static String getCustomClientId(Context context) {
        Properties properties = getPropertiesFile(context);
        return properties.getProperty("clientid.custom");
    }

    public static boolean isCustomDefined(Context context) {
        return getCustomClientId(context) != null;
    }

    private static String getPreference(Context context, int prefKeyId, int defaultValueKeyId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        String defaultValueKey = context.getString(defaultValueKeyId);
        return prefs.getString(prefKey, defaultValueKey);
    }

    private static void setPreference(Context context, String prefKey, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(prefKey, value).apply();
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
        labels.setCustomUp1Label(getPreference(context, R.string.pref_custom1_label_key,
                R.string.pref_custom1_label_default));
        labels.setCustomUp2Label(getPreference(context, R.string.pref_custom2_label_key,
                R.string.pref_custom2_label_default));
        labels.setCustomDown1Label(getPreference(context, R.string.pref_custom3_label_key,
                R.string.pref_custom3_label_default));
        labels.setCustomDown2Label(getPreference(context, R.string.pref_custom4_label_key,
                R.string.pref_custom4_label_default));
        labels.setCustomStr1Label(getPreference(context, R.string.pref_custom5_label_key,
                R.string.pref_custom5_label_default));
        labels.setCustomStr2Label(getPreference(context, R.string.pref_custom6_label_key,
                R.string.pref_custom6_label_default));

        return labels;
    }

    public static void setServer(Server server, Context context) {
        switch (server) {
            case NA:
                setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_na_value));
                setPreference(context, PREF_CLIENT_ID_KEY, getNaClientId(context));
                break;
            case EU:
                setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_eu_value));
                setPreference(context, PREF_CLIENT_ID_KEY, getEuClientId(context));
                break;
            case CUSTOM:
                setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_custom_value));
                setPreference(context, PREF_CLIENT_ID_KEY, getCustomClientId(context));
                break;
            default:
                throw new IllegalArgumentException("Should be NA or EU");
        }
    }

    public static void saveAuthentication(Context context, Authentication auth) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        prefs.edit().putString(PREF_ACCESS_TOKEN, auth.getAccessToken())
                .putLong(PREF_TOKEN_EXPIRES_AT, auth.getExpirationDate().getTime()).apply();
    }

    public static void resetAuthentication(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        prefs.edit().remove(PREF_ACCESS_TOKEN).remove(PREF_TOKEN_EXPIRES_AT).apply();

    }

    public static Authentication readAuthentication(Context context) {

        Authentication auth = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String accessToken = prefs.getString(PREF_ACCESS_TOKEN, null);

        Long expiresAtMs = null;

        try {
            expiresAtMs = prefs.getLong(PREF_TOKEN_EXPIRES_AT, 0);
        } catch (ClassCastException e) {
            // An earlier version might have stored the token as a string
            String expiresAtSt = null;
            try {
                expiresAtSt = prefs.getString(PREF_TOKEN_EXPIRES_AT, null);
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
