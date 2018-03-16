package com.sierrawireless.avphone.task

import com.sierrawireless.avphone.tools.Tools
import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvSystem
import net.airvantage.model.MqttCommunication
import net.airvantage.utils.AirVantageClient
import net.airvantage.utils.Utils
import java.io.IOException
import java.util.HashMap
import kotlin.collections.ArrayList
import kotlin.collections.set

class SystemClient internal constructor(private val client: AirVantageClient) : ISystemClient {

    @Throws(IOException::class, AirVantageException::class)
    override fun getSystem(serialNumber: String, type: String, deviceName: String): net.airvantage.model.AvSystem? {
        val systems = client.getSystemsBySerialNumber(Tools.buildSerialNumber(serialNumber, type, deviceName))

        return Utils.firstWhere(systems, AvSystem.hasSerialNumber(Tools.buildSerialNumber(serialNumber, type, deviceName)))
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createSystem(serialNumber: String, iccid: String, type: String, mqttPassword: String,
                              applicationUid: String, deviceName: String, userName: String, imei: String): net.airvantage.model.AvSystem {
        val system = net.airvantage.model.AvSystem()

        val uid = client.getGateway((serialNumber + "-ANDROID-" + type + "-" + deviceName.replace(" ", "_")).toUpperCase())
        val gateway = net.airvantage.model.AvSystem.Gateway()

        if (uid == null) {
            gateway.serialNumber = (serialNumber + "-ANDROID-" + type + "-" +  deviceName.replace(" ", "_")).toUpperCase()
            // gateway.imei = imei + type;
            gateway.type = type
        } else {
            gateway.uid = uid
        }
        system.name = Tools.buildSystemName(deviceName, userName, type)

        system.gateway = gateway

        system.state = "READY"

        val application = Application()
        application.uid = applicationUid
        val tmp = ArrayList<Application>()
        tmp.add(application)
        system.applications = tmp

        val mqtt = MqttCommunication()
        mqtt.password = mqttPassword

        system.communication = HashMap()
        system.communication!!["mqtt"] = mqtt
        system.type = type

        return client.createSystem(system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun updateSystem(system: AvSystem, serialNumber: String, iccid: String, type: String, mqttPassword: String,
                              applicationUid: String, deviceName: String, userName: String, imei: String) {


        val uid = client.getGateway((serialNumber + "-ANDROID-" + type + "-" + deviceName.replace(" ", "_")).toUpperCase())
        val gateway = net.airvantage.model.AvSystem.Gateway()

        if (uid == null) {
            gateway.serialNumber = (serialNumber + "-ANDROID-" + type + "-" +  deviceName.replace(" ", "_")).toUpperCase()
            // gateway.imei = imei + type;
            gateway.type = type
        } else {
            gateway.uid = uid
        }
        system.name = Tools.buildSystemName(deviceName, userName, type)

        system.gateway = gateway

        system.state = "READY"

        val application = Application()
        application.uid = applicationUid
        val tmp = ArrayList<Application>()
        tmp.add(application)
        system.applications = tmp

        val mqtt = MqttCommunication()
        mqtt.password = mqttPassword

        system.communication = HashMap()
        system.communication!!["mqtt"] = mqtt
        system.type = type

        client.updateSystem(system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun deleteSystem(system: net.airvantage.model.AvSystem) {
        client.deleteSystem(system)
    }
}
