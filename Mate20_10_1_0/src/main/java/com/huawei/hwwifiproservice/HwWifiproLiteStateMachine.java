package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.wifi.HwHiLog;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import java.security.SecureRandom;

public class HwWifiproLiteStateMachine extends StateMachine {
    public static final String ACTION_NOTIFY_WIFI_INTERNET_STATUS = "com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_INTERNET_STATUS";
    private static final int BASE = 131072;
    private static final int CMD_HTTP_GET_RESULT_RCVD = 109;
    private static final int CMD_HTTP_UNREACHABLE_BY_FIRST_DETECT = 103;
    private static final int CMD_INTERNET_CAPABILITY_RCVD = 104;
    private static final int CMD_INTERNET_STATUS_DETECT_INTERVAL = 107;
    private static final int CMD_NETWORK_CONNECTED_RCVD = 101;
    private static final int CMD_NETWORK_DISCONNECTED_RCVD = 102;
    private static final int CMD_NOTIFY_NO_INTERNET_REASON = 110;
    private static final int CMD_NO_INTERNET_DETECT_INTERVAL = 106;
    private static final int CMD_NO_INTERNET_SHOW_TOAST = 112;
    private static final int CMD_PORTAL_DETECT_INTERVAL = 105;
    private static final int CMD_SCE_NOTIFY_HTTP_GET_RESULT = 108;
    private static final int CMD_TCP_PKTS_RESP_RCVD = 100;
    private static final int CMD_WIFI_DISABLED_RCVD = 111;
    public static final String EXTRA_INTERNET_STATUS = "internet_status";
    public static final String EXTRA_NETWORK_CONNECTED_STATUS = "network_connected_status_portal";
    private static final int INVALID_LINK_DETECTED = 131875;
    private static final int NO_INTERNET_BY_PORTAL = -101;
    private static final int NO_INTERNET_BY_UNREACHABLE = -102;
    private static final String TAG = "HwWifiproLiteStateMachine";
    private static HwWifiproLiteStateMachine mLiteStateMachine = null;
    /* access modifiers changed from: private */
    public State mConnectedState = new ConnectedState();
    /* access modifiers changed from: private */
    public ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public String mCurrentSsid;
    /* access modifiers changed from: private */
    public WifiConfiguration mCurrentWifiConfig;
    private State mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public State mDisconnectedState = new DisconnectedState();
    /* access modifiers changed from: private */
    public State mHasInternetMonitorState = new HasInternetMonitorState();
    private boolean mInitialized = false;
    /* access modifiers changed from: private */
    public IPQosMonitor mIpQosMonitor;
    /* access modifiers changed from: private */
    public State mNoInternetMonitorState = new NoInternetMonitorState();
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    private WiFiProEvaluateController mWiFiProEvaluateController;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    private WifiProConfigStore mWifiProConfigStore;
    /* access modifiers changed from: private */
    public WifiProUIDisplayManager mWifiProUIDisplayManager;
    private AsyncChannel mWsmChannel;

    public static synchronized HwWifiproLiteStateMachine getInstance(Context context, Messenger messenger, Looper looper) {
        HwWifiproLiteStateMachine hwWifiproLiteStateMachine;
        synchronized (HwWifiproLiteStateMachine.class) {
            if (mLiteStateMachine == null) {
                mLiteStateMachine = new HwWifiproLiteStateMachine(context, messenger, looper);
            }
            hwWifiproLiteStateMachine = mLiteStateMachine;
        }
        return hwWifiproLiteStateMachine;
    }

    public static synchronized HwWifiproLiteStateMachine getInstance() {
        HwWifiproLiteStateMachine hwWifiproLiteStateMachine;
        synchronized (HwWifiproLiteStateMachine.class) {
            hwWifiproLiteStateMachine = mLiteStateMachine;
        }
        return hwWifiproLiteStateMachine;
    }

    private HwWifiproLiteStateMachine(Context context, Messenger messenger, Looper looper) {
        super(TAG);
        Looper looper2 = null;
        this.mWifiProConfigStore = null;
        this.mWiFiProEvaluateController = null;
        this.mWifiProUIDisplayManager = null;
        this.mContext = null;
        this.mWifiManager = null;
        this.mPowerManager = null;
        this.mWsmChannel = null;
        this.mCurrentSsid = null;
        this.mCurrentWifiConfig = null;
        this.mIpQosMonitor = null;
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mWsmChannel = new AsyncChannel();
        this.mWsmChannel.connectSync(this.mContext, getHandler(), messenger);
        this.mWifiProConfigStore = new WifiProConfigStore(this.mContext, this.mWsmChannel);
        this.mWiFiProEvaluateController = new WiFiProEvaluateController(context);
        this.mWifiProUIDisplayManager = WifiProUIDisplayManager.createInstance(context, null);
        this.mIpQosMonitor = new IPQosMonitor(getHandler());
        WifiProStatisticsManager.initStatisticsManager(this.mContext, getHandler() != null ? getHandler().getLooper() : looper2);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 29, new Bundle());
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        addState(this.mDefaultState);
        addState(this.mDisconnectedState, this.mDefaultState);
        addState(this.mConnectedState, this.mDefaultState);
        addState(this.mHasInternetMonitorState, this.mConnectedState);
        addState(this.mNoInternetMonitorState, this.mConnectedState);
        setInitialState(this.mDisconnectedState);
        start();
    }

    public synchronized void setup() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            registerReceivers();
        }
    }

    public synchronized void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction(WifiProCommonDefs.ACTION_FIRST_CHECK_NO_INTERNET_NOTIFICATION);
        intentFilter.addAction(WifiProCommonDefs.ACTION_NETWOR_PROPERTY_NOTIFICATION);
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.HwWifiproLiteStateMachine.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    Object infoTemp = intent.getParcelableExtra("networkInfo");
                    if (infoTemp instanceof NetworkInfo) {
                        NetworkInfo info = (NetworkInfo) infoTemp;
                        if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                            HwWifiproLiteStateMachine.this.sendMessage(102);
                        } else if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                            HwWifiproLiteStateMachine.this.sendMessage(101);
                        }
                    } else {
                        HwHiLog.w(HwWifiproLiteStateMachine.TAG, false, "registerReceivers:info is not match the Class", new Object[0]);
                    }
                } else if (WifiProCommonDefs.ACTION_FIRST_CHECK_NO_INTERNET_NOTIFICATION.equals(intent.getAction())) {
                    HwWifiproLiteStateMachine.this.sendMessage(103);
                } else if (WifiProCommonDefs.ACTION_NETWOR_PROPERTY_NOTIFICATION.equals(intent.getAction())) {
                    HwWifiproLiteStateMachine.this.sendMessage(104, intent.getIntExtra(WifiProCommonDefs.EXTRA_FLAG_NETWORK_PROPERTY, -1), 0);
                } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())) {
                    if (intent.getIntExtra("wifi_state", 4) == 1) {
                        HwWifiproLiteStateMachine.this.sendMessage(111);
                    }
                } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                    int networkType = intent.getIntExtra("networkType", 1);
                    NetworkInfo mobileInfo = HwWifiproLiteStateMachine.this.mConnectivityManager.getNetworkInfo(0);
                    if (networkType == 0 && mobileInfo != null && NetworkInfo.DetailedState.CONNECTED == mobileInfo.getDetailedState()) {
                        HwWifiproLiteStateMachine.this.sendMessage(112);
                    }
                }
            }
        }, intentFilter);
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            return true;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "==> ##DisconnectedState", new Object[0]);
            String unused = HwWifiproLiteStateMachine.this.mCurrentSsid = null;
            WifiConfiguration unused2 = HwWifiproLiteStateMachine.this.mCurrentWifiConfig = null;
        }

        public boolean processMessage(Message message) {
            if (message.what != 101) {
                return false;
            }
            HwWifiproLiteStateMachine hwWifiproLiteStateMachine = HwWifiproLiteStateMachine.this;
            hwWifiproLiteStateMachine.transitionTo(hwWifiproLiteStateMachine.mConnectedState);
            return true;
        }
    }

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
            String str;
            HwWifiproLiteStateMachine hwWifiproLiteStateMachine = HwWifiproLiteStateMachine.this;
            WifiConfiguration unused = hwWifiproLiteStateMachine.mCurrentWifiConfig = WifiProCommonUtils.getCurrentWifiConfig(hwWifiproLiteStateMachine.mWifiManager);
            HwWifiproLiteStateMachine hwWifiproLiteStateMachine2 = HwWifiproLiteStateMachine.this;
            String unused2 = hwWifiproLiteStateMachine2.mCurrentSsid = WifiProCommonUtils.getCurrentSsid(hwWifiproLiteStateMachine2.mWifiManager);
            Object[] objArr = new Object[1];
            if (HwWifiproLiteStateMachine.this.mCurrentWifiConfig != null) {
                str = HwWifiproLiteStateMachine.this.mCurrentWifiConfig.configKey();
            } else {
                str = StringUtilEx.safeDisplaySsid(HwWifiproLiteStateMachine.this.mCurrentSsid);
            }
            objArr[0] = str;
            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "==> ##ConnectedState, network = %{private}s", objArr);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 111) {
                switch (i) {
                    case 102:
                        HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "##CMD_NETWORK_DISCONNECTED_RCVD", new Object[0]);
                        HwWifiproLiteStateMachine hwWifiproLiteStateMachine = HwWifiproLiteStateMachine.this;
                        hwWifiproLiteStateMachine.transitionTo(hwWifiproLiteStateMachine.mDisconnectedState);
                        break;
                    case 103:
                        if (HwWifiproLiteStateMachine.this.getCurrentState() == HwWifiproLiteStateMachine.this.mConnectedState) {
                            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "##CMD_HTTP_UNREACHABLE_BY_FIRST_DETECT", new Object[0]);
                            handleHttpUnreachableByFirstDetect();
                            break;
                        }
                        break;
                    case 104:
                        HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "##CMD_INTERNET_CAPABILITY_RCVD, internetCapability = %{public}d", new Object[]{Integer.valueOf(message.arg1)});
                        handleInternetCapabilityRcvd(message.arg1);
                        break;
                    default:
                        return false;
                }
            } else {
                HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "##CMD_WIFI_DISABLED_RCVD", new Object[0]);
                HwWifiproLiteStateMachine hwWifiproLiteStateMachine2 = HwWifiproLiteStateMachine.this;
                hwWifiproLiteStateMachine2.transitionTo(hwWifiproLiteStateMachine2.mDisconnectedState);
            }
            return true;
        }

        private void handleHttpUnreachableByFirstDetect() {
            HwWifiproLiteStateMachine.this.updateInternetCapabilityUI(true, false);
        }

        private void handleInternetCapabilityRcvd(int internetCapability) {
            if (internetCapability == -1) {
                HwWifiproLiteStateMachine hwWifiproLiteStateMachine = HwWifiproLiteStateMachine.this;
                hwWifiproLiteStateMachine.transitionTo(hwWifiproLiteStateMachine.mNoInternetMonitorState);
                HwWifiproLiteStateMachine.this.sendMessageDelayed(HwWifiproLiteStateMachine.this.obtainMessage(110, -102, 0), 100);
            } else if (internetCapability == 6) {
                HwWifiproLiteStateMachine hwWifiproLiteStateMachine2 = HwWifiproLiteStateMachine.this;
                hwWifiproLiteStateMachine2.transitionTo(hwWifiproLiteStateMachine2.mNoInternetMonitorState);
                HwWifiproLiteStateMachine.this.sendMessageDelayed(HwWifiproLiteStateMachine.this.obtainMessage(110, -101, 0), 100);
            } else if (internetCapability == 5) {
                HwWifiproLiteStateMachine hwWifiproLiteStateMachine3 = HwWifiproLiteStateMachine.this;
                hwWifiproLiteStateMachine3.transitionTo(hwWifiproLiteStateMachine3.mHasInternetMonitorState);
            } else {
                HwHiLog.w(HwWifiproLiteStateMachine.TAG, false, "handleInternetCapabilityRcvd, unknown internetCapability = %{public}d", new Object[]{Integer.valueOf(internetCapability)});
            }
        }
    }

    class HasInternetMonitorState extends State {
        private static final int INTERNET_STATUS_DETECT_INTERVAL_MS = 8000;
        private int httpGetReqSessionId = -1;
        private int mInternetFailedCounter = 0;
        private boolean mInternetSelfCureAllowed = true;
        private int mLastDnsFailCounter = 0;
        private int mLastTcpRxCounter = 0;
        private int mLastTcpTxCounter = 0;
        private boolean mMobileHotspot = false;

        HasInternetMonitorState() {
        }

        public void enter() {
            this.mInternetFailedCounter = 0;
            this.mLastDnsFailCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
            this.mLastTcpTxCounter = 0;
            this.mLastTcpRxCounter = 0;
            this.httpGetReqSessionId = -1;
            this.mInternetSelfCureAllowed = true;
            this.mMobileHotspot = HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(HwWifiproLiteStateMachine.this.mContext);
            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "==> ##HasInternetMonitorState, currentSsid = %{public}s, mobileHotspot = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(HwWifiproLiteStateMachine.this.mCurrentSsid), String.valueOf(this.mMobileHotspot)});
            HwSelfCureEngine.getInstance().notifyInternetAccessRecovery();
            HwWifiproLiteStateMachine.this.updateInternetCapabilityUI(false, false);
            HwWifiproLiteStateMachine.this.sendMessageDelayed(107, 8000);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                if (parseNetworkInternetGood(message)) {
                    this.mInternetFailedCounter = 0;
                    this.mInternetSelfCureAllowed = true;
                } else {
                    this.mInternetFailedCounter++;
                    if (this.mInternetFailedCounter >= 2 && WifiProCommonUtils.isWifiConnected(HwWifiproLiteStateMachine.this.mWifiManager)) {
                        WifiInfo wifiInfo = HwWifiproLiteStateMachine.this.mWifiManager.getConnectionInfo();
                        int currentRssi = wifiInfo.getRssi();
                        HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "internet access abnormal, currentRssi = %{public}d, internetFailedCounter = %{public}d, mobileHotspot = %{public}s", new Object[]{Integer.valueOf(currentRssi), Integer.valueOf(this.mInternetFailedCounter), String.valueOf(this.mMobileHotspot)});
                        if (allowSelfCureNetwork(currentRssi)) {
                            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "notify SCE(Self Cure Engine) to handle it, internetFailedCounter = %{public}d", new Object[]{Integer.valueOf(this.mInternetFailedCounter)});
                            HwSelfCureEngine.getInstance().notifyInternetFailureDetected(false);
                            this.mInternetSelfCureAllowed = false;
                        } else if (WifiProCommonUtils.getCurrenSignalLevel(wifiInfo) >= 2) {
                            this.httpGetReqSessionId = HwWifiproLiteStateMachine.this.asynDoHttpGetOneTime();
                        } else {
                            handleNoInternetAccessMidway();
                        }
                    }
                }
                HwWifiproLiteStateMachine.this.sendMessageDelayed(107, 8000);
            } else if (i != 112) {
                switch (i) {
                    case 107:
                        if (!HwSelfCureEngine.getInstance().isSelfCureOngoing() && WifiProCommonUtils.isWifiConnected(HwWifiproLiteStateMachine.this.mWifiManager)) {
                            HwWifiproLiteStateMachine.this.mIpQosMonitor.queryPackets(0);
                            break;
                        } else {
                            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "SelfCureOngoing or supp state isn't completed, internetFailedCounter = %{public}d", new Object[]{Integer.valueOf(this.mInternetFailedCounter)});
                            HwWifiproLiteStateMachine.this.sendMessageDelayed(107, 8000);
                            break;
                        }
                        break;
                    case 108:
                        HwWifiproLiteStateMachine.this.removeMessages(107);
                        if (!(message.obj instanceof Boolean)) {
                            HwHiLog.w(HwWifiproLiteStateMachine.TAG, false, "class not match Boolean", new Object[0]);
                            break;
                        } else {
                            boolean httpReachable = ((Boolean) message.obj).booleanValue();
                            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "#CMD_SCE_NOTIFY_HTTP_GET_RESULT, httpReachable = %{public}s", new Object[]{String.valueOf(httpReachable)});
                            if (!httpReachable) {
                                handleNoInternetAccessMidway();
                                break;
                            } else {
                                resetByInternetRechable();
                                HwWifiproLiteStateMachine.this.sendMessageDelayed(107, 8000);
                                break;
                            }
                        }
                    case 109:
                        HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "#CMD_HTTP_GET_RESULT_RCVD, respCode = %{public}d", new Object[]{Integer.valueOf(message.arg2)});
                        if (this.httpGetReqSessionId == message.arg1) {
                            if (message.arg2 != 204 && !WifiProCommonUtils.isRedirectedRespCodeByGoogle(message.arg2)) {
                                handleNoInternetAccessMidway();
                                break;
                            } else {
                                resetByInternetRechable();
                                HwWifiproLiteStateMachine.this.sendMessageDelayed(107, 8000);
                                break;
                            }
                        } else {
                            HwHiLog.w(HwWifiproLiteStateMachine.TAG, false, "#CMD_HTTP_GET_RESULT_RCVD, httpGetReqSessionId unmatched!", new Object[0]);
                            break;
                        }
                        break;
                    default:
                        return false;
                }
            } else {
                WifiProUIDisplayManager access$1300 = HwWifiproLiteStateMachine.this.mWifiProUIDisplayManager;
                WifiProUIDisplayManager unused = HwWifiproLiteStateMachine.this.mWifiProUIDisplayManager;
                access$1300.showWifiProToast(1);
            }
            return true;
        }

        public void exit() {
            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "exit ##HasInternetMonitorState", new Object[0]);
            HwWifiproLiteStateMachine.this.removeMessages(107);
        }

        private void resetByInternetRechable() {
            this.mLastTcpTxCounter = 0;
            this.mLastTcpRxCounter = 0;
            this.httpGetReqSessionId = -1;
            this.mInternetFailedCounter = 0;
            this.mInternetSelfCureAllowed = true;
        }

        private boolean allowSelfCureNetwork(int currentRssi) {
            if (this.mMobileHotspot || !this.mInternetSelfCureAllowed || currentRssi < -70 || HwSelfCureEngine.getInstance().isSelfCureOngoing()) {
                return false;
            }
            return true;
        }

        private void handleNoInternetAccessMidway() {
            Bundle data = new Bundle();
            data.putInt("messageWhat", HwWifiproLiteStateMachine.INVALID_LINK_DETECTED);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
            HwWifiproLiteStateMachine hwWifiproLiteStateMachine = HwWifiproLiteStateMachine.this;
            hwWifiproLiteStateMachine.transitionTo(hwWifiproLiteStateMachine.mNoInternetMonitorState);
            HwWifiproLiteStateMachine.this.sendMessageDelayed(HwWifiproLiteStateMachine.this.obtainMessage(110, -102, 0), 100);
        }

        private boolean parseNetworkInternetGood(Message message) {
            int i;
            boolean queryResp = message.arg1 == 0;
            int packetsLength = message.arg2;
            if (queryResp && packetsLength > 7 && (message.obj instanceof int[])) {
                int[] packets = (int[]) message.obj;
                int tcpTxPkts = packets[6];
                int tcpRxPkts = packets[7];
                int i2 = this.mLastTcpTxCounter;
                if (i2 == 0 || (i = this.mLastTcpRxCounter) == 0) {
                    this.mLastTcpTxCounter = tcpTxPkts;
                    this.mLastTcpRxCounter = tcpRxPkts;
                    this.mLastDnsFailCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                    return true;
                }
                int deltaTcpTxPkts = tcpTxPkts - i2;
                int deltaTcpRxPkts = tcpRxPkts - i;
                this.mLastTcpTxCounter = tcpTxPkts;
                this.mLastTcpRxCounter = tcpRxPkts;
                if (deltaTcpRxPkts == 0) {
                    if (deltaTcpTxPkts >= 3) {
                        this.mLastDnsFailCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                        return false;
                    }
                    int currentDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                    int deltaFailedDns = currentDnsFailedCounter - this.mLastDnsFailCounter;
                    this.mLastDnsFailCounter = currentDnsFailedCounter;
                    if (deltaFailedDns >= 2) {
                        return false;
                    }
                }
            }
            this.mLastDnsFailCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
            return true;
        }
    }

    class NoInternetMonitorState extends State {
        private static final int NO_INTERNET_DETECT_INTERVAL_MS = 5000;
        private static final int PORTAL_DETECT_INTERVAL_MS = 15000;
        private int httpGetReqSessionId = -1;
        private int mTcpRxPacketsCounter = 0;
        private int noInternetReason = -1;

        NoInternetMonitorState() {
        }

        public void enter() {
            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "==> ##NoInternetMonitorState, currentSsid = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(HwWifiproLiteStateMachine.this.mCurrentSsid)});
            this.httpGetReqSessionId = -1;
            this.noInternetReason = -1;
            this.mTcpRxPacketsCounter = 0;
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                int rx = parseTcpRxPacketsCounter(message);
                if (rx > 0) {
                    int i2 = this.mTcpRxPacketsCounter;
                    if (i2 == 0 || rx - i2 < 5) {
                        this.mTcpRxPacketsCounter = rx;
                    } else {
                        this.mTcpRxPacketsCounter = rx;
                        this.httpGetReqSessionId = HwWifiproLiteStateMachine.this.asynDoHttpGetOneTime();
                    }
                }
                HwWifiproLiteStateMachine.this.sendMessageDelayed(106, 5000);
            } else if (i != 105) {
                boolean z = false;
                if (i != 106) {
                    if (i == 109) {
                        HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "#CMD_HTTP_GET_RESULT_RCVD, respCode = %{public}d", new Object[]{Integer.valueOf(message.arg2)});
                        if (this.httpGetReqSessionId != message.arg1) {
                            HwHiLog.w(HwWifiproLiteStateMachine.TAG, false, "#CMD_HTTP_GET_RESULT_RCVD, httpGetReqSessionId unmatched!", new Object[0]);
                        } else if (message.arg2 == 204) {
                            if (this.noInternetReason == -101) {
                                notifyPortalHasInternetAccess();
                            }
                            HwWifiproLiteStateMachine hwWifiproLiteStateMachine = HwWifiproLiteStateMachine.this;
                            hwWifiproLiteStateMachine.transitionTo(hwWifiproLiteStateMachine.mHasInternetMonitorState);
                        } else {
                            int i3 = this.noInternetReason;
                            if (i3 == -102) {
                                HwWifiproLiteStateMachine.this.sendMessageDelayed(106, 5000);
                            } else if (i3 == -101) {
                                HwWifiproLiteStateMachine.this.sendMessageDelayed(105, 15000);
                            }
                        }
                    } else if (i != 110) {
                        return false;
                    } else {
                        this.noInternetReason = message.arg1;
                        Object[] objArr = new Object[1];
                        objArr[0] = this.noInternetReason == -101 ? "portal unlogin" : "http unreachable";
                        HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "#CMD_NOTIFY_NO_INTERNET_REASON, reason = %{public}s", objArr);
                        int i4 = this.noInternetReason;
                        if (i4 == -102) {
                            HwSelfCureEngine.getInstance().notifyInternetFailureDetected(true);
                            HwWifiproLiteStateMachine.this.sendMessageDelayed(106, 5000);
                        } else if (i4 == -101) {
                            HwWifiproLiteStateMachine.this.sendMessageDelayed(105, 15000);
                        }
                        HwWifiproLiteStateMachine hwWifiproLiteStateMachine2 = HwWifiproLiteStateMachine.this;
                        if (this.noInternetReason == -101) {
                            z = true;
                        }
                        hwWifiproLiteStateMachine2.updateInternetCapabilityUI(true, z);
                    }
                } else if (!HwWifiproLiteStateMachine.this.mPowerManager.isScreenOn()) {
                    HwWifiproLiteStateMachine.this.sendMessageDelayed(106, 5000);
                } else {
                    HwWifiproLiteStateMachine.this.mIpQosMonitor.queryPackets(0);
                }
            } else if (!HwWifiproLiteStateMachine.this.mPowerManager.isScreenOn()) {
                HwWifiproLiteStateMachine.this.sendMessageDelayed(106, 5000);
            } else {
                this.httpGetReqSessionId = HwWifiproLiteStateMachine.this.asynDoHttpGetOneTime();
            }
            return true;
        }

        public void exit() {
            HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "exit ##NoInternetMonitorState", new Object[0]);
            HwWifiproLiteStateMachine.this.removeMessages(106);
            HwWifiproLiteStateMachine.this.removeMessages(105);
        }

        private int parseTcpRxPacketsCounter(Message message) {
            boolean queryResp = message.arg1 == 0;
            int packetsLength = message.arg2;
            if (!queryResp || packetsLength <= 7 || !(message.obj instanceof int[])) {
                return 0;
            }
            return ((int[]) message.obj)[7];
        }

        private void notifyPortalHasInternetAccess() {
            if (isProvisioned(HwWifiproLiteStateMachine.this.mContext)) {
                HwHiLog.d(HwWifiproLiteStateMachine.TAG, false, "portal has internet access, force network re-evaluation", new Object[0]);
                ConnectivityManager connMgr = ConnectivityManager.from(HwWifiproLiteStateMachine.this.mContext);
                if (connMgr == null) {
                    HwHiLog.e(HwWifiproLiteStateMachine.TAG, false, "notifyPortalHasInternetAccess connMgr is null", new Object[0]);
                    return;
                }
                Network[] info = connMgr.getAllNetworks();
                int length = info.length;
                int i = 0;
                while (i < length) {
                    Network nw = info[i];
                    NetworkCapabilities nc = connMgr.getNetworkCapabilities(nw);
                    if (!nc.hasTransport(1) || !nc.hasCapability(12)) {
                        i++;
                    } else {
                        connMgr.reportNetworkConnectivity(nw, false);
                        return;
                    }
                }
            }
        }

        private boolean isProvisioned(Context context) {
            return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1;
        }
    }

    /* access modifiers changed from: private */
    public synchronized void updateInternetCapabilityUI(boolean noInternetAccess, boolean noInternetByPortal) {
        if (this.mCurrentWifiConfig == null) {
            HwHiLog.e(TAG, false, "updateInternetCapabilityUI, but configuration is null.", new Object[0]);
            return;
        }
        this.mWifiProUIDisplayManager.notificateNetAccessChange(noInternetAccess);
        boolean z = true;
        if (!noInternetAccess) {
            this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, false, 0, false);
            this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, 4, this.mCurrentSsid);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 4);
        } else if (noInternetByPortal) {
            this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, noInternetAccess, 1, false);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 3);
        } else {
            this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, noInternetAccess, 0, false);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 2);
        }
        if (noInternetAccess) {
            z = false;
        }
        sendResutlToPowerSaveGenie(z, noInternetByPortal);
    }

    private synchronized void sendResutlToPowerSaveGenie(boolean hasInternetAccess, boolean portalConnected) {
        Intent intent = new Intent("com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_INTERNET_STATUS");
        intent.setFlags(67108864);
        intent.putExtra("internet_status", hasInternetAccess);
        intent.putExtra("network_connected_status_portal", portalConnected);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public synchronized int asynDoHttpGetOneTime() {
        int sessionId;
        sessionId = new SecureRandom().nextInt(100000);
        new NetworkCheckThread(this.mContext, this, sessionId).start();
        return sessionId;
    }

    public synchronized void notifyHttpReachable(boolean isReachable) {
        sendMessage(108, Boolean.valueOf(isReachable));
    }

    public void notifyHttpRedirectedForWifiPro() {
        sendMessage(104, 6);
    }

    private class NetworkCheckThread extends Thread {
        private Context mContext;
        private int mSessionId = -1;
        private HwWifiproLiteStateMachine mStateMachine;

        public NetworkCheckThread(Context context, HwWifiproLiteStateMachine stateMachine, int sessionId) {
            this.mContext = context;
            this.mStateMachine = stateMachine;
            this.mSessionId = sessionId;
        }

        public void run() {
            HwNetworkPropertyChecker checker = new HwNetworkPropertyChecker(this.mContext, null, null, true, null, false);
            int respCode = checker.isCaptivePortal(true);
            checker.release();
            this.mStateMachine.sendMessage(109, this.mSessionId, respCode);
        }
    }
}
