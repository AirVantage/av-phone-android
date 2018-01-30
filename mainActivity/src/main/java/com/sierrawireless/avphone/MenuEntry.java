package com.sierrawireless.avphone;


enum MenuEntryType {
    TITLE,
    USER,
    COMMAND
}
public class MenuEntry {

    public String name;
    public  MenuEntryType type;
    MenuEntry(String name, MenuEntryType type) {
        this.name = name;
        this.type = type;
    }
}
