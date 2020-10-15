package com.android.server.rms.iaware.feature;

import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.CollectData;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AppMngConfig;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppMngDFX;
import com.android.server.rms.iaware.appmng.AwareAppUseDataManager;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize;
import java.util.concurrent.atomic.AtomicBoolean;

public class APPMngFeature extends RFeature {
    private static final String TAG = "APPMngFeature";
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    public APPMngFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
        initConfig();
    }

    private boolean reportOtherDatas(CollectData data) {
        if (data.getResId() == AwareConstant.ResourceType.RESOURCE_SCREEN_OFF.ordinal()) {
            AwareIntelligentRecg.getInstance().reportScreenChangedTime(SystemClock.elapsedRealtime());
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit != null) {
                habit.reportScreenState(data.getResId());
            }
            AwareIntelligentRecg.getInstance().report(90011, new Bundle());
            return true;
        } else if (data.getResId() != AwareConstant.ResourceType.RESOURCE_SCREEN_ON.ordinal()) {
            return false;
        } else {
            AwareIntelligentRecg.getInstance().report(20011, new Bundle());
            return true;
        }
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        if (data == null) {
            return false;
        }
        if (data.getResId() == AwareConstant.ResourceType.RESOURCE_APPASSOC.ordinal()) {
            Bundle bundle = data.getBundle();
            if (bundle == null) {
                return false;
            }
            if (isIAwareIIFeature(bundle)) {
                AwareIntelligentRecg.getInstance().report(bundle.getInt("relationType"), bundle);
            } else {
                AwareAppAssociate.getInstance().report(bundle.getInt("relationType"), bundle);
            }
            return true;
        } else if (data.getResId() == AwareConstant.ResourceType.RESOURCE_USERHABIT.ordinal()) {
            return handleResourceUserHabit(data);
        } else {
            if (data.getResId() == AwareConstant.ResourceType.RESOURCE_SCENE_REC.ordinal()) {
                Bundle bundle2 = data.getBundle();
                if (bundle2 == null) {
                    return false;
                }
                AwareSceneRecognize sceneRec = AwareSceneRecognize.getInstance();
                if (sceneRec != null) {
                    sceneRec.report(bundle2.getInt("relationType"), bundle2);
                }
                return true;
            } else if (data.getResId() != AwareConstant.ResourceType.RES_APP.ordinal()) {
                return reportOtherDatas(data);
            } else {
                AwareSceneRecognize sceneRec2 = AwareSceneRecognize.getInstance();
                if (sceneRec2 != null) {
                    sceneRec2.reportActivityStart(data);
                }
                return true;
            }
        }
    }

    private boolean handleResourceUserHabit(CollectData data) {
        Bundle bundle = data.getBundle();
        if (bundle == null) {
            return false;
        }
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit != null) {
            habit.report(bundle.getInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE), bundle);
        }
        AwareIntelligentRecg intlRecg = AwareIntelligentRecg.getInstance();
        if (intlRecg != null) {
            intlRecg.reportAppUpdate(bundle.getInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE), bundle);
        }
        AwareFakeActivityRecg recgFakeActivity = AwareFakeActivityRecg.self();
        if (recgFakeActivity == null) {
            return true;
        }
        recgFakeActivity.reportAppUpdate(bundle.getInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE), bundle);
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        if (this.mIRDataRegister == null) {
            return false;
        }
        this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_APPASSOC, this.mFeatureType);
        this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_USERHABIT, this.mFeatureType);
        this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCENE_REC, this.mFeatureType);
        this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RES_APP, this.mFeatureType);
        this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
        AwareAppMngSort.enable();
        AwareAppAssociate.enable();
        AwareAppKeyBackgroup.enable(this.mContext);
        AwareUserHabit.enable();
        AwareDefaultConfigList.enable(this.mContext);
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        if (this.mIRDataRegister == null) {
            return false;
        }
        this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_APPASSOC, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_USERHABIT, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_SCENE_REC, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RES_APP, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
        AwareAppMngSort.disable();
        AwareAppAssociate.disable();
        AwareAppKeyBackgroup.disable();
        AwareDefaultConfigList.disable();
        AwareUserHabit.disable();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public String saveBigData(boolean clear) {
        if (!AwareAppMngSort.checkAppMngEnable()) {
            return null;
        }
        return AwareAppMngDFX.getInstance().getAppMngDfxData(clear);
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean configUpdate() {
        return updateHabitConfig();
    }

    private boolean updateHabitConfig() {
        Bundle bdl = new Bundle();
        bdl.putInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE, 4);
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit == null) {
            return false;
        }
        habit.report(4, bdl);
        return true;
    }

    private void initConfig() {
        if (!sIsInitialized.get()) {
            sIsInitialized.set(true);
            AppMngConfig.init();
            DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.APP_CLEAN, this.mContext);
            AwareAppUseDataManager.getInstance().reportCleanConfigReadFinish();
        }
    }

    private boolean isIAwareIIFeature(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        int relationType = bundle.getInt("relationType");
        if (!(relationType == 8 || relationType == 9)) {
            if (!(relationType == 17 || relationType == 22 || relationType == 19 || relationType == 20 || relationType == 35 || relationType == 36)) {
                switch (relationType) {
                    case 27:
                        break;
                    case 28:
                    case 29:
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }
        if (bundle.getInt("windowtype") == 45) {
            return true;
        }
        return false;
    }
}
