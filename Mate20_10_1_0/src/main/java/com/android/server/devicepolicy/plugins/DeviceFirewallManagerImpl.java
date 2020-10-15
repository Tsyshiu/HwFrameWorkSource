package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import com.android.server.HwConnectivityManagerImpl;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import java.util.ArrayList;

public class DeviceFirewallManagerImpl extends DevicePolicyPlugin {
    private static final String GLOBAL_PROXY = "global-proxy";
    private static final String MDM_FIREWALL_PERMISSION = "com.huawei.permission.sec.MDM_FIREWALL";
    private static final String PAC = "PAC";
    public static final String TAG = "DeviceFirewallManagerImpl";

    public DeviceFirewallManagerImpl(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getFirewallPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(GLOBAL_PROXY, PolicyStruct.PolicyType.CONFIGURATION, new String[]{HwConnectivityManagerImpl.SET_IP_TABLES_KEY_HOST_NAME, "proxyPort", "exclusionList"});
        struct.addStruct(PAC, PolicyStruct.PolicyType.CONFIGURATION, new String[]{"pacFileUrl"});
        return struct;
    }

    public boolean onInit(PolicyStruct policyStruct) {
        HwLog.i(TAG, "onInit");
        if (policyStruct == null) {
            return false;
        }
        return true;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        HwLog.i(TAG, "checkCallingPermission");
        this.mContext.enforceCallingOrSelfPermission(MDM_FIREWALL_PERMISSION, "NEED MDM_FIREWALL PERMISSION");
        return true;
    }

    /* JADX INFO: finally extract failed */
    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        if (policyData == null) {
            HwLog.i(TAG, "onSetPolicy policyData is null");
            return false;
        }
        HwLog.i(TAG, "onSetPolicy");
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        char c = 65535;
        int hashCode = policyName.hashCode();
        if (hashCode != -1751360572) {
            if (hashCode == 78962 && policyName.equals(PAC)) {
                c = 1;
            }
        } else if (policyName.equals(GLOBAL_PROXY)) {
            c = 0;
        }
        if (c == 0) {
            String hostName = policyData.getString(HwConnectivityManagerImpl.SET_IP_TABLES_KEY_HOST_NAME);
            int proxyPort = 0;
            if (!(hostName == null || policyData.getString("proxyPort") == null)) {
                try {
                    proxyPort = Integer.parseInt(policyData.getString("proxyPort"));
                } catch (NumberFormatException e) {
                    HwLog.e(TAG, "proxyPort : NumberFormatException");
                }
            }
            ProxyInfo proxyInfo = new ProxyInfo(hostName, proxyPort, policyData.getString("exclusionList"));
            long ident = Binder.clearCallingIdentity();
            try {
                connectivityManager.setGlobalProxy(proxyInfo);
                Binder.restoreCallingIdentity(ident);
                if (connectivityManager.getGlobalProxy() == null && hostName == null) {
                    HwLog.i(TAG, "Set null proxy.");
                    return true;
                } else if (!proxyInfo.equals(connectivityManager.getGlobalProxy())) {
                    HwLog.i(TAG, "Invalid or repeated proxy properties, ignoring.");
                    return false;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else if (c != 1) {
            return true;
        } else {
            HwLog.i(TAG, "Set pac start.");
            String pacFileUrl = policyData.getString("pacFileUrl");
            ProxyInfo proxyInfo2 = new ProxyInfo(pacFileUrl);
            long ident2 = Binder.clearCallingIdentity();
            try {
                if (TextUtils.isEmpty(pacFileUrl)) {
                    connectivityManager.setGlobalProxy(null);
                } else {
                    connectivityManager.setGlobalProxy(proxyInfo2);
                }
                Binder.restoreCallingIdentity(ident2);
                if (connectivityManager.getGlobalProxy() == null && TextUtils.isEmpty(pacFileUrl)) {
                    HwLog.i(TAG, "Set null proxy.");
                    return true;
                } else if (!proxyInfo2.equals(connectivityManager.getGlobalProxy())) {
                    HwLog.i(TAG, "Invalid or repeated proxy properties, ignoring.");
                    return false;
                }
            } catch (Throwable th2) {
                Binder.restoreCallingIdentity(ident2);
                throw th2;
            }
        }
        return true;
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        HwLog.i(TAG, "onRemovePolicy");
        return true;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        HwLog.i(TAG, "onGetPolicy");
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyStruct.PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        return true;
    }

    public void onActiveAdminRemovedCompleted(ComponentName who, ArrayList<PolicyStruct.PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemovedCompleted");
        HwDevicePolicyManagerEx mDpm = new HwDevicePolicyManagerEx();
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        Bundle bundle = mDpm.getPolicy((ComponentName) null, GLOBAL_PROXY);
        if (bundle == null || bundle.getString(HwConnectivityManagerImpl.SET_IP_TABLES_KEY_HOST_NAME) == null || bundle.getString("proxyPort") == null) {
            Bundle bundlePac = mDpm.getPolicy((ComponentName) null, PAC);
            if (bundlePac == null || bundlePac.getString("pacFileUrl") == null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    connectivityManager.setGlobalProxy(null);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                ProxyInfo proxyInfo = new ProxyInfo(bundlePac.getString("pacFileUrl"));
                long ident2 = Binder.clearCallingIdentity();
                try {
                    connectivityManager.setGlobalProxy(proxyInfo);
                } finally {
                    Binder.restoreCallingIdentity(ident2);
                }
            }
        } else {
            try {
                ProxyInfo proxyInfo2 = new ProxyInfo(bundle.getString(HwConnectivityManagerImpl.SET_IP_TABLES_KEY_HOST_NAME), Integer.parseInt(bundle.getString("proxyPort")), bundle.getString("exclusionList"));
                long ident3 = Binder.clearCallingIdentity();
                try {
                    connectivityManager.setGlobalProxy(proxyInfo2);
                } finally {
                    Binder.restoreCallingIdentity(ident3);
                }
            } catch (NumberFormatException e) {
                HwLog.e(TAG, "proxyPort : NumberFormatException");
            }
        }
    }
}
