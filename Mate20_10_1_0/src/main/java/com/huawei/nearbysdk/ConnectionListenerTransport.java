package com.huawei.nearbysdk;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.nearbysdk.IInternalConnectionListener;
import java.util.HashMap;

public class ConnectionListenerTransport extends IInternalConnectionListener.Stub {
    private static final String BUNDLE_RECEIVE_MESSAGE = "MESSAGE";
    public static final int ERROR_SESSION_CREATE_FAIL = 0;
    private static final int EVENT_CONNECTED = 2;
    static final String TAG = "ConnectionListener";
    private static final int TYPE_CONNECTION_CHANGE = 2;
    private static final int TYPE_RECEIVE_DATA = 4;
    private static final int TYPE_STATUS_CHANGED = 1;
    private int mBusinessId;
    private Context mContext;
    private ConnectionListener mListener;
    private final Handler mListenerHandler;
    private HashMap<String, byte[]> mMapRecvs = new HashMap<>();

    @Override // com.huawei.nearbysdk.IInternalConnectionListener
    public void onStatusChange(int state) {
        HwLog.d(TAG, "onStatusChange state = " + state);
        sendMessage(1, null, state, null);
    }

    @Override // com.huawei.nearbysdk.IInternalConnectionListener
    public void onConnectionChange(NearbyDevice device, int status) {
        HwLog.d(TAG, "onConnectionChange state = " + status);
        sendMessage(2, device, status, null);
    }

    @Override // com.huawei.nearbysdk.IInternalConnectionListener
    public void onReceive(NearbyDevice device, byte[] recvMessage) {
        HwLog.d(TAG, "onReceive");
        sendMessage(4, device, -1, recvMessage);
    }

    private void sendMessage(int msgWhat, NearbyDevice device, int state, byte[] byteArray) {
        Message msg = this.mListenerHandler.obtainMessage(msgWhat, device);
        msg.arg1 = state;
        if (byteArray != null) {
            Bundle bundle = new Bundle();
            bundle.putByteArray(BUNDLE_RECEIVE_MESSAGE, byteArray);
            msg.setData(bundle);
        }
        this.mListenerHandler.sendMessage(msg);
    }

    ConnectionListenerTransport(Context context, int businessId, ConnectionListener listener, Looper looper) {
        this.mListener = listener;
        this.mBusinessId = businessId;
        this.mContext = context.getApplicationContext();
        this.mListenerHandler = new Handler(looper) {
            /* class com.huawei.nearbysdk.ConnectionListenerTransport.AnonymousClass1 */

            public void handleMessage(Message msg) {
                ConnectionListenerTransport.this._handleMessage(msg);
            }
        };
    }

    /* access modifiers changed from: private */
    public void _handleMessage(Message msg) {
        NearbyDevice device = (NearbyDevice) msg.obj;
        int arg1 = msg.arg1;
        byte[] recvMessage = msg.getData().getByteArray(BUNDLE_RECEIVE_MESSAGE);
        HwLog.d(TAG, "_handleMessage: " + msg.toString());
        int i = msg.what;
        if (i != 4) {
            switch (i) {
                case 1:
                    HwLog.d(TAG, "Listener.onStatusChange");
                    callBackOnStatusChange(arg1);
                    return;
                case 2:
                    HwLog.d(TAG, "Listener.onConnectionChange");
                    callBackOnConnectionChange(device, arg1);
                    return;
                default:
                    HwLog.e(TAG, "_handleMessage: unknown message " + msg.what);
                    return;
            }
        } else {
            HwLog.d(TAG, "Listener.onReceive");
            callBackOnReceive(device, recvMessage);
        }
    }

    private void callBackOnStatusChange(int state) {
        this.mListener.onStatusChange(state);
    }

    private void callBackOnConnectionChange(NearbyDevice device, int state) {
        HwLog.d(TAG, "callBackOnConnectionChange ");
        NearbySession session = NearbySession.getNearbySession(device);
        if (session == null) {
            this.mListener.onCreateFail(device, state, 0);
            return;
        }
        String key = device.getSummary();
        if (this.mMapRecvs.containsKey(key)) {
            this.mMapRecvs.remove(key);
            this.mListener.onReceiveSession(session);
            this.mListener.onReceiveData(session, this.mMapRecvs.get(key));
        } else if (state == 2) {
            this.mListener.onCreateSuccess(session);
        } else {
            this.mListener.onCreateFail(device, state, 0);
            session.close();
        }
    }

    private void callBackOnReceive(NearbyDevice device, byte[] recvMessage) {
        HwLog.d(TAG, "callBackOnReceive ");
        NearbySession session = NearbySession.getNearbySession(device);
        if (session == null) {
            this.mMapRecvs.put(device.getSummary(), recvMessage);
            NearbySession.createNearbySession(this.mContext, this.mBusinessId, device, 5000);
            return;
        }
        this.mListener.onReceiveSession(session);
        this.mListener.onReceiveData(session, recvMessage);
    }
}
