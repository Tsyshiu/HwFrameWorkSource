package com.android.location.provider;

import android.hardware.location.IActivityRecognitionHardware;
import android.hardware.location.IActivityRecognitionHardwareWatcher;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

@Deprecated
public class ActivityRecognitionProviderWatcher {
    private static final String TAG = "ActivityRecognitionProviderWatcher";
    private static ActivityRecognitionProviderWatcher sWatcher;
    private static final Object sWatcherLock = new Object();
    /* access modifiers changed from: private */
    public ActivityRecognitionProvider mActivityRecognitionProvider;
    private IActivityRecognitionHardwareWatcher.Stub mWatcherStub = new IActivityRecognitionHardwareWatcher.Stub() {
        /* class com.android.location.provider.ActivityRecognitionProviderWatcher.AnonymousClass1 */

        public void onInstanceChanged(IActivityRecognitionHardware instance) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000) {
                Log.d(ActivityRecognitionProviderWatcher.TAG, "Ignoring calls from non-system server. Uid: " + callingUid);
                return;
            }
            try {
                ActivityRecognitionProvider unused = ActivityRecognitionProviderWatcher.this.mActivityRecognitionProvider = new ActivityRecognitionProvider(instance);
            } catch (RemoteException e) {
                Log.e(ActivityRecognitionProviderWatcher.TAG, "Error creating Hardware Activity-Recognition", e);
            }
        }
    };

    private ActivityRecognitionProviderWatcher() {
    }

    public static ActivityRecognitionProviderWatcher getInstance() {
        ActivityRecognitionProviderWatcher activityRecognitionProviderWatcher;
        synchronized (sWatcherLock) {
            if (sWatcher == null) {
                sWatcher = new ActivityRecognitionProviderWatcher();
            }
            activityRecognitionProviderWatcher = sWatcher;
        }
        return activityRecognitionProviderWatcher;
    }

    public IBinder getBinder() {
        return this.mWatcherStub;
    }

    public ActivityRecognitionProvider getActivityRecognitionProvider() {
        return this.mActivityRecognitionProvider;
    }
}
