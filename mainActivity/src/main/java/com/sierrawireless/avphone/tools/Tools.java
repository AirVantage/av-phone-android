package com.sierrawireless.avphone.tools;

import android.content.Context;

public class Tools {
    public static final String NAME="name";
    public static final String VALUE="value";

    public static String buildSerialNumber(String serial, String type) {
        return (serial + "-ANDROID-" + type).toUpperCase();
    }
    public static float dp2px(int dip, Context context){
        float scale = context.getResources().getDisplayMetrics().density;
        return dip * scale + 0.5f;
    }
}
