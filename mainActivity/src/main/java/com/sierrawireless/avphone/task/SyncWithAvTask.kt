package com.sierrawireless.avphone.task

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.sierrawireless.avphone.tools.DeviceInfo
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.message.IMessageDisplayer
import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvError
import net.airvantage.model.AvSystem
import org.jetbrains.anko.longToast
import java.io.IOException
import java.util.*


typealias SyncWithAvListener = (SyncWithAvResult) -> Unit

open class SyncWithAvTask internal constructor(private val applicationClient: IApplicationClient, private val systemClient: ISystemClient,
                                               private val alertRuleClient: IAlertRuleClient, private val userClient: IUserClient, @field:SuppressLint("StaticFieldLeak")
                                               protected val context: Context) : AvPhoneTask<SyncWithAvParams, SyncProgress, SyncWithAvResult>() {

    private val syncListeners = ArrayList<SyncWithAvListener>()
    var deviceName:String? = null

    fun addProgressListener(listener: SyncWithAvListener) {
        this.syncListeners.add(listener)
    }

    @SuppressLint("DefaultLocale")
    override fun doInBackground(vararg params: SyncWithAvParams): SyncWithAvResult {

        try {

            publishProgress(SyncProgress.CHECKING_RIGHTS)


            val systemType: String?
            val syncParams = params[0]
            val user = userClient.user
            val imei = syncParams.imei!!
            val iccid = syncParams.iccid!!
            deviceName = syncParams.deviceName!!
            val mqttPassword = syncParams.mqttPassword!!
            val objectsManager = ObjectsManager.getInstance()

            val missingRights = userClient.checkRights()
            if (!missingRights.isEmpty()) {
                return SyncWithAvResult(AvError(AvError.MISSING_RIGHTS, missingRights))
            }


            systemType = objectsManager.savedObjectName

            // For emulator and iOs compatibility sake, using generated serial.
            val serialNumber = DeviceInfo.generateSerial(user?.uid!!)

            // Save Device serial in context
            if (context is MainActivity) {
                context.systemSerial = serialNumber
            }

            publishProgress(SyncProgress.CHECKING_APPLICATION)

            val application = this.applicationClient.ensureApplicationExists(deviceName!!)

            publishProgress(SyncProgress.CHECKING_SYSTEM)

            var system: net.airvantage.model.AvSystem? = this.systemClient.getSystem(serialNumber, systemType!!, deviceName!!)
            if (system == null) {

                publishProgress(SyncProgress.CREATING_SYSTEM)
                val uid = application.uid!!
                var name = if (user.name == null){
                    "Nobody"
                }else{
                    user.name!!
                }


                system = systemClient.createSystem(serialNumber, iccid, systemType, mqttPassword, uid, deviceName!!, name, imei)
            }
            objectsManager.savecObject.systemUid = system.uid
            objectsManager.saveOnPref()


            publishProgress(SyncProgress.CHECKING_ALERT_RULE)

            val alertRule = this.alertRuleClient.getAlertRule(serialNumber, system)
            if (alertRule == null) {

                publishProgress(SyncProgress.CREATING_ALERT_RULE)

                this.alertRuleClient.createAlertRule(application.uid!!, system, objectsManager.savecObject.alarmName)
            }else{
                if (alertRule.conditions == null || ! alertRule.conditions!![0].eventPropertyKey.equals(objectsManager.savecObject.alarmName)) {
                    this.alertRuleClient.updateAlertRule(application.uid!!, system, alertRule, objectsManager.savecObject.alarmName)
                }
            }

            publishProgress(SyncProgress.UPDATING_APPLICATION)

            this.applicationClient.setApplicationData(application.uid!!, objectsManager.savecObject)

            if (!hasApplication(system, application)) {

                publishProgress(SyncProgress.ADDING_APPLICATION)

                this.applicationClient.addApplication(system, application)
            }

            publishProgress(SyncProgress.DONE)

            return SyncWithAvResult(system, user)

        } catch (e: AirVantageException) {
            publishProgress(SyncProgress.DONE)
            return SyncWithAvResult(e.error!!)
        } catch (e: IOException) {
            Crashlytics.logException(e)
            Log.e(MainActivity::class.java.name, "Error when trying to synchronize with server", e)
            publishProgress(SyncProgress.DONE)
            return SyncWithAvResult(AvError("unkown.error"))
        }

    }

    override fun onPostExecute(result: SyncWithAvResult) {
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

    fun showResult(result: SyncWithAvResult, displayer: IMessageDisplayer, context: Activity) {

        if (result.isError) {
            var error = result.error
            if (error == null) {
                error = AvError("Internal Error")
            }
            if (error.errorParameters.size == 1 && error.errorParameters[0] == "No Connection") {
                context.longToast("Resync error\nYou don't have any data connection")
            }

            displayTaskError(error, displayer, context, userClient, deviceName!!)
        } else {
            displayer.showSuccess(R.string.sync_success)
        }
    }

}
