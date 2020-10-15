package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;

public class AppSceneMngFeature extends RFeature {
    private static final int BASE_VERSION = 5;
    private static final String TAG = "AppSceneMngFeature";
    private static boolean mIsEnable = false;
    private int mIAwareVersion = 5;

    public AppSceneMngFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    public static boolean isEnable() {
        return mIsEnable;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        if (!mIsEnable) {
            return false;
        }
        mIsEnable = false;
        AwareLog.i(TAG, "AppSceneMngFeature disable");
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        AwareLog.i(TAG, "AppSceneMng" + realVersion);
        if (realVersion < 5 || mIsEnable) {
            return false;
        }
        mIsEnable = true;
        AwareLog.i(TAG, "AppSceneMngFeature enableFeatureEx");
        return true;
    }
}
