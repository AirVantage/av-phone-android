package com.sierrawireless.avphone.task

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.tools.DeviceInfo
import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvError
import net.airvantage.model.AvSystem
import java.io.IOException
import java.util.ArrayList

typealias UpdateListener = (UpdateResult) -> Unit

open class UpdateTask internal constructor(private val applicationClient: IApplicationClient, private val systemClient: ISystemClient,
                                               private val alertRuleClient: IAlertRuleClient, private val userClient: IUserClient, @field:SuppressLint("StaticFieldLeak")
                                               protected val context: Context) : AvPhoneTask<UpdateParams, UpdateProgress, UpdateResult>() {

    private val syncListeners = ArrayList<UpdateListener>()
    var deviceName:String? = null

    fun addProgressListener(listener: UpdateListener) {
        this.syncListeners.add(listener)
    }

    @SuppressLint("DefaultLocale")
    override fun doInBackground(vararg params: UpdateParams): UpdateResult {

        try {

            publishProgress(UpdateProgress.CHECKING_RIGHTS)

            val missingRights = userClient.checkRights()
            if (!missingRights.isEmpty()) {
                return UpdateResult(AvError(AvError.MISSING_RIGHTS, missingRights))
            }

            val systemType: String?
            val syncParams = params[0]
            val user = userClient.user
            val imei = syncParams.imei
            val iccid = syncParams.iccid
            deviceName = syncParams.deviceName
            val mqttPassword = syncParams.mqttPassword
            val objectsManager = ObjectsManager.getInstance()

            systemType = objectsManager.savedObjectName

            // For emulator and iOs compatibility sake, using generated serial.
            val serialNumber = DeviceInfo.generateSerial(user!!.uid!!)

            // Save Device serial in context
            if (context is MainActivity) {
                context.systemSerial = serialNumber
            }

            publishProgress(UpdateProgress.CHECKING_APPLICATION)

            val application = this.applicationClient.ensureApplicationExists(deviceName!!)

            publishProgress(UpdateProgress.CHECKING_SYSTEM)

            var system: net.airvantage.model.AvSystem? = this.systemClient.getSystem(serialNumber, systemType!!, deviceName!!)
            publishProgress(UpdateProgress.UPDATING_SYSTEM)

            if (system != null) {
                systemClient.updateSystem(system, serialNumber, iccid!!, systemType, mqttPassword!!, application.uid!!, deviceName!!, user.name!!, imei!!)
            }else{
                system = systemClient.createSystem(serialNumber, iccid!!, systemType, mqttPassword!!, application.uid!!, deviceName!!, user.name!!, imei!!)
            }

            objectsManager.savecObject.systemUid = system.uid
            objectsManager.saveOnPref()

            publishProgress(UpdateProgress.CHECKING_ALERT_RULE)

            val alertRule = this.alertRuleClient.getAlertRule(serialNumber, system)
            publishProgress(UpdateProgress.UPDATING_ALERT_RULE)

            if (alertRule != null) {
                this.alertRuleClient.updateAlertRule(application.uid!!, system, alertRule)
            }else{
                this.alertRuleClient.createAlertRule(application.uid!!, system)
            }

            publishProgress(UpdateProgress.UPDATING_APPLICATION)

            this.applicationClient.setApplicationData(application.uid!!, objectsManager.savecObject.datas, objectsManager.savecObject.name!!)

            if (!hasApplication(system, application)) {

                publishProgress(UpdateProgress.ADDING_APPLICATION)

                this.applicationClient.addApplication(system, application)
            }

            publishProgress(UpdateProgress.DONE)

            return UpdateResult(system, user)

        } catch (e: AirVantageException) {
            publishProgress(UpdateProgress.DONE)
            return UpdateResult(e.error!!)
        } catch (e: IOException) {
            Crashlytics.logException(e)
            Log.e(MainActivity::class.java.name, "Error when trying to synchronize with server", e)
            publishProgress(UpdateProgress.DONE)
            return UpdateResult(AvError("unkown.error"))
        }
    }

    override fun onPostExecute(result: UpdateResult) {
        super.onPostExecute(result)
        for (listener in syncListeners) {
            listener.invoke(result)
        }
    }

    private fun hasApplication(system: AvSystem, application: Application): Boolean {
        var found = false
        if (system.applications != null) {
            system.applications!!
                    .filter { it.uid == application.uid }
                    .forEach { found = true }
        }
        return found
    }

    fun showResult(result: UpdateResult, name: String,  displayer: IMessageDisplayer, context: Activity) {

        if (result.isError) {
            var error = result.error
            if (error == null) {
                error = AvError("Internal Error")
            }
            displayTaskError(error, displayer, context, userClient, deviceName!!)
        } else {
            displayer.showSuccess(name + " updated with AirVantage")
        }
    }

}
