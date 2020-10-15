package com.huawei.hwwifiproservice;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class WifiproUtils {
    public static final String ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND = "com.huawei.wifipro.action.ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND";
    public static final String ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE = "com.huawei.wifipro.action.ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE";
    public static final String ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND = "com.huawei.wifipro.action.ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND";
    public static final String ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND = "com.huawei.wifipro.action.ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND";
    public static final String ACTION_NOTIFY_PORTAL_OUT_OF_RANGE = "com.huawei.wifipro.action.ACTION_NOTIFY_PORTAL_OUT_OF_RANGE";
    public static final String ACTION_NOTIFY_SAVED_PORTAL_FOUND = "com.huawei.wifipro.action.ACTION_NOTIFY_SAVED_PORTAL_FOUND";
    public static final String ACTION_NOTIFY_WIFI_CONNECTED_CONCURRENTLY = "com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_CONNECTED_CONCURRENTLY";
    public static final int AUTOMATIC_CONNECT_AP = 135676;
    public static final int AUTO_EVALUATE_BACK = 0;
    public static final int AUTO_EVALUATE_SETTINGS = 1;
    public static final int BSSID_SWITCH_EVENT = 1;
    public static final int CMD_WIFIPRO_CONNECTED_TO_VERIFY_STATE = 135669;
    public static final int CMD_WIFIPRO_STATE_BASE = 135668;
    public static final int CMD_WIFIPRO_VERIFY_TO_CONNECTED_STATE = 135670;
    public static final String CODE_EXCLUSIVE = "code_exclusive";
    public static final String CODE_EXCLUSIVE_DATE_1 = "code_exclusive_date_1";
    public static final String CODE_EXCLUSIVE_DATE_2 = "code_exclusive_date_2";
    public static final String CODE_EXCLUSIVE_DATE_3 = "code_exclusive_date_3";
    public static final String CODE_NECESSARY = "code_necessary";
    public static final boolean DBG = true;
    public static final int DUALBAND_REASON_BLACKLIST = 2;
    public static final int DUALBAND_REASON_CONNECT_FAIL = 4;
    public static final int DUALBAND_REASON_FAIL = 1;
    public static final int DUALBAND_REASON_IGNORE_ALREADY = 10;
    public static final int DUALBAND_REASON_IGNORE_DUE_PREFER = 11;
    public static final int DUALBAND_REASON_NO_SATISFIED_CONDITION = 3;
    public static final int DUALBAND_REASON_TRYCONN = 0;
    public static final int DUALBAND_SWITCH_EVENT = 2;
    public static final String EVALUATION_EVENT = "apEvaluateEvent";
    public static final String EVALUATION_HAS_INTERNET = "internetAPCnt";
    public static final String EVALUATION_NO_INTERNET = "noInternetAPCnt";
    public static final String EVALUATION_PORTAL_INTERNET = "portalAPCnt";
    public static final String EVALUATION_TRIGGER = "apEvaluateTrigCnt";
    public static final boolean IS_DUALBAND_ENABLE = true;
    public static final int MANUAL_CONNECT_AP = 135675;
    public static final int MANUAL_EVALUATE = 2;
    public static final int NET_INET_QOS_BSSID_CHG_WHILE_CHKING = -102;
    public static final int NET_INET_QOS_LEVEL_0_NOT_AVAILABLE = 0;
    public static final int NET_INET_QOS_LEVEL_1_VERY_POOR = 1;
    public static final int NET_INET_QOS_LEVEL_2_POOR = 2;
    public static final int NET_INET_QOS_LEVEL_3_GOOD = 3;
    public static final int NET_INET_QOS_LEVEL_4_BETTER = 4;
    public static final int NET_INET_QOS_LEVEL_5_BEST = 5;
    public static final int NET_INET_QOS_LEVEL_6_PORTAL = 6;
    public static final int NET_INET_QOS_LEVEL_7_TIMEOUT = 7;
    public static final int NET_INET_QOS_LEVEL_NEG_1_NO_INET = -1;
    public static final int NET_INET_QOS_LEVEL_NEG_2_MAYBE_POOR = -2;
    public static final int NET_INET_QOS_LEVEL_UNKNOWN = -101;
    public static final String PERMISSION_RECV_WIFI_CONNECTED_CONCURRENTLY = "com.huawei.wifipro.permission.RECV.WIFI_CONNECTED_CONCURRENTLY";
    public static final int REQUEST_POOR_RSSI_INET_CHECK = -104;
    public static final int REQUEST_WIFI_INET_CHECK = -103;
    private static final int RUN_WITH_SCISSORS_TIMEOUT_MILLIS = 4000;
    public static final String SMS_BODY_OPT = "sms_body_opt";
    public static final String SMS_NUM_BEGIN = "sms_num_begin";
    public static final String SMS_NUM_LEN = "sms_num_min_len";
    public static final int SSID_SWITCH_EVENT = 0;
    public static final String SWITCH_SUCCESS_INDEX = "index";
    public static final String TAG = "WiFi_PRO";
    public static final String UNEXPECTED_EVENT_USER_REJECT = "userRejectSwitch";
    public static final String UNEXPECTED_WIFI_SWITCH_EVENT = "unExpectSwitchEvent";
    public static final int WAVE_MAPPING_USER_PREFER_NW = 0;
    public static final int WIFIPRO_START_VERIFY_WITH_DATA_LINK = 135672;
    public static final int WIFIPRO_START_VERIFY_WITH_NOT_DATA_LINK = 135671;
    public static final int WIFIPRO_STOP_VERIFY_WITH_DATA_LINK = 135674;
    public static final int WIFIPRO_STOP_VERIFY_WITH_NOT_DATA_LINK = 135673;
    public static final int WIFI_BACKGROUND_AP_SCORE = 1;
    public static final int WIFI_BACKGROUND_IDLE = 0;
    public static final int WIFI_BACKGROUND_INTERNET_RECOVERY_CHECKING = 3;
    public static final int WIFI_BACKGROUND_PORTAL_CHECKING = 2;
    public static final int WIFI_CONNECT_WITH_DATA_LINK = 1;
    public static final String WIFI_SWITCH_CNT_EVENT = "wifiSwitchCnt";
    public static final String WIFI_SWITCH_EVENT = "wifiSwitchCntEvent";
    public static final String WIFI_SWITCH_SUCC_EVENT = "wifiSwitchSuccCnt";
    public static final int WIFI_VERIFY_NO_DATA_LINK = 2;

    public static String formatTime(long time) {
        if (time == 0) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
    }

    public static List<ScanResult> getScanResultsFromWsm() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 12, new Bundle());
        if (result == null) {
            return null;
        }
        return result.getParcelableArrayList("scanResults");
    }

    public static List<WifiConfiguration> getAllConfiguredNetworks() {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        if (wifiInjector == null) {
            Log.e("WiFi_PRO", "wifi injector is null");
            return Collections.emptyList();
        }
        List<WifiConfiguration> allConfigNetworks = new ArrayList<>();
        if (wifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(wifiInjector, allConfigNetworks) {
            /* class com.huawei.hwwifiproservice.$$Lambda$WifiproUtils$qcCXSFcVgfrXtatWEJutXEt7lM */
            private final /* synthetic */ WifiInjector f$0;
            private final /* synthetic */ List f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void run() {
                WifiproUtils.lambda$getAllConfiguredNetworks$0(this.f$0, this.f$1);
            }
        }, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT)) {
            return allConfigNetworks;
        }
        Log.e("WiFi_PRO", "Failed to post runnable to get wifi configured networks");
        return Collections.emptyList();
    }

    static /* synthetic */ void lambda$getAllConfiguredNetworks$0(WifiInjector wifiInjector, List allConfigNetworks) {
        List<WifiConfiguration> configNetworks = wifiInjector.getWifiConfigManager().getConfiguredNetworks();
        if (configNetworks != null) {
            allConfigNetworks.addAll(configNetworks);
        }
    }
}
