package com.android.server.wifi.ABS;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneCallback;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.ims.ImsManager;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HwABSStateMachine extends StateMachine {
    private static final long ABS_INTERVAL_TIME = 1800000;
    private static final long ABS_PUNISH_TIME = 60000;
    private static final long ABS_SCREEN_ON_TIME = 10000;
    private static final String ACTION_ABS_HANDOVER_TIMER = "android.net.wifi.abs_handover_timer";
    private static final int MAX_HANDOVER_TIME = 15;
    private static final long ONEDAYA_TIME = 86400000;
    private static final int SIM_CARD_STATE_MIMO = 2;
    private static final int SIM_CARD_STATE_SISO = 1;
    private static HwABSStateMachine mHwABSStateMachine = null;
    /* access modifiers changed from: private */
    public int ABS_HANDOVER_TIMES = 0;
    /* access modifiers changed from: private */
    public long ABS_LAST_HANDOVER_TIME = 0;
    /* access modifiers changed from: private */
    public boolean ANTENNA_STATE_IN_CALL = false;
    /* access modifiers changed from: private */
    public boolean ANTENNA_STATE_IN_CONNECT = false;
    /* access modifiers changed from: private */
    public boolean ANTENNA_STATE_IN_PREEMPTED = false;
    /* access modifiers changed from: private */
    public boolean ANTENNA_STATE_IN_SEARCH = false;
    /* access modifiers changed from: private */
    public int MODEM_TUNERIC_ACTIVE = 1;
    /* access modifiers changed from: private */
    public int MODEM_TUNERIC_IACTIVE = 0;
    /* access modifiers changed from: private */
    public int RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
    /* access modifiers changed from: private */
    public int RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
    private int RESTART_ABS_TIME = 300000;
    /* access modifiers changed from: private */
    public boolean isPuaseHandover = false;
    private boolean isSwitching = false;
    /* access modifiers changed from: private */
    public long mABSMIMOScreenOnStartTime = 0;
    /* access modifiers changed from: private */
    public long mABSMIMOStartTime = 0;
    /* access modifiers changed from: private */
    public long mABSSISOScreenOnStartTime = 0;
    /* access modifiers changed from: private */
    public long mABSSISOStartTime = 0;
    private Map<String, APHandoverInfo> mAPHandoverInfoList = new HashMap();
    /* access modifiers changed from: private */
    public PhoneCallback mActiveCallback = new PhoneCallback() {
        /* class com.android.server.wifi.ABS.HwABSStateMachine.AnonymousClass1 */

        public void onPhoneCallback1(int parm) {
            HwABSStateMachine.this.sendMessage(33, parm);
        }
    };
    private int mAddBlackListReason = 0;
    private String mAssociateBSSID = null;
    private String mAssociateSSID = null;
    /* access modifiers changed from: private */
    public Context mContext;
    private State mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public HwABSCHRManager mHwABSCHRManager;
    /* access modifiers changed from: private */
    public HwABSDataBaseManager mHwABSDataBaseManager;
    /* access modifiers changed from: private */
    public HwABSWiFiHandler mHwABSWiFiHandler;
    /* access modifiers changed from: private */
    public HwABSWiFiScenario mHwABSWiFiScenario;
    /* access modifiers changed from: private */
    public PhoneCallback mIactiveCallback = new PhoneCallback() {
        /* class com.android.server.wifi.ABS.HwABSStateMachine.AnonymousClass2 */

        public void onPhoneCallback1(int parm) {
            HwABSStateMachine.this.sendMessage(35, parm);
        }
    };
    /* access modifiers changed from: private */
    public boolean mIsInCallPunish = false;
    /* access modifiers changed from: private */
    public boolean mIsSupportVoWIFI = false;
    /* access modifiers changed from: private */
    public State mMimoState = new MimoState();
    /* access modifiers changed from: private */
    public List<Integer> mModemStateList = new ArrayList();
    /* access modifiers changed from: private */
    public State mSisoState = new SisoState();
    /* access modifiers changed from: private */
    public int mSwitchEvent = 0;
    /* access modifiers changed from: private */
    public int mSwitchType = 0;
    private TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public State mWiFiConnectedState = new WiFiConnectedState();
    /* access modifiers changed from: private */
    public State mWiFiDisableState = new WiFiDisableState();
    /* access modifiers changed from: private */
    public State mWiFiDisconnectedState = new WiFiDisconnectedState();
    /* access modifiers changed from: private */
    public State mWiFiEnableState = new WiFiEnableState();
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;

    static /* synthetic */ int access$1308(HwABSStateMachine x0) {
        int i = x0.RESENT_MODEM_TUNERIC_ACTIVE_TIMES;
        x0.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = i + 1;
        return i;
    }

    static /* synthetic */ int access$1808(HwABSStateMachine x0) {
        int i = x0.RESENT_MODEM_TUNERIC_IACTIVE_TIMES;
        x0.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = i + 1;
        return i;
    }

    class DefaultState extends State {
        Bundle mData = null;
        int mSubId = -1;

        DefaultState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                HwABSUtils.logD(false, "DefaultState MSG_WIFI_CONNECTED", new Object[0]);
                HwABSStateMachine hwABSStateMachine = HwABSStateMachine.this;
                hwABSStateMachine.transitionTo(hwABSStateMachine.mWiFiConnectedState);
                HwABSStateMachine.this.sendMessage(1);
            } else if (i == 2) {
                HwABSUtils.logD(false, "DefaultState MSG_WIFI_DISCONNECTED", new Object[0]);
                HwABSStateMachine.this.removeMessages(1);
                HwABSStateMachine hwABSStateMachine2 = HwABSStateMachine.this;
                hwABSStateMachine2.transitionTo(hwABSStateMachine2.mWiFiDisconnectedState);
            } else if (i == 3) {
                HwABSUtils.logD(false, "DefaultState MSG_WIFI_ENABLED", new Object[0]);
                HwABSStateMachine hwABSStateMachine3 = HwABSStateMachine.this;
                hwABSStateMachine3.transitionTo(hwABSStateMachine3.mWiFiEnableState);
            } else if (i == 4) {
                HwABSUtils.logD(false, "DefaultState MSG_WIFI_DISABLE", new Object[0]);
                HwABSStateMachine hwABSStateMachine4 = HwABSStateMachine.this;
                hwABSStateMachine4.transitionTo(hwABSStateMachine4.mWiFiDisableState);
            } else if (i == 7) {
                HwABSUtils.logD(false, "DefaultState MSG_OUTGOING_CALL", new Object[0]);
                boolean unused = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                HwABSStateMachine.this.resetCapablity(1);
            } else if (i == 8) {
                HwABSUtils.logD(false, "DefaultState MSG_CALL_STATE_IDLE", new Object[0]);
                boolean unused2 = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = false;
                HwABSStateMachine.this.resetCapablity(2);
            } else if (i == 9) {
                HwABSUtils.logD(false, "DefaultState MSG_CALL_STATE_RINGING", new Object[0]);
                boolean unused3 = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                HwABSStateMachine.this.resetCapablity(1);
            } else if (i == 22) {
                HwABSStateMachine.this.handlePowerOffMessage();
                if (HwABSStateMachine.this.isModemStateInIdle()) {
                    HwABSStateMachine.this.resetCapablity(2);
                }
            } else if (i == 25) {
                HwABSStateMachine hwABSStateMachine5 = HwABSStateMachine.this;
                boolean unused4 = hwABSStateMachine5.mIsSupportVoWIFI = ImsManager.isWfcEnabledByPlatform(hwABSStateMachine5.mContext);
                HwABSUtils.logD(false, "DefaultState mIsSupportVoWIFI = %{public}s", String.valueOf(HwABSStateMachine.this.mIsSupportVoWIFI));
            } else if (i != 103) {
                switch (i) {
                    case 11:
                    case 12:
                        HwABSUtils.logD(false, "DefaultState MSG_MODEM_ENTER_CONNECT_STATE", new Object[0]);
                        boolean unused5 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                        HwABSStateMachine.this.resetCapablity(1);
                        break;
                    case 13:
                        HwABSUtils.logD(false, "DefaultState MSG_MODEM_EXIT_CONNECT_STATE", new Object[0]);
                        if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                            boolean unused6 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = false;
                            HwABSStateMachine.this.resetCapablity(2);
                            break;
                        }
                        break;
                    case 14:
                        HwABSUtils.logD(false, "DefaultState MSG_MODEM_ENTER_SEARCHING_STATE", new Object[0]);
                        boolean unused7 = HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = true;
                        HwABSStateMachine.this.resetCapablity(1);
                        this.mData = message.getData();
                        this.mSubId = this.mData.getInt(HwABSUtils.SUB_ID);
                        HwABSStateMachine.this.addModemState(this.mSubId);
                        break;
                    case 15:
                        HwABSUtils.logD(false, "DefaultState MSG_MODEM_EXIT_SEARCHING_STATE", new Object[0]);
                        this.mData = message.getData();
                        this.mSubId = this.mData.getInt(HwABSUtils.SUB_ID);
                        if (HwABSStateMachine.this.removeModemState(this.mSubId) == 0) {
                            boolean unused8 = HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = false;
                            HwABSStateMachine.this.resetCapablity(2);
                            break;
                        }
                        break;
                    case 16:
                        HwABSUtils.logD(false, "DefaultState MSG_WIFI_ANTENNA_PREEMPTED", new Object[0]);
                        break;
                    default:
                        switch (i) {
                            case 33:
                                int active_result = message.arg1;
                                HwABSUtils.logD(false, "DefaultState MSG_MODEM_TUNERIC_ACTIVE_RESULT active_result = %{public}d  RESENT_MODEM_TUNERIC_ACTIVE_TIMES = %{public}d", Integer.valueOf(active_result), Integer.valueOf(HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES));
                                if (active_result != 1) {
                                    if (HwABSStateMachine.this.mWifiManager.isWifiEnabled() && HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES < 3) {
                                        HwABSStateMachine.this.removeMessages(34);
                                        HwABSStateMachine.this.sendMessageDelayed(34, 5000);
                                        break;
                                    }
                                } else {
                                    int unused9 = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
                                    break;
                                }
                            case 34:
                                HwABSUtils.logD(false, "DefaultState MSG_RESEND_TUNERIC_ACTIVE_MSG", new Object[0]);
                                if (HwABSStateMachine.this.mWifiManager.isWifiEnabled()) {
                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_ACTIVE, HwABSStateMachine.this.mActiveCallback);
                                    HwABSStateMachine.access$1308(HwABSStateMachine.this);
                                    break;
                                }
                                break;
                            case 35:
                                int iactive_result = message.arg1;
                                HwABSUtils.logD(false, "DefaultState MSG_MODEM_TUNERIC_IACTIVE_RESULT iactive_result = %{public}d  RESENT_MODEM_TUNERIC_IACTIVE_TIMES = %{public}d", Integer.valueOf(iactive_result), Integer.valueOf(HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES));
                                if (iactive_result != 1) {
                                    if (!HwABSStateMachine.this.mWifiManager.isWifiEnabled() && HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES < 3) {
                                        HwABSStateMachine.this.removeMessages(34);
                                        HwABSStateMachine.this.sendMessageDelayed(36, 5000);
                                        break;
                                    }
                                } else {
                                    int unused10 = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
                                    break;
                                }
                            case 36:
                                HwABSUtils.logD(false, "DefaultState MSG_RESEND_TUNERIC_IACTIVE_MSG", new Object[0]);
                                if (!HwABSStateMachine.this.mWifiManager.isWifiEnabled()) {
                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_IACTIVE, HwABSStateMachine.this.mIactiveCallback);
                                    HwABSStateMachine.access$1808(HwABSStateMachine.this);
                                    break;
                                }
                                break;
                            case HwABSUtils.MSG_BOOT_COMPLETED:
                                if (!HwABSStateMachine.this.mWifiManager.isWifiEnabled()) {
                                    HwABSUtils.logD(false, "DefaultState send MODEM_TUNERIC_IACTIVE_MSG", new Object[0]);
                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_IACTIVE, HwABSStateMachine.this.mIactiveCallback);
                                    int unused11 = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
                                    break;
                                } else {
                                    HwABSUtils.logD(false, "DefaultState send MODEM_TUNERIC_ACTIVE_MSG", new Object[0]);
                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_ACTIVE, HwABSStateMachine.this.mActiveCallback);
                                    int unused12 = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
                                    HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability());
                                    HwABSStateMachine.this.setBlackListBssid();
                                    break;
                                }
                            case HwABSUtils.MSG_SEL_ENGINE_RESET_COMPLETED:
                                HwABSUtils.logD(false, "DefaultState MSG_SEL_ENGINE_RESET_COMPLETED", new Object[0]);
                                HwABSStateMachine hwABSStateMachine6 = HwABSStateMachine.this;
                                hwABSStateMachine6.transitionTo(hwABSStateMachine6.mWiFiConnectedState);
                                HwABSStateMachine.this.sendMessage(1);
                                break;
                        }
                        break;
                }
            } else {
                HwABSUtils.logD(false, "DefaultState CMD_WIFI_PAUSE_HANDOVER", new Object[0]);
                boolean unused13 = HwABSStateMachine.this.isPuaseHandover = false;
            }
            return true;
        }
    }

    class WiFiEnableState extends State {
        WiFiEnableState() {
        }

        public void enter() {
            HwABSUtils.logD(false, "enter WiFiEnableState", new Object[0]);
            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability());
            HwABSUtils.logD(false, "WiFiEnableState send MODEM_TUNERIC_ACTIVE_MSG", new Object[0]);
            HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_ACTIVE, HwABSStateMachine.this.mActiveCallback);
            int unused = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
            HwABSStateMachine.this.setBlackListBssid();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 3) {
                HwABSUtils.logD(false, "WiFiEnableState MSG_WIFI_ENABLED", new Object[0]);
                return true;
            } else if (i != 4) {
                return false;
            } else {
                HwABSUtils.logD(false, "WiFiDisconnectedState MSG_WIFI_DISABLE", new Object[0]);
                return false;
            }
        }

        public void exit() {
            HwABSUtils.logD(false, "exit WiFiEnableState", new Object[0]);
        }
    }

    static class WiFiDisconnectedState extends State {
        WiFiDisconnectedState() {
        }

        public void enter() {
            HwABSUtils.logD(false, "enter WiFiDisconnectedState", new Object[0]);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 2) {
                HwABSUtils.logD(false, "WiFiDisconnectedState MSG_WIFI_DISCONNECTED", new Object[0]);
                return true;
            } else if (i != 4) {
                return false;
            } else {
                HwABSUtils.logD(false, "WiFiDisconnectedState MSG_WIFI_DISABLE", new Object[0]);
                return false;
            }
        }

        public void exit() {
            HwABSUtils.logD(false, "exit WiFiDisconnectedState", new Object[0]);
        }
    }

    class WiFiDisableState extends State {
        WiFiDisableState() {
        }

        public void enter() {
            HwABSUtils.logD(false, "enter WiFiDisableState ABS_HANDOVER_TIMES = %{public}d", Integer.valueOf(HwABSStateMachine.this.ABS_HANDOVER_TIMES));
            if (HwABSStateMachine.this.isScreenOn()) {
                int unused = HwABSStateMachine.this.ABS_HANDOVER_TIMES = 0;
            }
            HwABSUtils.logD(false, "WiFiDisableState send MODEM_TUNERIC_IACTIVE_MSG", new Object[0]);
            HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_IACTIVE, HwABSStateMachine.this.mIactiveCallback);
            int unused2 = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1 || i == 2 || i == 4) {
                HwABSUtils.logD(false, "WiFiDisableState handle message.what = %{public}d", Integer.valueOf(message.what));
                return true;
            }
            HwABSUtils.logD(false, "WiFiDisableState message.what = %{public}d", Integer.valueOf(message.what));
            return false;
        }

        public void exit() {
            HwABSUtils.logD(false, "exit WiFiDisableState", new Object[0]);
        }
    }

    class WiFiConnectedState extends State {
        private int mGetApMIMOCapabilityTimes = 0;

        WiFiConnectedState() {
        }

        public void enter() {
            HwABSUtils.logD(false, "enter WiFiConnectedState", new Object[0]);
            this.mGetApMIMOCapabilityTimes = 0;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:39:0x011a, code lost:
            if (r5 == null) goto L_0x019a;
         */
        public boolean processMessage(Message message) {
            int i;
            int i2 = message.what;
            if (i2 != 1) {
                switch (i2) {
                    case 17:
                        HwABSStateMachine.this.mHwABSWiFiHandler.hwABScheckLinked();
                        break;
                    case 18:
                        HwABSUtils.logE(false, "WiFiConnectedState MSG_WIFI_CHECK_LINK_SUCCESS", new Object[0]);
                        if (!HwABSStateMachine.this.isUsingMIMOCapability()) {
                            HwABSStateMachine hwABSStateMachine = HwABSStateMachine.this;
                            hwABSStateMachine.transitionTo(hwABSStateMachine.mSisoState);
                            break;
                        } else {
                            HwABSStateMachine hwABSStateMachine2 = HwABSStateMachine.this;
                            hwABSStateMachine2.transitionTo(hwABSStateMachine2.mMimoState);
                            break;
                        }
                    case 19:
                        HwABSUtils.logE(false, "WiFiConnectedState MSG_WIFI_CHECK_LINK_FAILED", new Object[0]);
                        WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
                        if (wifiInfo != null && wifiInfo.getBSSID() != null) {
                            HwABSApInfoData hwABSApInfoData = HwABSStateMachine.this.mHwABSDataBaseManager.getApInfoByBssid(wifiInfo.getBSSID());
                            if (hwABSApInfoData != null) {
                                hwABSApInfoData.mSwitch_siso_type = 2;
                                HwABSStateMachine.this.mHwABSDataBaseManager.addOrUpdateApInfos(hwABSApInfoData);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                                break;
                            }
                        } else {
                            HwABSUtils.logE(false, "MSG_WIFI_CHECK_LINK_FAILED error ", new Object[0]);
                            break;
                        }
                        break;
                    default:
                        return false;
                }
            } else {
                HwABSUtils.logE(false, "WiFiConnectedState MSG_WIFI_CONNECTED", new Object[0]);
                WifiInfo mWifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
                if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
                    HwABSUtils.logE(false, "WiFiConnectedState error ", new Object[0]);
                } else {
                    int mimoCapability = HwABSStateMachine.this.isAPSupportMIMOCapability(mWifiInfo.getBSSID());
                    if (mimoCapability == -1) {
                        HwABSUtils.logD(false, "isAPSupportMIMOCapability mNetworkDetail == null", new Object[0]);
                        if (HwABSStateMachine.this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID()) == null) {
                            HwABSUtils.logE(false, " It is a hidden AP,delay get scan result mGetApMIMOCapabilityTimes = %{public}d", Integer.valueOf(this.mGetApMIMOCapabilityTimes));
                            if (!HwABSStateMachine.this.hasMessages(1) && (i = this.mGetApMIMOCapabilityTimes) < 3) {
                                this.mGetApMIMOCapabilityTimes = i + 1;
                                HwABSStateMachine.this.sendMessageDelayed(1, 2000);
                            }
                        }
                    } else if (mimoCapability == 0) {
                        HwABSUtils.logE(false, " It is a siso AP", new Object[0]);
                    }
                    if (!ScanResult.is24GHz(mWifiInfo.getFrequency()) || HwABSStateMachine.this.isMobileAP()) {
                        HwABSUtils.logE(false, " It is a 5G AP or moblie AP", new Object[0]);
                        HwABSStateMachine.this.resetCapablity(2);
                    } else {
                        HwABSApInfoData data = HwABSStateMachine.this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID());
                        if (data == null) {
                            data = initApInfoData(mWifiInfo);
                        } else {
                            data.mLast_connect_time = System.currentTimeMillis();
                        }
                        HwABSStateMachine.this.mHwABSDataBaseManager.addOrUpdateApInfos(data);
                        HwABSUtils.logD(false, "now capability = %{public}d", Integer.valueOf(HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability()));
                        if (data.mIn_black_List == 1 && HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability() == 2) {
                            HwABSUtils.logD(false, "current AP is in blackList reset capability", new Object[0]);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.setBlackListBssid();
                        }
                        if (HwABSStateMachine.this.isUsingMIMOCapability()) {
                            HwABSStateMachine hwABSStateMachine3 = HwABSStateMachine.this;
                            hwABSStateMachine3.transitionTo(hwABSStateMachine3.mMimoState);
                        } else {
                            HwABSStateMachine hwABSStateMachine4 = HwABSStateMachine.this;
                            hwABSStateMachine4.transitionTo(hwABSStateMachine4.mSisoState);
                        }
                    }
                }
            }
            return true;
        }

        public void exit() {
            HwABSUtils.logD(false, "exit WiFiConnectedState", new Object[0]);
        }

        private HwABSApInfoData initApInfoData(WifiInfo wifiInfo) {
            int authType = 0;
            WifiConfiguration sWifiConfiguration = getCurrntConfig(wifiInfo);
            if (sWifiConfiguration != null && sWifiConfiguration.allowedKeyManagement.cardinality() <= 1) {
                authType = sWifiConfiguration.getAuthType();
            }
            return new HwABSApInfoData(wifiInfo.getBSSID(), HwABSCHRManager.getAPSSID(wifiInfo), 2, 2, authType, 0, 0, 0, 0, System.currentTimeMillis());
        }

        private WifiConfiguration getCurrntConfig(WifiInfo wifiInfo) {
            List<WifiConfiguration> configNetworks = HwABSStateMachine.this.mWifiManager.getConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                return null;
            }
            for (WifiConfiguration nextConfig : configNetworks) {
                if (isValidConfig(nextConfig) && nextConfig.networkId == wifiInfo.getNetworkId()) {
                    return nextConfig;
                }
            }
            return null;
        }

        private boolean isValidConfig(WifiConfiguration config) {
            if (config == null || config.SSID == null || config.allowedKeyManagement.cardinality() > 1) {
                return false;
            }
            return true;
        }
    }

    class MimoState extends State {
        private String mCurrentBSSID = null;
        private String mCurrentSSID = null;
        Bundle mData = null;
        int mSubId = -1;

        MimoState() {
        }

        public void enter() {
            HwABSUtils.logD(false, "enter MimoState", new Object[0]);
            HwABSStateMachine.this.setWiFiAntennaMonitor(true);
            long unused = HwABSStateMachine.this.mABSMIMOStartTime = System.currentTimeMillis();
            if (HwABSStateMachine.this.isScreenOn()) {
                long unused2 = HwABSStateMachine.this.mABSMIMOScreenOnStartTime = System.currentTimeMillis();
            }
            WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null && wifiInfo.getBSSID() != null) {
                this.mCurrentSSID = HwABSCHRManager.getAPSSID(wifiInfo);
                this.mCurrentBSSID = wifiInfo.getBSSID();
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                HwABSUtils.logE(false, "MimoState MSG_WIFI_CONNECTED mIsSupportVoWIFI = %{public}s", String.valueOf(HwABSStateMachine.this.mIsSupportVoWIFI));
                if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                    if (!HwABSStateMachine.this.mIsSupportVoWIFI || !HwABSStateMachine.this.mHwABSWiFiHandler.isHandoverTimeout()) {
                        HwABSStateMachine.this.updateABSAssociateSuccess();
                    } else {
                        HwABSUtils.logE(false, "MimoState MSG_WIFI_CONNECTED handover timeout", new Object[0]);
                        HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                    }
                    HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                }
            } else if (i == 2) {
                HwABSUtils.logD(false, "MimoState MSG_WIFI_DISCONNECTED", new Object[0]);
                if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                    HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                    HwABSStateMachine.this.mHwABSCHRManager.uploadABSReassociateExeption();
                    HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                }
                HwABSStateMachine hwABSStateMachine = HwABSStateMachine.this;
                hwABSStateMachine.transitionTo(hwABSStateMachine.mWiFiDisconnectedState);
            } else if (i == 5) {
                HwABSUtils.logE(false, "MimoState MSG_SCREEN_ON", new Object[0]);
                long unused = HwABSStateMachine.this.mABSMIMOScreenOnStartTime = System.currentTimeMillis();
            } else if (i == 6) {
                HwABSUtils.logE(false, "MimoState MSG_SCREEN_OFF", new Object[0]);
                if (HwABSStateMachine.this.mABSMIMOScreenOnStartTime != 0) {
                    HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, 0, 0, System.currentTimeMillis() - HwABSStateMachine.this.mABSMIMOScreenOnStartTime, 0);
                    long unused2 = HwABSStateMachine.this.mABSMIMOScreenOnStartTime = 0;
                }
            } else if (i == 7) {
                HwABSUtils.logE(false, "MimoState MSG_OUTGOING_CALL isAirModeOn =  %{public}s", String.valueOf(HwABSStateMachine.this.isAirModeOn()));
                if (!HwABSStateMachine.this.isAirModeOn()) {
                    boolean unused3 = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                    if (!HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() || HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        int unused4 = HwABSStateMachine.this.mSwitchType = 7;
                        int unused5 = HwABSStateMachine.this.mSwitchEvent = 6;
                        HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    } else {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(7);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(6);
                            HwABSStateMachine.this.mHwABSWiFiHandler.hwABSHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine hwABSStateMachine2 = HwABSStateMachine.this;
                        hwABSStateMachine2.transitionTo(hwABSStateMachine2.mSisoState);
                    }
                }
            } else if (i == 9) {
                HwABSUtils.logE(false, "MimoState MSG_CALL_STATE_RINGING isAirModeOn =  %{public}s", String.valueOf(HwABSStateMachine.this.isAirModeOn()));
                if (!HwABSStateMachine.this.isAirModeOn()) {
                    boolean unused6 = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                    if (!HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() || HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        int unused7 = HwABSStateMachine.this.mSwitchType = 6;
                        int unused8 = HwABSStateMachine.this.mSwitchEvent = 6;
                        HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    } else {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(6);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(6);
                            HwABSStateMachine.this.mHwABSWiFiHandler.hwABSHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine hwABSStateMachine3 = HwABSStateMachine.this;
                        hwABSStateMachine3.transitionTo(hwABSStateMachine3.mSisoState);
                    }
                }
            } else if (i == 23) {
                HwABSUtils.logD(false, "MIMO MSG_DELAY_SWITCH ANTENNA_STATE_IN_CALL = %{public}s ANTENNA_STATE_IN_SEARCH = %{public}s ANTENNA_STATE_IN_CONNECT = %{public}s ANTENNA_STATE_IN_PREEMPTED = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_CALL), String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH), String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT), String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED));
                if (HwABSStateMachine.this.ANTENNA_STATE_IN_CALL || HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH || HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT || HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED) {
                    HwABSUtils.logD(false, "MIMO MSG_DELAY_SWITCH mSwitchType=%{public}d mSwitchEvent=%{public}d", Integer.valueOf(HwABSStateMachine.this.mSwitchType), Integer.valueOf(HwABSStateMachine.this.mSwitchEvent));
                    if (!HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() || HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    } else {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(HwABSStateMachine.this.mSwitchType);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(HwABSStateMachine.this.mSwitchEvent);
                            HwABSStateMachine.this.hwABSWiFiHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine hwABSStateMachine4 = HwABSStateMachine.this;
                        hwABSStateMachine4.transitionTo(hwABSStateMachine4.mSisoState);
                    }
                }
            } else if (i != 24) {
                switch (i) {
                    case 11:
                        HwABSUtils.logE(false, "MimoState MSG_MODEM_ENTER_CONNECT_STATE", new Object[0]);
                        boolean unused9 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(1);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(1);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine hwABSStateMachine5 = HwABSStateMachine.this;
                            hwABSStateMachine5.transitionTo(hwABSStateMachine5.mSisoState);
                            break;
                        } else {
                            int unused10 = HwABSStateMachine.this.mSwitchType = 1;
                            int unused11 = HwABSStateMachine.this.mSwitchEvent = 1;
                            HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                            break;
                        }
                        break;
                    case 12:
                        HwABSUtils.logE(false, "MimoState MSG_MODEM_ENTER_CONNECT_STATE", new Object[0]);
                        boolean unused12 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(2);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(2);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine hwABSStateMachine6 = HwABSStateMachine.this;
                            hwABSStateMachine6.transitionTo(hwABSStateMachine6.mSisoState);
                            break;
                        } else {
                            int unused13 = HwABSStateMachine.this.mSwitchType = 2;
                            int unused14 = HwABSStateMachine.this.mSwitchEvent = 2;
                            HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                            break;
                        }
                        break;
                    case 13:
                        HwABSUtils.logE(false, "MimoState MSG_MODEM_EXIT_CONNECT_STATE ANTENNA_STATE_IN_CONNECT = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT));
                        if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                            boolean unused15 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = false;
                            break;
                        }
                        break;
                    case 14:
                        HwABSUtils.logE(false, "MimoState MSG_MODEM_ENTER_SEARCHING_STATE", new Object[0]);
                        boolean unused16 = HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = true;
                        this.mSubId = message.getData().getInt(HwABSUtils.SUB_ID);
                        HwABSStateMachine.this.addModemState(this.mSubId);
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(3);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(3);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine hwABSStateMachine7 = HwABSStateMachine.this;
                            hwABSStateMachine7.transitionTo(hwABSStateMachine7.mSisoState);
                            break;
                        } else {
                            int unused17 = HwABSStateMachine.this.mSwitchType = 3;
                            int unused18 = HwABSStateMachine.this.mSwitchEvent = 3;
                            HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                            break;
                        }
                        break;
                    case 15:
                        HwABSUtils.logE(false, "Mimo MSG_MODEM_EXIT_SEARCHING_STATE mModemStateList.size() == %{public}d", Integer.valueOf(HwABSStateMachine.this.mModemStateList.size()));
                        if (HwABSStateMachine.this.mModemStateList.size() != 0) {
                            this.mData = message.getData();
                            this.mSubId = this.mData.getInt(HwABSUtils.SUB_ID);
                            if (HwABSStateMachine.this.removeModemState(this.mSubId) == 0) {
                                boolean unused19 = HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = false;
                                break;
                            }
                        }
                        break;
                    case 16:
                        HwABSUtils.logE(false, "MimoState MSG_WIFI_ANTENNA_PREEMPTED", new Object[0]);
                        boolean unused20 = HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED = true;
                        if (HwABSStateMachine.this.isScreenOn()) {
                            int unused21 = HwABSStateMachine.this.mSwitchType = 4;
                            int unused22 = HwABSStateMachine.this.mSwitchEvent = 4;
                        } else {
                            int unused23 = HwABSStateMachine.this.mSwitchType = 5;
                            int unused24 = HwABSStateMachine.this.mSwitchEvent = 5;
                        }
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(HwABSStateMachine.this.mSwitchType);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(HwABSStateMachine.this.mSwitchEvent);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine hwABSStateMachine8 = HwABSStateMachine.this;
                            hwABSStateMachine8.transitionTo(hwABSStateMachine8.mSisoState);
                            break;
                        } else {
                            HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                            break;
                        }
                        break;
                    default:
                        return false;
                }
            } else {
                handleSuppliantComplete();
            }
            return true;
        }

        public void exit() {
            HwABSUtils.logD(false, "exit MimoState", new Object[0]);
            HwABSStateMachine.this.removeMessages(23);
            long mimoScreenOnTime = 0;
            long mimoTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSMIMOStartTime;
            if (HwABSStateMachine.this.mABSMIMOScreenOnStartTime != 0) {
                mimoScreenOnTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSMIMOScreenOnStartTime;
            }
            HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, mimoTime, 0, mimoScreenOnTime, 0);
            long unused = HwABSStateMachine.this.mABSMIMOScreenOnStartTime = 0;
            long unused2 = HwABSStateMachine.this.mABSMIMOStartTime = 0;
        }

        private void handleSuppliantComplete() {
            WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null && wifiInfo.getBSSID() != null) {
                if (this.mCurrentBSSID.equals(wifiInfo.getBSSID()) || !this.mCurrentSSID.equals(HwABSCHRManager.getAPSSID(wifiInfo))) {
                    if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover() && this.mCurrentBSSID.equals(wifiInfo.getBSSID())) {
                        HwABSUtils.logD(false, "mimo reassociate success", new Object[0]);
                        HwABSWiFiHandler access$2200 = HwABSStateMachine.this.mHwABSWiFiHandler;
                        HwABSWiFiHandler unused = HwABSStateMachine.this.mHwABSWiFiHandler;
                        access$2200.setTargetBssid("any");
                        HwABSStateMachine.this.sendMessage(1);
                    }
                } else if (!HwABSStateMachine.this.isApInDatabase(wifiInfo.getBSSID())) {
                    Message msg = Message.obtain();
                    msg.what = 1;
                    HwABSStateMachine.this.deferMessage(msg);
                    HwABSStateMachine hwABSStateMachine = HwABSStateMachine.this;
                    hwABSStateMachine.transitionTo(hwABSStateMachine.mWiFiConnectedState);
                } else {
                    HwABSStateMachine hwABSStateMachine2 = HwABSStateMachine.this;
                    hwABSStateMachine2.transitionTo(hwABSStateMachine2.mMimoState);
                }
            }
        }
    }

    class SisoState extends State {
        private String mCurrentBSSID = null;
        private String mCurrentSSID = null;

        SisoState() {
        }

        public void enter() {
            HwABSUtils.logD(false, "enter SisoState ANTENNA_STATE_IN_PREEMPTED = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED));
            HwABSStateMachine.this.setWiFiAntennaMonitor(true);
            if (!HwABSStateMachine.this.isScreenOn()) {
                long unused = HwABSStateMachine.this.mABSSISOScreenOnStartTime = System.currentTimeMillis();
            }
            long unused2 = HwABSStateMachine.this.mABSSISOStartTime = System.currentTimeMillis();
            WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (!(wifiInfo == null || wifiInfo.getSSID() == null || wifiInfo.getBSSID() == null)) {
                this.mCurrentSSID = HwABSCHRManager.getAPSSID(wifiInfo);
                this.mCurrentBSSID = wifiInfo.getBSSID();
            }
            if (HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED && HwABSStateMachine.this.isModemStateInIdle()) {
                boolean unused3 = HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED = false;
                HwABSStateMachine.this.handoverToMIMO();
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                HwABSUtils.logE(false, "SiSOState MSG_WIFI_CONNECTED mIsSupportVoWIFI = %{public}s", String.valueOf(HwABSStateMachine.this.mIsSupportVoWIFI));
                if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                    if (!HwABSStateMachine.this.mIsSupportVoWIFI || !HwABSStateMachine.this.mHwABSWiFiHandler.isHandoverTimeout()) {
                        HwABSStateMachine.this.updateABSAssociateSuccess();
                    } else {
                        HwABSUtils.logE(false, "SiSOState MSG_WIFI_CONNECTED handover time out", new Object[0]);
                        HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                    }
                    HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                }
            } else if (i == 2) {
                HwABSUtils.logD(false, "SiSOState MSG_WIFI_DISCONNECTED", new Object[0]);
                if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                    HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                    HwABSStateMachine.this.mHwABSCHRManager.uploadABSReassociateExeption();
                    HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                }
                HwABSStateMachine hwABSStateMachine = HwABSStateMachine.this;
                hwABSStateMachine.transitionTo(hwABSStateMachine.mWiFiDisconnectedState);
            } else if (i == 22) {
                HwABSStateMachine.this.handlePowerOffMessage();
                if (HwABSStateMachine.this.isModemStateInIdle()) {
                    HwABSStateMachine.this.handoverToMIMO();
                }
            } else if (i == 24) {
                WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
                if (!(wifiInfo == null || wifiInfo.getSSID() == null || wifiInfo.getBSSID() == null)) {
                    if (this.mCurrentBSSID.equals(wifiInfo.getBSSID()) || !this.mCurrentSSID.equals(HwABSCHRManager.getAPSSID(wifiInfo))) {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover() && this.mCurrentBSSID.equals(wifiInfo.getBSSID())) {
                            HwABSUtils.logD(false, "siso reassociate success", new Object[0]);
                            HwABSWiFiHandler access$2200 = HwABSStateMachine.this.mHwABSWiFiHandler;
                            HwABSWiFiHandler unused = HwABSStateMachine.this.mHwABSWiFiHandler;
                            access$2200.setTargetBssid("any");
                            HwABSStateMachine.this.sendMessage(1);
                        }
                    } else if (!HwABSStateMachine.this.isApInDatabase(wifiInfo.getBSSID())) {
                        Message msg = Message.obtain();
                        msg.what = 1;
                        HwABSStateMachine.this.deferMessage(msg);
                        HwABSStateMachine hwABSStateMachine2 = HwABSStateMachine.this;
                        hwABSStateMachine2.transitionTo(hwABSStateMachine2.mWiFiConnectedState);
                    } else {
                        HwABSStateMachine hwABSStateMachine3 = HwABSStateMachine.this;
                        hwABSStateMachine3.transitionTo(hwABSStateMachine3.mSisoState);
                    }
                }
            } else if (i == 101) {
                boolean isModemStateIdle = HwABSStateMachine.this.isModemStateInIdle();
                boolean isSIMCardInService = HwABSStateMachine.this.isSIMCardStatusIdle();
                boolean isInBlackList = HwABSStateMachine.this.isAPInBlackList();
                HwABSUtils.logE(false, "SiSOState CMD_WIFI_SWITCH_MIMO isModemStateInIdle = %{public}s isSIMCardInService = %{public}s isInBlackList = %{public}s", String.valueOf(isModemStateIdle), String.valueOf(isSIMCardInService), String.valueOf(isInBlackList));
                if (!isModemStateIdle || !isSIMCardInService || isInBlackList) {
                    HwABSUtils.logE(false, "SiSOState CMD_WIFI_SWITCH_MIMO keep in SISO", new Object[0]);
                } else if (!HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() || HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                    HwABSStateMachine.this.sendMessageDelayed(101, 1000);
                } else {
                    if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                        HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(8);
                        HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(7);
                        HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                        HwABSStateMachine.this.hwABSWiFiHandover(2);
                    } else {
                        HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(2);
                        HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(2);
                    }
                    HwABSStateMachine hwABSStateMachine4 = HwABSStateMachine.this;
                    hwABSStateMachine4.transitionTo(hwABSStateMachine4.mMimoState);
                }
            } else if (i != 103) {
                switch (i) {
                    case 5:
                        HwABSUtils.logE(false, "SiSOState MSG_SCREEN_ON isModemStateInIdle = %{public}s", String.valueOf(HwABSStateMachine.this.isModemStateInIdle()));
                        if (HwABSStateMachine.this.isModemStateInIdle()) {
                            if (HwABSStateMachine.this.isInPunishTime()) {
                                long mOverPunishTime = HwABSStateMachine.this.getPunishTime() - (System.currentTimeMillis() - HwABSStateMachine.this.ABS_LAST_HANDOVER_TIME);
                                HwABSUtils.logE(false, "SiSOState MSG_SCREEN_ON inpunish time = %{public}s", String.valueOf(mOverPunishTime));
                                if (mOverPunishTime > HwABSStateMachine.ABS_SCREEN_ON_TIME) {
                                    HwABSStateMachine.this.sendHandoverToMIMOMsg(101, mOverPunishTime);
                                } else {
                                    HwABSStateMachine.this.sendHandoverToMIMOMsg(101, HwABSStateMachine.ABS_SCREEN_ON_TIME);
                                }
                            } else {
                                HwABSStateMachine.this.sendHandoverToMIMOMsg(101, HwABSStateMachine.ABS_SCREEN_ON_TIME);
                            }
                        }
                        long unused2 = HwABSStateMachine.this.mABSSISOScreenOnStartTime = System.currentTimeMillis();
                        break;
                    case 6:
                        HwABSUtils.logE(false, "SiSOState MSG_SCREEN_OFF", new Object[0]);
                        HwABSStateMachine.this.removeMessages(101);
                        if (HwABSStateMachine.this.mABSSISOScreenOnStartTime != 0) {
                            HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, 0, 0, 0, System.currentTimeMillis() - HwABSStateMachine.this.mABSSISOScreenOnStartTime);
                            long unused3 = HwABSStateMachine.this.mABSSISOScreenOnStartTime = 0;
                            break;
                        }
                        break;
                    case 7:
                    case 9:
                        HwABSUtils.logD(false, "siso in or out call", new Object[0]);
                        boolean unused4 = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                        if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                            HwABSStateMachine.this.resetABSHandoverTimes();
                            break;
                        }
                        break;
                    case 8:
                        HwABSUtils.logE(false, "SisoState MSG_ANTENNA_STATE_IDLE ANTENNA_STATE_IN_CALL = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_CALL));
                        if (HwABSStateMachine.this.ANTENNA_STATE_IN_CALL) {
                            boolean unused5 = HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = false;
                            boolean unused6 = HwABSStateMachine.this.mIsInCallPunish = true;
                            HwABSStateMachine.this.handoverToMIMO();
                            break;
                        }
                        break;
                    default:
                        switch (i) {
                            case 11:
                            case 12:
                                HwABSUtils.logE(false, "SisoState MSG_MODEM_ENTER_CONNECT_STATE", new Object[0]);
                                boolean unused7 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                                break;
                            case 13:
                                HwABSUtils.logE(false, "SisoState MSG_MODEM_EXIT_CONNECT_STATE ANTENNA_STATE_IN_CONNECT = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT));
                                if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                                    boolean unused8 = HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = false;
                                    HwABSStateMachine.this.handoverToMIMO();
                                    break;
                                }
                                break;
                            case 14:
                                HwABSUtils.logE(false, "SisoState MSG_MODEM_ENTER_SEARCHING_STATE", new Object[0]);
                                boolean unused9 = HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = true;
                                HwABSStateMachine.this.removeMessages(101);
                                HwABSStateMachine.this.addModemState(message.getData().getInt(HwABSUtils.SUB_ID));
                                break;
                            case 15:
                                HwABSUtils.logE(false, "SisoState MSG_MODEM_EXIT_SEARCHING_STATE mModemStateList.size() ==%{public}d", Integer.valueOf(HwABSStateMachine.this.mModemStateList.size()));
                                if (HwABSStateMachine.this.mModemStateList.size() != 0) {
                                    Bundle mData = message.getData();
                                    int mSubId = mData.getInt(HwABSUtils.SUB_ID);
                                    int mResult = mData.getInt(HwABSUtils.RES);
                                    if (HwABSStateMachine.this.removeModemState(mSubId) == 0) {
                                        boolean unused10 = HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = false;
                                    }
                                    if (!HwABSStateMachine.this.isHaveSIMCard(mSubId)) {
                                        if (!HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH && (mResult == 0 || mResult == 1)) {
                                            HwABSStateMachine.this.handoverToMIMO();
                                            break;
                                        } else {
                                            HwABSUtils.logE(false, "SisoState keep stay in siso, have no sim card ANTENNA_STATE_IN_SEARCH = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH));
                                            break;
                                        }
                                    } else if (mResult == 0 && !HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH) {
                                        HwABSStateMachine.this.handoverToMIMO();
                                        break;
                                    } else {
                                        HwABSUtils.logE(false, "SisoState keep stay in siso, have sim card ANTENNA_STATE_IN_SEARCH = %{public}s", String.valueOf(HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH));
                                        break;
                                    }
                                }
                                break;
                            default:
                                return false;
                        }
                }
            } else {
                HwABSUtils.logD(false, "SiSOState CMD_WIFI_PAUSE_HANDOVER", new Object[0]);
                boolean unused11 = HwABSStateMachine.this.isPuaseHandover = false;
                HwABSStateMachine.this.handoverToMIMO();
            }
            return true;
        }

        public void exit() {
            HwABSUtils.logD(false, "exit SisoState", new Object[0]);
            long sisoScreenOnTime = 0;
            long sisoTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSSISOStartTime;
            if (HwABSStateMachine.this.mABSSISOScreenOnStartTime != 0) {
                sisoScreenOnTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSSISOScreenOnStartTime;
            }
            HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, 0, sisoTime, 0, sisoScreenOnTime);
            long unused = HwABSStateMachine.this.mABSSISOScreenOnStartTime = 0;
            long unused2 = HwABSStateMachine.this.mABSSISOStartTime = 0;
        }
    }

    public static HwABSStateMachine createHwABSStateMachine(Context context, ClientModeImpl wifiStateMachine) {
        if (mHwABSStateMachine == null) {
            mHwABSStateMachine = new HwABSStateMachine(context, wifiStateMachine);
        }
        return mHwABSStateMachine;
    }

    private HwABSStateMachine(Context context, ClientModeImpl wifiStateMachine) {
        super("HwABSStateMachine");
        this.mContext = context;
        this.mHwABSDataBaseManager = HwABSDataBaseManager.getInstance(context);
        this.mHwABSWiFiScenario = new HwABSWiFiScenario(context, getHandler());
        new HwABSModemScenario(context, getHandler());
        this.mHwABSWiFiHandler = new HwABSWiFiHandler(context, getHandler(), wifiStateMachine);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mHwABSCHRManager = HwABSCHRManager.getInstance(context);
        addState(this.mDefaultState);
        addState(this.mWiFiEnableState, this.mDefaultState);
        addState(this.mWiFiDisableState, this.mDefaultState);
        addState(this.mWiFiConnectedState, this.mWiFiEnableState);
        addState(this.mWiFiDisconnectedState, this.mWiFiEnableState);
        addState(this.mMimoState, this.mWiFiEnableState);
        addState(this.mSisoState, this.mWiFiEnableState);
        setInitialState(this.mDefaultState);
        start();
    }

    public void onStart() {
        this.mHwABSWiFiScenario.startMonitor();
    }

    public boolean isABSSwitching() {
        HwABSUtils.logE(false, "isABSSwitching isSwitching = %{public}s", String.valueOf(this.isSwitching));
        return this.isSwitching;
    }

    private NetworkDetail getNetWorkDetail(String bssid) {
        ScanDetail scanDetail;
        NetworkDetail detail = null;
        for (ScanResult result : this.mWifiManager.getScanResults()) {
            if (!(result.BSSID == null || !result.BSSID.equals(bssid) || (scanDetail = ScanResultUtil.toScanDetail(result)) == null)) {
                detail = scanDetail.getNetworkDetail();
            }
        }
        return detail;
    }

    /* access modifiers changed from: private */
    public boolean isUsingMIMOCapability() {
        if (this.mHwABSWiFiHandler.getCurrentCapability() == 2) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public int isAPSupportMIMOCapability(String bssid) {
        NetworkDetail mNetworkDetail = getNetWorkDetail(bssid);
        if (mNetworkDetail == null) {
            return -1;
        }
        HwABSUtils.logD(false, "isAPSupportMIMOCapability mNetworkDetail.getStream1() = %{public}d mNetworkDetail.getStream2() = %{public}d mNetworkDetail.getStream3() = %{public}d mNetworkDetail.getStream4() = %{public}d", Integer.valueOf(mNetworkDetail.getStream1()), Integer.valueOf(mNetworkDetail.getStream2()), Integer.valueOf(mNetworkDetail.getStream3()), Integer.valueOf(mNetworkDetail.getStream4()));
        return ((mNetworkDetail.getStream1() + mNetworkDetail.getStream2()) + mNetworkDetail.getStream3()) + mNetworkDetail.getStream4() >= 2 ? 1 : 0;
    }

    /* access modifiers changed from: private */
    public void hwABSWiFiHandover(int capability) {
        HwABSUtils.logD(false, "hwABSWiFiHandover capability = %{public}d", Integer.valueOf(capability));
        if (capability == 1) {
            setPunishTime();
            updateABSHandoverTime();
        }
        this.mHwABSWiFiHandler.hwABSHandover(capability);
    }

    private void setPunishTime() {
        HwABSCHRStatistics record;
        int i;
        if (this.ABS_LAST_HANDOVER_TIME == 0 || System.currentTimeMillis() - this.ABS_LAST_HANDOVER_TIME > ABS_INTERVAL_TIME) {
            this.ABS_HANDOVER_TIMES = 1;
            HwABSUtils.logD(false, "setPunishTime reset times ABS_HANDOVER_TIMES = %{public}d", Integer.valueOf(this.ABS_HANDOVER_TIMES));
        } else {
            this.ABS_HANDOVER_TIMES++;
            int i2 = this.ABS_HANDOVER_TIMES;
            if (i2 == 10) {
                this.mHwABSCHRManager.increaseEventStatistics(8);
            } else if (i2 >= 10 && (record = this.mHwABSCHRManager.getStatisticsInfo()) != null && record.max_ping_pong_times < (i = this.ABS_HANDOVER_TIMES)) {
                record.max_ping_pong_times = i;
                this.mHwABSCHRManager.updateCHRInfo(record);
            }
        }
        this.ABS_LAST_HANDOVER_TIME = System.currentTimeMillis();
    }

    /* access modifiers changed from: private */
    public boolean isHaveSIMCard(int subID) {
        int cardState = this.mTelephonyManager.getSimState(subID);
        if (cardState == 5) {
            HwABSUtils.logD(false, "isHaveSIMCard subID = %{public}d  cardState = SIM_STATE_READY", Integer.valueOf(subID));
            return true;
        }
        HwABSUtils.logD(false, "isHaveSIMCard subID = %{public}d  cardState = %{public}d", Integer.valueOf(subID), Integer.valueOf(cardState));
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isSIMCardStatusIdle() {
        boolean isCardReady = false;
        int phoneNum = this.mTelephonyManager.getPhoneCount();
        HwABSUtils.logD(false, "isSIMCardStatusIdle phoneNum = %{public}d", Integer.valueOf(phoneNum));
        if (phoneNum == 0) {
            return true;
        }
        int i = 0;
        while (true) {
            if (i >= phoneNum) {
                break;
            } else if (this.mTelephonyManager.getSimState(i) == 5) {
                isCardReady = true;
                break;
            } else {
                i++;
            }
        }
        if (isCardReady) {
            return compareSIMStatusWithCardReady(phoneNum);
        }
        HwABSUtils.logD(false, "isSIMCardStatusIdle return true", new Object[0]);
        return true;
    }

    private boolean compareSIMStatusWithCardReady(int cardNum) {
        List<Integer> statusList = new ArrayList<>();
        if (cardNum == 0) {
            return true;
        }
        for (int subId = 0; subId < cardNum; subId++) {
            int cardState = this.mTelephonyManager.getSimState(subId);
            HwABSUtils.logD(false, "compareSIMStatusWithCardReady subId = %{public}d cardState = %{public}d", Integer.valueOf(subId), Integer.valueOf(cardState));
            if (cardState != 5) {
                statusList.add(2);
            } else {
                ServiceState serviceState = this.mTelephonyManager.getServiceStateForSubscriber(subId);
                if (serviceState == null) {
                    statusList.add(1);
                } else {
                    int voiceState = serviceState.getState();
                    HwABSUtils.logD(false, "compareSIMStatusWithCardReady subId = %{public}d voiceState = %{public}d", Integer.valueOf(subId), Integer.valueOf(voiceState));
                    if (voiceState == 0 || voiceState == 3) {
                        statusList.add(2);
                    } else {
                        statusList.add(1);
                    }
                }
            }
        }
        for (int i = 0; i < statusList.size(); i++) {
            if (statusList.get(i).intValue() != 2) {
                HwABSUtils.logD(false, "compareSIMStatusWithCardReady return false", new Object[0]);
                return false;
            }
        }
        HwABSUtils.logD(false, "compareSIMStatusWithCardReady return true", new Object[0]);
        return true;
    }

    /* access modifiers changed from: private */
    public void setWiFiAntennaMonitor(boolean enable) {
        if (enable) {
            HwABSUtils.logD(false, "setWiFiAntennaMonitor enable", new Object[0]);
        } else {
            HwABSUtils.logD(false, "setWiFiAntennaMonitor disable", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public boolean isScreenOn() {
        if (((PowerManager) this.mContext.getSystemService("power")).isScreenOn()) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void resetCapablity(int capablity) {
        HwABSUtils.logD(false, "resetCapablity capablity = %{public}d", Integer.valueOf(capablity));
        if (capablity != 2) {
            this.mHwABSWiFiHandler.setAPCapability(capablity);
            this.mHwABSWiFiHandler.setABSCurrentState(capablity);
        } else if (isModemStateInIdle() && !isInPunishTime()) {
            this.mHwABSWiFiHandler.setAPCapability(capablity);
            this.mHwABSWiFiHandler.setABSCurrentState(capablity);
        }
    }

    /* access modifiers changed from: private */
    public boolean isModemStateInIdle() {
        if (this.ANTENNA_STATE_IN_CALL || this.ANTENNA_STATE_IN_SEARCH || this.ANTENNA_STATE_IN_CONNECT || !isScreenOn() || this.isPuaseHandover) {
            HwABSUtils.logD(false, "isModemStateInIdle return false ANTENNA_STATE_IN_CALL = %{public}s  ANTENNA_STATE_IN_SEARCH = %{public}s  ANTENNA_STATE_IN_CONNECT = %{public}s isScreenOn() = %{public}s isPuaseHandover = %{public}s", String.valueOf(this.ANTENNA_STATE_IN_CALL), String.valueOf(this.ANTENNA_STATE_IN_SEARCH), String.valueOf(this.ANTENNA_STATE_IN_CONNECT), String.valueOf(isScreenOn()), String.valueOf(this.isPuaseHandover));
            return false;
        }
        HwABSUtils.logD(false, "isModemStateInIdle return true", new Object[0]);
        return true;
    }

    /* access modifiers changed from: private */
    public void addModemState(int subId) {
        HwABSUtils.logD(false, "addModemState subId = %{public}d", Integer.valueOf(subId));
        if (this.mModemStateList.size() == 0) {
            this.mModemStateList.add(Integer.valueOf(subId));
        } else {
            int i = 0;
            while (i < this.mModemStateList.size()) {
                if (this.mModemStateList.get(i).intValue() != subId) {
                    i++;
                } else {
                    return;
                }
            }
            this.mModemStateList.add(Integer.valueOf(subId));
        }
        HwABSUtils.logD(false, "addModemState size = %{public}d", Integer.valueOf(this.mModemStateList.size()));
    }

    /* access modifiers changed from: private */
    public int removeModemState(int subId) {
        HwABSUtils.logD(false, "removeModemState size = %{public}d subId = %{public}d", Integer.valueOf(this.mModemStateList.size()), Integer.valueOf(subId));
        if (this.mModemStateList.size() == 0) {
            return 0;
        }
        int flag = -1;
        int i = 0;
        while (true) {
            if (i >= this.mModemStateList.size()) {
                break;
            }
            HwABSUtils.logD(false, "removeModemState mModemStateList.get(i) = %{public}d subId = %{public}d", this.mModemStateList.get(i), Integer.valueOf(subId));
            if (this.mModemStateList.get(i).intValue() == subId) {
                flag = i;
                break;
            }
            i++;
        }
        if (flag != -1) {
            this.mModemStateList.remove(flag);
        }
        HwABSUtils.logD(false, "removeModemState size = %{public}d", Integer.valueOf(this.mModemStateList.size()));
        return this.mModemStateList.size();
    }

    /* access modifiers changed from: private */
    public boolean isInPunishTime() {
        long sPunishTim = getPunishTime();
        if (this.ABS_LAST_HANDOVER_TIME > System.currentTimeMillis()) {
            this.ABS_LAST_HANDOVER_TIME = System.currentTimeMillis();
        }
        long currentTimeMillis = System.currentTimeMillis();
        long j = this.ABS_LAST_HANDOVER_TIME;
        if (currentTimeMillis - j > sPunishTim) {
            HwABSUtils.logD(false, "isInPunishTime is in not in punish", new Object[0]);
            return false;
        }
        HwABSUtils.logD(false, "isInPunishTime is in punish  sPunishTim =%{public}s", String.valueOf((j + sPunishTim) - System.currentTimeMillis()));
        return true;
    }

    /* access modifiers changed from: private */
    public long getPunishTime() {
        int i = this.ABS_HANDOVER_TIMES;
        long sPunishTim = ((long) (i * i)) * ABS_PUNISH_TIME;
        if (sPunishTim > ABS_INTERVAL_TIME) {
            return ABS_INTERVAL_TIME;
        }
        return sPunishTim;
    }

    /* access modifiers changed from: private */
    public void handoverToMIMO() {
        HwABSUtils.logD(false, "handoverToMIMO", new Object[0]);
        if (!isModemStateInIdle()) {
            HwABSUtils.logD(false, "handoverToMIMO is not in idle ignore it", new Object[0]);
            return;
        }
        if (hasMessages(101)) {
            removeMessages(101);
            HwABSUtils.logD(false, "handoverToMIMO is already have message remove it", new Object[0]);
        }
        if (isInPunishTime()) {
            long mOverPunishTime = getPunishTime() - (System.currentTimeMillis() - this.ABS_LAST_HANDOVER_TIME);
            HwABSUtils.logD(false, "handoverToMIMO mOverPunishTime = %{public}s mIsInCallPunish = %{public}s", String.valueOf(mOverPunishTime), String.valueOf(this.mIsInCallPunish));
            if (!this.mIsInCallPunish || mOverPunishTime >= ABS_PUNISH_TIME) {
                sendMessageDelayed(101, mOverPunishTime);
            } else {
                HwABSUtils.logD(false, "handoverToMIMO reset punish time = %{public}s", String.valueOf((long) ABS_PUNISH_TIME));
                sendMessageDelayed(101, ABS_PUNISH_TIME);
            }
        } else if (this.mIsInCallPunish) {
            HwABSUtils.logD(false, "handoverToMIMO mIsInCallPunish punish time = %{public}s", String.valueOf((long) ABS_PUNISH_TIME));
            sendMessageDelayed(101, ABS_PUNISH_TIME);
        } else {
            sendMessageDelayed(101, 2000);
        }
        this.mIsInCallPunish = false;
    }

    /* access modifiers changed from: private */
    public void sendHandoverToMIMOMsg(int msg, long time) {
        if (hasMessages(msg)) {
            removeMessages(msg);
        }
        sendMessageDelayed(msg, time);
    }

    /* access modifiers changed from: private */
    public boolean isAirModeOn() {
        Context context = this.mContext;
        if (context != null && Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) == 1) {
            return true;
        }
        return false;
    }

    private List<Integer> getPowerOffSIMSubId() {
        List<Integer> subId = new ArrayList<>();
        int phoneNum = this.mTelephonyManager.getPhoneCount();
        HwABSUtils.logD(false, "getPowerOffSIMSubId phoneNum = %{public}d", Integer.valueOf(phoneNum));
        if (phoneNum == 0) {
            return subId;
        }
        for (int i = 0; i < phoneNum; i++) {
            ServiceState serviceState = this.mTelephonyManager.getServiceStateForSubscriber(i);
            if (serviceState != null) {
                int voiceState = serviceState.getState();
                HwABSUtils.logD(false, "getPowerOffSIMSubId subID = %{public}d voiceState = %{public}d", Integer.valueOf(i), Integer.valueOf(voiceState));
                if (voiceState == 3) {
                    subId.add(Integer.valueOf(i));
                }
            }
        }
        return subId;
    }

    /* access modifiers changed from: private */
    public void handlePowerOffMessage() {
        if (this.ANTENNA_STATE_IN_SEARCH) {
            List<Integer> list = getPowerOffSIMSubId();
            if (list.size() != 0) {
                for (Integer num : list) {
                    removeModemState(num.intValue());
                }
                if (this.mModemStateList.size() == 0) {
                    this.ANTENNA_STATE_IN_SEARCH = false;
                }
            }
        }
        if (this.ANTENNA_STATE_IN_CONNECT) {
            this.ANTENNA_STATE_IN_CONNECT = false;
        }
    }

    /* access modifiers changed from: private */
    public boolean isMobileAP() {
        if (this.mContext != null) {
            return HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(this.mContext);
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void updateABSAssociateTimes(int associateTimes, int associateFailedTimes) {
        String ssid;
        String bssid;
        if (associateTimes == 1) {
            WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
            if (mWifiInfo == null || mWifiInfo.getBSSID() == null || mWifiInfo.getSSID() == null) {
                HwABSUtils.logE(false, "updateABSAssociateTimes mWifiInfo error", new Object[0]);
                return;
            }
            bssid = mWifiInfo.getBSSID();
            ssid = HwABSCHRManager.getAPSSID(mWifiInfo);
            this.mAssociateSSID = ssid;
            this.mAssociateBSSID = bssid;
        } else {
            ssid = this.mAssociateSSID;
            bssid = this.mAssociateBSSID;
        }
        HwABSApInfoData hwABSApInfoData = this.mHwABSDataBaseManager.getApInfoByBssid(bssid);
        if (hwABSApInfoData != null) {
            int blackListStatus = hwABSApInfoData.mIn_black_List;
            hwABSApInfoData.mReassociate_times += associateTimes;
            hwABSApInfoData.mFailed_times += associateFailedTimes;
            if (associateFailedTimes != 0) {
                updateABSAssociateFailedEvent(hwABSApInfoData);
            }
            this.mHwABSDataBaseManager.addOrUpdateApInfos(hwABSApInfoData);
            if (blackListStatus == 0 && hwABSApInfoData.mIn_black_List == 1) {
                setBlackListBssid();
                uploadBlackListException(hwABSApInfoData);
            }
        } else {
            HwABSUtils.logE(false, "updateABSAssociateTimes error!!", new Object[0]);
        }
        this.mHwABSCHRManager.updateCHRAssociateTimes(ssid, associateTimes, associateFailedTimes);
    }

    private void updateABSAssociateFailedEvent(HwABSApInfoData data) {
        int lowFailedRate;
        int highFailedRate;
        int continuousTimes;
        int failedRate = 0;
        HwABSUtils.logE(false, "updateABSAssociateFailedEvent mIsSupportVoWIFI = %{public}s", String.valueOf(this.mIsSupportVoWIFI));
        if (!this.mIsSupportVoWIFI) {
            continuousTimes = 3;
            highFailedRate = 10;
            lowFailedRate = 30;
        } else {
            continuousTimes = 2;
            highFailedRate = 5;
            lowFailedRate = 15;
        }
        data.mContinuous_failure_times++;
        if (data.mContinuous_failure_times >= continuousTimes) {
            HwABSUtils.logE(false, "updateABSAssociateFailedEvent mContinuous_failure_times = %{public}d", Integer.valueOf(data.mContinuous_failure_times));
            data.mIn_black_List = 1;
            this.mAddBlackListReason = 1;
            return;
        }
        if (data.mReassociate_times > 50) {
            failedRate = highFailedRate;
        } else if (data.mReassociate_times > 10) {
            failedRate = lowFailedRate;
        }
        int temp = (data.mFailed_times * 100) / data.mReassociate_times;
        HwABSUtils.logE(false, "updateABSAssociateFailedEvent temp = %{public}d failedRate = %{public}d", Integer.valueOf(temp), Integer.valueOf(failedRate));
        if (failedRate > 0 && temp > failedRate) {
            data.mIn_black_List = 1;
            this.mAddBlackListReason = 2;
        } else if (isHandoverTooMuch(data.mBssid)) {
            HwABSUtils.logE(false, "updateABSAssociateFailedEvent isHandoverTooMach", new Object[0]);
            data.mIn_black_List = 1;
            data.mSwitch_siso_type = 15;
            this.mAddBlackListReason = 3;
        }
    }

    /* access modifiers changed from: private */
    public boolean isAPInBlackList() {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
            HwABSUtils.logE(false, "isAPInBlackList mWifiInfo error", new Object[0]);
            return false;
        }
        HwABSApInfoData hwABSApInfoData = this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID());
        if (hwABSApInfoData == null || hwABSApInfoData.mIn_black_List != 1) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void updateABSAssociateSuccess() {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
            HwABSUtils.logE(false, "updateABSAssociateSuccess mWifiInfo error", new Object[0]);
            return;
        }
        HwABSApInfoData hwABSApInfoData = this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID());
        if (hwABSApInfoData != null) {
            hwABSApInfoData.mContinuous_failure_times = 0;
            this.mHwABSDataBaseManager.addOrUpdateApInfos(hwABSApInfoData);
        }
    }

    public void setBlackListBssid() {
        StringBuilder blackList = new StringBuilder();
        List<HwABSApInfoData> lists = initBlackListDate();
        if (lists.size() != 0) {
            for (HwABSApInfoData data : lists) {
                blackList.append(data.mBssid);
                blackList.append(";");
            }
            HwABSUtils.logD(false, "blackList  size = %{public}d", Integer.valueOf(lists.size()));
            this.mHwABSWiFiHandler.setABSBlackList(blackList.toString());
        }
    }

    private List<HwABSApInfoData> initBlackListDate() {
        List<HwABSApInfoData> lists = this.mHwABSDataBaseManager.getApInfoInBlackList();
        if (lists.size() <= 10) {
            return lists;
        }
        return seleteBlackApInfo(lists);
    }

    private List<HwABSApInfoData> seleteBlackApInfo(List<HwABSApInfoData> lists) {
        int size;
        List<HwABSApInfoData> result = new ArrayList<>();
        Collections.sort(lists);
        Collections.reverse(lists);
        if (lists.size() <= 10) {
            size = lists.size();
        } else {
            size = 10;
        }
        for (int i = 0; i < size; i++) {
            result.add(lists.get(i));
        }
        return result;
    }

    /* access modifiers changed from: private */
    public boolean isApInDatabase(String bssid) {
        if (this.mHwABSDataBaseManager.getApInfoByBssid(bssid) != null) {
            return true;
        }
        return false;
    }

    private void uploadBlackListException(HwABSApInfoData data) {
        List<HwABSApInfoData> lists = this.mHwABSDataBaseManager.getAllApInfo();
        List<HwABSApInfoData> blacklists = this.mHwABSDataBaseManager.getApInfoInBlackList();
        HwABSCHRBlackListEvent event = new HwABSCHRBlackListEvent();
        event.mABSApSsid = data.mSsid;
        event.mABSApBssid = data.mBssid;
        event.mABSAddReason = this.mAddBlackListReason;
        event.mABSSuportVoWifi = this.mIsSupportVoWIFI ? 1 : 0;
        event.mABSSwitchTimes = data.mReassociate_times;
        event.mABSFailedTimes = data.mFailed_times;
        if (lists != null) {
            event.mABSTotalNum = lists.size();
        }
        if (blacklists != null) {
            event.mABSBlackListNum = blacklists.size();
        }
        this.mHwABSCHRManager.uploadBlackListException(event);
    }

    public void notifySelEngineEnableWiFi() {
        HwABSUtils.logD(false, "notifySelEngineEnableWiFi", new Object[0]);
        HwABSWiFiHandler hwABSWiFiHandler = this.mHwABSWiFiHandler;
        hwABSWiFiHandler.setAPCapability(hwABSWiFiHandler.getCurrentCapability());
    }

    public void notifySelEngineResetCompelete() {
        HwABSUtils.logD(false, "notifySelEngineResetCompelete", new Object[0]);
        sendMessage(38);
    }

    public void puaseABSHandover() {
        HwABSUtils.logD(false, "puaseABSHandover, isPuaseHandover =%{public}s", String.valueOf(this.isPuaseHandover));
        if (!this.isPuaseHandover) {
            this.isPuaseHandover = true;
        } else if (hasMessages(103)) {
            removeMessages(103);
            HwABSUtils.logD(false, "puaseABSHandover is already have message remove it", new Object[0]);
        }
    }

    public void restartABSHandover() {
        if (this.isPuaseHandover && !hasMessages(103)) {
            HwABSUtils.logD(false, "restartABSHandover send delay message ", new Object[0]);
            sendMessageDelayed(103, (long) this.RESTART_ABS_TIME);
        }
    }

    private static class APHandoverInfo {
        public long lastTime;
        public int mHandoverTimes;

        private APHandoverInfo() {
            this.mHandoverTimes = 0;
            this.lastTime = 0;
        }
    }

    private long getTimesMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(11, 0);
        cal.set(13, 0);
        cal.set(12, 0);
        cal.set(14, 0);
        return cal.getTimeInMillis();
    }

    private boolean isInOneDay(long now) {
        long startTime = getTimesMorning();
        long endTime = ONEDAYA_TIME + startTime;
        if (startTime > now || now > endTime) {
            return false;
        }
        return true;
    }

    private void updateABSHandoverTime() {
        long curTime = System.currentTimeMillis();
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info == null || info.getBSSID() == null) {
            HwABSUtils.logE(false, "updateABSHandoverTime error ", new Object[0]);
        } else if (this.ANTENNA_STATE_IN_PREEMPTED || this.ANTENNA_STATE_IN_SEARCH || this.ANTENNA_STATE_IN_CONNECT) {
            if (this.mAPHandoverInfoList.containsKey(info.getBSSID())) {
                APHandoverInfo curApInfo = this.mAPHandoverInfoList.get(info.getBSSID());
                if (curApInfo != null) {
                    if (isInOneDay(curApInfo.lastTime)) {
                        curApInfo.mHandoverTimes++;
                        curApInfo.lastTime = curTime;
                    } else {
                        HwABSUtils.logE(false, "updateABSHandoverTime not in one day", new Object[0]);
                        curApInfo.mHandoverTimes = 1;
                        curApInfo.lastTime = curTime;
                        removeABSHandoverTimes();
                    }
                    HwABSUtils.logE(false, "updateABSHandoverTime curApInfo.mHandoverTimes = %{public}d", Integer.valueOf(curApInfo.mHandoverTimes));
                    this.mAPHandoverInfoList.put(info.getBSSID(), curApInfo);
                    return;
                }
                HwABSUtils.logE(false, "updateABSHandoverTime curApInfo == null", new Object[0]);
                this.mAPHandoverInfoList.remove(info.getBSSID());
            }
            APHandoverInfo apInfo = new APHandoverInfo();
            apInfo.mHandoverTimes = 1;
            apInfo.lastTime = curTime;
            this.mAPHandoverInfoList.put(info.getBSSID(), apInfo);
        } else {
            HwABSUtils.logE(false, "updateABSHandoverTime do not mach type ", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void resetABSHandoverTimes() {
        APHandoverInfo curApInfo;
        long curTime = System.currentTimeMillis();
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info == null || info.getBSSID() == null) {
            HwABSUtils.logE(false, "resetABSHandoverTimes error ", new Object[0]);
        } else if (this.mAPHandoverInfoList.containsKey(info.getBSSID()) && (curApInfo = this.mAPHandoverInfoList.get(info.getBSSID())) != null && curApInfo.mHandoverTimes >= 1) {
            HwABSUtils.logE(false, "resetABSHandoverTimes reset ", new Object[0]);
            curApInfo.mHandoverTimes--;
            curApInfo.lastTime = curTime;
            this.mAPHandoverInfoList.put(info.getBSSID(), curApInfo);
        }
    }

    private boolean isHandoverTooMuch(String bssid) {
        APHandoverInfo curApInfo;
        if (bssid == null || !this.mAPHandoverInfoList.containsKey(bssid) || (curApInfo = this.mAPHandoverInfoList.get(bssid)) == null) {
            return false;
        }
        HwABSUtils.logE(false, "isHandoverTooMach mHandoverTimes = %{public}d", Integer.valueOf(curApInfo.mHandoverTimes));
        if (curApInfo.mHandoverTimes >= 15) {
            return true;
        }
        return false;
    }

    private void removeABSHandoverTimes() {
        HwABSUtils.logE(false, "removeABSHandoverTimes", new Object[0]);
        List<String> strArray = new ArrayList<>();
        for (Map.Entry<String, APHandoverInfo> entry : this.mAPHandoverInfoList.entrySet()) {
            String bssidKey = entry.getKey();
            if (!isInOneDay(entry.getValue().lastTime)) {
                strArray.add(bssidKey);
            }
        }
        for (String keyWord : strArray) {
            this.mAPHandoverInfoList.remove(keyWord);
        }
    }
}
