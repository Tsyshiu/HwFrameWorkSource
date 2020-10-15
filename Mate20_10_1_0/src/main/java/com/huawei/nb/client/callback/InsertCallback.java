package com.huawei.nb.client.callback;

import android.os.RemoteException;
import com.huawei.nb.callback.IInsertCallback;
import com.huawei.nb.container.ObjectContainer;
import java.util.Collections;
import java.util.List;

public class InsertCallback extends IInsertCallback.Stub implements WaitableCallback<List> {
    private final CallbackManager callbackManager;
    private final CallbackWaiter<List> callbackWaiter = new CallbackWaiter<>(Collections.EMPTY_LIST);

    InsertCallback(CallbackManager callbackManager2) {
        this.callbackManager = callbackManager2;
    }

    @Override // com.huawei.nb.callback.IInsertCallback
    public void onResult(int transactionId, ObjectContainer container) throws RemoteException {
        this.callbackWaiter.set(transactionId, container == null ? Collections.EMPTY_LIST : container.get());
    }

    @Override // com.huawei.nb.client.callback.WaitableCallback
    public List await(int transactionId, long timeout) {
        this.callbackManager.startWaiting(this);
        List result = this.callbackWaiter.await(transactionId, timeout);
        this.callbackManager.stopWaiting(this);
        return result;
    }

    @Override // com.huawei.nb.client.callback.WaitableCallback
    public void interrupt() {
        this.callbackWaiter.interrupt();
    }
}
