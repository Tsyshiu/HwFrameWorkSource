package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/* compiled from: CPUXmlConfiguration */
class CPUVipParaConfig extends CPUCustBaseConfig {
    private static final String CONFIG_VIP = "vip_thread_param";
    private static final String CONFIG_VIP_DYM_GRAN = "vip_max_dynamic_granularity";
    private static final String CONFIG_VIP_MIGRATION = "vip_min_migration_delay";
    private static final String CONFIG_VIP_SCHED_DELAY = "vip_min_sched_delay_granularity";
    private static final String TAG = "CPUVipParaConfig";
    private CPUFeature mCPUFeatureInstance;
    private Map<String, Integer> mVipThreadParams = new ArrayMap();
    private Map<String, Integer> mVipThreadParamsOrder = new ArrayMap();

    CPUVipParaConfig() {
        init();
    }

    @Override // com.android.server.rms.iaware.cpu.CPUCustBaseConfig
    public void setConfig(CPUFeature feature) {
        if (feature != null) {
            this.mCPUFeatureInstance = feature;
            int size = this.mVipThreadParams.size();
            ByteBuffer buffer = ByteBuffer.allocate((size * 2 * 4) + 8);
            buffer.putInt(150);
            buffer.putInt(size);
            for (Map.Entry<String, Integer> entry : this.mVipThreadParams.entrySet()) {
                Integer msgId = this.mVipThreadParamsOrder.get(entry.getKey());
                if (msgId != null) {
                    buffer.putInt(msgId.intValue());
                    buffer.putInt(entry.getValue().intValue());
                } else {
                    AwareLog.e(TAG, "find a unknown params:" + entry.getKey() + ", return directly");
                    return;
                }
            }
            int resCode = this.mCPUFeatureInstance.sendPacket(buffer);
            if (resCode != 1) {
                AwareLog.e(TAG, "sendConfig sendPacket failed, send error code:" + resCode);
            }
        }
    }

    private void init() {
        obtainVipThreadParams();
        this.mVipThreadParamsOrder.put(CONFIG_VIP_SCHED_DELAY, 1);
        this.mVipThreadParamsOrder.put(CONFIG_VIP_DYM_GRAN, 2);
        this.mVipThreadParamsOrder.put(CONFIG_VIP_MIGRATION, 3);
    }

    private void obtainVipThreadParams() {
        List<AwareConfig.Item> awareConfigItemList = getItemList(CONFIG_VIP);
        if (awareConfigItemList != null) {
            this.mVipThreadParams.clear();
            for (AwareConfig.Item item : awareConfigItemList) {
                List<AwareConfig.SubItem> subItemList = getSubItem(item);
                if (subItemList != null) {
                    for (AwareConfig.SubItem subItem : subItemList) {
                        String itemName = subItem.getName();
                        String tempItemValue = subItem.getValue();
                        if (!(itemName == null || tempItemValue == null)) {
                            try {
                                this.mVipThreadParams.put(itemName, Integer.valueOf(Integer.parseInt(tempItemValue)));
                            } catch (NumberFormatException e) {
                                AwareLog.e(TAG, "itemValue string to int error!");
                            }
                        }
                    }
                }
            }
        }
    }
}
