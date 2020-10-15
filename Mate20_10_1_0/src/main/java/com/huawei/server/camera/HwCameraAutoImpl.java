package com.huawei.server.camera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IHwActivityNotifierEx;
import com.huawei.android.media.AudioManagerEx;
import com.huawei.android.media.IAudioModeCallback;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.android.os.TraceEx;
import com.huawei.android.os.UserHandleEx;
import com.huawei.android.provider.SettingsEx;
import com.huawei.android.view.WindowManagerEx;
import com.huawei.hwextdevice.HWExtDeviceEvent;
import com.huawei.hwextdevice.HWExtDeviceEventListener;
import com.huawei.hwextdevice.HWExtDeviceManager;
import com.huawei.hwextdevice.devices.HWExtMotion;
import com.huawei.hwpartcameraservice.BuildConfig;
import com.huawei.telephony.PhoneStateListenerEx;
import com.huawei.util.LogEx;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HwCameraAutoImpl {
    private static final String ACTION_CAMERA_DESCEND = "com.huawei.camera.action.CAMERA_DESCEND";
    private static final String ACTION_CAMERA_RISE = "com.huawei.camera.action.CAMERA_RISE";
    private static final String ACTIVITY_NOTIFY_COMPONENT_NAME = "comp";
    private static final String ACTIVITY_NOTIFY_ON_PAUSE = "onPause";
    private static final String ACTIVITY_NOTIFY_ON_RESUME = "onResume";
    private static final String ACTIVITY_NOTIFY_REASON = "activityLifeState";
    private static final String ACTIVITY_NOTIFY_STATE = "state";
    private static final String AUTO_CAMERA_STATUS_PERMISSION = "com.huawei.camera.permission.AUTO_CAMERA_STATUS";
    private static final String CAMERA_DESCENT_SOUND_FILE = "../media/audio/camera/camera_descent.ogg";
    private static final int CAMERA_LIFT_SOUNDS_DISABLED = 0;
    private static final int CAMERA_LIFT_SOUNDS_ENABLED = 1;
    private static final String CAMERA_LIFT_SOUNDS_KEY = "camera_lift_sounds_enabled";
    private static final String CAMERA_LIFT_SOUND_FILE = "../media/audio/camera/camera_lift.ogg";
    private static final int CAMERA_STATE_CONFIG_STREAM = 102;
    private static final int CAMERA_STATE_ENTER_GALLERY = 100;
    private static final int CAMERA_STATE_OPEN_SUCCESS = 101;
    private static final String CONSTANT_DEFAULT_STRING = "default";
    private static final int DELAY_TIME_DEFAULT = 220;
    private static final int DELAY_TIME_MAX = 5000;
    private static final int DELAY_TIME_MIN = 0;
    private static final int DP_MARGIN_TOP = 40;
    private static final String FALSE = "false";
    private static final Object HANDLER_THREAD_SYNC_OBJ = new Object();
    private static final String HW_CAMERA_MOCK = "HW.Camera.Mock";
    private static final String HW_CAMERA_NAME = "com.huawei.camera";
    private static final String HW_LAUNCHER_PKG_NAME = "com.huawei.android.launcher";
    private static final boolean IS_HW_DEBUG = (LogEx.getLogHWInfo() || (LogEx.getHWModuleLog() && Log.isLoggable(TAG, 3)));
    private static final int LENGTH = 2;
    private static final int MEDIUM = 160;
    private static final String MOTION_THREAD_NAME = "motionThread";
    private static final int MOTOR_CONTROL_DESCEND = 1;
    private static final int MOTOR_CONTROL_RISE = 0;
    private static final int RES_CLOSE_EMERGENCY = 109;
    private static final int RES_CLOSE_FAIL = 104;
    private static final int RES_CLOSE_SUCCESS = 103;
    private static final int RES_EVENT_PRESS = 107;
    private static final int RES_OPEN_FAIL = 102;
    private static final int RES_OPEN_SUCCESS = 101;
    private static final int RES_REQUEST_OVER_FREQ = 108;
    private static final int RES_START_CLOSE = 111;
    private static final int RES_START_OPEN = 110;
    private static final int SPECIAL_DELAY_TIME = 0;
    private static final int SPECIAL_DURATION_TIME = 100;
    private static final String STRATEGY_ACTIVITY_AND_NO_CLOSE = "2";
    private static final String STRATEGY_ACTIVITY_AND_ORIGIN = "1";
    private static final String STRATEGY_DELAY_CLOSE = "5";
    private static final String STRATEGY_NO_HANDLE = "0";
    private static final String STRATEGY_VIDEO_CALL = "3";
    private static final String STRATEGY_VIDEO_PHONE = "4";
    private static final String TAG = "HwCameraAutoImpl";
    private static final String TEST_FILE_NAME = "/data/cameratest.txt";
    private static final int TOAST_TEXT_SIZE = 13;
    private static final String TRUE = "true";
    private static final String WHITELIST_FILE_NAME = "/hw_product/etc/camera/camera_whitelist.json";
    private IHwActivityNotifierEx activityNotifierEx = new IHwActivityNotifierEx() {
        /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass6 */

        public void call(Bundle extras) {
            if (extras == null) {
                Log.e(HwCameraAutoImpl.TAG, "AMS callback, extras is null!");
                return;
            }
            Parcelable obj = extras.getParcelable(HwCameraAutoImpl.ACTIVITY_NOTIFY_COMPONENT_NAME);
            ComponentName componentName = null;
            if (obj instanceof ComponentName) {
                componentName = (ComponentName) obj;
            }
            String packageName = BuildConfig.FLAVOR;
            String className = componentName != null ? componentName.getClassName() : packageName;
            if (componentName != null) {
                packageName = componentName.getPackageName();
            }
            String state = extras.getString(HwCameraAutoImpl.ACTIVITY_NOTIFY_STATE);
            if (packageName != null && packageName.equals(HwCameraAutoImpl.this.currentFrontCameraClient) && HwCameraAutoImpl.this.isFrontCameraOpen && HwCameraAutoImpl.ACTIVITY_NOTIFY_ON_RESUME.equals(state) && !HwCameraAutoImpl.this.isShutdownReceive) {
                HwCameraAutoImpl.this.registerPopDeviceListener(0, HwCameraAutoImpl.DELAY_TIME_DEFAULT);
                Log.v(HwCameraAutoImpl.TAG, "Front camera is opened when back to same client");
            } else if (HwCameraAutoImpl.this.frontCameraActivity.equals(className)) {
                String strategy = HwCameraAutoImpl.this.getStrategyByActivity(className);
                if (!HwCameraAutoImpl.STRATEGY_ACTIVITY_AND_ORIGIN.equals(strategy) && !HwCameraAutoImpl.STRATEGY_ACTIVITY_AND_NO_CLOSE.equals(strategy)) {
                    return;
                }
                if (HwCameraAutoImpl.ACTIVITY_NOTIFY_ON_PAUSE.equals(state)) {
                    HwCameraAutoImpl.this.registerPopDeviceListener(1, HwCameraAutoImpl.DELAY_TIME_DEFAULT);
                    HwCameraAutoImpl.this.dismissSlidedownTip();
                    HwCameraAutoImpl.this.dismissCoverDialog();
                } else if (HwCameraAutoImpl.this.isFrontCameraOpen && HwCameraAutoImpl.ACTIVITY_NOTIFY_ON_RESUME.equals(state)) {
                    HwCameraAutoImpl.this.registerPopDeviceListener(0, HwCameraAutoImpl.DELAY_TIME_DEFAULT);
                }
            }
        }
    };
    private IAudioModeCallback audioCallback = new IAudioModeCallback() {
        /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass4 */

        public void onAudioModeChanged(int mode) {
            if (mode == 3) {
                Log.w(HwCameraAutoImpl.TAG, "currentMode is in communication");
                HwCameraAutoImpl.this.registerPopDeviceListener(0, 0);
            }
        }
    };
    private AudioManager audioManager = null;
    private AudioManagerEx audioManagerEx = null;
    private Context cameraContext = null;
    private int cameraDecentSoundId = 0;
    private int cameraLiftSoundId = 0;
    private Dialog coverDialog = null;
    private BroadcastReceiver coverStateReceiver = new BroadcastReceiver() {
        /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.w(HwCameraAutoImpl.TAG, "onReceive, the intent is null!");
                return;
            }
            Log.i(HwCameraAutoImpl.TAG, "onReceive screen off or home");
            HwCameraAutoImpl.this.dismissSlidedownTip();
            HwCameraAutoImpl.this.dismissCoverDialog();
            if (HwCameraAutoImpl.this.isEnterGallery) {
                HwCameraAutoImpl.this.registerPopDeviceListener(1, 0);
                boolean unused = HwCameraAutoImpl.this.isEnterGallery = false;
            }
        }
    };
    /* access modifiers changed from: private */
    public String currentFrontCameraClient = BuildConfig.FLAVOR;
    private Map<String, Integer> delayTimeMaps = null;
    private int densityDpiVar = 0;
    private long endTime = 0;
    /* access modifiers changed from: private */
    public String frontCameraActivity = BuildConfig.FLAVOR;
    private Dialog hwCameraAutoImplDialog = null;
    private Handler hwCameraAutoImplHandler = null;
    private IntentFilter hwCameraAutoImplIntentFilter = null;
    private Toast hwCameraAutoImplToast = null;
    private HWExtDeviceEventListener hwExtDevEventListener = new HWExtDeviceEventListener() {
        /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass3 */

        public void onDeviceDataChanged(HWExtDeviceEvent hwextDeviceEvent) {
            float[] deviceValues = hwextDeviceEvent.getDeviceValues();
            if (deviceValues == null || deviceValues.length < 1) {
                Log.w(HwCameraAutoImpl.TAG, "onDeviceDataChanged deviceValues is null");
            } else {
                HwCameraAutoImpl.this.handleMotionResult(deviceValues);
            }
        }
    };
    private HWExtDeviceManager hwExtDevManager = null;
    private HWExtMotion hwExtMotion = null;
    private Intent intentToHome = null;
    /* access modifiers changed from: private */
    public boolean isEnterGallery = false;
    private boolean isFirstCoverPopup = true;
    /* access modifiers changed from: private */
    public boolean isFrontCameraOpen = false;
    private boolean isScreenOffBroadcastRegisterd = false;
    private boolean isShutDownBroadcastRegisterd = false;
    /* access modifiers changed from: private */
    public boolean isShutdownReceive = false;
    private boolean isSpecialScene = false;
    private SoundPool mSoundPool = new SoundPool(1, 1, 0);
    private Handler motionHandler = null;
    private HandlerThread motionThread = null;
    private DialogInterface.OnClickListener onCoverClickListener = new DialogInterface.OnClickListener() {
        /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$mts0952iKKusu2CGwoWC3d7Ic */

        public final void onClick(DialogInterface dialogInterface, int i) {
            HwCameraAutoImpl.this.lambda$new$1$HwCameraAutoImpl(dialogInterface, i);
        }
    };
    private DialogInterface.OnClickListener onTtyClickListener = new DialogInterface.OnClickListener() {
        /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$IaC2zb82dzjjlPLc_xBjrclmjCA */

        public final void onClick(DialogInterface dialogInterface, int i) {
            HwCameraAutoImpl.this.lambda$new$0$HwCameraAutoImpl(dialogInterface, i);
        }
    };
    private PhoneStateListenerEx phoneStateListener = new PhoneStateListenerEx(Looper.getMainLooper()) {
        /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass5 */

        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == HwCameraAutoImpl.LENGTH) {
                Log.i(HwCameraAutoImpl.TAG, "callstate is offhook");
                HwCameraAutoImpl.this.registerPopDeviceListener(1, 0);
            }
        }
    };
    private IntentFilter shutDownIntentFilter = null;
    private BroadcastReceiver shutDownStateReceiver = new BroadcastReceiver() {
        /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.w(HwCameraAutoImpl.TAG, "onReceive, the intent is null!");
                return;
            }
            Log.i(HwCameraAutoImpl.TAG, "onReceive shutdown");
            boolean unused = HwCameraAutoImpl.this.isShutdownReceive = true;
            HwCameraAutoImpl.this.registerPopDeviceListener(1, 0);
        }
    };
    private String specialActivity = CONSTANT_DEFAULT_STRING;
    private long startTime = 0;
    private Map<String, List<String>> strategyMaps = null;
    private TelephonyManager telephonyManager = null;

    public /* synthetic */ void lambda$new$0$HwCameraAutoImpl(DialogInterface dialog, int which) {
        if (which == -2) {
            backToHome();
            dismissSlidedownTip();
        } else if (which == -1) {
            registerPopDeviceListener(0, 0);
        }
    }

    public /* synthetic */ void lambda$new$1$HwCameraAutoImpl(DialogInterface dialog, int which) {
        if (which == -2) {
            dismissCoverDialog();
        } else if (which == -1) {
            this.isFirstCoverPopup = false;
            registerPopDeviceListener(0, 0);
        }
    }

    private void loadSoundFile() {
        File cameraLiftSoundFile = HwCfgFilePolicy.getCfgFile(CAMERA_LIFT_SOUND_FILE, 0);
        File cameraDescentSoundFile = HwCfgFilePolicy.getCfgFile(CAMERA_DESCENT_SOUND_FILE, 0);
        if (cameraLiftSoundFile != null && cameraDescentSoundFile != null) {
            try {
                this.cameraLiftSoundId = this.mSoundPool.load(cameraLiftSoundFile.getCanonicalPath(), 1);
                this.cameraDecentSoundId = this.mSoundPool.load(cameraDescentSoundFile.getCanonicalPath(), 1);
            } catch (IOException e) {
                Log.e(TAG, "Fail to get camera popup sound file");
            }
        }
    }

    public HwCameraAutoImpl(Context context) {
        if (context != null) {
            this.cameraContext = context;
            loadSoundFile();
            this.hwExtDevManager = HWExtDeviceManager.getInstance(this.cameraContext);
            this.hwExtMotion = new HWExtMotion(3100);
            Object audioServiceObj = this.cameraContext.getSystemService("audio");
            if (audioServiceObj instanceof AudioManager) {
                this.audioManager = (AudioManager) audioServiceObj;
            } else {
                Log.e(TAG, "Fail to set audioManager");
            }
            this.audioManagerEx = new AudioManagerEx();
            Object telephonyServiceObj = this.cameraContext.getSystemService("phone");
            if (telephonyServiceObj instanceof TelephonyManager) {
                this.telephonyManager = (TelephonyManager) telephonyServiceObj;
            } else {
                Log.e(TAG, "Fail to set telephonyManager");
            }
            this.densityDpiVar = this.cameraContext.getResources().getDisplayMetrics().densityDpi;
            this.intentToHome = new Intent("android.intent.action.MAIN");
            this.intentToHome.addCategory("android.intent.category.HOME");
            this.intentToHome.setFlags(268435456);
            this.intentToHome.setPackage(HW_LAUNCHER_PKG_NAME);
            this.hwCameraAutoImplIntentFilter = new IntentFilter();
            this.hwCameraAutoImplIntentFilter.addAction("android.intent.action.SCREEN_OFF");
            this.hwCameraAutoImplIntentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            this.shutDownIntentFilter = new IntentFilter();
            this.shutDownIntentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
            this.hwCameraAutoImplHandler = new Handler(Looper.getMainLooper());
            this.hwCameraAutoImplHandler.post(new Runnable() {
                /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$7htG_o7gWfojnn6uBJiF1JirE */

                public final void run() {
                    HwCameraAutoImpl.this.lambda$new$2$HwCameraAutoImpl();
                }
            });
            initWhiteList();
            registerActivityNotifier();
            return;
        }
        Log.e(TAG, "context is null!");
    }

    public /* synthetic */ void lambda$new$2$HwCameraAutoImpl() {
        this.hwCameraAutoImplToast = new Toast(this.cameraContext.getApplicationContext());
        initToast(this.densityDpiVar);
        this.hwCameraAutoImplToast.setDuration(0);
    }

    private void initWhiteList() {
        int i = 0;
        String content = new ReadFileInfo(WHITELIST_FILE_NAME, false).readFileToString();
        if (!content.isEmpty()) {
            try {
                JSONArray strategyArray = new JSONArray(content);
                int strategyNumber = strategyArray.length();
                this.strategyMaps = new HashMap(strategyNumber);
                JSONArray delayTimeArray = null;
                int i2 = 0;
                while (i2 < strategyNumber) {
                    JSONObject strategyObject = strategyArray.getJSONObject(i2);
                    JSONArray activityArray = strategyObject.getJSONArray("activity");
                    String strategyId = strategyObject.getString("id");
                    int activityNumber = activityArray == null ? i : activityArray.length();
                    if (STRATEGY_DELAY_CLOSE.equals(strategyId)) {
                        delayTimeArray = strategyObject.optJSONArray("delaytime");
                        this.delayTimeMaps = new HashMap(activityNumber);
                    }
                    List<String> whiteList = new ArrayList<>(activityNumber);
                    for (int j = 0; j < activityNumber; j++) {
                        whiteList.add(activityArray.getString(j));
                        if (!(this.delayTimeMaps == null || delayTimeArray == null)) {
                            this.delayTimeMaps.put(activityArray.getString(j), Integer.valueOf(delayTimeArray.optInt(j)));
                        }
                    }
                    this.strategyMaps.put(strategyId, whiteList);
                    i2++;
                    i = 0;
                }
            } catch (JSONException e) {
                Log.e(TAG, "parase camera json throw JSONException");
            }
        }
    }

    private void initToast(int densityDpi) {
        Object layoutInflaterServiceObj = this.cameraContext.getSystemService("layout_inflater");
        if (layoutInflaterServiceObj instanceof LayoutInflater) {
            this.hwCameraAutoImplToast.setView(((LayoutInflater) layoutInflaterServiceObj).inflate(34013436, (ViewGroup) null));
        } else {
            Log.e(TAG, "Fail to set inflater");
        }
        this.hwCameraAutoImplToast.setGravity(49, 0, (densityDpi / MEDIUM) * DP_MARGIN_TOP);
    }

    private void showToast(String text) {
        this.hwCameraAutoImplHandler.post(new Runnable(text) {
            /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$43WD29bYKqBTWvi9w9ItPOcGwfQ */
            private final /* synthetic */ String f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwCameraAutoImpl.this.lambda$showToast$3$HwCameraAutoImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$showToast$3$HwCameraAutoImpl(String text) {
        if (this.hwCameraAutoImplToast != null) {
            int densityDpi = this.cameraContext.getResources().getDisplayMetrics().densityDpi;
            if (densityDpi != this.densityDpiVar) {
                this.densityDpiVar = densityDpi;
                initToast(this.densityDpiVar);
            }
            View viewObj = this.hwCameraAutoImplToast.getView().findViewById(34603183);
            if (viewObj instanceof TextView) {
                TextView tv = (TextView) viewObj;
                tv.setText(text);
                tv.setTextSize(13.0f);
            } else {
                Log.e(TAG, "Fail to set textView");
            }
            this.hwCameraAutoImplToast.show();
        }
    }

    private void registerBroadcast() {
        if (!this.isScreenOffBroadcastRegisterd) {
            this.cameraContext.registerReceiver(this.coverStateReceiver, this.hwCameraAutoImplIntentFilter);
            this.isScreenOffBroadcastRegisterd = true;
        }
        if (!this.isShutDownBroadcastRegisterd) {
            this.cameraContext.registerReceiver(this.shutDownStateReceiver, this.shutDownIntentFilter);
            this.isShutDownBroadcastRegisterd = true;
        }
    }

    private void unregisterBroadcast() {
        if (this.isScreenOffBroadcastRegisterd) {
            try {
                this.cameraContext.unregisterReceiver(this.coverStateReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unregister coverStateReceiver fail");
            }
            this.isScreenOffBroadcastRegisterd = false;
        }
        if (this.isShutDownBroadcastRegisterd) {
            try {
                this.cameraContext.unregisterReceiver(this.shutDownStateReceiver);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Unregister shutDownStateReceiver fail");
            }
            this.isShutDownBroadcastRegisterd = false;
        }
    }

    /* access modifiers changed from: private */
    public void backToHome() {
        this.cameraContext.getApplicationContext().startActivity(this.intentToHome);
    }

    private void showDialog() {
        this.hwCameraAutoImplHandler.post(new Runnable() {
            /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$qHkQ5cAco5TeAAcq5z456yoWqdc */

            public final void run() {
                HwCameraAutoImpl.this.lambda$showDialog$4$HwCameraAutoImpl();
            }
        });
    }

    public /* synthetic */ void lambda$showDialog$4$HwCameraAutoImpl() {
        this.hwCameraAutoImplDialog = new AlertDialog.Builder(this.cameraContext.getApplicationContext(), 33947691).setMessage(33685563).setPositiveButton(33685574, this.onTtyClickListener).setNegativeButton(33685573, this.onTtyClickListener).create();
        this.hwCameraAutoImplDialog.getWindow().setType(2009);
        WindowManagerEx.LayoutParamsEx hwLayoutParams = new WindowManagerEx.LayoutParamsEx(this.hwCameraAutoImplDialog.getWindow().getAttributes());
        hwLayoutParams.setLayoutParamsPrivateFlags(WindowManagerEx.LayoutParamsEx.getPrivateFlagShowForAllUsers());
        this.hwCameraAutoImplDialog.getWindow().setAttributes(hwLayoutParams.getLayoutParams());
        this.hwCameraAutoImplDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass7 */

            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode != 4) {
                    return false;
                }
                HwCameraAutoImpl.this.backToHome();
                HwCameraAutoImpl.this.dismissSlidedownTip();
                return false;
            }
        });
        Dialog dialog = this.hwCameraAutoImplDialog;
        if (dialog != null && dialog.isShowing()) {
            this.hwCameraAutoImplDialog.dismiss();
        }
        this.hwCameraAutoImplDialog.show();
    }

    /* access modifiers changed from: private */
    public void dismissSlidedownTip() {
        this.hwCameraAutoImplHandler.post(new Runnable() {
            /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$Yp2iMUvYOLN5WOCbR9H_zNQ6T0 */

            public final void run() {
                HwCameraAutoImpl.this.lambda$dismissSlidedownTip$5$HwCameraAutoImpl();
            }
        });
    }

    public /* synthetic */ void lambda$dismissSlidedownTip$5$HwCameraAutoImpl() {
        Dialog dialog = this.hwCameraAutoImplDialog;
        if (dialog != null && dialog.isShowing()) {
            Log.i(TAG, "dismiss hwCameraAutoImplDialog");
            this.hwCameraAutoImplDialog.dismiss();
            unregisterBroadcast();
        }
    }

    private void showCoverDialog() {
        this.hwCameraAutoImplHandler.post(new Runnable() {
            /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$sMc9yJyW2UjVaMBoii6XX110B8 */

            public final void run() {
                HwCameraAutoImpl.this.lambda$showCoverDialog$6$HwCameraAutoImpl();
            }
        });
    }

    public /* synthetic */ void lambda$showCoverDialog$6$HwCameraAutoImpl() {
        this.coverDialog = new AlertDialog.Builder(this.cameraContext.getApplicationContext(), 33947691).setMessage(33685570).setPositiveButton(33685571, this.onCoverClickListener).setNegativeButton(33685573, this.onCoverClickListener).create();
        this.coverDialog.getWindow().setType(2009);
        WindowManagerEx.LayoutParamsEx hwLayoutParams = new WindowManagerEx.LayoutParamsEx(this.coverDialog.getWindow().getAttributes());
        hwLayoutParams.setLayoutParamsPrivateFlags(WindowManagerEx.LayoutParamsEx.getPrivateFlagShowForAllUsers());
        this.coverDialog.getWindow().setAttributes(hwLayoutParams.getLayoutParams());
        this.coverDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            /* class com.huawei.server.camera.HwCameraAutoImpl.AnonymousClass8 */

            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode != 4) {
                    return false;
                }
                HwCameraAutoImpl.this.backToHome();
                HwCameraAutoImpl.this.dismissCoverDialog();
                return false;
            }
        });
        Dialog dialog = this.coverDialog;
        if (dialog != null && dialog.isShowing()) {
            this.coverDialog.dismiss();
        }
        this.coverDialog.show();
    }

    /* access modifiers changed from: private */
    public void dismissCoverDialog() {
        this.isFirstCoverPopup = true;
        this.hwCameraAutoImplHandler.post(new Runnable() {
            /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$SYIgwTsqpggd4t07HpQ_X629Nk */

            public final void run() {
                HwCameraAutoImpl.this.lambda$dismissCoverDialog$7$HwCameraAutoImpl();
            }
        });
    }

    public /* synthetic */ void lambda$dismissCoverDialog$7$HwCameraAutoImpl() {
        Dialog dialog = this.coverDialog;
        if (dialog != null && dialog.isShowing()) {
            Log.i(TAG, "dismiss coverDialog");
            this.coverDialog.dismiss();
            unregisterBroadcast();
        }
    }

    private void sendDeviceStatusBroadcast(String flag) {
        Intent intent = new Intent();
        intent.setAction(flag);
        intent.setPackage(HW_CAMERA_NAME);
        this.cameraContext.sendBroadcastAsUser(intent, UserHandleEx.ALL, AUTO_CAMERA_STATUS_PERMISSION);
    }

    private void showDialogToastOpenFail() {
        if (this.isFirstCoverPopup) {
            showCoverDialog();
        } else {
            showToast(this.cameraContext.getResources().getString(33685564));
        }
    }

    /* access modifiers changed from: private */
    public void handleMotionResult(float[] deviceValues) {
        int result = (int) deviceValues[0];
        Log.i(TAG, "Motion Result = " + result);
        switch (result) {
            case 101:
                sendDeviceStatusBroadcast(ACTION_CAMERA_RISE);
                return;
            case 102:
                showDialogToastOpenFail();
                break;
            case RES_CLOSE_SUCCESS /*{ENCODED_INT: 103}*/:
                unregisterBroadcast();
                break;
            case RES_CLOSE_FAIL /*{ENCODED_INT: 104}*/:
                showToast(this.cameraContext.getResources().getString(33685562));
                break;
            case RES_EVENT_PRESS /*{ENCODED_INT: 107}*/:
                playRing(this.cameraDecentSoundId);
                backToHome();
                break;
            case RES_REQUEST_OVER_FREQ /*{ENCODED_INT: 108}*/:
                if (deviceValues.length >= LENGTH) {
                    int timeSecond = (int) deviceValues[1];
                    showToast(this.cameraContext.getResources().getQuantityString(34406410, timeSecond, Integer.valueOf(timeSecond)));
                    break;
                }
                break;
            case RES_CLOSE_EMERGENCY /*{ENCODED_INT: 109}*/:
                playRing(this.cameraDecentSoundId);
                showDialog();
                break;
            case RES_START_OPEN /*{ENCODED_INT: 110}*/:
                playRing(this.cameraLiftSoundId);
                break;
            case RES_START_CLOSE /*{ENCODED_INT: 111}*/:
                playRing(this.cameraDecentSoundId);
                break;
        }
        sendDeviceStatusBroadcast(ACTION_CAMERA_DESCEND);
    }

    private void testHandleResult() {
        String mockValueStr = new ReadFileInfo(TEST_FILE_NAME, true).readFileToString();
        if (!mockValueStr.isEmpty()) {
            float[] deviceValuesTest = new float[LENGTH];
            try {
                String[] temps = mockValueStr.split(",");
                if (temps.length == 1) {
                    deviceValuesTest[0] = Float.parseFloat(temps[0]);
                } else if (temps.length == LENGTH) {
                    deviceValuesTest[0] = Float.parseFloat(temps[0]);
                    deviceValuesTest[1] = Float.parseFloat(temps[1]);
                } else {
                    return;
                }
                handleMotionResult(deviceValuesTest);
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException when read the mock file");
            }
        }
    }

    private void initMotionHandler() {
        synchronized (HANDLER_THREAD_SYNC_OBJ) {
            if (this.motionThread == null) {
                this.motionThread = new HandlerThread(MOTION_THREAD_NAME);
                this.motionThread.start();
                this.motionHandler = new Handler(this.motionThread.getLooper());
            }
        }
    }

    /* JADX INFO: finally extract failed */
    private String getForegroundActivityName() {
        long identityToken = Binder.clearCallingIdentity();
        try {
            ActivityInfo info = ActivityManagerEx.getLastResumedActivity();
            Binder.restoreCallingIdentity(identityToken);
            String activityName = BuildConfig.FLAVOR;
            if (info != null) {
                activityName = info.name;
            }
            hwLogD(TAG, "foreground activity name is " + activityName);
            return activityName;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    private void listenPhoneState(int state) {
        long identityToken = Binder.clearCallingIdentity();
        try {
            if (this.telephonyManager != null) {
                this.telephonyManager.listen(this.phoneStateListener, state);
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void registerActivityNotifier() {
        long identityToken = Binder.clearCallingIdentity();
        try {
            ActivityManagerEx.registerHwActivityNotifier(this.activityNotifierEx, ACTIVITY_NOTIFY_REASON);
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    /* access modifiers changed from: private */
    public void registerPopDeviceListener(int cmd, int time) {
        if (TRUE.equals(SystemPropertiesEx.get(HW_CAMERA_MOCK, FALSE))) {
            testHandleResult();
        } else if (this.hwExtDevManager == null || this.hwExtMotion == null) {
            Log.w(TAG, "hwExtDevManager or hwExtMotion is null!");
        } else {
            if (cmd == 0) {
                registerBroadcast();
            } else if (cmd == 1) {
                listenPhoneState(0);
                Log.i(TAG, "PhoneStateListener has been cancelled");
            } else {
                Log.d(TAG, "ERROR: CMD INVALID");
            }
            TraceEx.traceBegin(1024, "AutoPopupCamera");
            this.hwExtMotion.setHWExtDeviceAction(cmd);
            this.hwExtMotion.setHWExtDeviceDelay(time);
            initMotionHandler();
            boolean isRegistered = this.hwExtDevManager.registerDeviceListener(this.hwExtDevEventListener, this.hwExtMotion, this.motionHandler);
            hwLogD(TAG, "registerPopDeviceListener cmd = " + cmd + " result = " + isRegistered + " isEnterGallery = " + this.isEnterGallery + " delayTime = " + time);
            TraceEx.traceEnd(1024);
        }
    }

    /* access modifiers changed from: private */
    public String getStrategyByActivity(String activity) {
        Map<String, List<String>> map = this.strategyMaps;
        if (map == null || map.size() == 0) {
            Log.w(TAG, "getStrategyByActivity failed, strategyMaps is null or size is 0");
            return CONSTANT_DEFAULT_STRING;
        }
        for (String strategy : this.strategyMaps.keySet()) {
            List<String> whiteList = this.strategyMaps.get(strategy);
            if (whiteList != null && whiteList.contains(activity)) {
                return strategy;
            }
        }
        return CONSTANT_DEFAULT_STRING;
    }

    private int getDelayTimeByActivity(String activity) {
        Map<String, Integer> map = this.delayTimeMaps;
        if (map == null || map.size() == 0) {
            Log.w(TAG, "getDelayTimeByActivity failed, delayTimeMaps is null or size is 0");
            return DELAY_TIME_DEFAULT;
        }
        int delayTime = this.delayTimeMaps.get(activity).intValue();
        if (delayTime < 0 || delayTime > DELAY_TIME_MAX) {
            return DELAY_TIME_DEFAULT;
        }
        return delayTime;
    }

    private void preHandleCamera(int newCameraState, int facing, String activity, String clientName) {
        if (facing != 1) {
            return;
        }
        if (newCameraState == 0) {
            this.isFrontCameraOpen = true;
            this.frontCameraActivity = activity;
            if (clientName != null) {
                this.currentFrontCameraClient = clientName;
            }
        } else if (newCameraState == 3) {
            this.isFrontCameraOpen = false;
            this.currentFrontCameraClient = CONSTANT_DEFAULT_STRING;
            dismissSlidedownTip();
            dismissCoverDialog();
            this.endTime = System.currentTimeMillis();
        } else if (newCameraState == 101) {
            this.isSpecialScene = true;
            this.startTime = System.currentTimeMillis();
            if (IS_HW_DEBUG) {
                Log.d(TAG, "Receive popup CAMERA_STATE_OPEN_SUCCESS!");
            }
        } else if (newCameraState == 102) {
            if (IS_HW_DEBUG) {
                Log.d(TAG, "Receive popup CAMERA_STATE_CONFIG_STREAM");
            }
            this.isSpecialScene = false;
        }
    }

    private void runPopupCmd(int newCameraState, int facing, int delayTime, String activity) {
        if (facing == 1) {
            if (newCameraState == 0) {
                int time = delayTime;
                if (this.specialActivity.equals(activity)) {
                    time = 0;
                    Log.i(TAG, "no delay for specail scene");
                } else {
                    this.specialActivity = CONSTANT_DEFAULT_STRING;
                }
                registerPopDeviceListener(0, time);
            } else if (newCameraState == 3) {
                if (this.isSpecialScene) {
                    long j = this.startTime;
                    if (j > 0) {
                        long j2 = this.endTime;
                        if (j2 > 0 && j2 - j > 100) {
                            this.specialActivity = activity;
                        }
                    }
                }
                registerPopDeviceListener(1, this.isEnterGallery ? DELAY_TIME_MAX : delayTime);
                this.startTime = 0;
                this.endTime = 0;
                this.isSpecialScene = false;
                this.isEnterGallery = false;
                try {
                    this.audioManagerEx.unregisterAudioModeCallback(this.audioCallback);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unregister AudioModeCallback fail");
                }
            }
        }
    }

    private void dealStrategy(int newCameraState, int facing, String clientName, String strategy, String activity) {
        AudioManager audioManager2;
        int i = 0;
        int delayTime = HW_CAMERA_NAME.equals(clientName) ? 0 : DELAY_TIME_DEFAULT;
        boolean isRunPopupCmd = true;
        char c = 65535;
        switch (strategy.hashCode()) {
            case 49:
                if (strategy.equals(STRATEGY_ACTIVITY_AND_ORIGIN)) {
                    c = 0;
                    break;
                }
                break;
            case 50:
                if (strategy.equals(STRATEGY_ACTIVITY_AND_NO_CLOSE)) {
                    c = 1;
                    break;
                }
                break;
            case 51:
                if (strategy.equals(STRATEGY_VIDEO_CALL)) {
                    c = LENGTH;
                    break;
                }
                break;
            case 52:
                if (strategy.equals(STRATEGY_VIDEO_PHONE)) {
                    c = 3;
                    break;
                }
                break;
            case 53:
                if (strategy.equals(STRATEGY_DELAY_CLOSE)) {
                    c = 4;
                    break;
                }
                break;
        }
        if (c != 0) {
            if (c == 1) {
                isRunPopupCmd = this.isFrontCameraOpen;
                if (newCameraState == 0 && facing != 1) {
                    registerPopDeviceListener(1, DELAY_TIME_DEFAULT);
                    isRunPopupCmd = false;
                }
            } else if (c != LENGTH) {
                if (c == 3) {
                    if (this.isFrontCameraOpen) {
                        i = 32;
                    }
                    listenPhoneState(i);
                } else if (c == 4) {
                    delayTime = this.isFrontCameraOpen ? delayTime : getDelayTimeByActivity(activity);
                }
            } else if (this.isFrontCameraOpen && (audioManager2 = this.audioManager) != null) {
                if (audioManager2.getMode() == 3) {
                    Log.i(TAG, "CurrentMode is already in communication");
                } else {
                    this.audioManagerEx.registerAudioModeCallback(this.audioCallback, (Handler) null);
                    isRunPopupCmd = false;
                }
            }
        }
        if (isRunPopupCmd) {
            runPopupCmd(newCameraState, facing, delayTime, activity);
        }
    }

    public void handleCameraState(int newCameraState, int facing, String clientName) {
        if (facing == 1 && newCameraState == 100) {
            this.isEnterGallery = true;
        } else if (newCameraState == 0 || newCameraState == 3 || newCameraState == 101 || newCameraState == 102) {
            hwLogD(TAG, "clientName is " + clientName + " camerastate is " + newCameraState + " facing is " + facing);
            String currentActivity = getForegroundActivityName();
            String strategy = getStrategyByActivity(currentActivity);
            StringBuilder sb = new StringBuilder();
            sb.append("strategy is ");
            sb.append(strategy);
            hwLogD(TAG, sb.toString());
            if (!STRATEGY_NO_HANDLE.equals(strategy)) {
                preHandleCamera(newCameraState, facing, currentActivity, clientName);
                dealStrategy(newCameraState, facing, clientName, strategy, currentActivity);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void playRing(int soundId) {
        this.hwCameraAutoImplHandler.post(new Runnable(soundId) {
            /* class com.huawei.server.camera.$$Lambda$HwCameraAutoImpl$Y_mrqIgQNl4oPAjInnD_BkY9DkE */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwCameraAutoImpl.this.lambda$playRing$8$HwCameraAutoImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$playRing$8$HwCameraAutoImpl(int soundId) {
        int currentUserId = ActivityManagerEx.getCurrentUser();
        hwLogD(TAG, "playSlideSound currentUser = " + currentUserId + " soundId = " + soundId);
        if (SettingsEx.System.getIntForUser(this.cameraContext.getContentResolver(), CAMERA_LIFT_SOUNDS_KEY, 0, currentUserId) == 1) {
            this.mSoundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void hwLogD(String tag, String msg) {
        if (IS_HW_DEBUG) {
            Log.d(tag, msg);
        }
    }
}
