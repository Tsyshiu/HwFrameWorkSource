package com.android.server.gesture;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Flog;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.internal.app.AssistUtils;
import com.android.server.LocalServices;
import com.android.server.gesture.anim.HwGestureSideLayout;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.huawei.android.app.PackageManagerEx;
import com.huawei.controlcenter.ui.service.IControlCenterGesture;
import java.io.PrintWriter;
import java.util.List;

public class QuickSlideOutController extends QuickStartupStub {
    private static final String ACTION_STATUSBAR_CHANGE = "com.android.systemui.statusbar.visible.change";
    private static final String ASSISTANT_ACTION = "com.huawei.action.VOICE_ASSISTANT";
    private static final String ASSISTANT_ACTIVITY_NAME = "com.huawei.vassistant.ui.main.VAssistantActivity";
    private static final String AUTHORITY = "com.huawei.controlcenter.SwitchProvider";
    private static final Uri AUTHORITY_URI = Uri.parse("content://com.huawei.controlcenter.SwitchProvider");
    private static final String CTRLCENTER_ACTION = "com.huawei.controlcenter.action.CONTROL_CENTER_GESTURE";
    private static final String CTRLCENTER_PKG = "com.huawei.controlcenter";
    private static final String GOOGLE_ASSISTANT_PACKAGE_NAME = "com.google.android.googlequicksearchbox";
    private static final String HW_ASSISTANT_PACKAGE_NAME = "com.huawei.vassistant";
    private static final String ISCONTROLCENTERENABLE = "isControlCenterEnable";
    private static final String ISSWITCHON = "isSwitchOn";
    private static final String KEY_INVOKE = "invoke";
    private static final String KYE_VDRIVE_IS_RUN = "vdrive_is_run_state";
    private static final String METHOD_CHECK_CONTROL_CENTER_ENABLE = "getControlCenterState";
    private static final String METHOD_CHECK_SWITCH = "checkControlSwith";
    private static final int MSG_BIND_CTRLCENTER = 3;
    private static final int MSG_CLOSE_VIEW = 1;
    private static final int MSG_PRELOAD_CTRL = 4;
    private static final int MSG_UPDATE_WINDOW_VIEW = 2;
    private static final String SCANNER_APK_NAME = "HiVision";
    private static final String SCANNER_CLASS_NAME = "com.huawei.scanner.view.ScannerActivity";
    private static final String SCANNER_PACKAGE_NAME = "com.huawei.scanner";
    private static final int TYPE_NOT_PREINSTALL = 1;
    private static final int TYPE_PREINSTALL_AND_EXIST = 3;
    private static final int TYPE_PREINSTALL_BUT_UNINSTALLED = 2;
    private static final String URI_CONTROLCENTER_SWITCH = "content://com.huawei.controlcenter.SwitchProvider/controlSwitch";
    private static final String VALUE_INVOKE = "gesture_nav";
    private static final int VALUE_VDRIVE_IS_RUN = 1;
    private static final int VALUE_VDRIVE_IS_UNRUN = 0;
    private static ComponentName sVAssistantComponentName = null;
    private AssistUtils mAssistUtils;
    private final ServiceConnection mCtrlCenterConn = new ServiceConnection() {
        /* class com.android.server.gesture.QuickSlideOutController.AnonymousClass2 */

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "ControlCenterGesture Service connected");
            }
            IControlCenterGesture unused = QuickSlideOutController.this.mCtrlCenterIntf = IControlCenterGesture.Stub.asInterface(service);
            if (QuickSlideOutController.this.mCtrlCenterIntf == null) {
                Log.e(GestureNavConst.TAG_GESTURE_QSO, "mCtrlCenterIntf null object");
                return;
            }
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "success, isNeed " + QuickSlideOutController.this.mIsNeedStartAfterServiceConnected);
            }
            if (QuickSlideOutController.this.mIsNeedStartAfterServiceConnected) {
                boolean unused2 = QuickSlideOutController.this.mIsNeedStartAfterServiceConnected = false;
                QuickSlideOutController quickSlideOutController = QuickSlideOutController.this;
                quickSlideOutController.startCtrlCenter(quickSlideOutController.mIsSlidingOnLeft);
            } else if (QuickSlideOutController.this.mIsValidGuesture) {
                QuickSlideOutController.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "ControlCenterGesture Service disconnected");
            }
            IControlCenterGesture unused = QuickSlideOutController.this.mCtrlCenterIntf = null;
        }
    };
    /* access modifiers changed from: private */
    public IControlCenterGesture mCtrlCenterIntf = null;
    private float mCurrentTouch;
    /* access modifiers changed from: private */
    public Handler mHandler = new MyHandler();
    private boolean mIsAssistantGestureOn;
    private boolean mIsControlCenterGestureOn = false;
    private boolean mIsDisableSlidingAnim;
    private boolean mIsGestureNavReady;
    private boolean mIsGoogleMode = true;
    private boolean mIsInDriveMode = false;
    /* access modifiers changed from: private */
    public boolean mIsNeedStartAfterServiceConnected = false;
    private boolean mIsScannerExist = true;
    private boolean mIsScannerPreInstalled = true;
    private boolean mIsSlideOutEnabled;
    private boolean mIsSlideOverThreshold;
    private boolean mIsSlidingCtrlCenter;
    /* access modifiers changed from: private */
    public boolean mIsSlidingOnLeft = true;
    private boolean mIsSlowAnimTriggered;
    private boolean mIsStartCtrlCenter = false;
    /* access modifiers changed from: private */
    public boolean mIsStatusBarExplaned = false;
    private boolean mIsThresholdTriggered;
    private boolean mIsWindowViewSetuped;
    private final Object mLock = new Object();
    private PackageMonitorReceiver mPackageMonitorReceiver;
    private int mScannerAvailableType = 3;
    private SettingsObserver mSettingsObserver;
    private int mSlideMaxDistance;
    private float mSlidePhasePos;
    private int mSlideStartThreshold;
    private float mStartTouch;
    private StatusBarStatesChangedReceiver mStatusBarReceiver;
    private final Runnable mSuccessRunnable = new Runnable() {
        /* class com.android.server.gesture.QuickSlideOutController.AnonymousClass1 */

        public void run() {
            QuickSlideOutController.this.gestureSuccessAtEnd(false);
        }
    };
    private SlideOutContainer mViewContainer;
    private WindowManager mWindowManager;

    public QuickSlideOutController(Context context, Looper looper) {
        super(context);
        this.mAssistUtils = new AssistUtils(context);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    private void notifyStart() {
        this.mIsGestureNavReady = true;
        this.mStatusBarReceiver = new StatusBarStatesChangedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STATUSBAR_CHANGE);
        this.mContext.registerReceiverAsUser(this.mStatusBarReceiver, UserHandle.ALL, filter, "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM", this.mHandler);
        this.mPackageMonitorReceiver = new PackageMonitorReceiver();
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.PACKAGE_ADDED");
        filter2.addAction("android.intent.action.PACKAGE_REMOVED");
        filter2.addDataScheme("package");
        filter2.addDataSchemeSpecificPart(SCANNER_PACKAGE_NAME, 0);
        this.mContext.registerReceiverAsUser(this.mPackageMonitorReceiver, UserHandle.ALL, filter2, null, this.mHandler);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION_ASSISTANT), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Global.getUriFor(KYE_VDRIVE_IS_RUN), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Uri.parse(URI_CONTROLCENTER_SWITCH), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("assistant"), false, this.mSettingsObserver, -2);
        updateSettings();
        updateConfig();
        updatePackageExistState();
    }

    private void notifyStop() {
        if (this.mStatusBarReceiver != null) {
            this.mContext.unregisterReceiver(this.mStatusBarReceiver);
            this.mStatusBarReceiver = null;
        }
        if (this.mPackageMonitorReceiver != null) {
            this.mContext.unregisterReceiver(this.mPackageMonitorReceiver);
            this.mPackageMonitorReceiver = null;
        }
        if (this.mSettingsObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            this.mSettingsObserver = null;
        }
        this.mIsGestureNavReady = false;
    }

    private final class StatusBarStatesChangedReceiver extends BroadcastReceiver {
        private StatusBarStatesChangedReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && QuickSlideOutController.ACTION_STATUSBAR_CHANGE.equals(intent.getAction())) {
                String visible = "false";
                if (intent.getExtras() != null) {
                    visible = intent.getExtras().getString("visible");
                }
                if (visible != null) {
                    boolean unused = QuickSlideOutController.this.mIsStatusBarExplaned = Boolean.valueOf(visible).booleanValue();
                }
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_QSO, "mIsStatusBarExplaned:" + QuickSlideOutController.this.mIsStatusBarExplaned);
                }
            }
        }
    }

    private boolean isControlCenterSwitchOn() {
        ContentResolver resolver;
        Bundle res;
        try {
            if (!(AUTHORITY_URI == null || (res = (resolver = this.mContext.getContentResolver()).call(AUTHORITY_URI, METHOD_CHECK_CONTROL_CENTER_ENABLE, (String) null, (Bundle) null)) == null)) {
                if (res.getBoolean(ISCONTROLCENTERENABLE, false)) {
                    Bundle res2 = resolver.call(AUTHORITY_URI, METHOD_CHECK_SWITCH, (String) null, (Bundle) null);
                    if (res2 == null || !res2.getBoolean(ISSWITCHON, false)) {
                        return false;
                    }
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.w(GestureNavConst.TAG_GESTURE_QSO, "not ready.");
            return false;
        } catch (Exception e2) {
            Log.w(GestureNavConst.TAG_GESTURE_QSO, "Illegal.");
            return false;
        }
    }

    private boolean isCtrlCenterInstalled() {
        boolean isCtrlCenterExist = checkPackageExist(CTRLCENTER_PKG, ActivityManager.getCurrentUser());
        if (GestureNavConst.DEBUG) {
            Log.w(GestureNavConst.TAG_GESTURE_QSO, "ctrlCenter pkg isExist: " + isCtrlCenterExist);
        }
        return isCtrlCenterExist;
    }

    private void bindCtrlCenter() {
        if (this.mCtrlCenterIntf == null) {
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "bindCtrlCenter");
            }
            Intent intent = new Intent(CTRLCENTER_ACTION);
            intent.setPackage(CTRLCENTER_PKG);
            try {
                this.mContext.bindServiceAsUser(intent, this.mCtrlCenterConn, 1, UserHandle.of(ActivityManager.getCurrentUser()));
            } catch (SecurityException e) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "bind ControlCenterGesture Service failed");
            }
        }
    }

    /* access modifiers changed from: private */
    public void startCtrlCenter(boolean isLeft) {
        if (this.mCtrlCenterIntf != null) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "start control center: " + isLeft);
            try {
                this.mCtrlCenterIntf.startControlCenterSide(isLeft);
            } catch (RemoteException e) {
                Log.e(GestureNavConst.TAG_GESTURE_QSO, "start ctrlCenter fail");
            }
        } else {
            this.mIsNeedStartAfterServiceConnected = true;
        }
    }

    /* access modifiers changed from: private */
    public void handleCtrlCenterBindMsg() {
        if (this.mHandler.hasMessages(3)) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "repreat bind service");
        } else {
            bindCtrlCenter();
        }
    }

    /* access modifiers changed from: private */
    public void handlePreloadCtrl() {
        if (this.mHandler.hasMessages(4)) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "repreat preload");
        } else {
            preloadCtrlCenter();
        }
    }

    private void dismissCtrlCenter() {
        if (!this.mIsNeedStartAfterServiceConnected && this.mCtrlCenterIntf != null) {
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "dismiss control center: " + this.mIsStartCtrlCenter);
            }
            if (this.mIsStartCtrlCenter) {
                try {
                    this.mCtrlCenterIntf.dismissControlCenter();
                    this.mIsStartCtrlCenter = false;
                } catch (RemoteException | IllegalStateException e) {
                    Log.e(GestureNavConst.TAG_GESTURE_QSO, "dismiss ctrlCenter fail");
                }
            }
        }
    }

    private void preloadCtrlCenter() {
        if (this.mCtrlCenterIntf != null) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "preload control center:" + this.mIsSlidingOnLeft);
            try {
                this.mCtrlCenterIntf.preloadControlCenterSide(this.mIsSlidingOnLeft);
                this.mIsStartCtrlCenter = true;
            } catch (RemoteException | IllegalStateException e) {
                Log.e(GestureNavConst.TAG_GESTURE_QSO, "start ctrlCenter fail");
            }
        } else {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "preload control center, ctrlintf null");
        }
    }

    private void moveCtrlCenter(float currentTouch) {
        IControlCenterGesture iControlCenterGesture;
        if (!this.mIsNeedStartAfterServiceConnected && (iControlCenterGesture = this.mCtrlCenterIntf) != null) {
            try {
                iControlCenterGesture.moveControlCenter(currentTouch);
            } catch (RemoteException | IllegalStateException e) {
                Log.e(GestureNavConst.TAG_GESTURE_QSO, "move ctrlCenter fail");
            }
        }
    }

    private void locateCtrlCenter() {
        if (this.mIsNeedStartAfterServiceConnected) {
            this.mIsNeedStartAfterServiceConnected = false;
        } else if (this.mCtrlCenterIntf != null) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "locate control center");
            try {
                this.mCtrlCenterIntf.locateControlCenter();
            } catch (RemoteException | IllegalStateException e) {
                Log.e(GestureNavConst.TAG_GESTURE_QSO, "reset ctrlCenter fail");
            }
        } else {
            this.mIsNeedStartAfterServiceConnected = true;
        }
    }

    private final class PackageMonitorReceiver extends BroadcastReceiver {
        private PackageMonitorReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            QuickSlideOutController.this.updatePackageExistState();
        }
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange) {
            QuickSlideOutController.this.updateSettings();
        }
    }

    private boolean isSuperPowerSaveMode() {
        return GestureUtils.isSuperPowerSaveMode();
    }

    private boolean isInLockTaskMode() {
        return GestureUtils.isInLockTaskMode();
    }

    private boolean isTargetLaunched(boolean isOnLeft) {
        String focusApp = this.mDeviceStateController.getFocusPackageName();
        if (focusApp == null) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "onLeft:" + isOnLeft + ", focusApp:" + focusApp);
        }
        if (!GestureNavConst.CHINA_REGION) {
            return focusApp.equals(GOOGLE_ASSISTANT_PACKAGE_NAME);
        }
        if (isOnLeft) {
            return focusApp.equals(HW_ASSISTANT_PACKAGE_NAME);
        }
        return focusApp.equals(SCANNER_PACKAGE_NAME);
    }

    /* access modifiers changed from: private */
    public void updateSlideOutWindow() {
        synchronized (this.mLock) {
            if (!this.mIsSlideOutEnabled || !this.mIsGestureNavReady) {
                if (this.mIsWindowViewSetuped) {
                    destroySlideOutView();
                }
            } else if (!this.mIsWindowViewSetuped) {
                createSlideOutView();
            } else {
                updateSlideOutView();
            }
        }
    }

    public int slideOutThreshold(int windowThreshod) {
        return (int) (((float) windowThreshod) * 0.4f);
    }

    public boolean isSlideOutEnableAndAvailable(boolean isLeft) {
        synchronized (this.mLock) {
            boolean z = false;
            if (!this.mIsSlideOutEnabled) {
                return false;
            }
            if (isOverseaAssistantTarget(isLeft)) {
                if (this.mIsAssistantGestureOn && hasGoogleAssist()) {
                    z = true;
                }
                return z;
            }
            if (this.mIsControlCenterGestureOn && isCtrlCenterInstalled()) {
                z = true;
            }
            return z;
        }
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void onNavCreate(GestureNavView navView) {
        super.onNavCreate(navView);
        notifyStart();
        updateSlideOutWindow();
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void onNavUpdate() {
        super.onNavUpdate();
        updateSlideOutWindow();
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void onNavDestroy() {
        super.onNavDestroy();
        notifyStop();
        updateSlideOutWindow();
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void updateConfig() {
        this.mSlideStartThreshold = this.mContext.getResources().getDimensionPixelSize(34472763);
        this.mSlideMaxDistance = this.mContext.getResources().getDimensionPixelSize(34472762);
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "threshold=" + this.mSlideStartThreshold + ", max=" + this.mSlideMaxDistance);
        }
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void updateSettings() {
        synchronized (this.mLock) {
            boolean isLastSlideOutEnabled = this.mIsSlideOutEnabled;
            boolean z = false;
            this.mIsInDriveMode = Settings.Global.getInt(this.mContext.getContentResolver(), KYE_VDRIVE_IS_RUN, 0) == 1;
            this.mIsAssistantGestureOn = GestureNavConst.isAssistantGestureEnabled(this.mContext, -2);
            this.mIsControlCenterGestureOn = isControlCenterSwitchOn();
            if (this.mIsAssistantGestureOn || this.mIsControlCenterGestureOn) {
                z = true;
            }
            this.mIsSlideOutEnabled = z;
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "driveMode=" + this.mIsInDriveMode + ", assistOn=" + this.mIsAssistantGestureOn + ", ctrlCenterOn = " + this.mIsControlCenterGestureOn);
            }
            if (this.mIsSlideOutEnabled != isLastSlideOutEnabled) {
                this.mHandler.sendEmptyMessage(2);
            }
            this.mIsGoogleMode = HwGestureSideLayout.GOOGLE_VOICE_ASSISTANT.equals(Settings.Secure.getString(this.mContext.getContentResolver(), "assistant"));
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_QSO, "mIsGoogleMode:" + this.mIsGoogleMode);
            }
            if (this.mViewContainer != null) {
                this.mViewContainer.setVoiceIcon(this.mIsGoogleMode);
            }
        }
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public boolean isPreConditionNotReady(boolean isOnLeft) {
        if (!isSuperPowerSaveMode() && !this.mIsStatusBarExplaned && !this.mIsInDriveMode && !isInLockTaskMode() && !this.mDeviceStateController.isKeyguardLocked()) {
            return false;
        }
        if (!GestureNavConst.DEBUG) {
            return true;
        }
        Log.i(GestureNavConst.TAG_GESTURE_QSO, "StatusBarExplaned:" + this.mIsStatusBarExplaned + ",inDriveMode:" + this.mIsInDriveMode);
        return true;
    }

    public void setSlidingSide(boolean isOnLeft) {
        this.mIsSlidingOnLeft = isOnLeft;
        if (GestureNavConst.CHINA_REGION && !this.mIsSlidingOnLeft) {
            this.mScannerAvailableType = getScannerAvailableType();
        }
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "onLeft:" + isOnLeft + ", scannerType:" + this.mScannerAvailableType);
        }
        SlideOutContainer slideOutContainer = this.mViewContainer;
        if (slideOutContainer != null) {
            slideOutContainer.setSlidingSide(isOnLeft, this.mScannerAvailableType);
        }
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void handleTouchEvent(MotionEvent event) {
        if (this.mViewContainer != null) {
            int actionMasked = event.getActionMasked();
            if (actionMasked != 0) {
                if (actionMasked != 1) {
                    if (actionMasked == 2) {
                        handleActionMove(event);
                        return;
                    } else if (actionMasked != 3) {
                        return;
                    }
                }
                handleActionUp(event);
                return;
            }
            handleActionDown(event);
        }
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void onGestureReallyStarted() {
        super.onGestureReallyStarted();
        if (!this.mIsSlidingCtrlCenter) {
            return;
        }
        if (this.mCtrlCenterIntf == null) {
            this.mHandler.sendEmptyMessage(3);
        } else {
            preloadCtrlCenter();
        }
    }

    public void resetState(float pointerY) {
        super.resetAtDown();
        this.mViewContainer.reset();
        this.mIsThresholdTriggered = false;
        this.mIsSlideOverThreshold = false;
        this.mIsSlowAnimTriggered = false;
        this.mCurrentTouch = pointerY;
        this.mStartTouch = this.mCurrentTouch;
        this.mIsSlidingCtrlCenter = isSlidingCtrlCenter();
        boolean z = this.mIsSlidingCtrlCenter;
        this.mIsDisableSlidingAnim = z;
        if (z && this.mCtrlCenterIntf == null) {
            this.mHandler.sendEmptyMessage(3);
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetState(event.getY());
    }

    private void handleActionMove(MotionEvent event) {
        this.mCurrentTouch = event.getY();
        if (!this.mIsDisableSlidingAnim) {
            if (this.mIsGestureReallyStarted && !this.mViewContainer.isShowing()) {
                showOrb();
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_QSO, "start showOrb");
                }
            }
            if (!this.mIsSlowAnimTriggered && this.mIsGestureSlowProcessStarted) {
                this.mIsSlowAnimTriggered = true;
                notifyAnimStarted();
            }
            if (!this.mIsThresholdTriggered && this.mViewContainer.isVisible() && (slideOverThreshold(false) || !this.mViewContainer.isAnimationRunning())) {
                this.mIsThresholdTriggered = true;
                this.mSlidePhasePos = this.mCurrentTouch;
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_QSO, "slide over threshold, slidePos=" + this.mSlidePhasePos);
                }
            }
            if (this.mIsThresholdTriggered && this.mIsSlowAnimTriggered) {
                this.mIsSlideOverThreshold = slideOverThreshold(false);
                this.mViewContainer.setSlideOverThreshold(this.mIsSlideOverThreshold);
                float offset = this.mSlidePhasePos - this.mCurrentTouch;
                if (offset < 0.0f) {
                    offset = 0.0f;
                }
                this.mViewContainer.setSlideDistance(offset, MathUtils.constrain(offset, 0.0f, (float) this.mSlideMaxDistance) / ((float) this.mSlideMaxDistance));
            }
        } else if (this.mIsSlidingCtrlCenter && this.mIsGestureReallyStarted) {
            moveCtrlCenter(this.mCurrentTouch);
        }
    }

    private void handleActionUp(MotionEvent event) {
        int reportType;
        this.mCurrentTouch = event.getY();
        this.mIsSlideOverThreshold = slideOverThreshold(true);
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "slideOver=" + this.mIsSlideOverThreshold + ", valid=" + this.mIsValidGuesture + ", fast=" + this.mIsFastSlideGesture);
        }
        if (this.mIsSlidingOnLeft) {
            reportType = 850;
        } else {
            reportType = 851;
        }
        if (!this.mIsValidGuesture || !this.mIsSlideOverThreshold) {
            if (!this.mIsDisableSlidingAnim) {
                this.mViewContainer.startExitAnimation(false, false);
            } else {
                dismissCtrlCenter();
            }
            Flog.bdReport(this.mContext, reportType, GestureNavConst.reportResultStr(false, this.mGestureFailedReason));
            return;
        }
        performSlideEndAction(this.mIsFastSlideGesture);
        Flog.bdReport(this.mContext, reportType, GestureNavConst.reportResultStr(true, -1));
    }

    private boolean slideOverThreshold(boolean isCheckAtEnd) {
        if (!isCheckAtEnd || Math.abs(this.mStartTouch - this.mCurrentTouch) > ((float) this.mSlideStartThreshold)) {
            return true;
        }
        return false;
    }

    private void performSlideEndAction(boolean isFastSlide) {
        if (this.mIsDisableSlidingAnim || !this.mViewContainer.isAnimationRunning()) {
            gestureSuccessAtEnd(isFastSlide);
            return;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "preform action until anim finished");
        }
        this.mViewContainer.performOnAnimationFinished(this.mSuccessRunnable);
    }

    /* access modifiers changed from: private */
    public void gestureSuccessAtEnd(boolean isFastSlide) {
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "execute animation and start target, isFastSlide:" + isFastSlide);
        }
        if (!this.mIsDisableSlidingAnim) {
            this.mViewContainer.startExitAnimation(isFastSlide, true);
        }
        startTarget();
    }

    private void startTarget() {
        if (GestureNavConst.CHINA_REGION) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "start controlcenter");
            locateCtrlCenter();
        } else if (isOverseaAssistantTarget(this.mIsSlidingOnLeft)) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "start voice assistant");
            startVoiceAssist();
        } else {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "start controlcenter");
            locateCtrlCenter();
        }
    }

    private boolean isSlidingCtrlCenter() {
        if (isOverseaAssistantTarget(this.mIsSlidingOnLeft)) {
            return false;
        }
        return true;
    }

    private boolean isOverseaAssistantTarget(boolean isLeft) {
        return !GestureNavConst.CHINA_REGION;
    }

    private void startVoiceAssist() {
        try {
            StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarManager != null) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "startAssist");
                Bundle bundle = new Bundle();
                bundle.putBoolean("isFromGesture", true);
                statusBarManager.startAssist(bundle);
            }
        } catch (IllegalArgumentException e) {
            Log.e(GestureNavConst.TAG_GESTURE_QSO, "startVoiceAssist catch IllegalArgumentException");
        } catch (Exception e2) {
            Log.e(GestureNavConst.TAG_GESTURE_QSO, "startVoiceAssist catch Exception");
        }
    }

    private ComponentName getAssistInfo() {
        AssistUtils assistUtils = this.mAssistUtils;
        if (assistUtils != null) {
            return assistUtils.getAssistComponentForUser(-2);
        }
        return null;
    }

    private boolean hasAssist() {
        if (GestureNavConst.CHINA_REGION) {
            return checkPackageExist(HW_ASSISTANT_PACKAGE_NAME, ActivityManager.getCurrentUser());
        }
        return hasGoogleAssist();
    }

    private boolean hasGoogleAssist() {
        if (getAssistInfo() == null) {
            return false;
        }
        return true;
    }

    private ComponentName getVoiceInteractorComponentName() {
        AssistUtils assistUtils = this.mAssistUtils;
        if (assistUtils != null) {
            return assistUtils.getActiveServiceComponentName();
        }
        return null;
    }

    private final class MyHandler extends Handler {
        private MyHandler() {
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                QuickSlideOutController.this.hideSlideOutView();
            } else if (i == 2) {
                QuickSlideOutController.this.updateSlideOutWindow();
            } else if (i == 3) {
                QuickSlideOutController.this.handleCtrlCenterBindMsg();
            } else if (i == 4) {
                QuickSlideOutController.this.handlePreloadCtrl();
            }
        }
    }

    private void createSlideOutView() {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "createSlideOutView");
        }
        this.mViewContainer = (SlideOutContainer) LayoutInflater.from(this.mContext).inflate(34013349, (ViewGroup) null);
        this.mViewContainer.setOnTouchListener(new TouchOutsideListener(1));
        this.mViewContainer.setVisibility(8);
        GestureUtils.addWindowView(this.mWindowManager, this.mViewContainer, getSlideOutLayoutParams());
        this.mIsWindowViewSetuped = true;
    }

    private void updateSlideOutView() {
        GestureUtils.updateViewLayout(this.mWindowManager, this.mViewContainer, getSlideOutLayoutParams());
    }

    private void destroySlideOutView() {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "destroySlideOutView");
        }
        this.mIsWindowViewSetuped = false;
        GestureUtils.removeWindowView(this.mWindowManager, this.mViewContainer, true);
        this.mViewContainer = null;
    }

    /* access modifiers changed from: private */
    public void hideSlideOutView() {
        try {
            if (this.mViewContainer != null) {
                this.mViewContainer.hide(true);
            }
        } catch (IllegalArgumentException e) {
            Log.e(GestureNavConst.TAG_GESTURE_QSO, "hideSlideOutView catch IllegalArgumentException");
        } catch (Exception e2) {
            Log.e(GestureNavConst.TAG_GESTURE_QSO, "hideSlideOutView catch Exception");
        }
    }

    private WindowManager.LayoutParams getSlideOutLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(HwArbitrationDEFS.MSG_VPN_STATE_OPEN, 8519936, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.width = -1;
        lp.height = -1;
        lp.windowAnimations = 16974596;
        lp.softInputMode = 49;
        lp.setTitle("GestureSildeOut");
        return lp;
    }

    private class TouchOutsideListener implements View.OnTouchListener {
        private int mMsg;

        TouchOutsideListener(int msg) {
            this.mMsg = msg;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            int action = ev.getAction();
            if (action != 4 && action != 0) {
                return false;
            }
            QuickSlideOutController.this.mHandler.removeMessages(this.mMsg);
            QuickSlideOutController.this.mHandler.sendEmptyMessage(this.mMsg);
            return true;
        }
    }

    private void showOrb() {
        this.mViewContainer.show(true);
    }

    private void notifyAnimStarted() {
        this.mViewContainer.startEnterAnimation();
    }

    private void replaceDrawable(ImageView imageView, ComponentName component, String name) {
        replaceDrawable(imageView, component, name, false);
    }

    private void replaceDrawable(ImageView imageView, ComponentName component, String name, boolean isService) {
        Bundle metaData;
        int iconResId;
        Resources res;
        if (component != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                if (isService) {
                    metaData = packageManager.getServiceInfo(component, 128).metaData;
                } else {
                    metaData = packageManager.getActivityInfo(component, 128).metaData;
                }
                if (metaData != null && (iconResId = metaData.getInt(name)) != 0) {
                    if (isService) {
                        res = packageManager.getResourcesForApplication(component.getPackageName());
                    } else {
                        res = packageManager.getResourcesForActivity(component);
                    }
                    imageView.setImageDrawable(res.getDrawable(iconResId));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "Failed to swap drawable; " + component.flattenToShortString() + " not found", e);
            } catch (Resources.NotFoundException nfe) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "Failed to swap drawable from " + component.flattenToShortString(), nfe);
            }
        }
    }

    private ComponentName getVoiceAssistantComponentName() {
        ComponentName componentName = sVAssistantComponentName;
        if (componentName != null) {
            return componentName;
        }
        if (GestureNavConst.CHINA_REGION) {
            sVAssistantComponentName = new ComponentName(HW_ASSISTANT_PACKAGE_NAME, ASSISTANT_ACTIVITY_NAME);
            PackageManager packageManager = this.mContext.getPackageManager();
            if (packageManager == null) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "packageManager is null");
                return sVAssistantComponentName;
            }
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(new Intent(ASSISTANT_ACTION), 65536);
            if (resolveInfos == null || resolveInfos.size() == 0) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "resolveInfos is null");
                return sVAssistantComponentName;
            }
            ComponentInfo info = resolveInfos.get(0).activityInfo;
            if (info == null) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "activityInfo is null");
                return sVAssistantComponentName;
            }
            sVAssistantComponentName = new ComponentName(info.packageName, info.name);
        } else {
            sVAssistantComponentName = getAssistInfo();
        }
        return sVAssistantComponentName;
    }

    private boolean startScanner() {
        if (this.mContext == null) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "context error");
            return false;
        }
        Intent aiIntent = new Intent();
        aiIntent.setFlags(268435456);
        aiIntent.setFlags(65536);
        aiIntent.setClassName(SCANNER_PACKAGE_NAME, SCANNER_CLASS_NAME);
        aiIntent.putExtra(KEY_INVOKE, VALUE_INVOKE);
        try {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "start scanner");
            this.mContext.startActivityAsUser(aiIntent, UserHandle.CURRENT);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void updatePackageExistState() {
        boolean isAppExistInMainUser;
        int currentUserId = ActivityManager.getCurrentUser();
        this.mIsScannerExist = checkPackageExist(SCANNER_PACKAGE_NAME, currentUserId);
        if (!this.mIsScannerExist) {
            this.mIsScannerPreInstalled = false;
            if (currentUserId != 0) {
                isAppExistInMainUser = checkPackageExist(SCANNER_PACKAGE_NAME, 0);
            } else {
                isAppExistInMainUser = false;
            }
            if (isAppExistInMainUser) {
                this.mIsScannerPreInstalled = true;
            } else {
                checkScanInstallList();
            }
        } else {
            this.mIsScannerPreInstalled = true;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "scannerExist=" + this.mIsScannerExist + ", scannerPreInstalled=" + this.mIsScannerPreInstalled);
        }
    }

    private void checkScanInstallList() {
        List<String> list = PackageManagerEx.getScanInstallList();
        if (list != null) {
            for (String apkPatch : list) {
                if (apkPatch != null && apkPatch.contains(SCANNER_APK_NAME)) {
                    this.mIsScannerPreInstalled = true;
                    return;
                }
            }
        }
    }

    private boolean checkPackageExist(String packageName, int userId) {
        try {
            this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 128, userId);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(GestureNavConst.TAG_GESTURE_QSO, packageName + " not found for userId:" + userId);
            return false;
        } catch (Exception e2) {
            Log.w(GestureNavConst.TAG_GESTURE_QSO, packageName + " not available for userId:" + userId);
            return false;
        }
    }

    private int getScannerAvailableType() {
        if (this.mIsScannerExist) {
            return 3;
        }
        if (this.mIsScannerPreInstalled) {
            return 2;
        }
        return 1;
    }

    private void showNotAvailableToast() {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "showNotAvailableToast");
        }
        this.mHandler.post(new Runnable() {
            /* class com.android.server.gesture.QuickSlideOutController.AnonymousClass3 */

            public void run() {
                Toast toast = Toast.makeText(QuickSlideOutController.this.mContext, 33686229, 0);
                toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                toast.getWindowParams().privateFlags |= 16;
                toast.show();
            }
        });
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        pw.print("sidleOutEnabled=" + this.mIsSlideOutEnabled);
        pw.print(" assitOn=" + this.mIsAssistantGestureOn);
        pw.print(" ctrlCenterOn=" + this.mIsControlCenterGestureOn);
        pw.print(" hasGoogleAssist=" + hasGoogleAssist());
        pw.print(" hasCtrlCenter=" + isCtrlCenterInstalled());
        pw.println();
        pw.print(prefix);
        pw.print("driveMode=" + this.mIsInDriveMode);
        pw.print(" statusBarExplaned=" + this.mIsStatusBarExplaned);
        pw.print(" superPowerMode=" + isSuperPowerSaveMode());
        pw.println();
    }
}
