package com.android.server.display;

import android.hardware.display.WifiDisplay;
import android.os.SystemProperties;
import com.huawei.android.hardware.display.HwWifiDisplayParameters;

public interface IHwWifiDisplayControllerEx {
    public static final boolean ENABLED_PC;
    public static final boolean WFD_OPTIMIZE = SystemProperties.getBoolean("ro.config.hw_wfd_optimize", false);
    public static final boolean WFD_PC_MODE = SystemProperties.getBoolean("ro.config.hw_emui_wfd_pc_mode", false);

    void advertisDisplayCasting(WifiDisplay wifiDisplay);

    void checkVerificationResult(boolean z);

    void displayDataNotify(String str);

    int getConnectionErrorCode();

    void registerPGStateEvent();

    void requestStartScan(int i);

    void resetDisplayParameters();

    void sendWifiDisplayAction(String str);

    void setConnectParameters(boolean z, boolean z2, HwWifiDisplayParameters hwWifiDisplayParameters);

    void setDisplayParameters();

    void setVideoBitrate();

    void setWorkFrequency(int i);

    boolean tryDiscoverPeersEx();

    void unregisterPGStateEvent();

    void updateConnectionErrorCode(int i);

    static {
        boolean z = false;
        if (WFD_OPTIMIZE && WFD_PC_MODE) {
            z = true;
        }
        ENABLED_PC = z;
    }
}
