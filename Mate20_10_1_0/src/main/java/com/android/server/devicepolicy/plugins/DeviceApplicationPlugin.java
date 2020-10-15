package com.android.server.devicepolicy.plugins;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwDevicePolicyManagerServiceUtil;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import java.util.ArrayList;

public class DeviceApplicationPlugin extends DevicePolicyPlugin {
    private static final String INSTALL_APKS_BLACK_LIST = "install-packages-black-list";
    private static final String INSTALL_APKS_BLACK_LIST_ITEM = "install-packages-black-list/install-packages-black-list-item";
    public static final String TAG = DeviceApplicationPlugin.class.getSimpleName();

    public DeviceApplicationPlugin(Context context) {
        super(context);
    }

    public boolean onInit(final PolicyStruct struct) {
        HwLog.i(TAG, "onInit");
        if (struct == null || !struct.containsPolicyName("policy-single-app")) {
            return true;
        }
        IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.devicepolicy.plugins.DeviceApplicationPlugin.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                Intent launchIntent;
                DeviceApplicationPlugin.this.mContext.unregisterReceiver(this);
                String packageName = struct.getPolicyItem("policy-single-app").getAttrValue("value");
                if (packageName != null && !packageName.isEmpty() && (launchIntent = DeviceApplicationPlugin.this.mContext.getPackageManager().getLaunchIntentForPackage(packageName)) != null) {
                    DeviceApplicationPlugin.this.mContext.startActivity(launchIntent);
                }
            }
        }, filter);
        return true;
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(INSTALL_APKS_BLACK_LIST_ITEM, PolicyStruct.PolicyType.LIST, new String[]{"value"});
        struct.addStruct("ignore-frequent-relaunch-app", PolicyStruct.PolicyType.LIST, new String[0]);
        struct.addStruct("ignore-frequent-relaunch-app/ignore-frequent-relaunch-app-item", PolicyStruct.PolicyType.LIST, new String[]{"value"});
        struct.addStruct("policy-single-app", PolicyStruct.PolicyType.CONFIGURATION, new String[]{"value"});
        return struct;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        HwLog.i(TAG, "checkCallingPermission");
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        HwLog.i(TAG, "onSetPolicy");
        if (policyData == null) {
            HwLog.i(TAG, "onSetPolicy policyData is null");
            return false;
        }
        char c = 65535;
        if (policyName.hashCode() == -414055785 && policyName.equals("policy-single-app")) {
            c = 0;
        }
        if (c == 0) {
            HwLog.i(TAG, "onSetPolicy POLICY_SINGLE_APP");
            Intent launchIntent = this.mContext.getPackageManager().getLaunchIntentForPackage(policyData.getString("value"));
            if (launchIntent == null) {
                return false;
            }
            this.mContext.startActivity(launchIntent);
            return true;
        } else if (isNotEffective(who, policyName, policyData, isEffective)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        HwLog.i(TAG, "onRemovePolicy");
        if (isNotEffective(who, policyName, policyData, isEffective)) {
            return false;
        }
        return true;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        HwLog.i(TAG, "onGetPolicy");
        if (isNotEffective(who, policyName, policyData, true)) {
            return false;
        }
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyStruct.PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        return true;
    }

    public boolean isNotEffective(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        if (policyData != null && ("install-packages-black-list".equals(policyName) || "ignore-frequent-relaunch-app".equals(policyName))) {
            ArrayList<String> packageNames = policyData.getStringArrayList("value");
            if (!HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
                throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
            }
        }
        return false;
    }
}
