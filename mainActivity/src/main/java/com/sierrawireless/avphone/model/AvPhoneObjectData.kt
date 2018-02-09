package com.sierrawireless.avphone.model

import android.text.TextUtils
import com.sierrawireless.avphone.tools.Tools


class AvPhoneObjectData(var name: String, var unit: String, var defaults: String, mode: Mode, private val label: String) {
    var mode: Mode = Mode.None
    var current: Int? = null
    private var min: Int? = null
    private var max: Int? = null


    val isInteger: Boolean
        get() = !defaults.isEmpty() && TextUtils.isDigitsOnly(defaults)

    enum class Mode {
        None,
        UP,
        DOWN,
        RANDOM
    }

    init {
        if (isInteger) {
            this.mode = mode
            current = Integer.parseInt(defaults)
        } else {
            if (mode == Mode.RANDOM) {
                val value = defaults.split(",")
                if (value.size == 2) {
                    if (!value[0].isEmpty() && TextUtils.isDigitsOnly(value[0]) &&
                            !value[1].isEmpty() && TextUtils.isDigitsOnly(value[1])) {
                        min = Integer.parseInt(value[0])
                        max = Integer.parseInt(value[1])
                        this.mode = mode
                        current = Tools.rand(min!!, max!!).toInt()
                    } else {
                        this.mode = Mode.None
                    }
                } else {
                    this.mode = Mode.None
                }
            }else{
                this.mode = Mode.None
            }
        }
    }

    override fun toString(): String {
        var returned = "{"
        returned = "$returned\"name\": \"$name\","
        returned = "$returned\"unit\": \"$unit\","
        returned = "$returned\"defaults\": \"$defaults\","
        returned = returned + "\"mode\": \"" + modeToString(mode) + "\","
        returned = "$returned\"label\": \"$label\"}"
        return returned
    }

    fun execMode(): String {
        return when {
            this.mode == Mode.UP -> increment()
            this.mode == Mode.DOWN -> decrement()
            this.mode == Mode.RANDOM -> random()
            else -> defaults
        }
    }

    private fun modeToString(mode: Mode): String {
        return when (mode) {
            AvPhoneObjectData.Mode.UP -> "Increase indefinitely"
            AvPhoneObjectData.Mode.DOWN -> "Decrease to zero"
            AvPhoneObjectData.Mode.None -> "None"
            AvPhoneObjectData.Mode.RANDOM -> "Random"
        }
    }

    private fun increment(): String {
        if (current == null) {
            current = Integer.parseInt(defaults)
        }
        current = current!! + 1
        return current!!.toString()
    }

    private fun decrement(): String {
        if (current == null) {
            current = Integer.parseInt(defaults)
        }
        if (current!! > 0)
            current = current!! - 1
        else if (current == 0) {
            current = Integer.parseInt(defaults)

        }
        return current!!.toString()
    }

    private fun random(): String {
        current =  Tools.rand(min!!, max!!).toInt()
        return current!!.toString()
    }

    fun modePosition(): Int {
        return when (mode) {
            AvPhoneObjectData.Mode.None -> 0
            AvPhoneObjectData.Mode.UP -> 1
            AvPhoneObjectData.Mode.DOWN -> 2
            AvPhoneObjectData.Mode.RANDOM -> 3
        }
    }

    companion object {

        fun modeFromPosition(position: Int): Mode {
            when (position) {
                0 -> return Mode.None
                1 -> return Mode.UP
                2 -> return Mode.DOWN
                3 -> return Mode.RANDOM
            }
            return Mode.None
        }
    }


}
