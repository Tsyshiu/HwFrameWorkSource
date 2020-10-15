package com.android.server.wifi.fastsleep;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.RemoteException;
import android.util.wifi.HwHiLog;
import com.android.server.hidata.appqoe.HwAPPQoEResourceManger;
import java.util.List;

public class FsMonitor {
    private static final int CURRENT_TASK_NUMBER = 0;
    private static final int RUNNING_TASK_NUMBER = 1;
    private static final String TAG = "FSMonitor";
    /* access modifiers changed from: private */
    public int mActiveNetworkType = 101;
    private ActivityManager mActivityManager = null;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext;
    private FsBcastReceiver mFsBcastReceiver = null;
    private ConnectivityManager.NetworkCallback mFsNetwrokCallback = null;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private HwProcessObserver mHwProcessObserver = null;
    /* access modifiers changed from: private */
    public boolean mIsWifiConnected = false;

    public FsMonitor(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        registerBroadcastReceiver();
        registerProcessObserver();
        registerNetworkChangeCallback();
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mFsBcastReceiver = new FsBcastReceiver();
        this.mContext.registerReceiver(this.mFsBcastReceiver, filter);
    }

    private class FsBcastReceiver extends BroadcastReceiver {
        private boolean mIsP2pConnected;

        private FsBcastReceiver() {
            this.mIsP2pConnected = false;
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(action)) {
                    handleP2pConnectionChanged(intent);
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    handleNetworkStateChanged(intent);
                } else {
                    HwHiLog.d(FsMonitor.TAG, false, "FSBcastReceiver unknow action: %{public}s", new Object[]{action});
                }
            }
        }

        private void handleP2pConnectionChanged(Intent intent) {
            NetworkInfo p2pNetworkInfo;
            if (intent != null && (p2pNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo")) != null) {
                boolean isP2pConnected = p2pNetworkInfo.isConnected();
                HwHiLog.d(FsMonitor.TAG, false, "handleP2pConnectionChanged: %{public}s", new Object[]{Boolean.valueOf(isP2pConnected)});
                if (this.mIsP2pConnected != isP2pConnected) {
                    this.mIsP2pConnected = isP2pConnected;
                    FsMonitor.this.mHandler.sendMessage(FsMonitor.this.mHandler.obtainMessage(4, FsUtils.booleanToInt(isP2pConnected), 0));
                }
            }
        }

        private void handleNetworkStateChanged(Intent intent) {
            if (intent != null) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                NetworkInfo.DetailedState state = info == null ? NetworkInfo.DetailedState.IDLE : info.getDetailedState();
                if (state == NetworkInfo.DetailedState.CONNECTED) {
                    if (!FsMonitor.this.mIsWifiConnected) {
                        HwHiLog.d(FsMonitor.TAG, false, "handleNetworkStateChanged:MSG_WIFI_CONNECTED", new Object[0]);
                        FsMonitor.this.mHandler.sendEmptyMessage(2);
                    }
                    boolean unused = FsMonitor.this.mIsWifiConnected = true;
                } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
                    if (FsMonitor.this.mIsWifiConnected) {
                        HwHiLog.d(FsMonitor.TAG, false, "handleNetworkStateChanged:MSG_WIFI_DISCONNECTED", new Object[0]);
                        FsMonitor.this.mHandler.sendEmptyMessage(3);
                    }
                    boolean unused2 = FsMonitor.this.mIsWifiConnected = false;
                } else {
                    HwHiLog.d(FsMonitor.TAG, false, "handleNetworkStateChanged state: %{public}s", new Object[]{state});
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public String getTopPackageName() {
        List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
        if (tasks == null || tasks.isEmpty() || tasks.get(0) == null || tasks.get(0).topActivity == null) {
            return "";
        }
        return tasks.get(0).topActivity.getPackageName();
    }

    /* access modifiers changed from: private */
    public boolean isInGameScene(HwAPPQoEResourceManger qoeResourceManager, String appName) {
        if (qoeResourceManager == null || appName == null || qoeResourceManager.checkIsMonitorGameScence(appName) == null) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isInBlackListScene(HwAPPQoEResourceManger qoeResourceManager, String appName) {
        if (qoeResourceManager == null || appName == null || !qoeResourceManager.isInBlackListScene(appName)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isInWhiteListScene(HwAPPQoEResourceManger qoeResourceManager, String appName) {
        if (qoeResourceManager == null || appName == null || !qoeResourceManager.isInWhiteListScene(appName)) {
            return false;
        }
        return true;
    }

    private class HwProcessObserver extends IProcessObserver.Stub {
        private HwProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            HwHiLog.d(FsMonitor.TAG, false, "onForegroundActivitiesChanged: %{public}d, %{public}d, %{public}s", new Object[]{Integer.valueOf(pid), Integer.valueOf(uid), String.valueOf(foregroundActivities)});
            boolean isLowLatencyActivity = false;
            boolean isBlackListActivity = false;
            boolean isWhiteListActivity = false;
            if (!FsMonitor.this.mIsWifiConnected) {
                HwHiLog.d(FsMonitor.TAG, false, "network disconnected, ignore FGActivitiesChanged", new Object[0]);
                return;
            }
            HwAPPQoEResourceManger qoeResourceManager = HwAPPQoEResourceManger.getInstance();
            if (foregroundActivities) {
                String currentAppName = FsMonitor.this.getAppNameUid(uid);
                if (FsMonitor.this.isInGameScene(qoeResourceManager, currentAppName)) {
                    isLowLatencyActivity = true;
                }
                if (FsMonitor.this.isInBlackListScene(qoeResourceManager, currentAppName)) {
                    isBlackListActivity = true;
                }
                if (FsMonitor.this.isInWhiteListScene(qoeResourceManager, currentAppName)) {
                    isWhiteListActivity = true;
                }
                HwHiLog.d(FsMonitor.TAG, false, "foregroundActivities is %{public}s, LOWLATENCY_SCENE: %{public}sBLACKLIST_SCENE: %{public}s, WHITELIST_SCENE: %{public}s", new Object[]{currentAppName, Boolean.valueOf(isLowLatencyActivity), Boolean.valueOf(isBlackListActivity), Boolean.valueOf(isWhiteListActivity)});
            } else {
                String currentAppName2 = FsMonitor.this.getTopPackageName();
                if (FsMonitor.this.isInGameScene(qoeResourceManager, currentAppName2)) {
                    isLowLatencyActivity = true;
                }
                if (FsMonitor.this.isInBlackListScene(qoeResourceManager, currentAppName2)) {
                    isBlackListActivity = true;
                }
                if (FsMonitor.this.isInWhiteListScene(qoeResourceManager, currentAppName2)) {
                    isWhiteListActivity = true;
                }
                HwHiLog.d(FsMonitor.TAG, false, "switch to background, toppackagename is %{public}s, LOWLATENCY_SCENE: %{public}s, BLACKLIST_SCENE: %{public}s, WHITELIST_SCENE: %{public}s", new Object[]{currentAppName2, Boolean.valueOf(isLowLatencyActivity), Boolean.valueOf(isBlackListActivity), Boolean.valueOf(isWhiteListActivity)});
            }
            int lowLatencyStatus = FsUtils.booleanToInt(isLowLatencyActivity);
            FsMonitor.this.mHandler.sendMessage(FsMonitor.this.mHandler.obtainMessage(7, FsUtils.booleanToInt(isBlackListActivity), lowLatencyStatus, Integer.valueOf(FsUtils.booleanToInt(isWhiteListActivity))));
        }

        public void onProcessDied(int pid, int uid) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    }

    private class FsNetwrokCallback extends ConnectivityManager.NetworkCallback {
        private Network mLastNetwork;
        private NetworkCapabilities mLastNetworkCapabilities;

        private FsNetwrokCallback() {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            if (network == null || networkCapabilities == null) {
                HwHiLog.d(FsMonitor.TAG, false, "network or networkCapabilities is null", new Object[0]);
            } else if (!networkCapabilities.hasTransport(1)) {
                HwHiLog.d(FsMonitor.TAG, false, "network is not wifi!", new Object[0]);
                FsMonitor.this.mHandler.sendEmptyMessage(9);
            } else if (networkCapabilities.hasCapability(16)) {
                HwHiLog.d(FsMonitor.TAG, false, "networkType:TRANSPORT_WIFI, network: %{public}s", new Object[]{network.toString()});
                FsMonitor.this.mHandler.sendEmptyMessage(8);
            } else {
                HwHiLog.d(FsMonitor.TAG, false, "network not validated! send network_disconnect msg", new Object[0]);
                FsMonitor.this.mHandler.sendEmptyMessage(9);
            }
        }

        public void onLost(Network network) {
            HwHiLog.d(FsMonitor.TAG, false, "onLost", new Object[0]);
            if (network == null || !network.equals(this.mLastNetwork)) {
                HwHiLog.d(FsMonitor.TAG, false, "lost network is null or not equal to mLastNetwork", new Object[0]);
                return;
            }
            if (FsMonitor.this.mActiveNetworkType == 100) {
                HwHiLog.d(FsMonitor.TAG, false, "onNetworkCallback:MSG_WIFI_STATE_DISCONNECT", new Object[0]);
                FsMonitor.this.mHandler.sendEmptyMessage(9);
            }
            int unused = FsMonitor.this.mActiveNetworkType = 101;
        }
    }

    private void registerNetworkChangeCallback() {
        if (this.mConnectivityManager != null) {
            this.mFsNetwrokCallback = new FsNetwrokCallback();
            this.mConnectivityManager.registerDefaultNetworkCallback(this.mFsNetwrokCallback, this.mHandler);
        }
    }

    private void registerProcessObserver() {
        this.mHwProcessObserver = new HwProcessObserver();
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mHwProcessObserver);
        } catch (RemoteException e) {
            HwHiLog.e(TAG, false, "register process observer failed", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public String getAppNameUid(int uid) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList;
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager == null || (appProcessList = activityManager.getRunningAppProcesses()) == null) {
            return "";
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.uid == uid) {
                return appProcess.processName;
            }
        }
        return "";
    }
}
