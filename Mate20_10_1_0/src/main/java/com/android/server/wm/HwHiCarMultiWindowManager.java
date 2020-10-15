package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.view.Display;
import android.view.WindowManager;
import com.android.server.am.HwActivityManagerService;
import com.huawei.android.content.ContentResolverExt;
import com.huawei.android.view.WindowManagerEx;
import com.huawei.hwpartpowerofficeservices.BuildConfig;
import com.huawei.server.hwmultidisplay.hicar.HiCarManager;
import com.huawei.server.pc.HwPCManagerService;
import com.huawei.util.LogEx;
import com.huawei.utils.HwPartResourceUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class HwHiCarMultiWindowManager extends DefaultHwHiCarMultiWindowManager {
    private static final String ACTION_NOTIFY_APP_END_FRONT = "com.huawei.braodcast.hicar.fromapp.end_always_front";
    private static final String ACTION_NOTIFY_APP_START_FRONT = "com.huawei.braodcast.hicar.fromapp.start_always_front";
    private static final String ACTION_NOTIFY_FWK_END_SPLIT = "com.huawei.braodcast.hicar.fromfwk.end_split";
    private static final String ACTION_NOTIFY_FWK_START_SPLIT = "com.huawei.braodcast.hicar.fromfwk.start_split";
    private static final String BAIDU_INPUT_PACKAGE_NAME = "com.baidu.input_huawei";
    public static final boolean DEBUG = LogEx.getLogHWInfo();
    public static final boolean DEFER_RESUME = true;
    private static final String HICAR_BANACTIVITY_NAME = "com.huawei.hicar.mdmp.ui.BanAppActivity";
    private static final int INVALID_TASKID = -1;
    private static final Object LOCK = new Object();
    private static final String PERMISSION_BROADCAST_HICAR_SPLIT = "com.huawei.permission.HICAR_FWK_APP";
    private static final int RESIZE_MODE_SYSTEM = 0;
    private static final float ROTATION_LANDSCAPE_RATE = 1.33f;
    private static final String TAG = "HwHiCarMultiWindowManager";
    private static final float WINDOW_SPLIT_RATE_FOR_LANDSCAPE = 0.35f;
    private static final float WINDOW_SPLIT_RATE_FOR_PORTRAIT = 0.25f;
    private static volatile HwHiCarMultiWindowManager mSingleInstance = null;
    private volatile boolean isInSplitMode = false;
    private boolean isRotationLandscape;
    private int mAppDockHeight;
    private int mAppDockWidth;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public String mCurrentImePkg;
    private Point mDisplaySize;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public volatile int mMapTaskId = -1;
    private final BroadcastReceiver mNotifyReceiver = new BroadcastReceiver() {
        /* class com.android.server.wm.HwHiCarMultiWindowManager.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwPCUtils.log(HwHiCarMultiWindowManager.TAG, "mNotifyReceiver received a null intent");
                return;
            }
            String action = intent.getAction();
            if (HwHiCarMultiWindowManager.ACTION_NOTIFY_APP_START_FRONT.equals(action)) {
                int unused = HwHiCarMultiWindowManager.this.mMapTaskId = intent.getIntExtra("taskId", -1);
                HwPCUtils.log(HwHiCarMultiWindowManager.TAG, "hicar, receive ACTION_NOTIFY_APP_ALWAYS_FRONT. taskId=" + HwHiCarMultiWindowManager.this.mMapTaskId);
            } else if (HwHiCarMultiWindowManager.ACTION_NOTIFY_APP_END_FRONT.equals(action)) {
                int unused2 = HwHiCarMultiWindowManager.this.mMapTaskId = -1;
                HwPCUtils.log(HwHiCarMultiWindowManager.TAG, "hicar, receive ACTION_NOTIFY_APP_END_FRONT. taskId=" + intent.getIntExtra("taskId", -1));
            }
        }
    };
    private HwPCManagerService mPcManager;
    private ActivityTaskManagerServiceEx mService;
    private SettingsObserver mSettingsObserver;
    private int mStatusNaviHeigh;

    private HwHiCarMultiWindowManager(Context context, HwPCManagerService pcManager, HwActivityManagerService service, Display display, Looper looper) {
        HwPCUtils.log(TAG, "HwHiCarMultiWindowManager init.");
        this.mContext = context;
        this.mPcManager = pcManager;
        this.mService = service.getActivityTaskManagerServiceEx();
        this.mHandler = new Handler(looper);
        IntentFilter filterForHiCar = new IntentFilter();
        filterForHiCar.addAction(ACTION_NOTIFY_APP_START_FRONT);
        filterForHiCar.addAction(ACTION_NOTIFY_APP_END_FRONT);
        this.mContext.registerReceiver(this.mNotifyReceiver, filterForHiCar, PERMISSION_BROADCAST_HICAR_SPLIT, null);
        Point fullScreenSize = new Point();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealSize(fullScreenSize);
        display.getMetrics(displayMetrics);
        Context externalContext = this.mContext.createDisplayContext(display);
        this.mStatusNaviHeigh = externalContext.getResources().getDimensionPixelSize(HwPartResourceUtils.getResourceId("hw_hicar_status_bar_height"));
        if (((float) fullScreenSize.x) * 1.0f < ((float) fullScreenSize.y) * ROTATION_LANDSCAPE_RATE) {
            this.isRotationLandscape = false;
            this.mAppDockWidth = 0;
            this.mAppDockHeight = externalContext.getResources().getDimensionPixelSize(HwPartResourceUtils.getResourceId("hw_hicar_dock_height"));
        } else {
            this.isRotationLandscape = true;
            this.mAppDockWidth = externalContext.getResources().getDimensionPixelSize(HwPartResourceUtils.getResourceId("hw_hicar_dock_width"));
            this.mAppDockHeight = 0;
        }
        this.mDisplaySize = new Point();
        display.getRealSize(this.mDisplaySize);
        HwPCUtils.log(TAG, "isRotationLandscape=" + this.isRotationLandscape + " mStatusNaviHeight=" + this.mAppDockWidth + " mAppDockWidth=" + this.mAppDockWidth + " mAppDockHeight=" + this.mAppDockHeight + " densityDpi=" + displayMetrics.densityDpi);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: package-private */
        public void observe() {
            ContentResolverExt.registerContentObserver(HwHiCarMultiWindowManager.this.mContext.getContentResolver(), Settings.Secure.getUriFor("default_input_method"), false, this, 0);
            updateImePkgFromSettings();
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                updateImePkgFromSettings();
            }
        }

        /* access modifiers changed from: package-private */
        public void updateImePkgFromSettings() {
            String[] tmpPkg = Settings.Secure.getString(HwHiCarMultiWindowManager.this.mContext.getContentResolver(), "default_input_method").split("/");
            if (tmpPkg != null && tmpPkg.length > 0) {
                HwPCUtils.log(HwHiCarMultiWindowManager.TAG, "updateImePkgFromSettings");
                String unused = HwHiCarMultiWindowManager.this.mCurrentImePkg = tmpPkg[0];
            }
        }
    }

    public static void initInstance(Context context, HwPCManagerService pcManager, HwActivityManagerService service, Display display, Looper looper) {
        synchronized (LOCK) {
            mSingleInstance = new HwHiCarMultiWindowManager(context, pcManager, service, display, looper);
        }
    }

    public static void onDisplayRemoved() {
        synchronized (LOCK) {
            if (mSingleInstance != null) {
                mSingleInstance.mContext.unregisterReceiver(mSingleInstance.mNotifyReceiver);
            }
        }
    }

    public static HwHiCarMultiWindowManager getInstance() {
        if (mSingleInstance != null) {
            return mSingleInstance;
        }
        throw new RuntimeException("HwHiCarMultiWindowManager not instanced.");
    }

    public String getCurrentImePkg() {
        return this.mCurrentImePkg;
    }

    public Rect getWindowBounds(TaskRecordEx recordEx) {
        Rect rect;
        String packageName = recordEx.getPkgNameFromRootActivity();
        if (DEBUG) {
            HwPCUtils.log(TAG, "getWindowBounds enter. packageName=" + packageName);
        }
        synchronized (this.mService.getGlobalLock()) {
            rect = null;
            boolean needMoveBackTasks = false;
            Map<String, TaskRecordEx> taskToMoveBack = new HashMap<>();
            int stackCount = recordEx.getDisplayChildCount();
            for (int i = 0; i < stackCount; i++) {
                ActivityStackEx stackEx = recordEx.getActivityStackExByIndex(i);
                int j = 0;
                int taskCount = stackEx.getChildCount();
                while (true) {
                    if (j >= taskCount) {
                        break;
                    } else if (!stackEx.shouldBeVisible((ActivityRecordEx) null)) {
                        break;
                    } else {
                        TaskRecordEx taskEx = stackEx.getChildAt(j);
                        if (taskEx.getTaskId() != recordEx.getTaskId() && taskEx.isVisible()) {
                            if (!taskEx.isRootActivityEmpty()) {
                                if (DEBUG) {
                                    HwPCUtils.log(TAG, "getWindowBounds stackList packageName=" + taskEx.getPkgNameFromRootActivity());
                                }
                                if (!taskEx.getPkgNameFromRootActivity().equals(packageName)) {
                                    if (taskEx.getTaskId() == this.mMapTaskId) {
                                        HwPCUtils.log(TAG, "getWindowBounds resizeTask Map.");
                                        this.isInSplitMode = true;
                                        resizeTask(taskEx, getRectForAreaB());
                                        sendBroadcastForSplit(taskEx.getTaskId(), true);
                                        rect = getRectForAreaA();
                                    } else {
                                        HwPCUtils.log(TAG, "add task to moveTaskBackwards list, taskId=" + taskEx.getTaskId());
                                        taskToMoveBack.put(packageName, taskEx);
                                        if (taskEx.getPkgNameFromRootActivity().equals("com.android.incallui")) {
                                            needMoveBackTasks = true;
                                            HwPCUtils.log(TAG, "getWindowBounds moveTaskBackwards isInCall.");
                                        }
                                    }
                                }
                            }
                        }
                        j++;
                    }
                }
            }
            if (HICAR_BANACTIVITY_NAME.equals(recordEx.getRootActivityInfo().name)) {
                HwPCUtils.log(TAG, "HiCarBanActivity move to front, move other tasks back.");
                needMoveBackTasks = true;
            }
            if (needMoveBackTasks) {
                HwPCUtils.log(TAG, "Move other tasks back in HiCar mode");
                taskToMoveBack.forEach(new BiConsumer() {
                    /* class com.android.server.wm.$$Lambda$HwHiCarMultiWindowManager$vKgTS4OQ1kBF__IflnXrJ5QvcXM */

                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        HwHiCarMultiWindowManager.this.lambda$getWindowBounds$0$HwHiCarMultiWindowManager((String) obj, (TaskRecordEx) obj2);
                    }
                });
            }
            if (rect == null) {
                this.isInSplitMode = false;
                sendBroadcastForSplit(recordEx.getTaskId(), false);
                rect = getMaximizedBounds();
            }
            HwPCUtils.log(TAG, "getWindowBounds end. " + rect.toShortString());
        }
        return rect;
    }

    /* access modifiers changed from: private */
    /* renamed from: moveTaskBackwards */
    public void lambda$getWindowBounds$0$HwHiCarMultiWindowManager(String packageName, TaskRecordEx task) {
        if (!packageName.equals(BuildConfig.FLAVOR) && !HiCarManager.HI_CAR_LAUNCHER_PKG.equals(task.getPkgNameFromRootActivity()) && !packageName.equals("com.android.incallui") && !packageName.equals("com.android.server.telecom")) {
            this.mHandler.post(new Runnable(task.getTaskId()) {
                /* class com.android.server.wm.$$Lambda$HwHiCarMultiWindowManager$tJKaAnrUFqXMJ9ybnzQS2fAPBZs */
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HwHiCarMultiWindowManager.this.lambda$moveTaskBackwards$1$HwHiCarMultiWindowManager(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$moveTaskBackwards$1$HwHiCarMultiWindowManager(int toBackTaskId) {
        this.mService.moveTaskBackwards(toBackTaskId);
    }

    public void onMoveTaskToFront(int taskId) {
        synchronized (this.mService.getGlobalLock()) {
            onMoveTaskToFront(this.mService.anyTaskForId(taskId));
        }
    }

    private void onMoveTaskToFront(TaskRecordEx recordEx) {
        if (recordEx != null) {
            HwPCUtils.log(TAG, "onMoveTaskToFront enter. taskId=" + recordEx.getTaskId());
            resizeTask(recordEx, getWindowBounds(recordEx));
        }
    }

    public void onMoveTaskToBack(int taskId) {
        HwPCUtils.log(TAG, "onMoveTaskToBack enter. taskId=" + taskId);
        this.mPcManager.onTaskMovedToBack(taskId);
        onTaskBackOrRemovedForCar(taskId);
    }

    public void onTaskRemoved(int taskId) {
        HwPCUtils.log(TAG, "onTaskRemoved enter. taskId=" + taskId);
        onTaskBackOrRemovedForCar(taskId);
    }

    private void onTaskBackOrRemovedForCar(int taskId) {
        TaskRecordEx taskEx;
        if (!HwPCUtils.isHiCarCastMode()) {
            HwPCUtils.log(TAG, "HiCar has exited.");
            return;
        }
        synchronized (this.mService.getGlobalLock()) {
            TaskRecordEx incallUiTask = null;
            boolean isSplitCurrent = false;
            boolean isInCallUiMoveBack = false;
            try {
                List<TaskRecordEx> currFrontTaskList = new ArrayList<>();
                ActivityDisplayEx activityDisplayEx = this.mService.getActivityDisplayEx(HwPCUtils.getPCDisplayID());
                if (activityDisplayEx == null) {
                    HwPCUtils.log(TAG, "HiCar activityDisplayEx has exited. PCDisplayID=" + HwPCUtils.getPCDisplayID());
                    return;
                }
                int stackCount = activityDisplayEx.getChildCount();
                for (int i = 0; i < stackCount; i++) {
                    ActivityStackEx stackEx = activityDisplayEx.getChildAt(i);
                    int j = 0;
                    int taskCount = stackEx.getChildCount();
                    while (true) {
                        if (j >= taskCount) {
                            break;
                        }
                        TaskRecordEx task = stackEx.getChildAt(j);
                        if (stackEx.shouldBeVisible((ActivityRecordEx) null)) {
                            if (task.isVisible()) {
                                if (!task.isTopActivityEmpty()) {
                                    if ("com.android.incallui".equals(task.getPkgNameFromTopActivity())) {
                                        incallUiTask = task;
                                    }
                                    if (task.getTaskId() == this.mMapTaskId) {
                                        isSplitCurrent = true;
                                    }
                                    currFrontTaskList.add(task);
                                    HwPCUtils.log(TAG, "onTaskBackOrRemovedForCar, front task taskId=" + task.getTaskId());
                                    incallUiTask = incallUiTask;
                                }
                            }
                            j++;
                        } else if (task.getTaskId() == taskId) {
                            isInCallUiMoveBack = true;
                        }
                    }
                }
                if (currFrontTaskList.isEmpty()) {
                    HwPCUtils.log(TAG, "onTaskBackOrRemovedForCar, No TaskRecord needs to be fronted.");
                    return;
                }
                if (currFrontTaskList.size() == 1) {
                    resizeTask(currFrontTaskList.get(0), getMaximizedBounds());
                    this.isInSplitMode = false;
                    sendBroadcastForSplit(currFrontTaskList.get(0).getTaskId(), false);
                    this.mPcManager.onTaskMovedToFront(currFrontTaskList.get(0).getTaskId());
                    HwPCUtils.log(TAG, "onTaskBackOrRemovedForCar, TaskRecord needs to be maximized and fronted.");
                } else if (this.isInSplitMode || !isInCallUiMoveBack) {
                    int i2 = currFrontTaskList.size() - 1;
                    while (true) {
                        if (i2 < 0) {
                            break;
                        }
                        taskEx = currFrontTaskList.get(i2);
                        if (taskEx.getTaskId() == this.mMapTaskId || !(incallUiTask == null || incallUiTask == taskEx)) {
                            i2--;
                        }
                    }
                    if (isSplitCurrent) {
                        HwPCUtils.log(TAG, "In split onTaskBackOrRemovedForCar, resize task for areaA: " + taskEx);
                        resizeTask(taskEx, getRectForAreaA());
                    } else {
                        HwPCUtils.log(TAG, "Not in split, resize task max for: " + taskEx);
                        resizeTask(taskEx, getMaximizedBounds());
                    }
                    this.mPcManager.onTaskMovedToFront(taskEx.getTaskId());
                } else {
                    HwPCUtils.log(TAG, "onTaskBackOrRemovedForCar, Not in split mode.");
                }
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        }
    }

    private void sendBroadcastForSplit(int taskId, boolean isStartSplit) {
        if (this.mMapTaskId == taskId) {
            Intent intent = new Intent();
            intent.setAction(isStartSplit ? ACTION_NOTIFY_FWK_START_SPLIT : ACTION_NOTIFY_FWK_END_SPLIT);
            intent.putExtra("taskId", taskId);
            this.mHandler.post(new Runnable(intent) {
                /* class com.android.server.wm.$$Lambda$HwHiCarMultiWindowManager$dkHoWqGStWNJGYd91N20Bmtv68o */
                private final /* synthetic */ Intent f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HwHiCarMultiWindowManager.this.lambda$sendBroadcastForSplit$2$HwHiCarMultiWindowManager(this.f$1);
                }
            });
            HwPCUtils.log(TAG, "hicar, send broadcast. taskId= " + taskId + " isStartSplit=" + isStartSplit);
            HwPCUtils.bdReport(this.mContext, isStartSplit ? 10063 : 10064, BuildConfig.FLAVOR);
        }
    }

    public /* synthetic */ void lambda$sendBroadcastForSplit$2$HwHiCarMultiWindowManager(Intent intent) {
        this.mContext.sendBroadcast(intent, null);
    }

    private void resizeTask(TaskRecordEx recordEx, Rect bounds) {
        recordEx.resize(bounds, 0, true, false);
    }

    private Rect getRectForAreaA() {
        if (this.isRotationLandscape) {
            return new Rect((int) (((float) this.mAppDockWidth) + (((float) (this.mDisplaySize.x - this.mAppDockWidth)) * WINDOW_SPLIT_RATE_FOR_LANDSCAPE)), this.mStatusNaviHeigh, this.mDisplaySize.x, this.mDisplaySize.y);
        }
        return new Rect(0, (int) (((float) this.mStatusNaviHeigh) + (((float) ((this.mDisplaySize.y - this.mAppDockHeight) - this.mStatusNaviHeigh)) * WINDOW_SPLIT_RATE_FOR_PORTRAIT)), this.mDisplaySize.x, this.mDisplaySize.y - this.mAppDockHeight);
    }

    private Rect getRectForAreaB() {
        if (!this.isRotationLandscape) {
            return new Rect(0, this.mStatusNaviHeigh, this.mDisplaySize.x, (int) (((float) this.mStatusNaviHeigh) + (((float) ((this.mDisplaySize.y - this.mAppDockHeight) - this.mStatusNaviHeigh)) * WINDOW_SPLIT_RATE_FOR_PORTRAIT)));
        }
        int i = this.mAppDockWidth;
        return new Rect(i, this.mStatusNaviHeigh, (int) (((float) i) + (((float) (this.mDisplaySize.x - this.mAppDockWidth)) * WINDOW_SPLIT_RATE_FOR_LANDSCAPE)), this.mDisplaySize.y);
    }

    public Rect getMaximizedBounds() {
        return new Rect(this.mAppDockWidth, this.mStatusNaviHeigh, this.mDisplaySize.x, this.mDisplaySize.y - this.mAppDockHeight);
    }

    public int getInputMethodWidth() {
        return getMaximizedBounds().width();
    }

    public boolean isRotationLandscape() {
        return this.isRotationLandscape;
    }

    public int getStatusNavigationHeigh() {
        return this.mStatusNaviHeigh;
    }

    public int getAppDockWidth() {
        return this.mAppDockWidth;
    }

    public int getAppDockHeight() {
        return this.mAppDockHeight;
    }

    public boolean isHiCarNavigationBar(WindowManager.LayoutParams attrs) {
        if (attrs.type != 2104 || (WindowManagerEx.LayoutParamsEx.getHwFlags(attrs) & 128) == 0) {
            return false;
        }
        return true;
    }
}
