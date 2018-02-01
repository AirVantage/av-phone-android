package com.sierrawireless.avphone.task

import android.util.Log
import com.crashlytics.android.Crashlytics
import net.airvantage.model.AirVantageException
import net.airvantage.model.User
import net.airvantage.utils.IAirVantageClient
import java.util.*

class UserClient internal constructor(private val client: IAirVantageClient) : IUserClient {

    override val user: User?
        get() {
            return try {
                this.client.currentUser
            } catch (e: Exception) {
                Crashlytics.logException(e)
                Log.e(LOGTAG, "Could not get user name", e)
                null
            }

        }

    @Throws(AirVantageException::class)
    override fun checkRights(): List<String> {

        val requiredRights = ArrayList(Arrays.asList("entities.applications.view",
                "entities.applications.create", "entities.applications.edit", "entities.systems.view",
                "entities.systems.create", "entities.systems.edit", "entities.alerts.rule.view",
                "entities.alerts.rule.create.edit.delete"))

        try {
            val rights = client.userRights
            requiredRights.removeAll(rights)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(LOGTAG, "Could not get user rights", e)
        }

        return requiredRights

    }

    companion object {

        private val LOGTAG = UserClient::class.java.name
    }
}
