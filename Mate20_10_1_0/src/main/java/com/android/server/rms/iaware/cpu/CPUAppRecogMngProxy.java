package com.android.server.rms.iaware.cpu;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.rms.iaware.AwareLog;
import com.huawei.android.pgmng.plug.PowerKit;
import com.huawei.displayengine.IDisplayEngineService;
import java.util.concurrent.atomic.AtomicBoolean;

public class CPUAppRecogMngProxy {
    private static final int CONNECT_PG_DELAYED = 5000;
    private static final int CYCLE_MAX_NUM = 6;
    private static final int MSG_PG_CONNECT = 1;
    private static final String TAG = "CPUAppRecogMngProxy";
    private Context mContext;
    private CpuAppRecogMngProxyHandler mCpuAppRecogMngProxyHandler;
    private int mCycleNum = 0;
    private PowerKit mPgSdk = null;
    private AtomicBoolean mRegistered = new AtomicBoolean(false);
    private PowerKit.Sink mSink = null;

    public CPUAppRecogMngProxy(Context context) {
        this.mContext = context;
        this.mCpuAppRecogMngProxyHandler = new CpuAppRecogMngProxyHandler();
        getPgSdk();
    }

    private class CpuAppRecogMngProxyHandler extends Handler {
        private CpuAppRecogMngProxyHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what != 1) {
                AwareLog.w(CPUAppRecogMngProxy.TAG, "msg.what = " + msg.what + "  is Invalid !");
                return;
            }
            CPUAppRecogMngProxy.this.getPgSdk();
        }
    }

    private void callPgRegisterListener() {
        PowerKit.Sink sink;
        if (!this.mRegistered.get()) {
            PowerKit powerKit = this.mPgSdk;
            if (powerKit == null || (sink = this.mSink) == null) {
                AwareLog.e(TAG, "callPgRegisterListener mPgSdk == null");
                return;
            }
            try {
                powerKit.enableStateEvent(sink, (int) IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT);
                this.mPgSdk.enableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                this.mPgSdk.enableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_START);
                this.mPgSdk.enableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_END);
                this.mRegistered.set(true);
            } catch (RemoteException e) {
                this.mPgSdk = null;
                this.mCycleNum = 0;
                AwareLog.e(TAG, "mPgSdk registerSink && enableStateEvent happend RemoteException!");
            }
        }
    }

    private void callPgUnregisterListener() {
        if (this.mPgSdk != null && this.mRegistered.get()) {
            try {
                this.mPgSdk.disableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT);
                this.mPgSdk.disableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                this.mPgSdk.disableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_START);
                this.mPgSdk.disableStateEvent(this.mSink, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_END);
                this.mRegistered.set(false);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "callPgUnregisterListener happend RemoteException!");
            }
        }
    }

    /* access modifiers changed from: private */
    public void getPgSdk() {
        int i;
        if (this.mPgSdk == null) {
            this.mPgSdk = PowerKit.getInstance();
            if (this.mPgSdk != null || (i = this.mCycleNum) >= 6) {
                callPgRegisterListener();
                return;
            }
            this.mCycleNum = i + 1;
            this.mCpuAppRecogMngProxyHandler.removeMessages(1);
            this.mCpuAppRecogMngProxyHandler.sendEmptyMessageDelayed(1, 5000);
        }
    }

    public boolean isGameType(int stateType) {
        if (stateType == 10011 || stateType == 10002) {
            return true;
        }
        return false;
    }

    public boolean isVideoType(int stateType) {
        if (stateType == 10015 || stateType == 10016) {
            return true;
        }
        return false;
    }

    public void register(PowerKit.Sink sink) {
        if (this.mSink == null && sink != null) {
            this.mSink = sink;
            callPgRegisterListener();
        }
    }

    public void unregister(PowerKit.Sink sink) {
        if (this.mSink == sink && sink != null) {
            callPgUnregisterListener();
            this.mSink = null;
        }
    }
}
