package com.huawei.nb.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nb.diffprivacy.DiffPrivacyRequestInfo;

public interface IDiffPrivacyServiceCall extends IInterface {
    String getDiffPrivacy(DiffPrivacyRequestInfo diffPrivacyRequestInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements IDiffPrivacyServiceCall {
        private static final String DESCRIPTOR = "com.huawei.nb.service.IDiffPrivacyServiceCall";
        static final int TRANSACTION_getDiffPrivacy = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDiffPrivacyServiceCall asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IDiffPrivacyServiceCall)) {
                return new Proxy(obj);
            }
            return (IDiffPrivacyServiceCall) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            DiffPrivacyRequestInfo _arg0;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = DiffPrivacyRequestInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    String _result = getDiffPrivacy(_arg0);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IDiffPrivacyServiceCall {
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

            @Override // com.huawei.nb.service.IDiffPrivacyServiceCall
            public String getDiffPrivacy(DiffPrivacyRequestInfo requestInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (requestInfo != null) {
                        _data.writeInt(1);
                        requestInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
