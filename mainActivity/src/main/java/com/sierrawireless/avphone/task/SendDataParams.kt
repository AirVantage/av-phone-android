package com.sierrawireless.avphone.task

import android.content.Context

import com.sierrawireless.avphone.service.MqttPushClient
import com.sierrawireless.avphone.service.NewData

class SendDataParams {
    var client: MqttPushClient? = null
    var data: NewData? = null
    var alarm: Boolean = false
    var value: Boolean = false
    var context: Context? = null

}
