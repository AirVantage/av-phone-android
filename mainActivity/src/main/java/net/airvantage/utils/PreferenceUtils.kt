package net.airvantage.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.auth.Authentication
import com.sierrawireless.avphone.model.CustomDataLabels
import java.io.IOException
import java.util.*

object PreferenceUtils {

    private val TAG = PreferenceUtils::class.simpleName

    private const val DEFAULT_COMM_PERIOD = "2"
    const val PREF_SERVER_KEY = "pref_server_key"
    const val PREF_CLIENT_ID_KEY = "pref_client_id_key"
    const val PREF_PASSWORD_KEY = "pref_password_key"
    const val PREF_PERIOD_KEY = "pref_period_key"
    private const val PREF_ACCESS_TOKEN = "pref_access_token"
    private const val PREF_TOKEN_EXPIRES_AT = "pref_token_expires_at"
    private var properties: Properties? = null

    enum class Server {
        NA, EU, CUSTOM
    }

    fun getAvPhonePrefs(context: Context): AvPhonePrefs {
        val res = AvPhonePrefs()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        res.serverHost = prefs.getString(PREF_SERVER_KEY, context.getString(R.string.pref_server_na_value))

        res.clientId = prefs.getString(PREF_CLIENT_ID_KEY, getNaClientId(context))

        val serverMapping = HashMap<String, Server>()
        serverMapping[context.getString(R.string.pref_server_eu_value)] = Server.EU
        serverMapping[context.getString(R.string.pref_server_na_value)] = Server.NA
        serverMapping[context.getString(R.string.pref_server_custom_value)] = Server.CUSTOM
        res.usesServer = serverMapping[res.serverHost!!]

        res.password = prefs.getString(PREF_PASSWORD_KEY, context.getString(R.string.pref_password_default))
        res.period = prefs.getString(PREF_PERIOD_KEY, DEFAULT_COMM_PERIOD)

        return res
    }

    private fun getPropertiesFile(context: Context): Properties? {
        if (properties == null) {
            try {
                val `in` = context.assets.open("avphone.properties")
                properties = Properties()
                properties!!.load(`in`)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return properties
    }

    private fun getNaClientId(context: Context): String {
        val properties = getPropertiesFile(context)
        return properties!!.getProperty("clientid.na")
    }

    private fun getEuClientId(context: Context): String {
        val properties = getPropertiesFile(context)
        return properties!!.getProperty("clientid.eu")
    }

    private fun getCustomClientId(context: Context): String? {
        val properties = getPropertiesFile(context)
        return properties!!.getProperty("clientid.custom")
    }

    fun isCustomDefined(context: Context): Boolean {
        return getCustomClientId(context) != null
    }

    private fun getPreference(context: Context, prefKeyId: Int, defaultValueKeyId: Int): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(prefKeyId)
        val defaultValueKey = context.getString(defaultValueKeyId)
        return prefs.getString(prefKey, defaultValueKey)
    }

    private fun setPreference(context: Context, prefKey: String, value: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(prefKey, value).apply()
    }

    fun showMissingPrefsDialog(activity: Activity) {
        AlertDialog.Builder(activity).setTitle(R.string.invalid_prefs).setMessage(R.string.prefs_missing)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    // do nothing
                }.show()
    }

    fun getCustomDataLabels(context: Context): CustomDataLabels {
        val labels = CustomDataLabels()
        labels.customUp1Label = getPreference(context, R.string.pref_custom1_label_key,
                R.string.pref_custom1_label_default)
        labels.customUp2Label = getPreference(context, R.string.pref_custom2_label_key,
                R.string.pref_custom2_label_default)
        labels.customDown1Label = getPreference(context, R.string.pref_custom3_label_key,
                R.string.pref_custom3_label_default)
        labels.customDown2Label = getPreference(context, R.string.pref_custom4_label_key,
                R.string.pref_custom4_label_default)
        labels.customStr1Label = getPreference(context, R.string.pref_custom5_label_key,
                R.string.pref_custom5_label_default)
        labels.customStr2Label = getPreference(context, R.string.pref_custom6_label_key,
                R.string.pref_custom6_label_default)

        return labels
    }

    fun setServer(server: Server, context: Context) {
        when (server) {
            PreferenceUtils.Server.NA -> {
                setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_na_value))
                setPreference(context, PREF_CLIENT_ID_KEY, getNaClientId(context))
            }
            PreferenceUtils.Server.EU -> {
                setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_eu_value))
                setPreference(context, PREF_CLIENT_ID_KEY, getEuClientId(context))
            }
            PreferenceUtils.Server.CUSTOM -> {
                setPreference(context, PREF_SERVER_KEY, context.getString(R.string.pref_server_custom_value))
                setPreference(context, PREF_CLIENT_ID_KEY, getCustomClientId(context))
            }
        }
    }

    fun saveAuthentication(context: Context, auth: Authentication) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit().putString(PREF_ACCESS_TOKEN, auth.accessToken)
                .putLong(PREF_TOKEN_EXPIRES_AT, auth.expirationDate!!.time).apply()
    }

    fun resetAuthentication(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit().remove(PREF_ACCESS_TOKEN).remove(PREF_TOKEN_EXPIRES_AT).apply()

    }

    fun readAuthentication(context: Context): Authentication? {

        var auth: Authentication? = null

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val accessToken = prefs.getString(PREF_ACCESS_TOKEN, null)

        var expiresAtMs: Long? = null

        try {
            expiresAtMs = prefs.getLong(PREF_TOKEN_EXPIRES_AT, 0)
        } catch (e: ClassCastException) {
            // An earlier version might have stored the token as a string
            var expiresAtSt: String? = null
            try {
                expiresAtSt = prefs.getString(PREF_TOKEN_EXPIRES_AT, null)
                if (expiresAtSt != null) {
                    expiresAtMs = java.lang.Long.parseLong(expiresAtSt)
                }
            } catch (nfe: NumberFormatException) {
                // The string was not even a valid one, we'll ignore it.
                Log.w(TAG, "pref_token_expires_at stored as invalid String : '$expiresAtSt'", nfe)
            }

        }

        if (accessToken != null && expiresAtMs != null && (expiresAtMs) != 0L) {
            val expiresAt = Date(expiresAtMs)
            auth = Authentication()
            auth.accessToken = accessToken
            auth.expirationDate = expiresAt
        }

        return auth
    }

}
