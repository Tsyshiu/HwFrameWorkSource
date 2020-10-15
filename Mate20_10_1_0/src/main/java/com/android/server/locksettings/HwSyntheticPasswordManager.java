package com.android.server.locksettings;

import android.content.Context;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.gatekeeper.IGateKeeperService;
import android.util.Slog;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.locksettings.HwSyntheticPasswordManager;
import com.android.server.locksettings.SyntheticPasswordManager;
import vendor.huawei.hardware.weaver.V1_0.IWeaver;
import vendor.huawei.hardware.weaver.V1_0.WeaverSlotStatus;

public class HwSyntheticPasswordManager extends SyntheticPasswordManager {
    private static final int INVALID_WEAVER_SLOT = -1;
    public static final int PWD_BACKEND_HARDWARE_ERR = 1;
    public static final int PWD_BACKEND_OTHER_ERR = 2;
    public static final int PWD_BACKEND_STATUS_NOT_SEC_CHIP = 10;
    public static final int PWD_BACKEND_STATUS_OK = 0;
    private static final String STRONG_AUTH_SOLUTION_FLAG = "strong_auth_solution_flag";
    private static final String STRONG_AUTH_SOLUTION_WEAVER_FLAG = "strong_auth_solution_weaver_flag";
    private static final String TAG = "HwLSS-SPM";
    private IHwBinder.DeathRecipient mWeaverDeathRecipient = new IHwBinder.DeathRecipient() {
        /* class com.android.server.locksettings.HwSyntheticPasswordManager.AnonymousClass1 */

        public void serviceDied(long cookie) {
            Slog.e(HwSyntheticPasswordManager.TAG, "weaver service is died, try to reconnect it later.");
            HwSyntheticPasswordManager.this.mWeaver = null;
        }
    };

    public HwSyntheticPasswordManager(Context context, LockSettingsStorage storage, UserManager userManager, PasswordSlotManager slotManager) {
        super(context, storage, userManager, slotManager);
    }

    /* access modifiers changed from: protected */
    public void destroySPBlobKey(String keyAlias) {
        HwSyntheticPasswordManager.super.destroySPBlobKey(keyAlias);
        flog(TAG, "destroySPBlobKey " + keyAlias);
    }

    /* access modifiers changed from: protected */
    public void saveState(String stateName, byte[] data, long handle, int userId) {
        HwSyntheticPasswordManager.super.saveState(stateName, data, handle, userId);
        flog(TAG, "saveState U" + userId + "  " + stateName + " " + Long.toHexString(handle));
    }

    public VerifyCredentialResponse verifyChallenge(IGateKeeperService gatekeeper, AuthenticationToken auth, long challenge, int userId) throws RemoteException {
        VerifyCredentialResponse r = HwSyntheticPasswordManager.super.verifyChallenge(gatekeeper, auth, challenge, userId);
        StringBuilder sb = new StringBuilder();
        sb.append("verifyChallenge U");
        sb.append(userId);
        sb.append(" R:");
        sb.append(r == null ? "NUL" : Integer.toString(r.getResponseCode()));
        flog(TAG, sb.toString());
        return r;
    }

    /* access modifiers changed from: protected */
    public void destroyState(String stateName, long handle, int userId) {
        HwSyntheticPasswordManager.super.destroyState(stateName, handle, userId);
        flog(TAG, "destroyState U" + userId + "  " + stateName + " " + Long.toHexString(handle));
    }

    /* access modifiers changed from: protected */
    public void updateWeaverTypeFlag(int flag, int userId) {
        String newFlag = Long.toString((long) flag);
        String storedFlag = this.mStorage.readKeyValue(STRONG_AUTH_SOLUTION_WEAVER_FLAG, "0", userId);
        Slog.i(TAG, "updateWeaverTypeFlag newFlag " + newFlag + " storedFlag " + storedFlag);
        if (!newFlag.equalsIgnoreCase(storedFlag)) {
            this.mStorage.writeKeyValue(STRONG_AUTH_SOLUTION_WEAVER_FLAG, newFlag, userId);
            flog(TAG, "updateWeaverTypeFlag U" + userId + "  newFlag " + newFlag);
        }
    }

    /* access modifiers changed from: protected */
    public void updateCredentialTypeFlag(long flag, int userId) {
        String newFlag = Long.toString(flag);
        String storedFlag = this.mStorage.readKeyValue(STRONG_AUTH_SOLUTION_FLAG, "0", userId);
        Slog.i(TAG, "updateCredentialTypeFlag newFlag " + newFlag + " storedFlag " + storedFlag);
        if (!newFlag.equalsIgnoreCase(storedFlag)) {
            this.mStorage.writeKeyValue(STRONG_AUTH_SOLUTION_FLAG, newFlag, userId);
            flog(TAG, "updateCredentialTypeFlag U" + userId + "  newFlag " + newFlag);
        }
    }

    /* access modifiers changed from: package-private */
    public static class WeaverLockedStatus {
        public boolean isSuccess;
        public int remainCount;
        public long timeout;

        WeaverLockedStatus() {
        }
    }

    public WeaverLockedStatus getWeaverLockedStatus(long handle, int userId) throws RemoteException {
        WeaverLockedStatus lockedStatus = new WeaverLockedStatus();
        lockedStatus.isSuccess = false;
        int slotId = loadWeaverSlot(handle, userId);
        if (slotId == -1) {
            return lockedStatus;
        }
        this.mWeaver.getStatus(slotId, new IWeaver.getStatusCallback(slotId, lockedStatus) {
            /* class com.android.server.locksettings.$$Lambda$HwSyntheticPasswordManager$PNwQRkpjfGduP77Mr1ZxE8cAlpo */
            private final /* synthetic */ int f$0;
            private final /* synthetic */ WeaverLockedStatus f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void onValues(int i, WeaverSlotStatus weaverSlotStatus) {
                HwSyntheticPasswordManager.lambda$getWeaverLockedStatus$0(this.f$0, this.f$1, i, weaverSlotStatus);
            }
        });
        return lockedStatus;
    }

    static /* synthetic */ void lambda$getWeaverLockedStatus$0(int slotId, WeaverLockedStatus lockedStatus, int status, WeaverSlotStatus statusResult) {
        Slog.i(TAG, "weaver get status " + status + ", slot: " + slotId + " " + statusResult.timeout);
        if (status == 0) {
            lockedStatus.isSuccess = true;
            lockedStatus.timeout = (long) statusResult.timeout;
            lockedStatus.remainCount = statusResult.count;
        }
    }

    public int getPasswordBackendStatus() {
        int status = 10;
        if (this.mWeaver == null) {
            return 10;
        }
        try {
            int statusResult = this.mWeaver.getWeaverHardErr();
            if (statusResult == 1) {
                status = 1;
            } else if (statusResult == 2) {
                status = 2;
            } else {
                status = 0;
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "weaver get pwd backend exception.");
        }
        Slog.i(TAG, "weaver get pwd backend status = " + status);
        return status;
    }

    /* access modifiers changed from: protected */
    public void weaverLinkToDeath() {
        if (this.mWeaver != null) {
            try {
                this.mWeaver.linkToDeath(this.mWeaverDeathRecipient, 0);
                Slog.i(TAG, "mWeaver linkToDeath success");
            } catch (RemoteException e) {
                Slog.e(TAG, "mWeaver linkToDeath RemoteException error");
            } catch (Exception e2) {
                Slog.e(TAG, "mWeaver linkToDeath error");
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void errorLog(String tag, String msg) {
        Slog.e(tag, msg);
        this.mStorage.flog(tag, msg);
    }

    /* access modifiers changed from: package-private */
    public void warnLog(String tag, String msg) {
        Slog.w(tag, msg);
        this.mStorage.flog(tag, msg);
    }

    /* access modifiers changed from: package-private */
    public void flog(String tag, String msg) {
        this.mStorage.flog(tag, msg);
    }
}
