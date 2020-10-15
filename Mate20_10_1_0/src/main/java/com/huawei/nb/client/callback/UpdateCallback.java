package com.huawei.nb.client.callback;

import android.os.RemoteException;
import com.huawei.nb.callback.IUpdateCallback;

public class UpdateCallback extends IUpdateCallback.Stub implements WaitableCallback<Integer> {
    private static final Integer INVALID_COUNT = -1;
    private final CallbackManager callbackManager;
    private final CallbackWaiter<Integer> callbackWaiter = new CallbackWaiter<>(INVALID_COUNT);

    UpdateCallback(CallbackManager callbackManager2) {
        this.callbackManager = callbackManager2;
    }

    @Override // com.huawei.nb.callback.IUpdateCallback
    public void onResult(int transactionId, int count) throws RemoteException {
        this.callbackWaiter.set(transactionId, Integer.valueOf(count));
    }

    @Override // com.huawei.nb.client.callback.WaitableCallback
    public Integer await(int transactionId, long timeout) {
        this.callbackManager.startWaiting(this);
        Integer result = this.callbackWaiter.await(transactionId, timeout);
        this.callbackManager.stopWaiting(this);
        return result;
    }

    @Override // com.huawei.nb.client.callback.WaitableCallback
    public void interrupt() {
        this.callbackWaiter.interrupt();
    }
}
