package com.huawei.android.location.activityrecognition;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.server.hidata.wavemapping.modelservice.ModelBaseService;
import com.huawei.android.location.activityrecognition.IActivityRecognitionHardwareService;
import com.huawei.android.location.activityrecognition.IActivityRecognitionHardwareSink;
import com.huawei.systemserver.activityrecognition.HwActivityChangedEvent;
import com.huawei.systemserver.activityrecognition.HwActivityChangedExtendEvent;
import com.huawei.systemserver.activityrecognition.HwActivityRecognitionEvent;
import com.huawei.systemserver.activityrecognition.HwActivityRecognitionExtendEvent;
import com.huawei.systemserver.activityrecognition.HwEnvironmentChangedEvent;
import com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService;
import com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink;
import com.huawei.systemserver.activityrecognition.OtherParameters;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class HwActivityRecognition {
    public static final String ACTIVITY_AR_K_N_ACT = "android.activity_recognition.ar_k_n_act";
    public static final String ACTIVITY_CLIMBING_MOUNT = "android.activity_recognitio.climbing_mount";
    public static final String ACTIVITY_DROP = "android.activity_recognition.drop";
    public static final String ACTIVITY_ELEVATOR = "android.activity_recognition.elevator";
    public static final String ACTIVITY_FAST_WALKING = "android.activity_recognition.fast_walking";
    public static final String ACTIVITY_IN_VEHICLE = "android.activity_recognition.in_vehicle";
    public static final String ACTIVITY_ON_BICYCLE = "android.activity_recognition.on_bicycle";
    public static final String ACTIVITY_ON_FOOT = "android.activity_recognition.on_foot";
    public static final String ACTIVITY_OUTDOOR = "android.activity_recognition.outdoor";
    public static final String ACTIVITY_RELATIVE_STILL = "android.activity_recognition.relative_still";
    public static final String ACTIVITY_RUNNING = "android.activity_recognition.running";
    public static final String ACTIVITY_RUN_FOR_HEALTH = "android.activity_recognition.run_for_health";
    public static final String ACTIVITY_STILL = "android.activity_recognition.still";
    public static final String ACTIVITY_STOP_VEHICLE = "android.activity_recognition.stop_vehicle";
    public static final String ACTIVITY_TILT = "android.activity_recognition.tilting";
    public static final String ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL = "android.activity_recognition.high_speed_rail";
    public static final String ACTIVITY_UNKNOWN = "android.activity_recognition.unknown";
    public static final String ACTIVITY_VE_AUTO = "android.activity_recognitio.auto";
    public static final String ACTIVITY_VE_BUS = "android.activity_recognition.bus";
    public static final String ACTIVITY_VE_CAR = "android.activity_recognition.car";
    public static final String ACTIVITY_VE_METRO = "android.activity_recognition.metro";
    public static final String ACTIVITY_VE_RAIL = "android.activity_recognitio.rail";
    public static final String ACTIVITY_VE_TRAIN = "android.activity_recognition.train";
    public static final String ACTIVITY_VE_UNKNOWN = "android.activity_recognition.ve_unknown";
    public static final String ACTIVITY_WALKING = "android.activity_recognition.walking";
    public static final String ACTIVITY_WALK_FOR_HEALTH = "android.activity_recognition.walk_for_health";
    private static final String AIDL_MESSAGE_SERVICE_CLASS = "com.huawei.android.location.activityrecognition.ActivityRecognitionService";
    private static final String AIDL_MESSAGE_SERVICE_CLASS_O = "com.huawei.systemserver.activityrecognition.ActivityRecognitionService";
    private static final String AIDL_MESSAGE_SERVICE_PACKAGE = "com.huawei.android.location.activityrecognition";
    private static final String AIDL_MESSAGE_SERVICE_PACKAGE_O = "com.huawei.systemserver";
    private static final int ANDROID_O = 25;
    private static final int AR_SDK_VERSION = 1;
    public static final String ENV_TYPE_HOME = "android.activity_recognition.env_home";
    public static final String ENV_TYPE_OFFICE = "android.activity_recognition.env_office";
    public static final String ENV_TYPE_WAY_HOME = "android.activity_recognition.env_way_home";
    public static final String ENV_TYPE_WAY_OFFICE = "android.activity_recognition.env_way_office";
    public static final int EVENT_TYPE_ENTER = 1;
    public static final int EVENT_TYPE_EXIT = 2;
    private static final int MSG_BIND = 0;
    private static final int MSG_RECONNECTION = 1;
    private static final String TAG = "ARMoudle.HwActivityRecognition";
    private static int mARServiceVersion = -1;
    /* access modifiers changed from: private */
    public static final int sdkVersion = Build.VERSION.SDK_INT;
    /* access modifiers changed from: private */
    public ServiceDeathHandler deathHandler;
    private int mConnectCount = 0;
    private ServiceConnection mConnection = new ServiceConnection() {
        /* class com.huawei.android.location.activityrecognition.HwActivityRecognition.AnonymousClass2 */

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            SDKLog.d(HwActivityRecognition.TAG, "Connection service ok");
            HwActivityRecognition.this.mHandler.removeMessages(1);
            if (HwActivityRecognition.sdkVersion >= 25) {
                HwActivityRecognition.this.mService_O = IActivityRecognitionHardwareService.Stub.asInterface(service);
                HwActivityRecognition.this.getARVersion();
            } else {
                HwActivityRecognition.this.mService = IActivityRecognitionHardwareService.Stub.asInterface(service);
            }
            boolean unused = HwActivityRecognition.this.registerSink();
            HwActivityRecognition.this.notifyServiceDied();
            if (HwActivityRecognition.sdkVersion >= 25) {
                HwActivityRecognition.this.mServiceConnection.onServiceConnected();
            } else {
                HwActivityRecognition.this.mHandler.sendEmptyMessage(0);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            if (HwActivityRecognition.sdkVersion >= 25) {
                HwActivityRecognition.this.mService_O = null;
            } else {
                HwActivityRecognition.this.mService = null;
            }
            HwActivityRecognition.this.mServiceConnection.onServiceDisconnected();
        }
    };
    private Context mContext = null;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        /* class com.huawei.android.location.activityrecognition.HwActivityRecognition.AnonymousClass1 */

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                HwActivityRecognition.this.handleProviderLoad();
            } else if (i == 1) {
                HwActivityRecognition.this.bindService();
            }
        }
    };
    /* access modifiers changed from: private */
    public IActivityRecognitionHardwareService mService = null;
    /* access modifiers changed from: private */
    public HwActivityRecognitionServiceConnection mServiceConnection = null;
    /* access modifiers changed from: private */
    public com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService mService_O = null;
    private IActivityRecognitionHardwareSink mSink;
    private IActivityRecognitionHardwareSink mSink_O;
    private String packageName;

    public HwActivityRecognition(Context context) {
        SDKLog.d(TAG, "AR sdk version:1");
        SDKLog.d(TAG, "HwActivityRecognition, android version:" + sdkVersion);
        if (context != null) {
            this.mContext = context;
            this.packageName = context.getPackageName();
            this.deathHandler = new ServiceDeathHandler(this, null);
        }
    }

    public HwActivityRecognition(Context context, Object object) {
        SDKLog.d(TAG, "AR sdk version:1");
        SDKLog.d(TAG, "HwActivityRecognition, android version:" + sdkVersion);
        if (context != null) {
            this.mContext = context;
            if (object != null) {
                this.packageName = object.getClass().getName();
            } else {
                this.packageName = context.getClass().getName();
            }
            this.deathHandler = new ServiceDeathHandler(this, null);
        }
    }

    public static int getARServiceVersion() {
        return mARServiceVersion;
    }

    public static void setARServiceVersion(int aRServiceVersion) {
        mARServiceVersion = aRServiceVersion;
    }

    private boolean isSystemUser() {
        try {
            int userId = ((Integer) Class.forName("android.os.UserHandle").getMethod("myUserId", new Class[0]).invoke(null, new Object[0])).intValue();
            SDKLog.d(TAG, "user id:" + userId);
            if (userId == 0) {
                return true;
            }
            return false;
        } catch (ClassNotFoundException e) {
            SDKLog.e(TAG, "ClassNotFoundException");
            return false;
        } catch (NoSuchMethodException e2) {
            SDKLog.e(TAG, "NoSuchMethodException");
            return false;
        } catch (IllegalAccessException e3) {
            SDKLog.e(TAG, "IllegalAccessException");
            return false;
        } catch (IllegalArgumentException e4) {
            SDKLog.e(TAG, "IllegalArgumentException");
            return false;
        } catch (InvocationTargetException e5) {
            SDKLog.e(TAG, "InvocationTargetException");
            return false;
        }
    }

    public boolean connectService(HwActivityRecognitionHardwareSink sink, HwActivityRecognitionServiceConnection connection) {
        SDKLog.d(TAG, "connectService");
        if (!isSystemUser()) {
            SDKLog.e(TAG, "not system user.");
            return false;
        } else if (connection == null || sink == null) {
            SDKLog.e(TAG, "connection or sink is null.");
            return false;
        } else {
            this.mServiceConnection = connection;
            if (sdkVersion >= 25) {
                if (this.mService_O != null) {
                    return true;
                }
                this.mSink_O = createActivityRecognitionHardwareSink_O(sink);
                bindService();
                return true;
            } else if (this.mService != null) {
                return true;
            } else {
                this.mSink = createActivityRecognitionHardwareSink(sink);
                bindService();
                return true;
            }
        }
    }

    public boolean disconnectService() {
        SDKLog.d(TAG, "disconnectService");
        if (sdkVersion >= 25) {
            com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
            if (iActivityRecognitionHardwareService == null) {
                SDKLog.e(TAG, "mService_O is null.");
                return false;
            }
            iActivityRecognitionHardwareService.asBinder().unlinkToDeath(this.deathHandler, 0);
        } else {
            IActivityRecognitionHardwareService iActivityRecognitionHardwareService2 = this.mService;
            if (iActivityRecognitionHardwareService2 == null) {
                SDKLog.e(TAG, "mService is null.");
                return false;
            }
            iActivityRecognitionHardwareService2.asBinder().unlinkToDeath(this.deathHandler, 0);
        }
        unregisterSink();
        this.mContext.unbindService(this.mConnection);
        this.mServiceConnection.onServiceDisconnected();
        if (sdkVersion >= 25) {
            this.mService_O = null;
        } else {
            this.mService = null;
        }
        this.mConnectCount = 0;
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(0);
        return true;
    }

    /* access modifiers changed from: private */
    public boolean registerSink() {
        if (sdkVersion >= 25) {
            return registerSink_O();
        }
        return registerSink_N();
    }

    private boolean registerSink_N() {
        IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink;
        SDKLog.d(TAG, "registerSink_N");
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null || (iActivityRecognitionHardwareSink = this.mSink) == null) {
            SDKLog.e(TAG, "mService or mSink is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.registerSink(iActivityRecognitionHardwareSink);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "registerSink error:" + e.getMessage());
            return false;
        }
    }

    private boolean registerSink_O() {
        IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink;
        SDKLog.d(TAG, "registerSink_O");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null || (iActivityRecognitionHardwareSink = this.mSink_O) == null) {
            SDKLog.e(TAG, "mService_O or mSink_O is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.registerSink(this.packageName, iActivityRecognitionHardwareSink);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "registerSink error:" + e.getMessage());
            return false;
        }
    }

    private boolean unregisterSink() {
        if (sdkVersion >= 25) {
            return unregisterSink_O();
        }
        return unregisterSink_N();
    }

    private boolean unregisterSink_N() {
        IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink;
        SDKLog.d(TAG, "unregisterSink_N");
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null || (iActivityRecognitionHardwareSink = this.mSink) == null) {
            SDKLog.e(TAG, "mService or mSink is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.unregisterSink(iActivityRecognitionHardwareSink);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "unregisterSink error:" + e.getMessage());
            return false;
        }
    }

    private boolean unregisterSink_O() {
        IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink;
        SDKLog.d(TAG, "unregisterSink_O");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null || (iActivityRecognitionHardwareSink = this.mSink_O) == null) {
            SDKLog.e(TAG, "mService_O or mService_O is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.unregisterSink(this.packageName, iActivityRecognitionHardwareSink);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "unregisterSink error:" + e.getMessage());
            return false;
        }
    }

    public int getSupportedModule() {
        SDKLog.d(TAG, "getSupportedModule");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService_O is null.");
            return 0;
        }
        try {
            return iActivityRecognitionHardwareService.getSupportedModule();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getSupportedModule error:" + e.getMessage());
            return 0;
        }
    }

    public String[] getSupportedActivities() {
        if (sdkVersion >= 25) {
            return getSupportedActivities_O();
        }
        return getSupportedActivities_N();
    }

    private String[] getSupportedActivities_N() {
        SDKLog.d(TAG, "getSupportedActivities_N");
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return new String[0];
        }
        try {
            return iActivityRecognitionHardwareService.getSupportedActivities();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getSupportedActivities error:" + e.getMessage());
            return new String[0];
        }
    }

    private String[] getSupportedActivities_O() {
        SDKLog.d(TAG, "getSupportedActivities_O");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService_O is null.");
            return new String[0];
        }
        try {
            return iActivityRecognitionHardwareService.getSupportedActivities();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getSupportedActivities error:" + e.getMessage());
            return new String[0];
        }
    }

    public String[] getSupportedEnvironments() {
        SDKLog.d(TAG, "getSupportedEnvironments");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService_O is null.");
            return new String[0];
        }
        try {
            return iActivityRecognitionHardwareService.getSupportedEnvironments();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getSupportedEnvironments error:" + e.getMessage());
            return new String[0];
        }
    }

    public boolean enableActivityEvent(String activity, int eventType, long reportLatencyNs) {
        if (sdkVersion >= 25) {
            return enableActivityEvent_O(activity, eventType, reportLatencyNs);
        }
        return enableActivityEvent_N(activity, eventType, reportLatencyNs);
    }

    private boolean enableActivityEvent_N(String activity, int eventType, long reportLatencyNs) {
        SDKLog.d(TAG, "enableActivityEvent");
        if (TextUtils.isEmpty(activity) || reportLatencyNs < 0) {
            SDKLog.e(TAG, "activity is null or reportLatencyNs < 0");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(activity) + "," + eventType + "," + reportLatencyNs);
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.enableActivityEvent(activity, eventType, reportLatencyNs);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "enableActivityEvent error:" + e.getMessage());
            return false;
        }
    }

    private boolean enableActivityEvent_O(String activity, int eventType, long reportLatencyNs) {
        SDKLog.d(TAG, "enableActivityEvent");
        if (TextUtils.isEmpty(activity) || reportLatencyNs < 0) {
            SDKLog.e(TAG, "activity is null or reportLatencyNs < 0");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(activity) + "," + eventType + "," + reportLatencyNs);
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.enableActivityEvent(this.packageName, activity, eventType, reportLatencyNs);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "enableActivityEvent error:" + e.getMessage());
            return false;
        }
    }

    public boolean enableActivityEvent(String activity, int eventType, long reportLatencyNs, OtherParameters params) {
        SDKLog.d(TAG, "enableActivityExtendEvent");
        if (TextUtils.isEmpty(activity) || reportLatencyNs < 0 || params == null) {
            SDKLog.e(TAG, "activity is null or reportLatencyNs < 0 or params is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(activity) + "," + eventType + "," + reportLatencyNs + "," + params.toString());
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        boolean result = false;
        try {
            result = iActivityRecognitionHardwareService.enableActivityExtendEvent(this.packageName, activity, eventType, reportLatencyNs, tranferToOtherParameters_O(params));
        } catch (RemoteException e) {
            SDKLog.e(TAG, "enableActivityextendEvent error:" + e.getMessage());
        }
        SDKLog.d(TAG, "activityExtendEventEnable:" + result);
        return result;
    }

    private OtherParameters tranferToOtherParameters_O(OtherParameters params) {
        if (params == null) {
            return null;
        }
        return new OtherParameters(params.getmParam1(), params.getmParam2(), params.getmParam3(), params.getmParam4(), params.getmParam5());
    }

    public boolean disableActivityEvent(String activity, int eventType) {
        if (sdkVersion >= 25) {
            return disableActivityEvent_O(activity, eventType);
        }
        return disableActivityEvent_N(activity, eventType);
    }

    private boolean disableActivityEvent_N(String activity, int eventType) {
        SDKLog.d(TAG, "disableActivityEvent");
        if (TextUtils.isEmpty(activity)) {
            SDKLog.e(TAG, "activity is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(activity) + "," + eventType);
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.disableActivityEvent(activity, eventType);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "disableActivityEvent error:" + e.getMessage());
            return false;
        }
    }

    private boolean disableActivityEvent_O(String activity, int eventType) {
        SDKLog.d(TAG, "disableActivityEvent");
        if (TextUtils.isEmpty(activity)) {
            SDKLog.e(TAG, "activity is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(activity) + "," + eventType);
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService_O is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.disableActivityEvent(this.packageName, activity, eventType);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "disableActivityEvent error:" + e.getMessage());
            return false;
        }
    }

    public String getCurrentActivity() {
        SDKLog.d(TAG, "getCurrentActivity");
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return ModelBaseService.UNKONW_IDENTIFY_RET;
        }
        try {
            return iActivityRecognitionHardwareService.getCurrentActivity();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getCurrentActivity error:" + e.getMessage());
            return ModelBaseService.UNKONW_IDENTIFY_RET;
        }
    }

    public HwActivityChangedExtendEvent getCurrentActivityExtend() {
        SDKLog.d(TAG, "getCurrentActivityExtend");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return null;
        }
        HwActivityChangedExtendEvent hwActivityEvent = null;
        try {
            if (mARServiceVersion == 1) {
                hwActivityEvent = iActivityRecognitionHardwareService.getCurrentActivityV1_1();
            } else {
                hwActivityEvent = iActivityRecognitionHardwareService.getCurrentActivity();
            }
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getCurrentActivity error:" + e.getMessage());
        }
        SDKLog.d(TAG, "hwActivityEvent:" + hwActivityEvent);
        return tranferToHwActivityChangedExtendEvent(hwActivityEvent);
    }

    public boolean initEnvironmentFunction(String environment, OtherParameters params) {
        SDKLog.d(TAG, "initEnvironmentFunction");
        if (TextUtils.isEmpty(environment) || params == null) {
            SDKLog.e(TAG, "environment or params is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(environment) + "," + params.toString());
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        boolean result = false;
        try {
            result = iActivityRecognitionHardwareService.initEnvironmentFunction(this.packageName, environment, tranferToOtherParameters_O(params));
        } catch (RemoteException e) {
            SDKLog.e(TAG, "initEnvironmentFunction error:" + e.getMessage());
        }
        SDKLog.d(TAG, "environmentInit:" + result);
        return result;
    }

    public boolean exitEnvironmentFunction(String environment, OtherParameters params) {
        SDKLog.d(TAG, "exitEnvironmentFunction");
        if (TextUtils.isEmpty(environment) || params == null) {
            SDKLog.e(TAG, "environment or params is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(environment) + "," + params.toString());
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        boolean result = false;
        try {
            result = iActivityRecognitionHardwareService.exitEnvironmentFunction(this.packageName, environment, tranferToOtherParameters_O(params));
        } catch (RemoteException e) {
            SDKLog.e(TAG, "exitEnvironmentFunction error:" + e.getMessage());
        }
        SDKLog.d(TAG, "environmentExit:" + result);
        return result;
    }

    public boolean enableEnvironmentEvent(String environment, int eventType, long reportLatencyNs, OtherParameters params) {
        SDKLog.d(TAG, "enableEnvironmentEvent");
        if (TextUtils.isEmpty(environment) || reportLatencyNs < 0 || params == null) {
            SDKLog.e(TAG, "environment is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(environment) + "," + eventType + "," + reportLatencyNs + "," + params.toString());
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        boolean result = false;
        try {
            result = iActivityRecognitionHardwareService.enableEnvironmentEvent(this.packageName, environment, eventType, reportLatencyNs, tranferToOtherParameters_O(params));
        } catch (RemoteException e) {
            SDKLog.e(TAG, "enableEnvironmentEvent error:" + e.getMessage());
        }
        SDKLog.d(TAG, "environmentEnable:" + result);
        return result;
    }

    public boolean disableEnvironmentEvent(String environment, int eventType) {
        SDKLog.d(TAG, "disableEnvironmentEvent");
        if (TextUtils.isEmpty(environment)) {
            SDKLog.e(TAG, "environment is null.");
            return false;
        }
        SDKLog.d(TAG, String.valueOf(environment) + "," + eventType);
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        boolean result = false;
        try {
            result = iActivityRecognitionHardwareService.disableEnvironmentEvent(this.packageName, environment, eventType);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "disableEnvironmentEvent error:" + e.getMessage());
        }
        SDKLog.d(TAG, "environmentDisable:" + result);
        return result;
    }

    public HwEnvironmentChangedEvent getCurrentEnvironment() {
        SDKLog.d(TAG, "getCurrentEnvironment");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return null;
        }
        HwEnvironmentChangedEvent hwEnvironmentEvent = null;
        try {
            if (mARServiceVersion == 1) {
                hwEnvironmentEvent = iActivityRecognitionHardwareService.getCurrentEnvironmentV1_1();
            } else {
                hwEnvironmentEvent = iActivityRecognitionHardwareService.getCurrentEnvironment();
            }
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getCurrentEnvironment error:" + e.getMessage());
        }
        SDKLog.d(TAG, "hwEnvironmentEvent:" + hwEnvironmentEvent);
        return tranferToHwEnvironmentChangedEvent(hwEnvironmentEvent);
    }

    public boolean flush() {
        if (sdkVersion >= 25) {
            return flush_O();
        }
        return flush_N();
    }

    private boolean flush_N() {
        SDKLog.d(TAG, "flush");
        IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.flush();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "flush error:" + e.getMessage());
            return false;
        }
    }

    private boolean flush_O() {
        SDKLog.d(TAG, "flush");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return false;
        }
        try {
            return iActivityRecognitionHardwareService.flush();
        } catch (RemoteException e) {
            SDKLog.e(TAG, "flush error:" + e.getMessage());
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void getARVersion() {
        int version = -1;
        SDKLog.d(TAG, "getARVersion");
        com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService iActivityRecognitionHardwareService = this.mService_O;
        if (iActivityRecognitionHardwareService == null) {
            SDKLog.e(TAG, "mService is null.");
            return;
        }
        try {
            version = iActivityRecognitionHardwareService.getARVersion(this.packageName, 1);
            SDKLog.d(TAG, "version:" + version);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "getARVersion error:" + e.getMessage());
        }
        setARServiceVersion(version);
    }

    private IActivityRecognitionHardwareSink createActivityRecognitionHardwareSink(final HwActivityRecognitionHardwareSink sink) {
        if (sink == null) {
            return null;
        }
        return new IActivityRecognitionHardwareSink.Stub() {
            /* class com.huawei.android.location.activityrecognition.HwActivityRecognition.AnonymousClass3 */

            @Override // com.huawei.android.location.activityrecognition.IActivityRecognitionHardwareSink
            public void onActivityChanged(HwActivityChangedEvent event) throws RemoteException {
                sink.onActivityChanged(event);
            }
        };
    }

    static class ActivityEvent {
        private String activity;
        private int confidence;
        private int eventType;
        private OtherParameters otherParams;
        private long timestampNs;

        public int getConfidence() {
            return this.confidence;
        }

        public String getActivity() {
            return this.activity;
        }

        public int getEventType() {
            return this.eventType;
        }

        public long getTimestampNs() {
            return this.timestampNs;
        }

        public OtherParameters getOtherParams() {
            return this.otherParams;
        }

        public ActivityEvent(String activity2, int eventType2, long timestampNs2, OtherParameters otherParams2, int confidence2) {
            this.activity = activity2;
            this.eventType = eventType2;
            this.timestampNs = timestampNs2;
            this.otherParams = otherParams2;
            this.confidence = confidence2;
        }
    }

    /* access modifiers changed from: private */
    public HwActivityChangedEvent tranferToHwActivityChangedEvent(HwActivityChangedEvent event) {
        if (event == null) {
            return null;
        }
        List<ActivityEvent> events = new ArrayList<>();
        for (HwActivityRecognitionEvent e : event.getActivityRecognitionEvents()) {
            events.add(new ActivityEvent(e.getActivity(), e.getEventType(), e.getTimestampNs(), null, e.getConfidence()));
        }
        HwActivityRecognitionEvent[] activityRecognitionEventArray = new HwActivityRecognitionEvent[events.size()];
        for (int j = 0; j < events.size(); j++) {
            ActivityEvent arEvent = events.get(j);
            activityRecognitionEventArray[j] = new HwActivityRecognitionEvent(arEvent.getActivity(), arEvent.getEventType(), arEvent.getTimestampNs(), arEvent.getConfidence());
        }
        return new HwActivityChangedEvent(activityRecognitionEventArray);
    }

    /* access modifiers changed from: private */
    public HwActivityChangedExtendEvent tranferToHwActivityChangedExtendEvent(HwActivityChangedExtendEvent event) {
        if (event == null) {
            return null;
        }
        List<ActivityEvent> events = new ArrayList<>();
        for (HwActivityRecognitionExtendEvent e : event.getActivityRecognitionExtendEvents()) {
            events.add(new ActivityEvent(e.getActivity(), e.getEventType(), e.getTimestampNs(), tranferToOtherParameters_N(e.getOtherParams()), e.getConfidence()));
        }
        HwActivityRecognitionExtendEvent[] activityRecognitionEventArray = new HwActivityRecognitionExtendEvent[events.size()];
        for (int j = 0; j < events.size(); j++) {
            ActivityEvent arEvent = events.get(j);
            activityRecognitionEventArray[j] = new HwActivityRecognitionExtendEvent(arEvent.getActivity(), arEvent.getEventType(), arEvent.getTimestampNs(), arEvent.getOtherParams(), arEvent.getConfidence());
        }
        return new HwActivityChangedExtendEvent(activityRecognitionEventArray);
    }

    /* access modifiers changed from: private */
    public HwEnvironmentChangedEvent tranferToHwEnvironmentChangedEvent(HwEnvironmentChangedEvent event) {
        if (event == null) {
            return null;
        }
        List<ActivityEvent> events = new ArrayList<>();
        for (HwActivityRecognitionExtendEvent e : event.getEnvironmentRecognitionEvents()) {
            events.add(new ActivityEvent(e.getActivity(), e.getEventType(), e.getTimestampNs(), tranferToOtherParameters_N(e.getOtherParams()), e.getConfidence()));
        }
        HwActivityRecognitionExtendEvent[] activityRecognitionEventArray = new HwActivityRecognitionExtendEvent[events.size()];
        for (int j = 0; j < events.size(); j++) {
            ActivityEvent arEvent = events.get(j);
            activityRecognitionEventArray[j] = new HwActivityRecognitionExtendEvent(arEvent.getActivity(), arEvent.getEventType(), arEvent.getTimestampNs(), arEvent.getOtherParams(), arEvent.getConfidence());
        }
        return new HwEnvironmentChangedEvent(activityRecognitionEventArray);
    }

    private OtherParameters tranferToOtherParameters_N(OtherParameters otherParams) {
        if (otherParams == null) {
            return null;
        }
        return new OtherParameters(otherParams.getmParam1(), otherParams.getmParam2(), otherParams.getmParam3(), otherParams.getmParam4(), otherParams.getmParam5());
    }

    private com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink createActivityRecognitionHardwareSink_O(final HwActivityRecognitionHardwareSink sink) {
        if (sink == null) {
            return null;
        }
        return new IActivityRecognitionHardwareSink.Stub() {
            /* class com.huawei.android.location.activityrecognition.HwActivityRecognition.AnonymousClass4 */

            @Override // com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink
            public void onActivityChanged(HwActivityChangedEvent event) throws RemoteException {
                sink.onActivityChanged(HwActivityRecognition.this.tranferToHwActivityChangedEvent(event));
            }

            @Override // com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink
            public void onActivityExtendChanged(HwActivityChangedExtendEvent event) throws RemoteException {
                sink.onActivityExtendChanged(HwActivityRecognition.this.tranferToHwActivityChangedExtendEvent(event));
            }

            @Override // com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink
            public void onEnvironmentChanged(HwEnvironmentChangedEvent event) throws RemoteException {
                sink.onEnvironmentChanged(HwActivityRecognition.this.tranferToHwEnvironmentChangedEvent(event));
            }
        };
    }

    /* access modifiers changed from: private */
    public void handleProviderLoad() {
        try {
            if (this.mService == null) {
                return;
            }
            if (this.mService.providerLoadOk()) {
                this.mHandler.removeMessages(0);
                this.mServiceConnection.onServiceConnected();
                return;
            }
            this.mHandler.sendEmptyMessageDelayed(0, 500);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "providerLoadOk fail");
        }
    }

    /* access modifiers changed from: private */
    public void bindService() {
        if (this.mConnectCount > 10) {
            SDKLog.d(TAG, "try connect 10 times, connection fail");
        } else if (sdkVersion >= 25) {
            if (this.mService_O == null) {
                SDKLog.d(TAG, String.valueOf(this.mContext.getPackageName()) + " bind ar service.");
                Intent bindIntent = new Intent();
                bindIntent.setClassName(AIDL_MESSAGE_SERVICE_PACKAGE_O, AIDL_MESSAGE_SERVICE_CLASS_O);
                this.mContext.bindService(bindIntent, this.mConnection, 1);
                this.mConnectCount++;
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
        } else if (this.mService == null) {
            SDKLog.d(TAG, String.valueOf(this.mContext.getPackageName()) + " bind ar service.");
            Intent bindIntent2 = new Intent();
            bindIntent2.setClassName(AIDL_MESSAGE_SERVICE_PACKAGE, AIDL_MESSAGE_SERVICE_CLASS);
            this.mContext.bindService(bindIntent2, this.mConnection, 1);
            this.mConnectCount++;
            this.mHandler.sendEmptyMessageDelayed(1, 2000);
        }
    }

    /* access modifiers changed from: private */
    public void notifyServiceDied() {
        try {
            if (sdkVersion >= 25) {
                if (this.mService_O != null) {
                    this.mService_O.asBinder().linkToDeath(this.deathHandler, 0);
                }
            } else if (this.mService != null) {
                this.mService.asBinder().linkToDeath(this.deathHandler, 0);
            }
        } catch (RemoteException e) {
            SDKLog.e(TAG, "IBinder register linkToDeath function fail.");
        }
    }

    /* access modifiers changed from: private */
    public class ServiceDeathHandler implements IBinder.DeathRecipient {
        private ServiceDeathHandler() {
        }

        /* synthetic */ ServiceDeathHandler(HwActivityRecognition hwActivityRecognition, ServiceDeathHandler serviceDeathHandler) {
            this();
        }

        public void binderDied() {
            SDKLog.d(HwActivityRecognition.TAG, "Ar service has died!");
            if (HwActivityRecognition.this.mServiceConnection != null) {
                HwActivityRecognition.this.mServiceConnection.onServiceDisconnected();
            }
            if (HwActivityRecognition.sdkVersion >= 25) {
                if (HwActivityRecognition.this.mService_O != null) {
                    HwActivityRecognition.this.mService_O.asBinder().unlinkToDeath(HwActivityRecognition.this.deathHandler, 0);
                    HwActivityRecognition.this.mService_O = null;
                }
            } else if (HwActivityRecognition.this.mService != null) {
                HwActivityRecognition.this.mService.asBinder().unlinkToDeath(HwActivityRecognition.this.deathHandler, 0);
                HwActivityRecognition.this.mService = null;
            }
        }
    }
}
