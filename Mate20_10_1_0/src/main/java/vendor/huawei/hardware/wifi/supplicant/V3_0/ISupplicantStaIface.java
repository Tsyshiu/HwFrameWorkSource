package vendor.huawei.hardware.wifi.supplicant.V3_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISupplicantStaIface extends android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface {
    public static final String kInterfaceName = "vendor.huawei.hardware.wifi.supplicant@3.0::ISupplicantStaIface";

    @FunctionalInterface
    public interface VowifiDetectCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface deliverStaIfaceDataCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getApVendorInfoCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getCapabRsdbCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getMssStateCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getWpasConfigCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface heartBeatCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    SupplicantStatus AbsPowerCtrl(int i) throws RemoteException;

    SupplicantStatus SetAbsBlacklist(String str) throws RemoteException;

    SupplicantStatus SetAbsCapability(int i) throws RemoteException;

    void VowifiDetect(String str, VowifiDetectCallback vowifiDetectCallback) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    void deliverStaIfaceData(int i, int i2, String str, deliverStaIfaceDataCallback deliverstaifacedatacallback) throws RemoteException;

    SupplicantStatus enableHiLinkHandshake(boolean z, String str) throws RemoteException;

    void getApVendorInfo(getApVendorInfoCallback getapvendorinfocallback) throws RemoteException;

    void getCapabRsdb(getCapabRsdbCallback getcapabrsdbcallback) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getMssState(getMssStateCallback getmssstatecallback) throws RemoteException;

    void getWpasConfig(int i, getWpasConfigCallback getwpasconfigcallback) throws RemoteException;

    void heartBeat(String str, heartBeatCallback heartbeatcallback) throws RemoteException;

    SupplicantStatus hwStaRegisterCallback(ISupplicantStaIfaceCallback iSupplicantStaIfaceCallback) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    SupplicantStatus pwrPercentBoostModeset(int i) throws RemoteException;

    SupplicantStatus setFilterEnable(boolean z) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    SupplicantStatus setTxPower(int i) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    SupplicantStatus wnmBssQurey(int i) throws RemoteException;

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    static default ISupplicantStaIface asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISupplicantStaIface)) {
            return (ISupplicantStaIface) iface;
        }
        ISupplicantStaIface proxy = new Proxy(binder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (it.next().equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    static default ISupplicantStaIface castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    static default ISupplicantStaIface getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    static default ISupplicantStaIface getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    static default ISupplicantStaIface getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
    static default ISupplicantStaIface getService() throws RemoteException {
        return getService("default");
    }

    public static final class AbsMode {
        public static final int Abs_MIMO_Mode = 2;
        public static final int Abs_SISO_Mode = 1;

        public static final String toString(int o) {
            if (o == 1) {
                return "Abs_SISO_Mode";
            }
            if (o == 2) {
                return "Abs_MIMO_Mode";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            if ((o & 1) == 1) {
                list.add("Abs_SISO_Mode");
                flipped = 0 | 1;
            }
            if ((o & 2) == 2) {
                list.add("Abs_MIMO_Mode");
                flipped |= 2;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class Proxy implements ISupplicantStaIface {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.huawei.hardware.wifi.supplicant@3.0::ISupplicantStaIface]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public void getName(getNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public void getType(getTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public void addNetwork(addNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus removeNetwork(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public void getNetwork(int id, getNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public void listNetworks(listNetworksCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsDeviceName(String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsDeviceType(byte[] type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(8);
            if (type == null || type.length != 8) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, type);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsManufacturer(String manufacturer) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(manufacturer);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsModelName(String modelName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsModelNumber(String modelNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsSerialNumber(String serialNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(serialNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface
        public SupplicantStatus setWpsConfigMethods(short configMethods) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt16(configMethods);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus registerCallback(ISupplicantStaIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus reassociate() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus reconnect() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus disconnect() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus setPowerSave(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus initiateTdlsDiscover(byte[] macAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (macAddress == null || macAddress.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, macAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus initiateTdlsSetup(byte[] macAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (macAddress == null || macAddress.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, macAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus initiateTdlsTeardown(byte[] macAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (macAddress == null || macAddress.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, macAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus initiateAnqpQuery(byte[] macAddress, ArrayList<Short> infoElements, ArrayList<Integer> subTypes) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (macAddress == null || macAddress.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, macAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt16Vector(infoElements);
            _hidl_request.writeInt32Vector(subTypes);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(22, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus initiateHs20IconQuery(byte[] macAddress, String fileName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (macAddress == null || macAddress.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, macAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeString(fileName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public void getMacAddress(getMacAddressCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_macAddr = new byte[6];
                _hidl_reply.readBuffer(6).copyToInt8Array(0, _hidl_out_macAddr, 6);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_macAddr);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus startRxFilter() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(25, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus stopRxFilter() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(26, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus addRxFilter(byte type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt8(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(27, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus removeRxFilter(byte type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt8(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(28, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus setBtCoexistenceMode(byte mode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt8(mode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(29, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus setBtCoexistenceScanModeEnabled(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(30, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus setSuspendModeEnabled(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(31, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus setCountryCode(byte[] code) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(2);
            if (code == null || code.length != 2) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, code);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(32, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus startWpsRegistrar(byte[] bssid, String pin) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (bssid == null || bssid.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, bssid);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeString(pin);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(33, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus startWpsPbc(byte[] bssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (bssid == null || bssid.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, bssid);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(34, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus startWpsPinKeypad(String pin) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(pin);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(35, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public void startWpsPinDisplay(byte[] bssid, startWpsPinDisplayCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            if (bssid == null || bssid.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, bssid);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(36, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus cancelWps() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(37, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus setExternalSim(boolean useExternalSim) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(useExternalSim);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(38, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public void addExtRadioWork(String name, int freqInMhz, int timeoutInSec, addExtRadioWorkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(name);
            _hidl_request.writeInt32(freqInMhz);
            _hidl_request.writeInt32(timeoutInSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(39, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus removeExtRadioWork(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(40, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface
        public SupplicantStatus enableAutoReconnect(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(41, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface
        public SupplicantStatus registerCallback_1_1(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(42, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public SupplicantStatus registerCallback_1_2(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(43, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public void getKeyMgmtCapabilities(getKeyMgmtCapabilitiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(44, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public void addDppPeerUri(String uri, addDppPeerUriCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(uri);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(45, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public SupplicantStatus removeDppUri(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(46, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public SupplicantStatus startDppConfiguratorInitiator(int peerBootstrapId, int ownBootstrapId, String ssid, String password, String psk, int netRole, int securityAkm) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(peerBootstrapId);
            _hidl_request.writeInt32(ownBootstrapId);
            _hidl_request.writeString(ssid);
            _hidl_request.writeString(password);
            _hidl_request.writeString(psk);
            _hidl_request.writeInt32(netRole);
            _hidl_request.writeInt32(securityAkm);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(47, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public SupplicantStatus startDppEnrolleeInitiator(int peerBootstrapId, int ownBootstrapId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(peerBootstrapId);
            _hidl_request.writeInt32(ownBootstrapId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(48, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface
        public SupplicantStatus stopDppInitiator() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(49, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus hwStaRegisterCallback(ISupplicantStaIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(50, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus setFilterEnable(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(51, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void heartBeat(String param, heartBeatCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(param);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(52, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus wnmBssQurey(int reason) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(reason);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(53, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void getWpasConfig(int psktype, getWpasConfigCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(psktype);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(54, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus enableHiLinkHandshake(boolean enable, String bssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            _hidl_request.writeString(bssid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(55, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus SetAbsCapability(int capability) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(capability);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(56, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus SetAbsBlacklist(String Blacklist) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(Blacklist);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(57, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus AbsPowerCtrl(int operation) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(operation);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(58, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void VowifiDetect(String OpCmd, VowifiDetectCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeString(OpCmd);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(59, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void getCapabRsdb(getCapabRsdbCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(60, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus setTxPower(int level) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(level);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(61, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public SupplicantStatus pwrPercentBoostModeset(int rssi) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(rssi);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(62, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void getMssState(getMssStateCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(63, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void getApVendorInfo(getApVendorInfoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(64, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface
        public void deliverStaIfaceData(int cmdType, int dataType, String carryData, deliverStaIfaceDataCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdType);
            _hidl_request.writeInt32(dataType);
            _hidl_request.writeString(carryData);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(65, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                return _hidl_reply.readStringVector();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256131655, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                return _hidl_reply.readString();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList<>();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16);
                int _hidl_vec_size = _hidl_blob.getInt32(8);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer((long) (_hidl_vec_size * 32), _hidl_blob.handle(), 0, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    byte[] _hidl_vec_element = new byte[32];
                    childBlob.copyToInt8Array((long) (_hidl_index_0 * 32), _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends HwBinder implements ISupplicantStaIface {
        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantStaIface.kInterfaceName, android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface.kInterfaceName, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName, ISupplicantIface.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> arrayList) {
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return ISupplicantStaIface.kInterfaceName;
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{109, -125, -8, 76, 117, 95, 72, 34, 63, 76, 103, -113, -101, 89, -29, 113, -9, 32, 51, -107, -102, -107, -21, 31, 36, -51, 27, 99, 120, 83, -20, 113}, new byte[]{126, -2, 43, 5, 126, -97, -109, -121, -77, 80, 14, 103, -81, -105, -108, 42, -89, -56, 0, -114, 110, -25, -40, -36, -86, -28, 16, Byte.MAX_VALUE, -38, -124, 1, 107}, new byte[]{33, 117, 125, 14, 93, -44, -73, -28, -67, -104, 26, 74, 32, 83, 27, -54, 60, 50, 39, 26, -39, 119, 123, 23, -73, 78, -75, -95, -22, 80, -125, -124}, new byte[]{119, 82, -31, -34, -109, -86, -11, -2, -45, 112, 17, -62, 25, -84, 36, 112, 105, -10, -81, 50, 11, 8, 16, -38, -87, -123, 16, 88, 74, 16, -25, -76}, new byte[]{53, -70, 123, -51, -15, -113, 36, -88, 102, -89, -27, 66, -107, 72, -16, 103, 104, -69, 32, -94, 87, -9, 91, 16, -93, -105, -60, -40, 37, -17, -124, 56}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, 24, -54, 76}));
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_0.ISupplicantIface, vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ISupplicantStaIface.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, final HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            boolean _hidl_is_oneway = false;
            boolean _hidl_is_oneway2 = true;
            switch (_hidl_code) {
                case 1:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    getName(new getNameCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass1 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNameCallback
                        public void onValues(SupplicantStatus status, String name) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(name);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 2:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    getType(new getTypeCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass2 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getTypeCallback
                        public void onValues(SupplicantStatus status, int type) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(type);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    addNetwork(new addNetworkCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass3 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface.addNetworkCallback
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeStrongBinder(network == null ? null : network.asBinder());
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status = removeNetwork(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 5:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    getNetwork(_hidl_request.readInt32(), new getNetworkCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass4 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNetworkCallback
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeStrongBinder(network == null ? null : network.asBinder());
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 6:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    listNetworks(new listNetworksCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass5 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantIface.listNetworksCallback
                        public void onValues(SupplicantStatus status, ArrayList<Integer> networkIds) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32Vector(networkIds);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 7:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status2 = setWpsDeviceName(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status2.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 8:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    byte[] type = new byte[8];
                    _hidl_request.readBuffer(8).copyToInt8Array(0, type, 8);
                    SupplicantStatus _hidl_out_status3 = setWpsDeviceType(type);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status3.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 9:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status4 = setWpsManufacturer(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status4.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 10:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status5 = setWpsModelName(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status5.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 11:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status6 = setWpsModelNumber(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status6.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 12:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status7 = setWpsSerialNumber(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status7.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 13:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status8 = setWpsConfigMethods(_hidl_request.readInt16());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status8.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 14:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status9 = registerCallback(ISupplicantStaIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status9.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 15:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status10 = reassociate();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status10.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 16:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status11 = reconnect();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status11.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 17:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status12 = disconnect();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status12.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 18:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status13 = setPowerSave(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status13.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 19:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] macAddress = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, macAddress, 6);
                    SupplicantStatus _hidl_out_status14 = initiateTdlsDiscover(macAddress);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status14.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 20:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] macAddress2 = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, macAddress2, 6);
                    SupplicantStatus _hidl_out_status15 = initiateTdlsSetup(macAddress2);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status15.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] macAddress3 = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, macAddress3, 6);
                    SupplicantStatus _hidl_out_status16 = initiateTdlsTeardown(macAddress3);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status16.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 22:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] macAddress4 = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, macAddress4, 6);
                    SupplicantStatus _hidl_out_status17 = initiateAnqpQuery(macAddress4, _hidl_request.readInt16Vector(), _hidl_request.readInt32Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status17.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 23:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] macAddress5 = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, macAddress5, 6);
                    SupplicantStatus _hidl_out_status18 = initiateHs20IconQuery(macAddress5, _hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status18.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 24:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    getMacAddress(new getMacAddressCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass6 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getMacAddressCallback
                        public void onValues(SupplicantStatus status, byte[] macAddr) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            HwBlob _hidl_blob = new HwBlob(6);
                            if (macAddr == null || macAddr.length != 6) {
                                throw new IllegalArgumentException("Array element is not of the expected length");
                            }
                            _hidl_blob.putInt8Array(0, macAddr);
                            _hidl_reply.writeBuffer(_hidl_blob);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 25:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status19 = startRxFilter();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status19.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status20 = stopRxFilter();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status20.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 27:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status21 = addRxFilter(_hidl_request.readInt8());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status21.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 28:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status22 = removeRxFilter(_hidl_request.readInt8());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status22.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 29:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status23 = setBtCoexistenceMode(_hidl_request.readInt8());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status23.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 30:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status24 = setBtCoexistenceScanModeEnabled(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status24.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 31:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status25 = setSuspendModeEnabled(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status25.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 32:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] code = new byte[2];
                    _hidl_request.readBuffer(2).copyToInt8Array(0, code, 2);
                    SupplicantStatus _hidl_out_status26 = setCountryCode(code);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status26.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 33:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] bssid = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, bssid, 6);
                    SupplicantStatus _hidl_out_status27 = startWpsRegistrar(bssid, _hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status27.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 34:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] bssid2 = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, bssid2, 6);
                    SupplicantStatus _hidl_out_status28 = startWpsPbc(bssid2);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status28.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 35:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status29 = startWpsPinKeypad(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status29.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 36:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    byte[] bssid3 = new byte[6];
                    _hidl_request.readBuffer(6).copyToInt8Array(0, bssid3, 6);
                    startWpsPinDisplay(bssid3, new startWpsPinDisplayCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass7 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.startWpsPinDisplayCallback
                        public void onValues(SupplicantStatus status, String generatedPin) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(generatedPin);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 37:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status30 = cancelWps();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status30.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 38:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status31 = setExternalSim(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status31.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 39:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    addExtRadioWork(_hidl_request.readString(), _hidl_request.readInt32(), _hidl_request.readInt32(), new addExtRadioWorkCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass8 */

                        @Override // android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.addExtRadioWorkCallback
                        public void onValues(SupplicantStatus status, int id) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(id);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 40:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status32 = removeExtRadioWork(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status32.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ISupplicantStaIfaceCallback.StatusCode.GROUP_CIPHER_NOT_VALID:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status33 = enableAutoReconnect(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status33.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 42:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status34 = registerCallback_1_1(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status34.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 43:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status35 = registerCallback_1_2(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status35.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case ISupplicantStaIfaceCallback.StatusCode.UNSUPPORTED_RSN_IE_VERSION:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    getKeyMgmtCapabilities(new getKeyMgmtCapabilitiesCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass9 */

                        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.getKeyMgmtCapabilitiesCallback
                        public void onValues(SupplicantStatus status, int keyMgmtMask) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(keyMgmtMask);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 45:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    addDppPeerUri(_hidl_request.readString(), new addDppPeerUriCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass10 */

                        @Override // android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.addDppPeerUriCallback
                        public void onValues(SupplicantStatus status, int id) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(id);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 46:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status36 = removeDppUri(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status36.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 47:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status37 = startDppConfiguratorInitiator(_hidl_request.readInt32(), _hidl_request.readInt32(), _hidl_request.readString(), _hidl_request.readString(), _hidl_request.readString(), _hidl_request.readInt32(), _hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status37.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 48:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status38 = startDppEnrolleeInitiator(_hidl_request.readInt32(), _hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status38.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 49:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status39 = stopDppInitiator();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status39.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 50:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status40 = hwStaRegisterCallback(ISupplicantStaIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status40.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 51:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status41 = setFilterEnable(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status41.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 52:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    heartBeat(_hidl_request.readString(), new heartBeatCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass11 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.heartBeatCallback
                        public void onValues(SupplicantStatus status, String reply) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 53:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status42 = wnmBssQurey(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status42.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 54:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    getWpasConfig(_hidl_request.readInt32(), new getWpasConfigCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass12 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.getWpasConfigCallback
                        public void onValues(SupplicantStatus status, String reply) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 55:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status43 = enableHiLinkHandshake(_hidl_request.readBool(), _hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status43.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 56:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status44 = SetAbsCapability(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status44.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 57:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status45 = SetAbsBlacklist(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status45.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 58:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status46 = AbsPowerCtrl(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status46.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 59:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    VowifiDetect(_hidl_request.readString(), new VowifiDetectCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass13 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.VowifiDetectCallback
                        public void onValues(SupplicantStatus status, String reply) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 60:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    getCapabRsdb(new getCapabRsdbCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass14 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.getCapabRsdbCallback
                        public void onValues(SupplicantStatus status, String reply) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 61:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status47 = setTxPower(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status47.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 62:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status48 = pwrPercentBoostModeset(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status48.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 63:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    getMssState(new getMssStateCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass15 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.getMssStateCallback
                        public void onValues(SupplicantStatus status, String reply) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 64:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    getApVendorInfo(new getApVendorInfoCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass16 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.getApVendorInfoCallback
                        public void onValues(SupplicantStatus status, String reply) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 65:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISupplicantStaIface.kInterfaceName);
                    deliverStaIfaceData(_hidl_request.readInt32(), _hidl_request.readInt32(), _hidl_request.readString(), new deliverStaIfaceDataCallback() {
                        /* class vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.Stub.AnonymousClass17 */

                        @Override // vendor.huawei.hardware.wifi.supplicant.V3_0.ISupplicantStaIface.deliverStaIfaceDataCallback
                        public void onValues(SupplicantStatus status, String data) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(data);
                            _hidl_reply.send();
                        }
                    });
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList<String> _hidl_out_descriptors = interfaceChain();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeStringVector(_hidl_out_descriptors);
                            _hidl_reply.send();
                            return;
                        case 256131655:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            debug(_hidl_request.readNativeHandle(), _hidl_request.readStringVector());
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 256136003:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            String _hidl_out_descriptor = interfaceDescriptor();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeString(_hidl_out_descriptor);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            _hidl_reply.writeStatus(0);
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                                long _hidl_array_offset_1 = (long) (_hidl_index_0 * 32);
                                byte[] _hidl_array_item_1 = _hidl_out_hashchain.get(_hidl_index_0);
                                if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                                    throw new IllegalArgumentException("Array element is not of the expected length");
                                }
                                childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                            }
                            _hidl_blob.putBlob(0, childBlob);
                            _hidl_reply.writeBuffer(_hidl_blob);
                            _hidl_reply.send();
                            return;
                        case 256462420:
                            if (_hidl_flags != false && true) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            setHALInstrumentation();
                            return;
                        case 256660548:
                            if (_hidl_flags != false && true) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        case 256921159:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ping();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 257049926:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            DebugInfo _hidl_out_info = getDebugInfo();
                            _hidl_reply.writeStatus(0);
                            _hidl_out_info.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                            return;
                        case 257120595:
                            if (_hidl_flags != false && true) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            notifySyspropsChanged();
                            return;
                        case 257250372:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        default:
                            return;
                    }
            }
        }
    }
}
