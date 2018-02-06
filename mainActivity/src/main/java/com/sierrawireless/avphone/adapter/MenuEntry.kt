package com.sierrawireless.avphone.adapter


internal enum class MenuEntryType {
    TITLE,
    USER,
    COMMAND
}

class MenuEntry internal constructor(var name: String, var type: MenuEntryType)
