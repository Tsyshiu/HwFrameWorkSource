package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.Bundle;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.mtm.iaware.appmng.appiolimit.AwareAppIoLimitMng;
import com.android.server.rms.iaware.IRDataRegister;
import java.util.concurrent.atomic.AtomicBoolean;

public class APPIoLimitFeature extends RFeature {
    private static final int IAWARE5_VERSION = 5;
    private static final int MIN_VERSION = 2;
    private static final String TAG = "APPIoLimitFeature";
    private static boolean sIsIAware5Enable = false;
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    public APPIoLimitFeature(Context context, AwareConstant.FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        initConfig();
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        if (data == null) {
            return false;
        }
        int i = AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.getResourceType(data.getResId()).ordinal()];
        if (i == 1) {
            AwareAppIoLimitMng.getInstance().report(20011);
            return true;
        } else if (i == 2) {
            AwareAppIoLimitMng.getInstance().report(90011);
            return true;
        } else if (i == 3) {
            Bundle bAppSSoc = data.getBundle();
            if (bAppSSoc != null) {
                AwareAppIoLimitMng.getInstance().report(bAppSSoc.getInt("relationType"), bAppSSoc);
            }
            return true;
        } else if (i == 4) {
            Bundle bStatus = data.getBundle();
            if (bStatus != null) {
                AwareAppIoLimitMng.getInstance().report(bStatus.getInt("eventid"));
            }
            return true;
        } else if (i != 5) {
            return false;
        } else {
            AwareLog.d(TAG, "click media button");
            Bundle bMedia = data.getBundle();
            if (bMedia != null) {
                AwareAppIoLimitMng.getInstance().report(bMedia.getInt("eventid"), bMedia);
            }
            return true;
        }
    }

    /* renamed from: com.android.server.rms.iaware.feature.APPIoLimitFeature$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$ResourceType = new int[AwareConstant.ResourceType.values().length];

        static {
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_SCREEN_ON.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_SCREEN_OFF.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_APPASSOC.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_STATUS_BAR.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_MEDIA_BTN.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        AwareLog.d(TAG, "disable App Io Limit Feature!");
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_APPASSOC, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_STATUS_BAR, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_MEDIA_BTN, this.mFeatureType);
        }
        AwareAppIoLimitMng.disable();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        AwareLog.d(TAG, "enable App Io Limit Feature version:!" + realVersion);
        if (realVersion >= 2) {
            if (this.mIRDataRegister != null) {
                this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
                this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
                this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_APPASSOC, this.mFeatureType);
                this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_STATUS_BAR, this.mFeatureType);
                this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_MEDIA_BTN, this.mFeatureType);
            }
            AwareAppIoLimitMng.enable();
        }
        if (realVersion >= 5) {
            sIsIAware5Enable = true;
        }
        return true;
    }

    public static boolean isIAware5Enable() {
        return sIsIAware5Enable;
    }

    private void initConfig() {
        if (!sIsInitialized.get()) {
            AwareAppIoLimitMng.getInstance();
            sIsInitialized.set(true);
        }
    }
}
