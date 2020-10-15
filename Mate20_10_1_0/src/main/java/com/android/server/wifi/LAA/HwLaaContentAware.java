package com.android.server.wifi.LAA;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.server.hidata.HwQoEUdpNetWorkInfo;
import com.android.server.wifi.HwQoE.HwQoEJNIAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HwLaaContentAware {
    private static final int MSG_APP_UDP_MONITOR = 1;
    private static final String TAG = "LAA_HwLaaContentAware";
    private static final long UDP_ACCESS_MONITOR_INTERVAL = 3000;
    /* access modifiers changed from: private */
    public boolean isLaaContentAwareEnabled;
    /* access modifiers changed from: private */
    public boolean isRequestLaaDisEnabled;
    /* access modifiers changed from: private */
    public boolean isSensitiveApp;
    private ActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public int mAppSensitivityScore;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    /* access modifiers changed from: private */
    public int mCurrMoniorUid;
    /* access modifiers changed from: private */
    public HwQoEUdpNetWorkInfo mCurrUdpInfoForMonitor;
    /* access modifiers changed from: private */
    public String mForegroundName;
    /* access modifiers changed from: private */
    public Handler mHwLaaContentAwareHandler;
    private Handler mHwLaaControllerHandler;
    private HwProcessObserver mHwProcessObserver;
    /* access modifiers changed from: private */
    public HwQoEJNIAdapter mHwQoEJNIAdapter;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public HwQoEUdpNetWorkInfo mLastUdpInfoForMonitor;
    private PackageManager mPackageManager = this.mContext.getPackageManager();
    /* access modifiers changed from: private */
    public Map<String, Integer> mSensitiveAppHashMap;

    public HwLaaContentAware(Context context, Handler handler) {
        this.mContext = context;
        this.mHwLaaControllerHandler = handler;
        this.mHwQoEJNIAdapter = HwQoEJNIAdapter.getInstance();
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        initialSensitiveAppHashMap();
        initHwLaaContentAwareHandler();
        registerProcessObserver();
    }

    public synchronized void setLaaContentAwareEnabled(boolean enabled) {
        HwLaaUtils.logD(TAG, false, "setLaaContentAwareEnabled: %{public}s, isSensitiveApp:%{public}s", String.valueOf(enabled), String.valueOf(this.isSensitiveApp));
        this.isLaaContentAwareEnabled = enabled;
        if (enabled) {
            if (this.isSensitiveApp && !this.mHwLaaContentAwareHandler.hasMessages(1)) {
                this.mHwLaaContentAwareHandler.sendEmptyMessage(1);
            }
        } else if (this.mHwLaaContentAwareHandler.hasMessages(1)) {
            HwLaaUtils.logD(TAG, false, "removeMessages : MSG_APP_UDP_MONITOR", new Object[0]);
            this.mHwLaaContentAwareHandler.removeMessages(1);
        }
    }

    private void initHwLaaContentAwareHandler() {
        HandlerThread handlerThread = new HandlerThread("hw_laa_plus_handler_thread");
        handlerThread.start();
        this.mHwLaaContentAwareHandler = new Handler(handlerThread.getLooper()) {
            /* class com.android.server.wifi.LAA.HwLaaContentAware.AnonymousClass1 */

            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (!HwLaaContentAware.this.isLaaContentAwareEnabled) {
                        HwLaaUtils.logD(HwLaaContentAware.TAG, false, "ContentAware is disenable", new Object[0]);
                        return;
                    }
                    HwLaaContentAware hwLaaContentAware = HwLaaContentAware.this;
                    HwQoEUdpNetWorkInfo unused = hwLaaContentAware.mCurrUdpInfoForMonitor = hwLaaContentAware.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(HwLaaContentAware.this.mCurrMoniorUid, 0);
                    HwLaaContentAware hwLaaContentAware2 = HwLaaContentAware.this;
                    hwLaaContentAware2.calculateNewUdpAccessScore(hwLaaContentAware2.mCurrUdpInfoForMonitor, HwLaaContentAware.this.mLastUdpInfoForMonitor);
                    if (HwLaaContentAware.this.mLastUdpInfoForMonitor == null) {
                        HwQoEUdpNetWorkInfo unused2 = HwLaaContentAware.this.mLastUdpInfoForMonitor = new HwQoEUdpNetWorkInfo();
                    }
                    HwLaaContentAware.this.mLastUdpInfoForMonitor.setUdpNetWorkInfo(HwLaaContentAware.this.mCurrUdpInfoForMonitor);
                    HwLaaUtils.logD(HwLaaContentAware.TAG, false, "AppSensitivityScore = %{public}d, RequestLaaDisEnabled = %{public}s ,isSensitiveApp = %{public}s", Integer.valueOf(HwLaaContentAware.this.mAppSensitivityScore), String.valueOf(HwLaaContentAware.this.isRequestLaaDisEnabled), String.valueOf(HwLaaContentAware.this.isSensitiveApp));
                    if (!HwLaaContentAware.this.isRequestLaaDisEnabled && HwLaaContentAware.this.mAppSensitivityScore >= 2) {
                        boolean unused3 = HwLaaContentAware.this.requestSendLaaCmd(0);
                    } else if (HwLaaContentAware.this.isRequestLaaDisEnabled && HwLaaContentAware.this.mAppSensitivityScore == 0) {
                        boolean unused4 = HwLaaContentAware.this.requestSendLaaCmd(1);
                    }
                    if (HwLaaContentAware.this.isSensitiveApp) {
                        if (HwLaaContentAware.this.mHwLaaContentAwareHandler.hasMessages(1)) {
                            HwLaaUtils.logD(HwLaaContentAware.TAG, false, "the message already exists,remove it", new Object[0]);
                            HwLaaContentAware.this.mHwLaaContentAwareHandler.removeMessages(1);
                        }
                        HwLaaContentAware.this.mHwLaaContentAwareHandler.sendEmptyMessageDelayed(1, HwLaaContentAware.UDP_ACCESS_MONITOR_INTERVAL);
                    }
                }
            }
        };
    }

    private class HwProcessObserver extends IProcessObserver.Stub {
        private HwProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities) {
                HwLaaUtils.logD(HwLaaContentAware.TAG, false, "uid name :%{public}s, uid:%{public}d, moniorUid:%{public}d", HwLaaContentAware.this.getAppNameUid(uid), Integer.valueOf(uid), Integer.valueOf(HwLaaContentAware.this.mCurrMoniorUid));
            }
            if (foregroundActivities && HwLaaContentAware.this.mSensitiveAppHashMap.containsValue(Integer.valueOf(uid))) {
                HwLaaUtils.logD(HwLaaContentAware.TAG, false, "SensitiveApp is foregroundActivities, isLaaContentAwareEnabled = %{public}s", String.valueOf(HwLaaContentAware.this.isLaaContentAwareEnabled));
                boolean unused = HwLaaContentAware.this.isSensitiveApp = true;
                int unused2 = HwLaaContentAware.this.mCurrMoniorUid = uid;
                if (HwLaaContentAware.this.isLaaContentAwareEnabled && !HwLaaContentAware.this.mHwLaaContentAwareHandler.hasMessages(1)) {
                    HwLaaContentAware.this.mHwLaaContentAwareHandler.sendEmptyMessage(1);
                }
            } else if (HwLaaContentAware.this.isSensitiveApp && !foregroundActivities && HwLaaContentAware.this.mSensitiveAppHashMap.containsValue(Integer.valueOf(uid))) {
                HwLaaContentAware hwLaaContentAware = HwLaaContentAware.this;
                String unused3 = hwLaaContentAware.mForegroundName = hwLaaContentAware.getForegroundActivity();
                HwLaaUtils.logD(HwLaaContentAware.TAG, false, "SensitiveApp is BackgroundActivities,isRequestLaaDisEnabled:%{public}s, foregroundName:%{public}s", String.valueOf(HwLaaContentAware.this.isRequestLaaDisEnabled), HwLaaContentAware.this.mForegroundName);
                if (!TextUtils.isEmpty(HwLaaContentAware.this.mForegroundName)) {
                    String access$1300 = HwLaaContentAware.this.mForegroundName;
                    HwLaaContentAware hwLaaContentAware2 = HwLaaContentAware.this;
                    if (access$1300.equals(hwLaaContentAware2.getAppNameUid(hwLaaContentAware2.mCurrMoniorUid))) {
                        HwLaaUtils.logD(HwLaaContentAware.TAG, false, "SensitiveApp is Not BackgroundActivities", new Object[0]);
                        return;
                    }
                }
                boolean unused4 = HwLaaContentAware.this.isSensitiveApp = false;
                if (HwLaaContentAware.this.mHwLaaContentAwareHandler.hasMessages(1)) {
                    HwLaaUtils.logD(HwLaaContentAware.TAG, false, "BackgroundActivities,removeMessages: MSG_APP_UDP_MONITOR", new Object[0]);
                    HwLaaContentAware.this.mHwLaaContentAwareHandler.removeMessages(1);
                }
                if (HwLaaContentAware.this.isRequestLaaDisEnabled) {
                    boolean unused5 = HwLaaContentAware.this.requestSendLaaCmd(1);
                }
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            if (uid == HwLaaContentAware.this.mCurrMoniorUid && HwLaaContentAware.this.mSensitiveAppHashMap.containsValue(Integer.valueOf(uid))) {
                HwLaaContentAware hwLaaContentAware = HwLaaContentAware.this;
                String unused = hwLaaContentAware.mForegroundName = hwLaaContentAware.getForegroundActivity();
                HwLaaUtils.logD(HwLaaContentAware.TAG, false, "onProcessDied,  foregroundName:%{public}s", HwLaaContentAware.this.mForegroundName);
            }
        }
    }

    public class AppInstallReceiver extends BroadcastReceiver {
        public AppInstallReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            int add_uid;
            if (intent != null) {
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REPLACED".equals(action)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    HwLaaUtils.logD(HwLaaContentAware.TAG, false, " add_packageName = %{public}s", packageName);
                    if (!TextUtils.isEmpty(packageName) && HwLaaUtils.matchSensitiveApp(packageName) && (add_uid = HwLaaContentAware.this.getAppUid(packageName)) > 0) {
                        HwLaaContentAware.this.mSensitiveAppHashMap.put(packageName, Integer.valueOf(add_uid));
                    }
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    String packageName2 = intent.getData().getSchemeSpecificPart();
                    HwLaaUtils.logD(HwLaaContentAware.TAG, false, " removed_packageName = %{public}s", packageName2);
                    if (!TextUtils.isEmpty(packageName2) && HwLaaUtils.matchSensitiveApp(packageName2) && HwLaaContentAware.this.mSensitiveAppHashMap.containsKey(packageName2)) {
                        HwLaaContentAware.this.mSensitiveAppHashMap.remove(packageName2);
                    }
                }
            }
        }
    }

    private void initialSensitiveAppHashMap() {
        this.mSensitiveAppHashMap = new HashMap();
        for (int i = 0; i < HwLaaUtils.DELAY_SENSITIVE_APPS.length; i++) {
            int uid = getAppUid(HwLaaUtils.DELAY_SENSITIVE_APPS[i]);
            if (uid > 0) {
                this.mSensitiveAppHashMap.put(HwLaaUtils.DELAY_SENSITIVE_APPS[i], Integer.valueOf(uid));
            }
        }
    }

    /* access modifiers changed from: private */
    public int getAppUid(String processName) {
        if (TextUtils.isEmpty(processName)) {
            return -1;
        }
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(processName, 1);
            if (ai == null) {
                return -1;
            }
            int uid = ai.uid;
            HwLaaUtils.logD(TAG, false, "packageName = %{public}s, uid = %{public}d", processName, Integer.valueOf(uid));
            return uid;
        } catch (PackageManager.NameNotFoundException e) {
            HwLaaUtils.logD(TAG, false, "NameNotFoundException: %{public}s", e.getMessage());
            return -1;
        }
    }

    private String getAppName(int pID) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = this.mActivityManager.getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID) {
                return appProcess.processName;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public String getAppNameUid(int uid) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = this.mActivityManager.getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.uid == uid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public synchronized String getForegroundActivity() {
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = this.mActivityManager.getRunningTasks(1);
        if (runningTaskInfos != null) {
            if (!runningTaskInfos.isEmpty()) {
                ActivityManager.RunningTaskInfo mRunningTask = runningTaskInfos.get(0);
                if (mRunningTask == null) {
                    HwLaaUtils.logD(TAG, false, "failed to get RunningTaskInfo", new Object[0]);
                    return null;
                }
                return mRunningTask.topActivity.getPackageName();
            }
        }
        HwLaaUtils.logD(TAG, false, "running task is null, ams is abnormal!!!", new Object[0]);
        return null;
    }

    private void registerProcessObserver() {
        this.mHwProcessObserver = new HwProcessObserver();
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mHwProcessObserver);
        } catch (RemoteException e) {
            HwLaaUtils.logD(TAG, false, "register process observer failed,%{public}s", e.getMessage());
        }
        this.mBroadcastReceiver = new AppInstallReceiver();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        this.mIntentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        this.mIntentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        this.mIntentFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    /* access modifiers changed from: private */
    public void calculateNewUdpAccessScore(HwQoEUdpNetWorkInfo currUdpInfo, HwQoEUdpNetWorkInfo lastUdpInfo) {
        if (currUdpInfo == null || lastUdpInfo == null) {
            this.mAppSensitivityScore = 0;
        } else if (currUdpInfo.getUid() != lastUdpInfo.getUid()) {
            HwLaaUtils.logD(TAG, false, "uid is error,ignore calculate score", new Object[0]);
            this.mAppSensitivityScore = 0;
        } else {
            long timestamp = currUdpInfo.getTimestamp() - lastUdpInfo.getTimestamp();
            if (timestamp <= 0 || timestamp > 10000) {
                HwLaaUtils.logD(TAG, false, "[timestamp]: %{public}s", String.valueOf(timestamp));
                this.mAppSensitivityScore = 0;
                return;
            }
            long txUdpPackets = currUdpInfo.getTxUdpPackets() - lastUdpInfo.getTxUdpPackets();
            long rxUdpPackets = currUdpInfo.getRxUdpPackets() - lastUdpInfo.getRxUdpPackets();
            HwLaaUtils.logD(TAG, false, "[timestamp]: %{public}s, [txUdpPackets]: %{public}s, [rxUdpPackets]: %{public}s", String.valueOf(timestamp), String.valueOf(txUdpPackets), String.valueOf(rxUdpPackets));
            long stamp = timestamp / 1000;
            if (stamp == 0) {
                stamp = 1;
            }
            if ((rxUdpPackets + txUdpPackets) / stamp > 10) {
                int i = this.mAppSensitivityScore;
                if (i < 2) {
                    this.mAppSensitivityScore = i + 1;
                    return;
                }
                return;
            }
            int i2 = this.mAppSensitivityScore;
            if (i2 > 0) {
                this.mAppSensitivityScore = i2 - 1;
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean requestSendLaaCmd(int cmd) {
        this.mHwLaaControllerHandler.sendMessage(this.mHwLaaControllerHandler.obtainMessage(1, cmd, 5));
        if (cmd == 0) {
            this.isRequestLaaDisEnabled = true;
        } else if (1 == cmd) {
            this.isRequestLaaDisEnabled = false;
        }
        return true;
    }
}
