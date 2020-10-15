package com.huawei.hiai.awareness.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hiai.awareness.common.log.LogUtil;
import com.huawei.msdp.movement.HwMSDPOtherParameters;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {
    private static final String MSDP_PACKAGE_NAME = "com.huawei.msdp";
    private static final String TAG = ("sdk_" + Utils.class.getSimpleName());
    private static boolean sIsMsdpInstalled = false;

    private Utils() {
    }

    public static boolean checkApkExist(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName) || context == null) {
            return false;
        }
        try {
            if (context.getPackageManager().getApplicationInfo(packageName, 8192) != null) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e(TAG, "checkApkExist false PackageManager.NameNotFoundException");
            return false;
        }
    }

    public static synchronized boolean isMsdpInstalled(Context context) {
        synchronized (Utils.class) {
            if (sIsMsdpInstalled) {
                return true;
            }
            if (context == null) {
                LogUtil.e(TAG, "isMsdpInstalled() context = null");
                return false;
            }
            sIsMsdpInstalled = checkApkExist(context, MSDP_PACKAGE_NAME);
            return sIsMsdpInstalled;
        }
    }

    public static HwMSDPOtherParameters getHwMsdpOtherParametersByString(String hwMsdpOtherParameters) {
        LogUtil.d(TAG, "enter into getHwMsdpOtherParametersByString");
        if (TextUtils.isEmpty(hwMsdpOtherParameters)) {
            LogUtil.e(TAG, "getHwMsdpOtherParametersByString string is empty!");
            return null;
        }
        String[] paramArray = hwMsdpOtherParameters.split(",");
        if (paramArray.length != 5) {
            LogUtil.e(TAG, "getHwMsdpOtherParametersByString string length error!");
            return null;
        }
        try {
            return new HwMSDPOtherParameters(Double.parseDouble(paramArray[0].split("=")[1]), Double.parseDouble(paramArray[1].split("=")[1]), Double.parseDouble(paramArray[2].split("=")[1]), Double.parseDouble(paramArray[3].split("=")[1]), paramArray[4].replace("Param5=", ""));
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "getHwMsdpOtherParametersByString param is illegal!");
            return null;
        }
    }

    public static String getMsdpSupportActionStr(int action, ConcurrentHashMap<String, Integer> map, String[] arrayString) {
        if (map == null || map.isEmpty()) {
            LogUtil.e(TAG, "getMsdpSupportActionStr parameters illegal map is null");
            return null;
        } else if (arrayString == null || arrayString.length == 0) {
            LogUtil.e(TAG, "getMsdpSupportActionStr parameters illegal arrayString is null");
            return null;
        } else {
            for (String actionString : arrayString) {
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (actionString != null && actionString.equals(entry.getKey()) && action == entry.getValue().intValue()) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }
    }

    private static ArrayList<Integer> getArrayListFromString(String strArrayList) {
        ArrayList<Integer> arrayList = new ArrayList<>(16);
        if (TextUtils.isEmpty(strArrayList)) {
            LogUtil.e(TAG, "getArrayListFromString String is empty!");
            return arrayList;
        }
        LogUtil.d(TAG, "getArrayListFromString String is strArrayList:" + strArrayList);
        String[] dataArray = strArrayList.replace("[", "").replace("]", "").replace(" ", "").split(",");
        LogUtil.d(TAG, "getArrayListFromString arrayString is length:" + dataArray.length);
        try {
            for (String content : dataArray) {
                arrayList.add(Integer.valueOf(Integer.parseInt(content)));
            }
            LogUtil.d(TAG, "getArrayListFromString,arrayList:" + arrayList.toString());
            return arrayList;
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "getArrayListFromString param is illegal!");
            arrayList.clear();
            return arrayList;
        }
    }

    public static ArrayList<Integer> getFenceActionArrayListFromTopKey(String topKey) {
        if (TextUtils.isEmpty(topKey)) {
            LogUtil.e(TAG, "getFenceActionArrayListFromTopKey parameters illegal!");
            return null;
        }
        String str = TAG;
        LogUtil.d(str, "enter into getFenceActionArrayListFromTopKey topKey:" + topKey);
        String[] splitTopKeyArray = topKey.split(";");
        if (splitTopKeyArray.length != 2) {
            LogUtil.e(TAG, "getFenceActionArrayListFromTopKey() splitTopKey length error");
            return null;
        }
        String fenceKey = splitTopKeyArray[1];
        ArrayList<Integer> arrayList = new ArrayList<>(16);
        if (fenceKey.contains(AwarenessConstants.SECOND_ACTION_SPLITE_TAG)) {
            String[] fenceKeyArray = fenceKey.split(AwarenessConstants.SECOND_ACTION_SPLITE_TAG);
            if (fenceKeyArray.length != 2) {
                LogUtil.e(TAG, "getFenceActionArrayListFromTopKey() splitFenceKey length error");
                return null;
            }
            ArrayList<Integer> arrayList2 = getArrayListFromString(fenceKeyArray[1]);
            updateFenceActionArrayListFromAction(arrayList2, fenceKeyArray[0]);
            return arrayList2;
        }
        updateFenceActionArrayListFromAction(arrayList, fenceKey);
        return arrayList;
    }

    private static void updateFenceActionArrayListFromAction(ArrayList<Integer> arrayList, String fenceKey) {
        if (arrayList == null) {
            LogUtil.e(TAG, "getFenceActionArrayListFromAction arrayList == null");
        } else if (TextUtils.isEmpty(fenceKey)) {
            LogUtil.e(TAG, "getFenceActionArrayListFromAction fenceKey is empty");
        } else {
            String[] typeStatusActionArray = fenceKey.split(",");
            if (typeStatusActionArray.length == 3) {
                try {
                    arrayList.add(Integer.valueOf(Integer.parseInt(typeStatusActionArray[2])));
                } catch (NumberFormatException e) {
                    LogUtil.e(TAG, "getFenceActionArrayListFromAction NumberFormatException");
                    arrayList.clear();
                }
            }
        }
    }

    public static boolean isApkVersionSatisfied(String needVersion, String realVersion) {
        if (TextUtils.isEmpty(needVersion) || TextUtils.isEmpty(realVersion)) {
            LogUtil.e(TAG, "isApkVersionSatisfied needVersion or realVersion is empty!");
            return false;
        }
        String str = TAG;
        LogUtil.d(str, "isApkVersionSatisfied realVersion = " + realVersion);
        String[] realVersionArray = realVersion.split("\\.");
        String[] needVersionArray = needVersion.split("\\.");
        int minArrayLen = realVersionArray.length;
        if (realVersionArray.length > needVersionArray.length) {
            minArrayLen = needVersionArray.length;
        }
        int i = 0;
        while (i < minArrayLen) {
            try {
                if (Integer.parseInt(realVersionArray[i]) > Integer.parseInt(needVersionArray[i])) {
                    return true;
                }
                if (Integer.parseInt(realVersionArray[i]) < Integer.parseInt(needVersionArray[i])) {
                    return false;
                }
                LogUtil.d(TAG, "isApkVersionSatisfied realVersionArray[i] == needVersionArray[i]");
                if (i == minArrayLen - 1) {
                    LogUtil.d(TAG, "isApkVersionSatisfied version equal");
                    return true;
                }
                i++;
            } catch (NumberFormatException e) {
                LogUtil.e(TAG, "isApkVersionSatisfied NumberFormatException ");
                return false;
            }
        }
        return false;
    }
}
