package com.huawei.hiai.awareness.service;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.RemoteException;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hiai.awareness.common.log.LogUtil;
import com.huawei.hiai.awareness.movement.MovementController;

public class AwarenessBinder {
    private static final String TAG = "sdk_AwarenessBinder";
    private static AwarenessBinder sInstance;

    private AwarenessBinder() {
        LogUtil.d(TAG, "AwarenessBinder()");
    }

    public static synchronized AwarenessBinder getInstance() {
        AwarenessBinder awarenessBinder;
        synchronized (AwarenessBinder.class) {
            if (sInstance == null) {
                sInstance = new AwarenessBinder();
            }
            awarenessBinder = sInstance;
        }
        return awarenessBinder;
    }

    public boolean unRegisterFence(IRequestCallBack callback, AwarenessFence awarenessFence, Bundle bundle, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "unRegisterFence() awarenessFence : " + awarenessFence);
        if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "unRegisterFence() param error");
            return false;
        }
        LogUtil.d(TAG, "unRegisterFence() pendingOperation.hashCode : " + pendingOperation.hashCode());
        boolean isUnregisterSuccess = false;
        if (awarenessFence.getType() == 1) {
            isUnregisterSuccess = MovementController.getInstance().doSensorUnregister(awarenessFence);
        }
        if (isUnregisterSuccess) {
            RequestResult result = new RequestResult();
            result.setRegisterTopKey(awarenessFence.getTopKey());
            result.setResultType(1);
            result.setTriggerStatus(5);
            try {
                callback.onRequestResult(result);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "unRegisterFence() success RemoteException");
            }
        } else {
            RequestResult result2 = new RequestResult();
            result2.setRegisterTopKey(awarenessFence.getTopKey());
            result2.setResultType(1);
            result2.setTriggerStatus(6);
            try {
                callback.onRequestResult(result2);
            } catch (RemoteException e2) {
                LogUtil.e(TAG, "unRegisterFence() failed RemoteException");
            }
        }
        LogUtil.d(TAG, "unRegisterFence() isUnregisterSuccess : " + isUnregisterSuccess);
        return isUnregisterSuccess;
    }

    private boolean registerFence(int fenceType, int fenceAction, IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerFence() fenceType : " + fenceType + " fenceAction : " + fenceAction);
        if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerFence() param error");
            return false;
        }
        LogUtil.d(TAG, "registerFence() pendingOperation.hashCode : " + pendingOperation.hashCode());
        RequestResult result = new RequestResult();
        if (!ServiceBindingManager.getInstance().isFenceFunctionSupported(fenceType)) {
            RequestResult result2 = new RequestResult(AwarenessConstants.ERROR_FUNCTION_NOT_SUPPORTED_CODE, AwarenessConstants.ERROR_FUNCTION_NOT_SUPPORTED);
            result2.setResultType(1);
            result2.setTriggerStatus(4);
            try {
                callback.onRequestResult(result2);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "registerFence() RemoteException");
            }
            return false;
        } else if (isFenceParameterError(fenceType, fenceAction, awarenessFence)) {
            LogUtil.e(TAG, "registerFence() parameter error");
            result.setResultType(1);
            result.setTriggerStatus(4);
            result.setErrorCode(AwarenessConstants.ERROR_PARAMETER_CODE);
            result.setErrorResult(AwarenessConstants.ERROR_PARAMETER);
            try {
                callback.onRequestResult(result);
            } catch (RemoteException e2) {
                LogUtil.e(TAG, "registerFence() RemoteException");
            }
            return false;
        } else {
            awarenessFence.setPendingOperation(pendingOperation);
            if (awarenessFence.getType() != 1) {
                return false;
            }
            return MovementController.getInstance().doSensorRegister(awarenessFence, callback, pendingOperation);
        }
    }

    private boolean isFenceParameterError(int fenceType, int fenceAction, AwarenessFence awarenessFence) {
        LogUtil.d(TAG, "isFenceParameterError() fenceType : " + fenceType + " fenceAction : " + fenceAction);
        boolean isParameterError = false;
        if (awarenessFence == null) {
            LogUtil.e(TAG, "isFenceParameterError() awarenessFence == null");
            return true;
        }
        if (fenceType != 1) {
            if (fenceType != 3) {
                LogUtil.e(TAG, "isFenceParameterError() unknown type error");
                isParameterError = true;
            } else if (awarenessFence.getType() != 3) {
                LogUtil.e(TAG, "isFenceParameterError() DEVICE_STATUS_TYPE type error");
                isParameterError = true;
            }
        } else if (awarenessFence.getType() != 1) {
            LogUtil.e(TAG, "isFenceParameterError() MOVEMENT_TYPE type error");
            isParameterError = true;
        }
        LogUtil.d(TAG, "isFenceParameterError() isParameterError : " + isParameterError);
        return isParameterError;
    }

    public RequestResult getSupportAwarenessCapability(int type) {
        LogUtil.d(TAG, "getSupportAwarenessCapability() type : " + type);
        RequestResult result = new RequestResult();
        if (type != 1) {
            result = new RequestResult(AwarenessConstants.ERROR_PARAMETER_CODE, AwarenessConstants.ERROR_PARAMETER);
            result.setResultType(3);
        } else {
            int capability = ConnectServiceManager.getInstance().getMovementCapability();
            if (capability >= 0) {
                result.setAction(capability);
                result.setResultType(5);
            } else {
                result = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
                result.setResultType(4);
            }
            LogUtil.d(TAG, "getSupportAwarenessCapability() MOVEMENT_TYPE result : " + result);
        }
        result.setType(type);
        LogUtil.d(TAG, "getSupportAwarenessCapability() result : " + result);
        return result;
    }

    public boolean registerMovementFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, Bundle bundle, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerMovementFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(1, -1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerMovementFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerDeviceStatusFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, Bundle bundle, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerDeviceStatusFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(3, -1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerDeviceStatusFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean isIntegrateSensorHub() {
        boolean isSensorHubIntegrated = ConnectServiceManager.getInstance().isIntegrateSensorHub();
        LogUtil.d(TAG, "isIntegrateSensorHub() isSensorHubIntegrated : " + isSensorHubIntegrated);
        return isSensorHubIntegrated;
    }
}
