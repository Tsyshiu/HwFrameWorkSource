package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HwDualBandStateMachine extends StateMachine {
    private static final int CHR_WIFI_HIGH_SCAN_FREQUENCY = 5;
    private static final int CHR_WIFI_MID_SCAN_FREQUENCY = 3;
    private static final int COMMON_SIGANL_LEVEL = 3;
    private static final int INITIAL_RSSI = -200;
    private static final int WIFI_MAX_SCAN_THRESHOLD = -30;
    private static final int WIFI_MIN_SCAN_THRESHOLD = -90;
    private static final int WIFI_RSSI_GAP = 10;
    private static final int WIFI_SCANNING_CHANNEL_INDEX = 3;
    private static final int WIFI_SCANNING_CHANNEL_INDEX_MAX = 5;
    private static final int WIFI_SCANNING_CHANNEL_INDEX_MIN = 0;
    private static final int WIFI_SCAN_INTERVAL = 5;
    private WifiProDualbandExceptionRecord mCHRHandoverTooSlow = new WifiProDualbandExceptionRecord();
    /* access modifiers changed from: private */
    public int mCHRMixAPScanCount = 0;
    private List<HwDualBandMonitorInfo> mCHRSavedAPList = new ArrayList();
    /* access modifiers changed from: private */
    public int mCHRScanAPType = 0;
    /* access modifiers changed from: private */
    public int mCHRSingleAPScanCount = 0;
    /* access modifiers changed from: private */
    public int mChrDualBandScanCount = 0;
    /* access modifiers changed from: private */
    public State mConnectedState = new ConnectedState();
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public State mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public State mDisabledState = new DisabledState();
    /* access modifiers changed from: private */
    public List<HwDualBandMonitorInfo> mDisappearAPList = new ArrayList();
    /* access modifiers changed from: private */
    public State mDisconnectedState = new DisconnectedState();
    /* access modifiers changed from: private */
    public HwDualBandAdaptiveThreshold mHwDualBandAdaptiveThreshold;
    /* access modifiers changed from: private */
    public HwDualBandInformationManager mHwDualBandInformationManager = null;
    /* access modifiers changed from: private */
    public HwDualBandRelationManager mHwDualBandRelationManager = null;
    private HwDualBandWiFiMonitor mHwDualBandWiFiMonitor = null;
    /* access modifiers changed from: private */
    public IDualBandManagerCallback mIDualbandManagerCallback = null;
    /* access modifiers changed from: private */
    public State mInternetReadyState = new InternetReadyState();
    /* access modifiers changed from: private */
    public boolean mIsDualbandScanning = false;
    /* access modifiers changed from: private */
    public List<HwDualBandMonitorInfo> mMonitorAPList = new ArrayList();
    /* access modifiers changed from: private */
    public State mMonitorState = new MonitorState();
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public State mStopState = new StopState();
    /* access modifiers changed from: private */
    public List<HwDualBandMonitorInfo> mTargetAPList;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;

    static /* synthetic */ int access$1608(HwDualBandStateMachine x0) {
        int i = x0.mChrDualBandScanCount;
        x0.mChrDualBandScanCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1708(HwDualBandStateMachine x0) {
        int i = x0.mCHRSingleAPScanCount;
        x0.mCHRSingleAPScanCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1808(HwDualBandStateMachine x0) {
        int i = x0.mCHRMixAPScanCount;
        x0.mCHRMixAPScanCount = i + 1;
        return i;
    }

    public HwDualBandStateMachine(Context context, IDualBandManagerCallback callBack) {
        super("HwDualBandStateMachine");
        this.mContext = context;
        this.mIDualbandManagerCallback = callBack;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mHwDualBandWiFiMonitor = new HwDualBandWiFiMonitor(context, getHandler());
        this.mHwDualBandInformationManager = HwDualBandInformationManager.createInstance(context);
        this.mHwDualBandRelationManager = HwDualBandRelationManager.createInstance(context, getHandler());
        this.mHwDualBandAdaptiveThreshold = new HwDualBandAdaptiveThreshold(context);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        addState(this.mDefaultState);
        addState(this.mDisabledState, this.mDefaultState);
        addState(this.mConnectedState, this.mDefaultState);
        addState(this.mMonitorState, this.mConnectedState);
        addState(this.mInternetReadyState, this.mConnectedState);
        addState(this.mDisconnectedState, this.mDefaultState);
        addState(this.mStopState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        start();
    }

    public void onStart() {
        getHandler().sendEmptyMessage(100);
        this.mHwDualBandWiFiMonitor.startMonitor();
    }

    public void onStop() {
        this.mHwDualBandWiFiMonitor.stopMonitor();
        getHandler().sendEmptyMessage(101);
    }

    public Handler getStateMachineHandler() {
        return getHandler();
    }

    public boolean isDualbandScanning() {
        return this.mIsDualbandScanning;
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter DefaultState");
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                Log.e(HwDualBandMessageUtil.TAG, "DefaultState MSG_WIFI_CONNECTED");
                HwDualBandStateMachine hwDualBandStateMachine = HwDualBandStateMachine.this;
                hwDualBandStateMachine.transitionTo(hwDualBandStateMachine.mConnectedState);
            } else if (i == 2) {
                HwDualBandStateMachine hwDualBandStateMachine2 = HwDualBandStateMachine.this;
                hwDualBandStateMachine2.transitionTo(hwDualBandStateMachine2.mDisconnectedState);
            } else if (i == 4) {
                HwDualBandStateMachine hwDualBandStateMachine3 = HwDualBandStateMachine.this;
                hwDualBandStateMachine3.transitionTo(hwDualBandStateMachine3.mDisabledState);
            } else if (i == 8) {
                Log.e(HwDualBandMessageUtil.TAG, "DefaultState MSG_WIFI_CONFIG_CHANGED");
                Bundle data = message.getData();
                String ssid = data.getString("ssid");
                int authtype = data.getInt(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE);
                Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_CONFIG_CHANGED ssid = " + WifiProCommonUtils.safeDisplaySsid(ssid));
                if (ssid != null) {
                    HwDualBandStateMachine.this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(ssid, authtype);
                }
            } else if (i == 101) {
                HwDualBandStateMachine hwDualBandStateMachine4 = HwDualBandStateMachine.this;
                hwDualBandStateMachine4.transitionTo(hwDualBandStateMachine4.mStopState);
            }
            return true;
        }
    }

    class DisabledState extends State {
        DisabledState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter DisabledState");
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            if (HwDualBandStateMachine.this.mTargetAPList != null && HwDualBandStateMachine.this.mTargetAPList.size() != 0) {
                HwDualBandStateMachine.this.mTargetAPList.clear();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what != 4) {
                return false;
            }
            return true;
        }
    }

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter ConnectedState");
            WifiInfo currentWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (currentWifiInfo != null && currentWifiInfo.getBSSID() != null) {
                Log.e(HwDualBandMessageUtil.TAG, "Enter ConnectedState ssid = " + WifiProCommonUtils.safeDisplaySsid(currentWifiInfo.getSSID()));
            }
        }

        public void exit() {
            HwDualBandStateMachine.this.removeMessages(11);
        }

        public boolean processMessage(Message message) {
            WifiProDualBandApInfoRcd info;
            int i = message.what;
            if (i != 1) {
                switch (i) {
                    case 11:
                        Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_CONNECTED");
                        WifiInfo mInternetWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                        if (mInternetWifiInfo != null && mInternetWifiInfo.getBSSID() != null) {
                            HwDualBandStateMachine hwDualBandStateMachine = HwDualBandStateMachine.this;
                            hwDualBandStateMachine.transitionTo(hwDualBandStateMachine.mInternetReadyState);
                            HwDualBandStateMachine.this.sendMessage(104);
                            break;
                        } else {
                            if (mInternetWifiInfo == null) {
                                Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_CONNECTED mInternetWifiInfo == null");
                            } else {
                                Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_CONNECTED mInternetWifiInfo.getBSSID() == null");
                            }
                            HwDualBandStateMachine.this.sendMessageDelayed(11, 2000);
                            return true;
                        }
                        break;
                    case 12:
                    case 13:
                        Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_DISCONNECTED");
                        WifiInfo mNoInternetWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                        if (!(mNoInternetWifiInfo == null || mNoInternetWifiInfo.getBSSID() == null || (info = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mNoInternetWifiInfo.getBSSID())) == null)) {
                            info.mInetCapability = 2;
                            HwDualBandStateMachine.this.mHwDualBandInformationManager.updateAPInfo(info);
                            break;
                        }
                    default:
                        return false;
                }
            }
            return true;
        }
    }

    class InternetReadyState extends State {
        private String mCurrentBSSID = null;
        private String mCurrentSSID = null;
        private HwDualBandMonitorInfo mDualBandMonitorInfo = null;
        private int mLastRecordLevel = 0;
        private int mLastRecordScore = 0;

        InternetReadyState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter InternetReadyState");
            WifiInfo currentWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (currentWifiInfo == null || currentWifiInfo.getBSSID() == null) {
                Log.e(HwDualBandMessageUtil.TAG, "Enter InternetReadyState error info");
                return;
            }
            this.mCurrentSSID = currentWifiInfo.getSSID();
            this.mCurrentBSSID = currentWifiInfo.getBSSID();
            Log.e(HwDualBandMessageUtil.TAG, "Enter InternetReadyState mCurrentSSID = " + StringUtilEx.safeDisplaySsid(this.mCurrentSSID));
        }

        public boolean processMessage(Message message) {
            List<ScanResult> mLists;
            int i = message.what;
            if (i == 1) {
                WifiInfo mConnectedWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                if (!(mConnectedWifiInfo == null || mConnectedWifiInfo.getBSSID() == null || mConnectedWifiInfo.getBSSID().equals(this.mCurrentBSSID))) {
                    Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_CONNECTED");
                    HwDualBandStateMachine hwDualBandStateMachine = HwDualBandStateMachine.this;
                    hwDualBandStateMachine.transitionTo(hwDualBandStateMachine.mConnectedState);
                }
            } else if (i != 7) {
                if (i == 19) {
                    Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_VERIFYING_POOR_LINK");
                    HwDualBandStateMachine hwDualBandStateMachine2 = HwDualBandStateMachine.this;
                    hwDualBandStateMachine2.transitionTo(hwDualBandStateMachine2.mConnectedState);
                } else if (i == 102) {
                    List unused = HwDualBandStateMachine.this.mTargetAPList = message.getData().getParcelableArrayList(HwDualBandMessageUtil.MSG_KEY_APLIST);
                    if (HwDualBandStateMachine.this.mTargetAPList != null) {
                        Log.e(HwDualBandMessageUtil.TAG, "CMD_START_MONITOR size = " + HwDualBandStateMachine.this.mTargetAPList.size());
                    }
                    HwDualBandStateMachine hwDualBandStateMachine3 = HwDualBandStateMachine.this;
                    hwDualBandStateMachine3.transitionTo(hwDualBandStateMachine3.mMonitorState);
                } else if (i == 104) {
                    WifiInfo mInternetWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                    if (mInternetWifiInfo == null || mInternetWifiInfo.getBSSID() == null) {
                        Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState mInternetWifiInfo == null");
                    } else if (HwDualBandStateMachine.this.mHwDualBandInformationManager.isEnterpriseSecurity(mInternetWifiInfo.getNetworkId()) || isMobileAP()) {
                        Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState isEnterpriseAP");
                        HwDualBandStateMachine.this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(mInternetWifiInfo.getSSID(), HwDualBandStateMachine.this.mHwDualBandInformationManager.getAuthType(mInternetWifiInfo.getNetworkId()));
                    } else {
                        HwDualBandStateMachine.this.mHwDualBandInformationManager.saveAPInfo();
                        HwDualBandStateMachine.this.mHwDualBandRelationManager.updateAPRelation();
                    }
                } else if (i == 16) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE");
                    setDualBandNetWorkType(HwDualBandStateMachine.this.mWifiManager.getConnectionInfo());
                } else if (i != 17) {
                    switch (i) {
                        case 11:
                        case 13:
                            break;
                        default:
                            return false;
                        case 12:
                            Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_INTERNET_DISCONNECTED");
                            HwDualBandStateMachine hwDualBandStateMachine4 = HwDualBandStateMachine.this;
                            hwDualBandStateMachine4.transitionTo(hwDualBandStateMachine4.mConnectedState);
                            HwDualBandStateMachine.this.sendMessage(12);
                            break;
                    }
                } else {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX");
                    this.mDualBandMonitorInfo = null;
                    this.mLastRecordLevel = 0;
                    this.mLastRecordScore = -1;
                    WifiInfo mMixWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                    if (!(mMixWifiInfo == null || mMixWifiInfo.getBSSID() == null)) {
                        WifiProDualBandApInfoRcd mMixAPinfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mMixWifiInfo.getBSSID());
                        if (mMixAPinfo == null) {
                            Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX mMixAPinfo == null");
                        } else {
                            List<ScanResult> lists = WifiproUtils.getScanResultsFromWsm();
                            if (lists == null) {
                                Log.e(HwDualBandMessageUtil.TAG, "getScanResultsFromWsm lists is null");
                            } else {
                                List<WifiProRelateApRcd> mMixLists = mMixAPinfo.getRelateApRcds();
                                Log.e(HwDualBandMessageUtil.TAG, "mMixLists.size() = " + mMixLists.size());
                                setRecordLevel(lists, mMixLists);
                                List<HwDualBandMonitorInfo> mMixAPList = new ArrayList<>();
                                if (this.mDualBandMonitorInfo != null) {
                                    Log.i(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX select ssid = " + WifiProCommonUtils.safeDisplaySsid(this.mDualBandMonitorInfo.mSsid));
                                    mMixAPList.add(this.mDualBandMonitorInfo);
                                }
                                Log.e(HwDualBandMessageUtil.TAG, "filter mMixAPList.size() = " + mMixAPList.size());
                                if (mMixAPList.size() > 0) {
                                    HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(2, mMixAPList, getScanApNums(lists, mMixLists));
                                }
                            }
                        }
                    }
                }
            } else if (HwDualBandBlackListManager.getHwDualBandBlackListMgrInstance().getWifiBlacklist().isEmpty() && HwDualBandStateMachine.this.mWifiManager != null && (mLists = HwDualBandStateMachine.this.mWifiManager.getScanResults()) != null && mLists.size() > 0 && is5gApAvailble(mLists)) {
                Log.i(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_UPDATE_SCAN_RESULT");
                HwDualBandStateMachine.this.sendMessage(104);
            }
            return true;
        }

        private void setDualBandNetWorkType(WifiInfo currentWifiInfo) {
            if (currentWifiInfo != null && currentWifiInfo.getBSSID() != null) {
                WifiProDualBandApInfoRcd apinfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(currentWifiInfo.getBSSID());
                if (apinfo == null) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE apinfo == null");
                    return;
                }
                List<WifiProRelateApRcd> mLists = apinfo.getRelateApRcds();
                if (mLists.size() <= 0) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE mLists.size() <= 0");
                    return;
                }
                Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE mLists.size() = " + mLists.size());
                WifiProRelateApRcd relateApInfo = mLists.get(0);
                WifiProDualBandApInfoRcd m5GAPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(relateApInfo.mRelatedBSSID);
                if (m5GAPInfo == null) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE m5GAPInfo == null");
                } else if (m5GAPInfo.mInBlackList == 1) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE m5GAPInfo.isInBlackList = " + m5GAPInfo.mInBlackList);
                } else {
                    HwDualBandMonitorInfo mHwDualBandMonitorInfo = new HwDualBandMonitorInfo(relateApInfo.mRelatedBSSID, m5GAPInfo.mApSSID, m5GAPInfo.mApAuthType.shortValue(), 0, 0, relateApInfo.mRelateType);
                    mHwDualBandMonitorInfo.mIsNearAP = 1;
                    List<HwDualBandMonitorInfo> apList = new ArrayList<>();
                    Log.i(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE find ssid = " + WifiProCommonUtils.safeDisplaySsid(mHwDualBandMonitorInfo.mSsid) + " m5GAPInfo.mApAuthType");
                    apList.add(mHwDualBandMonitorInfo);
                    HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(1, apList, 1);
                }
            }
        }

        private int getScanApNums(List<ScanResult> lists, List<WifiProRelateApRcd> mMixLists) {
            int target5gApNum = 0;
            for (WifiProRelateApRcd record : mMixLists) {
                for (ScanResult result : lists) {
                    boolean isBssidValue = false;
                    boolean isSsidValue = result.SSID != null && result.SSID.length() > 0;
                    if (result.BSSID != null && result.BSSID.equals(record.mRelatedBSSID)) {
                        isBssidValue = true;
                    }
                    if (isSsidValue && isBssidValue) {
                        target5gApNum++;
                    }
                }
            }
            Log.i(HwDualBandMessageUtil.TAG, "target5GApNum = " + target5gApNum);
            return target5gApNum;
        }

        private void setRecordLevel(List<ScanResult> lists, List<WifiProRelateApRcd> mMixLists) {
            int i;
            for (WifiProRelateApRcd record : mMixLists) {
                WifiProDualBandApInfoRcd m5GAPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(record.mRelatedBSSID);
                if (m5GAPInfo == null) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX m5GAPInfo == null");
                } else if (m5GAPInfo.mInBlackList == 1) {
                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX mIsInblackList = " + m5GAPInfo.mInBlackList);
                } else if (m5GAPInfo.mInetCapability.shortValue() == 1) {
                    HwDualBandMonitorInfo mHwDualBandMonitorInfo = new HwDualBandMonitorInfo(m5GAPInfo.mApBSSID, m5GAPInfo.mApSSID, m5GAPInfo.mApAuthType.shortValue(), 0, 0, record.mRelateType);
                    if (mHwDualBandMonitorInfo.mIsDualbandAp == 1 || isNearAP(record)) {
                        mHwDualBandMonitorInfo.mIsNearAP = 1;
                    } else {
                        mHwDualBandMonitorInfo.mIsNearAP = 0;
                    }
                    Iterator<ScanResult> it = lists.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        ScanResult result = it.next();
                        boolean ssidValue = result.SSID != null && result.SSID.length() > 0;
                        boolean bssidValue = result.BSSID != null && result.BSSID.equals(mHwDualBandMonitorInfo.mBssid);
                        if (ssidValue && bssidValue) {
                            Log.i(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX find ssid = " + WifiProCommonUtils.safeDisplaySsid(mHwDualBandMonitorInfo.mSsid) + " , mApAuthType = " + m5GAPInfo.mApAuthType + " , level = " + result.level);
                            HwDualBandMonitorInfo hwDualBandMonitorInfo = this.mDualBandMonitorInfo;
                            if (hwDualBandMonitorInfo != null) {
                                if (hwDualBandMonitorInfo.mIsDualbandAp == 1 && (i = this.mLastRecordLevel) < 0 && i >= -65) {
                                    Log.i(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX  AP_TYPE_SINGLE");
                                    break;
                                } else if (mHwDualBandMonitorInfo.mIsDualbandAp != 1 || result.level < -65) {
                                    selectBest5gAp(mHwDualBandMonitorInfo, result);
                                } else {
                                    this.mDualBandMonitorInfo = mHwDualBandMonitorInfo;
                                    this.mLastRecordLevel = result.level;
                                    this.mLastRecordScore = WifiProCommonUtils.calculateScore(result);
                                }
                            } else {
                                this.mDualBandMonitorInfo = mHwDualBandMonitorInfo;
                                this.mLastRecordLevel = result.level;
                                this.mLastRecordScore = WifiProCommonUtils.calculateScore(result);
                            }
                        }
                    }
                } else {
                    Log.i(HwDualBandMessageUtil.TAG, "ssid = " + WifiProCommonUtils.safeDisplaySsid(m5GAPInfo.mApSSID) + " have no internet");
                }
            }
        }

        private void selectBest5gAp(HwDualBandMonitorInfo hwDualBandMonitorInfo, ScanResult result) {
            if (this.mLastRecordLevel >= 0 || hwDualBandMonitorInfo == null || result == null) {
                Log.e(HwDualBandMessageUtil.TAG, "abnormal value, return.");
                return;
            }
            int newScore = WifiProCommonUtils.calculateScore(result);
            Log.i(HwDualBandMessageUtil.TAG, "result bssid = " + StringUtilEx.safeDisplayBssid(result.BSSID) + ", rssi = " + result.level + ", supportedWifiCategory = " + result.supportedWifiCategory + ", mLastRecordScore = " + this.mLastRecordScore + ", mLastRecordLevel = " + this.mLastRecordLevel + ", newScore = " + newScore);
            int i = this.mLastRecordScore;
            if (newScore > i || (newScore == i && result.level > this.mLastRecordLevel)) {
                this.mDualBandMonitorInfo = hwDualBandMonitorInfo;
                this.mLastRecordLevel = result.level;
                this.mLastRecordScore = newScore;
            }
        }

        private boolean is5gApAvailble(List<ScanResult> scanResults) {
            if (scanResults == null) {
                return false;
            }
            if (HwDualBandStateMachine.this.mWifiManager == null) {
                return false;
            }
            List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
            int scanResultsSize = scanResults.size();
            for (int i = 0; i < scanResultsSize; i++) {
                ScanResult nextResult = scanResults.get(i);
                if (!(nextResult == null || configNetworks == null || !nextResult.is5GHz())) {
                    int signalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                    int configNetworksSize = configNetworks.size();
                    if (signalLevel >= 3) {
                        for (int k = 0; k < configNetworksSize; k++) {
                            WifiConfiguration nextConfig = configNetworks.get(k);
                            StringBuilder sb = new StringBuilder();
                            sb.append("\"");
                            sb.append(nextResult.SSID);
                            sb.append("\"");
                            if (((nextConfig != null && nextConfig.SSID.equals(sb.toString())) && (nextConfig != null && nextConfig.SSID != null && WifiProCommonUtils.isSameEncryptType(nextResult.capabilities, nextConfig.configKey()))) && nextConfig != null && !nextConfig.noInternetAccess && !WifiProCommonUtils.isOpenAndPortal(nextConfig)) {
                                return true;
                            }
                        }
                        continue;
                    } else {
                        continue;
                    }
                }
            }
            return false;
        }

        private boolean isNearAP(WifiProRelateApRcd record) {
            if (record.mMaxRelatedRSSI == 0 || record.mMinCurrentRSSI == 0) {
                if (record.mMaxCurrentRSSI - record.mMinRelatedRSSI <= 10) {
                    return true;
                }
                return false;
            } else if (record.mMaxCurrentRSSI - record.mMinRelatedRSSI > 10 || record.mMaxRelatedRSSI - record.mMinCurrentRSSI > 10) {
                return false;
            } else {
                return true;
            }
        }

        private boolean isMobileAP() {
            if (HwDualBandStateMachine.this.mContext != null) {
                return HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(HwDualBandStateMachine.this.mContext);
            }
            return false;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter DisconnectedState");
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            if (!(HwDualBandStateMachine.this.mTargetAPList == null || HwDualBandStateMachine.this.mTargetAPList.size() == 0)) {
                HwDualBandStateMachine.this.mTargetAPList.clear();
            }
            if (HwDualBandStateMachine.this.mDisappearAPList != null && HwDualBandStateMachine.this.mDisappearAPList.size() != 0) {
                HwDualBandStateMachine.this.mDisappearAPList.clear();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what != 2) {
                return false;
            }
            return true;
        }
    }

    class MonitorState extends State {
        private String m24GBssid = null;
        private int m24GRssi = -1;
        private int mScanIndex = 0;

        MonitorState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter MonitorState");
            boolean unused = HwDualBandStateMachine.this.mIsDualbandScanning = true;
            this.mScanIndex = 0;
            int unused2 = HwDualBandStateMachine.this.mChrDualBandScanCount = 0;
            int unused3 = HwDualBandStateMachine.this.mCHRSingleAPScanCount = 0;
            int unused4 = HwDualBandStateMachine.this.mCHRMixAPScanCount = 0;
            int unused5 = HwDualBandStateMachine.this.mCHRScanAPType = 0;
            this.m24GBssid = null;
            this.m24GRssi = -1;
            HwDualBandStateMachine.this.mDisappearAPList.clear();
            WifiInfo currentWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (currentWifiInfo == null) {
                Log.e(HwDualBandMessageUtil.TAG, "currentWifiInfo is null");
                return;
            }
            this.m24GBssid = currentWifiInfo.getBSSID();
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mTargetAPList) {
                if (info.mDualBandApInfoRcd == null) {
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState info.mDualBandApInfoRcd == null, ssid = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid));
                } else if (info.mIsDualbandAp == 1) {
                    info.mScanRssi = HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.getScanRSSIThreshold(currentWifiInfo.getBSSID(), info.mBssid, info.mTargetRssi);
                    Log.i(HwDualBandMessageUtil.TAG, "MonitorState isdulabanAP 2.4G SSID = " + WifiProCommonUtils.safeDisplaySsid(currentWifiInfo.getSSID()) + " BSSID = " + WifiProCommonUtils.safeDisplayBssid(currentWifiInfo.getBSSID()) + " 5G SSID = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " BSSID = " + WifiProCommonUtils.safeDisplayBssid(info.mBssid) + " info.mScanRssi = " + info.mScanRssi + " info.mTargetRssi = " + info.mTargetRssi);
                    HwDualBandStateMachine.this.mMonitorAPList.add(info);
                } else {
                    WifiProDualBandApInfoRcd aPInfo = null;
                    WifiProRelateApRcd relationInfo = null;
                    if (currentWifiInfo.getBSSID() != null) {
                        aPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(currentWifiInfo.getBSSID());
                        relationInfo = HwDualBandStateMachine.this.mHwDualBandRelationManager.getRelateAPInfo(currentWifiInfo.getBSSID(), info.mBssid);
                    }
                    if (!(relationInfo == null || aPInfo == null)) {
                        if (info.mIsNearAP != 1 || relationInfo.mMinCurrentRSSI == 0) {
                            info.mScanRssi = relationInfo.mMaxCurrentRSSI;
                        } else {
                            info.mScanRssi = info.mTargetRssi - 5;
                        }
                        info.mInitializationRssi = info.mScanRssi;
                        Log.e(HwDualBandMessageUtil.TAG, "MonitorState mix AP 2.4G SSID = " + WifiProCommonUtils.safeDisplaySsid(currentWifiInfo.getSSID()) + " BSSID = " + WifiProCommonUtils.safeDisplayBssid(currentWifiInfo.getBSSID()) + " 5G SSID= " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " BSSID = " + WifiProCommonUtils.safeDisplayBssid(info.mBssid) + " info.mScanRssi = " + info.mScanRssi + " info.mTargetRssi = " + info.mTargetRssi + " info.mAuthType = " + info.mAuthType + " info.mIsNearAP = " + info.mIsNearAP + " aPInfo.mChannelFrequency = " + aPInfo.mChannelFrequency + " aPInfo.mApAuthType = " + aPInfo.mApAuthType);
                        HwDualBandStateMachine.this.mMonitorAPList.add(info);
                    }
                }
            }
            if (HwDualBandStateMachine.this.mMonitorAPList.size() <= 0) {
                HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(0, HwDualBandStateMachine.this.mMonitorAPList, 0);
                HwDualBandStateMachine.this.sendMessage(103);
            }
        }

        public void exit() {
            boolean unused = HwDualBandStateMachine.this.mIsDualbandScanning = false;
            this.mScanIndex = 0;
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            int unused2 = HwDualBandStateMachine.this.mCHRSingleAPScanCount = 0;
            int unused3 = HwDualBandStateMachine.this.mChrDualBandScanCount = 0;
            int unused4 = HwDualBandStateMachine.this.mCHRMixAPScanCount = 0;
            int unused5 = HwDualBandStateMachine.this.mCHRScanAPType = 0;
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 2) {
                HwDualBandStateMachine.this.initDualbandChrHandoverTooSlow(this.m24GBssid, this.m24GRssi);
                HwDualBandStateMachine hwDualBandStateMachine = HwDualBandStateMachine.this;
                hwDualBandStateMachine.transitionTo(hwDualBandStateMachine.mDisconnectedState);
            } else if (i == 103) {
                Log.e(HwDualBandMessageUtil.TAG, "MonitorState CMD_STOP_MONITOR");
                HwDualBandStateMachine hwDualBandStateMachine2 = HwDualBandStateMachine.this;
                hwDualBandStateMachine2.transitionTo(hwDualBandStateMachine2.mInternetReadyState);
            } else if (i != 7) {
                boolean sceneLimited = false;
                if (i == 8) {
                    Bundle removeData = message.getData();
                    String ssid = removeData.getString("ssid");
                    int authtype = removeData.getInt(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE);
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_REMOVE_CONFIG_CHANGED ssid = " + WifiProCommonUtils.safeDisplaySsid(ssid));
                    if (ssid != null) {
                        HwDualBandStateMachine.this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(ssid, authtype);
                        removeFromMonitorList(ssid);
                        if (HwDualBandStateMachine.this.mMonitorAPList instanceof ArrayList) {
                            Object obj = ((ArrayList) HwDualBandStateMachine.this.mMonitorAPList).clone();
                            if (obj instanceof ArrayList) {
                                Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_REMOVE_CONFIG_CHANGED tmpMonitorList.size() = " + HwDualBandStateMachine.this.mMonitorAPList.size());
                                HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(0, (ArrayList) obj, 0);
                                HwDualBandStateMachine hwDualBandStateMachine3 = HwDualBandStateMachine.this;
                                hwDualBandStateMachine3.transitionTo(hwDualBandStateMachine3.mInternetReadyState);
                            }
                        } else {
                            Log.e(HwDualBandMessageUtil.TAG, "MonitorState type is not match");
                        }
                    }
                } else if (i == 18) {
                    this.m24GRssi = message.getData().getInt(HwDualBandMessageUtil.MSG_KEY_RSSI);
                    boolean screenValue = isFullscreen() || !HwDualBandStateMachine.this.mPowerManager.isScreenOn() || WifiProCommonUtils.isLandscapeMode(HwDualBandStateMachine.this.mContext);
                    boolean commonValue = WifiProCommonUtils.isCalling(HwDualBandStateMachine.this.mContext) || !isSuppOnCompletedState();
                    if (screenValue || commonValue) {
                        sceneLimited = true;
                    }
                    Log.i(HwDualBandMessageUtil.TAG, "MonitorState m24GRssi = " + this.m24GRssi + " , sceneLimited = " + sceneLimited);
                    if (!sceneLimited && isSatisfiedScanCondition(this.m24GRssi)) {
                        if (this.mScanIndex >= 3 || WifiProCommonUtils.isQueryActivityMatched(HwDualBandStateMachine.this.mContext, WifiProCommonUtils.HUAWEI_SETTINGS_WLAN)) {
                            Log.e(HwDualBandMessageUtil.TAG, "startScan for full channels, mScanIndex = " + this.mScanIndex);
                            HwDualBandStateMachine.this.mWifiManager.startScan();
                        } else {
                            WifiScanner.ScanSettings settings = getCustomizedScanSettings();
                            Log.e(HwDualBandMessageUtil.TAG, "startScan for restrict channels, mScanIndex = " + this.mScanIndex);
                            if (settings != null) {
                                HwDualBandStateMachine.this.startCustomizedScan(settings);
                            } else {
                                HwDualBandStateMachine.this.mWifiManager.startScan();
                            }
                        }
                        this.mScanIndex++;
                        HwDualBandStateMachine.access$1608(HwDualBandStateMachine.this);
                        int i2 = this.mScanIndex;
                        if (i2 > 5) {
                            i2 = 5;
                        }
                        this.mScanIndex = i2;
                        if (HwDualBandStateMachine.this.mCHRScanAPType == 1) {
                            HwDualBandStateMachine.access$1708(HwDualBandStateMachine.this);
                        } else if (HwDualBandStateMachine.this.mCHRScanAPType == 2) {
                            HwDualBandStateMachine.access$1808(HwDualBandStateMachine.this);
                        }
                    }
                } else if (i != 19) {
                    return false;
                } else {
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_VERIFYING_POOR_LINK");
                    HwDualBandStateMachine hwDualBandStateMachine4 = HwDualBandStateMachine.this;
                    hwDualBandStateMachine4.transitionTo(hwDualBandStateMachine4.mConnectedState);
                }
            } else {
                Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_UPDATE_SCAN_RESULT");
                List<ScanResult> mLists = HwDualBandStateMachine.this.mWifiManager.getScanResults();
                if (mLists != null && mLists.size() > 0 && isSatisfiedScanResult(mLists)) {
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_UPDATE_SCAN_RESULT find AP");
                    HwDualBandStateMachine hwDualBandStateMachine5 = HwDualBandStateMachine.this;
                    hwDualBandStateMachine5.transitionTo(hwDualBandStateMachine5.mInternetReadyState);
                }
            }
            return true;
        }

        private boolean isFullscreen() {
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 9, new Bundle());
            if (result != null) {
                return result.getBoolean("isFullscreen");
            }
            return false;
        }

        private boolean isSuppOnCompletedState() {
            WifiInfo info = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (info == null || info.getSupplicantState().ordinal() != SupplicantState.COMPLETED.ordinal()) {
                return false;
            }
            return true;
        }

        private boolean isSatisfiedScanCondition(int rssi) {
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mMonitorAPList) {
                int unused = HwDualBandStateMachine.this.mCHRScanAPType = info.mIsDualbandAp;
                if (info.mIsNearAP == 1) {
                    if (rssi >= info.mScanRssi) {
                        return true;
                    }
                } else if (rssi <= -90 || rssi > info.mInitializationRssi) {
                    info.mScanRssi = info.mInitializationRssi;
                    return false;
                } else if (rssi <= info.mScanRssi) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSatisfiedScanResult(List<ScanResult> mLists) {
            List<HwDualBandMonitorInfo> mAPList = new ArrayList<>();
            for (ScanResult result : mLists) {
                String str = this.m24GBssid;
                if (str != null && str.equals(result.BSSID)) {
                    this.m24GRssi = result.level;
                    Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult m24GRssi = " + this.m24GRssi);
                }
                if (HwDualBandStateMachine.this.mDisappearAPList.size() > 0) {
                    for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mDisappearAPList) {
                        boolean ssidValue = result.SSID != null && result.SSID.length() > 0;
                        boolean bssidValue = info.mBssid.equals(result.BSSID) && !isInMonitorList(info);
                        if (ssidValue && bssidValue) {
                            HwDualBandStateMachine.this.mMonitorAPList.add(info);
                        }
                    }
                }
            }
            if (this.m24GRssi == -1) {
                Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult m24GBssid == -1");
                return false;
            }
            int scanResultsFound = getScanResultsFound(mLists, mAPList);
            Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult mMonitorAPList.size = " + HwDualBandStateMachine.this.mMonitorAPList.size() + ", scanResultsFound = " + scanResultsFound + ", mScanIndex = " + this.mScanIndex + ", mAPList.size = " + mAPList.size() + ", mDisappearAPList.size() = " + HwDualBandStateMachine.this.mDisappearAPList.size());
            if (this.mScanIndex >= 5) {
                int unused = HwDualBandStateMachine.this.mCHRSingleAPScanCount = 0;
                int unused2 = HwDualBandStateMachine.this.mCHRMixAPScanCount = 0;
                int unused3 = HwDualBandStateMachine.this.mCHRScanAPType = 0;
                updateAPInfo(HwDualBandStateMachine.this.mDisappearAPList);
                HwDualBandStateMachine.this.sendMessage(103);
                return false;
            }
            if (scanResultsFound == HwDualBandStateMachine.this.mMonitorAPList.size()) {
                this.mScanIndex = 0;
            }
            Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult mAPList.size() = " + mAPList.size() + ", scanResultsFound = " + scanResultsFound + ", mScanIndex = " + this.mScanIndex);
            if (mAPList.size() <= 0) {
                return false;
            }
            HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkFind(mAPList, HwDualBandStateMachine.this.mChrDualBandScanCount);
            HwDualBandStateMachine.this.sendMessage(103);
            return true;
        }

        private int getScanResultsFound(List<ScanResult> mLists, List<HwDualBandMonitorInfo> mAPList) {
            int scanResults = 0;
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mMonitorAPList) {
                boolean isMonitorAPFound = false;
                for (ScanResult result : mLists) {
                    if (info.mBssid.equals(result.BSSID) && result.SSID != null && result.SSID.length() > 0) {
                        Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult result.SSID = " + StringUtilEx.safeDisplaySsid(result.SSID) + " result.level = " + result.level);
                        isMonitorAPFound = true;
                        scanResults++;
                        String scanSSID = "\"" + result.SSID + "\"";
                        if (result.frequency != info.mDualBandApInfoRcd.mChannelFrequency || !info.mSsid.equals(scanSSID)) {
                            Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult update AP frequency mChannelFrequency = " + info.mDualBandApInfoRcd.mChannelFrequency + " new frequency = " + result.frequency);
                            info.mDualBandApInfoRcd.mChannelFrequency = result.frequency;
                            info.mDualBandApInfoRcd.mApSSID = scanSSID;
                            info.mSsid = scanSSID;
                            HwDualBandInformationManager.getInstance().updateAPInfo(info.mDualBandApInfoRcd);
                        }
                        if (info.mIsDualbandAp == 1) {
                            processSingleAPResult(info, result, mAPList);
                        } else {
                            processMixAPResult(info, result, mAPList);
                        }
                    }
                }
                if (!isMonitorAPFound && info.mIsDualbandAp == 2) {
                    info.mScanRssi = updateScanBssid(info, -200);
                }
                if (this.m24GRssi > info.mScanRssi && info.mIsDualbandAp == 1 && !isMonitorAPFound) {
                    HwDualBandStateMachine.this.addDisappearAPList(info);
                    HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.updateRSSIThreshold(this.m24GBssid, info.mBssid, this.m24GRssi, -127, info.mScanRssi, info.mTargetRssi);
                    info.mScanRssi = HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.getScanRSSIThreshold(this.m24GBssid, info.mBssid, info.mTargetRssi);
                    Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult renew info.mSsid = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " info.mScanRssi = " + info.mScanRssi + " info.mTargetRssi = " + info.mTargetRssi);
                }
            }
            return scanResults;
        }

        private void processSingleAPResult(HwDualBandMonitorInfo info, ScanResult result, List<HwDualBandMonitorInfo> mAPList) {
            if (this.m24GRssi >= info.mScanRssi) {
                HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.updateRSSIThreshold(this.m24GBssid, result.BSSID, this.m24GRssi, result.level, info.mScanRssi, info.mTargetRssi);
                info.mScanRssi = HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.getScanRSSIThreshold(this.m24GBssid, result.BSSID, info.mTargetRssi);
                Log.e(HwDualBandMessageUtil.TAG, "processSingleAPResult renew info.mSsid = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " info.mScanRssi = " + info.mScanRssi + " info.mTargetRssi = " + info.mTargetRssi);
            }
            if (result.level >= info.mTargetRssi) {
                info.mCurrentRssi = result.level;
                mAPList.add(info);
                Log.e(HwDualBandMessageUtil.TAG, "processSingleAPResult info.mSsid = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " info.mCurrentRssi = " + info.mCurrentRssi + " info.mTargetRssi = " + info.mTargetRssi);
            }
            if (info.mDualBandApInfoRcd.mDisappearCount > 0) {
                Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult update AP disappear number = " + info.mDualBandApInfoRcd.mDisappearCount + " --> 0");
                info.mDualBandApInfoRcd.mDisappearCount = 0;
                HwDualBandInformationManager.getInstance().updateAPInfo(info.mDualBandApInfoRcd);
            }
        }

        private void processMixAPResult(HwDualBandMonitorInfo info, ScanResult result, List<HwDualBandMonitorInfo> mAPList) {
            if (result.level >= info.mTargetRssi) {
                info.mCurrentRssi = result.level;
                mAPList.add(info);
                Log.e(HwDualBandMessageUtil.TAG, "processMixAPResult info.mSsid = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " info.mCurrentRssi = " + info.mCurrentRssi + " info.mTargetRssi = " + info.mTargetRssi);
                return;
            }
            info.mScanRssi = updateScanBssid(info, result.level);
            Log.e(HwDualBandMessageUtil.TAG, "processMixAPResult renew info.mSsid = " + WifiProCommonUtils.safeDisplaySsid(info.mSsid) + " info.mScanRssi = " + info.mScanRssi + " info.mTargetRssi = " + info.mTargetRssi);
        }

        private WifiScanner.ScanSettings getCustomizedScanSettings() {
            WifiInfo currentWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (currentWifiInfo == null || currentWifiInfo.getBSSID() == null || HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(currentWifiInfo.getBSSID()) == null) {
                return null;
            }
            WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
            settings.band = 0;
            settings.reportEvents = 3;
            settings.numBssidsPerScan = 0;
            settings.channels = new WifiScanner.ChannelSpec[HwDualBandStateMachine.this.mMonitorAPList.size()];
            int index = 0;
            for (HwDualBandMonitorInfo record : HwDualBandStateMachine.this.mMonitorAPList) {
                WifiProDualBandApInfoRcd scanAPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(record.mBssid);
                if (scanAPInfo != null) {
                    int index2 = index + 1;
                    settings.channels[index] = new WifiScanner.ChannelSpec(scanAPInfo.mChannelFrequency);
                    Log.i(HwDualBandMessageUtil.TAG, "getCustomizedScanSettings:  Frequency = " + scanAPInfo.mChannelFrequency);
                    index = index2;
                }
            }
            if (index != HwDualBandStateMachine.this.mMonitorAPList.size()) {
                return null;
            }
            return settings;
        }

        private int updateScanBssid(HwDualBandMonitorInfo info, int rssi) {
            int targetScanRssi;
            if (info.mIsNearAP == 1) {
                targetScanRssi = info.mScanRssi + 5;
            } else {
                targetScanRssi = info.mScanRssi - 5;
            }
            if (targetScanRssi >= HwDualBandStateMachine.WIFI_MAX_SCAN_THRESHOLD || targetScanRssi <= -90) {
                targetScanRssi = info.mInitializationRssi;
            }
            Log.e(HwDualBandMessageUtil.TAG, "updateScanBssid targetScanRssi = " + targetScanRssi + " scanRssi = " + info.mScanRssi + " mInitializationRssi = " + info.mInitializationRssi + " rssi = " + rssi);
            return targetScanRssi;
        }

        private void removeFromMonitorList(String ssid) {
            List<HwDualBandMonitorInfo> delectList = new ArrayList<>();
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mMonitorAPList) {
                if (info.mSsid.equals(ssid)) {
                    delectList.add(info);
                }
            }
            for (HwDualBandMonitorInfo info2 : delectList) {
                HwDualBandStateMachine.this.mMonitorAPList.remove(info2);
            }
        }

        private void updateAPInfo(List<HwDualBandMonitorInfo> disappearAPList) {
            for (HwDualBandMonitorInfo info : disappearAPList) {
                if (info.mDualBandApInfoRcd != null) {
                    info.mDualBandApInfoRcd.mDisappearCount++;
                    Log.e(HwDualBandMessageUtil.TAG, "updateAPInfo info.mSsid = " + info.mSsid + ", info.mDualBandApInfoRcd.mDisappearCount = " + info.mDualBandApInfoRcd.mDisappearCount);
                    if (info.mDualBandApInfoRcd.mDisappearCount > 3) {
                        HwDualBandInformationManager.getInstance().delectDualBandAPInfoBySsid(info.mSsid, info.mAuthType);
                    } else {
                        HwDualBandInformationManager.getInstance().updateAPInfo(info.mDualBandApInfoRcd);
                    }
                }
                removeFromMonitorList(info.mSsid);
            }
        }

        private boolean isInMonitorList(HwDualBandMonitorInfo info) {
            if (HwDualBandStateMachine.this.mMonitorAPList.size() <= 0) {
                return false;
            }
            for (HwDualBandMonitorInfo monitorInfo : HwDualBandStateMachine.this.mMonitorAPList) {
                if (monitorInfo.mBssid.equals(info.mBssid)) {
                    return true;
                }
            }
            return false;
        }
    }

    class StopState extends State {
        StopState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter StopState");
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            if (HwDualBandStateMachine.this.mTargetAPList != null && HwDualBandStateMachine.this.mTargetAPList.size() != 0) {
                HwDualBandStateMachine.this.mTargetAPList.clear();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what != 100) {
                return true;
            }
            HwDualBandStateMachine hwDualBandStateMachine = HwDualBandStateMachine.this;
            hwDualBandStateMachine.transitionTo(hwDualBandStateMachine.mDefaultState);
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void initDualbandChrHandoverTooSlow(String ssid, int rssi) {
        WifiProDualbandExceptionRecord wifiProDualbandExceptionRecord = this.mCHRHandoverTooSlow;
        if (wifiProDualbandExceptionRecord != null) {
            wifiProDualbandExceptionRecord.mSSID_2G = ssid;
            wifiProDualbandExceptionRecord.mRSSI_2G = (short) rssi;
            Log.e(HwDualBandMessageUtil.TAG, "db_chr initDualbandChrHandoverTooSlow mCHR24GSsid" + StringUtilEx.safeDisplaySsid(ssid) + ", m24GRssi = " + ((int) this.mCHRHandoverTooSlow.mRSSI_2G));
            for (HwDualBandMonitorInfo info : this.mMonitorAPList) {
                this.mCHRSavedAPList.add(info);
            }
        }
    }

    /* access modifiers changed from: private */
    public void startCustomizedScan(WifiScanner.ScanSettings requested) {
        Bundle data = new Bundle();
        data.putParcelable("ScanSettings", requested);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 10, data);
    }

    /* access modifiers changed from: private */
    public void addDisappearAPList(HwDualBandMonitorInfo info) {
        boolean addFlag = true;
        Iterator<HwDualBandMonitorInfo> it = this.mDisappearAPList.iterator();
        while (true) {
            if (it.hasNext()) {
                if (it.next().mBssid.equals(info.mBssid)) {
                    addFlag = false;
                    break;
                }
            } else {
                break;
            }
        }
        if (addFlag) {
            this.mDisappearAPList.add(info);
        }
    }
}
