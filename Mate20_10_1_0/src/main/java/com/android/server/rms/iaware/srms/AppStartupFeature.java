package com.android.server.rms.iaware.srms;

import android.content.Context;
import android.os.SystemProperties;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.feature.RFeature;

public class AppStartupFeature extends RFeature {
    private static final int MIN_VERSION = 2;
    private static final String TAG = "AppStartupFeature";
    private static boolean mBetaUser;
    private static boolean mFeature = SystemProperties.getBoolean("persist.sys.appstart.enable", false);

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        mBetaUser = z;
    }

    public AppStartupFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    public static boolean isAppStartupEnabled() {
        return mFeature;
    }

    public static boolean isBetaUser() {
        return mBetaUser;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        setEnable(false);
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        setEnable(false);
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 2) {
            setEnable(false);
            return false;
        }
        setEnable(true);
        AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
        if (policy != null) {
            policy.initSystemUidCache();
        }
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public String getBigDataByVersion(int iawareVer, boolean forBeta, boolean clearData) {
        if (!mFeature || iawareVer < 2) {
            AwareLog.e(TAG, "bigdata is not support, mFeature=" + mFeature + ", iawareVer=" + iawareVer);
            return null;
        } else if (mBetaUser == forBeta) {
            return SRMSDumpRadar.getInstance().saveStartupBigData(forBeta, clearData, false);
        } else {
            AwareLog.i(TAG, "request bigdata is not match, betaUser=" + mBetaUser + ", forBeta=" + forBeta + ", clearData=" + clearData);
            return null;
        }
    }

    public static void setEnable(boolean on) {
        AwareLog.i(TAG, "iaware appstartup feature changed: " + mFeature + "->" + on);
        if (mFeature != on) {
            AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
            if (policy != null) {
                policy.setEnable(on);
            }
            mFeature = on;
        }
    }
}
