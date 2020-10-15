package com.huawei.server.security.behaviorcollect;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.security.core.IHwSecurityPlugin;
import com.huawei.security.behaviorauth.IBehaviorCollectService;

public class HwBehaviorCollectPlugin extends IBehaviorCollectService.Stub implements IHwSecurityPlugin {
    public static final IHwSecurityPlugin.Creator CREATOR = new IHwSecurityPlugin.Creator() {
        /* class com.huawei.server.security.behaviorcollect.HwBehaviorCollectPlugin.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            Log.d(HwBehaviorCollectPlugin.TAG, "create BehaviorCollectService");
            return new HwBehaviorCollectPlugin(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return HwBehaviorCollectPlugin.PERMISSION_BEHAVIOR_COLLECT;
        }
    };
    private static final String PERMISSION_BEHAVIOR_COLLECT = "com.huawei.permission.BEHAVIOR_COLLECT";
    /* access modifiers changed from: private */
    public static final String TAG = HwBehaviorCollectPlugin.class.getSimpleName();
    private Context behaviorContext;

    public HwBehaviorCollectPlugin(Context context) {
        this.behaviorContext = context;
    }

    public int initBotDetect(String pkgName) throws RemoteException {
        Log.d(TAG, "begin init the bot collect service");
        checkPermission(PERMISSION_BEHAVIOR_COLLECT);
        return BehaviorCollector.getInstance().addPackage(pkgName);
    }

    public int releaseBotDetect(String pkgName) throws RemoteException {
        Log.d(TAG, "begin release the bot collect service");
        checkPermission(PERMISSION_BEHAVIOR_COLLECT);
        return BehaviorCollector.getInstance().removePackage(pkgName);
    }

    public float getBotDetectResult(String pkgName) throws RemoteException {
        Log.d(TAG, "begin get the botdetect result");
        checkPermission(PERMISSION_BEHAVIOR_COLLECT);
        return BehaviorCollector.getInstance().getBotResultFromModel(pkgName);
    }

    private void checkPermission(String permission) {
        Context context = this.behaviorContext;
        context.enforceCallingPermission(permission, "Must have " + permission + " permission");
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
    }
}
