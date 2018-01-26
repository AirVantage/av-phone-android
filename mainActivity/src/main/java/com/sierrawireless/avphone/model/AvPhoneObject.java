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
        StringBuilder returned = new StringBuilder("{");
        returned.append("\"name\" : \"").append(name).append("\",");
        returned.append("\"datas\":[");
        for (AvPhoneObjectData data: datas) {
            returned.append(data.toString()).append(",");
        }
        returned.append("]}");
        return returned.toString();
    }

    public void exec() {
        for (AvPhoneObjectData data: datas) {
            data.execMode();
        }
    }

}
