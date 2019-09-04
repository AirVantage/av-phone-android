package com.sierrawireless.avphone.task

import com.crashlytics.android.Crashlytics
import net.airvantage.model.AirVantageException
import net.airvantage.model.User
import net.airvantage.utils.IAirVantageClient
import java.io.IOException
import java.net.UnknownHostException
import java.util.*

class UserClient internal constructor(private val client: IAirVantageClient) : IUserClient {
    override val user: User?
        @Throws(IOException::class, AirVantageException::class)
        get() {
            return try {
                this.client.currentUser
            } catch (e: Exception) {
                Crashlytics.log("Couldn't get user name")
                Crashlytics.logException(e)
                null
            }

        }

    @Throws(AirVantageException::class)
    override fun checkRights(): List<String> {
        var requiredRights = ArrayList(listOf("entities.applications.view",
                "entities.applications.create", "entities.applications.edit", "entities.systems.view",
                "entities.systems.create", "entities.systems.edit", "entities.alerts.rule.view",
                "entities.alerts.rule.create.edit.delete"))
        try {
            val rights = client.userRights
            requiredRights.removeAll(rights)
        } catch (e: UnknownHostException) {
            requiredRights.clear()
            requiredRights =  ArrayList(listOf("No Connection"))
        }
        catch (e: Exception) {
            Crashlytics.log("Couldn't get user rights for user " + user.toString())
            Crashlytics.logException(e)
            requiredRights.clear()
            requiredRights =  ArrayList(listOf("No Connection"))
        }
        return requiredRights
    }

//    companion object {
//        private val TAG = UserClient::class.simpleName
//    }

}
