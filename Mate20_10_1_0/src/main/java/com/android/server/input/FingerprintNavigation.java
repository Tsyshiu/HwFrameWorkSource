package com.android.server.input;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Debug;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.pc.IHwPCManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.HwPCUtils;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.server.LocalServices;
import com.android.server.am.HwActivityManagerService;
import com.android.server.policy.AbsPhoneWindowManager;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.statusbar.HwStatusBarManagerService;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.cust.HwCustUtils;
import com.huawei.hiai.awareness.AwarenessConstants;
import huawei.android.provider.FrontFingerPrintSettings;

public final class FingerprintNavigation {
    private static final int CALLER_STACK_NUM = 4;
    private static final int DEFAULT_FINGER_PRINT_ID = -1;
    private static final int DEFAULT_FP_DEVICE_ID = -1;
    static final String FINGERPRINT_ANSWER_CALL = "fp_answer_call";
    static final String FINGERPRINT_BACK_TO_HOME = "fp_return_desk";
    static final String FINGERPRINT_CAMERA_SWITCH = "fp_take_photo";
    static final String FINGERPRINT_GALLERY_SLIDE = "fingerprint_gallery_slide";
    static final String FINGERPRINT_GO_BACK = "fp_go_back";
    static final String FINGERPRINT_LOCK_DEVICE = "fp_lock_device";
    static final String FINGERPRINT_MARKET_DEMO_PKG = "com.szdv";
    static final String FINGERPRINT_RECENT_APP = "fp_recent_application";
    static final String FINGERPRINT_SHOW_NOTIFICATION = "fp_show_notification";
    static final String FINGERPRINT_SLIDE_SWITCH = "fingerprint_slide_switch";
    static final String FINGERPRINT_STOP_ALARM = "fp_stop_alarm";
    static final String FINGERPRINT_USED_FIRSTLY = "fp_used_firstyl";
    /* access modifiers changed from: private */
    public static final boolean IS_DEBUG;
    static final boolean IS_ENABLE_BACK_TO_HOME = false;
    static final boolean IS_ENABLE_DOUBLE_TAP = false;
    static final boolean IS_ENABLE_LOCK_DEVICE = false;
    static final boolean IS_ENABLE_SHOW_FINGERPRINT_TIPS = false;
    static final boolean IS_ENABLE_SHOW_NOTIFICATION = false;
    public static final boolean IS_FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    /* access modifiers changed from: private */
    public static final boolean IS_SHOW_BACKFP_FUNCTION = SystemProperties.getBoolean("ro.feature.show_backfp_function", false);
    private static final int KEY_EVENT_DEVICE_ID = 6;
    private static final int MOTION_EVENT_DEVICE_ID = 5;
    private static final int SINGLETAP_DELAY_TIMEOUT = 300;
    static final String TAG = "FingerprintNavigation";
    static final String TAG_FP = "FPNavigation";
    static final int VERIFY_MSG = 1;
    private static final int VIBRATE_LAST_TIME = 500;
    private static final int WAKE_LOCK_TIME = 10000;
    private AccessibilityManager mAccessibilityManager;
    final ComponentName mAlarmServiceCmp = ComponentName.unflattenFromString("com.huawei.deskclock/com.android.deskclock.alarmclock.AlarmKlaxon");
    final ComponentName mAlarmServiceCmpOld = ComponentName.unflattenFromString("com.android.deskclock/.alarmclock.AlarmKlaxon");
    FingerprintNavigationInspector mCameraInspector;
    /* access modifiers changed from: private */
    public final Runnable mCameraLongPress = new Runnable() {
        /* class com.android.server.input.FingerprintNavigation.AnonymousClass2 */

        public void run() {
            Log.d(FingerprintNavigation.TAG, "CameraLongPress ,so send KeyEvent.KEYCODE_CAMERA");
            FingerprintNavigation.this.sendKeyEvent(27);
        }
    };
    FingerprintNavigationInspector mCollapsePanelsInspector;
    /* access modifiers changed from: private */
    public Context mContext;
    int mCurUser;
    FingerprintNavigationInspector mDefaultInspector;
    FingerprintNavigationInspector mDoubleTapInspector;
    private int mFingerPrintId = -1;
    FingerprintNavigationInspector mFingerprintDemoInspector;
    private FrontFingerprintNavigation mFrontFingerprintNav = null;
    FingerprintNavigationInspector mGalleryInspector;
    final Handler mHandler;
    private HwCustFingerprintNavigation mHwCust;
    FingerprintNavigationInspector mInCallInspector;
    boolean mIsAnswerCall = false;
    boolean mIsBackToHome = false;
    /* access modifiers changed from: private */
    public boolean mIsDeviceProvisioned;
    boolean mIsFingerprintMarketDemoSwitch = false;
    private boolean mIsFingerprintUsedFirstly;
    boolean mIsGallerySlide;
    boolean mIsGoBack = false;
    boolean mIsHasReadDb = false;
    boolean mIsInjectCamera = false;
    boolean mIsInjectSlide = false;
    boolean mIsLockDevice = false;
    boolean mIsRecentApp = false;
    boolean mIsShowNotification = false;
    boolean mIsStopAlarm = false;
    private KeyEvent mKeyEvent = null;
    FingerprintNavigationInspector mLauncherInspector;
    private final Runnable mLauncherLongPress = new Runnable() {
        /* class com.android.server.input.FingerprintNavigation.AnonymousClass1 */

        public void run() {
            Log.d(FingerprintNavigation.TAG, "LauncherLongPress ,so expandNotificationsPanel");
            ((StatusBarManager) FingerprintNavigation.this.mContext.getSystemService("statusbar")).expandNotificationsPanel();
        }
    };
    FingerprintNavigationInspector mLongPressOnScreenOffInspector;
    private CheckForSingleTap mPendingCheckForSingleTap;
    PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public final ContentResolver mResolver;
    final SettingsObserver mSettingsObserver;
    FingerprintNavigationInspector mSingleTapInspector;
    FingerprintNavigationInspector mStartHomeInspector;
    FingerprintNavigationInspector mStopAlarmInspector;
    private WindowManagerInternal mWindowManagerInternal;

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.debuggable", 0) != 1) {
            z = false;
        }
        IS_DEBUG = z;
    }

    FingerprintNavigation(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        Object hwCustObj = HwCustUtils.createObj(HwCustFingerprintNavigation.class, new Object[]{this.mContext});
        if (hwCustObj instanceof HwCustFingerprintNavigation) {
            this.mHwCust = (HwCustFingerprintNavigation) hwCustObj;
        }
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        this.mStartHomeInspector = new StartHomeInspector();
        this.mSingleTapInspector = new SingleTapInspector();
        this.mDoubleTapInspector = new DoubleTapInspector();
        this.mCollapsePanelsInspector = new CollapsePanelsInspector();
        this.mInCallInspector = new InCallInspector();
        this.mStopAlarmInspector = new StopAlarmInspector();
        this.mFingerprintDemoInspector = new FingerprintDemoInspector();
        this.mGalleryInspector = new GalleryInspector();
        this.mLongPressOnScreenOffInspector = new LongPressOnScreenOffInspector();
        this.mLauncherInspector = new LauncherInspector();
        this.mCameraInspector = new CameraInspector();
        this.mDefaultInspector = new DefaultInspector();
        this.mIsGallerySlide = false;
        this.mFrontFingerprintNav = new FrontFingerprintNavigation(context);
    }

    public void showNotificationTips() {
    }

    public void systemRunning() {
        FrontFingerprintNavigation frontFingerprintNavigation = this.mFrontFingerprintNav;
        if (frontFingerprintNavigation != null) {
            frontFingerprintNavigation.systemRunning();
        }
        boolean z = false;
        this.mIsDeviceProvisioned = Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), FINGERPRINT_USED_FIRSTLY, 1) != 0) {
            z = true;
        }
        this.mIsFingerprintUsedFirstly = z;
        Log.d(TAG, "SystemReady mIsDeviceProvisioned: " + this.mIsDeviceProvisioned + ",mIsFingerprintUsedFirstly: " + this.mIsFingerprintUsedFirstly);
    }

    /* access modifiers changed from: package-private */
    public boolean dispatchUnhandledKey(InputEvent event, int policyFlags) {
        if (event instanceof KeyEvent) {
            KeyEvent kv = (KeyEvent) event;
            if (IS_DEBUG) {
                Log.d(TAG, "unhandled fingprint event=" + kv + ",fromFingerprint=" + (kv.getFlags() & 2048));
            }
            if ((kv.getFlags() & 2048) == 0) {
                return false;
            }
            if (this.mInCallInspector.probe(kv)) {
                this.mInCallInspector.handle(kv);
            } else if (this.mStopAlarmInspector.probe(kv)) {
                this.mStopAlarmInspector.handle(kv);
            } else if (this.mStartHomeInspector.probe(kv, true)) {
                this.mStartHomeInspector.handle(kv);
                return true;
            }
        }
        return false;
    }

    private boolean interceptSystemNavigationKeyAsGoogle(KeyEvent event) {
        int code = event.getKeyCode();
        int transCode = -1;
        if (this.mAccessibilityManager == null) {
            Log.e(TAG, "Accessibility is Null !");
            return false;
        }
        switch (code) {
            case AwarenessConstants.SWING_GESTURE_ACTION_MAX /*{ENCODED_INT: 511}*/:
                transCode = 280;
                break;
            case 512:
                transCode = 281;
                break;
            case 513:
                transCode = 282;
                break;
            case 514:
                transCode = 283;
                break;
        }
        Log.d(TAG, "interceptSystemNavigation code:" + code + " transCode:" + transCode);
        if (transCode == -1 || !this.mAccessibilityManager.isEnabled() || !this.mAccessibilityManager.sendFingerprintGesture(transCode)) {
            return false;
        }
        Log.d(TAG, "Accessibility consume transCode:" + transCode);
        return true;
    }

    private void updateSwitchValue() {
        if (!this.mIsHasReadDb) {
            updateFingerNaviSwitchValue(true);
            boolean z = false;
            this.mIsFingerprintMarketDemoSwitch = Settings.System.getInt(this.mResolver, "fingerprint_market_demo_switch", 0) == 1;
            if (IS_SHOW_BACKFP_FUNCTION) {
                if (Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_GALLERY_SLIDE, 1, ActivityManager.getCurrentUser()) != 0) {
                    z = true;
                }
                this.mIsGallerySlide = z;
            } else {
                if (Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_GALLERY_SLIDE, 0, ActivityManager.getCurrentUser()) != 0) {
                    z = true;
                }
                this.mIsGallerySlide = z;
            }
            this.mIsHasReadDb = true;
        }
    }

    private boolean handleFingerTapEvent(KeyEvent kv) {
        if (kv != null) {
            int keyCode = kv.getKeyCode();
            if (keyCode == 66) {
                String compName = getTopApp();
                if (!(compName != null && (compName.startsWith("com.huawei.photos") || compName.startsWith("com.android.gallery3d"))) || isAlarm()) {
                    if (isInCallUi()) {
                        this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
                    }
                } else if (kv.getAction() == 0) {
                    return true;
                } else {
                    sendKeyEvent(502);
                    return true;
                }
            } else if (keyCode != 501) {
                if (keyCode == 601) {
                    if (kv.getAction() == 0) {
                        return true;
                    }
                    handleSingleTapEvent(kv);
                    return true;
                }
            } else if (kv.getAction() == 0) {
                return true;
            } else {
                this.mHandler.removeCallbacks(this.mPendingCheckForSingleTap);
                this.mPendingCheckForSingleTap = null;
                if (IS_DEBUG) {
                    Log.d(TAG, "keycode is : " + keyCode + " sent to app");
                }
                if (!isAlarm()) {
                    sendKeyEvent(keyCode);
                }
                return true;
            }
        }
        return false;
    }

    private void handleCustFingerEvent(InputEvent event) {
        HwCustFingerprintNavigation hwCustFingerprintNavigation = this.mHwCust;
        if (hwCustFingerprintNavigation != null && hwCustFingerprintNavigation.handleFingerprintEvent(event)) {
            return;
        }
        if (this.mLongPressOnScreenOffInspector.probe(event)) {
            this.mLongPressOnScreenOffInspector.handle(event);
        } else if (this.mFingerprintDemoInspector.probe(event)) {
            this.mFingerprintDemoInspector.handle(event);
        } else if (this.mSingleTapInspector.probe(event)) {
            this.mSingleTapInspector.handle(event);
        } else if (this.mDoubleTapInspector.probe(event)) {
            this.mDoubleTapInspector.handle(event);
        } else if (this.mStopAlarmInspector.probe(event)) {
            this.mStopAlarmInspector.handle(event);
        } else if (this.mInCallInspector.probe(event)) {
            this.mInCallInspector.handle(event);
        } else if (this.mCameraInspector.probe(event)) {
            this.mCameraInspector.handle(event);
        } else if (this.mStartHomeInspector.probe(event, false)) {
            this.mStartHomeInspector.handle(event);
        } else if (this.mLauncherInspector.probe(event)) {
            this.mLauncherInspector.handle(event);
        } else if (this.mCollapsePanelsInspector.probe(event)) {
            this.mCollapsePanelsInspector.handle(event);
        } else if (this.mGalleryInspector.probe(event)) {
            this.mGalleryInspector.handle(event);
        } else if (this.mDefaultInspector.probe(event)) {
            this.mDefaultInspector.handle(event);
        }
    }

    private boolean checkFpDeviceIdValid(InputEvent event) {
        if (this.mFingerPrintId < 0) {
            this.mFingerPrintId = SystemProperties.getInt("sys.fingerprint.deviceId", -1);
        }
        int fpdeviceId = -1;
        if (event instanceof MotionEvent) {
            fpdeviceId = ((MotionEvent) event).getDeviceId();
        } else if (event instanceof KeyEvent) {
            this.mKeyEvent = (KeyEvent) event;
            fpdeviceId = this.mKeyEvent.getDeviceId();
        }
        if (fpdeviceId < 0 || fpdeviceId != this.mFingerPrintId) {
            return false;
        }
        return true;
    }

    public boolean filterInputEvent(InputEvent event, int policyFlags) {
        FrontFingerprintNavigation frontFingerprintNavigation;
        this.mKeyEvent = null;
        if (!checkFpDeviceIdValid(event)) {
            return false;
        }
        if (HwPCUtils.isPcCastModeInServer()) {
            try {
                IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
                if (pcMgr != null && !pcMgr.isScreenPowerOn()) {
                    pcMgr.setScreenPower(true);
                    this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                    HwPCUtils.log(TAG, "lightscreen in desktop mode when touch fingerprint");
                }
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "filterInputEvent RemoteException");
            }
        }
        if (IS_FRONT_FINGERPRINT_NAVIGATION && this.mKeyEvent != null && !isVrMode() && (frontFingerprintNavigation = this.mFrontFingerprintNav) != null && frontFingerprintNavigation.handleFingerprintEvent(event)) {
            return true;
        }
        if (this.mKeyEvent != null && !FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION && interceptSystemNavigationKeyAsGoogle(this.mKeyEvent)) {
            return true;
        }
        if (!SystemProperties.getBoolean("ro.config.fingerOnSmartKey", false) || !needDropFingerprintEvent()) {
            updateSwitchValue();
            if (handleFingerTapEvent(this.mKeyEvent)) {
                return true;
            }
            handleCustFingerEvent(event);
            event.recycle();
            return true;
        }
        Log.d(TAG, "drop fingerprintnavigation event!");
        return true;
    }

    private final class CheckForSingleTap implements Runnable {
        private CheckForSingleTap() {
        }

        public void run() {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "sendBack, goBack=" + FingerprintNavigation.this.mIsGoBack);
            }
            if (FingerprintNavigation.this.isInCallUi()) {
                FingerprintNavigation.this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
            }
            if (FingerprintNavigation.this.mIsGoBack) {
                FingerprintNavigation.this.showNotificationTips();
                StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 3);
                FingerprintNavigation.this.mSingleTapInspector.handleTap();
            }
        }
    }

    private void handleSingleTapEvent(InputEvent event) {
        if (!this.mHandler.hasCallbacks(this.mPendingCheckForSingleTap)) {
            this.mPendingCheckForSingleTap = new CheckForSingleTap();
            if (IS_DEBUG) {
                Log.d(TAG, "checkSingleTap, caller=" + Debug.getCallers(4));
            }
            this.mHandler.postDelayed(this.mPendingCheckForSingleTap, 300);
            return;
        }
        CheckForSingleTap checkForSingleTap = this.mPendingCheckForSingleTap;
        if (checkForSingleTap != null) {
            this.mHandler.removeCallbacks(checkForSingleTap);
            this.mPendingCheckForSingleTap = null;
        }
        this.mDoubleTapInspector.handleDoubleTap();
    }

    /* access modifiers changed from: private */
    public boolean isKeyguardLocked() {
        KeyguardManager keyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguard != null) {
            return keyguard.isKeyguardLocked();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return !power.isScreenOn();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isInCallUiAndRinging() {
        String pkgName = getTopApp();
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        return "com.android.incallui/.InCallActivity".equals(pkgName) && telecomManager != null && telecomManager.isRinging();
    }

    /* access modifiers changed from: private */
    public boolean isInCallUi() {
        return "com.android.incallui/.InCallActivity".equals(getTopApp());
    }

    /* access modifiers changed from: package-private */
    public boolean isAlarm() {
        return serviceIsRunning(this.mAlarmServiceCmp, this.mCurUser) || serviceIsRunning(this.mAlarmServiceCmpOld, this.mCurUser);
    }

    /* access modifiers changed from: private */
    public boolean isLandscape() {
        return this.mContext.getResources().getConfiguration().orientation == 2;
    }

    /* access modifiers changed from: private */
    public boolean isCamera() {
        String pkgName = getTopApp();
        return pkgName != null && pkgName.startsWith("com.huawei.camera");
    }

    /* access modifiers changed from: private */
    public boolean isFingerprintDemo() {
        String pkgName = getTopApp();
        return pkgName != null && pkgName.startsWith(FINGERPRINT_MARKET_DEMO_PKG);
    }

    /* access modifiers changed from: private */
    public boolean isSpecialKey(InputEvent event, int code) {
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        KeyEvent ev = (KeyEvent) event;
        Log.i(TAG_FP, "keycode is : " + ev.getKeyCode());
        if (ev.getKeyCode() == code) {
            return true;
        }
        return false;
    }

    private void broadCastToKeyguard(boolean isValidated) {
        Intent intent = new Intent("com.android.server.input.fpn");
        intent.putExtra("validated", isValidated);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void sendKeyEvent(int keycode) {
        int[] actions;
        for (int i : new int[]{0, 1}) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, i, keycode, 0, 0, 6, 0, 2056, 257), 0);
        }
    }

    /* access modifiers changed from: private */
    public String getTopApp() {
        ActivityInfo topActivity = HwActivityTaskManager.getLastResumedActivity();
        if (topActivity == null) {
            return null;
        }
        if (IS_DEBUG) {
            Log.d(TAG_FP, "TopApp is " + topActivity.packageName);
        }
        return topActivity.getComponentName().flattenToShortString();
    }

    private boolean serviceIsRunning(ComponentName cmpName, int user) {
        return ((HwActivityManagerService) ServiceManager.getService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).serviceIsRunning(cmpName, user);
    }

    /* access modifiers changed from: private */
    public boolean statusBarObsecured() {
        WindowManagerPolicy wmp = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        HwPhoneWindowManager policy = null;
        if (wmp instanceof HwPhoneWindowManager) {
            policy = (HwPhoneWindowManager) wmp;
        }
        if (policy == null) {
            Log.e(TAG, "statusBarObsecured policy is null");
            return false;
        }
        boolean isStatusBarObsecured = policy.isStatusBarObsecured();
        Log.d(TAG, "isStatusBarObsecured: " + isStatusBarObsecured);
        return isStatusBarObsecured;
    }

    /* access modifiers changed from: private */
    public boolean focusedWinOverStatusBar() {
        WindowManagerPolicy wmp = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        HwPhoneWindowManager policy = null;
        if (wmp instanceof HwPhoneWindowManager) {
            policy = (HwPhoneWindowManager) wmp;
        }
        if (policy == null) {
            Log.e(TAG, "focusedWinOverStatusBar policy is null");
            return false;
        }
        WindowManagerPolicy.WindowState ws = policy.getFocusedWindow();
        WindowState focusedWin = null;
        if (ws instanceof WindowState) {
            focusedWin = (WindowState) ws;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("focusedWinOverStatusBar: ");
        sb.append(focusedWin != null ? policy.getWindowLayerFromTypeLw(focusedWin.getBaseType()) : 0);
        sb.append(", statusbarLayer=");
        sb.append(policy.getWindowLayerFromTypeLw(2000));
        Log.d(TAG, sb.toString());
        if (focusedWin == null || policy.getWindowLayerFromTypeLw(focusedWin.getBaseType()) <= policy.getWindowLayerFromTypeLw(2000)) {
            return false;
        }
        return true;
    }

    private void startVibrate() {
        Vibrator vb = (Vibrator) this.mContext.getSystemService("vibrator");
        Log.d(TAG, "startVibrate");
        if (vb.hasVibrator()) {
            Log.d(TAG, "real startVibrate");
            vb.vibrate(500);
        }
    }

    /* access modifiers changed from: private */
    public void updateFingerNaviSwitchValue(boolean isCareReadDb) {
        int defaultValue = 1;
        if (IS_FRONT_FINGERPRINT_NAVIGATION && !isCareReadDb) {
            defaultValue = 0;
        }
        boolean z = true;
        this.mIsInjectSlide = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, defaultValue, ActivityManager.getCurrentUser()) != 0;
        if (IS_SHOW_BACKFP_FUNCTION) {
            this.mIsInjectCamera = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_CAMERA_SWITCH, defaultValue, ActivityManager.getCurrentUser()) != 0;
        } else {
            this.mIsInjectCamera = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_CAMERA_SWITCH, 0, ActivityManager.getCurrentUser()) != 0;
        }
        this.mIsAnswerCall = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_ANSWER_CALL, 0, ActivityManager.getCurrentUser()) != 0;
        this.mIsShowNotification = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_SHOW_NOTIFICATION, 0, ActivityManager.getCurrentUser()) != 0;
        HwCustFingerprintNavigation hwCustFingerprintNavigation = this.mHwCust;
        if (hwCustFingerprintNavigation != null && hwCustFingerprintNavigation.needCustNavigation()) {
            this.mIsBackToHome = this.mHwCust.getCustNeedValue(this.mResolver, FINGERPRINT_BACK_TO_HOME, 0, ActivityManager.getCurrentUser(), 0);
            this.mIsGoBack = this.mHwCust.getCustNeedValue(this.mResolver, FINGERPRINT_GO_BACK, 0, ActivityManager.getCurrentUser(), 0);
            this.mIsRecentApp = this.mHwCust.getCustNeedValue(this.mResolver, FINGERPRINT_RECENT_APP, 0, ActivityManager.getCurrentUser(), 0);
        }
        if (Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_STOP_ALARM, 0, ActivityManager.getCurrentUser()) == 0) {
            z = false;
        }
        this.mIsStopAlarm = z;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
        }

        public void registerContentObserver(int userId) {
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_SLIDE_SWITCH), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_CAMERA_SWITCH), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_ANSWER_CALL), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_SHOW_NOTIFICATION), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_BACK_TO_HOME), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_STOP_ALARM), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_LOCK_DEVICE), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_GO_BACK), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_RECENT_APP), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.System.getUriFor("fingerprint_market_demo_switch"), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE), false, this, userId);
        }

        public void onChange(boolean isSelfChange) {
            Log.d(FingerprintNavigation.TAG, "SettingDB has Changed");
            boolean z = false;
            FingerprintNavigation.this.updateFingerNaviSwitchValue(false);
            FingerprintNavigation fingerprintNavigation = FingerprintNavigation.this;
            boolean unused = fingerprintNavigation.mIsDeviceProvisioned = Settings.Secure.getInt(fingerprintNavigation.mResolver, "device_provisioned", 0) != 0;
            FingerprintNavigation fingerprintNavigation2 = FingerprintNavigation.this;
            fingerprintNavigation2.mIsFingerprintMarketDemoSwitch = Settings.System.getInt(fingerprintNavigation2.mResolver, "fingerprint_market_demo_switch", 0) == 1;
            if (FingerprintNavigation.IS_FRONT_FINGERPRINT_NAVIGATION) {
                FingerprintNavigation fingerprintNavigation3 = FingerprintNavigation.this;
                if (Settings.Secure.getIntForUser(fingerprintNavigation3.mResolver, FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE, 0, ActivityManager.getCurrentUser()) != 0) {
                    z = true;
                }
                fingerprintNavigation3.mIsGallerySlide = z;
            } else if (FingerprintNavigation.IS_SHOW_BACKFP_FUNCTION) {
                FingerprintNavigation fingerprintNavigation4 = FingerprintNavigation.this;
                if (Settings.Secure.getIntForUser(fingerprintNavigation4.mResolver, FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE, 1, ActivityManager.getCurrentUser()) != 0) {
                    z = true;
                }
                fingerprintNavigation4.mIsGallerySlide = z;
            } else {
                FingerprintNavigation fingerprintNavigation5 = FingerprintNavigation.this;
                if (Settings.Secure.getIntForUser(fingerprintNavigation5.mResolver, FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE, 0, ActivityManager.getCurrentUser()) != 0) {
                    z = true;
                }
                fingerprintNavigation5.mIsGallerySlide = z;
            }
        }
    }

    abstract class FingerprintNavigationInspector {
        FingerprintNavigationInspector() {
        }

        public boolean probe(InputEvent event) {
            return false;
        }

        public boolean probe(InputEvent event, boolean isUnHandledKey) {
            return false;
        }

        public void handle(InputEvent event) {
        }

        public void handleTap() {
        }

        public void handleDoubleTap() {
        }
    }

    final class InCallInspector extends FingerprintNavigationInspector {
        InCallInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            TelecomManager telecomManager;
            if (FingerprintNavigation.this.mIsAnswerCall && !FingerprintNavigation.this.isScreenOff() && ((FingerprintNavigation.this.isSpecialKey(event, 66) || FingerprintNavigation.this.isSpecialKey(event, 502)) && (telecomManager = (TelecomManager) FingerprintNavigation.this.mContext.getSystemService("telecom")) != null && telecomManager.isRinging())) {
                return true;
            }
            return false;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            TelecomManager telecomManager;
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.showNotificationTips();
                if (FingerprintNavigation.this.mIsAnswerCall && (telecomManager = (TelecomManager) FingerprintNavigation.this.mContext.getSystemService("telecom")) != null) {
                    StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 6);
                    telecomManager.acceptRingingCall();
                }
            }
        }
    }

    final class SingleTapInspector extends FingerprintNavigationInspector {
        SingleTapInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, WifiProCommonUtils.RESP_CODE_UNSTABLE)) {
                return false;
            }
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
            }
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handleTap() {
            if (!FingerprintNavigation.this.isInCallUiAndRinging()) {
                FingerprintNavigation.this.sendKeyEvent(4);
            }
        }
    }

    final class DoubleTapInspector extends FingerprintNavigationInspector {
        DoubleTapInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (!FingerprintNavigation.this.mIsLockDevice || FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isInCallUi() || FingerprintNavigation.this.isCamera() || FingerprintNavigation.this.isAlarm() || !FingerprintNavigation.this.isSpecialKey(event, 26)) {
                return false;
            }
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.sendKeyEvent(26);
            }
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handleDoubleTap() {
            Log.e(FingerprintNavigation.TAG, "sendDoubleTap");
            FingerprintNavigation.this.sendKeyEvent(501);
        }
    }

    final class StopAlarmInspector extends FingerprintNavigationInspector {
        StopAlarmInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "StopAlarm State prob, mIsStopAlarm: " + FingerprintNavigation.this.mIsStopAlarm + ",isAlarm: " + FingerprintNavigation.this.isAlarm());
            }
            if (!FingerprintNavigation.this.mIsStopAlarm || FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isAlarm()) {
                return false;
            }
            if (FingerprintNavigation.this.isSpecialKey(event, 66) || FingerprintNavigation.this.isSpecialKey(event, 502)) {
                return true;
            }
            return false;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "StopAlarm State handle, event: " + event);
            }
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() != 0) {
                FingerprintNavigation.this.showNotificationTips();
                if (FingerprintNavigation.this.mIsStopAlarm) {
                    long curTime = SystemClock.uptimeMillis();
                    Intent intent = new Intent("com.android.server.input.fpn.stopalarm");
                    intent.putExtra("keytype", ev.getAction());
                    intent.putExtra("eventtime", curTime);
                    FingerprintNavigation.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
            }
        }
    }

    final class StartHomeInspector extends FingerprintNavigationInspector {
        StartHomeInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event, boolean isUnHandledKey) {
            int i = 66;
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "StartHomeInspector State probe, isEnter: " + FingerprintNavigation.this.isSpecialKey(event, 66) + ",isScreenOff(): " + FingerprintNavigation.this.isScreenOff() + ",isKeyguardLocked: " + FingerprintNavigation.this.isKeyguardLocked() + ",isCamera: " + FingerprintNavigation.this.isCamera());
            }
            if (!FingerprintNavigation.this.mIsBackToHome || FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isKeyguardLocked()) {
                return false;
            }
            FingerprintNavigation fingerprintNavigation = FingerprintNavigation.this;
            if (isUnHandledKey) {
                i = 502;
            }
            if (!fingerprintNavigation.isSpecialKey(event, i) || FingerprintNavigation.this.isAlarm() || FingerprintNavigation.this.isInCallUiAndRinging()) {
                return false;
            }
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "StartHomeInspector State handle, event: " + event);
            }
            if (((KeyEvent) event).getAction() != 0) {
                StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 4);
                FingerprintNavigation.this.sendKeyEvent(3);
            }
        }
    }

    final class LongPressOnScreenOffInspector extends FingerprintNavigationInspector {
        LongPressOnScreenOffInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (!FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, 66)) {
                return false;
            }
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            ((PowerManager) FingerprintNavigation.this.mContext.getSystemService("power")).newWakeLock(1, "COVER_WAKE_LOCK").acquire(10000);
            Log.d(FingerprintNavigation.TAG, "Current State is LongPressOnScreenOff , Start dealwith event!");
            long curTime = SystemClock.uptimeMillis();
            Intent intent = new Intent("com.android.server.input.fpn");
            intent.putExtra("keytype", ((KeyEvent) event).getAction());
            intent.putExtra("eventtime", curTime);
            FingerprintNavigation.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isScenseExcluded() {
        String appName = getTopApp();
        return appName != null && (appName.equals("com.huawei.hidisk/.strongbox.ui.activity.StrongBoxVerifyPassActivity") || appName.equals("com.android.settings/.fingerprint.enrollment.FingerprintEnrollActivity") || appName.equals("com.android.settings/.fingerprint.FingerprintSettingsActivity") || appName.equals("com.android.settings/.ConfirmLockPassword") || appName.equals("com.android.settings/.password.ConfirmLockPassword") || !this.mIsDeviceProvisioned);
    }

    final class CollapsePanelsInspector extends FingerprintNavigationInspector {
        CollapsePanelsInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "CollapsePanelsInspector prob");
            }
            if (FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isVrMode() || !FingerprintNavigation.this.isSpecialKey(event, AwarenessConstants.SWING_GESTURE_ACTION_MAX)) {
                return false;
            }
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "Current State is Launcher and KEYCODE_DPAD_TOP Event, event=" + event);
            }
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.showNotificationTips();
                HwStatusBarManagerService hwStatusBarService = (HwStatusBarManagerService) ServiceManager.getService("statusbar");
                if (hwStatusBarService != null) {
                    if (FingerprintNavigation.IS_DEBUG) {
                        Log.d(FingerprintNavigation.TAG, "collapse process, isScenseExcluded=" + FingerprintNavigation.this.isScenseExcluded() + ", mIsShowNotification=" + FingerprintNavigation.this.mIsShowNotification + ",statusBarExpanded=" + hwStatusBarService.statusBarExpanded() + ",isAlarm()=" + FingerprintNavigation.this.isAlarm() + ",isLandscape=" + FingerprintNavigation.this.isLandscape() + ",mIsRecentApp=" + FingerprintNavigation.this.mIsRecentApp);
                    }
                    FingerprintNavigation.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                    if (!FingerprintNavigation.this.mIsShowNotification || (FingerprintNavigation.this.mIsRecentApp && !hwStatusBarService.statusBarExpanded())) {
                        if (!FingerprintNavigation.this.mIsRecentApp || FingerprintNavigation.this.isAlarm() || FingerprintNavigation.this.isInCallUiAndRinging() || FingerprintNavigation.this.isLandscape()) {
                            Log.w(FingerprintNavigation.TAG, "No switch is oppened for process");
                            return;
                        }
                        StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 5);
                        FingerprintNavigation.this.sendKeyEvent(187);
                    } else if (FingerprintNavigation.this.isScenseExcluded()) {
                    } else {
                        if ((!FingerprintNavigation.this.isAlarm() || !FingerprintNavigation.this.topIsFullScreen()) && !FingerprintNavigation.this.isLandscape()) {
                            StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 2);
                            hwStatusBarService.collapsePanels();
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean canShowTransientBar() {
        AbsPhoneWindowManager wmp = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        AbsPhoneWindowManager policy = null;
        if (wmp instanceof HwPhoneWindowManager) {
            policy = (HwPhoneWindowManager) wmp;
        }
        if (policy == null) {
            Log.e(TAG, "canShowTransientBar policy is null");
            return false;
        }
        boolean isOkToShowTransientBar = policy.okToShowTransientBar();
        Log.d(TAG, "canShowTransientBar=" + isOkToShowTransientBar);
        return isOkToShowTransientBar;
    }

    /* access modifiers changed from: private */
    public boolean topIsFullScreen() {
        AbsPhoneWindowManager wmp = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        AbsPhoneWindowManager policy = null;
        if (wmp instanceof HwPhoneWindowManager) {
            policy = (HwPhoneWindowManager) wmp;
        }
        if (policy == null) {
            Log.e(TAG, "topIsFullScreen policy is null");
            return false;
        }
        boolean isTopIsFullscreen = policy.isTopIsFullscreen();
        Log.d(TAG, "isTopIsFullscreen=" + isTopIsFullscreen);
        return isTopIsFullscreen;
    }

    /* access modifiers changed from: private */
    public void requestTransientStatusBars() {
        AbsPhoneWindowManager wmp = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        AbsPhoneWindowManager policy = null;
        if (wmp instanceof HwPhoneWindowManager) {
            policy = (HwPhoneWindowManager) wmp;
        }
        if (policy == null) {
            Log.e(TAG, "requestTransientStatusBars policy is null");
        } else {
            policy.requestTransientStatusBars();
        }
    }

    final class LauncherInspector extends FingerprintNavigationInspector {
        LauncherInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "LauncherInspector State probe, isScreenOff() = " + FingerprintNavigation.this.isScreenOff() + ",isSpecialKey = " + FingerprintNavigation.this.isSpecialKey(event, 512) + ",isScenseExcluded = " + FingerprintNavigation.this.isScenseExcluded() + ",isAlarm = " + FingerprintNavigation.this.isAlarm() + ",isLandscape()=" + FingerprintNavigation.this.isLandscape());
            }
            if (FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, 512) || FingerprintNavigation.this.isScenseExcluded()) {
                return false;
            }
            if ((FingerprintNavigation.this.isAlarm() && FingerprintNavigation.this.topIsFullScreen()) || FingerprintNavigation.this.isLandscape()) {
                return false;
            }
            Log.d(FingerprintNavigation.TAG, "LauncherInspector State ok");
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "handle Launcher KEYCODE_DPAD_DOWN Event, topIsFullScreen: " + FingerprintNavigation.this.topIsFullScreen() + ", focusedWinOverStatusBar: " + FingerprintNavigation.this.focusedWinOverStatusBar() + ", obs: " + FingerprintNavigation.this.statusBarObsecured());
            }
            if (((KeyEvent) event).getAction() == 0) {
                Log.d(FingerprintNavigation.TAG, "getAction is ACTION_DOWN");
                return;
            }
            FingerprintNavigation.this.showNotificationTips();
            Log.d(FingerprintNavigation.TAG, "LauncherInspector handle mIsShowNotification :" + FingerprintNavigation.this.mIsShowNotification);
            if (FingerprintNavigation.this.mIsShowNotification && !FingerprintNavigation.this.focusedWinOverStatusBar() && !FingerprintNavigation.this.statusBarObsecured()) {
                StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 1);
                FingerprintNavigation.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                if (!FingerprintNavigation.this.topIsFullScreen() || !FingerprintNavigation.this.canShowTransientBar()) {
                    ((StatusBarManager) FingerprintNavigation.this.mContext.getSystemService("statusbar")).expandNotificationsPanel();
                } else {
                    FingerprintNavigation.this.requestTransientStatusBars();
                }
            }
        }
    }

    final class CameraInspector extends FingerprintNavigationInspector {
        CameraInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            String pkgName = FingerprintNavigation.this.getTopApp();
            if (pkgName == null || event == null) {
                return false;
            }
            boolean isGallery = pkgName.startsWith("com.huawei.photos") || pkgName.startsWith("com.android.gallery3d");
            if ((!FingerprintNavigation.this.isCamera() && !isGallery) || !FingerprintNavigation.this.mIsInjectCamera) {
                if (FingerprintNavigation.this.mIsInjectCamera) {
                    Log.d(FingerprintNavigation.TAG, "Top app is not available,reset settings DB!");
                }
                return false;
            } else if (FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, 66)) {
                return false;
            } else {
                return true;
            }
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "Current State is Camera and KEYCODE_ENTER Event!");
            }
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() == 0) {
                FingerprintNavigation.this.mHandler.postDelayed(FingerprintNavigation.this.mCameraLongPress, 0);
            } else if (ev.getAction() == 1) {
                FingerprintNavigation.this.mHandler.removeCallbacks(FingerprintNavigation.this.mCameraLongPress);
            }
        }
    }

    final class FingerprintDemoInspector extends FingerprintNavigationInspector {
        FingerprintDemoInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isKeyguardLocked() || !FingerprintNavigation.this.mIsFingerprintMarketDemoSwitch || !FingerprintNavigation.this.isFingerprintDemo()) {
                return false;
            }
            if (!FingerprintNavigation.IS_DEBUG) {
                return true;
            }
            Log.d(FingerprintNavigation.TAG, "FingerprintDemoInspector check ok");
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() != 0) {
                FingerprintNavigation.this.sendKeyEvent(ev.getKeyCode());
            }
        }
    }

    final class GalleryInspector extends FingerprintNavigationInspector {
        GalleryInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.IS_DEBUG) {
                Log.d(FingerprintNavigation.TAG, "GalleryInspector State probe");
            }
            if (!FingerprintNavigation.this.mIsGallerySlide || !FingerprintNavigation.this.isGallery() || FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isVrMode()) {
                return false;
            }
            if (!FingerprintNavigation.this.isSpecialKey(event, 513) && !FingerprintNavigation.this.isSpecialKey(event, 514)) {
                return false;
            }
            if (!FingerprintNavigation.IS_DEBUG) {
                return true;
            }
            Log.d(FingerprintNavigation.TAG, "GalleryInspector State ok");
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() != 0) {
                FingerprintNavigation.this.sendKeyEvent(ev.getKeyCode());
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isGallery() {
        String activityName = getTopApp();
        if (activityName == null) {
            Log.d(TAG, "gallery name is null");
            return false;
        } else if (activityName.startsWith("com.huawei.photos/com.huawei.gallery.app") || activityName.startsWith("com.android.gallery3d/com.huawei.gallery.app")) {
            return true;
        } else {
            return false;
        }
    }

    final class DefaultInspector extends FingerprintNavigationInspector {
        DefaultInspector() {
            super();
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isKeyguardLocked() || !FingerprintNavigation.this.mIsInjectSlide) {
                return false;
            }
            return true;
        }

        @Override // com.android.server.input.FingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            if (event instanceof MotionEvent) {
                Log.d(FingerprintNavigation.TAG, "Start Inject Motionevent!");
                MotionEvent mv = (MotionEvent) event;
                long curTime = SystemClock.uptimeMillis();
                int pointCount = mv.getPointerCount();
                MotionEvent.PointerProperties[] ppts = MotionEvent.PointerProperties.createArray(pointCount);
                MotionEvent.PointerCoords[] pcds = MotionEvent.PointerCoords.createArray(pointCount);
                for (int i = 0; i < pointCount; i++) {
                    mv.getPointerProperties(i, ppts[i]);
                    mv.getPointerCoords(i, pcds[i]);
                }
                MotionEvent mv2 = MotionEvent.obtain(curTime, curTime, mv.getAction(), pointCount, ppts, pcds, mv.getMetaState(), mv.getButtonState(), mv.getXPrecision(), mv.getYPrecision(), 5, mv.getEdgeFlags(), mv.getSource(), mv.getFlags());
                InputManager.getInstance().injectInputEvent(mv2, 0);
                if (mv2 != null) {
                    mv2.recycle();
                }
            }
        }
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        FrontFingerprintNavigation frontFingerprintNavigation = this.mFrontFingerprintNav;
        if (frontFingerprintNavigation != null) {
            frontFingerprintNavigation.setCurrentUser(newUserId);
        }
        this.mCurUser = newUserId;
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
    }

    private boolean needDropFingerprintEvent() {
        WindowManagerPolicy wmp = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        HwPhoneWindowManager policy = null;
        if (wmp instanceof HwPhoneWindowManager) {
            policy = (HwPhoneWindowManager) wmp;
        }
        if (policy != null) {
            return policy.getNeedDropFingerprintEvent();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isVrMode() {
        if (HwFrameworkFactory.getVRSystemServiceManager() != null) {
            return HwFrameworkFactory.getVRSystemServiceManager().isVRMode();
        }
        return false;
    }
}
