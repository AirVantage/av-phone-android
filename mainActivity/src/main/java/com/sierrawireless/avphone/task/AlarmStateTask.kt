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
import org.jetbrains.anko.longToast
import java.io.IOException
import java.util.ArrayList


typealias AlarmStateListener = (AlarmStateResult) -> Unit

open class AlarmStateTask internal constructor(private val applicationClient: IApplicationClient, private val systemClient: ISystemClient,
                                               private val alertRuleClient: IAlertRuleClient, private val userClient: IUserClient, @field:SuppressLint("StaticFieldLeak")
                                               protected val context: Context) : AvPhoneTask<AlarmStateParams, AlarmStateProgress, AlarmStateResult>() {

    private val syncListeners = ArrayList<AlarmStateListener>()
    var deviceName:String? = null

    fun addProgressListener(listener: AlarmStateListener) {
        this.syncListeners.add(listener)
    }

    @SuppressLint("DefaultLocale")
    override fun doInBackground(vararg params: AlarmStateParams): AlarmStateResult {

        try {



            val systemType: String?
            val syncParams = params[0]
            val user = userClient.user
            deviceName = syncParams.deviceName!!
            val objectsManager = ObjectsManager.getInstance()
            var alertOn: Boolean




            systemType = objectsManager.savedObjectName

            // For emulator and iOs compatibility sake, using generated serial.
            val serialNumber = DeviceInfo.generateSerial(user?.uid!!)

            // Save Device serial in context
            if (context is MainActivity) {
                context.systemSerial = serialNumber
            }


            publishProgress(AlarmStateProgress.CHECKING_SYSTEM)
            Log.d("SYNC", "Check System")
            var system: AvSystem? = this.systemClient.getSystem(serialNumber, systemType!!, deviceName!!)
            if (system == null) {
                publishProgress(AlarmStateProgress.DONE)
                return AlarmStateResult(AvError("System not created"))

            }

            publishProgress(AlarmStateProgress.CHECKING_ALERT_RULE)
            Log.d("SYNC", "Alert rule")
            val alertRule = this.alertRuleClient.getAlertRule(serialNumber, system!!)
            if (alertRule == null) {
                publishProgress(AlarmStateProgress.DONE)
                return AlarmStateResult(AvError("Alert Rule not created"))
            }

            //# now try to retrieve the value
            publishProgress(AlarmStateProgress.GETTING_ALERT_STATE)
            alertOn = this.alertRuleClient.getAlertState(alertRule.uid!!)

            publishProgress(AlarmStateProgress.DONE)
            return AlarmStateResult( alertOn)

        } catch (e: AirVantageException) {
            publishProgress(AlarmStateProgress.DONE)
            return AlarmStateResult(e.error!!)
        } catch (e: IOException) {
            Crashlytics.logException(e)
            Log.e(MainActivity::class.java.name, "Error when trying to synchronize with server", e)
            publishProgress(AlarmStateProgress.DONE)
            return AlarmStateResult(AvError("unkown.error"))
        }

    }

    override fun onPostExecute(result: AlarmStateResult) {
        super.onPostExecute(result)
        for (listener in syncListeners) {
            listener.invoke(result)
        }
    }


    fun showResult(result: AlarmStateResult, displayer: IMessageDisplayer, context: Activity) {

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
