package com.huawei.nearbysdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IBroadcastScanResultCallBack extends IInterface {
    void onScanResult(byte[] bArr) throws RemoteException;

    public static abstract class Stub extends Binder implements IBroadcastScanResultCallBack {
        private static final String DESCRIPTOR = "com.huawei.nearbysdk.IBroadcastScanResultCallBack";
        static final int TRANSACTION_onScanResult = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBroadcastScanResultCallBack asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBroadcastScanResultCallBack)) {
                return new Proxy(obj);
            }
            return (IBroadcastScanResultCallBack) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                byte[] _arg0 = data.createByteArray();
                onScanResult(_arg0);
                reply.writeNoException();
                reply.writeByteArray(_arg0);
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IBroadcastScanResultCallBack {
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

            @Override // com.huawei.nearbysdk.IBroadcastScanResultCallBack
            public void onScanResult(byte[] msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(msg);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    _reply.readByteArray(msg);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
