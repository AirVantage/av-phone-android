package com.sierrawireless.avphone.model

import java.util.ArrayList

class AvPhoneObject {
    var name: String? = null
    var systemUid: String? = null

    var alarm: Boolean = false
    var datas: ArrayList<AvPhoneObjectData> = ArrayList()

    fun add(data: AvPhoneObjectData) {
        datas.add(data)
    }

    override fun toString(): String {
        val returned = StringBuilder("{")
        returned.append("\"name\" : \"").append(name).append("\",")
        returned.append("\"alarm\" : ").append(alarm).append(",")

        returned.append("\"datas\":[")
        for (data in datas) {
            returned.append(data.toString()).append(",")
        }
        returned.append("]}")
        return returned.toString()
    }

    fun exec() {
        for (data in datas) {
            data.execMode()
        }
    }

}
