package com.sierrawireless.avphone.task


class SendDataResult {
    var isError: Boolean = false
        private set
    var lastLog: String? = null
        private set
    var alarmLog: String? = null
        private set



    constructor(lastLog: String?, alarmLog: String?, error: Boolean) {
        this.isError = error
        this.lastLog = lastLog
        this.alarmLog = alarmLog
    }

    constructor(lastLog: String?, alarmLog: String?) {
        this.lastLog = lastLog
        this.isError = false
        this.alarmLog = alarmLog
    }
}
