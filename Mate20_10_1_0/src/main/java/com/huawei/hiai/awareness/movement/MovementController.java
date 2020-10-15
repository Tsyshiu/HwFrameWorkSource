package com.huawei.hiai.awareness.movement;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hiai.awareness.awareness.Event;
import com.huawei.hiai.awareness.common.Utils;
import com.huawei.hiai.awareness.common.log.LogUtil;
import com.huawei.hiai.awareness.service.AwarenessFence;
import com.huawei.hiai.awareness.service.ConnectServiceManager;
import com.huawei.hiai.awareness.service.ExtendAwarenessFence;
import com.huawei.hiai.awareness.service.IRequestCallBack;
import com.huawei.hiai.awareness.service.RequestResult;
import com.huawei.hiai.awareness.service.ServiceBindingManager;
import com.huawei.msdp.movement.HwMSDPMovementChangeEvent;
import com.huawei.msdp.movement.HwMSDPMovementEvent;
import com.huawei.msdp.movement.HwMSDPOtherParameters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MovementController {
    private static final String TAG = ("sdk_" + MovementController.class.getSimpleName());
    private static ConcurrentHashMap<String, Integer> sDefaultMovementActionConfigMap = new ConcurrentHashMap<>(16);
    private static ConcurrentHashMap<Integer, Integer> sDefaultMovementStatusConfigMap = new ConcurrentHashMap<>(2);
    private static MovementController sInstance;
    private ConcurrentHashMap<String, PendingIntent> mActionCallbackMap;
    private String[] mEmptyStringArray = new String[0];
    private Event mMovementStatusEvent = new Event();
    private int mMsdpSupportModule;
    private String[] mMsdpSupportedEnvironments;
    private String[] mSupportedActivities;

    public MovementController() {
        String[] strArr = this.mEmptyStringArray;
        this.mSupportedActivities = strArr;
        this.mMsdpSupportedEnvironments = strArr;
        this.mMsdpSupportModule = -1;
        this.mActionCallbackMap = new ConcurrentHashMap<>(16);
    }

    static {
        sDefaultMovementStatusConfigMap.put(1, 1);
        sDefaultMovementStatusConfigMap.put(2, 2);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.in_vehicle", 1);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.on_bicycle", 2);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.walking", 4);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.running", 8);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.still", 16);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.fast_walking", 32);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.high_speed_rail", 64);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.on_foot", 128);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.elevator", 256);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.relative_still", 512);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.env_home", 65536);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.env_office", 131072);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.env_way_home", 262144);
        sDefaultMovementActionConfigMap.put("android.activity_recognition.env_way_office", Integer.valueOf((int) AwarenessConstants.MSDP_ENVIRONMENT_TYPE_WAY_OFFICE));
    }

    public static ConcurrentHashMap<String, Integer> getDefaultMovementActionConfigMap() {
        return sDefaultMovementActionConfigMap;
    }

    public Event getMovementStatusEvent() {
        return this.mMovementStatusEvent;
    }

    public static synchronized MovementController getInstance() {
        MovementController movementController;
        synchronized (MovementController.class) {
            if (sInstance == null) {
                sInstance = new MovementController();
            }
            movementController = sInstance;
        }
        return movementController;
    }

    public boolean doSensorRegister(AwarenessFence awarenessFence, IRequestCallBack requestCallBack, PendingIntent pendingIntent) {
        LogUtil.d(TAG, "doSensorRegister");
        if (awarenessFence == null) {
            LogUtil.e(TAG, "doSensorRegister illegal!");
            return false;
        }
        initMsdpSupportParam();
        if (awarenessFence instanceof ExtendAwarenessFence) {
            ConcurrentHashMap<String, Integer> defaultMovementActionConfigMap = sDefaultMovementActionConfigMap;
            if (!isFenceParamCorrect(awarenessFence, requestCallBack, defaultMovementActionConfigMap)) {
                return false;
            }
            String topKey = awarenessFence.getTopKey();
            if (topKey != null) {
                String str = TAG;
                LogUtil.d(str, "doSensorRegister mActionCallbackMap put topKey = " + topKey);
                this.mActionCallbackMap.put(topKey, pendingIntent);
            }
            if (registerToMsdp((ExtendAwarenessFence) awarenessFence, defaultMovementActionConfigMap)) {
                ServiceBindingManager.getInstance().registerResultCallback(requestCallBack, awarenessFence, 3, 200000);
                return true;
            }
        }
        return false;
    }

    private boolean isFenceParamCorrect(AwarenessFence awarenessFence, IRequestCallBack requestCallBack, ConcurrentHashMap<String, Integer> defaultMovementActionConfigMap) {
        Map.Entry<String, Integer> entry;
        LogUtil.d(TAG, "isFenceParamCorrect");
        if (awarenessFence == null || requestCallBack == null || defaultMovementActionConfigMap == null) {
            LogUtil.e(TAG, "isFenceParamCorrect illegal!");
            return false;
        }
        boolean isRegisteredParametersSupported = false;
        int action = awarenessFence.getAction();
        Iterator<Map.Entry<String, Integer>> it = defaultMovementActionConfigMap.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            entry = it.next();
            if ((entry.getValue().intValue() & awarenessFence.getAction()) != 0) {
                isRegisteredParametersSupported = true;
                String activity = Utils.getMsdpSupportActionStr(entry.getValue().intValue(), defaultMovementActionConfigMap, entry.getValue().intValue() < 65536 ? this.mSupportedActivities : this.mMsdpSupportedEnvironments);
                if ((entry.getValue().intValue() & awarenessFence.getAction()) != entry.getValue().intValue() || activity == null) {
                    String str = TAG;
                    LogUtil.e(str, "isFenceParamCorrect unsupport register: 0x" + Integer.toHexString(entry.getValue().intValue()));
                    isRegisteredParametersSupported = false;
                    action = entry.getValue().intValue();
                } else {
                    String str2 = TAG;
                    LogUtil.d(str2, "isFenceParamCorrect support register: 0x" + Integer.toHexString(entry.getValue().intValue()));
                }
            }
        }
        String str3 = TAG;
        LogUtil.e(str3, "isFenceParamCorrect unsupport register: 0x" + Integer.toHexString(entry.getValue().intValue()));
        isRegisteredParametersSupported = false;
        action = entry.getValue().intValue();
        if (!isRegisteredParametersSupported) {
            awarenessFence.setAction(action);
            ServiceBindingManager.getInstance().registerResultCallback(requestCallBack, awarenessFence, 4, AwarenessConstants.ERROR_FUNCTION_NOT_SUPPORTED_CODE);
        }
        return isRegisteredParametersSupported;
    }

    public boolean doSensorUnregister(AwarenessFence fence) {
        LogUtil.d(TAG, "doSensorUnregister ");
        if (fence == null) {
            LogUtil.e(TAG, "doSensorUnregister illegal!");
            return false;
        }
        String[] strArr = this.mSupportedActivities;
        if (strArr == null || strArr.length == 0) {
            this.mSupportedActivities = ConnectServiceManager.getInstance().getMsdpMovementSupportedActivities();
        }
        String[] strArr2 = this.mMsdpSupportedEnvironments;
        if (strArr2 == null || strArr2.length == 0) {
            this.mMsdpSupportedEnvironments = ConnectServiceManager.getInstance().getMsdpSupportedEnvironments();
        }
        ConcurrentHashMap<String, Integer> defaultMovementActionConfigMap = sDefaultMovementActionConfigMap;
        boolean isUnregisterSuccess = true;
        for (Map.Entry<String, Integer> entry : defaultMovementActionConfigMap.entrySet()) {
            if ((entry.getValue().intValue() & fence.getAction()) != 0) {
                String activity = Utils.getMsdpSupportActionStr(entry.getValue().intValue(), defaultMovementActionConfigMap, entry.getValue().intValue() < 65536 ? this.mSupportedActivities : this.mMsdpSupportedEnvironments);
                if ((entry.getValue().intValue() & fence.getAction()) == entry.getValue().intValue() && activity != null) {
                    String str = TAG;
                    LogUtil.d(str, "doSensorUnregister success, entry.getValue():0x" + Integer.toHexString(entry.getValue().intValue()));
                    ConnectServiceManager.getInstance().disableMovementEvent(activity, 0);
                }
            }
        }
        if (!doRealUnRegisterSensor(fence)) {
            isUnregisterSuccess = false;
        }
        String str2 = TAG;
        LogUtil.d(str2, "doSensorUnregister isUnregisterSuccess = " + isUnregisterSuccess);
        return isUnregisterSuccess;
    }

    private boolean doRealUnRegisterSensor(AwarenessFence fence) {
        String topKey;
        String str = TAG;
        LogUtil.d(str, "doRealUnRegisterSensor fence = " + fence);
        if (fence == null || (topKey = fence.getTopKey()) == null || !this.mActionCallbackMap.containsKey(topKey)) {
            return false;
        }
        String str2 = TAG;
        LogUtil.d(str2, "doRealUnRegisterSensor remove topKey = " + topKey);
        this.mActionCallbackMap.remove(fence.getTopKey());
        return true;
    }

    public void onMovementStatusChanged(HwMSDPMovementChangeEvent movementChangeEvent) {
        int eventTypeInt;
        String eventTypeString;
        LogUtil.d(TAG, "onMSChanged");
        if (movementChangeEvent == null) {
            LogUtil.e(TAG, "onMSChanged movementChangeEvent == null");
            return;
        }
        Iterable<HwMSDPMovementEvent> eventIterable = movementChangeEvent.getMovementEvents();
        if (eventIterable == null) {
            LogUtil.e(TAG, "onMSChanged eventIterable == null");
            return;
        }
        int i = 0;
        for (HwMSDPMovementEvent event : eventIterable) {
            i++;
            String movement = event.getMovement();
            String str = TAG;
            LogUtil.d(str, "onMSChanged i = " + i + " " + movement);
            if (TextUtils.isEmpty(movement) || sDefaultMovementActionConfigMap.containsKey(movement)) {
                int eventTypeInt2 = event.getEventType();
                if (eventTypeInt2 == 1) {
                    eventTypeString = "In";
                    eventTypeInt = 1;
                } else if (eventTypeInt2 == 2) {
                    eventTypeString = "Out";
                    eventTypeInt = 2;
                } else {
                    LogUtil.e(TAG, "onMSChanged unknown eventTypeInt");
                }
                long timeNs = event.getTimestampNs();
                int confidence = event.getConfidence();
                String str2 = TAG;
                LogUtil.d(str2, "onMSChanged timeNs:" + timeNs + ",eventTypeString:" + eventTypeString + ",confidence:" + confidence);
                prepareQueryMovementStatus(movement, eventTypeInt, timeNs, confidence);
                executeSensorCallback(event);
            } else {
                LogUtil.e(TAG, "onMSChanged containsKey false");
            }
        }
    }

    private void prepareQueryMovementStatus(String movement, int eventType, long timeNs, int confidence) {
        String str = TAG;
        LogUtil.d(str, "prepareQuery eventType : " + eventType);
        int status = -1;
        if (sDefaultMovementStatusConfigMap.containsKey(Integer.valueOf(eventType))) {
            status = sDefaultMovementStatusConfigMap.get(Integer.valueOf(eventType)).intValue();
        }
        int action = -1;
        if (sDefaultMovementActionConfigMap.containsKey(movement)) {
            action = sDefaultMovementActionConfigMap.get(movement).intValue();
        }
        String str2 = TAG;
        LogUtil.d(str2, "prepareQuery status : " + status + " action : " + action);
        this.mMovementStatusEvent.setEventConfidence(confidence);
        this.mMovementStatusEvent.setEventCurAction(action);
        this.mMovementStatusEvent.setEventCurStatus(status);
        this.mMovementStatusEvent.setEventCurType(1);
        this.mMovementStatusEvent.setEventSensorTime(timeNs);
        this.mMovementStatusEvent.setEventTime(System.currentTimeMillis());
        this.mMovementStatusEvent.setEventTriggerStatus(1);
    }

    private void executeSensorCallback(HwMSDPMovementEvent event) {
        LogUtil.d(TAG, "executeCallback");
        for (Map.Entry<String, PendingIntent> entry : this.mActionCallbackMap.entrySet()) {
            ArrayList<Integer> arrayList = Utils.getFenceActionArrayListFromTopKey(entry.getKey());
            if (arrayList == null) {
                LogUtil.e(TAG, "executeCallback arrayList == null");
                return;
            }
            String str = TAG;
            LogUtil.d(str, "executeCallback arrayList:" + arrayList);
            if (arrayList.size() != 1) {
                LogUtil.e(TAG, "executeCallback arrayList size is not 1");
                return;
            }
            int registerAction = arrayList.get(0).intValue();
            if (registerAction == -1) {
                LogUtil.e(TAG, "executeCallback registerAction invalid");
                return;
            }
            String str2 = TAG;
            LogUtil.d(str2, "executeCallback registerAction : " + registerAction);
            int action = sDefaultMovementActionConfigMap.get(event.getMovement()).intValue();
            long timeNs = event.getTimestampNs();
            int eventTypeInt = event.getEventType();
            if ((action & registerAction) == action) {
                PendingIntent pendingIntent = entry.getValue();
                if (pendingIntent != null) {
                    Intent intent = new Intent();
                    intent.putExtra(AwarenessConstants.DATA_SENSOR_TIME_STAMP, timeNs);
                    intent.putExtra(AwarenessConstants.DATA_EVENT_TYPE, eventTypeInt);
                    intent.putExtra(AwarenessConstants.DATA_ACTION_TYPE, action);
                    intent.putExtra(AwarenessConstants.DATA_ACTION_STRING_TYPE, event.getMovement());
                    String str3 = TAG;
                    LogUtil.d(str3, "executeCallback send eventTypeInt:" + eventTypeInt + ",action:" + action);
                    try {
                        pendingIntent.send(ConnectServiceManager.getInstance().getConnectServiceManagerContext(), 0, intent);
                    } catch (PendingIntent.CanceledException e) {
                        LogUtil.e(TAG, "executeCallback send failure");
                    }
                } else {
                    return;
                }
            }
        }
    }

    public RequestResult doSetReportPeriod(ExtendAwarenessFence awarenessFence) {
        Map.Entry<String, Integer> entry;
        if (awarenessFence == null || awarenessFence.getRegisterBundle() == null) {
            LogUtil.e(TAG, "doSetReportPeriod(): illegal parameters!");
            RequestResult result = new RequestResult(AwarenessConstants.ERROR_UNKNOWN_CODE, AwarenessConstants.ERROR_UNKNOWN);
            result.setResultType(7);
            return result;
        }
        initMsdpSupportParam();
        ConcurrentHashMap<String, Integer> defaultMovementActionConfigmap = sDefaultMovementActionConfigMap;
        boolean isRegistered = false;
        Iterator<Map.Entry<String, Integer>> it = defaultMovementActionConfigmap.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            entry = it.next();
            if ((entry.getValue().intValue() & awarenessFence.getAction()) != 0) {
                isRegistered = true;
                String activity = Utils.getMsdpSupportActionStr(entry.getValue().intValue(), defaultMovementActionConfigmap, entry.getValue().intValue() < 65536 ? this.mSupportedActivities : this.mMsdpSupportedEnvironments);
                if ((entry.getValue().intValue() & awarenessFence.getAction()) != entry.getValue().intValue() || activity == null) {
                    String str = TAG;
                    LogUtil.e(str, "doSetReportPeriod unsupport register type: entry.getValue(): 0x" + Integer.toHexString(entry.getValue().intValue()));
                    isRegistered = false;
                } else {
                    LogUtil.d(TAG, "doSetReportPeriod support register");
                }
            }
        }
        String str2 = TAG;
        LogUtil.e(str2, "doSetReportPeriod unsupport register type: entry.getValue(): 0x" + Integer.toHexString(entry.getValue().intValue()));
        isRegistered = false;
        if (!isRegistered) {
            RequestResult result2 = new RequestResult(AwarenessConstants.ERROR_PARAMETER_CODE, AwarenessConstants.ERROR_PARAMETER);
            result2.setResultType(7);
            return result2;
        } else if (registerToMsdp(awarenessFence, defaultMovementActionConfigmap)) {
            RequestResult result3 = new RequestResult(200000, null);
            result3.setResultType(6);
            return result3;
        } else {
            LogUtil.e(TAG, "doSetReportPeriod(): illegal parameters!");
            RequestResult result4 = new RequestResult(AwarenessConstants.ERROR_UNKNOWN_CODE, AwarenessConstants.ERROR_UNKNOWN);
            result4.setResultType(7);
            return result4;
        }
    }

    private void initMsdpSupportParam() {
        String[] strArr;
        String[] strArr2;
        if (this.mMsdpSupportModule == -1) {
            this.mMsdpSupportModule = ConnectServiceManager.getInstance().getMsdpSupportModule();
            if ((this.mMsdpSupportModule & 1) == 1 && (strArr2 = this.mSupportedActivities) != null && strArr2.length == 0) {
                this.mSupportedActivities = ConnectServiceManager.getInstance().getMsdpMovementSupportedActivities();
                LogUtil.d(TAG, "initParam activities");
            }
            if ((this.mMsdpSupportModule & 2) == 2 && (strArr = this.mMsdpSupportedEnvironments) != null && strArr.length == 0) {
                this.mMsdpSupportedEnvironments = ConnectServiceManager.getInstance().getMsdpSupportedEnvironments();
                LogUtil.d(TAG, "initParam environments");
            }
            String[] strArr3 = this.mSupportedActivities;
            if (strArr3 != null && strArr3.length == 0) {
                LogUtil.e(TAG, "initParam get movement mSupportedActivities failure!");
                this.mMsdpSupportModule = -1;
            }
        }
        String str = TAG;
        LogUtil.d(str, "initParam mMSDPSupportModule = " + this.mMsdpSupportModule);
    }

    private boolean registerToMsdp(ExtendAwarenessFence awarenessFence, ConcurrentHashMap<String, Integer> map) {
        int movementType = 0;
        if (awarenessFence == null || map == null) {
            LogUtil.e(TAG, "registerToMsdp illegal parameters!");
            return false;
        }
        Bundle bundle = awarenessFence.getRegisterBundle();
        if (bundle == null) {
            LogUtil.e(TAG, "registerToMsdp bundle == null");
            return false;
        }
        long screenOnReportPeriod = bundle.getLong(AwarenessConstants.SENSORHUB_CONTROL_REPORT_PERIOD, AwarenessConstants.MSDP_REPORT_FREQUECE_NS);
        String strHwMsdpOtherParameters = null;
        if (Build.VERSION.SDK_INT >= 12) {
            strHwMsdpOtherParameters = bundle.getString(AwarenessConstants.HW_MSDP_OTHER_PARAMS, null);
        }
        HwMSDPOtherParameters hwMsdpOtherParameters = Utils.getHwMsdpOtherParametersByString(strHwMsdpOtherParameters);
        if (hwMsdpOtherParameters != null) {
            movementType = 1;
        }
        LogUtil.d(TAG, "registerToMsdp action:" + awarenessFence.getAction() + " period:" + screenOnReportPeriod + " param: " + hwMsdpOtherParameters + " movementType:" + movementType);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if ((entry.getValue().intValue() & awarenessFence.getAction()) != 0) {
                ConnectServiceManager.getInstance().enableMovementEvent(Utils.getMsdpSupportActionStr(entry.getValue().intValue(), map, entry.getValue().intValue() < 65536 ? this.mSupportedActivities : this.mMsdpSupportedEnvironments), movementType, screenOnReportPeriod, hwMsdpOtherParameters);
            }
        }
        return true;
    }
}
