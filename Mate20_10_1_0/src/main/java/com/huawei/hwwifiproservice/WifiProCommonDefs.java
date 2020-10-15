package com.huawei.hwwifiproservice;

public class WifiProCommonDefs {
    public static final String ACTION_DHCP_OFFER_INFO = "com.hw.wifipro.action.DHCP_OFFER_INFO";
    public static final String ACTION_FIRST_CHECK_NO_INTERNET_NOTIFICATION = "com.huawei.wifi.action.FIRST_CHECK_NO_INTERNET_NOTIFICATION";
    public static final String ACTION_INVALID_DHCP_OFFER_RCVD = "com.hw.wifipro.action.INVALID_DHCP_OFFER_RCVD";
    public static final String ACTION_NETWORK_CONDITIONS_MEASURED = "huawei.conn.NETWORK_CONDITIONS_MEASURED";
    public static final String ACTION_NETWORK_PROPERTY_CHR = "com.huawei.wifi.action.NETWORK_PROPERTY_CHR";
    public static final String ACTION_NETWOR_PROPERTY_NOTIFICATION = "com.huawei.wifi.action.NETWOR_PROPERTY_NOTIFICATION";
    public static final String ACTION_NOTIFY_WIFI_SECURITY_STATUS = "com.huawei.wifipro.ACTION_NOTIFY_WIFI_SECURITY_STATUS";
    public static final String ACTION_PORTAL_CANCELED_BY_USER = "com.huawei.wifipro.action.ACTION_PORTAL_CANCELED_BY_USER";
    public static final String ACTION_PORTAL_USED_BY_USER = "com.huawei.wifipro.action.ACTION_PORTAL_USED_BY_USER";
    public static final String ACTION_QUERY_WIFI_SECURITY = "com.huawei.wifipro.ACTION_QUERY_WIFI_SECURITY";
    public static final String ACTION_REQUEST_TCP_RX_COUNTER = "com.huawei.wifi.action.ACTION_REQUEST_TCP_RX_COUNTER";
    public static final String ACTION_REQUEST_WEBVIEW_CHECK = "com.huawei.wifipro.action.ACTION_REQUEST_WEBVIEW_CHECK";
    public static final String ACTION_REQUEST_WEBVIEW_CHECK_TO_SERVICE = "com.huawei.wifipro.action.ACTION_REQUEST_WEBVIEW_CHECK_TO_SERVICE";
    public static final String ACTION_RESPONSE_TCP_RX_COUNTER = "com.huawei.wifi.action.ACTION_RESPONSE_TCP_RX_COUNTER";
    public static final String ACTION_RESP_WEBVIEW_CHECK = "com.huawei.wifipro.action.ACTION_RESP_WEBVIEW_CHECK";
    public static final String ACTION_RESP_WEBVIEW_CHECK_FROM_SERVICE = "com.huawei.wifipro.action.ACTION_RESP_WEBVIEW_CHECK_FROM_SERVICE";
    public static final String ACTION_UPDATE_CONFIG_HISTORY = "com.huawei.wifipro.ACTION_UPDATE_CONFIG_HISTORY";
    public static final String EXTRA_FLAG_NETWORK_PROPERTY = "wifi_network_property";
    public static final String EXTRA_FLAG_NETWORK_TYPE = "wifipro_flag_network_type";
    public static final String EXTRA_FLAG_NEW_WIFI_CONFIG = "new_wifi_config";
    public static final String EXTRA_FLAG_OVERSEA = "wifipro_flag_oversea";
    public static final String EXTRA_FLAG_TCP_RX_COUNTER = "wifipro_tcp_rx_counter";
    public static final String EXTRA_IS_INTERNET_READY = "extra_is_internet_ready";
    public static final String EXTRA_RAW_REDIRECTED_HOST = "raw_redirected_host";
    public static final String EXTRA_STANDARD_PORTAL_NETWORK = "standard_portal_network";
    public static final String FLAG_BSSID = "com.huawei.wifipro.FLAG_BSSID";
    public static final String FLAG_DHCP_OFFER_INFO = "com.hw.wifipro.FLAG_DHCP_OFFER_INFO";
    public static final String FLAG_SECURITY_STATUS = "com.huawei.wifipro.FLAG_SECURITY_STATUS";
    public static final String FLAG_SSID = "com.huawei.wifipro.FLAG_SSID";
    public static final int MIN_VAL_LEVEL_2_24G = -82;
    public static final int MIN_VAL_LEVEL_2_5G = -79;
    public static final int MIN_VAL_LEVEL_3 = -75;
    public static final int MIN_VAL_LEVEL_3_24G = -75;
    public static final int MIN_VAL_LEVEL_3_5 = -70;
    public static final int MIN_VAL_LEVEL_3_5G = -72;
    public static final int MIN_VAL_LEVEL_4 = -65;
    public static final String NETWORK_CHECKER_RECV_PERMISSION = "com.huawei.wifipro.permission.RECV.NETWORK_CHECKER";
    public static final int QUERY_TIMEOUT_MS = 30000;
    public static final int TYEP_HAS_INTERNET = 101;
    public static final int TYEP_PORTAL = 102;
    public static final int TYEP_UNKNOWN = 100;
    public static final String WIFI_PRO_SECURITY_RECV_PERMISSION = "com.huawei.wifipro.permission.WIFI_SECURITY_CHECK";
    public static final int WIFI_SECURITY_ARP_FAILED = 4;
    public static final int WIFI_SECURITY_DNS_FAILED = 2;
    public static final int WIFI_SECURITY_DNS_PHISHING_TIMEOUT = 1;
    public static final int WIFI_SECURITY_OK = 0;
    public static final int WIFI_SECURITY_PHISHING_FAILED = 3;
    public static final int WIFI_SECURITY_UNKNOWN = -1;

    private WifiProCommonDefs() {
    }
}
