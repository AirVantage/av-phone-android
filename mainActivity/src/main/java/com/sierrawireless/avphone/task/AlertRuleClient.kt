package com.sierrawireless.avphone.task

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.utils.IAirVantageClient

import com.sierrawireless.avphone.model.AvPhoneApplication

class AlertRuleClient internal constructor(private val client: IAirVantageClient) : IAlertRuleClient {

    @Throws(IOException::class, AirVantageException::class)
    override fun getAlertRule(serialNumber: String, Application: String): AlertRule? {
        val alertRuleName = AvPhoneApplication.ALERT_RULE_NAME
        return client.getAlertRuleByName(alertRuleName, Application)
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createAlertRule(Application: String) {
        val alertRule = AvPhoneApplication.createAlertRule()
        client.createAlertRule(alertRule, Application)
    }


}
