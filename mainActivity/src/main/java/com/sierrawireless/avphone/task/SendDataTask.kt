package com.sierrawireless.avphone.task

import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import com.crashlytics.android.Crashlytics
import com.sierrawireless.avphone.service.LogMessage
import java.util.*

typealias SendDataListener = (SendDataResult) -> Unit
class SendDataTask : AsyncTask<SendDataParams, Void, SendDataResult>() {


    private val syncListeners = ArrayList<SendDataListener>()


    fun addProgressListener(listener: SendDataListener) {
        this.syncListeners.add(listener)
    }

    override fun doInBackground(vararg params: SendDataParams): SendDataResult {
        var lastLog: String

        val sendParams = params[0]
        val client = sendParams.client
        val data = sendParams.data
        val alarm = sendParams.alarm
        val context = sendParams.context
        val value = sendParams.value
        try {
            if (!client!!.isConnected) {
                client.connect()
            }

            if (!alarm)
            // dispatch new data event to update the activity UI
                LocalBroadcastManager.getInstance(context).sendBroadcast(data!!)

            client.push(data!!)
            lastLog = if (!alarm)
                data.size().toString() + " data pushed to the server"
            else if (value)
                "Alarm on sent to server"
            else
                "Alarm off sent to server"

            LocalBroadcastManager.getInstance(context).sendBroadcast(LogMessage(lastLog, alarm))
            return SendDataResult(lastLog)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            lastLog = "ERROR: " + e.message
            LocalBroadcastManager.getInstance(context).sendBroadcast(LogMessage(lastLog, alarm))
            return SendDataResult(lastLog, true)
        }


    }

    override fun onPostExecute(result: SendDataResult) {
        super.onPostExecute(result)
        for (listener in syncListeners) {
            listener.invoke(result)
        }
    }

}
