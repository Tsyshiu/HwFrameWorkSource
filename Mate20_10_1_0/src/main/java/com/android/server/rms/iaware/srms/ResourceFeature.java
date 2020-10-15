package com.android.server.rms.iaware.srms;

import android.content.Context;
import android.os.SystemProperties;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DumpData;
import android.rms.iaware.StatisticsData;
import com.android.server.am.HwActivityManagerService;
import com.android.server.rms.HwSysResManagerService;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.android.server.rms.iaware.feature.RFeature;
import java.util.ArrayList;

public class ResourceFeature extends RFeature {
    private static final int QUEUESIZE_FOR_DUMPDATA = 12;
    private static final String TAG = "ResourceFeature";
    static final boolean enableResMonitor = SystemProperties.getBoolean("ro.config.res_monitor", true);
    static final boolean enableResQueue = SystemProperties.getBoolean("ro.config.res_queue", true);
    private static boolean mFeature;

    static {
        boolean z = false;
        if (SystemProperties.getBoolean("persist.sys.enable_iaware", false) && SystemProperties.getBoolean("persist.sys.srms.enable", true)) {
            z = true;
        }
        mFeature = z;
    }

    public ResourceFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        setEnable();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        setDisable();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean configUpdate() {
        HwSysResManagerService.self().cloudFileUpate();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public String saveBigData(boolean clear) {
        if (mFeature) {
            return SRMSDumpRadar.getInstance().saveSRMSBigData(clear);
        }
        AwareLog.e(TAG, "iaware srms is close, it is invalid operation to save big data.");
        return null;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public ArrayList<StatisticsData> getStatisticsData() {
        if (mFeature) {
            return SRMSDumpRadar.getInstance().getStatisticsData();
        }
        AwareLog.e(TAG, "iaware srms is close, it is invalid operation to get statistics data.");
        return null;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public ArrayList<DumpData> getDumpData(int time) {
        if (mFeature) {
            return getDumpData();
        }
        AwareLog.e(TAG, "iaware srms is close, it is invalid operation to get dump data.");
        return null;
    }

    private void setEnable() {
        AwareLog.i(TAG, "open iaware srms feature!");
        mFeature = true;
    }

    private void setDisable() {
        AwareLog.i(TAG, "close iaware srms feature!");
        mFeature = false;
    }

    public static boolean getIawareResourceFeature(int type) {
        if (type == 1) {
            return mFeature && enableResQueue;
        }
        if (type != 2) {
            return mFeature;
        }
        return mFeature && enableResMonitor;
    }

    private ArrayList<DumpData> getDumpData() {
        ArrayList<Integer> queueSizes = HwActivityManagerService.self().getIawareDumpData();
        if (queueSizes.size() < 12) {
            AwareLog.e(TAG, "get iaware srms dump data error,size is too small.");
            return null;
        }
        ArrayList<DumpData> dumpDatas = new ArrayList<>();
        long currTime = System.currentTimeMillis();
        int RESOURCE_FEATURE_ID = AwareConstant.FeatureType.getFeatureId(AwareConstant.FeatureType.FEATURE_RESOURCE);
        StringBuffer queueSizeBuffer = new StringBuffer();
        queueSizeBuffer.append(queueSizes.get(0));
        queueSizeBuffer.append("&");
        queueSizeBuffer.append(queueSizes.get(1));
        queueSizeBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        queueSizeBuffer.append(queueSizes.get(2));
        queueSizeBuffer.append("&");
        queueSizeBuffer.append(queueSizes.get(3));
        queueSizeBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        queueSizeBuffer.append(queueSizes.get(4));
        queueSizeBuffer.append("&");
        queueSizeBuffer.append(queueSizes.get(5));
        queueSizeBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        queueSizeBuffer.append(queueSizes.get(6));
        queueSizeBuffer.append("&");
        queueSizeBuffer.append(queueSizes.get(7));
        queueSizeBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        queueSizeBuffer.append(queueSizes.get(8));
        queueSizeBuffer.append("&");
        queueSizeBuffer.append(queueSizes.get(9));
        queueSizeBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        queueSizeBuffer.append(queueSizes.get(10));
        queueSizeBuffer.append("&");
        queueSizeBuffer.append(queueSizes.get(11));
        dumpDatas.add(new DumpData(currTime, RESOURCE_FEATURE_ID, "fg.p&o#bg.p&o#fg3.p&o#bg3.p&o#fgk.p&o#fgk.p&o", 0, new String(queueSizeBuffer)));
        return dumpDatas;
    }
}
