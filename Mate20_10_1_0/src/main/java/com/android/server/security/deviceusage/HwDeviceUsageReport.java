package com.android.server.security.deviceusage;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.security.IHwSecurityDiagnosePlugin;
import huawei.android.security.IHwSecurityService;
import java.util.Date;

public class HwDeviceUsageReport {
    private static final int DEVICE_SECURE_DIAGNOSE_ID = 2;
    private static final int ISBN_ID = 0;
    private static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String TAG = "HwDeviceUsageReport";
    private Bundle mDeviceUseData = new Bundle();
    private IHwSecurityDiagnosePlugin mHwSecurityDiagnosePlugin;

    public HwDeviceUsageReport(Context context) {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "HwDeviceUsageReport create");
        }
        IBinder binder = ServiceManager.getService("securityserver");
        if (binder != null) {
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "getHwSecurityService");
            }
            try {
                IBinder diagnoseServiceBinder = IHwSecurityService.Stub.asInterface(binder).querySecurityInterface(2);
                if (diagnoseServiceBinder != null) {
                    this.mHwSecurityDiagnosePlugin = IHwSecurityDiagnosePlugin.Stub.asInterface(diagnoseServiceBinder);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Error in get Diagnose Service ");
            }
        } else {
            Slog.e(TAG, "getHwSecurityService binder is null");
        }
    }

    public void reportFirstUseTime(long time) {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "getFirstUseTime");
        }
        this.mDeviceUseData.putString(HwSecDiagnoseConstant.DEVICE_RENEW_TIME, new Date(time).toString());
        this.mDeviceUseData.putString(HwSecDiagnoseConstant.DEVICE_RENEW_SN_CODE, getIsbn());
        IHwSecurityDiagnosePlugin iHwSecurityDiagnosePlugin = this.mHwSecurityDiagnosePlugin;
        if (iHwSecurityDiagnosePlugin == null) {
            Slog.e(TAG, "getFirstUseTime mHwSecurityDiagnosePlugin is null");
            return;
        }
        try {
            iHwSecurityDiagnosePlugin.report(101, this.mDeviceUseData);
        } catch (RemoteException e) {
            Slog.e(TAG, "reportFirstUseTime EXCEPTION");
        }
    }

    private String getIsbn() {
        return HwOEMInfoAdapter.getIsbnOrSn(0);
    }
}
