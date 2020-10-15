package com.android.server.policy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Window;
import com.android.internal.globalactions.Action;
import com.android.internal.globalactions.ToggleAction;
import com.android.internal.policy.HwPhoneLayoutInflater;
import com.android.internal.policy.HwPhoneWindow;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.PhoneLayoutInflater;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.HwPolicyFactory;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.HwKeyguardStateMonitor;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize;
import huawei.com.android.server.policy.BootMessageActions;
import huawei.com.android.server.policy.HwGlobalActions;
import java.util.ArrayList;

public class HwPolicyFactoryImpl implements HwPolicyFactory.Factory {
    private static final String ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE";
    private static final String ACTION_HWSYSTEMMANAGER_START_SUPER_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_START_SUPER_POWERMODE";
    private static final String ACTION_USE_POWER_GENIE_CHANGE_MODE = "huawei.intent.action.PG_EXTREME_MODE_ENABLE_ACTION";
    private static final String TAG = "HwPolicyFactoryImpl";
    /* access modifiers changed from: private */
    public static Dialog mExitDialog;
    private boolean isScroll = false;
    private IBinder mAwareService = null;
    BootMessageActions mBootMessageActions;
    HwGlobalActions mHwGlobalActions;
    private ToggleAction mUltraPowerSaveOn;
    private ToggleAction.State mUltraPowerState = ToggleAction.State.Off;

    public void addUltraPowerSaveImpl(ArrayList mItems, final Context context) {
        if (Settings.Global.getInt(context.getContentResolver(), "device_provisioned", -1) != 0) {
            this.mUltraPowerSaveOn = new ToggleAction(33751437, 33751436, 33685710, 33685711, 33685712) {
                /* class com.android.server.policy.HwPolicyFactoryImpl.AnonymousClass1 */

                public void onToggle(boolean on) {
                    HwPolicyFactoryImpl.this.changeUltraPowerSaveSetting(on, context);
                }

                public boolean showDuringKeyguard() {
                    return true;
                }

                public boolean showBeforeProvisioning() {
                    return true;
                }
            };
            mItems.add(this.mUltraPowerSaveOn);
            this.mUltraPowerState = SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false) ? ToggleAction.State.On : ToggleAction.State.Off;
            this.mUltraPowerSaveOn.updateState(this.mUltraPowerState);
        }
    }

    /* access modifiers changed from: private */
    public void changeUltraPowerSaveSetting(boolean on, Context context) {
        if (on) {
            context.sendBroadcast(new Intent(ACTION_HWSYSTEMMANAGER_START_SUPER_POWERMODE));
        } else {
            showExitDialog(context);
        }
    }

    /* access modifiers changed from: private */
    public void sendExitLimitPowerModeBroadcast(Context context) {
        SystemProperties.set(GestureNavConst.KEY_SUPER_SAVE_MODE, "false");
        Intent intent = new Intent(ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE);
        intent.putExtra("shutdomn_limit_powermode", 0);
        context.sendBroadcast(intent);
        Intent callPowerGenieIntent = new Intent("huawei.intent.action.PG_EXTREME_MODE_ENABLE_ACTION");
        callPowerGenieIntent.putExtra("enable", false);
        context.sendBroadcast(callPowerGenieIntent);
    }

    private void showExitDialog(Context context) {
        ContextThemeWrapper themeContext = new ContextThemeWrapper(context, context.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null));
        Dialog dialog = mExitDialog;
        if (dialog != null) {
            dialog.dismiss();
        }
        mExitDialog = createExitDialog(themeContext);
        mExitDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS);
        mExitDialog.show();
    }

    private Dialog createExitDialog(final Context context) {
        return new AlertDialog.Builder(context).setTitle(33685708).setMessage(33685709).setPositiveButton(33685707, new DialogInterface.OnClickListener() {
            /* class com.android.server.policy.HwPolicyFactoryImpl.AnonymousClass2 */

            public void onClick(DialogInterface arg0, int arg1) {
                HwPolicyFactoryImpl.this.sendExitLimitPowerModeBroadcast(context);
                if (HwPolicyFactoryImpl.mExitDialog != null) {
                    HwPolicyFactoryImpl.mExitDialog.dismiss();
                }
            }
        }).setNegativeButton(17039360, (DialogInterface.OnClickListener) null).create();
    }

    public void addRebootMenu(ArrayList<Action> items) {
        items.add(new ToggleAction(33751040, 33751040, 33685515, 33685516, 33685516) {
            /* class com.android.server.policy.HwPolicyFactoryImpl.AnonymousClass3 */

            public void onToggle(boolean on) {
                try {
                    IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
                    if (pm != null) {
                        pm.reboot(true, "huawei_reboot", false);
                    }
                } catch (RemoteException e) {
                    Log.e(HwPolicyFactoryImpl.TAG, "PowerManager service died!", e);
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        });
    }

    public WindowManagerPolicy getHwPhoneWindowManager() {
        return new HwPhoneWindowManager();
    }

    public Window getHwPhoneWindow(Context context) {
        return new HwPhoneWindow(context);
    }

    public PhoneLayoutInflater getHwPhoneLayoutInflater(Context context) {
        return new HwPhoneLayoutInflater(context);
    }

    public void showHwGlobalActionsFragment(Context tContext, WindowManagerPolicy.WindowManagerFuncs tWindowManagerFuncs, PowerManager powerManager, boolean keyguardShowing, boolean keyguardSecure, boolean isDeviceProvisioned) {
        if (this.mHwGlobalActions == null) {
            this.mHwGlobalActions = new HwGlobalActions(tContext, tWindowManagerFuncs);
        }
        this.mHwGlobalActions.showDialog(keyguardShowing, keyguardSecure, isDeviceProvisioned);
        if (keyguardShowing) {
            powerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    public void showBootMessage(Context tContext, int curr, int total) {
        if (this.mBootMessageActions == null) {
            this.mBootMessageActions = new BootMessageActions(tContext);
        }
        this.mBootMessageActions.showBootMessage(curr, total);
    }

    public void hideBootMessage() {
        BootMessageActions bootMessageActions = this.mBootMessageActions;
        if (bootMessageActions != null) {
            bootMessageActions.hideBootMessage();
        }
    }

    public boolean isHwGlobalActionsShowing() {
        HwGlobalActions hwGlobalActions = this.mHwGlobalActions;
        return hwGlobalActions != null && hwGlobalActions.isHwGlobalActionsShowing();
    }

    public boolean isHwFastShutdownEnable() {
        HwGlobalActions hwGlobalActions = this.mHwGlobalActions;
        return hwGlobalActions != null && hwGlobalActions.isHwFastShutdownEnable();
    }

    public boolean ifUseHwGlobalActions() {
        return true;
    }

    public void reportToAware(int code, int duration) {
        if (!AwareSceneRecognize.isEnable()) {
            Log.w(TAG, "mAware is disable");
            return;
        }
        if (code != 15007) {
            if (code == 15009) {
                this.isScroll = false;
            } else if (code == 85007) {
                if (this.isScroll) {
                    this.isScroll = false;
                } else {
                    return;
                }
            }
        } else if (!this.isScroll) {
            this.isScroll = true;
        } else {
            return;
        }
        if (this.mAwareService == null) {
            this.mAwareService = ServiceManager.getService("hwsysresmanager");
        }
        if (this.mAwareService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken("android.rms.IHwSysResManager");
                data.writeInt(duration);
                this.mAwareService.transact(code, data, reply, 0);
                reply.readException();
            } catch (RemoteException e) {
                Log.e(TAG, "mAwareService ontransact failed");
            } catch (Throwable th) {
                data.recycle();
                reply.recycle();
                throw th;
            }
            data.recycle();
            reply.recycle();
            return;
        }
        Log.e(TAG, "mAwareService is not start");
    }

    public KeyguardStateMonitor getHwKeyguardStateMonitor(Context context, IKeyguardService service, KeyguardStateMonitor.StateCallback callback) {
        return new HwKeyguardStateMonitor(context, service, callback);
    }
}
