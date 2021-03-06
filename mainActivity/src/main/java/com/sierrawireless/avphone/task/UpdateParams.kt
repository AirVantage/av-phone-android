package com.sierrawireless.avphone.task

import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.model.CustomDataLabels

class UpdateParams {
    var deviceId: String? = null
    var imei: String? = null
    var mqttPassword: String? = null
    var iccid: String? = null
    var deviceName: String? = null
    var customData: CustomDataLabels? = null
    var activity: MainActivity? = null

}
