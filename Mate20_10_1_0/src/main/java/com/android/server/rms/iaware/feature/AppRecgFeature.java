package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize;

public class AppRecgFeature extends RFeature {
    private static final int BASE_VERSION = 1;
    private static final int MIN_VERSION = 2;
    private static final String TAG = "AppRecgFeature";
    private int mIAwareVersion = 1;

    public AppRecgFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
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
        if (this.mIAwareVersion < 2) {
            return false;
        }
        AwareIntelligentRecg.commDisable();
        AwareSceneRecognize.disable();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        this.mIAwareVersion = realVersion;
        if (realVersion < 2) {
            return false;
        }
        AwareLog.i(TAG, "AppRecgFeature enableFeatureEx");
        AwareIntelligentRecg.commEnable();
        AwareSceneRecognize.enable();
        return true;
    }
}
