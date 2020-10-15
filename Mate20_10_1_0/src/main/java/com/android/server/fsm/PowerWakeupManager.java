package com.android.server.fsm;

import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.LocalServices;

public final class PowerWakeupManager extends WakeupManager {
    private static final String TAG = "Fsm_PowerWakeupManager";
    private PowerManagerInternal mPowerManagerInternal;

    @Override // com.android.server.fsm.WakeupManager
    public void wakeup() {
        Slog.d("Fsm_PowerWakeupManager", "Wakeup in PowerWakeupManager");
        if (this.mPowerManagerInternal == null) {
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }
        this.mPowerManagerInternal.powerWakeup(SystemClock.uptimeMillis(), this.mReason, this.mDetails, this.mUid, this.mOpPackageName, this.mUid);
    }
}
