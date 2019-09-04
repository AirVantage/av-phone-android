package com.sierrawireless.avphone.adapter

import android.graphics.drawable.Drawable

enum class MenuEntryType {
    TITLE,
    USER,
    COMMAND
}

class MenuEntry internal constructor(var name: String, var type: MenuEntryType, var drawable:Drawable? = null, var button:Boolean = false)
