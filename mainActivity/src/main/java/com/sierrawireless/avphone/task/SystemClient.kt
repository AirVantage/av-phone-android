package com.sierrawireless.avphone.task

import android.util.Log

import com.sierrawireless.avphone.tools.Tools

import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvSystem
import net.airvantage.model.MqttCommunication
import net.airvantage.utils.AirVantageClient
import net.airvantage.utils.Utils

import java.io.IOException
import java.util.Collections
import java.util.HashMap

class SystemClient internal constructor(private val client: AirVantageClient) : ISystemClient {

    @Throws(IOException::class, AirVantageException::class)
    override fun getSystem(serialNumber: String, type: String): net.airvantage.model.AvSystem? {
        val systems = client.getSystemsBySerialNumber(Tools.buildSerialNumber(serialNumber, type))

        return Utils.firstWhere(systems, AvSystem.hasSerialNumber(Tools.buildSerialNumber(serialNumber, type)))
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createSystem(serialNumber: String, iccid: String, type: String, mqttPassword: String,
                              applicationUid: String, deviceName: String, userName: String, imei: String): net.airvantage.model.AvSystem {
        val system = net.airvantage.model.AvSystem()

        val exist = client.getGateway((serialNumber + "-ANDROID-" + type).toUpperCase())
        val gateway = net.airvantage.model.AvSystem.Gateway()

        if (!(exist!!)) {
            gateway.serialNumber = (serialNumber + "-ANDROID-" + type).toUpperCase()
            // gateway.imei = imei + type;
            gateway.type = type
        } else {
            gateway.serialNumber = (serialNumber + "-ANDROID-" + type).toUpperCase()
        }
        Log.d(TAG, "createSystem: Gateway exit " + exist!!)
        Log.d(TAG, "gateway is " + (serialNumber + "-ANDROID-" + type).toUpperCase())
        system.name = "$deviceName de $userName($type)"

        system.gateway = gateway

        system.state = "READY"

        val application = Application()
        application.uid = applicationUid
        var tmp = ArrayList<Application>()
        tmp.add(application!!)
        system.applications = tmp

        val mqtt = MqttCommunication()
        mqtt.password = mqttPassword

        system.communication = HashMap()
        system.communication!!["mqtt"] = mqtt
        system.type = type

        return client.createSystem(system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun deleteSystem(system: net.airvantage.model.AvSystem) {
        client.deleteSystem(system)
    }

    companion object {
        private val TAG = "SystemClient"
    }

}
