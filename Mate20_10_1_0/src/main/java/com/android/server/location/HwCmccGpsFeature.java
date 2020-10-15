package com.android.server.location;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.SettingsEx;
import com.huawei.utils.reflect.EasyInvokeFactory;

public class HwCmccGpsFeature implements IHwCmccGpsFeature {
    private static final int AGPS_ROAMING_ENABLED = 1;
    private static final int AGPS_ROAMING_UNENABLED = 0;
    private static final String AGPS_SERVICE_IP_DEFAULT = "supl.google.com";
    private static final int AGPS_SERVICE_PORT_DEFAULT = 7275;
    private static final int COLD_MODE = 2;
    private static final int GPS_POSITION_MODE_MS_ASSISTED = 2;
    private static final int GPS_POSITION_MODE_MS_BASED = 1;
    private static final int GPS_POSITION_MODE_STANDALONE = 0;
    private static final int HOT_MODE = 0;
    private static final String KEY_AGPS_ROAMING_ENABLED = "assisted_gps_roaming_enabled";
    private static final String KEY_AGPS_SERVICE_ADDRESS = "assisted_gps_service_IP";
    private static final String KEY_AGPS_SERVICE_PORT = "assisted_gps_service_port";
    private static final String KEY_AGPS_SETTINGS = "assisted_gps_mode";
    private static final String TAG = "HwCmccGpsFeature";
    private static final String TIME_SYNCHRONIZATION = "time_synchronization";
    private static final int TIME_SYNCHRONIZTION_OFF = 0;
    private static final int TIME_SYNCHRONIZTION_ON = 1;
    private static final int WARM_MODE = 1;
    private boolean mAgpsSwitchOn = SystemProperties.getBoolean("ro.config.agps_server_setting", false);
    private Context mContext;
    private GnssLocationProvider mGnssLocationProvider;
    private boolean mIsRoaming = false;
    private int mNeedSyncTime;
    private boolean mSyncedTimeFlag = true;
    private GpsLocationProviderUtils utils;

    public HwCmccGpsFeature(Context context, GnssLocationProvider gnssLocationProvider) {
        this.mContext = context;
        this.mGnssLocationProvider = gnssLocationProvider;
        this.utils = EasyInvokeFactory.getInvokeUtils(GpsLocationProviderUtils.class);
    }

    public void setRoaming(boolean flag) {
        this.mIsRoaming = flag;
    }

    public boolean checkSuplInit() {
        if (!this.mAgpsSwitchOn) {
            return false;
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "assisted_gps_enabled", 1) == 0) {
            return true;
        }
        setAgpsServer();
        return false;
    }

    private void setAgpsServer() {
        String strSuplServerHost = SettingsEx.Systemex.getString(this.mContext.getContentResolver(), KEY_AGPS_SERVICE_ADDRESS);
        int suplServerPort = SettingsEx.Systemex.getInt(this.mContext.getContentResolver(), KEY_AGPS_SERVICE_PORT, (int) AGPS_SERVICE_PORT_DEFAULT);
        if (strSuplServerHost == null) {
            strSuplServerHost = AGPS_SERVICE_IP_DEFAULT;
        }
        this.utils.setSuplHostPort(this.mGnssLocationProvider, strSuplServerHost, String.valueOf(suplServerPort));
    }

    private int setPostionMode() {
        int positionMode = SettingsEx.Systemex.getInt(this.mContext.getContentResolver(), KEY_AGPS_SETTINGS, 1);
        if (!(positionMode == 0 || positionMode == 1 || positionMode == 2)) {
            positionMode = 1;
        }
        if (SettingsEx.Systemex.getInt(this.mContext.getContentResolver(), KEY_AGPS_ROAMING_ENABLED, 0) != 1 && this.mIsRoaming) {
            positionMode = 0;
        }
        LBSLog.i(TAG, false, "setPostionMode positionMode:%{public}d", Integer.valueOf(positionMode));
        return positionMode;
    }

    public int setPostionModeAndAgpsServer(int oldPositionMode, boolean agpsEnabled) {
        if (!this.mAgpsSwitchOn || !agpsEnabled) {
            return oldPositionMode;
        }
        int positionMode = setPostionMode();
        if (positionMode != 0) {
            setAgpsServer();
        }
        this.mSyncedTimeFlag = true;
        this.mNeedSyncTime = SettingsEx.Systemex.getInt(this.mContext.getContentResolver(), TIME_SYNCHRONIZATION, 0);
        return positionMode;
    }

    public void syncTime(long timestamp) {
        if (this.mNeedSyncTime == 1 && this.mSyncedTimeFlag && this.mAgpsSwitchOn) {
            LBSLog.i(TAG, false, "syncing gps time", new Object[0]);
            this.mSyncedTimeFlag = false;
            SystemClock.setCurrentTimeMillis(timestamp);
        }
    }

    public void setDelAidData() {
        int mode = SettingsEx.Systemex.getInt(this.mContext.getContentResolver(), "gps_start_mode", 0);
        Bundle extras = new Bundle();
        if (mode == 0) {
            LBSLog.i(TAG, false, "HOT_MODE", new Object[0]);
        } else if (mode == 1) {
            LBSLog.i(TAG, false, "WARM_MODE", new Object[0]);
            extras.putBoolean("ephemeris", true);
            this.mGnssLocationProvider.sendExtraCommand("delete_aiding_data", extras);
        } else if (mode == 2) {
            LBSLog.i(TAG, false, "COLD_MODE", new Object[0]);
            extras.putBoolean("ephemeris", true);
            extras.putBoolean("position", true);
            extras.putBoolean("time", true);
            this.mGnssLocationProvider.sendExtraCommand("delete_aiding_data", extras);
        }
    }
}
