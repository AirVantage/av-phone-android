package com.sierrawireless.avphone.task


import net.airvantage.model.AvError
import net.airvantage.model.AvSystem
import net.airvantage.model.User

class AlarmStateResult {

    var error: AvError? = null

    var alarmOn: Boolean? = null

    val isError: Boolean
        get() = error != null

    constructor(error: AvError) {
        this.error = error
    }

    constructor(alarmOn: Boolean) {
        this.alarmOn = alarmOn
    }
}
