package com.sierrawireless.avphone.model

import android.text.TextUtils


class AvPhoneObjectData(var name: String, var unit: String, var defaults: String, mode: Mode, private val label: String) {
    var mode: Mode
    var current: Int? = null


    val isInteger: Boolean
        get() = !defaults.isEmpty() && TextUtils.isDigitsOnly(defaults)

    enum class Mode {
        None,
        UP,
        DOWN
    }

    init {
        if (isInteger) {
            this.mode = mode
            current = Integer.parseInt(defaults)
        } else {
            this.mode = Mode.None
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
        if (this.mode == Mode.UP) {
            return increment()
        } else if (this.mode == Mode.DOWN) {
            return decrement()
        }
        return defaults
    }

    private fun modeToString(mode: Mode): String {
        return when (mode) {
            AvPhoneObjectData.Mode.UP -> "Increase indefinitely"
            AvPhoneObjectData.Mode.DOWN -> "Decrease to zero"
            AvPhoneObjectData.Mode.None -> "None"
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
        return current!!.toString()
    }

    fun modePosition(): Int {
        return when (mode) {
            AvPhoneObjectData.Mode.None -> 0
            AvPhoneObjectData.Mode.UP -> 1
            AvPhoneObjectData.Mode.DOWN -> 2
        }
    }

    companion object {

        fun modeFromPosition(position: Int): Mode {
            when (position) {
                0 -> return Mode.None
                1 -> return Mode.UP
                2 -> return Mode.DOWN
            }
            return Mode.None
        }
    }


}
