package de.hernan.qu1ckr00t;

import android.os.Build;
import android.text.TextUtils;

public class DeviceInfo {
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getAndroidPatchLevel() {
        // Ensure SECURITY_PATCH is accessed only on API 23+ (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Build.VERSION.SECURITY_PATCH;
        } else {
            return "Patch level not available"; // Return default message for API levels below 23
        }
    }

    public static String getKernelVersion() {
        return System.getProperty("os.version");
    }

    public static String getDeviceArchitecture() {
        return System.getProperty("os.arch");
    }

    public static String getBuildFingerprint() {
        return Build.FINGERPRINT;
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
}
