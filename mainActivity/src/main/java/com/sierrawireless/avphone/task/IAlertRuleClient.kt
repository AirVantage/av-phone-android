package com.sierrawireless.avphone.task

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.AvSystem
import net.airvantage.model.alert.v1.AlertRule

interface IAlertRuleClient {

    @Throws(IOException::class, AirVantageException::class)
    fun getAlertRule(serialNumber: String, system: AvSystem): AlertRule?

    @Throws(IOException::class, AirVantageException::class)
    fun getAlertState(uid: String): Boolean

    @Throws(IOException::class, AirVantageException::class)
    fun createAlertRule(Application: String, system: AvSystem, name:String)

    @Throws(IOException::class, AirVantageException::class)
    fun updateAlertRule(Application: String, system: AvSystem, alertRule: AlertRule, name:String)


    @Throws(IOException::class, AirVantageException::class)
    fun deleteAlertRule(alert:AlertRule)

}
