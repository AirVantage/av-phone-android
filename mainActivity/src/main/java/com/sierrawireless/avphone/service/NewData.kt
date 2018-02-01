package com.sierrawireless.avphone.service

import android.content.Intent
import android.os.Bundle
import com.sierrawireless.avphone.ObjectsManager

class NewData internal constructor() : Intent(NEW_DATA) {

    var rssi: Int?
        get() = if (this.extras == null) null else (this.extras!!.get(RSSI_KEY) as Int?)

        internal set(rssi) {
            if (rssi != null && rssi < 0) {
                this.putExtra(RSSI_KEY, rssi.toInt())
            }
        }

    var rsrp: Int?
    get() = if (this.extras == null) null else (this.extras!!.get(RSRP_KEY) as Int?)
    internal set(rsrp) {
        if (rsrp != null && rsrp < 0) {
            this.putExtra(RSRP_KEY, rsrp.toInt())
        }
    }

    var operator: String?
    get() = if (this.extras == null) null else (this.extras!!.get(OPERATOR_KEY) as String?)
    internal set(operator) {
        if (operator != null) {
            this.putExtra(OPERATOR_KEY, operator)
        }
    }

    var imei: String?
    get() = if (this.extras == null) null else (this.extras!!.get(IMEI_KEY) as String?)
    set(imei) {
        if (imei != null) {
            this.putExtra(IMEI_KEY, imei)
        }
    }

    var networkType: String?
    get() = if (this.extras == null) null else (this.extras!!.get(NETWORK_TYPE_KEY) as String?)
    internal set(networkType) {
        if (networkType != null) {
            this.putExtra(NETWORK_TYPE_KEY, networkType)
        }
    }

    val isWifiActive: Boolean?
    get() = if (this.extras == null) null else (this.extras!!.get(ACTIVE_WIFI_KEY) as Boolean?)

    var batteryLevel: Float?
    get() =if (this.extras == null) null else (this.extras!!.get(BATTERY_KEY) as Float?)
    internal set(batteryLevel) {
        if (batteryLevel != null && batteryLevel > 0f) {
            this.putExtra(BATTERY_KEY, batteryLevel)
        }

    }

    var latitude: Double?
    get() = if (this.extras == null) null else (this.extras!!.get(LATITUDE_KEY) as Double?)
    internal set(latitude) {
        if (latitude != null) {
            this.putExtra(LATITUDE_KEY, latitude.toDouble())
        }
    }

    var longitude: Double?
    get() = if (this.extras == null) null else (this.extras!!.get(LONGITUDE_KEY) as Double?)
    internal set(longitude) {
        if (longitude != null) {
            this.putExtra(LONGITUDE_KEY, longitude.toDouble())
        }
    }

    var bytesReceived: Long?
    get() = if (this.extras == null) null else (this.extras!!.get(BYTES_RECEIVED_KEY) as Long?)
    internal set(received) {
        if (received != null && received > 0L) {
            this.putExtra(BYTES_RECEIVED_KEY, received.toLong())
        }
    }

    var bytesSent: Long?
    get() = if (this.extras == null) null else (this.extras!!.get(BYTES_SENT_KEY) as Long?)
    internal set(sent) {
        if (sent != null && sent > 0L) {
            this.putExtra(BYTES_SENT_KEY, sent.toLong())
        }
    }

    var runningApps: Int?
    get() = if (this.extras == null) null else (this.extras!!.get(RUNNING_APPS_KEY) as Int?)
    internal set(nbApps) {
        if (nbApps != null && nbApps > 0) {
            this.putExtra(RUNNING_APPS_KEY, nbApps.toInt())
        }
    }

    var memoryUsage: Float?
    get() = if (this.extras == null) null else (this.extras!!.get(MEMORY_USAGE_KEY) as Float?)
    internal set(memoryUsage) {
        if (memoryUsage != null && memoryUsage > 0f) {
            this.putExtra(MEMORY_USAGE_KEY, memoryUsage)
        }
    }

    var androidVersion: String?
    get() = if (this.extras == null) null else (this.extras!!.get(ANDROID_VERSION_KEY) as String?)
    internal set(version) {
        if (version != null) {
            this.putExtra(ANDROID_VERSION_KEY, version)
        }
    }

    internal var isAlarmActivated: Boolean?
    get() = if (this.extras == null) null else (this.extras!!.get(ALARM_KEY) as Boolean?)
    set(value) {
        this.putExtra(ALARM_KEY, value)
    }


    init {
        this.putExtras(Bundle())
    }

    internal fun setActiveWifi(activeWifi: Boolean?) {
        if (activeWifi != null) {
            this.putExtra(ACTIVE_WIFI_KEY, activeWifi)
        }
    }

    fun size(): Int {
        return this.extras!!.size()
    }

    internal fun setCustom() {
        val objectsManager = ObjectsManager.getInstance()
        val `object` = objectsManager.currentObject
        var pos: Int? = 1
        for (data in `object`!!.datas) {
            if (data.isInteger!!) {
                this.putExtra(`object`.name + "." + CUSTOM + pos!!.toString(), Integer.parseInt(data.execMode()))
            } else {
                this.putExtra(`object`.name + "." + CUSTOM + pos!!.toString(), data.defaults)
            }
            pos++
        }
    }

    companion object {

        const val NEW_DATA = "com.sierrawireless.avphone.newdata"

        // keys used for broadcasting new data events
        private const val NEW_DATA_PREFIX = "newdata."
        private const val RSSI_KEY = NEW_DATA_PREFIX + "rssi"
        private const val RSRP_KEY = NEW_DATA_PREFIX + "rsrp"
        private const val BATTERY_KEY = NEW_DATA_PREFIX + "battery"
        private const val OPERATOR_KEY = NEW_DATA_PREFIX + "operator"
        private const val NETWORK_TYPE_KEY = NEW_DATA_PREFIX + "networktype"
        private const val IMEI_KEY = NEW_DATA_PREFIX + "imei"
        private const val LATITUDE_KEY = NEW_DATA_PREFIX + "latitude"
        private const val LONGITUDE_KEY = NEW_DATA_PREFIX + "longitude"
        private const val BYTES_SENT_KEY = NEW_DATA_PREFIX + "bytessent"
        private const val BYTES_RECEIVED_KEY = NEW_DATA_PREFIX + "bytesreceived"
        private const val ACTIVE_WIFI_KEY = NEW_DATA_PREFIX + "activewifi"
        private const val RUNNING_APPS_KEY = NEW_DATA_PREFIX + "runningapps"
        private const val MEMORY_USAGE_KEY = NEW_DATA_PREFIX + "memory"
        private const val ANDROID_VERSION_KEY = NEW_DATA_PREFIX + "androidversion"

        private const val ALARM_KEY = NEW_DATA_PREFIX + "alarm"

        private const val CUSTOM = NEW_DATA_PREFIX + "custom."
    }
}
