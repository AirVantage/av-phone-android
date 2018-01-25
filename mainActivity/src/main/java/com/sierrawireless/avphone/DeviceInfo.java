package com.sierrawireless.avphone;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class DeviceInfo {
    private static final String TAG = "DeviceInfo";

    /** Returns the consumer friendly device name */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toUpperCase().startsWith(manufacturer.toUpperCase())) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    /**
    * Serial number is in `syncParams.deviceId`, why not using it?
    *
    * - It is not available running emulator
    * - It is not available to our iOs counterpart
    *
    * So to be AV Phone iOs compatible and able to run emulator, we use: uppercase(userUid + "-" + systemType)
    */
    @SuppressLint("DefaultLocale")
	public static String generateSerial(final String userUid, final String systemType) {
        return (userUid);
    }

    @SuppressLint("DefaultLocale")
    public static String getUniqueId(final Context context) {

        if (context instanceof MainActivity) {
            final MainActivity mainActivity = (MainActivity) context;
            return mainActivity.getSystemSerial();
        }

        return null;
    }

    /**
     * Is this SN one of the common ones used by cheap phones (eg wiko)?
     *
     * Currently we had issues with :
     *
     * - "123456789ABCD"
     * - "0123456789ABCDEF"
     *
     * We simply check if "123456789ABCD" is a substring, this should do for a while.
     */
    @SuppressLint("DefaultLocale")
    private static boolean isCommonSerialNumber(final String serialNumber) {
        return serialNumber.toUpperCase().contains("123456789ABCD");
    }

    public static String getIMEI(final Context context) {

        final TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            return telManager.getDeviceId();
        }

        return null;
    }

    public static String getICCID(final Context context) {
        SubscriptionManager sm = SubscriptionManager.from(context);
        List<SubscriptionInfo> sis = sm.getActiveSubscriptionInfoList();
        SubscriptionInfo si = sis.get(0);
        return si.getIccId();

    }
}
