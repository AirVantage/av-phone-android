package net.airvantage.model

import net.airvantage.utils.Predicate


class AvSystem {
    var uid: String? = null
    var name: String? = null
    var type: String? = null
    var state: String? = null
    var gateway: Gateway? = null
    var data: Data? = null
    var applications: MutableList<Application>? = null
    var communication: MutableMap<String, MqttCommunication>? = null

    class Data {
        var rssi: Double? = null
        var rssiLevel: String? = null
        var networkServiceType: String? = null
        var latitude: Double? = null
        var longitude: Double? = null
    }

    class Gateway {
        var uid: String? = null
        var imei: String? = null
        var serialNumber: String? = null
        var type: String? = null
    }

    companion object {


        fun hasSerialNumber(serialNumber: String?): Predicate<AvSystem> {
            return  { item -> serialNumber != null && serialNumber == item.gateway!!.serialNumber }
        }
    }


}
