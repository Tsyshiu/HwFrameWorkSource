package com.android.server.devicepolicy;

import android.util.Log;

public class HwLog {
    public static final String ERROR_PREFIX = "error_mdm: ";
    private static final boolean HWDBG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    private static final boolean HWINFO;
    public static final String PREFIX = " ";
    public static final String TAG = "HwDPMS";
    public static final String WARNING_PREFIX = "warning_mdm: ";

    static {
        boolean z = false;
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) {
            z = true;
        }
        HWINFO = z;
    }

    public static void v(String tag, String msg) {
        if (HWDBG) {
            Log.v(TAG, makeupForString(tag) + PREFIX + msg);
        }
    }

    public static void d(String tag, String msg) {
        if (HWDBG) {
            Log.d(TAG, makeupForString(tag) + PREFIX + msg);
        }
    }

    public static void i(String tag, String msg) {
        if (HWINFO) {
            Log.i(TAG, makeupForString(tag) + PREFIX + msg);
        }
    }

    public static void w(String tag, String msg) {
        Log.w(TAG, makeupForString(tag) + WARNING_PREFIX + msg);
    }

    public static void e(String tag, String msg) {
        Log.e(TAG, makeupForString(tag) + ERROR_PREFIX + msg);
    }

    public static String makeupForString(String tag) {
        StringBuffer sb = new StringBuffer(tag);
        while (sb.length() < 8) {
            sb.append("-");
        }
        return sb.toString();
    }
}
