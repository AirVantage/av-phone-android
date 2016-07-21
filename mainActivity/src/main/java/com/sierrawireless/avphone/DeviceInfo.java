package com.sierrawireless.avphone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

public class DeviceInfo {


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
        return (userUid + "-" + systemType).toUpperCase();
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
}
