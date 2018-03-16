package net.airvantage.utils.alert

import android.net.Uri
import net.airvantage.model.AirVantageException
import net.airvantage.model.AvSystem
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.model.alert.v1.AlertRuleList
import net.airvantage.utils.Utils
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL


class AlertAdapterV1 internal constructor(server: String, accessToken: String) : DefaultAlertAdapter(server, accessToken) {

    override  val prefix: String
        get() = "/api/v1/"


    @Throws(IOException::class, AirVantageException::class)
    override fun getAlertRuleByName(name: String, system:AvSystem): AlertRule? {

        val str = Uri.parse(buildEndpoint(API_PATH))
                .buildUpon()
                .appendQueryParameter("access_token", access_token)
                .appendQueryParameter("fields", "uid,name")
                .appendQueryParameter("name", name)
                .build()
                .toString()

        val url = URL(str)
        val `in` = get(url)
        val rules = gson.fromJson(InputStreamReader(`in`), AlertRuleList::class.java)
        return Utils.firstWhere(rules.items, AlertRule.isNamed(name))
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createAlertRule(alertRule: AlertRule, application: String, system:AvSystem) {
        val url = URL(buildEndpoint(API_PATH))
        val `in` = post(url, alertRule)
        gson.fromJson(InputStreamReader(`in`), AlertRule::class.java)
    }



    @Throws(IOException::class, AirVantageException::class)
    override fun updateAlertRule(alertRule: AlertRule, application: String, system:AvSystem) {
        val url = URL(buildEndpoint(API_PATH + "/" + alertRule.uid))
        val `in` = put(url, alertRule)
        gson.fromJson(InputStreamReader(`in`), AlertRule::class.java)
    }
    override fun deleteAlertRule(alertRule: AlertRule) {

        val url = URL(buildEndpoint(API_PATH + alertRule.uid))
        delete(url)

    }

    companion object {

        private const val API_PATH = "/alerts/rules"
    }

}
