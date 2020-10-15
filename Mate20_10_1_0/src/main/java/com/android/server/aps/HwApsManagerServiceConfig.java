package com.android.server.aps;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.aps.ApsAppInfo;
import android.aps.IApsManagerServiceCallback;
import android.common.HwFrameworkFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import android.vrsystem.IVRSystemServiceManager;
import com.android.internal.util.FastXmlSerializer;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hwaps.HwApsImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class HwApsManagerServiceConfig {
    private static final int APS_MSG_FORCESTOP_APK = 310;
    private static final int APS_MSG_WRITE = 300;
    private static final String BRIGHTNESS_PERCENT_STRING = "brightness";
    private static final String COMPAT_FILE_MAIN_TAG = "hwaps-compat-packages";
    private static final String COMPAT_FILE_NAME_TAG = "name";
    private static final String COMPAT_FILE_PACKAGE_TAG = "pkg";
    private static final int COMPAT_WRITE_WAIT_TIME = 10000;
    private static final String FRAME_RATE_STRING = "fps";
    private static final String GET_FPS_SOURCE_CONFIG = "Aps Config";
    private static final String GET_FPS_SOURCE_SERVICE = "Aps Service";
    private static final String GET_FPS_SOURCE_UNKOWN = "unkown source";
    private static final String KIRIN_PLATFORM_970 = "kirin970";
    private static final String MAX_FRAME_RATE_STRING = "maxfps";
    private static final String OLD_BRIGHTNESS_PERCENT_STRING = "brightnesspercent";
    private static final String OLD_FRAME_RATE_STRING = "framerate";
    private static final String OLD_MAX_FRAME_RATE_STRING = "maxframerate";
    private static final String OLD_RESOLUTION_RATIO_STRING = "resolutionratio";
    private static final String OLD_SWITCHABLE_STRING = "switchable";
    private static final String OLD_TEXTURE_PERCENT_STRING = "texturepercent";
    private static final String PROP_KIRIN_PLATFORM = "ro.board.platform";
    private static final String RESOLUTION_RATIO_STRING = "ratio";
    private static final String SWITCHABLE_STRING = "switch";
    private static final String TAG = "HwApsManagerConfig";
    private static final String TEXTURE_PERCENT_STRING = "texture";
    private static final float VR_APP_RATIO = 0.625f;
    private static final String mHwApsPackagesListNewPath = "/data/system/packages-cache.dat";
    private static final String mHwApsPackagesListPath = "/data/system/hwaps-packages-compat.xml";
    private static int mLowResolutionMode = 0;
    private final HwApsManagerService mApsService;
    private int mApsSupportValue;
    private final AtomicFile mFile;
    private final ApsHandler mHandler;
    private boolean mIsSmartAlwaysSwitchable = true;
    private final ConcurrentHashMap<String, Float> mNewResolutionRatioMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ApsAppInfo> mPackages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, IApsManagerServiceCallback> mPkgnameCallbackMap = new ConcurrentHashMap<>();
    private IVRSystemServiceManager mVrMananger;
    private ConcurrentHashMap<String, Integer> newFpsMap = new ConcurrentHashMap<>();

    private final class ApsHandler extends Handler {
        public ApsHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 300) {
                HwApsManagerServiceConfig.this.saveApsAppInfo();
            } else if (i == HwApsManagerServiceConfig.APS_MSG_FORCESTOP_APK) {
                HwApsManagerServiceConfig.this.stopApsCompatPackages();
            }
        }
    }

    public HwApsManagerServiceConfig(HwApsManagerService service, Handler handler) {
        this.mApsSupportValue = 0;
        mLowResolutionMode = service.getLowResolutionSwitchState();
        this.mIsSmartAlwaysSwitchable = HwApsImpl.getDefault().isAllLowResolutionSwitchable();
        this.mApsSupportValue = SystemProperties.getInt("sys.aps.support", 0);
        this.mVrMananger = HwFrameworkFactory.getVRSystemServiceManager();
        this.mHandler = new ApsHandler(handler.getLooper());
        this.mApsService = service;
        this.mFile = new AtomicFile(new File(mHwApsPackagesListNewPath));
        File oldFile = new File(mHwApsPackagesListPath);
        if (oldFile.exists()) {
            processOldCompatFileExist(oldFile);
            return;
        }
        FileInputStream fis = null;
        try {
            FileInputStream fis2 = this.mFile.openRead();
            byte[] bArray = new byte[fis2.available()];
            fis2.read(bArray);
            for (int i = 0; i < bArray.length; i++) {
                bArray[i] = (byte) (~bArray[i]);
            }
            readCompatXmlIntoMap(new ByteArrayInputStream(bArray), new String[]{RESOLUTION_RATIO_STRING, FRAME_RATE_STRING, MAX_FRAME_RATE_STRING, TEXTURE_PERCENT_STRING, BRIGHTNESS_PERCENT_STRING, "switch"});
            try {
                fis2.close();
            } catch (IOException e) {
                Slog.w(TAG, "Error reading hwaps-compat-packages, IOException is thrown when closing fis.");
            }
        } catch (IOException e2) {
            if (0 != 0) {
                Slog.w(TAG, "Error reading hwaps-compat-packages, IOException is thrown in ctor");
            }
            if (0 != 0) {
                fis.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e3) {
                    Slog.w(TAG, "Error reading hwaps-compat-packages, IOException is thrown when closing fis.");
                }
            }
            throw th;
        }
    }

    private void processOldCompatFileExist(File oldFile) {
        FileInputStream fis = null;
        try {
            fis = new AtomicFile(oldFile).openRead();
            readCompatXmlIntoMap(fis, new String[]{OLD_RESOLUTION_RATIO_STRING, OLD_FRAME_RATE_STRING, OLD_MAX_FRAME_RATE_STRING, OLD_TEXTURE_PERCENT_STRING, OLD_BRIGHTNESS_PERCENT_STRING, OLD_SWITCHABLE_STRING});
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Error closing stream.");
                }
            }
        } catch (IOException e2) {
            if (fis != null) {
                Slog.w(TAG, "Error reading hwaps-compat-packages. io exception is thrown");
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e3) {
                    Slog.w(TAG, "Error closing stream.");
                }
            }
            throw th;
        }
        saveApsAppInfo();
        oldFile.delete();
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0032 A[Catch:{ XmlPullParserException -> 0x011b, IOException -> 0x0112 }, RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0033 A[Catch:{ XmlPullParserException -> 0x011b, IOException -> 0x0112 }] */
    private void readCompatXmlIntoMap(InputStream is, String[] tags) {
        boolean switchableBoolean;
        int brightnesspercentInt;
        int texturepercentInt;
        int maxframerateInt;
        int framerateInt;
        float resolutionratioFloat;
        if (is != null && tags != null) {
            if (tags.length >= 6) {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(is, StandardCharsets.UTF_8.name());
                    int eventType = parser.getEventType();
                    while (true) {
                        char c = 1;
                        int i = 2;
                        if (eventType != 2 && eventType != 1) {
                            eventType = parser.next();
                        } else if (eventType == 1) {
                            if (COMPAT_FILE_MAIN_TAG.equals(parser.getName())) {
                                int eventType2 = parser.next();
                                while (true) {
                                    if (eventType2 == i) {
                                        String tagName = parser.getName();
                                        if (parser.getDepth() == i) {
                                            if ("pkg".equals(tagName)) {
                                                String pkg = parser.getAttributeValue(null, "name");
                                                String resolutionratio = parser.getAttributeValue(null, tags[0]);
                                                String framerate = parser.getAttributeValue(null, tags[c]);
                                                String maxframerate = parser.getAttributeValue(null, tags[i]);
                                                String texturepercent = parser.getAttributeValue(null, tags[3]);
                                                String brightnesspercent = parser.getAttributeValue(null, tags[4]);
                                                String switchable = parser.getAttributeValue(null, tags[5]);
                                                float resolutionratioFloat2 = 0.0f;
                                                int framerateInt2 = 0;
                                                int maxframerateInt2 = 0;
                                                int texturepercentInt2 = 0;
                                                int brightnesspercentInt2 = 0;
                                                try {
                                                    resolutionratioFloat2 = Float.parseFloat(resolutionratio);
                                                    framerateInt2 = Integer.parseInt(framerate);
                                                    maxframerateInt2 = Integer.parseInt(maxframerate);
                                                    texturepercentInt2 = Integer.parseInt(texturepercent);
                                                    brightnesspercentInt2 = Integer.parseInt(brightnesspercent);
                                                    switchableBoolean = Boolean.parseBoolean(switchable);
                                                    resolutionratioFloat = resolutionratioFloat2;
                                                    framerateInt = framerateInt2;
                                                    maxframerateInt = maxframerateInt2;
                                                    texturepercentInt = texturepercentInt2;
                                                    brightnesspercentInt = brightnesspercentInt2;
                                                } catch (NumberFormatException e) {
                                                    Slog.w(TAG, "NumberFormatException is thrown.");
                                                    resolutionratioFloat = resolutionratioFloat2;
                                                    framerateInt = framerateInt2;
                                                    maxframerateInt = maxframerateInt2;
                                                    texturepercentInt = texturepercentInt2;
                                                    brightnesspercentInt = brightnesspercentInt2;
                                                    switchableBoolean = true;
                                                }
                                                try {
                                                    this.mPackages.put(pkg, new ApsAppInfo(pkg, resolutionratioFloat, framerateInt, maxframerateInt, texturepercentInt, brightnesspercentInt, switchableBoolean));
                                                } catch (XmlPullParserException e2) {
                                                    Slog.w(TAG, "Error reading hwaps-compat-packages, XmlPullParserException is thrown");
                                                } catch (IOException e3) {
                                                    Slog.w(TAG, "Error reading hwaps-compat-packages, IOException is thrown");
                                                }
                                            }
                                        }
                                    }
                                    eventType2 = parser.next();
                                    c = 1;
                                    if (eventType2 != 1) {
                                        i = 2;
                                    } else {
                                        return;
                                    }
                                }
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    if (eventType == 1) {
                    }
                } catch (XmlPullParserException e4) {
                    Slog.w(TAG, "Error reading hwaps-compat-packages, XmlPullParserException is thrown");
                } catch (IOException e5) {
                    Slog.w(TAG, "Error reading hwaps-compat-packages, IOException is thrown");
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void stopApsCompatPackages() {
        ApsAppInfo info;
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            for (String pkgName : this.mPackages.keySet()) {
                if (!(pkgName == null || (info = this.mPackages.get(pkgName)) == null || Float.compare(info.getResolutionRatio(), 1.0f) == 0)) {
                    boolean configuredSwitchable = info.getSwitchable();
                    if (configuredSwitchable || (!configuredSwitchable && this.mIsSmartAlwaysSwitchable)) {
                        try {
                            Slog.i(TAG, "stopApsCompatPackages pkgName = " + pkgName);
                            am.forceStopPackage(pkgName, -1);
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Failed to kill aps package of " + pkgName);
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void stopAllAppsInLowResolution() {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            for (Map.Entry<String, ApsAppInfo> entry : this.mPackages.entrySet()) {
                String pkgName = entry.getKey();
                ApsAppInfo info = entry.getValue();
                if (info.getSwitchable()) {
                    if (info.getSwitchable()) {
                        int i = mLowResolutionMode;
                        HwApsManagerService hwApsManagerService = this.mApsService;
                        if (i != 1) {
                        }
                    }
                }
                try {
                    am.forceStopPackage(pkgName, -1);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to kill aps package of " + pkgName + " when stop all low resolution apps.");
                }
            }
        }
    }

    public boolean findPackageInListLocked(String pkgName) {
        if (this.mPackages.get(pkgName) == null) {
            return false;
        }
        return true;
    }

    public boolean registerCallbackLocked(String pkgName, IApsManagerServiceCallback callback) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        if (callback != null) {
            this.mPkgnameCallbackMap.put(pkgName, callback);
            Slog.i(TAG, "registerCallbackLocked success, pkgName:" + pkgName + ", callback_count:" + this.mPkgnameCallbackMap.size());
            doCallbackAtFirstRegisterLocked(pkgName, callback);
            return true;
        }
        Slog.i(TAG, "unregisterCallback from service, pkg:" + pkgName);
        this.mPkgnameCallbackMap.remove(pkgName);
        return true;
    }

    public int notifyApsManagerServiceCallback(String pkgName, int apsCallbackCode, int data) {
        IApsManagerServiceCallback callback = this.mPkgnameCallbackMap.get(pkgName);
        if (callback == null) {
            Slog.d(TAG, "notifyApsManagerServiceCallback, pkgName:" + pkgName + " , callback is not found.");
            return -1;
        }
        try {
            Slog.d(TAG, "notifyApsManagerServiceCallback, pkgName:" + pkgName + ", apsCallbackCode:" + apsCallbackCode + ", data:" + data);
            callback.doCallback(apsCallbackCode, data);
            return 0;
        } catch (RemoteException ex) {
            this.mPkgnameCallbackMap.remove(pkgName);
            Slog.w(TAG, "notifyApsManagerServiceCallback,ex:" + ex + ", remove " + pkgName + " from mPkgnameCallbackMap.");
            return -5;
        }
    }

    private void doCallbackAtFirstRegisterLocked(String pkgName, IApsManagerServiceCallback callback) {
        try {
            Slog.i(TAG, "doCallbackAtFirstRegisterLocked, start ! pkgName:" + pkgName);
            if (this.mNewResolutionRatioMap.get(pkgName) != null) {
                int data = (int) (this.mNewResolutionRatioMap.get(pkgName).floatValue() * 100000.0f);
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:1, data: " + data);
                callback.doCallback(1, data);
            }
            if (this.newFpsMap.get(pkgName) != null) {
                int data2 = this.newFpsMap.get(pkgName).intValue();
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:0, data: " + data2 + " , from new config.");
                callback.doCallback(0, data2);
            }
        } catch (RemoteException ex) {
            Slog.w(TAG, "doCallbackAtFirstRegisterLocked, pkgName: " + pkgName + ", ex:" + ex);
        }
    }

    public int setResolutionLocked(String pkgName, float ratio, boolean switchable) {
        ApsAppInfo apsInfo = this.mPackages.get(pkgName);
        if (apsInfo == null) {
            apsInfo = new ApsAppInfo(pkgName, ratio, 60, 100, 100, switchable);
        } else {
            apsInfo.setResolutionRatio(ratio, switchable);
        }
        this.mPackages.put(pkgName, apsInfo);
        scheduleWrite();
        return 0;
    }

    public int setLowResolutionModeLocked(int lowResolutionMode) {
        Slog.i(TAG, "setLowResolutionModeLocked, lowReosulotionMode = " + lowResolutionMode);
        mLowResolutionMode = lowResolutionMode;
        this.mHandler.removeMessages(APS_MSG_FORCESTOP_APK);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(APS_MSG_FORCESTOP_APK));
        return 0;
    }

    public int setFpsLocked(String pkgName, int fps) {
        ApsAppInfo apsInfo = this.mPackages.get(pkgName);
        if (apsInfo == null) {
            Slog.w(TAG, "setFpsLocked can not get ApsAppInfo. We will create a new one");
            apsInfo = new ApsAppInfo(pkgName, 0.0f, fps, 100, 100, true);
        } else {
            apsInfo.setFps(fps);
        }
        this.mPackages.put(pkgName, apsInfo);
        scheduleWrite();
        return 0;
    }

    public int setMaxFpsLocked(String pkgName, int fps) {
        return -1;
    }

    public int setBrightnessLocked(String pkgName, int ratioPercent) {
        return -1;
    }

    public int setTextureLocked(String pkgName, int texture) {
        return -1;
    }

    public int setDynamicResolutionRatioLocked(String pkgName, float ratio) {
        if (ratio <= 0.0f || ratio > 1.0f) {
            Slog.i(TAG, "setDynamicResolutionRatioLocked, pkg:" + pkgName + ", ratio:" + ratio + ", return APS_ERRNO_RUNAS_CONFIG]");
            return -4;
        }
        if (ratio == 1.0f) {
            try {
                this.mNewResolutionRatioMap.remove(pkgName);
            } catch (Exception e) {
                Slog.e(TAG, "APS, HwApsManagerServiceConfig, setDynamicResolutionRatioLocked, exception");
                return -6;
            }
        } else {
            this.mNewResolutionRatioMap.put(pkgName, Float.valueOf(ratio));
        }
        Slog.i(TAG, "APSLog, setDynamicResolutionRatioLocked, pkg:" + pkgName + ", ratio:" + ratio + ", retCode:0");
        return 0;
    }

    public float getDynamicResolutionRatioLocked(String pkgName) {
        try {
            if (this.mNewResolutionRatioMap.get(pkgName) == null) {
                return 1.0f;
            }
            return this.mNewResolutionRatioMap.get(pkgName).floatValue();
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, getDynamicResolutionRatioLocked, exception");
            return 1.0f;
        }
    }

    public int setDynamicFpsLocked(String pkgName, int fps) {
        try {
            ApsAppInfo apsInfo = this.mPackages.get(pkgName);
            if (apsInfo == null || fps <= apsInfo.getMaxFrameRatio()) {
                if (fps == -1) {
                    this.newFpsMap.remove(pkgName);
                } else {
                    this.newFpsMap.put(pkgName, Integer.valueOf(fps));
                }
                Slog.i(TAG, "APSLog, setDynamicFpsLocked: pkg:" + pkgName + ",fps:" + fps + ",retCode:0 ");
                return 0;
            }
            Slog.i(TAG, "APSLog, setDynamicFpsLocked: pkg:" + pkgName + ",fps:" + fps + ",retCode:-4->APS_ERRNO_RUNAS_CONFIG, config fps:" + apsInfo.getFrameRatio());
            return -4;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setDynamicFPSLocked, exception");
            return -6;
        }
    }

    public int getDynamicFpsLocked(String pkgName) {
        if (pkgName == null) {
            try {
                Slog.e(TAG, "getResolutionLocked input invalid param!");
                return -1;
            } catch (Exception e) {
                Slog.e(TAG, "APS, HwApsManagerServiceConfig, getDynamicFPSLocked, exception");
                return -6;
            }
        } else {
            int retFps = -1;
            String fpsSourceForPrintLog = GET_FPS_SOURCE_UNKOWN;
            ApsAppInfo apsInfo = this.mPackages.get(pkgName);
            if (this.newFpsMap.containsKey(pkgName)) {
                retFps = this.newFpsMap.get(pkgName).intValue();
                fpsSourceForPrintLog = GET_FPS_SOURCE_SERVICE;
            } else if (apsInfo != null) {
                retFps = apsInfo.getFrameRatio();
                fpsSourceForPrintLog = GET_FPS_SOURCE_CONFIG;
            }
            Slog.i(TAG, "APSLog -> getFps:[from:" + fpsSourceForPrintLog + ",pkgName:" + pkgName + ",fps:" + retFps + "]");
            return retFps;
        }
    }

    public int setPackageApsInfoLocked(String pkgName, ApsAppInfo info) {
        if (info == null) {
            return -1;
        }
        this.mPackages.put(pkgName, new ApsAppInfo(info));
        scheduleWrite();
        return 0;
    }

    public ApsAppInfo getPackageApsInfoLocked(String pkgName) {
        if (pkgName != null) {
            return this.mPackages.get(pkgName);
        }
        Slog.e(TAG, "getPackageApsInfoLocked input invalid param!");
        return null;
    }

    public float getResolutionLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getResolutionLocked input invalid param!");
            return -1.0f;
        } else if ((this.mApsSupportValue & AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION) == 0) {
            Slog.w(TAG, "HwApsManagerServiceConfig.getResoutionLocked, application low resolution is not supported.");
            return -1.0f;
        } else if (this.mApsService.mInCarMode) {
            return -1.0f;
        } else {
            ApsAppInfo info = this.mPackages.get(pkgName);
            String platform = SystemProperties.get(PROP_KIRIN_PLATFORM, "");
            if (this.mVrMananger.isVRDeviceConnected() && KIRIN_PLATFORM_970.equals(platform) && this.mVrMananger.isVRLowPowerApp(pkgName)) {
                return VR_APP_RATIO;
            }
            boolean isSwitchOff = mLowResolutionMode == 0;
            if (info == null || ((info.getSwitchable() || this.mIsSmartAlwaysSwitchable) && isSwitchOff)) {
                return -1.0f;
            }
            float ratio = info.getResolutionRatio();
            float rogScaleRatio = getRogScaleRatio();
            if (0.0f >= ratio || ratio >= 1.0f || rogScaleRatio == 0.0f) {
                return ratio;
            }
            if (ratio >= rogScaleRatio) {
                return -1.0f;
            }
            return ratio / rogScaleRatio;
        }
    }

    private float getRogScaleRatio() {
        int dpi = SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)));
        int realdpi = SystemProperties.getInt("persist.sys.realdpi", dpi);
        if (dpi == 0 || realdpi == dpi) {
            return 1.0f;
        }
        return (((float) realdpi) * 1.0f) / ((float) dpi);
    }

    public int getTextureLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getTextureLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getTexturePercent();
    }

    public int getFpsLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getFpsLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getFrameRatio();
    }

    public int getMaxFpsLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getMaxFpsLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getMaxFrameRatio();
    }

    public int getBrightnessLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getResolutionLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getBrightnessPercent();
    }

    public boolean deletePackageApsInfoLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "deletePackageApsInfoLocked input invalid param!");
            return false;
        } else if (this.mPackages.get(pkgName) == null) {
            return false;
        } else {
            this.mPackages.remove(pkgName);
            scheduleWrite();
            return true;
        }
    }

    public List<ApsAppInfo> getAllPackagesApsInfoLocked() {
        List<ApsAppInfo> apsAppInfolist = new ArrayList<>();
        for (Map.Entry<String, ApsAppInfo> entry : this.mPackages.entrySet()) {
            apsAppInfolist.add(entry.getValue());
        }
        return apsAppInfolist;
    }

    public List<String> getAllApsPackagesLocked() {
        List<String> apsPackageNameList = new ArrayList<>();
        for (String pkgName : this.mPackages.keySet()) {
            apsPackageNameList.add(pkgName);
        }
        return apsPackageNameList;
    }

    public boolean updateApsInfoLocked(List<ApsAppInfo> infos) {
        for (ApsAppInfo apsInfoUpdate : infos) {
            if (apsInfoUpdate == null) {
                Slog.e(TAG, "updateApsInfoLocked error, can not find apsInfo.");
                return false;
            }
            String pkgNameUpdate = apsInfoUpdate.getBasePackageName();
            for (String pkgName : this.mPackages.keySet()) {
                if (pkgName.equals(pkgNameUpdate)) {
                    this.mPackages.get(pkgName);
                }
            }
        }
        return false;
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(300);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(300), 10000);
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0106  */
    /* JADX WARNING: Removed duplicated region for block: B:40:? A[RETURN, SYNTHETIC] */
    public void saveApsAppInfo() {
        HashMap<String, ApsAppInfo> pkgs;
        synchronized (this.mApsService) {
            pkgs = new HashMap<>(this.mPackages);
        }
        FileOutputStream fos = null;
        try {
            fos = this.mFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            out.setOutput(baos, StandardCharsets.UTF_8.name());
            String str = null;
            out.startDocument(null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, COMPAT_FILE_MAIN_TAG);
            Iterator<Map.Entry<String, ApsAppInfo>> it = pkgs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ApsAppInfo> entry = it.next();
                String pkg = entry.getKey();
                ApsAppInfo apsInfo = entry.getValue();
                if (apsInfo != null) {
                    out.startTag(str, "pkg");
                    out.attribute(str, "name", pkg);
                    float rr = apsInfo.getResolutionRatio();
                    int fr = apsInfo.getFrameRatio();
                    int maxfr = apsInfo.getMaxFrameRatio();
                    int tp = apsInfo.getTexturePercent();
                    int bp = apsInfo.getBrightnessPercent();
                    boolean sa = apsInfo.getSwitchable();
                    try {
                        out.attribute(null, RESOLUTION_RATIO_STRING, Float.toString(rr));
                        out.attribute(null, FRAME_RATE_STRING, Integer.toString(fr));
                        out.attribute(null, MAX_FRAME_RATE_STRING, Integer.toString(maxfr));
                        out.attribute(null, TEXTURE_PERCENT_STRING, Integer.toString(tp));
                        out.attribute(null, BRIGHTNESS_PERCENT_STRING, Integer.toString(bp));
                        out.attribute(null, "switch", Boolean.toString(sa));
                        out.endTag(null, "pkg");
                        pkgs = pkgs;
                        it = it;
                        str = null;
                    } catch (IOException e) {
                        e = e;
                        Slog.e(TAG, "Error writing hwaps compat packages", e);
                        if (fos == null) {
                        }
                    }
                }
            }
            out.endTag(null, COMPAT_FILE_MAIN_TAG);
            out.endDocument();
            byte[] buf = baos.toByteArray();
            if (buf != null) {
                for (int i = 0; i < buf.length; i++) {
                    buf[i] = (byte) (~buf[i]);
                }
                fos.write(buf);
            }
            this.mFile.finishWrite(fos);
        } catch (IOException e2) {
            e = e2;
            Slog.e(TAG, "Error writing hwaps compat packages", e);
            if (fos == null) {
                this.mFile.failWrite(fos);
            }
        }
    }
}
