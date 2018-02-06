package com.sierrawireless.avphone.adapter

import android.graphics.drawable.Drawable

internal enum class MenuEntryType {
    TITLE,
    USER,
    COMMAND
}

class MenuEntry internal constructor(var name: String, var type: MenuEntryType, var drawable:Drawable? = null)
