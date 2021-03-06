package com.mediatek.ims.impl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMtkMapconServiceCallback extends IInterface {
    void onVoWifiCloseDone() throws RemoteException;

    public static class Default implements IMtkMapconServiceCallback {
        @Override // com.mediatek.ims.impl.IMtkMapconServiceCallback
        public void onVoWifiCloseDone() throws RemoteException {
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IMtkMapconServiceCallback {
        private static final String DESCRIPTOR = "com.mediatek.ims.impl.IMtkMapconServiceCallback";
        static final int TRANSACTION_onVoWifiCloseDone = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkMapconServiceCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IMtkMapconServiceCallback)) {
                return new Proxy(obj);
            }
            return (IMtkMapconServiceCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                onVoWifiCloseDone();
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IMtkMapconServiceCallback {
            public static IMtkMapconServiceCallback sDefaultImpl;
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

            @Override // com.mediatek.ims.impl.IMtkMapconServiceCallback
            public void onVoWifiCloseDone() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (this.mRemote.transact(1, _data, null, 1) || Stub.getDefaultImpl() == null) {
                        _data.recycle();
                    } else {
                        Stub.getDefaultImpl().onVoWifiCloseDone();
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IMtkMapconServiceCallback impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static IMtkMapconServiceCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
