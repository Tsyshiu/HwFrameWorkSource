package com.huawei.hiai.awareness.service;

import android.os.RemoteException;
import com.huawei.hiai.awareness.common.ThreadPoolManager;
import com.huawei.hiai.awareness.common.Utils;
import com.huawei.hiai.awareness.common.log.LogUtil;

public class ServiceBindingManager {
    /* access modifiers changed from: private */
    public static final String TAG = ("sdk_" + ServiceBindingManager.class.getSimpleName());
    private static ServiceBindingManager sServiceBindingManager = null;

    private ServiceBindingManager() {
        LogUtil.d(TAG, "ServiceBindingManager()");
    }

    public static ServiceBindingManager getInstance() {
        ServiceBindingManager serviceBindingManager;
        synchronized (ServiceBindingManager.class) {
            if (sServiceBindingManager == null) {
                sServiceBindingManager = new ServiceBindingManager();
            }
            serviceBindingManager = sServiceBindingManager;
        }
        return serviceBindingManager;
    }

    public boolean isFenceFunctionSupported(int fenceType) {
        if ((fenceType == 1 || fenceType == 3) && !Utils.isMsdpInstalled(ConnectServiceManager.getInstance().getConnectServiceManagerContext())) {
            return false;
        }
        return true;
    }

    private static class RequestRunnable implements Runnable {
        private IRequestCallBack requestCallBack;
        private RequestResult result;

        private RequestRunnable(IRequestCallBack requestCallBack2, RequestResult result2) {
            this.requestCallBack = requestCallBack2;
            this.result = result2;
        }

        public void run() {
            try {
                String access$000 = ServiceBindingManager.TAG;
                LogUtil.d(access$000, "RequestRunnable run() start result : " + this.result);
                if (this.requestCallBack != null) {
                    this.requestCallBack.onRequestResult(this.result);
                }
            } catch (RemoteException e) {
                LogUtil.e(ServiceBindingManager.TAG, "RequestRunnable run() RemoteException ");
            }
        }
    }

    private RequestResult buildRequestResultFromAwareness(AwarenessFence awarenessFence, int resultType, int triggerStatus) {
        String str = TAG;
        LogUtil.d(str, "buildRequestResultFromAwareness() awarenessFence :  " + awarenessFence + " resultType : " + resultType + " triggerStatus : " + triggerStatus);
        if (awarenessFence != null) {
            RequestResult result = new RequestResult(awarenessFence.getType(), awarenessFence.getStatus(), awarenessFence.getAction(), null);
            result.setTime(System.currentTimeMillis());
            result.setSensorTime(System.currentTimeMillis());
            result.setConfidence(100);
            result.setRegisterTopKey(awarenessFence.getTopKey());
            result.setResultType(resultType);
            result.setTriggerStatus(triggerStatus);
            String str2 = TAG;
            LogUtil.d(str2, "buildRequestResultFromAwareness() result : " + result);
            return result;
        }
        LogUtil.e(TAG, "buildRequestResultFromAwareness() awarenessFence == null ");
        return null;
    }

    public void registerResultCallback(IRequestCallBack requestCallBack, AwarenessFence awarenessFence, int triggerStatus, int resultCode) {
        LogUtil.d(TAG, "registerResultCallback awarenessFence : " + awarenessFence + " triggerStatus : " + triggerStatus + " resultCode : " + resultCode);
        if (requestCallBack == null || awarenessFence == null) {
            LogUtil.e(TAG, "registerResultCallback() param error");
            return;
        }
        RequestResult result = buildRequestResultFromAwareness(awarenessFence, 1, triggerStatus);
        if (result == null) {
            LogUtil.e(TAG, "registerResultCallback() result == null");
            return;
        }
        if (resultCode == 200009) {
            result.setErrorCode(resultCode);
            result.setErrorResult("unsupport action: " + awarenessFence.getAction());
        }
        ThreadPoolManager.getInstance().startInCacheChildThread(new RequestRunnable(requestCallBack, result));
    }
}
