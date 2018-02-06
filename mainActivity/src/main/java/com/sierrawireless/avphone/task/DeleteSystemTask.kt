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
import net.airvantage.model.AvError
import java.io.IOException
import java.util.*

typealias DeleteSystemListenerAlias = (DeleteSystemResult) -> Unit

open class DeleteSystemTask internal constructor(private val systemClient: ISystemClient, private val userClient: IUserClient, @field:SuppressLint("StaticFieldLeak")
protected val context: Context) : AvPhoneTask<Void, DeleteSystemProgress, DeleteSystemResult>() {
    private val syncListeners = ArrayList<DeleteSystemListenerAlias>()


    fun addProgressListener(listener: DeleteSystemListenerAlias) {
        this.syncListeners.add(listener)
    }


    @SuppressLint("DefaultLocale")
    override fun doInBackground(vararg params: Void): DeleteSystemResult {

        try {

            publishProgress(DeleteSystemProgress.CHECKING_RIGHTS)

            val missingRights = userClient.checkRights()
            if (!missingRights.isEmpty()) {
                return DeleteSystemResult(AvError(AvError.MISSING_RIGHTS, missingRights))
            }

            val systemType: String
            val user = userClient.user

            val objectsManager = ObjectsManager.getInstance()

            systemType = objectsManager.savedObjectName!!

            // For emulator and iOs compatibility sake, using generated serial.
            val serialNumber = DeviceInfo.generateSerial(user!!.uid!!)

            // Save Device serial in context
            if (context is MainActivity) {
                context.systemSerial = serialNumber
            }


            publishProgress(DeleteSystemProgress.CHECKING_SYSTEM)
            val system = this.systemClient.getSystem(serialNumber, systemType)
            if (system != null) {
                publishProgress(DeleteSystemProgress.DELETING_SYSTEM)
                systemClient.deleteSystem(system)
            }
            publishProgress(DeleteSystemProgress.DONE)
            return DeleteSystemResult(user)


        } catch (e: AirVantageException) {
            publishProgress(DeleteSystemProgress.DONE)
            return DeleteSystemResult(e.error!!)
        } catch (e: IOException) {
            Crashlytics.logException(e)
            Log.e(MainActivity::class.java.name, "Error when trying to synchronize with server", e)
            publishProgress(DeleteSystemProgress.DONE)
            return DeleteSystemResult(AvError("unkown.error"))
        }

    }

    override fun onPostExecute(result: DeleteSystemResult) {
        super.onPostExecute(result)
        for (listener in syncListeners) {
            listener.invoke(result)
        }
    }

    fun showResult(result: DeleteSystemResult, displayer: IMessageDisplayer, context: Activity) {

        if (result.isError) {
            val error = result.error!!
            displayTaskError(error, displayer, context, userClient, "")

        } else {
            displayer.showSuccess(R.string.sync_success)
        }
    }
}

