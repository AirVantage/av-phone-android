package com.sierrawireless.avphone.task


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.message.IMessageDisplayer
import net.airvantage.model.AirVantageException
import net.airvantage.model.AvError
import java.util.*

typealias GetUserListener = (GetUserResult) -> Unit

open class GetUserTask internal constructor(private val userClient: IUserClient, @field:SuppressLint("StaticFieldLeak")
protected val context: Context) : AvPhoneTask<GetUserParams, Void, GetUserResult>() {


    private val syncListeners = ArrayList<GetUserListener>()

    fun addProgressListener(listener: GetUserListener) {
        this.syncListeners.add(listener)
    }

    @SuppressLint("DefaultLocale")
    override fun doInBackground(vararg params: GetUserParams): GetUserResult {

        try {


            val missingRights = userClient.checkRights()
            if (!missingRights.isEmpty()) {
                return GetUserResult(AvError(AvError.MISSING_RIGHTS, missingRights))
            }

            val user = userClient.user!!

            return GetUserResult(user)
        } catch (e: AirVantageException) {

            return GetUserResult(e.error)
        }

    }

    override fun onPostExecute(result: GetUserResult) {
        super.onPostExecute(result)
        for (listener in syncListeners) {
            listener.invoke(result)
        }
    }

    fun showResult(result: GetUserResult, displayer: IMessageDisplayer, context: Activity) {

        if (result.isError) {
            val error = result.error
            displayTaskError(error!!, displayer, context, userClient)

        } else {
            displayer.showSuccess(R.string.sync_success)
        }
    }
}
