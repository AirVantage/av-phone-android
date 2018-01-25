package com.sierrawireless.avphone.tools;

/**
 * Created by JDamiano on 25/01/2018.
 */

public class Constant {
    public static final String NAME="name";
    public static final String VALUE="value";

    public static String buildSerialNumber(String serial, String type) {
        return (serial + "-ANDROID-" + type).toUpperCase();
    }
}
