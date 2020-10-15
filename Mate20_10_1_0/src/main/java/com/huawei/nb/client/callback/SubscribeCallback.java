package com.huawei.nb.client.callback;

import android.os.RemoteException;
import com.huawei.nb.callback.ISubscribeCallback;

public class SubscribeCallback extends ISubscribeCallback.Stub implements WaitableCallback<Boolean> {
    private final CallbackManager callbackManager;
    private final CallbackWaiter<Boolean> callbackWaiter = new CallbackWaiter<>(false);

    public SubscribeCallback(CallbackManager callbackManager2) {
        this.callbackManager = callbackManager2;
    }

    @Override // com.huawei.nb.callback.ISubscribeCallback
    public void onSuccess(int transactionId) throws RemoteException {
        this.callbackWaiter.set(transactionId, true);
    }

    @Override // com.huawei.nb.callback.ISubscribeCallback
    public void onFailure(int transactionId, String message) throws RemoteException {
        this.callbackWaiter.set(transactionId, false);
    }

    @Override // com.huawei.nb.client.callback.WaitableCallback
    public void interrupt() {
        this.callbackWaiter.interrupt();
    }

    @Override // com.huawei.nb.client.callback.WaitableCallback
    public Boolean await(int transactionId, long timeout) {
        this.callbackManager.startWaiting(this);
        Boolean result = this.callbackWaiter.await(transactionId, timeout);
        this.callbackManager.stopWaiting(this);
        return result;
    }
}
