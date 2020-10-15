package com.android.server.security.deviceusage;

import android.util.Log;
import android.util.Slog;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HwDeviceUsageOEMINFO {
    private static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int OEM_INFO_ENABLE_RETREAD = 163;
    private static final int OEM_INFO_ENABLE_RETREAD_SIZE = 40;
    private static final String TAG = "HwDeviceUsageOEMINFO";
    private static HwDeviceUsageOEMINFO sInstance;
    private ByteBuffer mDataBuffer = ByteBuffer.allocate(40);
    private OemInfoData mOemInfoData = new OemInfoData();

    private HwDeviceUsageOEMINFO() {
        getOemInfoData();
    }

    public static synchronized HwDeviceUsageOEMINFO getInstance() {
        HwDeviceUsageOEMINFO hwDeviceUsageOEMINFO;
        synchronized (HwDeviceUsageOEMINFO.class) {
            if (sInstance == null) {
                sInstance = new HwDeviceUsageOEMINFO();
            }
            hwDeviceUsageOEMINFO = sInstance;
        }
        return hwDeviceUsageOEMINFO;
    }

    private void getOemInfoByteArray() {
        byte[] oemInfoBytes = HwOEMInfoAdapter.getByteArrayFromOeminfo(OEM_INFO_ENABLE_RETREAD, 40);
        if (oemInfoBytes == null || oemInfoBytes.length < 40) {
            Slog.e(TAG, "get oem info error");
        } else {
            setByteArrayToDataBuffer(oemInfoBytes);
        }
    }

    public boolean isOpenFlagSet() {
        getOemInfoData();
        boolean isFlagSet = this.mOemInfoData.getOpenFlag() != 0;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "isOpenFlagSet = " + isFlagSet);
        }
        return isFlagSet;
    }

    public long getScreenOnTime() {
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "getScreenOnTime");
        }
        return this.mOemInfoData.getScreenOnTime();
    }

    public long getChargeTime() {
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "getChargeTime");
        }
        return this.mOemInfoData.getChargeTime();
    }

    public long getTalkTime() {
        return this.mOemInfoData.getTalkTime();
    }

    public long getFirstUseTime() {
        return this.mOemInfoData.getFirstUseTime();
    }

    public void setOpenFlag(int flag) {
        this.mOemInfoData.setOpenFlag(flag);
        setDataBuffer();
        HwOEMInfoAdapter.writeByteArrayToOeminfo(OEM_INFO_ENABLE_RETREAD, 40, this.mDataBuffer.array());
    }

    public void setScreenOnTime(long time) {
        this.mOemInfoData.setScreenOnTime(time);
        setDataBuffer();
        HwOEMInfoAdapter.writeByteArrayToOeminfo(OEM_INFO_ENABLE_RETREAD, 40, this.mDataBuffer.array());
    }

    public void setChargeTime(long time) {
        this.mOemInfoData.setChargeTime(time);
        setDataBuffer();
        HwOEMInfoAdapter.writeByteArrayToOeminfo(OEM_INFO_ENABLE_RETREAD, 40, this.mDataBuffer.array());
    }

    public void setTalkTime(long time) {
        this.mOemInfoData.setTalkTime(time);
        setDataBuffer();
        HwOEMInfoAdapter.writeByteArrayToOeminfo(OEM_INFO_ENABLE_RETREAD, 40, this.mDataBuffer.array());
    }

    public int setFirstUseTime(long time) {
        this.mOemInfoData.setFirstUseTime(time);
        setDataBuffer();
        return HwOEMInfoAdapter.writeByteArrayToOeminfo(OEM_INFO_ENABLE_RETREAD, 40, this.mDataBuffer.array());
    }

    private void setDataBuffer() {
        this.mDataBuffer.clear();
        if (this.mDataBuffer.remaining() >= 40) {
            this.mDataBuffer.putInt(this.mOemInfoData.getOpenFlag());
            this.mDataBuffer.putInt(0);
            this.mDataBuffer.putLong(this.mOemInfoData.getScreenOnTime());
            this.mDataBuffer.putLong(this.mOemInfoData.getChargeTime());
            this.mDataBuffer.putLong(this.mOemInfoData.getTalkTime());
            this.mDataBuffer.putLong(this.mOemInfoData.getFirstUseTime());
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "setDataBuffer mOpenFlag = " + this.mOemInfoData.getOpenFlag() + " OemInfoData.mScreenOnTime = " + this.mOemInfoData.getScreenOnTime() + " mChargeTime = " + this.mOemInfoData.getChargeTime() + " mTalkTime = " + this.mOemInfoData.getTalkTime() + " mFirstUseTime = " + this.mOemInfoData.getFirstUseTime());
            }
        }
    }

    private void setByteArrayToDataBuffer(byte[] bytes) {
        this.mDataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.mDataBuffer.clear();
        this.mDataBuffer.put(bytes);
    }

    private void getOemInfoData() {
        getOemInfoByteArray();
        ByteBuffer byteBuffer = this.mDataBuffer;
        if (byteBuffer == null) {
            Slog.e(TAG, "mDataBuffer is null");
            return;
        }
        byteBuffer.flip();
        if (this.mDataBuffer.remaining() >= 40) {
            this.mOemInfoData.setOpenFlag(this.mDataBuffer.getInt());
            this.mDataBuffer.getInt();
            this.mOemInfoData.setScreenOnTime(this.mDataBuffer.getLong());
            this.mOemInfoData.setChargeTime(this.mDataBuffer.getLong());
            this.mOemInfoData.setTalkTime(this.mDataBuffer.getLong());
            this.mOemInfoData.setFirstUseTime(this.mDataBuffer.getLong());
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "getOemInfoData mOpenFlag " + this.mOemInfoData.getOpenFlag() + " OemInfoData.mScreenOnTime " + this.mOemInfoData.getScreenOnTime() + " mChargeTime " + this.mOemInfoData.getChargeTime() + " mTalkTime " + this.mOemInfoData.getTalkTime() + " mFirstUseTime " + this.mOemInfoData.getFirstUseTime());
            }
        }
    }

    private class OemInfoData {
        private long mChargeTime;
        private long mFirstUseTime;
        private int mOpenFlag;
        private long mScreenOnTime;
        private long mTalkTime;

        private OemInfoData() {
        }

        /* access modifiers changed from: private */
        public void setOpenFlag(int flag) {
            this.mOpenFlag = flag;
        }

        /* access modifiers changed from: private */
        public int getOpenFlag() {
            return this.mOpenFlag;
        }

        /* access modifiers changed from: private */
        public void setScreenOnTime(long time) {
            this.mScreenOnTime = time;
        }

        /* access modifiers changed from: private */
        public long getScreenOnTime() {
            return this.mScreenOnTime;
        }

        /* access modifiers changed from: private */
        public void setChargeTime(long time) {
            this.mChargeTime = time;
        }

        /* access modifiers changed from: private */
        public long getChargeTime() {
            return this.mChargeTime;
        }

        /* access modifiers changed from: private */
        public void setTalkTime(long time) {
            this.mTalkTime = time;
        }

        /* access modifiers changed from: private */
        public long getTalkTime() {
            return this.mTalkTime;
        }

        /* access modifiers changed from: private */
        public void setFirstUseTime(long time) {
            this.mFirstUseTime = time;
        }

        /* access modifiers changed from: private */
        public long getFirstUseTime() {
            return this.mFirstUseTime;
        }
    }
}
