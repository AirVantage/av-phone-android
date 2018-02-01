package com.sierrawireless.avphone.tools

import android.content.Context

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
}
