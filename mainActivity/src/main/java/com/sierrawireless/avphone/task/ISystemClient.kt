package com.sierrawireless.avphone.task

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.AvSystem

interface ISystemClient {

    @Throws(IOException::class, AirVantageException::class)
    fun getSystem(serialNumber: String, type: String): AvSystem?

    @Throws(IOException::class, AirVantageException::class)
    fun deleteSystem(system: AvSystem)

    @Throws(IOException::class, AirVantageException::class)
    fun createSystem(serialNumber: String, iccid: String, type: String, mqttPassword: String, applicationUid: String, deviceName: String, userName: String, imei: String): AvSystem

}
