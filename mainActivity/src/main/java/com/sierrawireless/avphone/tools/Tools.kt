package com.sierrawireless.avphone.tools

import android.content.Context
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.model.AvPhoneApplication
import java.util.*

object Tools {
    const val NAME = "name"
    const val VALUE = "value"

    fun buildSerialNumber(serial: String, type: String): String {
        return (serial + "-ANDROID-" + type).toUpperCase()
    }

    fun dp2px(context: Context): Float {
        val scale = context.resources.displayMetrics.density
        return 90 * scale + 0.5f
    }

    fun ClosedRange<Int>.random() =
            (Random().nextInt(endInclusive - start) +  start).toLong()

    fun rand(min:Int, max:Int):Long {
        return (min..max).random()
    }

    fun buildAlertName():String {
        var objectsManager = ObjectsManager.getInstance()
        var systemType = objectsManager.savedObjectName
        return systemType + " " + AvPhoneApplication.ALERT_RULE_NAME + " on " + DeviceInfo.deviceName + "..."
    }
}
