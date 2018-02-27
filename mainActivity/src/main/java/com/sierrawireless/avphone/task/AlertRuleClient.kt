package com.sierrawireless.avphone.task

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.utils.IAirVantageClient

import com.sierrawireless.avphone.model.AvPhoneApplication
import com.sierrawireless.avphone.tools.Tools
import net.airvantage.model.AvSystem

class AlertRuleClient internal constructor(private val client: IAirVantageClient) : IAlertRuleClient {

    @Throws(IOException::class, AirVantageException::class)
    override fun getAlertRule(serialNumber: String, system: AvSystem): AlertRule? {
        return client.getAlertRuleByName(Tools.buildAlertName(), system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createAlertRule(Application: String, system: AvSystem) {
        val alertRule = AvPhoneApplication.createAlertRule(system)
        client.createAlertRule(alertRule, Application, system)
    }


    @Throws(IOException::class, AirVantageException::class)
    override fun updateAlertRule(Application: String, system: AvSystem, alertRule: AlertRule) {
        AvPhoneApplication.updateAlertRule(system, alertRule)
        client.updateAlertRule(alertRule, Application, system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun deleteAlertRule(alert: AlertRule) {
        client.deleteAlertRule(alert)
    }



}
