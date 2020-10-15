package com.huawei.android.feature.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

public class CommonUtils {
    private static final String TAG = CommonUtils.class.getSimpleName();

    @TargetApi(21)
    public static boolean is64Bit(Context context) {
        if (context == null) {
            Log.e(TAG, "Null context, please check it.");
            return false;
        }
        if (context.getApplicationContext() != null) {
            context = context.getApplicationContext();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return Process.is64Bit();
        }
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        }
        try {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), 128).nativeLibraryDir.contains("64");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Get application info failed: name not found.");
            return false;
        }
    }
}
