package net.airvantage.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.model.CustomDataLabels;

public class PreferenceUtils {

    private static final String DEFAULT_COMM_PERIOD = "2";

    private SharedPreferences prefs;
    private Fragment fragment;

    /**
     * Wrapper for usefull functions on a fragment. A Fragment is passed rather than an activity, since it seems like
     * keeping references to activities is not safe.
     */
    public PreferenceUtils(Fragment fragment) {

        this.fragment = fragment;
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    public void addListener(OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public AvPhonePrefs getAvPhonePrefs() {
        AvPhonePrefs res = new AvPhonePrefs();

        res.serverHost = prefs.getString(getActivity().getString(R.string.pref_server_key), null);
        res.password = prefs.getString(getActivity().getString(R.string.pref_password_key), null);
        res.period = prefs.getString(getActivity().getString(R.string.pref_period_key), DEFAULT_COMM_PERIOD);

        return res;
    }

    public String getString(int prefKey, int defaultValueKey) {
        return prefs.getString(fragment.getString(prefKey), fragment.getString(defaultValueKey));
    }

    public void setString(int prefKey, String value) {
        prefs.edit().putString(fragment.getString(prefKey), value);
    }

    public void showMissingPrefsDialog() {
        new AlertDialog.Builder(getActivity()).setTitle(R.string.invalid_prefs).setMessage(R.string.prefs_missing)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                }).show();
    }

    protected Activity getActivity() {
        return this.fragment.getActivity();
    }

    public CustomDataLabels getCustomDataLabels() {
        CustomDataLabels labels = new CustomDataLabels();
        labels.customUp1Label = getString(R.id.custom1_value, R.string.pref_custom1_label_default);
        labels.customUp2Label = getString(R.id.custom2_value, R.string.pref_custom2_label_default);
        labels.customDown1Label = getString(R.id.custom3_value, R.string.pref_custom3_label_default);
        labels.customDown2Label = getString(R.id.custom4_value, R.string.pref_custom4_label_default);
        labels.customStr1Label = getString(R.id.custom5_value, R.string.pref_custom5_label_default);
        labels.customStr2Label = getString(R.id.custom6_value, R.string.pref_custom6_label_default);

        return labels;
    }

}
