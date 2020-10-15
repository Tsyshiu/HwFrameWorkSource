package com.android.server.zrhung;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.zrhung.IFaultEventCallback;
import android.zrhung.IFaultEventService;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaultNotifyService extends IFaultEventService.Stub {
    private static final int ANR = 1;
    private static final int CRASH = 2;
    private static final int INIT_MAP_SIZE = 16;
    private static final int MASK = 1;
    private static final int MAX_NOTIFY_SIZE = 64;
    private static final int REGIST_TYPE_SIZE = 3;
    private static final String STR_ANR = "anr";
    private static final String STR_CRASH = "crash";
    private static final String STR_TOMSTONE = "tombstone";
    private static final String TAG = "FaultNotifyService";
    private static final int TOMBSTONE = 4;
    private Context mContext;
    private final Map<String, Map<Integer, IFaultEventCallback>> mNotifyMaps = new ConcurrentHashMap(16);

    public FaultNotifyService(Context context) {
        this.mContext = context;
        Log.i(TAG, "FaultNotifyService on create");
    }

    public boolean registerCallback(String packagerName, IFaultEventCallback callBack, int flag) throws RemoteException {
        if (this.mNotifyMaps.size() >= 64) {
            return false;
        }
        if (callBack == null || packagerName == null || flag <= 0 || flag > 7) {
            Log.i(TAG, "register fail :packageName :" + packagerName + " Callback = " + callBack + " flag = " + flag);
            return false;
        }
        registToNotifyMap(packagerName, flag, callBack);
        if (this.mNotifyMaps.size() < 64) {
            return true;
        }
        maintainNotifyMap();
        return true;
    }

    public void unRegisterCallback(String packagerName) throws RemoteException {
        if (packagerName != null && this.mNotifyMaps.containsKey(packagerName)) {
            this.mNotifyMaps.remove(packagerName);
            Log.i(TAG, "unRegister success");
        }
    }

    public void callBack(String packageName, String tag, List<String> faultInfo) throws RemoteException {
        if (this.mNotifyMaps.containsKey(packageName)) {
            int eventId = getEventId(tag.toLowerCase(Locale.ROOT));
            if (this.mNotifyMaps.get(packageName).containsKey(Integer.valueOf(eventId))) {
                this.mNotifyMaps.get(packageName).get(Integer.valueOf(eventId)).actionPerforemed(eventId, faultInfo);
                Log.i(TAG, "callBack Success ");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0039  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0042 A[ORIG_RETURN, RETURN, SYNTHETIC] */
    private int getEventId(String evenType) {
        char c;
        int hashCode = evenType.hashCode();
        if (hashCode != 96741) {
            if (hashCode != 94921639) {
                if (hashCode == 1836179733 && evenType.equals(STR_TOMSTONE)) {
                    c = 2;
                    if (c != 0) {
                        return 1;
                    }
                    if (c == 1) {
                        return 2;
                    }
                    if (c != 2) {
                        return 0;
                    }
                    return 4;
                }
            } else if (evenType.equals(STR_CRASH)) {
                c = 1;
                if (c != 0) {
                }
            }
        } else if (evenType.equals(STR_ANR)) {
            c = 0;
            if (c != 0) {
            }
        }
        c = 65535;
        if (c != 0) {
        }
    }

    private void addToNotifyMap(String packagerName, int registType, IFaultEventCallback callBack) {
        if (this.mNotifyMaps.containsKey(packagerName)) {
            this.mNotifyMaps.get(packagerName).put(Integer.valueOf(registType), callBack);
            return;
        }
        Map<Integer, IFaultEventCallback> typeCallBack = new ConcurrentHashMap<>(16);
        typeCallBack.put(Integer.valueOf(registType), callBack);
        this.mNotifyMaps.put(packagerName, typeCallBack);
    }

    private void registToNotifyMap(String packagerName, int flag, IFaultEventCallback callBack) {
        int maskTemp = 1;
        if (!this.mNotifyMaps.containsKey(packagerName)) {
            for (int i = 0; i < 3; i++) {
                int registType = flag & maskTemp;
                maskTemp <<= 1;
                if (registType != 0) {
                    addToNotifyMap(packagerName, registType, callBack);
                }
            }
            Log.i(TAG, "register success");
            return;
        }
        for (int i2 = 0; i2 < 3; i2++) {
            int registerType = flag & maskTemp;
            maskTemp <<= 1;
            if (registerType != 0) {
                this.mNotifyMaps.get(packagerName).put(Integer.valueOf(registerType), callBack);
            }
        }
    }

    private synchronized void maintainNotifyMap() {
        ActivityManager am = null;
        if (this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG) instanceof ActivityManager) {
            am = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        }
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> runAppList = am.getRunningAppProcesses();
            List<String> runAppNameList = new ArrayList<>(16);
            List<String> removeAppLists = new ArrayList<>(16);
            for (ActivityManager.RunningAppProcessInfo runApp : runAppList) {
                runAppNameList.add(runApp.processName);
            }
            for (Map.Entry<String, Map<Integer, IFaultEventCallback>> entry : this.mNotifyMaps.entrySet()) {
                String appName = entry.getKey();
                if (!runAppNameList.contains(appName)) {
                    removeAppLists.add(appName);
                }
            }
            for (String removeAppName : removeAppLists) {
                this.mNotifyMaps.remove(removeAppName);
            }
        }
    }
}
