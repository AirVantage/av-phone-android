package com.sierrawireless.avphone;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.crashlytics.android.Crashlytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class DeviceInfo {

    @SuppressLint("DefaultLocale")
    public static String getUniqueId(Context context) {

        String identifier = Build.SERIAL;
        if (identifier == null || isCommonSerialNumber(identifier)) {
            // from IMEI
            String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            identifier = hash(deviceId);
        }
        if (identifier == null) {
            // from Android ID
            // should not be used since this ID can change
            identifier = hash(Secure.ANDROID_ID);
        }
        return identifier != null ? identifier.toUpperCase() : null;
    }

    @SuppressLint("DefaultLocale")
    /**
     * Is this SN one of the common ones used by cheap phones (eg wiko) ? 
     * Currently we had issues with :
     * - "123456789ABCD"
     * - "0123456789ABCDEF"
     *
     * We simply check if "123456789ABCD" is a substring, this should do
     * for a while.
     */
    private static boolean isCommonSerialNumber(String serialNumber) {
        return serialNumber.toUpperCase().contains("123456789ABCD");
    }

    // first 12 digits of SHA-1 hash
    private static String hash(String msg) {
        if (msg != null) {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("SHA-1");
                digest.update(msg.getBytes("UTF-8"));
                byte[] msgDigest = digest.digest();

                // hex string
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < msgDigest.length; i++) {
                    hexString.append(Integer.toHexString(0xFF & msgDigest[i]));
                }
                return hexString.toString().substring(0, 12);
            } catch (NoSuchAlgorithmException e) {
                // Should not happen
                Crashlytics.logException(e);
            } catch (UnsupportedEncodingException e) {
                // Should not happen
                Crashlytics.logException(e);
            }
        }
        return null;
    }

    public static String getIMEI(Context context) {
        String imei = null;
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            imei = telManager.getDeviceId();
        }
        return imei;
    }
}
