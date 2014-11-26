package net.airvantage.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class PreferenceUtils {

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
        res.serverHost = PreferenceUtils
                .getPreference(context, R.string.pref_server_key, R.string.pref_server_na_value);

        res.clientId = PreferenceUtils.getPreference(context, R.string.pref_client_id_key, R.string.pref_client_id_na);

        res.usesNA = (context.getString(R.string.pref_server_na_value)).equals(res.serverHost);
        res.usesEU = (context.getString(R.string.pref_server_eu_value)).equals(res.serverHost);

        res.password = PreferenceUtils.getPreference(context, R.string.pref_password_key, R.string.pref_password_default);
        res.period = prefs.getString(context.getString(R.string.pref_period_key), DEFAULT_COMM_PERIOD);

        return res;
    }

    public static String getPreference(Context context, int prefKeyId, int defaultValueKeyId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        String defaultValueKey = context.getString(defaultValueKeyId);
        return prefs.getString(prefKey, defaultValueKey);
    }

    public static void setPreference(Context context, int prefKeyId, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
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

    public static void toggleServers(Context context) {
        AvPhonePrefs prefs = PreferenceUtils.getAvPhonePrefs(context);
        if (prefs.usesEU()) {
            PreferenceUtils.setPreference(context, R.string.pref_server_key,
                    context.getString(R.string.pref_server_na_value));
            PreferenceUtils.setPreference(context, R.string.pref_client_id_key,
                    context.getString(R.string.pref_client_id_na));
        } else if (prefs.usesNA()) {
            PreferenceUtils.setPreference(context, R.string.pref_server_key,
                    context.getString(R.string.pref_server_eu_value));
            PreferenceUtils.setPreference(context, R.string.pref_client_id_key,
                    context.getString(R.string.pref_client_id_eu));
        }
    }

}
