package com.sierrawireless.avphone.task


class SendDataResult {
    var isError: Boolean = false
        private set
    var lastLog: String? = null
        private set


    constructor(lastLog: String, error: Boolean) {
        this.isError = error
        this.lastLog = lastLog
    }

    constructor(lastLog: String) {
        this.lastLog = lastLog
        this.isError = false
    }
}
