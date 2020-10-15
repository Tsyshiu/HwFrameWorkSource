package com.android.server.wifi.p2p;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.wifi.HwHiLog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.p2p.HwWifiP2pService;
import java.util.Locale;

public class HwWifiP2pServiceTvEx implements IHwWifiP2pServiceTvEx {
    private static final int ALLOW_UNTRUST_INVITE = 143402;
    private static final int BASE = 143360;
    private static final int DIALOG_TIMEOUT_MS = 30000;
    private static final int DISALLOW_UNTRUST_INVITE = 143401;
    private static final int DISALLOW_UNTRUST_INVITE_DIALOG_TIMEOUT = 143404;
    private static final int DISALLOW_UNTRUST_INVITE_DURATION = 600000;
    private static final int DISALLOW_UNTRUST_INVITE_TIMEOUT = 143403;
    private static final int GROUP_CREATING_WAIT_TIME_MS = 30000;
    private static final int INVITATION_DIALOG_TIMEOUT = 143400;
    private static final int ONE_MINITE_UNIT = 60000;
    private static final int P2P_DIALOG_WIDTH = 360;
    private static final float P2P_DIP2PX_CONSTANT = 0.5f;
    private static final int P2P_LISTEN_INTERVAL = 500;
    private static final int P2P_LISTEN_INTERVAL_COEXIST_STA = 100;
    private static final int P2P_LISTEN_PERIOD = 490;
    private static final int P2P_LISTEN_PERIOD_COEXIST_STA = 10;
    private static final int P2P_REJECT_FIRST_TIME = 1;
    private static final int P2P_REJECT_MAX_COUNT = 3;
    private static final int P2P_REJECT_MAX_COUNT_LIMITED_TIME = 180000;
    private static final String TAG = "HwWifiP2pServiceTvEx";
    private static final long WAKEUP_SCREEN_TIME = 3000;
    private Context mContext;
    private IHwWifiP2pServiceInner mHwWifiP2pServiceInner = null;
    /* access modifiers changed from: private */
    public AlertDialog mInviteDialog = null;
    private int mP2pPauseUntrustInviteIndex = 0;
    /* access modifiers changed from: private */
    public HwWifiP2pService.HwP2pStateMachine mP2pStateMachine = null;
    /* access modifiers changed from: private */
    public AlertDialog mUntrustDialog = null;
    private WifiInjector mWifiInjector = null;
    private int mWifiP2pRejectCount = 0;
    private long mWifiP2pRejectFirstTime = 0;

    private HwWifiP2pServiceTvEx(Context context, StateMachine p2pStateMachine, WifiInjector wifiInjector, IHwWifiP2pServiceInner hwWifiP2pServiceInner) {
        this.mContext = context;
        this.mP2pStateMachine = (HwWifiP2pService.HwP2pStateMachine) p2pStateMachine;
        this.mWifiInjector = wifiInjector;
        this.mHwWifiP2pServiceInner = hwWifiP2pServiceInner;
    }

    public static HwWifiP2pServiceTvEx createHwWifiP2pServiceTvEx(Context context, StateMachine p2pStateMachine, WifiInjector wifiInjector, IHwWifiP2pServiceInner hwWifiP2pServiceInner) {
        if (p2pStateMachine != null && wifiInjector != null && hwWifiP2pServiceInner != null && context != null && (p2pStateMachine instanceof HwWifiP2pService.HwP2pStateMachine)) {
            return new HwWifiP2pServiceTvEx(context, p2pStateMachine, wifiInjector, hwWifiP2pServiceInner);
        }
        HwHiLog.e(TAG, false, "create HwWifiP2pServiceTvEx fail", new Object[0]);
        return null;
    }

    public int[] getP2pExtListenTime(boolean isConnected) {
        if (!isConnected) {
            isConnected = isWifiConnected();
            HwHiLog.i(TAG, false, "getP2pExtListenTime isConnected is false, recheck result is " + isConnected, new Object[0]);
        }
        int[] p2pExtListenTime = {0, 0};
        p2pExtListenTime[0] = isConnected ? 10 : P2P_LISTEN_PERIOD;
        p2pExtListenTime[1] = isConnected ? 100 : 500;
        return p2pExtListenTime;
    }

    public int getP2pGroupCreatingWaitTime() {
        return 30000;
    }

    public void setP2pInviteDialog(AlertDialog alertDialog) {
        this.mInviteDialog = alertDialog;
    }

    public void dismissP2pInviteDialog() {
        if (this.mInviteDialog != null) {
            HwHiLog.i(TAG, false, "dismiss InviteDialog for tv!", new Object[0]);
            this.mInviteDialog.dismiss();
            this.mInviteDialog = null;
        }
    }

    public void dismissP2pDisallowUntrustInviteDialog() {
        if (this.mUntrustDialog != null) {
            HwHiLog.i(TAG, false, "dismiss UntrustDialog!", new Object[0]);
            this.mUntrustDialog.dismiss();
            this.mUntrustDialog = null;
            this.mWifiP2pRejectCount = 0;
        }
    }

    public void handleP2pUserAuthorizingJoinStateEnter() {
        if (!this.mHwWifiP2pServiceInner.hasMessages((int) INVITATION_DIALOG_TIMEOUT)) {
            this.mP2pStateMachine.sendMessageDelayed(INVITATION_DIALOG_TIMEOUT, 30000);
        }
    }

    public boolean handleP2pUserAuthorizingJoinStateMessage(Message message) {
        if (message.what != INVITATION_DIALOG_TIMEOUT) {
            return false;
        }
        HwHiLog.i(TAG, false, "INVITATION_DIALOG_TIMEOUT then change to nextstate", new Object[0]);
        this.mP2pStateMachine.transitionTo(this.mHwWifiP2pServiceInner.getAfterUserAuthorizingJoinState());
        return true;
    }

    public boolean handleP2pEnabledStateMessage(Message message) {
        switch (message.what) {
            case DISALLOW_UNTRUST_INVITE /*{ENCODED_INT: 143401}*/:
                this.mP2pStateMachine.sendMessageDelayed(DISALLOW_UNTRUST_INVITE_TIMEOUT, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
                return true;
            case ALLOW_UNTRUST_INVITE /*{ENCODED_INT: 143402}*/:
            case DISALLOW_UNTRUST_INVITE_TIMEOUT /*{ENCODED_INT: 143403}*/:
                this.mWifiP2pRejectCount = 0;
                return true;
            case DISALLOW_UNTRUST_INVITE_DIALOG_TIMEOUT /*{ENCODED_INT: 143404}*/:
                if (message.arg1 != this.mP2pPauseUntrustInviteIndex) {
                    return true;
                }
                dismissP2pDisallowUntrustInviteDialog();
                return true;
            default:
                return false;
        }
    }

    public boolean handleGroupCreatedStateMessage(Message message) {
        if (message.what != 139329) {
            return false;
        }
        HwHiLog.i(TAG, false, "forbid start listen on GroupCreatedState for tv", new Object[0]);
        this.mP2pStateMachine.replyToMessage(message, 139330);
        return true;
    }

    public boolean handleGroupCreatingStateMessage(Message message) {
        int i = message.what;
        if (i == 139329) {
            HwHiLog.i(TAG, false, "forbid start listen on GroupCreatingState for tv", new Object[0]);
            this.mP2pStateMachine.replyToMessage(message, 139330);
            return true;
        } else if (i != 139332) {
            return false;
        } else {
            HwHiLog.i(TAG, false, "forbid stop listen on GroupCreatingState for tv", new Object[0]);
            this.mP2pStateMachine.replyToMessage(message, 139333);
            return true;
        }
    }

    public void notifyP2pInvitationReceived(int groupCreatingTimeoutIndex, final WifiP2pConfig savedPeerConfig, String deviceName) {
        if (savedPeerConfig == null || deviceName == null) {
            HwHiLog.i(TAG, false, "notifyP2pInvitationReceived parameter is null", new Object[0]);
            return;
        }
        if (savedPeerConfig.wps.setup != 0) {
            HwHiLog.i(TAG, false, "The device type is not WpsInfo.PBC.", new Object[0]);
        }
        if (this.mWifiP2pRejectCount >= 3) {
            HwHiLog.i(TAG, false, "mWifiP2pRejectCount=" + this.mWifiP2pRejectCount + " don't show dialog", new Object[0]);
            HwWifiP2pService.HwP2pStateMachine hwP2pStateMachine = this.mP2pStateMachine;
            hwP2pStateMachine.sendMessage(hwP2pStateMachine.obtainMessage(143361, groupCreatingTimeoutIndex, 0));
            return;
        }
        wakeScreenFromProtector();
        View view = LayoutInflater.from(this.mContext).inflate(34013459, (ViewGroup) null, false);
        final AlertDialog dialog = new AlertDialog.Builder(this.mContext).setView(view).create();
        ((Button) view.findViewById(34603451)).setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wifi.p2p.HwWifiP2pServiceTvEx.AnonymousClass1 */

            public void onClick(View mainView) {
                HwHiLog.i(HwWifiP2pServiceTvEx.TAG, false, " accept invitation " + savedPeerConfig, new Object[0]);
                HwWifiP2pServiceTvEx.this.mP2pStateMachine.sendMessage(143362);
                dialog.dismiss();
                AlertDialog unused = HwWifiP2pServiceTvEx.this.mInviteDialog = null;
            }
        });
        ((Button) view.findViewById(34603452)).setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wifi.p2p.HwWifiP2pServiceTvEx.AnonymousClass2 */

            public void onClick(View mainView) {
                HwHiLog.i(HwWifiP2pServiceTvEx.TAG, false, " ignore connect", new Object[0]);
                HwWifiP2pServiceTvEx.this.mP2pStateMachine.sendMessage(143363);
                HwWifiP2pServiceTvEx.this.dealP2pPeerConnectionUserReject();
                dialog.dismiss();
                AlertDialog unused = HwWifiP2pServiceTvEx.this.mInviteDialog = null;
            }
        });
        Resources resources = Resources.getSystem();
        ((TextView) view.findViewById(34603453)).setText(String.format(Locale.ROOT, resources.getString(33685699), deviceName));
        setDialogSkipUpDownKeyEvent(dialog);
        setDialogWindowStyle(dialog);
        dialog.show();
        this.mInviteDialog = dialog;
    }

    /* access modifiers changed from: private */
    public void dealP2pPeerConnectionUserReject() {
        this.mWifiP2pRejectCount++;
        HwHiLog.i(TAG, false, "P2pRejectCount=" + this.mWifiP2pRejectCount, new Object[0]);
        if (this.mWifiP2pRejectCount == 1) {
            this.mWifiP2pRejectFirstTime = SystemClock.elapsedRealtime();
            HwHiLog.i(TAG, false, "WifiP2pRejectFirstTime=" + this.mWifiP2pRejectFirstTime, new Object[0]);
        } else {
            HwHiLog.i(TAG, false, "CurrentTime-WifiP2pRejectFirstTime=" + (SystemClock.elapsedRealtime() - this.mWifiP2pRejectFirstTime), new Object[0]);
        }
        if (this.mWifiP2pRejectCount < 3) {
            return;
        }
        if (SystemClock.elapsedRealtime() - this.mWifiP2pRejectFirstTime < 180000) {
            pauseUntrustRequest();
            return;
        }
        this.mWifiP2pRejectFirstTime = SystemClock.elapsedRealtime();
        this.mWifiP2pRejectCount = 1;
        HwHiLog.i(TAG, false, "Set mWifiP2pRejectCount as first time and Reset mWifiP2pRejectFirstTime = " + this.mWifiP2pRejectFirstTime, new Object[0]);
    }

    private void pauseUntrustRequest() {
        HwHiLog.i(TAG, false, "Enter PauseUntrustRequest", new Object[0]);
        HwWifiP2pService.HwP2pStateMachine hwP2pStateMachine = this.mP2pStateMachine;
        int i = this.mP2pPauseUntrustInviteIndex + 1;
        this.mP2pPauseUntrustInviteIndex = i;
        hwP2pStateMachine.sendMessageDelayed(hwP2pStateMachine.obtainMessage(DISALLOW_UNTRUST_INVITE_DIALOG_TIMEOUT, i, 0), 30000);
        View view = LayoutInflater.from(this.mContext).inflate(34013460, (ViewGroup) null, false);
        final AlertDialog dialog = new AlertDialog.Builder(this.mContext).setView(view).create();
        ((Button) view.findViewById(34603458)).setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wifi.p2p.HwWifiP2pServiceTvEx.AnonymousClass3 */

            public void onClick(View mainView) {
                HwHiLog.i(HwWifiP2pServiceTvEx.TAG, false, " Reject invitation in 10 minutes", new Object[0]);
                HwWifiP2pServiceTvEx.this.mP2pStateMachine.sendMessage(HwWifiP2pServiceTvEx.DISALLOW_UNTRUST_INVITE);
                dialog.dismiss();
                AlertDialog unused = HwWifiP2pServiceTvEx.this.mUntrustDialog = null;
            }
        });
        ((Button) view.findViewById(34603456)).setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wifi.p2p.HwWifiP2pServiceTvEx.AnonymousClass4 */

            public void onClick(View mainView) {
                HwHiLog.i(HwWifiP2pServiceTvEx.TAG, false, " allow to accept invitaion", new Object[0]);
                HwWifiP2pServiceTvEx.this.mP2pStateMachine.sendMessage(HwWifiP2pServiceTvEx.ALLOW_UNTRUST_INVITE);
                dialog.dismiss();
                AlertDialog unused = HwWifiP2pServiceTvEx.this.mUntrustDialog = null;
            }
        });
        Resources resources = Resources.getSystem();
        ((TextView) view.findViewById(34603455)).setText(String.format(Locale.ROOT, resources.getString(33686264), 10));
        setDialogSkipUpDownKeyEvent(dialog);
        setDialogWindowStyle(dialog);
        dialog.show();
        this.mUntrustDialog = dialog;
    }

    private void setDialogWindowStyle(AlertDialog dialog) {
        dialog.getWindow().setType(2003);
        WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
        attrs.privateFlags = 16;
        dialog.getWindow().setAttributes(attrs);
        dialog.getWindow().setBackgroundDrawableResource(17170445);
        dialog.getWindow().setLayout(switchDipToPx(this.mContext, 360.0f), -2);
        dialog.setCanceledOnTouchOutside(false);
    }

    private void setDialogSkipUpDownKeyEvent(AlertDialog dialog) {
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            /* class com.android.server.wifi.p2p.HwWifiP2pServiceTvEx.AnonymousClass5 */

            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode != 19 && keyCode != 20) {
                    return false;
                }
                HwHiLog.i(HwWifiP2pServiceTvEx.TAG, false, "discard keyCode= " + keyCode, new Object[0]);
                return true;
            }
        });
    }

    private int switchDipToPx(Context context, float dipValue) {
        if (context == null) {
            return 0;
        }
        return (int) ((dipValue * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private void wakeScreenFromProtector() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(268435482, getClass().getCanonicalName());
        wakeLock.acquire(WAKEUP_SCREEN_TIME);
        powerManager.userActivity(SystemClock.uptimeMillis(), false);
        wakeLock.release();
    }

    private boolean isWifiConnected() {
        WifiInfo wifiInfo;
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager == null || wifiManager.getWifiState() != 3 || (wifiInfo = wifiManager.getConnectionInfo()) == null) {
            return false;
        }
        int ipAddress = wifiInfo.getIpAddress();
        boolean suppCompleted = wifiInfo.getSupplicantState() == SupplicantState.COMPLETED;
        HwHiLog.i(TAG, false, "isWifiConnected, ipAddress: " + ipAddress + ", suppCompleted = " + suppCompleted, new Object[0]);
        if (ipAddress != 0 || suppCompleted) {
            return true;
        }
        return false;
    }
}
