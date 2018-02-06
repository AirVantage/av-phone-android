package com.sierrawireless.avphone.activity

import net.airvantage.utils.PreferenceUtils
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import com.sierrawireless.avphone.R

class SettingsActivity : PreferenceActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences)

            onSharedPreferenceChanged(preferenceScreen.sharedPreferences, null)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            // period
            val periodPref = findPreference(PreferenceUtils.PREF_PERIOD_KEY) as ListPreference
            periodPref.summary = periodPref.entry
        }
    }

}
