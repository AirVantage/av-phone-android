package com.sierrawireless.avphone.model

import android.text.Html
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.tools.Tools
import net.airvantage.model.*
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.model.alert.v1.Condition

object AvPhoneApplication {

    const val ALERT_RULE_NAME = "raised an alert"
    private var objectsManager: ObjectsManager? = null

    fun createApplication(userName: String, phoneName:String): Application {
        val application = Application()
        application.name = AvPhoneApplication.appName(userName, phoneName)
        application.type = AvPhoneApplication.appType(userName, phoneName)
        application.revision = "0.0.0"
        return application
    }

    fun createProtocols(): List<Protocol> {
        val mqtt = Protocol()
        mqtt.type = "MQTT"
        mqtt.commIdType = "SERIAL"
        return listOf(mqtt)
    }

    fun createApplicationData(obj: AvPhoneObject): List<ApplicationData> {

        // <data>
        // <encoding type="MQTT">
        // <asset default-label="Android Phone" id="phone">
        // <setting default-label="RSSI" path="rssi" type="int"/>
        // <setting default-label="Service type" path="service" type="string"/>
        // <setting default-label="Operator" path="operator" type="string"/>
        // <setting default-label="Latitude" path="latitude"type="double"/>
        // <setting default-label="Longitude" path="longitude" type="double"/>
        // <setting default-label="Battery level" path="batterylevel" type="double"/>
        // <setting default-label="Bytes received"path="bytesreceived" type="double"/>
        // <setting default-label= "Bytes sent" path="bytessent" type="double"/>
        // <setting default-label= "Memory usage" path="memoryusage" type="double"/>
        // <setting default-label="Running applications" path="runningapps" type="int"/>
        // <setting default-label= "Active Wi-Fi" path="activewifi" type="boolean"/>
        //
        // <command default-label="Notify" path="notify" > <parameter id="message" type="string" /></command>
        // </asset>
        // </encoding>
        // </data>

        val applicationData = ApplicationData()
        applicationData.id = "0"
        applicationData.label = "AV Phone Demo"
        applicationData.encoding = "MQTT"
        applicationData.elementType = "node"
        applicationData.data = ArrayList()

        val asset = Data("phone", "Phone", "node")
        asset.data = ArrayList()

        asset.data!!.add(Variable(obj.alarmName, "Active alarm", "boolean"))

        var pos = 1
        for (data in obj.datas) {
            val type: String = if (data.mode != AvPhoneObjectData.Mode.None) {
                "int"
            } else {
                "string"
            }

            if (data.path == null) {
                asset.data!!.add(Variable(obj.name!! + "." + AvPhoneData.CUSTOM + pos.toString(), data.name, type))
            }else{
                asset.data!!.add(Variable(data.path!!, data.name, type))
            }
            pos++
        }

        val c = Command(AvPhoneData.NOTIFY, "Notify")
        val p = Parameter("message", "string")
        c.parameters = listOf(p)

        asset.data!!.add(c)

        applicationData.data!!.add(asset)

        return listOf(applicationData)

    }

    private fun appName(userName: String, phoneName: String): String {
        objectsManager = ObjectsManager.getInstance()

        return Html.escapeHtml(phoneName.replace(" ", "_") + "_av_phone_" + objectsManager!!.savecObject.name + "_" + userName)
    }

    fun appType(userName: String, phoneName: String): String {
        objectsManager = ObjectsManager.getInstance()
        return Html.escapeHtml(phoneName.replace(" ", ".") + ".av.phone.demo." + objectsManager!!.savecObject.name + userName)
    }

    fun createAlertRule(system: AvSystem, name: String): AlertRule {
        val rule = AlertRule()

        rule.active = true
        rule.name = Tools.buildAlertName()
        rule.eventType = "event.system.incoming.communication"

        val alarmCondition = Condition()
        alarmCondition.eventProperty = "communication.data.value"
        alarmCondition.eventPropertyKey = name
        alarmCondition.operator = "EQUALS"
        alarmCondition.value = "true"

        val systemCondition = Condition()
        systemCondition.eventProperty = "system.data.value"
        systemCondition.eventPropertyKey = "system.id"
        systemCondition.operator = "EQUALS"
        systemCondition.value = system.uid


        val tmp = ArrayList<Condition>()
        tmp.add(alarmCondition)
        tmp.add(systemCondition)
        rule.conditions = tmp

        return rule
    }

    fun updateAlertRule(system: AvSystem, alertRule: AlertRule, name: String) {

        alertRule.active = true
        alertRule.name = Tools.buildAlertName()
        alertRule.eventType = "event.system.incoming.communication"

        val alarmCondition = Condition()
        alarmCondition.eventProperty = "communication.data.value"
        alarmCondition.eventPropertyKey = name
        alarmCondition.operator = "EQUALS"
        alarmCondition.value = "true"

        val systemCondition = Condition()
        systemCondition.eventProperty = "system.data.value"
        systemCondition.eventPropertyKey = "system.id"
        systemCondition.operator = "EQUALS"
        systemCondition.value = system.uid


        val tmp = ArrayList<Condition>()
        tmp.add(alarmCondition)
        tmp.add(systemCondition)
        alertRule.conditions = tmp
    }

}
