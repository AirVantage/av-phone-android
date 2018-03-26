package com.sierrawireless.avphone.tools

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.sierrawireless.avphone.activity.MainActivity
import org.jetbrains.anko.toast

object DeviceInfo {

    /** Returns the consumer friendly device name  */
     val deviceName: String?
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.toUpperCase().startsWith(manufacturer.toUpperCase())) {
                capitalize(model)
            } else capitalize(manufacturer) + " " + model
        }

    private fun capitalize(str: String): String? {
        if (TextUtils.isEmpty(str)) {
            return str
        }
        val arr = str.toCharArray()
        var capitalizeNext = true

        val phrase = StringBuilder()
        for (c in arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c))
                capitalizeNext = false
                continue
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true
            }
            phrase.append(c)
        }

        return phrase.toString()
    }

    /**
     * Serial number is in `syncParams.deviceId`, why not using it?
     *
     * - It is not available running emulator
     * - It is not available to our iOs counterpart
     *
     * So to be AV Phone iOs compatible and able to run emulator, we use: uppercase(userUid + "-" + systemType)
     */
    @SuppressLint("DefaultLocale")
    fun generateSerial(userUid: String): String {
        return userUid
    }

    @SuppressLint("DefaultLocale")
     fun getUniqueId(context: Context): String? {

        return (context as? MainActivity)?.systemSerial

    }


    @SuppressLint("HardwareIds", "MissingPermission")
    fun getIMEI(context: Context): String? {

        val telManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var rc:String? = null
        try {
            rc = if (telManager.phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                @Suppress("DEPRECATION")
                telManager.deviceId
            } else null
        }catch(e:SecurityException) {
            MainActivity.instance.runOnUiThread {
                MainActivity.instance.toast("Read Phone Permission not given")
            }
        }

        return rc
    }

    fun getICCID(context: Context): String {
        val sm = SubscriptionManager.from(context)
        val sis = sm.activeSubscriptionInfoList
        var rc = ""
        try {
            rc = if (sis != null) {
                val si = sis[0]
                si.iccId
            } else {
                ""
            }
        }catch(e:SecurityException) {
            MainActivity.instance.runOnUiThread {
                MainActivity.instance.toast("Read Phone Permission not given")
            }
        }

        return rc


    }
}
