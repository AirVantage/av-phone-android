package com.sierrawireless.avphone.activity

import android.app.DialogFragment.STYLE_NO_TITLE
import net.airvantage.utils.PreferenceUtils
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.EditTextPreference
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.sierrawireless.avphone.R

class SettingsActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, MySettingsFragment()).commit()
    }

    class MySettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            retainInstance = true;
            setPreferencesFromResource(R.xml.preferences, rootKey)



            onSharedPreferenceChanged(preferenceScreen.sharedPreferences, rootKey)
        }

        override fun onDisplayPreferenceDialog(preference: Preference?) {
            if (preference is androidx.preference.EditTextPreference) {
                Log.d("JB", "*********************************")
                val editText = EditTextPreferenceDialogFragmentCompat.newInstance(preference!!.key)
                editText.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
                editText.setTargetFragment(this, 0);
                editText.show(fragmentManager!!,
                        "android.support.v7.preference.PreferenceFragment.DIALOG");



            } else if (preference is ListPreference) {
                Log.d("JB", "*********************************")
                val list = ListPreferenceDialogFragmentCompat.newInstance(preference!!.key)
                list.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
                list.setTargetFragment(this, 0);
                list.show(fragmentManager!!,
                        "android.support.v7.preference.PreferenceFragment.DIALOG");
            }else{

                super.onDisplayPreferenceDialog(preference)
            }
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            // period
            val periodPref = findPreference<ListPreference>(PreferenceUtils.PREF_PERIOD_KEY)
            periodPref?.summary = periodPref?.entry
        }
    }

}
