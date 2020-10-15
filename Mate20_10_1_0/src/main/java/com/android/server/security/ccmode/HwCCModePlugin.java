package com.android.server.security.ccmode;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.admin.SecurityLog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.security.core.IHwSecurityPlugin;
import huawei.android.security.IHwCCModePlugin;

public class HwCCModePlugin extends IHwCCModePlugin.Stub implements IHwSecurityPlugin {
    public static final Object BINDLOCK = new Object();
    public static final int CLOSE_CCMODE_SUCCESS = 102;
    public static final Creator CREATOR = new Creator() {
        /* class com.android.server.security.ccmode.HwCCModePlugin.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            if (HwCCModePlugin.HWDBG) {
                Slog.d(HwCCModePlugin.TAG, "createPlugin");
            }
            return new HwCCModePlugin(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return HwCCModePlugin.USE_CCMODE;
        }
    };
    private static final String ENCRYPTED_TAG = "encrypted";
    private static final int EVENT_SELFTEST_RESULT = 0;
    /* access modifiers changed from: private */
    public static final boolean HWDBG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String INTERVAL = "interval";
    private static final int INTERVAL_VALUE = 1;
    private static final int MSG_SELFTEST_FAILED = 0;
    private static final String NOWAIT = "nowait";
    private static final int NOWAIT_VALUE = 1;
    public static final int NO_SCREEN_LOCK_ERROR = 103;
    public static final int OPEN_CCMODE_SUCCESS = 101;
    public static final int PLATFORM_NOT_SUPPORTED = 107;
    public static final int PREPARE_ENABLECCMODE_SUCCESS = 106;
    private static final String PROPERTIES_CC_MODE = "ro.boot.sys.ccmode";
    private static final String RO_CRYPTO_STATE = "ro.crypto.state";
    private static final String RO_CRYPTO_STATE_DEFAULT = "0";
    public static final int SCREEN_LOOK_MANAGER_NULL_ERROR = 105;
    private static final int SELFTEST_ERROR = 0;
    private static final int SELFTEST_OK = 1;
    private static final String TAG = "HwCCMD";
    public static final int UNENCRYPT_STORAGE_ERROR = 104;
    private static final String USE_CCMODE = "com.huawei.permission.USE_CCMODE";
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        /* class com.android.server.security.ccmode.HwCCModePlugin.AnonymousClass2 */

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                HwCCModePlugin.this.waitForConfirm();
            }
        }
    };

    public HwCCModePlugin(Context context) {
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.security.ccmode.HwCCModePlugin */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        return this;
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
        if (isCCModeOn()) {
            new Thread() {
                /* class com.android.server.security.ccmode.HwCCModePlugin.AnonymousClass3 */

                public void run() {
                    boolean testOk;
                    long beginTime = System.currentTimeMillis();
                    SecurityLog.writeEvent(210038, new Object[0]);
                    try {
                        testOk = CCModeFuncInternal.native_dxCryptoSelftest() && CCModeFuncInternal.native_kernelCryptoSelftest() && CCModeFuncInternal.native_boringSslSelftest();
                        SecurityLog.writeEvent(210031, new Object[]{Integer.valueOf(testOk ? 1 : 0)});
                    } catch (UnsatisfiedLinkError e) {
                        testOk = true;
                        Slog.e(HwCCModePlugin.TAG, "self test jni error");
                    }
                    long costTime = System.currentTimeMillis() - beginTime;
                    if (HwCCModePlugin.HWDBG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("CC mode self test ");
                        sb.append(testOk ? "OK" : "failed");
                        sb.append("!!!");
                        Slog.w(HwCCModePlugin.TAG, sb.toString());
                        Slog.w(HwCCModePlugin.TAG, "selfTest cost:" + costTime);
                    }
                    if (!testOk) {
                        HwCCModePlugin.this.mHandler.sendEmptyMessage(0);
                    }
                }
            }.start();
        }
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
    }

    public int openCCMode() throws RemoteException {
        checkPermission(USE_CCMODE);
        if (HWDBG) {
            Slog.i(TAG, "Open CCMode !");
        }
        if (isCCModeOn()) {
            return 101;
        }
        int prepareRst = prepareEnableCCMode();
        if (106 != prepareRst) {
            return prepareRst;
        }
        setCCMode(true);
        if (HWDBG) {
            Slog.d(TAG, "reboot system");
        }
        Intent intent = new Intent("android.intent.action.REBOOT");
        intent.putExtra(NOWAIT, 1);
        intent.putExtra("interval", 1);
        this.mContext.sendBroadcast(intent);
        return 101;
    }

    public int closeCCMode() throws RemoteException {
        checkPermission(USE_CCMODE);
        if (HWDBG) {
            Slog.i(TAG, "Close CCMode !");
        }
        if (!isCCModeOn()) {
            return 102;
        }
        setCCMode(false);
        return 102;
    }

    private boolean isCCModeOn() {
        return SystemProperties.getBoolean(PROPERTIES_CC_MODE, false);
    }

    private void setCCMode(boolean isOn) {
        SystemProperties.set(PROPERTIES_CC_MODE, "" + isOn);
    }

    private int prepareEnableCCMode() {
        KeyguardManager screenLockManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (screenLockManager == null) {
            Slog.e(TAG, "screenLockManager is null !");
            return 105;
        } else if (!ENCRYPTED_TAG.equals(SystemProperties.get(RO_CRYPTO_STATE, "0"))) {
            return 104;
        } else {
            if (!screenLockManager.isKeyguardSecure()) {
                return 103;
            }
            return 106;
        }
    }

    /* access modifiers changed from: private */
    public void waitForConfirm() {
        if (HWDBG) {
            Slog.d(TAG, "wait for user to confirm");
        }
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(this.mContext.getString(33686165)).setMessage(this.mContext.getString(33686168)).setNeutralButton(this.mContext.getString(33685561), new DialogInterface.OnClickListener() {
            /* class com.android.server.security.ccmode.HwCCModePlugin.AnonymousClass4 */

            public void onClick(DialogInterface dialog, int which) {
                if (HwCCModePlugin.HWDBG) {
                    Slog.d(HwCCModePlugin.TAG, "Entering recovery mode");
                }
                ((PowerManager) HwCCModePlugin.this.mContext.getSystemService("power")).reboot("recovery");
            }
        }).setCancelable(false).create();
        dialog.getWindow().setType(2003);
        dialog.show();
    }

    private void checkPermission(String permission) {
        Context context = this.mContext;
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }
}
