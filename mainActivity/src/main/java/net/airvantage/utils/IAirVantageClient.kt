package net.airvantage.utils

import java.io.IOException

import net.airvantage.model.AirVantageException
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.model.Application
import net.airvantage.model.ApplicationData
import net.airvantage.model.AvSystem
import net.airvantage.model.Protocol
import net.airvantage.model.User
import net.airvantage.model.UserRights

interface IAirVantageClient {

    val userRights: UserRights

    val currentUser: User

    @Throws(IOException::class, AirVantageException::class)
    fun createAlertRule(alertRule: AlertRule, application: String, system: AvSystem)

    @Throws(IOException::class, AirVantageException::class)
    fun updateAlertRule(alertRule: AlertRule, application: String, system: AvSystem)

    @Throws(IOException::class, AirVantageException::class)
    fun getAlertRuleByName(name: String, system: AvSystem): AlertRule?

    @Throws(IOException::class, AirVantageException::class)
    fun deleteAlertRule(alertRule: AlertRule)

    @Throws(IOException::class, AirVantageException::class)
    fun setApplicationData(applicationUid: String, data: List<ApplicationData>)

    @Throws(IOException::class, AirVantageException::class)
    fun getApplications(appType: String): List<Application>?

    @Throws(IOException::class, AirVantageException::class)
    fun createApplication(application: Application): Application

    @Throws(IOException::class, AirVantageException::class)
    fun setApplicationCommunication(applicationUid: String, protocols: List<Protocol>)

    @Throws(IOException::class, AirVantageException::class)
    fun updateSystem(system: AvSystem)

    @Throws(IOException::class, AirVantageException::class)
    fun logout()
}
