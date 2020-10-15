package com.android.server.wifi;

import android.content.Context;
import android.net.DhcpResults;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.StaticIpConfiguration;
import android.net.ip.IpClientManager;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiSsid;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.StateMachine;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class AbsWifiStateMachine extends StateMachine {
    public static final int NETWORK_STATUS_UNWANTED_PORTAL = 3;

    public String getWpaSuppConfig() {
        return "";
    }

    protected AbsWifiStateMachine(String name, Looper looper) {
        super(name, looper);
    }

    protected AbsWifiStateMachine(String name, Handler handler) {
        super(name, handler);
    }

    protected AbsWifiStateMachine(String name) {
        super(name);
    }

    /* access modifiers changed from: protected */
    public boolean processScanModeSetMode(Message message, int mLastOperationMode) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean processConnectModeSetMode(Message message) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean processL2ConnectedSetMode(Message message) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean processDisconnectedSetMode(Message message) {
        return false;
    }

    /* access modifiers changed from: protected */
    public void enterConnectedStateByMode() {
    }

    /* access modifiers changed from: protected */
    public boolean enterDriverStartedStateByMode() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void enableAllNetworksByMode() {
    }

    /* access modifiers changed from: protected */
    public void handleNetworkDisconnect() {
    }

    /* access modifiers changed from: protected */
    public void loadAndEnableAllNetworksByMode() {
    }

    public boolean isScanAndManualConnectMode() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean processConnectModeAutoConnectByMode() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void recordAssociationRejectStatusCode(int statusCode) {
    }

    /* access modifiers changed from: protected */
    public void startScreenOffScan() {
    }

    /* access modifiers changed from: protected */
    public boolean processScreenOffScan(Message message) {
        return false;
    }

    /* access modifiers changed from: protected */
    public void makeHwDefaultIPTable(DhcpResults dhcpResults) {
    }

    /* access modifiers changed from: protected */
    public boolean handleHwDefaultIPConfiguration() {
        return false;
    }

    public DhcpResults getCachedDhcpResultsForCurrentConfig() {
        return null;
    }

    /* access modifiers changed from: protected */
    public boolean hasMeteredHintForWi(Inet4Address ip) {
        return false;
    }

    /* access modifiers changed from: protected */
    public void processSetVoWifiDetectMode(Message msg) {
    }

    /* access modifiers changed from: protected */
    public void processSetVoWifiDetectPeriod(Message msg) {
    }

    /* access modifiers changed from: protected */
    public void processIsSupportVoWifiDetect(Message msg) {
    }

    /* access modifiers changed from: protected */
    public void processStatistics(int event) {
    }

    public int resetScoreByInetAccess(int score) {
        return score;
    }

    public boolean isWifiProEnabled() {
        return false;
    }

    public void startWifi2WifiRequest() {
    }

    public void getConfiguredNetworks(Message message) {
    }

    public void saveConnectingNetwork(WifiConfiguration config) {
    }

    public void reportPortalNetworkStatus() {
    }

    public boolean ignoreEnterConnectedState() {
        return false;
    }

    public void wifiNetworkExplicitlyUnselected() {
    }

    public void wifiNetworkExplicitlySelected() {
    }

    public void handleConnectedInWifiPro() {
    }

    public void handleDisconnectedInWifiPro() {
    }

    public void handleUnwantedNetworkInWifiPro(WifiConfiguration config, int unwantedType) {
    }

    public void handleValidNetworkInWifiPro(WifiConfiguration config) {
    }

    public void saveWpsNetIdInWifiPro(int netId) {
    }

    public void handleHandoverConnectFailed(int netId, int disableReason) {
    }

    public void handleRxGoodInWifiPro(String cmd, String value, RssiPacketCountInfo info) {
    }

    public void updateWifiproWifiConfiguration(Message message) {
    }

    public void setWiFiProScanResultList(List<ScanResult> list) {
    }

    public boolean isWifiProEvaluatingAP() {
        return false;
    }

    public void updateScanDetailByWifiPro(ScanDetail scanDetail) {
    }

    public void updateScanDetailByWifiPro(List<ScanDetail> list) {
    }

    public void tryUseStaticIpForFastConnecting(int lastNid) {
    }

    public void updateNetworkConcurrently() {
    }

    public void handleConnectFailedInWifiPro(int netId, int disableReason) {
    }

    public void notifyWifiConnectedBackgroundReady() {
    }

    public void resetWifiproEvaluateConfig(WifiInfo mWifiInfo, int netId) {
    }

    public boolean ignoreNetworkStateChange(NetworkInfo networkInfo) {
        return false;
    }

    public boolean ignoreSupplicantStateChange(SupplicantState state) {
        return false;
    }

    public void triggerRoamingNetworkMonitor(boolean autoRoaming) {
    }

    public void triggerInvalidlinkNetworkMonitor() {
    }

    public void setWifiBackgroundReason(int status) {
    }

    public boolean isDualbandScanning() {
        return false;
    }

    public void updateWifiBackgroudStatus(int msgType) {
    }

    public void notifyWifiScanResultsAvailable(boolean success) {
    }

    public void notifyWifiRoamingStarted() {
    }

    public void notifyWifiRoamingCompleted(String newBssid) {
    }

    public boolean isWiFiProSwitchOnGoing() {
        return false;
    }

    public void handleDualbandHandoverFailed(int disableReason) {
    }

    public void setWiFiProRoamingSSID(WifiSsid SSID) {
    }

    public WifiSsid getWiFiProRoamingSSID() {
        return null;
    }

    public void updateNetworkConnFailedInfo(int netId, int rssi, int reason) {
    }

    public void requestUpdateDnsServers(ArrayList<String> arrayList) {
    }

    public void sendUpdateDnsServersRequest(Message message, LinkProperties lp) {
    }

    public void requestRenewDhcp() {
    }

    public void setForceDhcpDiscovery(IpClientManager ipClient) {
    }

    public void resetIpConfigStatus() {
    }

    public void notifyIpConfigCompleted() {
    }

    public boolean isRenewDhcpSelfCuring() {
        return false;
    }

    public void requestUseStaticIpConfig(StaticIpConfiguration staticIpConfig) {
    }

    public boolean notifyIpConfigLostAndFixedBySce(WifiConfiguration config) {
        return false;
    }

    public void handleStaticIpConfig(IpClientManager ipClient, WifiNative wifiNative, StaticIpConfiguration config) {
    }

    public boolean isWifiSelfCuring() {
        return false;
    }

    public void exitWifiSelfCure(int exitedType, int networkId) {
    }

    public void notifySelfCureComplete(boolean succ, int reasonCode) {
    }

    public void startSelfCureWifiReset() {
    }

    public void startSelfCureWifiReassoc() {
    }

    public boolean checkSelfCureWifiResult(int event) {
        return false;
    }

    public void requestResetWifi() {
    }

    public void requestReassocLink() {
    }

    public void stopSelfCureWifi(int status) {
    }

    public void stopSelfCureDelay(int status, int delay) {
    }

    public void setWifiBackgroundStatus(boolean background) {
    }

    public int getSelfCureNetworkId() {
        return -1;
    }

    public boolean isBssidDisabled(String bssid) {
        return false;
    }

    public void notifySelfCureNetworkLost() {
    }

    public void handleInvalidIpAddr() {
    }

    public void handleNoInternetIp() {
    }

    public void startSelfCureReconnect() {
    }

    public void requestWifiSoftSwitch() {
    }

    public void handleDisconnectedReason(WifiConfiguration config, int rssi, int local, int reason) {
    }

    public long getWifiEnabledTimeStamp() {
        return 0;
    }

    public void handleAntenaPreempted() {
    }

    public boolean isWlanSettingsActivity() {
        return false;
    }

    public boolean isEnterpriseHotspot(WifiConfiguration config) {
        return false;
    }

    public void updateCHRDNS(List<InetAddress> list) {
    }

    public List<String> syncGetApLinkedStaList(AsyncChannel channel) {
        return null;
    }

    public void setSoftapMacFilter(String macFilter) {
    }

    public void setSoftapDisassociateSta(String mac) {
    }

    public List<String> handleGetApLinkedStaList() {
        return null;
    }

    public void handleSetSoftapMacFilter(String macFilter) {
    }

    public void handleSetSoftapDisassociateSta(String mac) {
    }

    public void handleSetWifiApConfigurationHw(String channel) {
    }

    public boolean handleWapiFailureEvent(Message message, SupplicantStateTracker mSupplicantStateTracker) {
        return false;
    }

    public int[] syncGetApChannelListFor5G(AsyncChannel channel) {
        return null;
    }

    public void setLocalMacAddressFromMacfile() {
    }

    public String getWifiCountryCode(Context context, String countryCode) {
        return countryCode;
    }

    public void handleStopWifiRepeater(AsyncChannel wifiP2pChannel) {
    }

    public boolean isWifiRepeaterStarted() {
        return false;
    }

    public void setWifiRepeaterStoped() {
    }

    public void sendStaFrequency(int frequency) {
    }

    public boolean isHiLinkActive() {
        return false;
    }

    public void enableHiLinkHandshake(boolean uiEnable, String bssid, WifiConfiguration config) {
    }

    public void sendWpsOkcStartedBroadcast() {
    }

    public NetworkUpdateResult saveWpsOkcConfiguration(int connectionNetId, String connectionBssid) {
        return null;
    }

    /* access modifiers changed from: protected */
    public void notifyWlanChannelNumber(int channel) {
    }

    /* access modifiers changed from: protected */
    public void notifyWlanState(String state) {
    }

    public boolean disallowWifiScanRequest(int pid) {
        return false;
    }

    public void updateLastScanRequestTimestamp() {
    }

    public int isAllowedManualWifiPwrBoost() {
        return 0;
    }

    public boolean isRSDBSupported(AsyncChannel channel) {
        return false;
    }

    public boolean isWifiConnectivityManagerEnabled() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void handleSimAbsent(WifiConfiguration config) {
    }

    /* access modifiers changed from: protected */
    public void handleEapErrorcodeReport(int networkId, String ssid, int errorcode) {
    }

    public boolean setWifiMode(String packageName, int mode) {
        return false;
    }

    public int getWifiMode() {
        return 0;
    }

    public String getWifiModeCallerPackageName() {
        return "";
    }
}
