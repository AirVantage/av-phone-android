package com.sierrawireless.avphone.task

import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.sierrawireless.avphone.service.LogMessage
import org.eclipse.paho.client.mqttv3.MqttException
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
            return if (!alarm) {
                SendDataResult(lastLog, null)
            }else{
                SendDataResult(null, lastLog)
            }
        }catch (e: MqttException) {
            lastLog = "MQTT ERROR: " + when (e.reasonCode.toShort()){
                MqttException.REASON_CODE_BROKER_UNAVAILABLE -> "The broker was not available to handle the request"
                MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED -> "The client is already disconnected."
                MqttException.REASON_CODE_CLIENT_CLOSED -> "The client is closed - no operations are permitted on the client in this state"
                MqttException.REASON_CODE_CLIENT_CONNECTED -> "The client is already connected"
                MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED -> "Thrown when an attempt to call MqttClient.disconnect() has been made from within a method on MqttCallback"
                MqttException.REASON_CODE_CLIENT_DISCONNECTING -> "The client is currently disconnecting and cannot accept any new work"
                MqttException.REASON_CODE_CLIENT_EXCEPTION -> "Client encountered an exception, can't connect to server. port closed by firewall ???"
                MqttException.REASON_CODE_CLIENT_NOT_CONNECTED -> "The client is not connected to the server"
                MqttException.REASON_CODE_CLIENT_TIMEOUT -> "Client timed out while waiting for a response from the server"
                MqttException.REASON_CODE_CONNECT_IN_PROGRESS -> "A connect operation in already in progress, only one connect can happen at a time"
                MqttException.REASON_CODE_CONNECTION_LOST -> "The client has been unexpectedly disconnected from the server"
                MqttException.REASON_CODE_FAILED_AUTHENTICATION -> "Authentication with the server has failed, due to a bad user name or password"
                MqttException.REASON_CODE_INVALID_CLIENT_ID -> "The server has rejected the supplied client ID"
                MqttException.REASON_CODE_INVALID_MESSAGE -> "Protocol error: the message was not recognized as a valid MQTT packet"
                MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION -> "The protocol version requested is not supported by the server"
                MqttException.REASON_CODE_MAX_INFLIGHT -> "A request has been made to send a message but the maximum number of inflight messages has already been reached"
                MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE -> "Internal error, caused by no new message IDs being available"
                MqttException.REASON_CODE_NOT_AUTHORIZED -> "Not authorized to perform the requested operation"
                MqttException.REASON_CODE_SERVER_CONNECT_ERROR -> "Unable to connect to server"
                MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH -> "Server URI and supplied SocketFactory do not match"
                MqttException.REASON_CODE_SSL_CONFIG_ERROR -> "SSL configuration error"
                MqttException.REASON_CODE_SUBSCRIBE_FAILED -> "Error from subscribe - returned from the server"
                MqttException.REASON_CODE_TOKEN_INUSE -> "A request has been made to use a token that is already associated with another action"
                MqttException.REASON_CODE_UNEXPECTED_ERROR -> "An unexpected error has occurred"
                MqttException.REASON_CODE_WRITE_TIMEOUT -> "Client timed out while waiting to write messages to the server"
                else -> "Unkown reason code "+ e.reasonCode
            }
            Log.w("SendDataTasK", "Mqtt error " + lastLog )
            LocalBroadcastManager.getInstance(context).sendBroadcast(LogMessage(lastLog, alarm))
            return if (!alarm) {
                SendDataResult(lastLog, null,true)
            }else{
                SendDataResult(null, lastLog, true)
            }
        } catch (e: Exception) {
            Crashlytics.logException(e)
            lastLog = "ERROR: " + e.message
            LocalBroadcastManager.getInstance(context).sendBroadcast(LogMessage(lastLog, alarm))
            return if (!alarm) {
                SendDataResult(lastLog, null,true)
            }else{
                SendDataResult(null, lastLog, true)
            }
        }
    }

    override fun onPostExecute(result: SendDataResult) {
        super.onPostExecute(result)
        for (listener in syncListeners) {
            listener.invoke(result)
        }
    }

}
