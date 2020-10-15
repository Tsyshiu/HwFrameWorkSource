package com.huawei.opcollect.odmf;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.strategy.OpenPlatformSwitch;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectThreadLooperCheck;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;

public class OdmfCollectScheduler {
    private static final String CTRLTHREAD_NAME = "OdmfCtrlThread";
    private static final String DATATHREAD_NAME = "OdmfDataThread";
    private static final boolean DEBUG = false;
    private static final Object LOCK = new Object();
    public static final int MSG_CTRL_ODMF_INIT = 1;
    public static final int MSG_CTRL_TIME_TICK = 101;
    public static final int MSG_DATA_RAW_DATA_INSERT = 4;
    public static final int MSG_DATA_RAW_DATA_UPDATE = 5;
    public static final int MSG_MAX = 200;
    public static final int MSG_ODMF_CONNECTED = 103;
    public static final int MSG_ODMF_DISCONNECTED = 104;
    public static final int MSG_ODMF_POLICY_CHANGED = 102;
    public static final int MSG_ODMF_SWITCH_CHANGED = 105;
    public static final int MSG_SCREEN_OFF = 7;
    public static final int MSG_SCREEN_ON = 6;
    public static final int MSG_SWITCH_OFF = 3;
    public static final int MSG_SWITCH_ON = 2;
    private static final long ODMF_CONNECT_TIME = 2000;
    private static final String RECVTHREAD_NAME = "OdmfRecvThread";
    private static final int STATE_AMINITED = 3;
    private static final int STATE_INITED = 1;
    private static final int STATE_ODMFCONNECTED = 2;
    private static final int STATE_UNINITED = 0;
    private static final String TAG = "OdmfCollectScheduler";
    private static OdmfCollectScheduler instance = null;
    /* access modifiers changed from: private */
    public static StateMachine mState = new StateMachine();
    /* access modifiers changed from: private */
    public Context mContext = null;
    private OdmfCtrlMsgHandler mCtrlHandler = null;
    private OdmfCtrlMsgHandler mDataHandler = null;
    /* access modifiers changed from: private */
    public OdmfHelper mOdmfHelper = null;
    private OdmfCtrlMsgHandler mRecvHandler = null;

    private OdmfCollectScheduler() {
        OPCollectLog.r(TAG, TAG);
    }

    public static OdmfCollectScheduler getInstance() {
        OdmfCollectScheduler odmfCollectScheduler;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new OdmfCollectScheduler();
            }
            odmfCollectScheduler = instance;
        }
        return odmfCollectScheduler;
    }

    /* access modifiers changed from: private */
    public static final class StateMachine {
        /* access modifiers changed from: private */
        public int state;

        StateMachine() {
            this.state = 0;
            this.state = 0;
        }

        /* access modifiers changed from: package-private */
        public boolean isInited() {
            return this.state >= 1;
        }

        /* access modifiers changed from: package-private */
        public boolean isOdmfConnected() {
            return this.state >= 2;
        }

        public boolean isAMInited() {
            return this.state >= 3;
        }

        public String toString() {
            switch (this.state) {
                case 0:
                    return "STATE_UNINITED";
                case 1:
                    return "STATE_INITED";
                case 2:
                    return "STATE_ODMFCONNECTED";
                case 3:
                    return "STATE_AMINITED";
                default:
                    return "UNKNOWN STATE";
            }
        }
    }

    public void initialize(Context context) {
        OPCollectLog.e(TAG, "odmf initialize contxt: " + context);
        if (!OPCollectUtils.isPkgInstalled(context, OPCollectUtils.ODMF_PACKAGE_NAME)) {
            OPCollectLog.e(TAG, "odmf is not installed.");
            return;
        }
        this.mContext = context;
        int unused = mState.state = 1;
        this.mOdmfHelper = new OdmfHelper(this.mContext);
        HandlerThread ctrlThread = new HandlerThread(CTRLTHREAD_NAME);
        ctrlThread.start();
        this.mCtrlHandler = new OdmfCtrlMsgHandler(ctrlThread.getLooper());
        if (OpenPlatformSwitch.getInstance().getSwitchState()) {
            this.mCtrlHandler.sendEmptyMessage(1);
        }
        this.mDataHandler = new OdmfCtrlMsgHandler(ctrlThread.getLooper());
        HandlerThread receiverThread = new HandlerThread(RECVTHREAD_NAME);
        receiverThread.start();
        this.mRecvHandler = new OdmfCtrlMsgHandler(receiverThread.getLooper());
        OPCollectThreadLooperCheck.initLoopCheck(ctrlThread);
        OPCollectThreadLooperCheck.initLoopCheck(receiverThread);
        OpenPlatformSwitch.getInstance().initialize(context);
    }

    /* access modifiers changed from: private */
    public void switchOn() {
        if (mState.isOdmfConnected()) {
            OdmfActionManager.getInstance().initialize(this.mContext);
            int unused = mState.state = 3;
        } else if (mState.isInited()) {
            this.mCtrlHandler.removeMessages(1);
            this.mCtrlHandler.sendEmptyMessage(1);
        }
    }

    /* access modifiers changed from: private */
    public void switchOff() {
        if (mState.isAMInited()) {
            OdmfActionManager.getInstance().uninitialize();
            int unused = mState.state = 2;
        }
    }

    /* access modifiers changed from: private */
    public void screenOn() {
        OdmfActionManager.getInstance().checkIfEnableLocation();
        OdmfActionManager.getInstance().checkIfEnableARService();
    }

    /* access modifiers changed from: private */
    public void screenOff() {
        OdmfActionManager.getInstance().checkIfDisableLocation();
        OdmfActionManager.getInstance().checkIfDisableARService();
    }

    public Handler getCtrlHandler() {
        return this.mCtrlHandler;
    }

    public Handler getDataHandler() {
        return this.mDataHandler;
    }

    public Handler getRecvHandler() {
        return this.mRecvHandler;
    }

    public OdmfHelper getOdmfHelper() {
        return this.mOdmfHelper;
    }

    private final class OdmfCtrlMsgHandler extends Handler {
        public OdmfCtrlMsgHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what < 101 || msg.what > 200) {
                OPCollectLog.d(OdmfCollectScheduler.TAG, "handleMessage msg: " + msg.what);
                switch (msg.what) {
                    case 1:
                        if (!OdmfCollectScheduler.this.mOdmfHelper.connectOdmfService()) {
                            sendEmptyMessageDelayed(1, OdmfCollectScheduler.ODMF_CONNECT_TIME);
                            return;
                        }
                        if (!OdmfCollectScheduler.mState.isAMInited()) {
                            OdmfActionManager.getInstance().initialize(OdmfCollectScheduler.this.mContext);
                        }
                        int unused = OdmfCollectScheduler.mState.state = 3;
                        return;
                    case 2:
                        OdmfCollectScheduler.this.switchOn();
                        return;
                    case 3:
                        OdmfCollectScheduler.this.switchOff();
                        return;
                    case 4:
                        OdmfCollectScheduler.this.onRawDataInsert(msg);
                        return;
                    case 5:
                        OdmfCollectScheduler.this.onRawDataUpdate(msg);
                        return;
                    case 6:
                        OdmfCollectScheduler.this.screenOn();
                        return;
                    case 7:
                        OdmfCollectScheduler.this.screenOff();
                        return;
                    default:
                        OPCollectLog.e(OdmfCollectScheduler.TAG, "handleMessage error msg.");
                        return;
                }
            } else {
                OdmfActionManager.handleMessage(msg);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onRawDataInsert(Message msg) {
        AManagedObject rawData = (AManagedObject) msg.obj;
        if (rawData != null && this.mOdmfHelper != null) {
            this.mOdmfHelper.insertManageObject(rawData);
        }
    }

    /* access modifiers changed from: private */
    public void onRawDataUpdate(Message msg) {
        AManagedObject rawData = (AManagedObject) msg.obj;
        if (rawData != null && this.mOdmfHelper != null) {
            this.mOdmfHelper.updateManageObject(rawData);
        }
    }

    public static void dump(PrintWriter pw) {
        pw.println("OdmfCollectScheduler MachineState: " + mState.toString());
        OdmfActionManager.dump(pw);
    }
}
