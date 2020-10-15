package com.huawei.nearbysdk.closeRange;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ICloseRangeEventListener extends IInterface {
    void onEvent(CloseRangeResult closeRangeResult) throws RemoteException;

    public static abstract class Stub extends Binder implements ICloseRangeEventListener {
        private static final String DESCRIPTOR = "com.huawei.nearbysdk.closeRange.ICloseRangeEventListener";
        static final int TRANSACTION_onEvent = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICloseRangeEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ICloseRangeEventListener)) {
                return new Proxy(obj);
            }
            return (ICloseRangeEventListener) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            CloseRangeResult _arg0;
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                if (data.readInt() != 0) {
                    _arg0 = CloseRangeResult.CREATOR.createFromParcel(data);
                } else {
                    _arg0 = null;
                }
                onEvent(_arg0);
                reply.writeNoException();
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements ICloseRangeEventListener {
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

            @Override // com.huawei.nearbysdk.closeRange.ICloseRangeEventListener
            public void onEvent(CloseRangeResult result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
