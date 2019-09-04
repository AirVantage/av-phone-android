package com.sierrawireless.avphone.tools

import android.annotation.SuppressLint
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.model.AvPhoneApplication
import com.sierrawireless.avphone.model.AvPhoneData
import java.util.*

object Tools {
    const val NAME = "name"
    const val VALUE = "value"

    @SuppressLint("DefaultLocale")
    fun buildSerialNumber(serial: String, type: String, deviceName:String): String {
        return (serial + "-ANDROID-" + type + "-" + deviceName.replace(" ", "_")).toUpperCase()
    }

//    fun dp2px(context: Context): Float {
//        val scale = context.resources.displayMetrics.density
//        return 90 * scale + 0.5f
//    }

    private fun ClosedRange<Int>.random() =
            (Random().nextInt(endInclusive - start) +  start).toLong()

    fun rand(min:Int, max:Int):Long {
        return (min..max).random()
    }

    fun buildAlertName():String {
        val objectsManager = ObjectsManager.getInstance()
        val systemType = objectsManager.savedObjectName
        return systemType + " " + AvPhoneApplication.ALERT_RULE_NAME + " on " + DeviceInfo.deviceName + "..."
    }

    fun buildDefaultPath(name:String, pos:Int):String = name + "." + AvPhoneData.CUSTOM + pos.toString()

    fun buildSystemName(deviceName: String, userName: String, type: String) = "$type for $deviceName ($userName)"

}
