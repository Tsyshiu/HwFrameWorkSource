package com.android.server.rms.iaware.memory.utils;

import android.os.Process;
import android.rms.iaware.AwareLog;

public class CpuReader {
    private static final int CPU_INTERVAL_TIME = 20;
    private static final int INVALID_VALUE = -1;
    private static final int SYSTEM_STAT_DATA_LENGTH = 7;
    private static final int[] SYSTEM_STAT_FORMAT = {288, 8224, 8224, 8224, 8224, 8224, 8224, 8224};
    private static final String TAG = "AwareMem_CpuReader";
    private static CpuReader sReader;
    private static long[] sSystemStatData = new long[7];

    private static class CpuData {
        long mIdleTickTime;
        long mTotalTickTime;

        CpuData() {
            this.mTotalTickTime = 0;
            this.mIdleTickTime = 0;
            this.mTotalTickTime = 0;
            this.mIdleTickTime = 0;
        }
    }

    public static CpuReader getInstance() {
        CpuReader cpuReader;
        synchronized (CpuReader.class) {
            if (sReader == null) {
                sReader = new CpuReader();
            }
            cpuReader = sReader;
        }
        return cpuReader;
    }

    private int sample(CpuData data) {
        long[] sysCpu = sSystemStatData;
        if (!Process.readProcFile("/proc/stat", SYSTEM_STAT_FORMAT, null, sysCpu, null)) {
            AwareLog.w(TAG, "init read /proc/stat error !");
            return -1;
        }
        data.mTotalTickTime = sysCpu[0] + sysCpu[1] + sysCpu[2] + sysCpu[3] + sysCpu[4] + sysCpu[5] + sysCpu[6];
        data.mIdleTickTime = sysCpu[3];
        return 0;
    }

    public final long getCpuPercent() {
        CpuData systemStatData1 = new CpuData();
        if (sample(systemStatData1) != 0) {
            return -1;
        }
        try {
            Thread.sleep(20);
            CpuData systemStatData2 = new CpuData();
            if (sample(systemStatData2) != 0) {
                return -1;
            }
            long totalTickTime = systemStatData2.mTotalTickTime - systemStatData1.mTotalTickTime;
            long idleTickTime = systemStatData2.mIdleTickTime - systemStatData1.mIdleTickTime;
            if (totalTickTime < 0 || idleTickTime < 0 || idleTickTime > totalTickTime) {
                return -1;
            }
            if (totalTickTime == 0) {
                return 0;
            }
            return 100 - ((idleTickTime * 100) / totalTickTime);
        } catch (InterruptedException e) {
            AwareLog.e(TAG, "interrupt error !");
            return -1;
        }
    }
}
