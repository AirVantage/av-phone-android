package com.sierrawireless.avphone


internal enum class MenuEntryType {
    TITLE,
    USER,
    COMMAND
}

class MenuEntry internal constructor(var name: String, var type: MenuEntryType)
