package net.airvantage.utils

class AvPhonePrefs {

    var serverHost: String? = null
    var clientId: String? = null

    var password: String? = null
    var period: String? = null

    var usesServer: PreferenceUtils.Server? = null

    fun checkCredentials(): Boolean {
        return !(password == null || password!!.isEmpty() || serverHost == null || serverHost!!.isEmpty())
    }

    fun usesNA(): Boolean {
        return usesServer == PreferenceUtils.Server.NA
    }

    fun usesEU(): Boolean {
        return usesServer == PreferenceUtils.Server.EU
    }
}
