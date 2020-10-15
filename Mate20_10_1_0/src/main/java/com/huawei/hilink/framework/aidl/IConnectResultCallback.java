package com.huawei.hilink.framework.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IConnectResultCallback extends IInterface {
    void onConnectFailed(int i, int i2) throws RemoteException;

    void onConnectSuccess(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IConnectResultCallback {
        private static final String DESCRIPTOR = "com.huawei.hilink.framework.aidl.IConnectResultCallback";
        static final int TRANSACTION_onConnectFailed = 2;
        static final int TRANSACTION_onConnectSuccess = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectResultCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IConnectResultCallback)) {
                return new Proxy(obj);
            }
            return (IConnectResultCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                onConnectSuccess(data.readInt());
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                onConnectFailed(data.readInt(), data.readInt());
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IConnectResultCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // com.huawei.hilink.framework.aidl.IConnectResultCallback
            public void onConnectSuccess(int requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(requestId);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.huawei.hilink.framework.aidl.IConnectResultCallback
            public void onConnectFailed(int requestId, int errorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(requestId);
                    _data.writeInt(errorCode);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
