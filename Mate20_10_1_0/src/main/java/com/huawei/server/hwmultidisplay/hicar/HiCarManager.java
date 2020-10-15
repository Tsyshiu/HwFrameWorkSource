package com.huawei.server.hwmultidisplay.hicar;

import android.app.ActivityManager;
import android.app.HwRecentTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.HwPCUtils;
import android.view.Display;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.android.os.UserHandleEx;
import com.huawei.android.view.DisplayEx;
import java.util.List;

public class HiCarManager extends DefaultHiCarManager {
    private static final int DELAY_FOR_BIND_SERVICES = 800;
    public static final String HI_CAR_LAUNCHER_PKG = "com.huawei.hicar";
    private static final String HI_CAR_LAUNCHER_SERVICE_ACTION = "com.huawei.hicar.ACTION_ENABLE_CARDISPLAY";
    private static final String HI_CAR_LAUNCHER_SERVICE_CLASS = "com.huawei.hicar.services.CarService";
    private static final String KEY_HICAR_DISPLAY_ID = "KEY_HICAR_DISPLAY_ID";
    private static final Object LOCK = new Object();
    private static final int NOT_SYSTEM_PROCESS_ID = -1;
    private static final String PERMISSION_HICAR_DISPLAY = "com.huawei.permission.HICAR_DISPLAY";
    private static final String TAG = "HiCarManager";
    private static boolean mIsOverlay = false;
    private static boolean mSupportOverlay = SystemPropertiesEx.getBoolean("hw_pc_support_hicar_overlay", false);
    private static int sImeUid = -1;
    private static HiCarManager sInstance = null;
    private final ServiceConnection mConnLauncher = new ServiceConnection() {
        /* class com.huawei.server.hwmultidisplay.hicar.HiCarManager.AnonymousClass1 */

        public void onServiceConnected(ComponentName name, IBinder service) {
            HwPCUtils.log(HiCarManager.TAG, "HiCar launcher onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName name) {
            HwPCUtils.log(HiCarManager.TAG, "HiCar launcher onServiceDisconnected");
            HiCarManager.this.mHandler.postDelayed(new Runnable() {
                /* class com.huawei.server.hwmultidisplay.hicar.HiCarManager.AnonymousClass1.AnonymousClass1 */

                public void run() {
                    if (HwPCUtils.isHiCarCastMode()) {
                        HiCarManager.this.bindHiCarService(HiCarManager.this.mContext, HwPCUtils.getPCDisplayID());
                    }
                }
            }, 800);
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private final ComponentName mLauncherComponent = new ComponentName(HI_CAR_LAUNCHER_PKG, HI_CAR_LAUNCHER_SERVICE_CLASS);

    private HiCarManager() {
    }

    public static HiCarManager getInstance() {
        HiCarManager hiCarManager;
        synchronized (LOCK) {
            if (sInstance == null) {
                sInstance = new HiCarManager();
            }
            hiCarManager = sInstance;
        }
        return hiCarManager;
    }

    public HiCarManager(Handler handler, Context context) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void bindHiCarService(Context context, int displayId) {
        if (context == null) {
            HwPCUtils.log(TAG, "bindHiCarService fail context is null");
            return;
        }
        HwPCUtils.log(TAG, "bindHiCarService:" + displayId + "  " + displayId);
        Intent launcherIntent = new Intent();
        launcherIntent.setAction(HI_CAR_LAUNCHER_SERVICE_ACTION);
        launcherIntent.putExtra(KEY_HICAR_DISPLAY_ID, displayId);
        launcherIntent.setComponent(this.mLauncherComponent);
        bindService(context, launcherIntent, this.mConnLauncher);
    }

    private void bindService(Context context, Intent intent, ServiceConnection connection) {
        if (context != null) {
            try {
                context.bindService(intent, connection, 1);
            } catch (Exception e) {
                HwPCUtils.log(TAG, "failed to bind hicar service");
            }
        }
    }

    public void unBindHiCarService(Context context) {
        try {
            context.unbindService(this.mConnLauncher);
        } catch (Exception e) {
            HwPCUtils.log(TAG, "failed to unbind HiCar services");
        }
    }

    public static boolean isHiCarDevice(String name) {
        HwPCUtils.log(TAG, "isHiCarDevice device name " + name);
        return "HiSightDisplay".equals(name);
    }

    public static boolean isConnToHiCar(Context context, int displayId) {
        Display display = ((DisplayManager) context.getSystemService("display")).getDisplay(displayId);
        if (display == null) {
            HwPCUtils.log(TAG, "isConnToHiCar return false display is null!");
            return false;
        }
        int type = DisplayEx.getType(display);
        String name = display.getName();
        if (type == 4 && mSupportOverlay) {
            setIsOverlay(true);
        }
        if ((type != 5 || !"HiSightDisplay".equals(name)) && ((type != 5 && type != 4) || !mSupportOverlay)) {
            return false;
        }
        return true;
    }

    public static void setIsOverlay(boolean bool) {
        HwPCUtils.log(TAG, "setIsOverlay bool " + bool);
        mIsOverlay = bool;
    }

    public static boolean getIsOverlay() {
        return mIsOverlay;
    }

    public static boolean checkPermission(Context context, int uid) {
        if (uid != -1 && uid == sImeUid) {
            return true;
        }
        if (context == null) {
            HwPCUtils.log(TAG, "checkPermission fail context is null!");
            return false;
        } else if (context.checkPermission(PERMISSION_HICAR_DISPLAY, -1, UserHandleEx.getAppId(uid)) == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isHiCarHome(int taskId) {
        HwRecentTaskInfo info;
        try {
            HwRecentTaskInfo currentTask = HwActivityTaskManager.getHwRecentTaskInfo(taskId);
            if (currentTask != null && !HwPCUtils.isValidExtDisplayId(currentTask.displayId)) {
                return false;
            }
            List<ActivityManager.RunningTaskInfo> tasks = ActivityManagerEx.getTasks(1);
            if (tasks.isEmpty() || (info = HwActivityTaskManager.getHwRecentTaskInfo(tasks.get(0).id)) == null || HwPCUtils.isValidExtDisplayId(info.displayId)) {
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            HwPCUtils.log(TAG, "isHiCarHome RemoteException.");
        }
    }

    public void setInputMethodUid(int uid) {
        sImeUid = uid;
    }
}
