package com.sierrawireless.avphone.activity

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import net.airvantage.utils.PreferenceUtils


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
            retainInstance = true
            setPreferencesFromResource(com.sierrawireless.avphone.R.xml.preferences, rootKey)

            val passwordPReference: EditTextPreference? = this.findPreference("pref_password_key")


            passwordPReference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            onSharedPreferenceChanged(preferenceScreen.sharedPreferences, rootKey)
        }

        override fun onDisplayPreferenceDialog(preference: Preference?) {
            when (preference) {
                is EditTextPreference -> {
                    val editText = EditTextPreferenceDialogFragmentCompat.newInstance(preference.key)
                    editText.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
                    editText.setTargetFragment(this, 0)
                    editText.isCancelable = true
                    editText.show(fragmentManager!!, null)




                }
                is ListPreference -> {
                    val list = ListPreferenceDialogFragmentCompat.newInstance(preference.key)
                    list.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
                    list.setTargetFragment(this, 0)
                    list.isCancelable = true
                    list.show(fragmentManager!!, null)
                }
                else -> {

                    super.onDisplayPreferenceDialog(preference)
                }
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
