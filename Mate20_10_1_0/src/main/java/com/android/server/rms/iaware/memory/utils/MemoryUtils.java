package com.android.server.rms.iaware.memory.utils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwaredConnection;
import android.system.ErrnoException;
import android.system.Int32Ref;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.util.MemInfoReader;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.rms.collector.ResourceCollector;
import com.android.server.rms.iaware.feature.MemoryFeature2;
import com.android.server.rms.iaware.memory.policy.DMEServer;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MemoryUtils {
    private static final long FLUSH_TIMEOUT = 2000;
    private static final int INTEGER_BYTE_LENTH = 4;
    private static final int MAX_RECV_BYTE_BUFFER_LENTH = 8;
    private static final int MAX_SEND_BYTE_BUFFER_LENTH = 256;
    private static final String MEMORY_SOCKET = "iawared";
    private static final int[] PROCESS_FULL_STATS_FORMAT = {32, 4640, 32, 32, 32, 32, 32, 32, 32, 8224, 32, 8224, 32, 8224, 8224, 32, 32, 32, 32, 32, 32, 32, 8224};
    private static final int PROCESS_PROC_OUT_LONG = 8192;
    private static final int PROCESS_PROC_OUT_STRING = 4096;
    private static final int PROCESS_PROC_PARENS = 512;
    private static final String PROCESS_PROC_PATH = "/proc";
    private static final int PROCESS_PROC_SPACE_TERM = 32;
    private static final int PROCESS_PROC_STATS_STRING_LENGTH = 6;
    private static final String PROCESS_STAT_PATH = "stat";
    private static final String TAG = "AwareMem_MemoryUtils";
    private static InputStream mInputStream;
    private static final Object mLock = new Object();
    private static LocalSocket mMemorySocket;
    private static OutputStream mOutputStream;
    private static long sLastRamSize = 0;
    private static boolean sOpenProtectAlways = false;
    private static boolean sOpenProtectAlwaysConfig = false;

    public static AwareAppMngSortPolicy getAppMngSortPolicy(int resourceType, int groupId) {
        return getAppMngSortPolicy(resourceType, groupId, 0);
    }

    public static AwareAppMngSortPolicy getAppMngSortPolicy(int resourceType, int groupId, int subType) {
        AwareAppMngSort sorted;
        if (AwareAppMngSort.checkAppMngEnable() && groupId >= 0 && groupId <= 3 && (sorted = AwareAppMngSort.getInstance()) != null) {
            return sorted.getAppMngSortPolicy(resourceType, subType, groupId);
        }
        return null;
    }

    public static AwareAppMngSortPolicy getAppMngSortPolicyForMemRepair(int sceneType) {
        AwareAppMngSort sorted;
        if (AwareAppMngSort.checkAppMngEnable() && (sorted = AwareAppMngSort.getInstance()) != null) {
            return sorted.getAppMngSortPolicyForMemRepair(sceneType);
        }
        return null;
    }

    public static AwareAppMngSortPolicy getAppSortPolicyForMemRepairVss(int sceneType, AwareAppMngSortPolicy pssPolicy) {
        AwareAppMngSort sorted;
        if (AwareAppMngSort.checkAppMngEnable() && (sorted = AwareAppMngSort.getInstance()) != null) {
            return sorted.getAppMngSortPolicyForMemRepairVss(sceneType, pssPolicy);
        }
        return null;
    }

    public static List<AwareProcessInfo> getAppMngSortPolicyForSystemTrim() {
        AwareAppMngSort sorted;
        if (AwareAppMngSort.checkAppMngEnable() && (sorted = AwareAppMngSort.getInstance()) != null) {
            return sorted.getAppMngSortPolicyForSystemTrim();
        }
        return null;
    }

    public static List<AwareProcessBlockInfo> getAppMngProcGroup(AwareAppMngSortPolicy policy, int groupId) {
        if (policy == null) {
            AwareLog.e(TAG, "getAppMngProcGroup sort policy null!");
            return null;
        } else if (groupId == 0) {
            return policy.getForbidStopProcBlockList();
        } else {
            if (groupId == 1) {
                return policy.getShortageStopProcBlockList();
            }
            if (groupId == 2) {
                return policy.getAllowStopProcBlockList();
            }
            AwareLog.w(TAG, "getAppMngProcGroup unknown group id!");
            return null;
        }
    }

    public static int killProcessGroupForQuickKill(int uid, int pid) {
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            return ResourceCollector.killProcessGroupForQuickKill(uid, pid);
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    public static void writeSwappiness(int swappiness) {
        if (swappiness > 200 || swappiness < 0) {
            AwareLog.w(TAG, "invalid swappiness value");
            return;
        }
        AwareLog.i(TAG, "setSwappiness = " + swappiness);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(302);
        buffer.putInt(swappiness);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void writeDirectSwappiness(int directswappiness) {
        if (directswappiness > 200 || directswappiness < 0) {
            AwareLog.w(TAG, "invalid directswappiness value");
            return;
        }
        AwareLog.i(TAG, "setDirectSwappiness = " + directswappiness);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_DIRECT_SWAPPINESS);
        buffer.putInt(directswappiness);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void configQosMemoryWatermark() {
        String configVal = MemoryConstant.getConfigQosMemoryWatermark();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        try {
            byte[] stringBytes = configVal.getBytes("UTF-8");
            if (stringBytes.length >= 1) {
                if (stringBytes.length <= 7) {
                    buffer.clear();
                    buffer.putInt(MemoryConstant.MSG_CONFIG_QOS_MEMORY_WATERMARK);
                    buffer.putInt(stringBytes.length);
                    buffer.put(stringBytes);
                    buffer.putChar(0);
                    IAwaredConnection.getInstance().sendPacket(buffer.array());
                    MemoryConstant.setQosMemoryConfigState(true);
                    return;
                }
            }
            AwareLog.w(TAG, "configQosMemory incorrect val");
        } catch (UnsupportedEncodingException e) {
            AwareLog.w(TAG, "setPackageProtectLru UTF-8 not supported");
        }
    }

    public static void setQosMemorySwitch(int on) {
        if (on == 1) {
            if (MemoryConstant.getConfigQosMemorySwitch() == 1) {
                if (!MemoryConstant.getQosMemoryConfigState()) {
                    configQosMemoryWatermark();
                }
            } else {
                return;
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_CONFIG_QOS_MEMORY_SWITCH);
        buffer.putInt(on);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void setEglInitOptSwitch(int on) {
        if (MemoryConstant.getEglInitOptConfigSwitch() == 1) {
            String val = on == 1 ? "true" : "false";
            try {
                SystemProperties.set("sys.eglinit_opt", val);
            } catch (RuntimeException e) {
                AwareLog.w(TAG, "fail to set sys.eglinit_opt to " + val);
            }
        }
    }

    public static void writeExtraFreeKbytes(int extrafreekbytes) {
        if (extrafreekbytes <= 0 || extrafreekbytes >= 200000) {
            AwareLog.w(TAG, "invalid extrafreekbytes value");
            return;
        }
        int lastExtraFreeKbytes = SystemProperties.getInt("sys.sysctl.extra_free_kbytes", (int) MemoryConstant.PROCESSLIST_EXTRA_FREE_KBYTES);
        if (lastExtraFreeKbytes == extrafreekbytes) {
            AwareLog.d(TAG, "extrafreekbytes is already " + lastExtraFreeKbytes + ", no need to set");
            return;
        }
        SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(extrafreekbytes));
    }

    private static void configProtectLru() {
        setProtectLruLimit(MemoryConstant.getConfigProtectLruLimit());
        setProtectLruRatio(MemoryConstant.getConfigProtectLruRatio());
        AwareLog.d(TAG, "onProtectLruConfigUpdate");
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_PROTECTLRU_CONFIG_UPDATE);
        buffer.putInt(0);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
        setFileProtectLru(304);
        dynamicSetProtectLru(DMEServer.getInstance().getProtectLruState());
        sOpenProtectAlwaysConfig = MemoryConstant.getConfigProtectLruSwitch();
    }

    public static void enableProtectLru() {
        if (!sOpenProtectAlways) {
            setProtectLruSwitch(true);
            if (sOpenProtectAlwaysConfig) {
                sOpenProtectAlways = true;
            }
        }
    }

    public static void disableProtectLru() {
        if (!sOpenProtectAlways) {
            setProtectLruSwitch(false);
        }
    }

    public static void onProtectLruConfigUpdate() {
        configProtectLru();
        if (sOpenProtectAlwaysConfig) {
            sOpenProtectAlways = false;
            enableProtectLru();
            return;
        }
        disableProtectLru();
    }

    public static void dynamicSetProtectLru(int state) {
        if (state == 1) {
            enableProtectLru();
        } else if (state == 0) {
            disableProtectLru();
        }
    }

    private static void setProtectLruRatio(int ratio) {
        AwareLog.d(TAG, "set ProtectLru ratio = " + ratio);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO);
        buffer.putInt(ratio);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void reclaimProcessAll(int pid, boolean suspend) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        if (suspend) {
            buffer.putInt(MemoryConstant.MSG_PROCRECLAIM_ALL_SUSPEND);
        } else {
            buffer.putInt(MemoryConstant.MSG_PROCRECLAIM_ALL);
        }
        buffer.putInt(pid);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    private static void setFileProtectLru(int commandType) {
        ArrayMap<Integer, ArraySet<String>> filterMap = MemoryConstant.getFileCacheMap();
        if (filterMap != null) {
            AwareLog.i(TAG, "set ProtectLru filterMap size:" + filterMap.size());
            ByteBuffer buffer = ByteBuffer.allocate(272);
            int i = 0;
            try {
                int mapSize = filterMap.size();
                while (i < mapSize) {
                    int index = filterMap.keyAt(i).intValue();
                    int isDir = 0;
                    if (index > 50) {
                        index -= 50;
                        isDir = 1;
                    }
                    ArraySet<String> filterSet = filterMap.valueAt(i);
                    if (filterSet != null) {
                        Iterator<String> it = filterSet.iterator();
                        while (it.hasNext()) {
                            String filterStr = it.next();
                            if (!TextUtils.isEmpty(filterStr)) {
                                byte[] stringBytes = filterStr.getBytes("UTF-8");
                                if (stringBytes.length >= 1) {
                                    if (stringBytes.length <= 255) {
                                        AwareLog.d(TAG, "setPackageProtectLru filterStr = " + filterStr);
                                        buffer.clear();
                                        buffer.putInt(commandType);
                                        buffer.putInt(isDir);
                                        buffer.putInt(index);
                                        buffer.putInt(stringBytes.length);
                                        buffer.put(stringBytes);
                                        buffer.putChar(0);
                                        if (sendPacket(buffer) != 0) {
                                            AwareLog.w(TAG, "setPackageProtectLru sendPacket failed");
                                        }
                                    }
                                }
                                AwareLog.w(TAG, "setPackageProtectLru incorrect filterStr = " + filterStr);
                            }
                        }
                        i++;
                    } else {
                        return;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                AwareLog.w(TAG, "setPackageProtectLru UTF-8 not supported");
            }
        }
    }

    private static void setProtectLruLimit(String lruConfigStr) {
        if (checkLimitConfigStr(lruConfigStr)) {
            ByteBuffer buffer = ByteBuffer.allocate(268);
            try {
                byte[] stringBytes = lruConfigStr.getBytes("UTF-8");
                if (stringBytes.length >= 1) {
                    if (stringBytes.length <= 255) {
                        AwareLog.d(TAG, "setProtectLruLimit configstr=" + lruConfigStr);
                        buffer.clear();
                        buffer.putInt(305);
                        buffer.putInt(stringBytes.length);
                        buffer.put(stringBytes);
                        buffer.putChar(0);
                        if (sendPacket(buffer) != 0) {
                            AwareLog.w(TAG, "setProtectLruLimit sendPacket failed");
                            return;
                        }
                        return;
                    }
                }
                AwareLog.w(TAG, "setProtectLruLimit incorrect config = " + lruConfigStr);
            } catch (UnsupportedEncodingException e) {
                AwareLog.w(TAG, "setProtectLruLimit UTF-8 not supported?!?");
            }
        }
    }

    private static boolean checkLimitConfigStr(String lruConfigStr) {
        if (lruConfigStr == null) {
            return false;
        }
        String[] lruConfigStrArray = lruConfigStr.split(" ");
        if (lruConfigStrArray.length != 3) {
            return false;
        }
        int i = 0;
        while (i < lruConfigStrArray.length) {
            try {
                int levelValue = Integer.parseInt(lruConfigStrArray[i]);
                if (levelValue >= 0) {
                    if (levelValue <= 100) {
                        i++;
                    }
                }
                AwareLog.w(TAG, "protect lru level value is invalid: " + levelValue);
                return false;
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "protectlru limit parse error");
                return false;
            }
        }
        return true;
    }

    private static void setProtectLruSwitch(boolean isEnable) {
        AwareLog.d(TAG, "set ProtectLru switch = " + isEnable);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_PROTECTLRU_SWITCH);
        buffer.putInt(isEnable ? 1 : 0);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static boolean checkRamSize(String ramSize, Long totalMemMb) {
        if (ramSize == null) {
            return false;
        }
        try {
            long ramSizeL = Long.parseLong(ramSize.trim());
            if (totalMemMb.longValue() > ramSizeL || totalMemMb.longValue() <= sLastRamSize) {
                sLastRamSize = ramSizeL;
                return false;
            }
            sLastRamSize = ramSizeL;
            return true;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse ramsze error: " + e);
            return false;
        }
    }

    public static void initialRamSizeLowerBound() {
        sLastRamSize = 0;
    }

    public static void rccCompress(long reqkb) {
        int compressMb = (int) (((long) (((double) reqkb) / 0.65d)) / 1024);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_RCC_COMPRESS);
        buffer.putInt(compressMb);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
        AwareLog.i(TAG, "rcc compress " + compressMb + "mb");
    }

    public static void rccIdleCompressEnable(boolean enable) {
        AwareLog.i(TAG, "rcc rccIdleCompressEnable");
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_RCC_IDLE_COMPRESS_SWITCH);
        buffer.putInt(enable ? 1 : 0);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void rccSetIdleThreshold(int percent) {
        AwareLog.i(TAG, "rcc rccSetIdleThreshold");
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_RCC_IDLE_THRESHOLD);
        buffer.putInt(percent);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void rccSetAvailTarget(int target) {
        AwareLog.i(TAG, "rcc rccSetAvailTarget");
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_RCC_AVAIL_TARGET);
        buffer.putInt(target);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void rccSetAnonTarget(int target) {
        if (target >= 0) {
            AwareLog.i(TAG, "rcc rccSetAnonTarget");
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putInt(MemoryConstant.MSG_RCC_ANON_TARGET);
            buffer.putInt(target);
            IAwaredConnection.getInstance().sendPacket(buffer.array());
        }
    }

    public static void rccSetSwapPercent(int target) {
        if (target >= 0) {
            AwareLog.i(TAG, "rcc rccSetSwapPercent");
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putInt(MemoryConstant.MSG_RCC_ZRAM_PERCENT_LOW);
            buffer.putInt(target);
            IAwaredConnection.getInstance().sendPacket(buffer.array());
        }
    }

    public static void rccPause() {
        AwareLog.i(TAG, "rcc pause");
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(MemoryConstant.MSG_RCC_PAUSE);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void setReclaimGPUMemory(boolean isCompress, int pid) {
        StringBuilder sb = new StringBuilder();
        sb.append(isCompress ? "compress" : "decompress");
        sb.append(" GPU memory, pid:");
        sb.append(pid);
        AwareLog.d(TAG, sb.toString());
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(MemoryConstant.MSG_COMPRESS_GPU);
        buffer.putInt(isCompress ? 1 : 0);
        buffer.putInt(pid);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static boolean trimMemory(HwActivityManagerService hwAms, String proc, int level) {
        return trimMemory(hwAms, proc, -2, level, true);
    }

    public static boolean trimMemory(HwActivityManagerService hwAms, String proc, int userId, int level, boolean fromIAware) {
        if (hwAms == null) {
            return false;
        }
        AwareLog.d(TAG, "trim Memory, proc:" + proc + " userId:" + userId + " level:" + level + " fromIAware:" + fromIAware);
        try {
            return hwAms.setProcessMemoryTrimLevel(proc, userId, level, fromIAware);
        } catch (RemoteException e) {
            AwareLog.w(TAG, "trim Memory remote exception, proc:" + proc + " userId:" + userId + " level:" + level + " fromIAware:" + fromIAware);
            return false;
        } catch (IllegalArgumentException e2) {
            AwareLog.w(TAG, "trim Memory illegal arg exception, proc:" + proc + " userId:" + userId + " level:" + level + " fromIAware:" + fromIAware);
            return false;
        }
    }

    public static void sendActivityDisplayedTime(String activityName, int pid, int time) {
        ByteBuffer buffer = ByteBuffer.allocate(276);
        try {
            byte[] stringBytes = activityName.getBytes("UTF-8");
            if (stringBytes.length >= 1) {
                if (stringBytes.length <= 255) {
                    AwareLog.d(TAG, "sendActivityDisplayedTime: " + activityName + " " + pid + " " + time);
                    buffer.clear();
                    buffer.putInt(MemoryConstant.MSG_ACTIVITY_DISPLAY_STATISTICS);
                    buffer.putInt(pid);
                    buffer.putInt(time);
                    buffer.putInt(stringBytes.length);
                    buffer.put(stringBytes);
                    buffer.putChar(0);
                    if (sendPacket(buffer) != 0) {
                        AwareLog.w(TAG, "sendActivityDisplayedTime sendPacket failed");
                    }
                    return;
                }
            }
            AwareLog.w(TAG, "sendActivityDisplayedTime incorrect activityName = " + activityName);
        } catch (UnsupportedEncodingException e) {
            AwareLog.w(TAG, "sendActivityDisplayedTime UTF-8 not supported?!?");
        }
    }

    public static void writeMMonitorSwitch(int switchValue) {
        if (switchValue > 1 || switchValue < 0) {
            AwareLog.w(TAG, "invalid mmonitor switch value");
            return;
        }
        AwareLog.i(TAG, "writeMMonitorSwitch = " + switchValue);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_MMONITOR_SWITCH);
        buffer.putInt(switchValue);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    private static void createSocket() {
        if (mMemorySocket == null) {
            try {
                mMemorySocket = new LocalSocket(3);
                mMemorySocket.connect(new LocalSocketAddress("iawared", LocalSocketAddress.Namespace.RESERVED));
                mOutputStream = mMemorySocket.getOutputStream();
                mInputStream = mMemorySocket.getInputStream();
                mMemorySocket.setReceiveBufferSize(8);
                AwareLog.d(TAG, "createSocket Success!");
            } catch (IOException e) {
                AwareLog.e(TAG, "createSocket happend IOException");
                destroySocket();
            }
        }
    }

    public static void destroySocket() {
        synchronized (mLock) {
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch (IOException e) {
                    AwareLog.e(TAG, "mOutputStream close failed! happend IOException");
                }
                mOutputStream = null;
            }
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e2) {
                    AwareLog.e(TAG, "mInputStream close failed! happend IOException");
                }
                mInputStream = null;
            }
            if (mMemorySocket != null) {
                try {
                    mMemorySocket.close();
                } catch (IOException e3) {
                    AwareLog.e(TAG, "closeSocket failed! happend IOException");
                }
                mMemorySocket = null;
            }
        }
    }

    public static int sendPacket(ByteBuffer buffer) {
        synchronized (mLock) {
            if (buffer == null) {
                try {
                    AwareLog.w(TAG, "sendPacket ByteBuffer is null!");
                    return -1;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                int retry = 2;
                do {
                    if (mMemorySocket == null) {
                        createSocket();
                    }
                    if (mOutputStream != null) {
                        try {
                            mOutputStream.write(buffer.array(), 0, buffer.position());
                            flush(FLUSH_TIMEOUT);
                            return 0;
                        } catch (IOException e) {
                            AwareLog.e(TAG, "mOutputStream write failed! happend IOException");
                            destroySocket();
                            retry--;
                            continue;
                        }
                    }
                } while (retry > 0);
                return -1;
            }
        }
    }

    private static void flush(long millis) throws IOException {
        FileDescriptor myFd = mMemorySocket.getFileDescriptor();
        if (myFd != null) {
            long start = SystemClock.uptimeMillis();
            Int32Ref pending = new Int32Ref(0);
            while (true) {
                try {
                    Os.ioctlInt(myFd, OsConstants.TIOCOUTQ, pending);
                    if (pending.value > 0) {
                        if (SystemClock.uptimeMillis() - start < millis) {
                            int left = pending.value;
                            if (left <= 1000) {
                                try {
                                    Thread.sleep(0, 10);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            } else if (left <= 5000) {
                                Thread.sleep(0, 500);
                            } else {
                                Thread.sleep(1);
                            }
                        } else {
                            AwareLog.e(TAG, "Socket flush timed out !!!");
                            throw new IOException("flush timeout");
                        }
                    } else {
                        return;
                    }
                } catch (ErrnoException e2) {
                    throw e2.rethrowAsIOException();
                }
            }
        } else {
            throw new IOException("socket closed");
        }
    }

    public static byte[] recvPacket(int byteSize) {
        byte[] emptyByte = new byte[0];
        if (byteSize <= 0 || mInputStream == null) {
            return emptyByte;
        }
        byte[] recvByte = new byte[byteSize];
        try {
            Arrays.fill(recvByte, (byte) 0);
            if (mInputStream.read(recvByte) == 0) {
                return emptyByte;
            }
            return recvByte;
        } catch (IOException e) {
            AwareLog.e(TAG, " mInputStream write failed happend IOException!");
            destroySocket();
            return emptyByte;
        }
    }

    public static void enterSpecialSceneNotify(int total_watermark, int worker_mask, int autostop_timeout) {
        if (MemoryConstant.getConfigIonSpeedupSwitch() == 0) {
            AwareLog.w(TAG, "iaware2.0 mem feature camera ion memory speedup switch off");
        } else {
            doEnterSpecialSceneNotify(total_watermark, worker_mask, autostop_timeout);
        }
    }

    public static void doEnterSpecialSceneNotify(int total_watermark, int worker_mask, int autostop_timeout) {
        if (!MemoryFeature2.isUpMemoryFeature.get()) {
            AwareLog.w(TAG, "iaware2.0 mem feature  " + MemoryFeature2.isUpMemoryFeature.get());
            return;
        }
        AwareLog.i(TAG, "Enter special scene, total_watermark: " + total_watermark + ", worker_mask: " + worker_mask + ", autostop_timeout: " + autostop_timeout);
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(340);
        buffer.putInt(total_watermark);
        buffer.putInt(worker_mask);
        buffer.putInt(autostop_timeout);
        sendPacket(buffer);
    }

    public static void exitSpecialSceneNotify() {
        if (MemoryConstant.getConfigIonSpeedupSwitch() == 0) {
            AwareLog.w(TAG, "iaware2.0 mem feature camera ion memory speedup switch off...");
        } else {
            doExitSpecialSceneNotify();
        }
    }

    public static void doExitSpecialSceneNotify() {
        if (!MemoryFeature2.isUpMemoryFeature.get()) {
            AwareLog.w(TAG, "iaware2.0 mem feature " + MemoryFeature2.isUpMemoryFeature.get());
            return;
        }
        AwareLog.i(TAG, "Exit special scene");
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(MemoryConstant.MSG_SPECIAL_SCENE_POOL_EXIT);
        sendPacket(buffer);
    }

    public static AwareConfig.Item getCurrentMemItem(AwareConfig configList, boolean isCust) {
        AwareConfig.Item lastItem;
        String ramSize;
        if (configList == null || configList.getConfigList() == null) {
            AwareLog.w(TAG, "loadMemConfig fail, null configList");
            return null;
        }
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        Long totalMemMb = Long.valueOf(minfo.getTotalSize() / 1048576);
        List<AwareConfig.Item> itemList = configList.getConfigList();
        initialRamSizeLowerBound();
        for (AwareConfig.Item item : itemList) {
            if (item == null || item.getProperties() == null) {
                AwareLog.d(TAG, "loadMemConstantConfig continue cause null item");
            } else {
                String ramSize2 = item.getProperties().get(MemoryConstant.MEM_CONSTANT_RAMSIZENAME);
                if (checkRamSize(ramSize2, totalMemMb)) {
                    AwareLog.i(TAG, "loadMemConfig success. ramSize: " + ramSize2 + " totalMemMb: " + totalMemMb);
                    return item;
                }
            }
        }
        if (isCust || itemList.size() <= 0 || (lastItem = itemList.get(itemList.size() - 1)) == null || lastItem.getProperties() == null || (ramSize = lastItem.getProperties().get(MemoryConstant.MEM_CONSTANT_RAMSIZENAME)) == null) {
            return null;
        }
        try {
            if (totalMemMb.longValue() > Long.parseLong(ramSize.trim())) {
                AwareLog.i(TAG, "inheriteMemConfig success. ramSize: " + ramSize + " totalMemMb: " + totalMemMb);
                return lastItem;
            }
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse ramsize error");
        }
        return null;
    }

    public static int getNativeRelatedPid(String processName) {
        int[] pids = Process.getPids(PROCESS_PROC_PATH, null);
        if (pids == null) {
            return -1;
        }
        int length = pids.length;
        int i = 0;
        while (i < length) {
            int pid = pids[i];
            if (pid < 0) {
                break;
            }
            String[] procStatsString = new String[6];
            if (!Process.readProcFile(new File(new File(PROCESS_PROC_PATH, Integer.toString(pid)), PROCESS_STAT_PATH).toString(), PROCESS_FULL_STATS_FORMAT, procStatsString, new long[6], null) || !procStatsString[0].equals(processName)) {
                i++;
            } else {
                AwareLog.d(TAG, "getNativeRelated pid=" + pid);
                return pid;
            }
        }
        return -1;
    }

    public static void setTopnProcMem(int topN) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_TOPN_PROCESS_MEM);
        buffer.putInt(topN);
        AwareLog.i(TAG, "set process memory topn:" + topN);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void sortByTimeIfNeed(List<AwareProcessBlockInfo> procs) {
        AwareAppMngSort sorted;
        if (AwareAppMngSort.checkAppMngEnable() && (sorted = AwareAppMngSort.getInstance()) != null) {
            sorted.sortByTimeIfNeed(procs);
        }
    }
}
