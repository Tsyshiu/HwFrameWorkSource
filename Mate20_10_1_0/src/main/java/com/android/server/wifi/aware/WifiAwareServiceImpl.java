package com.android.server.wifi.aware;

import android.app.AppOpsManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.wifi.Clock;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class WifiAwareServiceImpl extends IWifiAwareManager.Stub {
    private static final String TAG = "WifiAwareService";
    private static final boolean VDBG = false;
    private AppOpsManager mAppOps;
    /* access modifiers changed from: private */
    public Context mContext;
    boolean mDbg = false;
    /* access modifiers changed from: private */
    public final SparseArray<IBinder.DeathRecipient> mDeathRecipientsByClientId = new SparseArray<>();
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private int mNextClientId = 1;
    private WifiAwareShellCommand mShellCommand;
    /* access modifiers changed from: private */
    public WifiAwareStateManager mStateManager;
    /* access modifiers changed from: private */
    public final SparseIntArray mUidByClientId = new SparseIntArray();
    private WifiPermissionsUtil mWifiPermissionsUtil;

    public WifiAwareServiceImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void start(HandlerThread handlerThread, final WifiAwareStateManager awareStateManager, WifiAwareShellCommand awareShellCommand, WifiAwareMetrics awareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionsWrapper, final FrameworkFacade frameworkFacade, final WifiAwareNativeManager wifiAwareNativeManager, final WifiAwareNativeApi wifiAwareNativeApi, final WifiAwareNativeCallback wifiAwareNativeCallback) {
        Log.i(TAG, "Starting Wi-Fi Aware service");
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mStateManager = awareStateManager;
        this.mShellCommand = awareShellCommand;
        this.mStateManager.start(this.mContext, handlerThread.getLooper(), awareMetrics, wifiPermissionsUtil, permissionsWrapper, new Clock());
        frameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(new Handler(handlerThread.getLooper())) {
            /* class com.android.server.wifi.aware.WifiAwareServiceImpl.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                WifiAwareServiceImpl wifiAwareServiceImpl = WifiAwareServiceImpl.this;
                wifiAwareServiceImpl.enableVerboseLogging(frameworkFacade.getIntegerSetting(wifiAwareServiceImpl.mContext, "wifi_verbose_logging_enabled", 0), awareStateManager, wifiAwareNativeManager, wifiAwareNativeApi, wifiAwareNativeCallback);
            }
        });
        enableVerboseLogging(frameworkFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0), awareStateManager, wifiAwareNativeManager, wifiAwareNativeApi, wifiAwareNativeCallback);
    }

    /* access modifiers changed from: private */
    public void enableVerboseLogging(int verbose, WifiAwareStateManager awareStateManager, WifiAwareNativeManager wifiAwareNativeManager, WifiAwareNativeApi wifiAwareNativeApi, WifiAwareNativeCallback wifiAwareNativeCallback) {
        boolean dbg;
        if (verbose > 0) {
            dbg = true;
        } else {
            dbg = false;
        }
        this.mDbg = dbg;
        awareStateManager.mDbg = dbg;
        if (awareStateManager.mDataPathMgr != null) {
            awareStateManager.mDataPathMgr.mDbg = dbg;
            WifiInjector.getInstance().getWifiMetrics().getWifiAwareMetrics().mDbg = dbg;
        }
        wifiAwareNativeCallback.mDbg = dbg;
        wifiAwareNativeManager.mDbg = dbg;
        wifiAwareNativeApi.mDbg = dbg;
    }

    public void startLate() {
        Log.i(TAG, "Late initialization of Wi-Fi Aware service");
        this.mStateManager.startLate();
    }

    public boolean isUsageEnabled() {
        enforceAccessPermission();
        return this.mStateManager.isUsageEnabled();
    }

    public Characteristics getCharacteristics() {
        enforceAccessPermission();
        if (this.mStateManager.getCapabilities() == null) {
            return null;
        }
        return this.mStateManager.getCapabilities().toPublicCharacteristics();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:31:0x00b1, code lost:
        r0 = th;
     */
    public void connect(final IBinder binder, String callingPackage, IWifiAwareEventCallback callback, ConfigRequest configRequest, boolean notifyOnIdentityChanged) {
        ConfigRequest configRequest2;
        final int clientId;
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        this.mAppOps.checkPackage(uid, callingPackage);
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (binder != null) {
            if (notifyOnIdentityChanged) {
                enforceLocationPermission(callingPackage, getMockableCallingUid());
            }
            if (configRequest != null) {
                enforceNetworkStackPermission();
                configRequest2 = configRequest;
            } else {
                configRequest2 = new ConfigRequest.Builder().build();
            }
            configRequest2.validate();
            int pid = getCallingPid();
            synchronized (this.mLock) {
                clientId = this.mNextClientId;
                this.mNextClientId = clientId + 1;
            }
            if (this.mDbg) {
                Log.v(TAG, "connect: uid=" + uid + ", clientId=" + clientId + ", configRequest" + configRequest2 + ", notifyOnIdentityChanged=" + notifyOnIdentityChanged);
            }
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                /* class com.android.server.wifi.aware.WifiAwareServiceImpl.AnonymousClass2 */

                public void binderDied() {
                    if (WifiAwareServiceImpl.this.mDbg) {
                        Log.v(WifiAwareServiceImpl.TAG, "binderDied: clientId=" + clientId);
                    }
                    binder.unlinkToDeath(this, 0);
                    synchronized (WifiAwareServiceImpl.this.mLock) {
                        WifiAwareServiceImpl.this.mDeathRecipientsByClientId.delete(clientId);
                        WifiAwareServiceImpl.this.mUidByClientId.delete(clientId);
                    }
                    WifiAwareServiceImpl.this.mStateManager.disconnect(clientId);
                }
            };
            try {
                binder.linkToDeath(dr, 0);
                synchronized (this.mLock) {
                    this.mDeathRecipientsByClientId.put(clientId, dr);
                    this.mUidByClientId.put(clientId, uid);
                }
                this.mStateManager.connect(clientId, uid, pid, callingPackage, callback, configRequest2, notifyOnIdentityChanged);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error on linkToDeath - " + e);
                try {
                    callback.onConnectFail(1);
                    return;
                } catch (RemoteException e2) {
                    Log.e(TAG, "Error on onConnectFail()");
                    return;
                }
            }
        } else {
            throw new IllegalArgumentException("Binder must not be null");
        }
        while (true) {
        }
    }

    public void disconnect(int clientId, IBinder binder) {
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        enforceClientValidity(uid, clientId);
        if (this.mDbg) {
            Log.v(TAG, "disconnect: uid=" + uid + ", clientId=" + clientId);
        }
        if (binder != null) {
            synchronized (this.mLock) {
                IBinder.DeathRecipient dr = this.mDeathRecipientsByClientId.get(clientId);
                if (dr != null) {
                    binder.unlinkToDeath(dr, 0);
                    this.mDeathRecipientsByClientId.delete(clientId);
                }
                this.mUidByClientId.delete(clientId);
            }
            this.mStateManager.disconnect(clientId);
            return;
        }
        throw new IllegalArgumentException("Binder must not be null");
    }

    public void terminateSession(int clientId, int sessionId) {
        enforceAccessPermission();
        enforceChangePermission();
        enforceClientValidity(getMockableCallingUid(), clientId);
        this.mStateManager.terminateSession(clientId, sessionId);
    }

    public void publish(String callingPackage, int clientId, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback callback) {
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        this.mAppOps.checkPackage(uid, callingPackage);
        enforceLocationPermission(callingPackage, getMockableCallingUid());
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (publishConfig != null) {
            publishConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(uid, clientId);
            this.mStateManager.publish(clientId, publishConfig, callback);
        } else {
            throw new IllegalArgumentException("PublishConfig must not be null");
        }
    }

    public void updatePublish(int clientId, int sessionId, PublishConfig publishConfig) {
        enforceAccessPermission();
        enforceChangePermission();
        if (publishConfig != null) {
            publishConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(getMockableCallingUid(), clientId);
            this.mStateManager.updatePublish(clientId, sessionId, publishConfig);
            return;
        }
        throw new IllegalArgumentException("PublishConfig must not be null");
    }

    public void subscribe(String callingPackage, int clientId, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback callback) {
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        this.mAppOps.checkPackage(uid, callingPackage);
        enforceLocationPermission(callingPackage, getMockableCallingUid());
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (subscribeConfig != null) {
            subscribeConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(uid, clientId);
            this.mStateManager.subscribe(clientId, subscribeConfig, callback);
        } else {
            throw new IllegalArgumentException("SubscribeConfig must not be null");
        }
    }

    public void updateSubscribe(int clientId, int sessionId, SubscribeConfig subscribeConfig) {
        enforceAccessPermission();
        enforceChangePermission();
        if (subscribeConfig != null) {
            subscribeConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(getMockableCallingUid(), clientId);
            this.mStateManager.updateSubscribe(clientId, sessionId, subscribeConfig);
            return;
        }
        throw new IllegalArgumentException("SubscribeConfig must not be null");
    }

    public void sendMessage(int clientId, int sessionId, int peerId, byte[] message, int messageId, int retryCount) {
        enforceAccessPermission();
        enforceChangePermission();
        if (retryCount != 0) {
            enforceNetworkStackPermission();
        }
        if (message != null && message.length > this.mStateManager.getCharacteristics().getMaxServiceSpecificInfoLength()) {
            throw new IllegalArgumentException("Message length longer than supported by device characteristics");
        } else if (retryCount < 0 || retryCount > DiscoverySession.getMaxSendRetryCount()) {
            throw new IllegalArgumentException("Invalid 'retryCount' must be non-negative and <= DiscoverySession.MAX_SEND_RETRY_COUNT");
        } else {
            enforceClientValidity(getMockableCallingUid(), clientId);
            this.mStateManager.sendMessage(clientId, sessionId, peerId, message, messageId, retryCount);
        }
    }

    public void requestMacAddresses(int uid, List peerIds, IWifiAwareMacAddressProvider callback) {
        enforceNetworkStackPermission();
        this.mStateManager.requestMacAddresses(uid, peerIds, callback);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wifi.aware.WifiAwareServiceImpl */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        this.mShellCommand.exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiAwareService from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Wi-Fi Aware Service");
        synchronized (this.mLock) {
            pw.println("  mNextClientId: " + this.mNextClientId);
            pw.println("  mDeathRecipientsByClientId: " + this.mDeathRecipientsByClientId);
            pw.println("  mUidByClientId: " + this.mUidByClientId);
        }
        this.mStateManager.dump(fd, pw, args);
    }

    private void enforceClientValidity(int uid, int clientId) {
        synchronized (this.mLock) {
            int uidIndex = this.mUidByClientId.indexOfKey(clientId);
            if (uidIndex < 0 || this.mUidByClientId.valueAt(uidIndex) != uid) {
                throw new SecurityException("Attempting to use invalid uid+clientId mapping: uid=" + uid + ", clientId=" + clientId);
            }
        }
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationPermission(String callingPackage, int uid) {
        this.mWifiPermissionsUtil.enforceLocationPermission(callingPackage, uid);
    }

    private void enforceNetworkStackPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
    }
}
