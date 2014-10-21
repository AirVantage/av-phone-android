package net.airvantage.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.sierrawireless.avphone.R;

public class PreferenceUtils {

	private static final String DEFAULT_COMM_PERIOD = "2";
	
	private SharedPreferences prefs;
	private Fragment fragment;

	/**
	 * Wrapper for usefull functions on a fragment.
	 * A Fragment is passed rather than an activity, since it seems 
	 * like keeping references to activities is not safe.
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

}
