package com.sierrawireless.avphone.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JDamiano on 24/01/2018.
 */

public class AvPhoneModel {
    public String name;
    ArrayList<AvPhoneModelData> datas;
    private static AvPhoneModel instance = null;

    public AvPhoneModel() {
        datas = new ArrayList<>();
    }

    public void add(AvPhoneModelData data ) {
        datas.add(data);
    }

    public String toString() {
        String returned = "{";
        returned = returned + "\"name\" : \"" + name + "\",";
        returned = returned + "\"datas\":[";
        for (AvPhoneModelData data: datas) {
            returned = returned + data.toString()+",";
        }
        returned = returned + "]}";
        return returned;
    }



}
