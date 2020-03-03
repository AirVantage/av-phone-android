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

    override fun getAlertState(uid: String): Boolean {
        return client.getAlertState(uid)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createAlertRule(Application: String, system: AvSystem, name:String) {
        val alertRule = AvPhoneApplication.createAlertRule(system, name)
        client.createAlertRule(alertRule, Application, system)
    }


    @Throws(IOException::class, AirVantageException::class)
    override fun updateAlertRule(Application: String, system: AvSystem, alertRule: AlertRule, name:String) {
        AvPhoneApplication.updateAlertRule(system, alertRule, name)
        client.updateAlertRule(alertRule, Application, system)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun deleteAlertRule(alert: AlertRule) {
        client.deleteAlertRule(alert)
    }



}
