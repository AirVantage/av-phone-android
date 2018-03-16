package com.sierrawireless.avphone.service

import android.content.Intent

class LogMessage(message: String, alarm: Boolean) : Intent(LOG_EVENT) {

    val message: String
        get() = this.getStringExtra(LOG)
    val alarm: Boolean
        get() = this.getBooleanExtra(ALARM, false)

    init {

        this.putExtra(LOG, message)
        this.putExtra(ALARM, alarm)
    }

    companion object {

        const val LOG_EVENT = "com.sierrawireless.avphone.event.log"

        // keys used for broadcasting log events
        const val LOG = "log"
        private const val ALARM = "alarm"
    }

}
