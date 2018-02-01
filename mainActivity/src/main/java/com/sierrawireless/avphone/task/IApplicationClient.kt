package com.sierrawireless.avphone.task

import java.io.IOException
import java.util.ArrayList

import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvSystem

import com.sierrawireless.avphone.model.AvPhoneObjectData

interface IApplicationClient {

    @Throws(IOException::class, AirVantageException::class)
    fun ensureApplicationExists(phoneName:String): Application

    @Throws(IOException::class, AirVantageException::class)
    fun setApplicationData(applicationUid: String, customData: ArrayList<AvPhoneObjectData>, `object`: String)

    @Throws(IOException::class, AirVantageException::class)
    fun createApplication(phoneName: String): Application

    @Throws(IOException::class, AirVantageException::class)
    fun setApplicationCommunication(applicationUid: String)

    @Throws(IOException::class, AirVantageException::class)
    fun addApplication(system: AvSystem, application: Application)

}