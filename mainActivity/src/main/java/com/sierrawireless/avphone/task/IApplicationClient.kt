package com.sierrawireless.avphone.task

import com.sierrawireless.avphone.model.AvPhoneObject
import net.airvantage.model.AirVantageException
import net.airvantage.model.Application
import net.airvantage.model.AvSystem
import java.io.IOException

interface IApplicationClient {

    @Throws(IOException::class, AirVantageException::class)
    fun ensureApplicationExists(phoneName:String): Application

    @Throws(IOException::class, AirVantageException::class)
    fun setApplicationData(applicationUid: String, obj: AvPhoneObject)

    @Throws(IOException::class, AirVantageException::class)
    fun createApplication(phoneName: String): Application

    @Throws(IOException::class, AirVantageException::class)
    fun setApplicationCommunication(applicationUid: String)

    @Throws(IOException::class, AirVantageException::class)
    fun addApplication(system: AvSystem, application: Application)
}