package com.sierrawireless.avphone.service

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.Gson
import com.sierrawireless.avphone.tools.DeviceInfo
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.model.AvPhoneData
import com.sierrawireless.avphone.model.AvPhoneObjectData
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.UnsupportedEncodingException
import java.util.*

class MqttPushClient @SuppressLint("DefaultLocale")
@Throws(MqttException::class)
internal constructor(clientId: String, password: String, serverHost: String, callback: MqttCallback) {
    private val client: MqttClient
    private val opt: MqttConnectOptions
    private val gson = Gson()
    private val objectsManager: ObjectsManager
    val isConnected: Boolean
        get() = client.isConnected

    init {

        DeviceInfo.generateSerial("")
        Log.i(TAG, "new client: $clientId - $password - $serverHost")

        this.client = MqttClient("tcp://$serverHost:1883", MqttClient.generateClientId(),
                MemoryPersistence())
        client.setCallback(callback)

        this.opt = MqttConnectOptions()
        opt.userName = clientId.toUpperCase()
        opt.password = password.toCharArray()
        opt.keepAliveInterval = 30
        objectsManager = ObjectsManager.getInstance()
    }

    @Throws(MqttException::class)
    fun connect() {
        client.connect(opt)
    }

    @Throws(MqttException::class)
    internal fun disconnect() {
        if (client.isConnected) {
            client.disconnect()
        }
    }

    @Throws(MqttException::class)
    fun push(data: NewData) {
        if (client.isConnected) {
            Log.i(TAG, "Pushing data to the server : " + data)
            val message = this.convertToJson(data)
            Log.i(TAG, "push: message " + message)

            var msg: MqttMessage? = null
            try {
                msg = MqttMessage(message.toByteArray(charset("UTF-8")))
            } catch (e: UnsupportedEncodingException) {
                // won't happen, UTF-8 is available
            }

            if (msg != null) {
                msg.qos = 0
            }
            this.client.publish(opt.userName + "/messages/json", msg)
        }
    }

    private fun convertToJson(data: NewData): String {
        val timestamp = System.currentTimeMillis()

        val values = HashMap<String, List<DataValue>>()

        if (data.rssi != null) {
            values["_RSSI"] = listOf(DataValue(timestamp, data.rssi!!))
        }

        if (data.rsrp != null) {
            values["_RSRP"] = listOf(DataValue(timestamp, data.rsrp!!))
        }

        if (data.operator != null) {
            values["_NETWORK_OPERATOR"] = listOf(DataValue(timestamp, data.operator!!))
        }


        if (data.networkType != null) {
            values["_NETWORK_SERVICE_TYPE"] = listOf(DataValue(timestamp, data.networkType!!))
        }

        if (data.latitude != null) {
            values["_LATITUDE"] = listOf(DataValue(timestamp, data.latitude!!))
        }

        if (data.longitude != null) {
            values["_LONGITUDE"] = listOf(DataValue(timestamp, data.longitude!!))
        }

        if (data.bytesReceived != null) {
            values["_BYTES_RECEIVED"] = listOf(DataValue(timestamp, data.bytesReceived!!))
        }

        if (data.bytesSent != null) {
            values["_BYTES_SENT"] = listOf(DataValue(timestamp, data.bytesSent!!))
        }

        if (data.isAlarmActivated != null) {
            values[data.alarmName!!] = listOf(DataValue(timestamp, data.isAlarmActivated!!))
        } else {
            val obj = objectsManager.currentObject
            var pos: Int? = 1
            var key: String?
            for (ldata in obj!!.datas) {
                key = ldata.path ?: obj.name + "." + AvPhoneData.CUSTOM + pos!!.toString()
                if (ldata.mode != AvPhoneObjectData.Mode.None) {
                    values[key] = listOf(DataValue(timestamp, ldata.current!!))
                } else {
                    values[key] = listOf(DataValue(timestamp, ldata.defaults))
                }
                pos = pos!! + 1
            }
        }
        return gson.toJson(listOf<Map<String, List<DataValue>>>(values))
    }

    //timestamp is used when the massage is sent for each entry. don't remove it...
    @Suppress("UNUSED")
    internal inner class DataValue(var timestamp: Long, var value: Any)

    companion object {
        private val TAG = MqttPushClient::class.simpleName
    }
}
