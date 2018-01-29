package com.sierrawireless.avphone.tools;

import android.app.Activity;
import android.content.Context;
import android.text.Html;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;
import com.sierrawireless.avphone.model.AvPhoneApplication;
import com.sierrawireless.avphone.task.IUserClient;

import net.airvantage.model.AvError;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;

import java.util.List;

public class Tools {
    public static final String NAME="name";
    public static final String VALUE="value";

    public static String buildSerialNumber(String serial, String type) {
        return (serial + "-ANDROID-" + type).toUpperCase();
    }
    public static float dp2px(Context context){
        float scale = context.getResources().getDisplayMetrics().density;
        return 90 * scale + 0.5f;
    }
}
