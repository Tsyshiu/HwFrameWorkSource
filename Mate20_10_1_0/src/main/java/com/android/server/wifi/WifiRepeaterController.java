package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.NetworkUtils;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.server.wifi.hwUtil.HwApConfigUtilEx;
import java.net.Inet4Address;
import java.util.Calendar;
import java.util.Random;

public class WifiRepeaterController extends WifiRepeater {
    private static final int BAND_2G = 0;
    private static final int BAND_5G = 1;
    private static final int BAND_ERROR = -1;
    private static final int CHANNEL_ERROR = -1;
    private static final int CMD_DOWNSTREAM_NETWORK_TETHERED = 4;
    private static final int CMD_DOWNSTREAM_NETWORK_UNTETHERED = 3;
    private static final int CMD_STOP_TETHERING = 0;
    private static final int CMD_UPSTREAM_NETWORK_CONNECT = 2;
    private static final int CMD_UPSTREAM_NETWORK_DISCONNECT = 1;
    private static final String EXTRA_WIFI_REPEATER_CLIENTS_SIZE = "wifi_repeater_clients_size";
    private static final int FREQUENCY_INVALID = -1;
    private static final long HANG_TIMEOUT = 30000;
    private static final String HW_SYSTEM_PERMISSION = "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM";
    private static final int RPT_GATEWAY_MASK = 16777215;
    private static final int RPT_INVALID_INETADDR = 0;
    private static final String WIFI_REPEATER_CLIENTS_CHANGED_ACTION = "com.huawei.wifi.action.WIFI_REPEATER_CLIENTS_CHANGED";
    private static final int WIFI_REPEATER_CLOSE = 0;
    private static final int WIFI_REPEATER_OPEN = 1;
    private static final String WIFI_REPEATER_STATE_CHANGED_ACTION = "com.huawei.wifi.action.WIFI_REPEATER_STATE_CHANGED";
    private static boolean mIsWifiConnected = false;
    private Context mContext;
    private State mDefaultState = new DefaultState();
    private int mDownstreamBand = -1;
    private WifiP2pGroup mDownstreamInfo;
    /* access modifiers changed from: private */
    public State mHangState = new HangState();
    private HwWifiCHRService mHwWifiCHRService;
    private final Object mLock = new Object();
    /* access modifiers changed from: private */
    public boolean mShouldRestart = false;
    /* access modifiers changed from: private */
    public State mTetheredState = new TetheredState();
    /* access modifiers changed from: private */
    public State mUntetheredState = new UntetheredState();
    private WifiConfiguration mUpstreamConfig;
    private WifiInfo mUpstreamInfo;
    private AsyncChannel mWifiP2pChannel = new AsyncChannel();

    public WifiRepeaterController(Context context, Messenger messenger) {
        this.mContext = context;
        this.mWifiP2pChannel.connectSync(this.mContext, getHandler(), messenger);
        initStateMachine();
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
    }

    public void handleP2pUntethered() {
        HwHiLog.d("WifiRepeater", false, "handleP2pUntethered", new Object[0]);
        this.mDownstreamInfo = null;
        sendMessage(3);
    }

    public void handleP2pTethered(WifiP2pGroup group) {
        HwHiLog.d("WifiRepeater", false, "%{public}s", new Object[]{"handleP2pTethered: " + group});
        this.mDownstreamInfo = group;
        sendMessage(4);
    }

    public void handleWifiDisconnect() {
        HwHiLog.d("WifiRepeater", false, "handleWifiDisconnect", new Object[0]);
        synchronized (this.mLock) {
            this.mUpstreamInfo = null;
        }
        mIsWifiConnected = false;
        sendMessage(1);
    }

    public void handleWifiConnect(WifiInfo wifiInfo, WifiConfiguration wifiConfig) {
        HwHiLog.d("WifiRepeater", false, "handleWifiConnect.", new Object[0]);
        synchronized (this.mLock) {
            this.mUpstreamInfo = wifiInfo;
        }
        this.mUpstreamConfig = wifiConfig;
        mIsWifiConnected = true;
        sendMessage(2);
    }

    public void handleClientListChanged(WifiP2pGroup group) {
        HwHiLog.d("WifiRepeater", false, "handleClientListChanged: size=%{public}d", new Object[]{Integer.valueOf(group.getClientList().size())});
        Intent intent = new Intent(WIFI_REPEATER_CLIENTS_CHANGED_ACTION);
        intent.putExtra(EXTRA_WIFI_REPEATER_CLIENTS_SIZE, group.getClientList().size());
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendStateChangedBroadcast() {
        HwHiLog.d("WifiRepeater", false, "sendStateChangedBroadcast", new Object[0]);
        this.mContext.sendStickyBroadcastAsUser(new Intent(WIFI_REPEATER_STATE_CHANGED_ACTION), UserHandle.ALL);
    }

    public int retrieveDownstreamChannel() {
        int result;
        int upstreamFrequency = getUpstreamFrequency();
        if (upstreamFrequency == -1) {
            HwHiLog.e("WifiRepeater", false, "retrieveDownstreamChannel: mUpstreamInfo == null;", new Object[0]);
            return -1;
        }
        int upstreamChannel = convertFreqToChannel(upstreamFrequency);
        int upstreamBand = convertFreqToBand(upstreamFrequency);
        if (-1 == upstreamChannel || -1 == upstreamBand) {
            HwHiLog.e("WifiRepeater", false, "retrieveDownstreamChannel: upstreamChannel == CHANNEL_ERROR", new Object[0]);
            return -1;
        }
        if (1 != upstreamBand) {
            result = upstreamChannel;
            this.mDownstreamBand = upstreamBand;
        } else if (isSupportRsdb()) {
            result = getRandom2GChannel();
            this.mDownstreamBand = 0;
        } else if (isDfsChannel(upstreamFrequency) || isIndoorChannel(upstreamFrequency)) {
            int result2 = HwApConfigUtilEx.getSelected5GChannel(SoftApChannelXmlParse.convertChannelListToFrequency(HwSoftApManager.getChannelListFor5GWithoutIndoor()));
            if (result2 == -1) {
                result = getRandom2GChannel();
                this.mDownstreamBand = 0;
            } else {
                this.mDownstreamBand = 1;
                result = result2;
            }
        } else {
            result = upstreamChannel;
            this.mDownstreamBand = upstreamBand;
        }
        if (this.mHwWifiCHRService != null) {
            Bundle data = new Bundle();
            data.putInt("repeaterBand", this.mDownstreamBand);
            data.putInt("repeaterChannel", result);
            this.mHwWifiCHRService.uploadDFTEvent(13, data);
        }
        HwHiLog.d("WifiRepeater", false, "retrieveDownstreamChannel: %{public}d", new Object[]{Integer.valueOf(result)});
        return result;
    }

    public int retrieveDownstreamBand() {
        int upstreamFrequency = getUpstreamFrequency();
        if (upstreamFrequency == -1) {
            HwHiLog.e("WifiRepeater", false, "retrieveDownstreamBand: mUpstreamInfo == null;", new Object[0]);
            return -1;
        } else if (-1 == convertFreqToBand(upstreamFrequency)) {
            HwHiLog.e("WifiRepeater", false, "retrieveDownstreamBand: upstreamBand == BAND_ERROR", new Object[0]);
            return -1;
        } else {
            int result = this.mDownstreamBand;
            HwHiLog.d("WifiRepeater", false, "retrieveDownstreamBand: %{public}d", new Object[]{Integer.valueOf(result)});
            return result;
        }
    }

    public boolean isEncryptionTypeTetheringAllowed() {
        WifiConfiguration wifiConfiguration = this.mUpstreamConfig;
        if (wifiConfiguration == null) {
            HwHiLog.e("WifiRepeater", false, "isEncryptionTypeTetheringAllowed: mUpstreamConfig==null", new Object[0]);
            return false;
        } else if (wifiConfiguration.enterpriseConfig == null) {
            HwHiLog.d("WifiRepeater", false, "isEncryptionTypeTetheringAllowed: enterpriseConfig is null, return true.", new Object[0]);
            return true;
        } else {
            if (this.mUpstreamConfig.enterpriseConfig != null) {
                int eapMethod = this.mUpstreamConfig.enterpriseConfig.getEapMethod();
                HwHiLog.d("WifiRepeater", false, "isEncryptionTypeTetheringAllowed: eapMethod=%{public}d", new Object[]{Integer.valueOf(eapMethod)});
                if (1 == eapMethod || 2 == eapMethod) {
                    return false;
                }
            }
            return true;
        }
    }

    private void initStateMachine() {
        HwHiLog.d("WifiRepeater", false, "initStateMachine.", new Object[0]);
        addState(this.mUntetheredState, this.mDefaultState);
        addState(this.mTetheredState, this.mDefaultState);
        addState(this.mHangState, this.mDefaultState);
        setInitialState(this.mUntetheredState);
        start();
    }

    /* access modifiers changed from: private */
    public void restartTethering() {
        HwHiLog.d("WifiRepeater", false, "restartTethering", new Object[0]);
        if (isEncryptionTypeTetheringAllowed()) {
            this.mWifiP2pChannel.sendMessage(141268);
        }
    }

    /* access modifiers changed from: private */
    public void stopTethering() {
        HwHiLog.d("WifiRepeater", false, "stopTethering", new Object[0]);
        this.mWifiP2pChannel.sendMessage((int) HwWifiStateMachine.CMD_STOP_WIFI_REPEATER);
    }

    /* access modifiers changed from: private */
    public boolean pauseDownstream() {
        try {
            getNwService().setIpForwardingEnabled(false);
            HwHiLog.d("WifiRepeater", false, "pauseDownstream: success.", new Object[0]);
            return true;
        } catch (IllegalStateException e) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream exception: %{public}s", new Object[]{e.getMessage()});
            return false;
        } catch (IllegalArgumentException e2) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream exception: %{public}s", new Object[]{e2.getMessage()});
            return false;
        } catch (RemoteException e3) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream exception: %{public}s", new Object[]{e3.getMessage()});
            return false;
        } catch (Exception e4) {
            HwHiLog.e("WifiRepeater", false, "pauseDownstream fail", new Object[0]);
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean resumeDownstream() {
        if (!isEncryptionTypeTetheringAllowed()) {
            return false;
        }
        try {
            getNwService().setIpForwardingEnabled(true);
            HwHiLog.d("WifiRepeater", false, "resumeDownstream: success.", new Object[0]);
            return true;
        } catch (IllegalStateException e) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream exception: %{public}s", new Object[]{e.getMessage()});
            return false;
        } catch (IllegalArgumentException e2) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream exception: %{public}s", new Object[]{e2.getMessage()});
            return false;
        } catch (RemoteException e3) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream exception: %{public}s", new Object[]{e3.getMessage()});
            return false;
        } catch (Exception e4) {
            HwHiLog.e("WifiRepeater", false, "resumeDownstream fail", new Object[0]);
            return false;
        }
    }

    private INetworkManagementService getNwService() {
        return INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    /* access modifiers changed from: private */
    public boolean isFrequencyCollision() {
        int upFreq = getUpstreamFrequency();
        if (upFreq == -1) {
            HwHiLog.e("WifiRepeater", false, "isFrequencyCollision: mUpstreamInfo==null", new Object[0]);
            return true;
        }
        WifiP2pGroup wifiP2pGroup = this.mDownstreamInfo;
        if (wifiP2pGroup == null) {
            HwHiLog.e("WifiRepeater", false, "isFrequencyCollision: mDownstreamInfo==null", new Object[0]);
            return true;
        }
        int downFreq = wifiP2pGroup.getFrequency();
        int upBand = convertFreqToBand(upFreq);
        int downBand = convertFreqToBand(downFreq);
        HwHiLog.d("WifiRepeater", false, "isFrequencyCollision: upFreq=%{public}d downFreq=%{public}d upBand=%{public}d downBand=%{public}d", new Object[]{Integer.valueOf(upFreq), Integer.valueOf(downFreq), Integer.valueOf(upBand), Integer.valueOf(downBand)});
        return isSupportRsdb() ? upBand == downBand && upFreq != downFreq : upFreq != downFreq;
    }

    /* access modifiers changed from: private */
    public boolean isGatewayCollision() {
        DhcpInfo dhcpInfo;
        int upstreamGateway = 0;
        int downstreamGateway = 0;
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (!(wifiManager == null || (dhcpInfo = wifiManager.getDhcpInfo()) == null)) {
            upstreamGateway = dhcpInfo.gateway & RPT_GATEWAY_MASK;
        }
        WifiP2pGroup wifiP2pGroup = this.mDownstreamInfo;
        if (wifiP2pGroup != null) {
            String addr = wifiP2pGroup.getP2pServerAddress();
            if (!TextUtils.isEmpty(addr)) {
                downstreamGateway = NetworkUtils.inetAddressToInt((Inet4Address) NetworkUtils.numericToInetAddress(addr)) & RPT_GATEWAY_MASK;
            }
        }
        HwHiLog.d("WifiRepeater", false, "gateway check: upstream=%{private}d downstream=%{private}d", new Object[]{Integer.valueOf(upstreamGateway), Integer.valueOf(downstreamGateway)});
        return upstreamGateway == downstreamGateway;
    }

    private boolean isSupportRsdb() {
        boolean ret = WifiInjector.getInstance().getWifiNative().mHwWifiNativeEx.isSupportRsdbByDriver();
        HwHiLog.d("WifiRepeater", false, "isSupportRsdb: %{public}s", new Object[]{String.valueOf(ret)});
        return ret;
    }

    private boolean isDfsChannel(int frequency) {
        boolean ret = WifiInjector.getInstance().getWifiNative().mHwWifiNativeEx.isDfsChannel(frequency);
        HwHiLog.d("WifiRepeater", false, "isDfsChannel: %{public}s", new Object[]{String.valueOf(ret)});
        return ret;
    }

    private boolean isIndoorChannel(int frequency) {
        boolean ret = false;
        ClientModeImpl wsm = WifiInjector.getInstance().getClientModeImpl();
        if (wsm instanceof HwWifiStateMachine) {
            ret = ((HwWifiStateMachine) wsm).getSoftApChannelXmlParse().isIndoorChannel(frequency, WifiInjector.getInstance().getWifiCountryCode().getCountryCodeSentToDriver());
        }
        HwHiLog.d("WifiRepeater", false, "isIndoorChannel: %{public}s", new Object[]{String.valueOf(ret)});
        return ret;
    }

    /* access modifiers changed from: private */
    public void sendWifiRepeaterStateBroadcast(int state) {
        HwHiLog.d("WifiRepeater", false, "sendWifiRepeaterStateBroadcast state=%{public}d", new Object[]{Integer.valueOf(state)});
        Intent intent = new Intent("com.huawei.android.net.wifi.p2p.action.WIFI_RPT_STATE_CHANGED");
        intent.putExtra("wifi_rpt_state", state);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, HW_SYSTEM_PERMISSION);
    }

    public static boolean isWifiConnected() {
        return mIsWifiConnected;
    }

    /* access modifiers changed from: private */
    public void persistStatus(int status) {
        HwHiLog.d("WifiRepeater", false, "persistStatus: %{public}d", new Object[]{Integer.valueOf(status)});
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_repeater_on", status);
    }

    private int getRandom2GChannel() {
        int[] channel2G = {1, 6, 10};
        int result = channel2G[new Random(Calendar.getInstance().getTimeInMillis()).nextInt(channel2G.length)];
        HwHiLog.d("WifiRepeater", false, "getRandom2GChannel: %{public}d", new Object[]{Integer.valueOf(result)});
        return result;
    }

    private int convertFreqToChannel(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return ((frequency - 2412) / 5) + 1;
        }
        if (frequency >= 5170 && frequency <= 5825) {
            return ((frequency - 5170) / 5) + 34;
        }
        HwHiLog.e("WifiRepeater", false, "convertFreqToChannel: CHANNEL_ERROR", new Object[0]);
        return -1;
    }

    private int convertFreqToBand(int frequency) {
        if (frequency > 2400 && frequency < 2500) {
            return 0;
        }
        if (frequency > 4900 && frequency < 5900) {
            return 1;
        }
        HwHiLog.e("WifiRepeater", false, "convertFreqToBand: BAND_ERROR", new Object[0]);
        return -1;
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(State state, Message message) {
        String str;
        int i = message.what;
        if (i == 0) {
            str = "CMD_STOP_TETHERING";
        } else if (i == 1) {
            str = "CMD_UPSTREAM_NETWORK_DISCONNECT";
        } else if (i == 2) {
            str = "CMD_UPSTREAM_NETWORK_CONNECT";
        } else if (i == 3) {
            str = "CMD_DOWNSTREAM_NETWORK_UNTETHERED";
        } else if (i != 4) {
            str = "what:" + Integer.toString(message.what);
        } else {
            str = "CMD_DOWNSTREAM_NETWORK_TETHERED";
        }
        HwHiLog.d("WifiRepeater", false, "%{public}s: handle message: %{public}s", new Object[]{state.getClass().getSimpleName(), str});
    }

    private int getUpstreamFrequency() {
        synchronized (this.mLock) {
            if (this.mUpstreamInfo == null) {
                return -1;
            }
            return this.mUpstreamInfo.getFrequency();
        }
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message message) {
            WifiRepeaterController.this.logStateAndMessage(this, message);
            return true;
        }
    }

    private class UntetheredState extends State {
        private UntetheredState() {
        }

        public void enter() {
            HwHiLog.d("WifiRepeater", false, "UntetheredState enter.", new Object[0]);
            if (WifiRepeaterController.this.mShouldRestart) {
                boolean unused = WifiRepeaterController.this.mShouldRestart = false;
                WifiRepeaterController.this.restartTethering();
            }
        }

        public boolean processMessage(Message message) {
            WifiRepeaterController.this.logStateAndMessage(this, message);
            if (message.what != 4) {
                return false;
            }
            WifiRepeaterController wifiRepeaterController = WifiRepeaterController.this;
            wifiRepeaterController.transitionTo(wifiRepeaterController.mTetheredState);
            return true;
        }
    }

    private class TetheredState extends State {
        private TetheredState() {
        }

        public void enter() {
            HwHiLog.d("WifiRepeater", false, "TetheredState enter.", new Object[0]);
            boolean unused = WifiRepeaterController.this.mShouldRestart = false;
        }

        public boolean processMessage(Message message) {
            WifiRepeaterController.this.logStateAndMessage(this, message);
            int i = message.what;
            if (i == 1) {
                WifiRepeaterController wifiRepeaterController = WifiRepeaterController.this;
                wifiRepeaterController.transitionTo(wifiRepeaterController.mHangState);
            } else if (i != 3) {
                return false;
            } else {
                WifiRepeaterController wifiRepeaterController2 = WifiRepeaterController.this;
                wifiRepeaterController2.transitionTo(wifiRepeaterController2.mUntetheredState);
            }
            return true;
        }
    }

    private class HangState extends State {
        private HangState() {
        }

        public void enter() {
            HwHiLog.d("WifiRepeater", false, "HangState enter.", new Object[0]);
            WifiRepeaterController.this.sendMessageDelayed(0, WifiRepeaterController.HANG_TIMEOUT);
            WifiRepeaterController.this.persistStatus(0);
            WifiRepeaterController.this.sendStateChangedBroadcast();
            WifiRepeaterController.this.sendWifiRepeaterStateBroadcast(0);
            if (!WifiRepeaterController.this.pauseDownstream()) {
                WifiRepeaterController.this.removeMessages(0);
                WifiRepeaterController.this.stopTethering();
            }
        }

        public boolean processMessage(Message message) {
            WifiRepeaterController.this.logStateAndMessage(this, message);
            int i = message.what;
            if (i == 0) {
                WifiRepeaterController.this.stopTethering();
            } else if (i != 1) {
                if (i == 2) {
                    WifiRepeaterController.this.removeMessages(0);
                    if (WifiRepeaterController.this.isFrequencyCollision() || WifiRepeaterController.this.isGatewayCollision()) {
                        boolean unused = WifiRepeaterController.this.mShouldRestart = true;
                        WifiRepeaterController.this.stopTethering();
                    } else if (WifiRepeaterController.this.resumeDownstream()) {
                        WifiRepeaterController.this.persistStatus(1);
                        WifiRepeaterController.this.sendStateChangedBroadcast();
                        WifiRepeaterController.this.sendWifiRepeaterStateBroadcast(1);
                        WifiRepeaterController wifiRepeaterController = WifiRepeaterController.this;
                        wifiRepeaterController.transitionTo(wifiRepeaterController.mTetheredState);
                    } else {
                        WifiRepeaterController.this.stopTethering();
                    }
                } else if (i != 3) {
                    return false;
                } else {
                    WifiRepeaterController wifiRepeaterController2 = WifiRepeaterController.this;
                    wifiRepeaterController2.transitionTo(wifiRepeaterController2.mUntetheredState);
                }
            }
            return true;
        }
    }
}
