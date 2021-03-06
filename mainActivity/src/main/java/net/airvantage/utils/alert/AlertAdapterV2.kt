package net.airvantage.utils.alert

import android.net.Uri
import android.util.Log
import com.google.gson.JsonIOException
import net.airvantage.model.AirVantageException
import net.airvantage.model.AvSystem
import net.airvantage.model.alert.v2.AlertRuleList
import net.airvantage.model.alert.v2.alertrule.AlertRule
import net.airvantage.model.alert.v2.alertrule.AttributeId
import net.airvantage.model.alert.v2.alertrule.Condition
import net.airvantage.model.alert.v2.alertrule.Operand
import net.airvantage.utils.Utils
import java.io.IOException
import java.io.InputStreamReader
import java.io.Serializable
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class AlertAdapterV2 internal constructor(server: String, accessToken: String) : DefaultAlertAdapter(server, accessToken) {

    private var alertRuleUrl: URL? = null

    override val prefix: String
        get() = "/api/v2/"

    @Throws(IOException::class, AirVantageException::class)
    override fun getAlertRuleByName(name: String, system:AvSystem): net.airvantage.model.alert.v1.AlertRule? {
        try {
            val inp = get(alertRuleUrl())
            val rules = gson.fromJson(InputStreamReader(inp), AlertRuleList::class.java)
            val alertRuleV2 = Utils.firstWhere(rules, AlertRule.isNamed(name))
            if (alertRuleV2 != null) {
                return convert(alertRuleV2)
            }
        } catch (e: JsonIOException) {
            Log.e(TAG, "Unable to read Alert Rules", e)
        }

        return null
    }

    @Throws(IOException::class, AirVantageException::class)
    override fun createAlertRule(alertRule: net.airvantage.model.alert.v1.AlertRule, application: String, system: AvSystem) {
        try {
            var alertRuleV2: AlertRule? = AlertRule()
            alertRuleV2!!.targetType = "SYSTEM"
            alertRuleV2.name = alertRule.name
            alertRuleV2.message = "Alarm for " + system.name + " is ON"
            alertRuleV2.active = true
            alertRuleV2.conditions = ArrayList()
            val metadata = HashMap<String, Any>()
            metadata["templateId"] = "alertrule.template.custom"

            val condi = HashMap<String, String>()
            condi["application"] = application
            metadata["condition_0"] = condi
            alertRuleV2.metadata = metadata
            for (condition in alertRule.conditions!!) {
                alertRuleV2.conditions!!.add(convert(condition))
            }
            val inp = post(alertRuleUrl(), alertRuleV2)
            alertRuleV2 = gson.fromJson(InputStreamReader(inp), AlertRule::class.java)
            convert(alertRuleV2)

        } catch (e: JsonIOException) {
            Log.e(TAG, "Unable to create Alert Rule", e)
        }

    }

    @Throws(IOException::class, AirVantageException::class)
    override fun updateAlertRule(alertRule: net.airvantage.model.alert.v1.AlertRule, application: String, system: AvSystem) {
        try {
            var alertRuleV2: AlertRule? = AlertRule()
            alertRuleV2!!.targetType = "SYSTEM"
            alertRuleV2.name = alertRule.name
            alertRuleV2.message = "Alarm for " + system.name + " is ON"
            alertRuleV2.active = true
            alertRuleV2.conditions = ArrayList()
            val metadata = HashMap<String, Any>()
            metadata["templateId"] = "alertrule.template.custom"

            val condi = HashMap<String, String>()
            condi["application"] = application
            metadata["condition_0"] = condi
            alertRuleV2.metadata = metadata
            for (condition in alertRule.conditions!!) {
                alertRuleV2.conditions!!.add(convert(condition))
            }
            val inp = put(alertRuleUrl("/" + alertRule.uid), alertRuleV2)
            alertRuleV2 = gson.fromJson(InputStreamReader(inp), AlertRule::class.java)
            convert(alertRuleV2)

        } catch (e: JsonIOException) {
            Log.e(TAG, "Unable to create Alert Rule", e)
        }

    }

    @Throws(IOException::class, AirVantageException::class)
    override fun deleteAlertRule(alertRule: net.airvantage.model.alert.v1.AlertRule) {
        delete(alertRuleUrl("/" + alertRule.uid))
    }


    @Throws(IOException::class)
    private fun alertRuleUrl(data:String? = null): URL {

        if (alertRuleUrl != null && data != null)
            return alertRuleUrl!!

        val apiPath = "alertrules"

        val urlString = if (data == null) {
            Uri.parse(buildEndpoint(apiPath)).toString()
        }else{
            Uri.parse(buildEndpoint(apiPath + data)).toString()
        }
        val tmp: URL?
        try {
            tmp = URL(urlString)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Sure of URL?", e)
            throw e
        }
        if (data != null) {
            alertRuleUrl = tmp
        }

        return tmp
    }

    companion object {
        private const val TAG = "AlertAdapterV2"

        private fun convert(condition: net.airvantage.model.alert.v1.Condition): Condition {

            val conditionV2 = Condition()
            conditionV2.operator = condition.operator

            val leftOperand = Operand()
            leftOperand.attributeId = AttributeId()
            if (condition.eventProperty == "communication.data.value") {
                leftOperand.attributeId!!.name = "DATA." + condition.eventPropertyKey
            }else{
                leftOperand.attributeId!!.name = condition.eventPropertyKey
            }
            val rightOperand = Operand()
            rightOperand.valueStr = condition.value

            conditionV2.operands = ArrayList(Arrays.asList(leftOperand, rightOperand))
            return conditionV2
        }

        private fun convert(alertRule: AlertRule): net.airvantage.model.alert.v1.AlertRule {
            val alertRuleV1: net.airvantage.model.alert.v1.AlertRule = net.airvantage.model.alert.v1.AlertRule()

            // Assign similar fields
            alertRuleV1.uid = alertRule.id
            alertRuleV1.active = alertRule.active
            alertRuleV1.name = alertRule.name
            // alertRuleV1.metadata = alertRule.metadata;

            // Only event created in AV Phone
            alertRuleV1.eventType = "event.system.incoming.communication"

            // Translating conditions
            alertRuleV1.conditions = ArrayList()
            for (condition in alertRule.conditions!!) {
                alertRuleV1.conditions!!.add(convert(condition))
            }
            return alertRuleV1
        }

        private fun convert(condition: Condition): net.airvantage.model.alert.v1.Condition {
            val conditionV1: net.airvantage.model.alert.v1.Condition = net.airvantage.model.alert.v1.Condition()
            conditionV1.eventProperty = "communication.data.value"
           // conditionV1.eventPropertyKey = "phone.alarm"
            conditionV1.operator = condition.operator

            // Finding values
            //Operand operand = Utils.first(condition.operands);
            val values = ArrayList<Serializable>()
            for (operand in condition.operands!!) {
                if (operand.attributeId == null) {
                    if (operand.valueStr != null) values.add(operand.valueStr!!)
                    if (operand.valueNum != null) values.add(operand.valueNum!!)
                }else{
                    if (operand.attributeId!!.name!!.startsWith("DATA.") ) {
                        conditionV1.eventPropertyKey = operand.attributeId!!.name!!.replace("DATA.", "")
                    }
                }
            }//values.add(Utils.first(operand.valuesStr));

            // Trimming nulls
            //   values.removeAll(Collections.singleton(null));
            values.removeAll(setOf(""))

            // Pick first one
            conditionV1.value = if (values.isEmpty()) "" else Utils.first(values).toString()
            return conditionV1
        }
    }
}
