package com.sierrawireless.avphone.model

import android.util.Log
import com.sierrawireless.avphone.ObjectsManager
import net.airvantage.model.*
import net.airvantage.model.alert.v1.AlertRule
import net.airvantage.model.alert.v1.Condition
import java.util.*

object AvPhoneApplication {

    private const val TAG = "AvPhoneApplication"
    const val ALERT_RULE_NAME = "AV Phone raised an alert"
    private var objectsManager: ObjectsManager? = null

    fun createApplication(userName: String): Application {
        val application = Application()
        application.name = AvPhoneApplication.appName(userName)
        application.type = AvPhoneApplication.appType(userName)
        application.revision = "0.0.0"
        return application
    }

    fun createProtocols(): List<Protocol> {
        val mqtt = Protocol()
        mqtt.type = "MQTT"
        mqtt.commIdType = "SERIAL"
        return listOf(mqtt)
    }

    fun createApplicationData(customData: ArrayList<AvPhoneObjectData>, `object`: String): List<ApplicationData> {

        Log.d(TAG, "createApplicationData: ************applicationData called")
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

        asset.data.add(Variable(AvPhoneData.ALARM, "Active alarm", "boolean"))

        var pos = 1
        for (data in customData) {
            val type: String = if (data.isInteger!!) {
                "int"
            } else {
                "string"
            }

            asset.data.add(Variable(`object` + "." + AvPhoneData.CUSTOM + pos.toString(), data.name, type))
            pos++
        }

        val c = Command(AvPhoneData.NOTIFY, "Notify")
        val p = Parameter("message", "string")
        c.parameters = listOf(p)

        asset.data.add(c)

        applicationData.data.add(asset)

        return listOf(applicationData)

    }

    private fun appName(userName: String): String {
        objectsManager = ObjectsManager.getInstance()

        return "av_phone_" + objectsManager!!.savecObject.name + "_" + userName
    }

    fun appType(userName: String): String {
        objectsManager = ObjectsManager.getInstance()
        return "av.phone.demo." + objectsManager!!.savecObject.name + userName
    }

    fun createAlertRule(): AlertRule {
        val rule = AlertRule()

        rule.active = true
        rule.name = ALERT_RULE_NAME
        rule.eventType = "event.system.incoming.communication"

        val alarmCondition = Condition()
        alarmCondition.eventProperty = "communication.data.value"
        alarmCondition.eventPropertyKey = AvPhoneData.ALARM
        alarmCondition.operator = "EQUALS"
        alarmCondition.value = "true"

        rule.conditions = listOf(alarmCondition)

        return rule
    }

}