package com.sierrawireless.avphone.task

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.alert.v1.AlertRule

interface IAlertRuleClient {

    @Throws(IOException::class, AirVantageException::class)
    fun getAlertRule(serialNumber: String, Application: String): AlertRule?

    @Throws(IOException::class, AirVantageException::class)
    fun createAlertRule(Application: String)


}
