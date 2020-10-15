package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.display.DisplayEffectMonitor;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.huawei.displayengine.DisplayEngineManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HwEyeProtectionControllerImpl {
    private static final int BUNDLE_BUFFER_LENGTH = 12;
    private static final int DEFAULT_COLOR_TEMP = -1;
    private static final int DEFAULT_FAIL_RETURN_NEGATIVE = -1;
    private static final int DELAY_MILLI_SECONDS_200 = 200;
    private static final int HOUR_IN_MINUTE = 60;
    private static final int RG_BLUE_INDEX = 2;
    private static final int RG_GREEN_INDEX = 1;
    private static final int RG_RED_INDEX = 0;
    private static final String TAG = "EyeProtectionControllerImpl";
    private static final int XCC_COEF_BLUE_INDEX = 2;
    private static final int XCC_COEF_DEFAULT_RATIO = 32768;
    private static final int XCC_COEF_GREEN_INDEX = 1;
    private static final int XCC_COEF_REG_INDEX = 0;
    private static final int XCC_COEF_SIZE = 3;
    /* access modifiers changed from: private */
    public HwNormalizedAutomaticBrightnessController mAutomaticBrightnessController;
    /* access modifiers changed from: private */
    public Context mContext;
    private int mCurrentUserId;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private DisplayEngineManager mDisplayEngineManager;
    private long mEyeComfortValidValue = 0;
    /* access modifiers changed from: private */
    public HwEyeProtectionDividedTimeControl mEyeProtectionDividedTimeControl;
    private int mEyeProtectionScreenOffMode = 0;
    private int mEyeProtectionTempMode = 0;
    /* access modifiers changed from: private */
    public int mEyeScheduleBeginTime;
    /* access modifiers changed from: private */
    public int mEyeScheduleEndTime;
    /* access modifiers changed from: private */
    public int mEyeScheduleSwitchMode;
    /* access modifiers changed from: private */
    public int mEyesProtectionMode;
    /* access modifiers changed from: private */
    public SmartDisplayHandler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mIsBootCompleted;
    private boolean mIsEyeProtectionControlFlag = false;
    private boolean mIsEyeProtectionScreenOff = false;
    private boolean mIsSetForce3dColorTemp = true;
    private boolean mIsSuperPowerMode = false;
    /* access modifiers changed from: private */
    public int mKidsEyesProtectionMode = 1;
    private ContentObserver mKidsEyesProtectionModeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass7 */

        public void onChange(boolean isSelfChange) {
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mKidsEyesProtectionMode = Settings.Global.getInt(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_KIDS_EYE_PROTECTION_MODE, 1);
            Slog.i(HwEyeProtectionControllerImpl.TAG, "eyes protection mode changed in Kids mode. mKidsEyesProtectionMode:" + HwEyeProtectionControllerImpl.this.mKidsEyesProtectionMode);
            HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
        }
    };
    /* access modifiers changed from: private */
    public int mKidsMode;
    private ContentObserver mKidsModeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass6 */

        public void onChange(boolean isSelfChange) {
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mKidsMode = Settings.Global.getInt(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_KIDS_MODE, 0);
            Slog.i(HwEyeProtectionControllerImpl.TAG, "Kids mode changed. mKidsMode:" + HwEyeProtectionControllerImpl.this.mKidsMode);
            HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
        }
    };
    private volatile boolean mLongDimmingFlag = false;
    private ContentObserver mProtectionModeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass2 */

        private void cancelTimeControlOperations() {
            HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
            HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.cancelTimeControlAlarm(0);
            HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.cancelTimeControlAlarm(1);
        }

        public void onChange(boolean isSelfChange) {
            int tempProtectionMode = HwEyeProtectionControllerImpl.this.mEyesProtectionMode;
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mEyesProtectionMode = Settings.System.getIntForUser(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
            Slog.i(HwEyeProtectionControllerImpl.TAG, "Eyes-Protect mode in Settings changed mEyesProtectionMode =" + HwEyeProtectionControllerImpl.this.mEyesProtectionMode + ", user =" + -2);
            HwEyeProtectionControllerImpl.this.setValidTimeOnProtectionMode(tempProtectionMode);
            if (HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 3) {
                HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
                return;
            }
            if (tempProtectionMode == 3 && HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 0 && HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
                HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
                HwEyeProtectionControllerImpl.this.setTimeControlAlarm(86400000);
            }
            if (tempProtectionMode == 1 && HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 0) {
                if (HwEyeProtectionControllerImpl.this.mEyeScheduleSwitchMode == 1) {
                    HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
                    if (HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.isNeedDelay()) {
                        HwEyeProtectionControllerImpl.this.setTimeControlAlarm(86400000);
                    } else {
                        HwEyeProtectionControllerImpl.this.setTimeControlAlarm(0);
                    }
                } else {
                    cancelTimeControlOperations();
                }
            }
            if (tempProtectionMode == 0 && HwEyeProtectionControllerImpl.this.mEyeScheduleSwitchMode == 1 && HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 1) {
                HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.updateDiviedTimeFlag();
                if (HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
                    HwEyeProtectionControllerImpl.this.resetTimeControlAlarm();
                }
            }
            HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
        }
    };
    private ContentObserver mScheduleBeginTimeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass4 */

        public void onChange(boolean isSelfChange) {
            Slog.i(HwEyeProtectionControllerImpl.TAG, "Eyes-schedule begin time changed");
            int eyeScheduleBeginTime = HwEyeProtectionControllerImpl.this.mEyeScheduleBeginTime;
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mEyeScheduleBeginTime = Settings.System.getIntForUser(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_STARTTIME, -1, -2);
            HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setValidTime(false);
            if (HwEyeProtectionControllerImpl.this.mEyeScheduleBeginTime != -1 && eyeScheduleBeginTime != HwEyeProtectionControllerImpl.this.mEyeScheduleBeginTime) {
                HwEyeProtectionControllerImpl.this.resetTimeControlAlarm();
                if (HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
                    if (HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 0) {
                        HwEyeProtectionControllerImpl.this.setEyeScheduleSwitchToUserMode(3);
                    } else {
                        HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
                    }
                } else if (HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 3) {
                    HwEyeProtectionControllerImpl.this.setEyeScheduleSwitchToUserMode(0);
                } else {
                    HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
                }
            }
        }
    };
    private ContentObserver mScheduleEndTimeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass5 */

        public void onChange(boolean isSelfChange) {
            Slog.i(HwEyeProtectionControllerImpl.TAG, "Eyes-schedule end time changed");
            int eyeScheduleEndTime = HwEyeProtectionControllerImpl.this.mEyeScheduleEndTime;
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mEyeScheduleEndTime = Settings.System.getIntForUser(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_ENDTIME, -1, -2);
            HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setValidTime(false);
            if (HwEyeProtectionControllerImpl.this.mEyeScheduleEndTime != -1 && eyeScheduleEndTime != HwEyeProtectionControllerImpl.this.mEyeScheduleEndTime) {
                HwEyeProtectionControllerImpl.this.resetTimeControlAlarm();
                if (HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
                    if (HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 0) {
                        HwEyeProtectionControllerImpl.this.setEyeScheduleSwitchToUserMode(3);
                    } else {
                        HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
                    }
                } else if (HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 3) {
                    HwEyeProtectionControllerImpl.this.setEyeScheduleSwitchToUserMode(0);
                } else {
                    HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
                }
            }
        }
    };
    private ContentObserver mScheduleModeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass3 */

        public void onChange(boolean isSelfChange) {
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mEyeScheduleSwitchMode = Settings.System.getIntForUser(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_SWITCH, 0, -2);
            Slog.i(HwEyeProtectionControllerImpl.TAG, "Eyes-schedule mode in Settings changed, mEyeScheduleSwitchMode =" + HwEyeProtectionControllerImpl.this.mEyeScheduleSwitchMode + ", user =" + -2);
            HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setValidTime(false);
            if (HwEyeProtectionControllerImpl.this.mEyeScheduleSwitchMode == 0) {
                if (HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag() && HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 3) {
                    HwEyeProtectionControllerImpl.this.setEyeScheduleSwitchToUserMode(0);
                }
                HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
                HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.cancelTimeControlAlarm(0);
                HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.cancelTimeControlAlarm(1);
            } else {
                HwEyeProtectionControllerImpl.this.resetTimeControlAlarm();
                if (HwEyeProtectionControllerImpl.this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag() && HwEyeProtectionControllerImpl.this.mEyesProtectionMode == 0) {
                    HwEyeProtectionControllerImpl.this.setEyeScheduleSwitchToUserMode(3);
                    return;
                }
            }
            HwEyeProtectionControllerImpl.this.updateGlobalSceneState();
        }
    };
    /* access modifiers changed from: private */
    public ScreenStateReceiver mScreenStateReceiver;
    private ContentObserver mSetColorTempValueObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.display.HwEyeProtectionControllerImpl.AnonymousClass1 */

        public void onChange(boolean isSelfChange) {
            HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
            int unused = hwEyeProtectionControllerImpl.mUserSetColorTempValue = Settings.System.getIntForUser(hwEyeProtectionControllerImpl.mContext.getContentResolver(), Utils.KEY_SET_COLOR_TEMP, 0, -2);
            Slog.i(HwEyeProtectionControllerImpl.TAG, "Eyes set warm mode in Settings changed, mUserSetColorTempValue =" + HwEyeProtectionControllerImpl.this.mUserSetColorTempValue + ", user: " + -2);
            if (HwEyeProtectionControllerImpl.this.mEyesProtectionMode != 0) {
                int unused2 = HwEyeProtectionControllerImpl.this.setUserColorTemperature();
            }
        }
    };
    private int mSuperPowerBeginDay;
    private int mSuperPowerBeginTime;
    /* access modifiers changed from: private */
    public int mUserSetColorTempValue;

    public void setLongDimmingFlag(boolean flag) {
        this.mLongDimmingFlag = flag;
    }

    public HwEyeProtectionControllerImpl(Context context, HwNormalizedAutomaticBrightnessController automaticBrightnessController) {
        this.mAutomaticBrightnessController = automaticBrightnessController;
        this.mContext = context;
        this.mDisplayEngineManager = new DisplayEngineManager();
        startThreadAndMessageHandle(context);
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(context);
    }

    private void startThreadAndMessageHandle(Context context) {
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new SmartDisplayHandler(this.mHandlerThread.getLooper());
        this.mEyeProtectionDividedTimeControl = new HwEyeProtectionDividedTimeControl(context, this);
        this.mHandler.sendEmptyMessage(8);
        this.mHandler.sendEmptyMessage(7);
        this.mHandler.sendEmptyMessage(1);
    }

    /* access modifiers changed from: private */
    public void setTimeControlAlarm(long delayTime) {
        this.mEyeProtectionDividedTimeControl.setTimeControlAlarm(delayTime, 0);
        this.mEyeProtectionDividedTimeControl.setTimeControlAlarm(delayTime, 1);
    }

    /* access modifiers changed from: private */
    public void setValidTimeOnProtectionMode(int tempProtectionMode) {
        int i = this.mEyesProtectionMode;
        if (i != 2 && tempProtectionMode != 2) {
            if (tempProtectionMode == 3 && i == 0 && this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
                this.mEyeProtectionDividedTimeControl.setValidTime(true);
            } else if (tempProtectionMode == 1 && this.mEyesProtectionMode == 0 && this.mEyeScheduleSwitchMode == 1 && this.mEyeProtectionDividedTimeControl.isNeedDelay()) {
                this.mEyeProtectionDividedTimeControl.setValidTime(true);
            } else {
                this.mEyeProtectionDividedTimeControl.setValidTime(false);
            }
        }
    }

    private void setColorTemperatureAccordingToSetting() {
        Slog.i(TAG, "setColorTemperatureAccordingToSetting");
        Slog.i(TAG, "setColorTemperatureAccordingToSetting new.");
        try {
            String stringNewRgb = Settings.System.getStringForUser(this.mContext.getContentResolver(), Utils.COLOR_TEMPERATURE_RGB, -2);
            if (stringNewRgb != null) {
                List<String> rgbarryList = new ArrayList<>(Arrays.asList(stringNewRgb.split(",")));
                if (rgbarryList.size() <= 2) {
                    Slog.e(TAG, "index going to be used exceed size of mLuxLevels");
                    return;
                }
                float red = Float.valueOf(rgbarryList.get(0)).floatValue();
                float green = Float.valueOf(rgbarryList.get(1)).floatValue();
                float blue = Float.valueOf(rgbarryList.get(2)).floatValue();
                Slog.i(TAG, "ColorTemperature read from setting:" + stringNewRgb + red + green + blue);
                updateRgbGamma(red, green, blue);
                return;
            }
            int operation = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.COLOR_TEMPERATURE, 128, -2);
            Slog.i(TAG, "ColorTemperature read from old setting:" + operation);
        } catch (UnsatisfiedLinkError e) {
            Slog.w(TAG, "ColorTemperature read from setting exception!");
            updateRgbGamma(1.0f, 1.0f, 1.0f);
        } catch (NumberFormatException e2) {
            Slog.w(TAG, "ColorTemperature read from setting exception:" + 1.0f + ", " + 1.0f + ", " + 1.0f);
        }
    }

    private int updateRgbGamma(float red, float green, float blue) {
        Slog.i(TAG, "updateRgbGamma:red=" + red + " green=" + green + " blue=" + blue);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray("Buffer", new int[]{(int) (red * 32768.0f), (int) (green * 32768.0f), (int) (32768.0f * blue)});
        bundle.putInt("BufferLength", 12);
        int ret = this.mDisplayEngineManager.setData(7, bundle);
        if (ret != 0) {
            Slog.e(TAG, "setData DATA_TYPE_3D_COLORTEMP failed, ret = " + ret);
        }
        return ret;
    }

    public void updateGlobalSceneState() {
        updateProtectionControlFlag();
        Slog.i(TAG, "updateGlobalSceneState, mIsEyeProtectionControlFlag =" + this.mIsEyeProtectionControlFlag + ", mUserSetColorTempValue=" + this.mUserSetColorTempValue);
        if (this.mIsEyeProtectionControlFlag) {
            callDisplayEngineManagerSetScene();
        } else {
            int ret = this.mDisplayEngineManager.setScene(15, isAlarmEffect(this.mEyesProtectionMode == 0) ? 49 : 17);
            if (ret != 0) {
                Slog.e(TAG, "setScene DE_SCENE_EYEPROTECTION DE_ACTION_MODE_OFF error:" + ret);
            }
            if (this.mIsSetForce3dColorTemp) {
                setRgbGamma();
            }
            this.mIsSetForce3dColorTemp = false;
            sendEyeProtectEnableToMonitor(false);
        }
        this.mHandler.sendEmptyMessageDelayed(0, 200);
    }

    private void setRgbGamma() {
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        try {
            String stringNewRgb = Settings.System.getStringForUser(this.mContext.getContentResolver(), Utils.COLOR_TEMPERATURE_RGB, -2);
            if (stringNewRgb != null) {
                List<String> rgbarryList = new ArrayList<>(Arrays.asList(stringNewRgb.split(",")));
                if (rgbarryList.size() <= 2) {
                    Slog.e(TAG, "index going to be used exceed size of mLuxLevels");
                    return;
                }
                red = Float.valueOf(rgbarryList.get(0)).floatValue();
                green = Float.valueOf(rgbarryList.get(1)).floatValue();
                blue = Float.valueOf(rgbarryList.get(2)).floatValue();
            } else {
                Slog.e(TAG, "ColorTemperature read from setting failed, and set default values");
            }
            updateRgbGamma(red, green, blue);
        } catch (UnsatisfiedLinkError e) {
            Slog.i(TAG, "ColorTemp read from setting exception:" + 1.0f + ", " + 1.0f + ", " + 1.0f);
            updateRgbGamma(1.0f, 1.0f, 1.0f);
        } catch (NumberFormatException e2) {
            Slog.i(TAG, "ColorTemperature read from setting exception:" + 1.0f + ", " + 1.0f + ", " + 1.0f);
        }
    }

    private void callDisplayEngineManagerSetScene() {
        int ret = this.mDisplayEngineManager.setScene(15, isAlarmEffect(this.mEyesProtectionMode != 1) ? 48 : 16);
        if (ret != 0) {
            Slog.e(TAG, "setScene DE_SCENE_EYEPROTECTION DE_ACTION_MODE_ON error:" + ret);
        }
        int ret2 = this.mDisplayEngineManager.setScene(11, this.mUserSetColorTempValue);
        if (ret2 != 0) {
            Slog.e(TAG, "setScene DE_SCENE_COLORTEMP mUserSetColorTempValue error: " + ret2);
        }
        sendEyeProtectEnableToMonitor(true);
    }

    private boolean isAlarmEffect(boolean eyeStatus) {
        Slog.i(TAG, "Long dimming flag:" + this.mLongDimmingFlag + ", Kids Mode:" + this.mKidsMode + ", Schedule Mode:" + this.mEyeScheduleSwitchMode + "Eye Mode:" + this.mEyesProtectionMode);
        if (!this.mLongDimmingFlag) {
            return false;
        }
        this.mLongDimmingFlag = false;
        if (this.mKidsMode == 1 || this.mEyeScheduleSwitchMode != 1 || !eyeStatus) {
            return false;
        }
        return true;
    }

    private void sendEyeProtectEnableToMonitor(boolean isEnable) {
        if (this.mDisplayEffectMonitor != null) {
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "eyeProtect");
            params.put("isEnable", Boolean.valueOf(isEnable));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    /* access modifiers changed from: private */
    public void initDataState() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Utils.KEY_EYES_PROTECTION), true, this.mProtectionModeObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Utils.KEY_SET_COLOR_TEMP), true, this.mSetColorTempValueObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Utils.KEY_EYE_SCHEDULE_SWITCH), true, this.mScheduleModeObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Utils.KEY_EYE_SCHEDULE_STARTTIME), true, this.mScheduleBeginTimeObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Utils.KEY_EYE_SCHEDULE_ENDTIME), true, this.mScheduleEndTimeObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Utils.KEY_KIDS_MODE), true, this.mKidsModeObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Utils.KEY_KIDS_EYE_PROTECTION_MODE), true, this.mKidsEyesProtectionModeObserver);
        this.mKidsMode = Settings.Global.getInt(this.mContext.getContentResolver(), Utils.KEY_KIDS_MODE, 0);
        this.mKidsEyesProtectionMode = Settings.Global.getInt(this.mContext.getContentResolver(), Utils.KEY_KIDS_EYE_PROTECTION_MODE, 1);
        this.mUserSetColorTempValue = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_SET_COLOR_TEMP, 0, -2);
        this.mEyeComfortValidValue = Settings.System.getLongForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_COMFORT_VALID, 0, -2);
        this.mEyesProtectionMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
        this.mEyeScheduleSwitchMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_SWITCH, 0, -2);
        this.mEyeScheduleBeginTime = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_STARTTIME, -1, -2);
        this.mEyeScheduleEndTime = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_ENDTIME, -1, -2);
        this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleBeginTime, 0);
        this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleEndTime, 1);
        this.mEyeProtectionDividedTimeControl.init();
        if (this.mEyeScheduleSwitchMode == 1) {
            this.mEyeProtectionDividedTimeControl.updateDiviedTimeFlag();
        }
    }

    /* access modifiers changed from: private */
    public void handleUserSwitch(int userId) {
        this.mCurrentUserId = userId;
        Slog.i(TAG, "onReceive ACTION_USER_SWITCHED mCurrentUserId = " + this.mCurrentUserId);
        this.mKidsMode = Settings.Global.getInt(this.mContext.getContentResolver(), Utils.KEY_KIDS_MODE, 0);
        this.mKidsEyesProtectionMode = Settings.Global.getInt(this.mContext.getContentResolver(), Utils.KEY_KIDS_EYE_PROTECTION_MODE, 1);
        this.mEyesProtectionMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, this.mCurrentUserId);
        this.mEyeScheduleSwitchMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_SWITCH, 0, this.mCurrentUserId);
        this.mEyeScheduleBeginTime = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_STARTTIME, -1, this.mCurrentUserId);
        this.mEyeScheduleEndTime = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_ENDTIME, -1, this.mCurrentUserId);
        this.mUserSetColorTempValue = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_SET_COLOR_TEMP, 0, -2);
        this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleBeginTime, 0);
        this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleEndTime, 1);
        Slog.i(TAG, "onReceive mEyesProtectionMode = " + this.mEyesProtectionMode + ",mEyeScheduleSwitchMode=" + this.mEyeScheduleSwitchMode);
        int i = this.mEyeScheduleSwitchMode;
        if (i == 0) {
            defaultHandleOnSwitchUser();
        } else if (i == 1) {
            this.mEyeProtectionDividedTimeControl.updateDiviedTimeFlag();
            setEyeModeOnSwitchMode();
            if (this.mEyesProtectionMode == 1) {
                updateGlobalSceneState();
            }
        }
    }

    private void defaultHandleOnSwitchUser() {
        this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
        this.mEyeProtectionDividedTimeControl.cancelTimeControlAlarm(0);
        this.mEyeProtectionDividedTimeControl.cancelTimeControlAlarm(1);
        updateGlobalSceneState();
    }

    /* access modifiers changed from: private */
    public void handleSuperPower(int status) {
        Slog.i(TAG, "onReceive ACTION_SUPER_POWERMODE mEyesProtectionMode =" + this.mEyesProtectionMode + ",status =" + status);
        this.mIsSuperPowerMode = status == 1;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int day = cal.get(6);
        int curTime = (cal.get(11) * 60) + cal.get(12);
        if (this.mIsSuperPowerMode) {
            if (this.mEyesProtectionMode == 1 || this.mEyeScheduleSwitchMode == 1) {
                this.mSuperPowerBeginDay = day;
                this.mSuperPowerBeginTime = curTime;
                Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 2, -2);
                this.mEyeProtectionTempMode = this.mEyesProtectionMode;
            }
        } else if (this.mEyesProtectionMode == 2) {
            if (this.mEyeProtectionTempMode == 1) {
                setEyeModeBackFromSuperPower(day, curTime);
            } else if (this.mEyeScheduleSwitchMode == 1) {
                setEyeModeOnSwitchMode();
            }
            this.mEyeProtectionTempMode = 0;
        }
    }

    private void setEyeModeBackFromSuperPower(int day, int curTime) {
        if (this.mEyeScheduleSwitchMode == 1) {
            int i = this.mEyeScheduleBeginTime;
            int i2 = this.mEyeScheduleEndTime;
            if (i < i2) {
                if (this.mSuperPowerBeginTime < i2 && curTime >= i2) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
                    return;
                } else if (day > this.mSuperPowerBeginDay && curTime < this.mEyeScheduleBeginTime && this.mSuperPowerBeginTime < this.mEyeScheduleEndTime) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
                    return;
                }
            } else if (day <= this.mSuperPowerBeginDay || curTime < i2 || curTime >= i) {
                int i3 = this.mSuperPowerBeginTime;
                int i4 = this.mEyeScheduleEndTime;
                if (i3 < i4 && curTime >= i4 && curTime < this.mEyeScheduleBeginTime) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
                    return;
                }
            } else {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
                return;
            }
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 1, -2);
    }

    /* access modifiers changed from: private */
    public void handleTimeAndTimezoneChanged() {
        this.mEyeScheduleSwitchMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_SWITCH, 0, this.mCurrentUserId);
        if (this.mEyeScheduleSwitchMode != 0 && this.mEyesProtectionMode != 1) {
            setEyeModeOnSwitchMode();
        }
    }

    private void setEyeModeOnSwitchMode() {
        boolean isScreenOn = ((PowerManager) this.mContext.getSystemService("power")).isScreenOn();
        this.mEyeProtectionDividedTimeControl.updateDiviedTimeFlag();
        if (!this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
            if (isScreenOn) {
                int i = this.mEyesProtectionMode;
                if (i == 0) {
                    updateGlobalSceneState();
                } else if (i == 1 && this.mEyeScheduleSwitchMode == 1) {
                    resetTimeControlAlarm();
                    return;
                } else {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
                }
            }
            resetTimeControlAlarm();
        } else if (!eyeComfortTimeIsValid()) {
            handleWhenComfortTimeIsNotValid(isScreenOn);
            resetTimeControlAlarm();
        } else {
            handlWhenComfortTimeIsValid(isScreenOn);
        }
    }

    private void handleWhenComfortTimeIsNotValid(boolean isScreenOn) {
        if (isScreenOn) {
            if (this.mEyesProtectionMode == 3) {
                updateGlobalSceneState();
            } else {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 3, -2);
            }
        }
    }

    private void handlWhenComfortTimeIsValid(boolean isScreenOn) {
        this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
        if (isScreenOn) {
            if (this.mEyesProtectionMode == 0) {
                updateGlobalSceneState();
            } else {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
            }
        }
        this.mEyeProtectionDividedTimeControl.setTimeControlAlarm(86400000, 0);
        this.mEyeProtectionDividedTimeControl.setTimeControlAlarm(86400000, 1);
    }

    /* access modifiers changed from: protected */
    public void updateProtectionControlFlag() {
        boolean isFlagOn = false;
        boolean z = true;
        if (this.mKidsMode == 1) {
            if (this.mKidsEyesProtectionMode != 1) {
                z = false;
            }
            isFlagOn = z;
            Slog.d(TAG, "updateProtectionControlFlag mKidsMode =" + this.mKidsMode + ",mKidsEyesProtectionMode=" + this.mKidsEyesProtectionMode);
        } else {
            if (this.mEyesProtectionMode == 1) {
                isFlagOn = true;
            }
            if (this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag()) {
                isFlagOn = true;
            }
            if (this.mIsSuperPowerMode) {
                isFlagOn = false;
            }
            Slog.d(TAG, "updateProtectionControlFlag mEyesProtectionMode =" + this.mEyesProtectionMode + ",inDividedTimeFlag=" + this.mEyeProtectionDividedTimeControl.getInDividedTimeFlag());
        }
        this.mIsEyeProtectionControlFlag = isFlagOn;
        this.mAutomaticBrightnessController.setSplineEyeProtectionControlFlag(isFlagOn);
    }

    /* access modifiers changed from: protected */
    public void resetTimeControlAlarm() {
        Slog.d(TAG, "resetTimeControlAlarm");
        this.mEyeScheduleBeginTime = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_STARTTIME, -1, -2);
        this.mEyeScheduleEndTime = Settings.System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_SCHEDULE_ENDTIME, -1, -2);
        int i = this.mEyeScheduleBeginTime;
        if (i >= 0 && this.mEyeScheduleEndTime >= 0) {
            if (this.mEyesProtectionMode == 1 && this.mEyeScheduleSwitchMode == 1) {
                this.mEyeProtectionDividedTimeControl.setTime(i, 0);
                this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleEndTime, 1);
                this.mEyeProtectionDividedTimeControl.reSetTimeControlAlarm();
            } else if (this.mEyesProtectionMode != 1 && this.mEyeScheduleSwitchMode != 0) {
                this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleBeginTime, 0);
                this.mEyeProtectionDividedTimeControl.setTime(this.mEyeScheduleEndTime, 1);
                Slog.d(TAG, "resetTimeControlAlarm mEyeScheduleBeginTime =" + this.mEyeScheduleBeginTime + ",mEyeScheduleEndTime=" + this.mEyeScheduleEndTime);
                this.mEyeProtectionDividedTimeControl.reSetTimeControlAlarm();
            }
        }
    }

    /* access modifiers changed from: private */
    public void setBootEyeProtectionControlStatus() {
        Slog.i(TAG, "setBootEyeProtectionControlStatus ");
        if (this.mKidsMode == 1) {
            updateGlobalSceneState();
            return;
        }
        int i = this.mEyesProtectionMode;
        if (i == 1) {
            if (this.mEyeScheduleSwitchMode == 1) {
                setEyeModeOnSwitchMode();
            }
            updateGlobalSceneState();
        } else if (this.mEyeScheduleSwitchMode == 1) {
            setEyeModeOnSwitchMode();
        } else if (i == 2) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 1, -2);
        }
    }

    public void setEyeScheduleSwitchToUserMode(int type) {
        Slog.i(TAG, "setEyeScheduleSwitchToUserMode type is " + type);
        if (!this.mIsSuperPowerMode) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, type, -2);
        }
    }

    public void setEyeProtectionScreenTurnOffMode(int mode) {
        this.mIsEyeProtectionScreenOff = true;
        this.mEyeProtectionScreenOffMode = mode;
    }

    /* access modifiers changed from: private */
    public void resetEyeProtectionScreenTurnOffMode() {
        this.mIsEyeProtectionScreenOff = false;
        this.mEyeProtectionScreenOffMode = 0;
    }

    /* access modifiers changed from: private */
    public void setScreenOffEyeProtection() {
        if (this.mIsEyeProtectionScreenOff && !this.mIsSuperPowerMode) {
            Slog.i(TAG, "setScreenOffEyeProtection mEyeProtectionScreenOffMode =" + this.mEyeProtectionScreenOffMode);
            int i = this.mEyeProtectionScreenOffMode;
            if (i == 2) {
                this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(true);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 3, -2);
            } else if (i == 1) {
                this.mEyeProtectionDividedTimeControl.setInDividedTimeFlag(false);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
            }
        }
    }

    private boolean eyeComfortTimeIsValid() {
        this.mEyeComfortValidValue = Settings.System.getLongForUser(this.mContext.getContentResolver(), Utils.KEY_EYE_COMFORT_VALID, 0, -2);
        Slog.i(TAG, "eyeComfortTimeIsValid mEyeComfortValidValue =" + this.mEyeComfortValidValue);
        return this.mEyeProtectionDividedTimeControl.testTimeIsValid(this.mEyeComfortValidValue);
    }

    /* access modifiers changed from: private */
    public int setUserColorTemperature() {
        int ret = this.mDisplayEngineManager.setScene(11, this.mUserSetColorTempValue);
        if (ret != 0) {
            Slog.e(TAG, "setScene DE_SCENE_COLORTEMP: " + this.mUserSetColorTempValue + ", ret=" + ret);
        }
        return ret == 0 ? 1 : -1;
    }

    /* access modifiers changed from: private */
    public class ScreenStateReceiver extends BroadcastReceiver {
        ScreenStateReceiver() {
            IntentFilter userSwitchFilter = new IntentFilter();
            userSwitchFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED);
            HwEyeProtectionControllerImpl.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, userSwitchFilter, null, null);
            IntentFilter superPowerFilter = new IntentFilter();
            superPowerFilter.addAction(Utils.ACTION_SUPER_POWERMODE);
            HwEyeProtectionControllerImpl.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, superPowerFilter, null, null);
            IntentFilter timeChageFilter = new IntentFilter();
            timeChageFilter.addAction("android.intent.action.TIME_SET");
            timeChageFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            HwEyeProtectionControllerImpl.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, timeChageFilter, null, null);
            IntentFilter screenOnChangeFilter = new IntentFilter();
            screenOnChangeFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
            HwEyeProtectionControllerImpl.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, screenOnChangeFilter, null, null);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Slog.i(HwEyeProtectionControllerImpl.TAG, "onReceive intent action = " + intent.getAction());
                if (intent.getAction() != null) {
                    Message message = Message.obtain();
                    if (Utils.ACTION_SUPER_POWERMODE.equals(intent.getAction())) {
                        message.what = 3;
                        if (intent.getBooleanExtra("enable", false)) {
                            message.arg1 = 1;
                        } else {
                            message.arg1 = 0;
                        }
                    } else if (SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED.equals(intent.getAction())) {
                        message.what = 2;
                        message.arg1 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    } else if ("android.intent.action.TIME_SET".equals(intent.getAction()) || "android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                        message.what = 4;
                    } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON.equals(intent.getAction())) {
                        message.what = 6;
                    }
                    HwEyeProtectionControllerImpl.this.mHandler.sendMessage(message);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public final class SmartDisplayHandler extends Handler {
        SmartDisplayHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (HwEyeProtectionControllerImpl.this.mAutomaticBrightnessController != null) {
                        HwEyeProtectionControllerImpl.this.mAutomaticBrightnessController.updateAutoBrightness(true, false);
                        Slog.i(HwEyeProtectionControllerImpl.TAG, "updateAutoBrightness.");
                        return;
                    }
                    return;
                case 1:
                    HwEyeProtectionControllerImpl.this.setBootEyeProtectionControlStatus();
                    return;
                case 2:
                    HwEyeProtectionControllerImpl.this.handleUserSwitch(msg.arg1);
                    return;
                case 3:
                    HwEyeProtectionControllerImpl.this.handleSuperPower(msg.arg1);
                    return;
                case 4:
                    HwEyeProtectionControllerImpl.this.handleTimeAndTimezoneChanged();
                    return;
                case 5:
                default:
                    Slog.e(HwEyeProtectionControllerImpl.TAG, "Invalid message");
                    return;
                case 6:
                    HwEyeProtectionControllerImpl.this.setScreenOffEyeProtection();
                    HwEyeProtectionControllerImpl.this.resetEyeProtectionScreenTurnOffMode();
                    return;
                case 7:
                    HwEyeProtectionControllerImpl hwEyeProtectionControllerImpl = HwEyeProtectionControllerImpl.this;
                    ScreenStateReceiver unused = hwEyeProtectionControllerImpl.mScreenStateReceiver = new ScreenStateReceiver();
                    return;
                case 8:
                    HwEyeProtectionControllerImpl.this.initDataState();
                    return;
            }
        }
    }
}
