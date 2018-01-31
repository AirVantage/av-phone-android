package com.sierrawireless.avphone.auth

import java.util.Date

class Authentication {
    var accessToken: String? = null
    var expirationDate: Date? = null

    val isExpired: Boolean
        get() = isExpired(Date())

    fun isExpired(date: Date): Boolean {
        return date.after(this.expirationDate)
    }
}
