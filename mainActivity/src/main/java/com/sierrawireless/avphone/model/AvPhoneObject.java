package com.sierrawireless.avphone.model;

import java.util.ArrayList;

public class AvPhoneObject {
    public String name;
    public ArrayList<AvPhoneObjectData> datas;

    public AvPhoneObject() {
        datas = new ArrayList<>();
    }

    public void add(AvPhoneObjectData data ) {
        datas.add(data);
    }

    public String toString() {
        String returned = "{";
        returned = returned + "\"name\" : \"" + name + "\",";
        returned = returned + "\"datas\":[";
        for (AvPhoneObjectData data: datas) {
            returned = returned + data.toString()+",";
        }
        returned = returned + "]}";
        return returned;
    }

    public void exec() {
        for (AvPhoneObjectData data: datas) {
            data.execMode();
        }
    }

}
