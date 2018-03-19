package com.sierrawireless.avphone.task

import android.util.Log
import com.sierrawireless.avphone.model.AvPhoneApplication
import com.sierrawireless.avphone.model.AvPhoneObject
import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvSystem
import net.airvantage.model.User
import net.airvantage.utils.IAirVantageClient
import net.airvantage.utils.Utils
import java.io.IOException
import java.util.*

class ApplicationClient internal constructor(private val client: IAirVantageClient) : IApplicationClient {
    private var currentUser: User? = null
    private var mPhoneName: String? = null

    private val application: Application?
        @Throws(IOException::class, AirVantageException::class)
        get() {
            val applications = client.getApplications(AvPhoneApplication.appType(currentUsername, mPhoneName!!))
            return Utils.first(applications)
        }

    private val currentUsername: String
        get() {
            if (currentUser == null) {
                try {
                    currentUser = client.currentUser
                } catch (e: IOException) {
                    return ""
                } catch (e: AirVantageException) {
                    return ""
                }

            }
            return currentUser!!.email!!
        }

    @Throws(IOException::class, AirVantageException::class)
    override fun ensureApplicationExists(phoneName: String): Application {
        mPhoneName = phoneName
        var application = application

        if (application == null) {
            application = createApplication(phoneName)
            setApplicationCommunication(application.uid!!)
        }
        Log.i(TAG, "ensureApplicationExists: application is " + application.uid)
        return application
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun setApplicationData(applicationUid: String, obj: AvPhoneObject) {
        val data = AvPhoneApplication.createApplicationData(obj)
        client.setApplicationData(applicationUid, data)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createApplication(phoneName: String): Application {
        val application = AvPhoneApplication.createApplication(currentUsername, phoneName)
        return client.createApplication(application)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun setApplicationCommunication(applicationUid: String) {
        val protocols = AvPhoneApplication.createProtocols()
        client.setApplicationCommunication(applicationUid, protocols)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun addApplication(system: AvSystem, application: Application) {
        client.updateSystem(systemWithApplication(system, application))
    }

    private fun systemWithApplication(system: AvSystem, appToAdd: Application): AvSystem {

        val res = AvSystem()
        res.uid = system.uid
        res.type = system.type
        res.applications = ArrayList()

        var appAlreadyLinked = false

        if (system.applications != null) {
            for (systemApp in system.applications!!) {
                val resApp = Application()
                resApp.uid = systemApp.uid
                res.applications!!.add(resApp)
                if (resApp.uid == appToAdd.uid) {
                    appAlreadyLinked = true
                }
            }
        }
        if (!appAlreadyLinked) {
            val addedApp = Application()
            addedApp.uid = appToAdd.uid
            res.applications!!.add(addedApp)
        }

        return res

    }

    companion object {
        private val TAG = ApplicationClient::class.simpleName
    }
}
