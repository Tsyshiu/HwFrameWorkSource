package com.android.server.hidata.histream;

import com.android.server.hidata.appqoe.HwAPPQoEUserAction;

/* compiled from: HwHiStreamAPInfo */
class HiStreamAPInfo {
    public static final int AP_USER_TYPE_AGGRESSIVE = 2;
    public static final int AP_USER_TYPE_ECO = 3;
    public static final int AP_USER_TYPE_GENERAL = 1;
    public static final int AP_USER_TYPE_UNKNOWN = 0;
    public static final int AP_USER_TYPE_WLANPLUSOFF = 4;
    public int APUsrType;
    public String mSSID;
    public int mScenceId;

    public HiStreamAPInfo(String ssid, int scenceId, int apUsrType) {
        this.mSSID = ssid == null ? HwAPPQoEUserAction.DEFAULT_CHIP_TYPE : ssid;
        this.mScenceId = scenceId;
        this.APUsrType = apUsrType;
    }
}
