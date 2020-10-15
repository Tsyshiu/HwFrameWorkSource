package com.android.server.am;

import android.app.AppGlobals;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.appwidget.AppWidgetManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.encrypt.ISDCardCryptedHelper;
import android.hwtheme.HwThemeManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IProgressListener;
import android.os.IRemoteCallback;
import android.os.IUserManager;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Flog;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimingsTraceLog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.FgThread;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.am.UserController;
import com.android.server.am.UserState;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.devicepolicy.HwLog;
import com.android.server.pm.UserManagerService;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerService;
import huawei.cust.HwCustUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class UserController implements Handler.Callback {
    static final int CONTINUE_USER_SWITCH_MSG = 20;
    static final int FOREGROUND_PROFILE_CHANGED_MSG = 70;
    static final int REPORT_LOCKED_BOOT_COMPLETE_MSG = 110;
    static final int REPORT_USER_SWITCH_COMPLETE_MSG = 80;
    static final int REPORT_USER_SWITCH_MSG = 10;
    private static final int SCREEN_STATE_FLAG_PASSWORD = 2;
    static final int START_PROFILES_MSG = 40;
    static final int START_USER_SWITCH_FG_MSG = 120;
    static final int START_USER_SWITCH_UI_MSG = 1000;
    static final int SYSTEM_USER_CURRENT_MSG = 60;
    static final int SYSTEM_USER_START_MSG = 50;
    static final int SYSTEM_USER_UNLOCK_MSG = 100;
    private static final String TAG = "ActivityManager";
    private static final int UNLOCK_TYPE_VALUE = 1;
    private static final int USER_SWITCH_CALLBACKS_TIMEOUT_MS = 5000;
    static final int USER_SWITCH_CALLBACKS_TIMEOUT_MSG = 90;
    static final int USER_SWITCH_TIMEOUT_MS = 3000;
    static final int USER_SWITCH_TIMEOUT_MSG = 30;
    long SwitchUser_Time;
    boolean isColdStart;
    volatile boolean mBootCompleted;
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public volatile ArraySet<String> mCurWaitingUserSwitchCallbacks;
    @GuardedBy({"mLock"})
    private int[] mCurrentProfileIds;
    @GuardedBy({"mLock"})
    private volatile int mCurrentUserId;
    private HwCustUserController mCust;
    boolean mDelayUserDataLocking;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    boolean mHaveTryCloneProUserUnlock;
    final Injector mInjector;
    private boolean mIsSupportISec;
    @GuardedBy({"mLock"})
    private final ArrayList<Integer> mLastActiveUsers;
    /* access modifiers changed from: private */
    public final Object mLock;
    private final LockPatternUtils mLockPatternUtils;
    int mMaxRunningUsers;
    @GuardedBy({"mLock"})
    private int[] mStartedUserArray;
    @GuardedBy({"mLock"})
    private final SparseArray<UserState> mStartedUsers;
    @GuardedBy({"mLock"})
    private String mSwitchingFromSystemUserMessage;
    @GuardedBy({"mLock"})
    private String mSwitchingToSystemUserMessage;
    @GuardedBy({"mLock"})
    private volatile int mTargetUserId;
    @GuardedBy({"mLock"})
    private ArraySet<String> mTimeoutUserSwitchCallbacks;
    private final Handler mUiHandler;
    @GuardedBy({"mLock"})
    private final ArrayList<Integer> mUserLru;
    @GuardedBy({"mLock"})
    private final SparseIntArray mUserProfileGroupIds;
    private final RemoteCallbackList<IUserSwitchObserver> mUserSwitchObservers;
    boolean mUserSwitchUiEnabled;
    boolean misHiddenSpaceSwitch;

    UserController(ActivityManagerService service) {
        this(new Injector(service));
    }

    @VisibleForTesting
    UserController(Injector injector) {
        this.isColdStart = false;
        this.misHiddenSpaceSwitch = false;
        this.mHaveTryCloneProUserUnlock = false;
        this.mLock = new Object();
        this.mIsSupportISec = SystemProperties.getBoolean("ro.config.support_iudf", false);
        this.mCurrentUserId = 0;
        this.mTargetUserId = -10000;
        this.mStartedUsers = new SparseArray<>();
        this.mUserLru = new ArrayList<>();
        this.mStartedUserArray = new int[]{0};
        this.mCurrentProfileIds = new int[0];
        this.mUserProfileGroupIds = new SparseIntArray();
        this.mUserSwitchObservers = new RemoteCallbackList<>();
        this.mUserSwitchUiEnabled = true;
        this.mLastActiveUsers = new ArrayList<>();
        this.mCust = (HwCustUserController) HwCustUtils.createObj(HwCustUserController.class, new Object[0]);
        this.mInjector = injector;
        this.mHandler = this.mInjector.getHandler(this);
        this.mUiHandler = this.mInjector.getUiHandler(this);
        UserState uss = new UserState(UserHandle.SYSTEM);
        uss.mUnlockProgress.addListener(new UserProgressListener());
        this.mStartedUsers.put(0, uss);
        this.mUserLru.add(0);
        this.mLockPatternUtils = this.mInjector.getLockPatternUtils();
        updateStartedUserArrayLU();
    }

    /* access modifiers changed from: package-private */
    public void finishUserSwitch(UserState uss) {
        this.mHandler.post(new Runnable(uss) {
            /* class com.android.server.am.$$Lambda$UserController$f2F3ceAG58MOmBJm9cmZ7HhYcmE */
            private final /* synthetic */ UserState f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                UserController.this.lambda$finishUserSwitch$0$UserController(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$finishUserSwitch$0$UserController(UserState uss) {
        finishUserBoot(uss);
        startProfiles();
        synchronized (this.mLock) {
            stopRunningUsersLU(this.mMaxRunningUsers);
        }
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mLock"})
    public List<Integer> getRunningUsersLU() {
        ArrayList<Integer> runningUsers = new ArrayList<>();
        Iterator<Integer> it = this.mUserLru.iterator();
        while (it.hasNext()) {
            Integer userId = it.next();
            UserState uss = this.mStartedUsers.get(userId.intValue());
            if (!(uss == null || uss.state == 4 || uss.state == 5)) {
                if (userId.intValue() != 0 || !UserInfo.isSystemOnly(userId.intValue())) {
                    runningUsers.add(userId);
                }
            }
        }
        return runningUsers;
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mLock"})
    public void stopRunningUsersLU(int maxRunningUsers) {
        List<Integer> currentlyRunning = getRunningUsersLU();
        Iterator<Integer> iterator = currentlyRunning.iterator();
        while (currentlyRunning.size() > maxRunningUsers && iterator.hasNext()) {
            Integer userId = iterator.next();
            if (!(userId.intValue() == 0 || userId.intValue() == this.mCurrentUserId || stopUsersLU(userId.intValue(), false, null, null) != 0)) {
                iterator.remove();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean canStartMoreUsers() {
        boolean z;
        synchronized (this.mLock) {
            z = getRunningUsersLU().size() < this.mMaxRunningUsers;
        }
        return z;
    }

    private void finishUserBoot(UserState uss) {
        finishUserBoot(uss, null);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0038, code lost:
        if (r22.setState(0, 1) == false) goto L_0x00ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x003a, code lost:
        r21.mInjector.getUserManagerInternal().setUserState(r6, r22.state);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0045, code lost:
        if (r6 != 0) goto L_0x0086;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x004d, code lost:
        if (r21.mInjector.isRuntimeRestarted() != false) goto L_0x0086;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0055, code lost:
        if (r21.mInjector.isFirstBootOrUpgrade() != false) goto L_0x0086;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0057, code lost:
        r0 = (int) (android.os.SystemClock.elapsedRealtime() / 1000);
        com.android.internal.logging.MetricsLogger.histogram(r21.mInjector.getContext(), "framework_locked_boot_completed", r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x006e, code lost:
        if (r0 <= com.android.server.am.UserController.START_USER_SWITCH_FG_MSG) goto L_0x0086;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0070, code lost:
        android.util.Slog.wtf("SystemServerTiming", "finishUserBoot took too long. uptimeSeconds=" + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0086, code lost:
        r0 = r21.mHandler;
        r0.sendMessage(r0.obtainMessage(110, r6, 0));
        r0 = new android.content.Intent("android.intent.action.LOCKED_BOOT_COMPLETED", (android.net.Uri) null);
        r0.putExtra("android.intent.extra.user_handle", r6);
        r0.addFlags(150994944);
        r20 = r6;
        r21.mInjector.broadcastIntent(r0, null, r23, 0, null, null, new java.lang.String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, android.os.Binder.getCallingUid(), android.os.Binder.getCallingPid(), r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x00ca, code lost:
        r20 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x00d8, code lost:
        if (r21.mInjector.getUserManager().isManagedProfile(r20) != false) goto L_0x00eb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00e4, code lost:
        if (r21.mInjector.getUserManager().isClonedProfile(r20) == false) goto L_0x00e7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x00e7, code lost:
        maybeUnlockUser(r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00eb, code lost:
        r0 = r21.mInjector.getUserManager().getProfileParent(r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00f5, code lost:
        if (r0 == null) goto L_0x0128;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00fd, code lost:
        if (android.os.storage.StorageManager.isUserKeyUnlocked(r0.id) == false) goto L_0x0128;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00ff, code lost:
        android.util.Slog.d("ActivityManager", "User " + r20 + " (parent " + r0.id + "): attempting unlock because parent is unlocked");
        maybeUnlockUser(r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0128, code lost:
        if (r0 != null) goto L_0x012d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x012a, code lost:
        r3 = "<null>";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x012d, code lost:
        r3 = java.lang.String.valueOf(r0.id);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0133, code lost:
        android.util.Slog.d("ActivityManager", "User " + r20 + " (parent " + r3 + "): delaying unlock because parent is locked");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:?, code lost:
        return;
     */
    private void finishUserBoot(UserState uss, IIntentReceiver resultTo) {
        int userId = uss.mHandle.getIdentifier();
        Slog.d("ActivityManager", "Finishing user boot " + userId);
        synchronized (this.mLock) {
            try {
                if (this.mStartedUsers.get(userId) != uss) {
                    try {
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    private boolean finishUserUnlocking(UserState uss) {
        int userId = uss.mHandle.getIdentifier();
        if (!StorageManager.isUserKeyUnlocked(userId)) {
            return false;
        }
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(userId) == uss) {
                if (uss.state == 1) {
                    uss.mUnlockProgress.start();
                    uss.mUnlockProgress.setProgress(5, this.mInjector.getContext().getString(17039592));
                    FgThread.getHandler().post(new Runnable(userId, uss) {
                        /* class com.android.server.am.$$Lambda$UserController$stQk1028ON105v_uVMykVjcxLk */
                        private final /* synthetic */ int f$1;
                        private final /* synthetic */ UserState f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            UserController.this.lambda$finishUserUnlocking$1$UserController(this.f$1, this.f$2);
                        }
                    });
                    return true;
                }
            }
            return false;
        }
    }

    public /* synthetic */ void lambda$finishUserUnlocking$1$UserController(int userId, UserState uss) {
        if (!StorageManager.isUserKeyUnlocked(userId)) {
            Slog.w("ActivityManager", "User key got locked unexpectedly, leaving user locked.");
            return;
        }
        this.mInjector.getUserManager().onBeforeUnlockUser(userId);
        synchronized (this.mLock) {
            if (uss.setState(1, 2)) {
                this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                uss.mUnlockProgress.setProgress(20);
                this.mHandler.obtainMessage(100, userId, 0, uss).sendToTarget();
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0033, code lost:
        r37.mInjector.getUserManagerInternal().setUserState(r15, r38.state);
        r38.mUnlockProgress.finish();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0043, code lost:
        if (r15 != 0) goto L_0x004c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0045, code lost:
        r37.mInjector.startPersistentApps(com.android.server.pm.DumpState.DUMP_DOMAIN_PREFERRED);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004c, code lost:
        r37.mInjector.installEncryptionUnawareProviders(r15);
        r0 = new android.content.Intent("android.intent.action.USER_UNLOCKED");
        r0.putExtra("android.intent.extra.user_handle", r15);
        r0.addFlags(1342177280);
        r37.mInjector.broadcastIntent(r0, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, android.os.Binder.getCallingUid(), android.os.Binder.getCallingPid(), r15);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0091, code lost:
        if (getUserInfo(r15).isManagedProfile() == false) goto L_0x00e1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0093, code lost:
        r3 = r37.mInjector.getUserManager().getProfileParent(r15);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x009d, code lost:
        if (r3 == null) goto L_0x00e1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x009f, code lost:
        r5 = new android.content.Intent("android.intent.action.MANAGED_PROFILE_UNLOCKED");
        r5.putExtra("android.intent.extra.USER", android.os.UserHandle.of(r15));
        r5.addFlags(1342177280);
        r37.mInjector.broadcastIntent(r5, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, android.os.Binder.getCallingUid(), android.os.Binder.getCallingPid(), r3.id);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x00e1, code lost:
        r3 = getUserInfo(r15);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00ed, code lost:
        if (java.util.Objects.equals(r3.lastLoggedInFingerprint, android.os.Build.FINGERPRINT) == false) goto L_0x0113;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00f7, code lost:
        if (java.util.Objects.equals(r3.lastLoggedInFingerprintEx, android.os.Build.FINGERPRINTEX) != false) goto L_0x00fa;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00fa, code lost:
        r5 = r37.mCust;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x00fc, code lost:
        if (r5 == null) goto L_0x010f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0102, code lost:
        if (r5.isUpgrade() == false) goto L_0x010f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0104, code lost:
        r37.mCust.sendPreBootBroadcastToManagedProvisioning(r15, new com.android.server.am.$$Lambda$UserController$MCFfPnx3jeypuy0BAGzvhPy3cLc(r37, r38));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x010f, code lost:
        finishUserUnlockedCompleted(r38);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0117, code lost:
        if (r3.isManagedProfile() != false) goto L_0x0122;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x011d, code lost:
        if (r3.isClonedProfile() == false) goto L_0x0120;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0120, code lost:
        r5 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x0124, code lost:
        if (r38.tokenProvided == false) goto L_0x0131;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x012c, code lost:
        if (r37.mLockPatternUtils.isSeparateProfileChallengeEnabled(r15) != false) goto L_0x012f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x012f, code lost:
        r5 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x0131, code lost:
        r5 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x0132, code lost:
        r37.mInjector.sendPreBootBroadcast(r15, r5, new com.android.server.am.$$Lambda$UserController$K71HFCIuD0iCwrDTKYnIUDyAeWg(r37, r38));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:?, code lost:
        return;
     */
    public void finishUserUnlocked(UserState uss) {
        int userId = uss.mHandle.getIdentifier();
        if (StorageManager.isUserKeyUnlocked(userId)) {
            synchronized (this.mLock) {
                try {
                    if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                        try {
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    } else if (!uss.setState(2, 3)) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0022, code lost:
        r0 = getUserInfo(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0026, code lost:
        if (r0 != null) goto L_0x0029;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0028, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x002d, code lost:
        if (android.os.storage.StorageManager.isUserKeyUnlocked(r6) != false) goto L_0x0030;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x002f, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0030, code lost:
        r26.mInjector.getUserManager().onUserLoggedIn(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x003d, code lost:
        if (r0.isInitialized() != false) goto L_0x008b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003f, code lost:
        if (r6 == 0) goto L_0x008b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0041, code lost:
        android.util.Slog.d("ActivityManager", "Initializing user #" + r6);
        r1 = new android.content.Intent("android.intent.action.USER_INITIALIZE");
        r1.addFlags(285212672);
        r26.mInjector.broadcastIntent(r1, null, new com.android.server.am.UserController.AnonymousClass1(r26), 0, null, null, null, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, android.os.Binder.getCallingUid(), android.os.Binder.getCallingPid(), r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x008b, code lost:
        r26.mInjector.startUserWidgets(r6);
        android.util.Slog.i("ActivityManager", "Posting BOOT_COMPLETED user #" + r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x00a6, code lost:
        if (r6 != 0) goto L_0x00cb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x00ae, code lost:
        if (r26.mInjector.isRuntimeRestarted() != false) goto L_0x00cb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x00b6, code lost:
        if (r26.mInjector.isFirstBootOrUpgrade() != false) goto L_0x00cb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00b8, code lost:
        com.android.internal.logging.MetricsLogger.histogram(r26.mInjector.getContext(), "framework_boot_completed", (int) (android.os.SystemClock.elapsedRealtime() / 1000));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x00cb, code lost:
        r1 = new android.content.Intent("android.intent.action.BOOT_COMPLETED", (android.net.Uri) null);
        r1.putExtra("android.intent.extra.user_handle", r6);
        r1.addFlags(-1996488704);
        com.android.server.FgThread.getHandler().post(new com.android.server.am.$$Lambda$UserController$iNxcwiechN4VieHOD0SwsPl6xc(r26, r1, r6, android.os.Binder.getCallingUid(), android.os.Binder.getCallingPid()));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00fa, code lost:
        return;
     */
    /* renamed from: finishUserUnlockedCompleted */
    public void lambda$finishUserUnlocked$3$UserController(UserState uss) {
        int userId = uss.mHandle.getIdentifier();
        synchronized (this.mLock) {
            try {
                if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                    try {
                    } catch (Throwable th) {
                        userInfo = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                userInfo = th2;
                            }
                        }
                        throw userInfo;
                    }
                }
            } catch (Throwable th3) {
                userInfo = th3;
                while (true) {
                    break;
                }
                throw userInfo;
            }
        }
    }

    public /* synthetic */ void lambda$finishUserUnlockedCompleted$4$UserController(Intent bootIntent, final int userId, int callingUid, int callingPid) {
        this.mInjector.broadcastIntent(bootIntent, null, new IIntentReceiver.Stub() {
            /* class com.android.server.am.UserController.AnonymousClass2 */

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                Slog.i("ActivityManager", "Finished processing BOOT_COMPLETED for u" + userId);
                UserController.this.mBootCompleted = true;
            }
        }, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, userId);
    }

    /* access modifiers changed from: package-private */
    public int restartUser(int userId, final boolean foreground) {
        return stopUser(userId, true, null, new UserState.KeyEvictedCallback() {
            /* class com.android.server.am.UserController.AnonymousClass3 */

            @Override // com.android.server.am.UserState.KeyEvictedCallback
            public void keyEvicted(int userId) {
                UserController.this.mHandler.post(new Runnable(userId, foreground) {
                    /* class com.android.server.am.$$Lambda$UserController$3$A5KxB7wo13M_s4_guYgNtwVi9U */
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ boolean f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        UserController.AnonymousClass3.this.lambda$keyEvicted$0$UserController$3(this.f$1, this.f$2);
                    }
                });
            }

            public /* synthetic */ void lambda$keyEvicted$0$UserController$3(int userId, boolean foreground) {
                UserController.this.startUser(userId, foreground);
            }
        });
    }

    /* access modifiers changed from: package-private */
    public int stopUser(int userId, boolean force, IStopUserCallback stopUserCallback, UserState.KeyEvictedCallback keyEvictedCallback) {
        int stopUsersLU;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: switchUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + "android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        } else if (userId < 0 || userId == 0) {
            throw new IllegalArgumentException("Can't stop system user " + userId);
        } else {
            enforceShellRestriction("no_debugging_features", userId);
            synchronized (this.mLock) {
                stopUsersLU = stopUsersLU(userId, force, stopUserCallback, keyEvictedCallback);
            }
            return stopUsersLU;
        }
    }

    @GuardedBy({"mLock"})
    private int stopUsersLU(int userId, boolean force, IStopUserCallback stopUserCallback, UserState.KeyEvictedCallback keyEvictedCallback) {
        if (userId == 0) {
            return -3;
        }
        if (isCurrentUserLU(userId)) {
            return -2;
        }
        int[] usersToStop = getUsersToStopLU(userId);
        for (int relatedUserId : usersToStop) {
            if (relatedUserId == 0 || isCurrentUserLU(relatedUserId)) {
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slog.i("ActivityManager", "stopUsersLocked cannot stop related user " + relatedUserId);
                }
                if (!force) {
                    return -4;
                }
                Slog.i("ActivityManager", "Force stop user " + userId + ". Related users will not be stopped");
                stopSingleUserLU(userId, stopUserCallback, keyEvictedCallback);
                return 0;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i("ActivityManager", "stopUsersLocked usersToStop=" + Arrays.toString(usersToStop));
        }
        int length = usersToStop.length;
        for (int i = 0; i < length; i++) {
            int userIdToStop = usersToStop[i];
            UserState.KeyEvictedCallback keyEvictedCallback2 = null;
            IStopUserCallback iStopUserCallback = userIdToStop == userId ? stopUserCallback : null;
            if (userIdToStop == userId) {
                keyEvictedCallback2 = keyEvictedCallback;
            }
            stopSingleUserLU(userIdToStop, iStopUserCallback, keyEvictedCallback2);
        }
        return 0;
    }

    @GuardedBy({"mLock"})
    private void stopSingleUserLU(int userId, IStopUserCallback stopUserCallback, UserState.KeyEvictedCallback keyEvictedCallback) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i("ActivityManager", "stopSingleUserLocked userId=" + userId);
        }
        UserState uss = this.mStartedUsers.get(userId);
        if (uss != null) {
            if (stopUserCallback != null) {
                uss.mStopCallbacks.add(stopUserCallback);
            }
            if (keyEvictedCallback != null) {
                uss.mKeyEvictedCallbacks.add(keyEvictedCallback);
            }
            if (uss.state != 4 && uss.state != 5) {
                uss.setState(4);
                this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                updateStartedUserArrayLU();
                this.mHandler.post(new Runnable(userId, uss) {
                    /* class com.android.server.am.$$Lambda$UserController$GgoW7PCUhl7RFNZzjZchd9xRw3c */
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ UserState f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        UserController.this.lambda$stopSingleUserLU$6$UserController(this.f$1, this.f$2);
                    }
                });
            }
        } else if (stopUserCallback != null) {
            this.mHandler.post(new Runnable(stopUserCallback, userId) {
                /* class com.android.server.am.$$Lambda$UserController$LbLD4MwASpIHOVLCZugSnJSjJyc */
                private final /* synthetic */ IStopUserCallback f$0;
                private final /* synthetic */ int f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                public final void run() {
                    UserController.lambda$stopSingleUserLU$5(this.f$0, this.f$1);
                }
            });
        }
    }

    static /* synthetic */ void lambda$stopSingleUserLU$5(IStopUserCallback stopUserCallback, int userId) {
        try {
            stopUserCallback.userStopped(userId);
        } catch (RemoteException e) {
        }
    }

    public /* synthetic */ void lambda$stopSingleUserLU$6$UserController(final int userId, final UserState uss) {
        Intent stoppingIntent = new Intent("android.intent.action.USER_STOPPING");
        stoppingIntent.addFlags(1073741824);
        stoppingIntent.putExtra("android.intent.extra.user_handle", userId);
        stoppingIntent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
        IIntentReceiver stoppingReceiver = new IIntentReceiver.Stub() {
            /* class com.android.server.am.UserController.AnonymousClass4 */

            public /* synthetic */ void lambda$performReceive$0$UserController$4(int userId, UserState uss) {
                UserController.this.finishUserStopping(userId, uss);
            }

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                UserController.this.mHandler.post(new Runnable(userId, uss) {
                    /* class com.android.server.am.$$Lambda$UserController$4$P3Sj7pxBXLC7k_puCIIki2uVgGE */
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ UserState f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        UserController.AnonymousClass4.this.lambda$performReceive$0$UserController$4(this.f$1, this.f$2);
                    }
                });
            }
        };
        this.mInjector.clearBroadcastQueueForUser(userId);
        this.mInjector.broadcastIntent(stoppingIntent, null, stoppingReceiver, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), -1);
    }

    /* access modifiers changed from: package-private */
    public void finishUserStopping(int userId, final UserState uss) {
        Intent shutdownIntent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        IIntentReceiver shutdownReceiver = new IIntentReceiver.Stub() {
            /* class com.android.server.am.UserController.AnonymousClass5 */

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                UserController.this.mHandler.post(new Runnable() {
                    /* class com.android.server.am.UserController.AnonymousClass5.AnonymousClass1 */

                    public void run() {
                        UserController.this.finishUserStopped(uss);
                    }
                });
            }
        };
        synchronized (this.mLock) {
            if (uss.state == 4) {
                uss.setState(5);
                this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                this.mInjector.batteryStatsServiceNoteEvent(16391, Integer.toString(userId), userId);
                this.mInjector.getSystemServiceManager().stopUser(userId);
                this.mInjector.broadcastIntent(shutdownIntent, null, shutdownReceiver, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), userId);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void finishUserStopped(UserState uss) {
        ArrayList<IStopUserCallback> stopCallbacks;
        ArrayList<UserState.KeyEvictedCallback> keyEvictedCallbacks;
        boolean stopped;
        int userId = uss.mHandle.getIdentifier();
        boolean lockUser = true;
        int userIdToLock = userId;
        synchronized (this.mLock) {
            stopCallbacks = new ArrayList<>(uss.mStopCallbacks);
            keyEvictedCallbacks = new ArrayList<>(uss.mKeyEvictedCallbacks);
            if (this.mStartedUsers.get(userId) == uss) {
                if (uss.state == 5) {
                    stopped = true;
                    this.mStartedUsers.remove(userId);
                    this.mUserLru.remove(Integer.valueOf(userId));
                    updateStartedUserArrayLU();
                    userIdToLock = updateUserToLockLU(userId);
                    if (userIdToLock == -10000) {
                        lockUser = false;
                    }
                }
            }
            stopped = false;
        }
        if (stopped) {
            this.mInjector.getUserManagerInternal().removeUserState(userId);
            this.mInjector.activityManagerOnUserStopped(userId);
            forceStopUser(userId, "finish user");
        }
        Iterator<IStopUserCallback> it = stopCallbacks.iterator();
        while (it.hasNext()) {
            IStopUserCallback callback = it.next();
            if (stopped) {
                try {
                    callback.userStopped(userId);
                } catch (RemoteException e) {
                }
            } else {
                callback.userStopAborted(userId);
            }
        }
        if (stopped) {
            this.mInjector.systemServiceManagerCleanupUser(userId);
            this.mInjector.stackSupervisorRemoveUser(userId);
            if (getUserInfo(userId).isEphemeral()) {
                this.mInjector.getUserManager().removeUserEvenWhenDisallowed(userId);
            }
            if (lockUser) {
                FgThread.getHandler().post(new Runnable(userIdToLock, userId, keyEvictedCallbacks) {
                    /* class com.android.server.am.$$Lambda$UserController$1yDtRxGsm5PByAaLkhjF19dIWw */
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ int f$2;
                    private final /* synthetic */ ArrayList f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        UserController.this.lambda$finishUserStopped$7$UserController(this.f$1, this.f$2, this.f$3);
                    }
                });
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0019, code lost:
        if (r0 == null) goto L_0x0023;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x001f, code lost:
        if (r0.isHwHiddenSpace() == false) goto L_0x0023;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0021, code lost:
        r1 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0023, code lost:
        r1 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0024, code lost:
        if (r1 != false) goto L_0x0044;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0028, code lost:
        if (r4.mIsSupportISec != false) goto L_0x0034;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x002a, code lost:
        r4.mInjector.getStorageManager().lockUserKey(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0034, code lost:
        r4.mInjector.getStorageManager().lockUserKeyISec(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x003e, code lost:
        r2 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0043, code lost:
        throw r2.rethrowAsRuntimeException();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0044, code lost:
        if (r5 != r6) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0046, code lost:
        r2 = r7.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x004e, code lost:
        if (r2.hasNext() == false) goto L_0x005a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0050, code lost:
        ((com.android.server.am.UserState.KeyEvictedCallback) r2.next()).keyEvicted(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0015, code lost:
        r0 = getUserInfo(r5);
     */
    public /* synthetic */ void lambda$finishUserStopped$7$UserController(int userIdToLockF, int userId, ArrayList keyEvictedCallbacks) {
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(userIdToLockF) != null) {
                Slog.w("ActivityManager", "User was restarted, skipping key eviction");
            }
        }
    }

    @GuardedBy({"mLock"})
    private int updateUserToLockLU(int userId) {
        if (!this.mDelayUserDataLocking || getUserInfo(userId).isEphemeral() || hasUserRestriction("no_run_in_background", userId)) {
            return userId;
        }
        this.mLastActiveUsers.remove(Integer.valueOf(userId));
        this.mLastActiveUsers.add(0, Integer.valueOf(userId));
        if (this.mStartedUsers.size() + this.mLastActiveUsers.size() > this.mMaxRunningUsers) {
            ArrayList<Integer> arrayList = this.mLastActiveUsers;
            int userIdToLock = arrayList.get(arrayList.size() - 1).intValue();
            ArrayList<Integer> arrayList2 = this.mLastActiveUsers;
            arrayList2.remove(arrayList2.size() - 1);
            Slog.i("ActivityManager", "finishUserStopped, stopping user:" + userId + " lock user:" + userIdToLock);
            return userIdToLock;
        }
        Slog.i("ActivityManager", "finishUserStopped, user:" + userId + ",skip locking");
        return -10000;
    }

    @GuardedBy({"mLock"})
    private int[] getUsersToStopLU(int userId) {
        int startedUsersSize = this.mStartedUsers.size();
        IntArray userIds = new IntArray();
        userIds.add(userId);
        int userGroupId = this.mUserProfileGroupIds.get(userId, -10000);
        for (int i = 0; i < startedUsersSize; i++) {
            int startedUserId = this.mStartedUsers.valueAt(i).mHandle.getIdentifier();
            boolean sameUserId = false;
            boolean sameGroup = userGroupId != -10000 && userGroupId == this.mUserProfileGroupIds.get(startedUserId, -10000);
            if (startedUserId == userId) {
                sameUserId = true;
            }
            if (sameGroup && !sameUserId) {
                userIds.add(startedUserId);
            }
        }
        return userIds.toArray();
    }

    private void forceStopUser(int userId, String reason) {
        this.mInjector.activityManagerForceStopPackage(userId, reason);
        Intent intent = new Intent("android.intent.action.USER_STOPPED");
        intent.addFlags(1342177280);
        intent.putExtra("android.intent.extra.user_handle", userId);
        this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), -1);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0039, code lost:
        r1 = getUserInfo(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0041, code lost:
        if (r1.isEphemeral() == false) goto L_0x004e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0043, code lost:
        ((android.os.UserManagerInternal) com.android.server.LocalServices.getService(android.os.UserManagerInternal.class)).onEphemeralUserStop(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0052, code lost:
        if (r1.isGuest() != false) goto L_0x005a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0058, code lost:
        if (r1.isEphemeral() == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x005a, code lost:
        r2 = r4.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x005c, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:?, code lost:
        stopUsersLU(r5, true, null, null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0062, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:?, code lost:
        return;
     */
    private void stopGuestOrEphemeralUserIfBackground(int oldUserId) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i("ActivityManager", "Stop guest or ephemeral user if background: " + oldUserId);
        }
        synchronized (this.mLock) {
            UserState oldUss = this.mStartedUsers.get(oldUserId);
            if (!(oldUserId == 0 || oldUserId == this.mCurrentUserId || oldUss == null || oldUss.state == 4)) {
                if (oldUss.state == 5) {
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void scheduleStartProfiles() {
        FgThread.getHandler().post(new Runnable() {
            /* class com.android.server.am.$$Lambda$UserController$Pih7tcLApDzPmXN5YpO0gEbpMtw */

            public final void run() {
                UserController.this.lambda$scheduleStartProfiles$8$UserController();
            }
        });
    }

    public /* synthetic */ void lambda$scheduleStartProfiles$8$UserController() {
        if (!this.mHandler.hasMessages(40)) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(40), 1000);
        }
    }

    /* access modifiers changed from: package-private */
    public void startProfiles() {
        int currentUserId = getCurrentUserId();
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i("ActivityManager", "startProfilesLocked");
        }
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(currentUserId, false);
        List<UserInfo> profilesToStart = new ArrayList<>(profiles.size());
        for (UserInfo user : profiles) {
            if ((user.flags & 16) == 16 && user.id != currentUserId && !user.isQuietModeEnabled()) {
                profilesToStart.add(user);
            } else if (user.id != currentUserId && user.isClonedProfile()) {
                Slog.i("ActivityManager", "startProfilesLocked clone profile: " + user);
                profilesToStart.add(user);
            }
        }
        int profilesToStartSize = profilesToStart.size();
        int i = 0;
        while (i < profilesToStartSize) {
            startUser(profilesToStart.get(i).id, false);
            i++;
        }
        if (i < profilesToStartSize) {
            Slog.w("ActivityManager", "More profiles than MAX_RUNNING_USERS");
        }
    }

    /* access modifiers changed from: package-private */
    public boolean startUser(int userId, boolean foreground) {
        return lambda$startUser$9$UserController(userId, foreground, null);
    }

    private void setMultiDpi(WindowManagerService wms, int userId) {
        int dpi = SystemProperties.getInt("persist.sys.dpi", 0);
        int width = SystemProperties.getInt("persist.sys.rog.width", 0);
        int realdpi = SystemProperties.getInt("persist.sys.realdpi", 0);
        if (width > 0) {
            dpi = SystemProperties.getInt("persist.sys.realdpi", SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0))));
        }
        if (wms != null && dpi > 0) {
            Slog.i("ActivityManager", "set multi dpi for user :" + userId + ", sys.dpi:" + dpi + ", readdpi:" + realdpi + ", width:" + width);
            wms.setForcedDisplayDensityForUser(0, dpi, userId);
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:100:0x0245, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x024e, code lost:
        if (r6.state != 5) goto L_0x026c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:106:0x0250, code lost:
        r6.setState(0);
        r40.mInjector.getUserManagerInternal().setUserState(r41, r6.state);
        r2 = r40.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x0261, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:?, code lost:
        updateStartedUserArrayLU();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x0265, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:111:0x0266, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x026c, code lost:
        r0 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:117:0x026f, code lost:
        if (r6.state != 0) goto L_0x0288;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:118:0x0271, code lost:
        r40.mInjector.getUserManager().onBeforeStartUser(r41);
        r40.mHandler.sendMessage(r40.mHandler.obtainMessage(50, r41, 0));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x0288, code lost:
        if (r42 == false) goto L_0x02bd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:120:0x028a, code lost:
        r40.mHandler.sendMessage(r40.mHandler.obtainMessage(com.android.server.am.UserController.SYSTEM_USER_CURRENT_MSG, r41, r0));
        r40.mHandler.removeMessages(10);
        r40.mHandler.removeMessages(30);
        r40.mHandler.sendMessage(r40.mHandler.obtainMessage(10, r0, r41, r6));
        r40.mHandler.sendMessageDelayed(r40.mHandler.obtainMessage(30, r0, r41, r6), com.android.server.backup.BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:121:0x02bd, code lost:
        if (r0 == false) goto L_0x0321;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:123:?, code lost:
        r2 = new android.content.Intent("android.intent.action.USER_STARTED");
        r2.addFlags(1342177280);
        r2.putExtra("android.intent.extra.user_handle", r41);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:124:0x02e8, code lost:
        r34 = r6;
        r37 = r0;
        r38 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:126:?, code lost:
        r40.mInjector.broadcastIntent(r2, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r29, r30, r41);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:127:0x0315, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:129:0x031a, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:131:0x0321, code lost:
        r34 = r6;
        r37 = r0;
        r38 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:132:0x032b, code lost:
        if (r42 == false) goto L_0x0337;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:133:0x032d, code lost:
        r6 = r41;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:?, code lost:
        moveUserToForeground(r34, r37, r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:136:0x0337, code lost:
        r6 = r41;
        finishUserBoot(r34);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:137:0x0340, code lost:
        if (r0 == false) goto L_0x037d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x0342, code lost:
        r2 = new android.content.Intent("android.intent.action.USER_STARTING");
        r2.addFlags(1073741824);
        r2.putExtra("android.intent.extra.user_handle", r6);
        r40.mInjector.broadcastIntent(r2, null, new com.android.server.am.UserController.AnonymousClass6(r40), 0, null, null, new java.lang.String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r29, r30, -1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:139:0x037d, code lost:
        r40.isColdStart = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:140:0x037f, code lost:
        android.os.Binder.restoreCallingIdentity(r31);
        android.util.Slog.i("ActivityManager", "_StartUser startUser userid:" + r6 + " cost " + (android.os.SystemClock.elapsedRealtime() - r38) + " ms");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x03ad, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:147:0x03ba, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x03d3, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x0182, code lost:
        if (getUserInfo(r0.intValue()).isClonedProfile() == false) goto L_0x0192;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x019d, code lost:
        if (r43 == null) goto L_0x01a4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x019f, code lost:
        r6.mUnlockProgress.addListener(r43);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x01a4, code lost:
        if (r33 == false) goto L_0x01b1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x01a6, code lost:
        r40.mInjector.getUserManagerInternal().setUserState(r41, r6.state);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x01b1, code lost:
        if (r42 == false) goto L_0x0204;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x01b3, code lost:
        r40.mInjector.reportGlobalUsageEventLocked(16);
        r2 = r40.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x01bc, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:?, code lost:
        r40.mCurrentUserId = r41;
        r40.mTargetUserId = -10000;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x01c3, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x01c4, code lost:
        r40.mInjector.updateUserConfiguration();
        updateCurrentProfileIds();
        r40.mInjector.getWindowManager().setCurrentUser(r41, getCurrentProfileIds());
        android.hwtheme.HwThemeManager.linkDataSkinDirAsUser(r41);
        r40.mInjector.reportCurWakefulnessUsageEvent();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x01e3, code lost:
        if (r40.mUserSwitchUiEnabled == false) goto L_0x0229;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x01eb, code lost:
        if (com.android.server.am.UserController.Injector.access$200(r40.mInjector, r3, r2) != false) goto L_0x0229;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x01ed, code lost:
        r40.mInjector.getWindowManager().setSwitchingUser(true);
        r40.mInjector.getWindowManager().lockNow((android.os.Bundle) null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x0204, code lost:
        r0 = java.lang.Integer.valueOf(r40.mCurrentUserId);
        updateCurrentProfileIds();
        r40.mInjector.getWindowManager().setCurrentProfileIds(getCurrentProfileIds());
        r4 = r40.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x021d, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:?, code lost:
        r40.mUserLru.remove(r0);
        r40.mUserLru.add(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x0228, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:94:0x022c, code lost:
        if (r6.state != 4) goto L_0x024b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x022e, code lost:
        r6.setState(r6.lastState);
        r40.mInjector.getUserManagerInternal().setUserState(r41, r6.state);
        r2 = r40.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:96:0x0240, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:?, code lost:
        updateStartedUserArrayLU();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:0x0244, code lost:
        monitor-exit(r2);
     */
    /* renamed from: startUser */
    public boolean lambda$startUser$9$UserController(int userId, boolean foreground, IProgressListener unlockListener) {
        boolean updateUmState;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            long startedTime = SystemClock.elapsedRealtime();
            this.SwitchUser_Time = startedTime;
            Slog.i("ActivityManager", "Starting userid:" + userId + " fg:" + foreground);
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long ident = Binder.clearCallingIdentity();
            try {
                int oldUserId = getCurrentUserId();
                if (oldUserId == userId) {
                    try {
                        UserState state = getStartedUserState(userId);
                        if (state == null) {
                            Slog.wtf("ActivityManager", "Current user has no UserState");
                        } else if (userId != 0 || state.state != 0) {
                            if (state.state == 3) {
                                notifyFinished(userId, unlockListener);
                            }
                            Binder.restoreCallingIdentity(ident);
                            return true;
                        }
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                }
                if (foreground) {
                    this.mInjector.clearAllLockedTasks("startUser");
                }
                UserInfo userInfo = getUserInfo(userId);
                if (userInfo == null) {
                    Slog.w("ActivityManager", "No user info for user #" + userId);
                    Binder.restoreCallingIdentity(ident);
                    return false;
                } else if (!foreground || !userInfo.isManagedProfile()) {
                    setMultiDpi(this.mInjector.getWindowManager(), userId);
                    UserInfo lastUserInfo = getUserInfo(this.mCurrentUserId);
                    if (foreground && this.mUserSwitchUiEnabled && !this.mInjector.shouldSkipKeyguard(lastUserInfo, userInfo)) {
                        this.mInjector.getWindowManager().startFreezingScreen(17432874, 17432873);
                    }
                    boolean needStart = false;
                    synchronized (this.mLock) {
                        try {
                            UserState uss = this.mStartedUsers.get(userId);
                            if (uss == null) {
                                try {
                                    uss = new UserState(UserHandle.of(userId));
                                    uss.mUnlockProgress.addListener(new UserProgressListener());
                                    this.mStartedUsers.put(userId, uss);
                                    updateStartedUserArrayLU();
                                    needStart = true;
                                    updateUmState = true;
                                } catch (Throwable th2) {
                                    th = th2;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                    throw th;
                                }
                            } else if (uss.state != 5 || isCallingOnHandlerThread()) {
                                updateUmState = false;
                            } else {
                                Slog.i("ActivityManager", "User #" + userId + " is shutting down - will start after full stop");
                                this.mHandler.post(new Runnable(userId, foreground, unlockListener) {
                                    /* class com.android.server.am.$$Lambda$UserController$iUhPl1IaxAC7Q7kTU8VNTT3nUc */
                                    private final /* synthetic */ int f$1;
                                    private final /* synthetic */ boolean f$2;
                                    private final /* synthetic */ IProgressListener f$3;

                                    {
                                        this.f$1 = r2;
                                        this.f$2 = r3;
                                        this.f$3 = r4;
                                    }

                                    public final void run() {
                                        UserController.this.lambda$startUser$9$UserController(this.f$1, this.f$2, this.f$3);
                                    }
                                });
                                Binder.restoreCallingIdentity(ident);
                                return true;
                            }
                            try {
                                Integer userIdInt = Integer.valueOf(userId);
                                if (getUserInfo(userIdInt.intValue()) != null) {
                                    try {
                                    } catch (Throwable th4) {
                                        th = th4;
                                        while (true) {
                                            break;
                                        }
                                        throw th;
                                    }
                                }
                                this.mUserLru.remove(userIdInt);
                                this.mUserLru.add(userIdInt);
                            } catch (Throwable th5) {
                                th = th5;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                } else {
                    Slog.w("ActivityManager", "Cannot switch to User #" + userId + ": not a full user");
                    Binder.restoreCallingIdentity(ident);
                    return false;
                }
            } catch (Throwable th7) {
                th = th7;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            String msg = "Permission Denial: switchUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + "android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        }
        while (true) {
        }
    }

    private boolean isCallingOnHandlerThread() {
        return Looper.myLooper() == this.mHandler.getLooper();
    }

    /* access modifiers changed from: package-private */
    public void startUserInForeground(int targetUserId) {
        if (!startUser(targetUserId, true)) {
            this.mInjector.getWindowManager().setSwitchingUser(false);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean unlockUser(int userId, byte[] token, byte[] secret, IProgressListener listener) {
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            long binderToken = Binder.clearCallingIdentity();
            try {
                return unlockUserCleared(userId, token, secret, listener);
            } finally {
                Binder.restoreCallingIdentity(binderToken);
            }
        } else {
            String msg = "Permission Denial: unlockUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + "android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        }
    }

    private boolean maybeUnlockUser(int userId) {
        return unlockUserCleared(userId, null, null, null);
    }

    private static void notifyFinished(int userId, IProgressListener listener) {
        if (listener != null) {
            try {
                listener.onFinished(userId, (Bundle) null);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0032, code lost:
        android.util.Slog.i("ActivityManager", "ClonedProfile user unlock, set mHaveTryCloneProUserUnlock true!");
        r17.mHaveTryCloneProUserUnlock = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x006a, code lost:
        if (r13.isClonedProfile() != false) goto L_0x0032;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0030, code lost:
        if (r13.isClonedProfile() != false) goto L_0x0032;
     */
    private boolean unlockUserCleared(final int userId, final byte[] token, final byte[] secret, IProgressListener listener) {
        UserState uss;
        int i;
        int[] userIds;
        UserInfo info;
        boolean isSuccess;
        final UserInfo userInfo = getUserInfo(userId);
        final IStorageManager storageManager = this.mInjector.getStorageManager();
        if (!StorageManager.isUserKeyUnlocked(userId)) {
            try {
                if (!this.mIsSupportISec) {
                    storageManager.unlockUserKey(userId, userInfo.serialNumber, token, secret);
                } else {
                    storageManager.unlockUserKeyISec(userId, userInfo.serialNumber, token, secret);
                }
                if (userInfo != null) {
                }
            } catch (RemoteException | RuntimeException e) {
                Slog.w("ActivityManager", "Failed to unlock: " + e.getMessage() + " ,SupportISec: " + this.mIsSupportISec);
                if (userInfo != null) {
                }
            } catch (Throwable th) {
                if (userInfo != null && userInfo.isClonedProfile()) {
                    Slog.i("ActivityManager", "ClonedProfile user unlock, set mHaveTryCloneProUserUnlock true!");
                    this.mHaveTryCloneProUserUnlock = true;
                }
                throw th;
            }
        } else if (this.mIsSupportISec) {
            if (token == null && secret == null) {
                Slog.w("ActivityManager", "is SupportISec,Failed to unlockUserScreenISec: token is null  And secret is null");
            } else {
                try {
                    isSuccess = storageManager.setScreenStateFlag(userId, userInfo.serialNumber, 2);
                } catch (RemoteException | RuntimeException e2) {
                    Slog.w("ActivityManager", "is SupportISec,Failed to setScreenStateFlag: " + e2.getMessage());
                    isSuccess = false;
                }
                if (isSuccess) {
                    this.mHandler.post(new Runnable() {
                        /* class com.android.server.am.UserController.AnonymousClass7 */

                        public void run() {
                            try {
                                storageManager.unlockUserScreenISec(userId, userInfo.serialNumber, token, secret, 1);
                            } catch (RemoteException | RuntimeException e) {
                                Slog.w("ActivityManager", "is SupportISec,Failed to unlockUserScreenISec: " + e.getMessage());
                            }
                        }
                    });
                }
            }
        }
        ISDCardCryptedHelper helper = HwServiceFactory.getSDCardCryptedHelper();
        if (!(helper == null || (info = getUserInfo(userId)) == null)) {
            helper.unlockKey(userId, info.serialNumber, token, secret);
        }
        synchronized (this.mLock) {
            uss = this.mStartedUsers.get(userId);
            if (uss != null) {
                uss.mUnlockProgress.addListener(listener);
                uss.tokenProvided = token != null;
            }
        }
        if (uss == null) {
            notifyFinished(userId, listener);
            return false;
        } else if (!finishUserUnlocking(uss)) {
            notifyFinished(userId, listener);
            return false;
        } else {
            synchronized (this.mLock) {
                userIds = new int[this.mStartedUsers.size()];
                for (int i2 = 0; i2 < userIds.length; i2++) {
                    userIds[i2] = this.mStartedUsers.keyAt(i2);
                }
            }
            for (int testUserId : userIds) {
                UserInfo parent = this.mInjector.getUserManager().getProfileParent(testUserId);
                if (!(parent == null || parent.id != userId || testUserId == userId)) {
                    Slog.d("ActivityManager", "User " + testUserId + " (parent " + parent.id + "): attempting unlock because parent was just unlocked");
                    maybeUnlockUser(testUserId);
                }
            }
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean switchUser(int targetUserId) {
        enforceShellRestriction("no_debugging_features", targetUserId);
        int currentUserId = getCurrentUserId();
        UserInfo targetUserInfo = getUserInfo(targetUserId);
        if (targetUserId == currentUserId) {
            Slog.i("ActivityManager", "user #" + targetUserId + " is already the current user");
            return true;
        } else if (targetUserInfo == null) {
            Slog.w("ActivityManager", "No user info for user #" + targetUserId);
            return false;
        } else if (!targetUserInfo.supportsSwitchTo()) {
            Slog.w("ActivityManager", "Cannot switch to User #" + targetUserId + ": not supported");
            return false;
        } else if (targetUserInfo.isManagedProfile()) {
            Slog.w("ActivityManager", "Cannot switch to User #" + targetUserId + ": not a full user");
            return false;
        } else {
            synchronized (this.mLock) {
                this.mTargetUserId = targetUserId;
            }
            UserInfo currentUserInfo = getUserInfo(this.mCurrentUserId);
            boolean isHiddenSpaceSwitch = this.mInjector.mService.mHwAMSEx.isHiddenSpaceSwitch(currentUserInfo, targetUserInfo);
            this.misHiddenSpaceSwitch = isHiddenSpaceSwitch;
            if (!this.mUserSwitchUiEnabled || isHiddenSpaceSwitch) {
                if (isHiddenSpaceSwitch) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        this.mInjector.cleanAppForHiddenSpace();
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                this.mHandler.removeMessages(START_USER_SWITCH_FG_MSG);
                Handler handler = this.mHandler;
                handler.sendMessage(handler.obtainMessage(START_USER_SWITCH_FG_MSG, targetUserId, 0));
            } else {
                Pair<UserInfo, UserInfo> userNames = new Pair<>(currentUserInfo, targetUserInfo);
                this.mUiHandler.removeMessages(1000);
                this.mUiHandler.sendMessage(this.mHandler.obtainMessage(1000, userNames));
            }
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public void showUserSwitchDialog(Pair<UserInfo, UserInfo> fromToUserPair) {
        this.mInjector.showUserSwitchingDialog((UserInfo) fromToUserPair.first, (UserInfo) fromToUserPair.second, getSwitchingFromSystemUserMessage(), getSwitchingToSystemUserMessage());
    }

    private void dispatchForegroundProfileChanged(int userId) {
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onForegroundProfileSwitch(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    /* access modifiers changed from: package-private */
    public void dispatchUserSwitchComplete(int userId) {
        long startedTime = SystemClock.elapsedRealtime();
        this.mInjector.getWindowManager().setSwitchingUser(false);
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onUserSwitchComplete(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
        if (this.misHiddenSpaceSwitch) {
            this.SwitchUser_Time = SystemClock.elapsedRealtime() - this.SwitchUser_Time;
            Context context = this.mInjector.getContext();
            Flog.bdReport(context, 530, "{isColdStart:" + this.isColdStart + ",SwitchUser_Time:" + this.SwitchUser_Time + "ms}");
        }
        Slog.i("ActivityManager", "_StartUser dispatchUserSwitchComplete userid:" + userId + " cost " + (SystemClock.elapsedRealtime() - startedTime) + " ms");
    }

    private void dispatchLockedBootComplete(int userId) {
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onLockedBootComplete(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    private void stopBackgroundUsersIfEnforced(int oldUserId) {
        if (oldUserId != 0) {
            if (hasUserRestriction("no_run_in_background", oldUserId) || this.mDelayUserDataLocking) {
                synchronized (this.mLock) {
                    if (ActivityManagerDebugConfig.DEBUG_MU) {
                        Slog.i("ActivityManager", "stopBackgroundUsersIfEnforced stopping " + oldUserId + " and related users");
                    }
                    stopUsersLU(oldUserId, false, null, null);
                }
            }
        }
    }

    private void timeoutUserSwitch(UserState uss, int oldUserId, int newUserId) {
        synchronized (this.mLock) {
            Slog.e("ActivityManager", "User switch timeout: from " + oldUserId + " to " + newUserId);
            this.mTimeoutUserSwitchCallbacks = this.mCurWaitingUserSwitchCallbacks;
            this.mHandler.removeMessages(USER_SWITCH_CALLBACKS_TIMEOUT_MSG);
            sendContinueUserSwitchLU(uss, oldUserId, newUserId);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(USER_SWITCH_CALLBACKS_TIMEOUT_MSG, oldUserId, newUserId), 5000);
        }
    }

    private void timeoutUserSwitchCallbacks(int oldUserId, int newUserId) {
        synchronized (this.mLock) {
            if (this.mTimeoutUserSwitchCallbacks != null && !this.mTimeoutUserSwitchCallbacks.isEmpty()) {
                Slog.wtf("ActivityManager", "User switch timeout: from " + oldUserId + " to " + newUserId + ". Observers that didn't respond: " + this.mTimeoutUserSwitchCallbacks);
                this.mTimeoutUserSwitchCallbacks = null;
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x009f, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00b7, code lost:
        r0 = th;
     */
    public void dispatchUserSwitch(final UserState uss, final int oldUserId, final int newUserId) {
        ArraySet<String> curWaitingUserSwitchCallbacks;
        int i;
        Slog.d("ActivityManager", "Dispatch onUserSwitching oldUser #" + oldUserId + " newUser #" + newUserId);
        final int observerCount = this.mUserSwitchObservers.beginBroadcast();
        if (observerCount > 0) {
            final ArraySet<String> curWaitingUserSwitchCallbacks2 = new ArraySet<>();
            synchronized (this.mLock) {
                uss.switching = true;
                this.mCurWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
            }
            final AtomicInteger waitingCallbacksCount = new AtomicInteger(observerCount);
            final long dispatchStartedTime = SystemClock.elapsedRealtime();
            int i2 = 0;
            while (i2 < observerCount) {
                try {
                    final String name = "#" + i2 + HwLog.PREFIX + this.mUserSwitchObservers.getBroadcastCookie(i2);
                    synchronized (this.mLock) {
                        curWaitingUserSwitchCallbacks2.add(name);
                    }
                    i = i2;
                    curWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
                    try {
                        this.mUserSwitchObservers.getBroadcastItem(i).onUserSwitching(newUserId, new IRemoteCallback.Stub() {
                            /* class com.android.server.am.UserController.AnonymousClass8 */

                            public void sendResult(Bundle data) throws RemoteException {
                                synchronized (UserController.this.mLock) {
                                    long delay = SystemClock.elapsedRealtime() - dispatchStartedTime;
                                    if (delay > BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS) {
                                        Slog.e("ActivityManager", "User switch timeout: observer " + name + " sent result after " + delay + " ms");
                                    }
                                    Slog.d("ActivityManager", "_StartUser User switch done: observer " + name + " sent result after " + delay + " ms, total:" + observerCount);
                                    curWaitingUserSwitchCallbacks2.remove(name);
                                    if (waitingCallbacksCount.decrementAndGet() == 0 && curWaitingUserSwitchCallbacks2 == UserController.this.mCurWaitingUserSwitchCallbacks) {
                                        Slog.i("ActivityManager", "_StartUser dispatchUserSwitch userid:" + newUserId + " cost " + (SystemClock.elapsedRealtime() - dispatchStartedTime) + " ms");
                                        UserController.this.sendContinueUserSwitchLU(uss, oldUserId, newUserId);
                                    }
                                }
                            }
                        });
                    } catch (RemoteException e) {
                    }
                } catch (RemoteException e2) {
                    i = i2;
                    curWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
                }
                i2 = i + 1;
                curWaitingUserSwitchCallbacks2 = curWaitingUserSwitchCallbacks;
            }
        } else {
            synchronized (this.mLock) {
                sendContinueUserSwitchLU(uss, oldUserId, newUserId);
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
        return;
        while (true) {
        }
        while (true) {
        }
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mLock"})
    public void sendContinueUserSwitchLU(UserState uss, int oldUserId, int newUserId) {
        this.mCurWaitingUserSwitchCallbacks = null;
        this.mHandler.removeMessages(30);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(20, oldUserId, newUserId, uss));
    }

    /* access modifiers changed from: package-private */
    public void continueUserSwitch(UserState uss, int oldUserId, int newUserId) {
        Slog.d("ActivityManager", "Continue user switch oldUser #" + oldUserId + ", newUser #" + newUserId);
        if (this.mUserSwitchUiEnabled) {
            this.mInjector.getWindowManager().stopFreezingScreen();
        }
        uss.switching = false;
        this.mHandler.removeMessages(80);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(80, newUserId, 0));
        stopGuestOrEphemeralUserIfBackground(oldUserId);
        stopBackgroundUsersIfEnforced(oldUserId);
    }

    private void moveUserToForeground(UserState uss, int oldUserId, int newUserId) {
        boolean homeInFront = this.mInjector.stackSupervisorSwitchUser(newUserId, uss);
        HwThemeManager.updateConfiguration(true);
        ContentResolver cr = this.mInjector.getContext().getContentResolver();
        Configuration config = new Configuration();
        HwThemeManager.retrieveSimpleUIConfig(cr, config, newUserId);
        config.fontScale = Settings.System.getFloatForUser(cr, "font_scale", config.fontScale, newUserId);
        this.mInjector.updatePersistentConfiguration(config);
        if (homeInFront) {
            this.mInjector.startHomeActivity(newUserId, "moveUserToForeground");
        } else {
            this.mInjector.stackSupervisorResumeFocusedStackTopActivity();
        }
        EventLogTags.writeAmSwitchUser(newUserId);
        sendUserSwitchBroadcasts(oldUserId, newUserId);
    }

    /* access modifiers changed from: package-private */
    public void sendUserSwitchBroadcasts(int oldUserId, int newUserId) {
        String str;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        String str2 = "android.intent.extra.user_handle";
        int i = 1342177280;
        if (oldUserId >= 0) {
            try {
                List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(oldUserId, false);
                int count = profiles.size();
                int i2 = 0;
                while (i2 < count) {
                    int profileUserId = profiles.get(i2).id;
                    Intent intent = new Intent("android.intent.action.USER_BACKGROUND");
                    intent.addFlags(i);
                    intent.putExtra(str2, profileUserId);
                    this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, profileUserId);
                    i2++;
                    count = count;
                    profiles = profiles;
                    str2 = str2;
                    i = 1342177280;
                }
                str = str2;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            str = str2;
        }
        if (newUserId >= 0) {
            List<UserInfo> profiles2 = this.mInjector.getUserManager().getProfiles(newUserId, false);
            int count2 = profiles2.size();
            int i3 = 0;
            while (i3 < count2) {
                int profileUserId2 = profiles2.get(i3).id;
                Intent intent2 = new Intent("android.intent.action.USER_FOREGROUND");
                intent2.addFlags(1342177280);
                intent2.putExtra(str, profileUserId2);
                this.mInjector.broadcastIntent(intent2, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, profileUserId2);
                i3++;
                count2 = count2;
                str = str;
            }
            Intent intent3 = new Intent("android.intent.action.USER_SWITCHED");
            intent3.addFlags(1342177280);
            intent3.putExtra(str, newUserId);
            this.mInjector.broadcastIntent(intent3, null, null, 0, null, null, new String[]{"android.permission.MANAGE_USERS"}, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, -1);
        }
        Binder.restoreCallingIdentity(ident);
    }

    /* access modifiers changed from: package-private */
    public int handleIncomingUser(int callingPid, int callingUid, int userId, boolean allowAll, int allowMode, String name, String callerPackage) {
        boolean allow;
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUserId == userId || this.mInjector.getUserManagerInternal().isSameGroupForClone(callingUserId, userId)) {
            return userId;
        }
        int targetUserId = unsafeConvertIncomingUser(userId);
        if (callingUid != 0 && callingUid != 1000) {
            if (this.mInjector.isCallerRecents(callingUid) && callingUserId == getCurrentUserId() && isSameProfileGroup(callingUserId, targetUserId)) {
                allow = true;
            } else if (this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, callingUid, -1, true) == 0) {
                allow = true;
            } else if (allowMode == 2) {
                allow = false;
            } else if (this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingPid, callingUid, -1, true) != 0) {
                allow = false;
            } else if (allowMode == 0) {
                allow = true;
            } else if (allowMode == 1) {
                allow = isSameProfileGroup(callingUserId, targetUserId);
            } else {
                throw new IllegalArgumentException("Unknown mode: " + allowMode);
            }
            if (!allow) {
                if (userId == -3) {
                    targetUserId = callingUserId;
                } else {
                    StringBuilder builder = new StringBuilder(128);
                    builder.append("Permission Denial: ");
                    builder.append(name);
                    if (callerPackage != null) {
                        builder.append(" from ");
                        builder.append(callerPackage);
                    }
                    builder.append(" asks to run as user ");
                    builder.append(userId);
                    builder.append(" but is calling from uid ");
                    UserHandle.formatUid(builder, callingUid);
                    builder.append("; this requires ");
                    builder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
                    if (allowMode != 2) {
                        builder.append(" or ");
                        builder.append("android.permission.INTERACT_ACROSS_USERS");
                    }
                    String msg = builder.toString();
                    Slog.w("ActivityManager", msg);
                    throw new SecurityException(msg);
                }
            }
        }
        if (!allowAll) {
            ensureNotSpecialUser(targetUserId);
        }
        if (callingUid != 2000 || targetUserId < 0 || !hasUserRestriction("no_debugging_features", targetUserId)) {
            return targetUserId;
        }
        throw new SecurityException("Shell does not have permission to access user " + targetUserId + "\n " + Debug.getCallers(3));
    }

    /* access modifiers changed from: package-private */
    public int unsafeConvertIncomingUser(int userId) {
        if (userId == -2 || userId == -3) {
            return getCurrentUserId();
        }
        return userId;
    }

    /* access modifiers changed from: package-private */
    public void ensureNotSpecialUser(int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("Call does not support special user #" + userId);
        }
    }

    /* access modifiers changed from: package-private */
    public void registerUserSwitchObserver(IUserSwitchObserver observer, String name) {
        Preconditions.checkNotNull(name, "Observer name cannot be null");
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            this.mUserSwitchObservers.register(observer, name);
            return;
        }
        String msg = "Permission Denial: registerUserSwitchObserver() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + "android.permission.INTERACT_ACROSS_USERS_FULL";
        Slog.w("ActivityManager", msg);
        throw new SecurityException(msg);
    }

    /* access modifiers changed from: package-private */
    public void sendForegroundProfileChanged(int userId) {
        this.mHandler.removeMessages(70);
        this.mHandler.obtainMessage(70, userId, 0).sendToTarget();
    }

    /* access modifiers changed from: package-private */
    public void unregisterUserSwitchObserver(IUserSwitchObserver observer) {
        this.mUserSwitchObservers.unregister(observer);
    }

    /* access modifiers changed from: package-private */
    public UserState getStartedUserState(int userId) {
        UserState userState;
        synchronized (this.mLock) {
            userState = this.mStartedUsers.get(userId);
        }
        return userState;
    }

    /* access modifiers changed from: package-private */
    public boolean hasStartedUserState(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mStartedUsers.get(userId) != null;
        }
        return z;
    }

    @GuardedBy({"mLock"})
    private void updateStartedUserArrayLU() {
        int num = 0;
        for (int i = 0; i < this.mStartedUsers.size(); i++) {
            UserState uss = this.mStartedUsers.valueAt(i);
            if (!(uss.state == 4 || uss.state == 5)) {
                num++;
            }
        }
        this.mStartedUserArray = new int[num];
        int num2 = 0;
        for (int i2 = 0; i2 < this.mStartedUsers.size(); i2++) {
            UserState uss2 = this.mStartedUsers.valueAt(i2);
            if (!(uss2.state == 4 || uss2.state == 5)) {
                this.mStartedUserArray[num2] = this.mStartedUsers.keyAt(i2);
                num2++;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void sendBootCompleted(IIntentReceiver resultTo) {
        SparseArray<UserState> startedUsers;
        synchronized (this.mLock) {
            startedUsers = this.mStartedUsers.clone();
        }
        for (int i = 0; i < startedUsers.size(); i++) {
            finishUserBoot(startedUsers.valueAt(i), resultTo);
        }
    }

    /* access modifiers changed from: package-private */
    public void onSystemReady() {
        updateCurrentProfileIds();
        this.mInjector.reportCurWakefulnessUsageEvent();
    }

    private void updateCurrentProfileIds() {
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(getCurrentUserId(), false);
        int[] currentProfileIds = new int[profiles.size()];
        for (int i = 0; i < currentProfileIds.length; i++) {
            currentProfileIds[i] = profiles.get(i).id;
        }
        List<UserInfo> users = this.mInjector.getUserManager().getUsers(false);
        synchronized (this.mLock) {
            this.mCurrentProfileIds = currentProfileIds;
            this.mUserProfileGroupIds.clear();
            for (int i2 = 0; i2 < users.size(); i2++) {
                UserInfo user = users.get(i2);
                if (user.profileGroupId != -10000) {
                    this.mUserProfileGroupIds.put(user.id, user.profileGroupId);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public int[] getStartedUserArray() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mStartedUserArray;
        }
        return iArr;
    }

    /* access modifiers changed from: package-private */
    public boolean isUserRunning(int userId, int flags) {
        UserState state = getStartedUserState(userId);
        if (state == null) {
            return false;
        }
        if ((flags & 1) != 0) {
            return true;
        }
        if ((flags & 2) != 0) {
            int i = state.state;
            if (i == 0 || i == 1) {
                return true;
            }
            return false;
        } else if ((flags & 8) != 0) {
            int i2 = state.state;
            if (i2 == 2 || i2 == 3) {
                return true;
            }
            if (i2 == 4 || i2 == 5) {
                return StorageManager.isUserKeyUnlocked(userId);
            }
            return false;
        } else if ((flags & 4) != 0) {
            int i3 = state.state;
            if (i3 == 3) {
                return true;
            }
            if (i3 == 4 || i3 == 5) {
                return StorageManager.isUserKeyUnlocked(userId);
            }
            return false;
        } else if (state.state == 4 || state.state == 5) {
            return false;
        } else {
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isSystemUserStarted() {
        synchronized (this.mLock) {
            boolean z = false;
            UserState uss = this.mStartedUsers.get(0);
            if (uss == null) {
                return false;
            }
            if (uss.state == 1 || uss.state == 2 || uss.state == 3) {
                z = true;
            }
            return z;
        }
    }

    /* access modifiers changed from: package-private */
    public UserInfo getCurrentUser() {
        UserInfo currentUserLU;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: getCurrentUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + "android.permission.INTERACT_ACROSS_USERS";
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        } else if (this.mTargetUserId == -10000) {
            return getUserInfo(this.mCurrentUserId);
        } else {
            synchronized (this.mLock) {
                currentUserLU = getCurrentUserLU();
            }
            return currentUserLU;
        }
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mLock"})
    public UserInfo getCurrentUserLU() {
        return getUserInfo(this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId);
    }

    /* access modifiers changed from: package-private */
    public int getCurrentOrTargetUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
        }
        return i;
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mLock"})
    public int getCurrentOrTargetUserIdLU() {
        return this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mLock"})
    public int getCurrentUserIdLU() {
        return this.mCurrentUserId;
    }

    /* access modifiers changed from: package-private */
    public int getCurrentUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mCurrentUserId;
        }
        return i;
    }

    @GuardedBy({"mLock"})
    private boolean isCurrentUserLU(int userId) {
        return userId == getCurrentOrTargetUserIdLU();
    }

    /* access modifiers changed from: package-private */
    public int[] getUsers() {
        UserManagerService ums = this.mInjector.getUserManager();
        if (ums != null) {
            return ums.getUserIds();
        }
        return new int[]{0};
    }

    /* access modifiers changed from: package-private */
    public UserInfo getUserInfo(int userId) {
        return this.mInjector.getUserManager().getUserInfo(userId);
    }

    /* access modifiers changed from: package-private */
    public int[] getUserIds() {
        return this.mInjector.getUserManager().getUserIds();
    }

    /* access modifiers changed from: package-private */
    public int[] expandUserId(int userId) {
        if (userId == -1) {
            return getUsers();
        }
        return new int[]{userId};
    }

    /* access modifiers changed from: package-private */
    public boolean exists(int userId) {
        return this.mInjector.getUserManager().exists(userId);
    }

    private void enforceShellRestriction(String restriction, int userHandle) {
        if (Binder.getCallingUid() != 2000) {
            return;
        }
        if (userHandle < 0 || hasUserRestriction(restriction, userHandle)) {
            throw new SecurityException("Shell does not have permission to access user " + userHandle);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasUserRestriction(String restriction, int userId) {
        return this.mInjector.getUserManager().hasUserRestriction(restriction, userId);
    }

    /* access modifiers changed from: package-private */
    public boolean isSameProfileGroup(int callingUserId, int targetUserId) {
        boolean z = true;
        if (callingUserId == targetUserId) {
            return true;
        }
        synchronized (this.mLock) {
            int callingProfile = this.mUserProfileGroupIds.get(callingUserId, -10000);
            int targetProfile = this.mUserProfileGroupIds.get(targetUserId, -10000);
            if (callingProfile == -10000 || callingProfile != targetProfile) {
                z = false;
            }
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public boolean isUserOrItsParentRunning(int userId) {
        synchronized (this.mLock) {
            if (isUserRunning(userId, 0)) {
                return true;
            }
            int parentUserId = this.mUserProfileGroupIds.get(userId, -10000);
            if (parentUserId == -10000) {
                return false;
            }
            return isUserRunning(parentUserId, 0);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isCurrentProfile(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mCurrentProfileIds, userId);
        }
        return contains;
    }

    /* access modifiers changed from: package-private */
    public int[] getCurrentProfileIds() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mCurrentProfileIds;
        }
        return iArr;
    }

    /* access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            for (int i = this.mUserProfileGroupIds.size() - 1; i >= 0; i--) {
                if (this.mUserProfileGroupIds.keyAt(i) == userId || this.mUserProfileGroupIds.valueAt(i) == userId) {
                    this.mUserProfileGroupIds.removeAt(i);
                }
            }
            this.mCurrentProfileIds = ArrayUtils.removeInt(this.mCurrentProfileIds, userId);
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0017, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0018, code lost:
        r0 = r3.mInjector.getKeyguardManager();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0022, code lost:
        if (r0.isDeviceLocked(r4) == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0028, code lost:
        if (r0.isDeviceSecure(r4) == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x002a, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:?, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0015, code lost:
        if (r3.mLockPatternUtils.isSeparateProfileChallengeEnabled(r4) != false) goto L_0x0018;
     */
    public boolean shouldConfirmCredentials(int userId) {
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(userId) == null) {
                return false;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isLockScreenDisabled(int userId) {
        return this.mLockPatternUtils.isLockScreenDisabled(userId);
    }

    /* access modifiers changed from: package-private */
    public void setSwitchingFromSystemUserMessage(String switchingFromSystemUserMessage) {
        synchronized (this.mLock) {
            this.mSwitchingFromSystemUserMessage = switchingFromSystemUserMessage;
        }
    }

    /* access modifiers changed from: package-private */
    public void setSwitchingToSystemUserMessage(String switchingToSystemUserMessage) {
        synchronized (this.mLock) {
            this.mSwitchingToSystemUserMessage = switchingToSystemUserMessage;
        }
    }

    private String getSwitchingFromSystemUserMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingFromSystemUserMessage;
        }
        return str;
    }

    private String getSwitchingToSystemUserMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingToSystemUserMessage;
        }
        return str;
    }

    /* access modifiers changed from: package-private */
    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        synchronized (this.mLock) {
            long token = proto.start(fieldId);
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                UserState uss = this.mStartedUsers.valueAt(i);
                long uToken = proto.start(2246267895809L);
                proto.write(1120986464257L, uss.mHandle.getIdentifier());
                uss.writeToProto(proto, 1146756268034L);
                proto.end(uToken);
            }
            for (int i2 = 0; i2 < this.mStartedUserArray.length; i2++) {
                proto.write(2220498092034L, this.mStartedUserArray[i2]);
            }
            for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
                proto.write(2220498092035L, this.mUserLru.get(i3).intValue());
            }
            if (this.mUserProfileGroupIds.size() > 0) {
                for (int i4 = 0; i4 < this.mUserProfileGroupIds.size(); i4++) {
                    long uToken2 = proto.start(2246267895812L);
                    proto.write(1120986464257L, this.mUserProfileGroupIds.keyAt(i4));
                    proto.write(1120986464258L, this.mUserProfileGroupIds.valueAt(i4));
                    proto.end(uToken2);
                }
            }
            proto.end(token);
        }
    }

    /* access modifiers changed from: package-private */
    public void dump(PrintWriter pw, boolean dumpAll) {
        synchronized (this.mLock) {
            pw.println("  mStartedUsers:");
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                UserState uss = this.mStartedUsers.valueAt(i);
                pw.print("    User #");
                pw.print(uss.mHandle.getIdentifier());
                pw.print(": ");
                uss.dump("", pw);
            }
            pw.print("  mStartedUserArray: [");
            for (int i2 = 0; i2 < this.mStartedUserArray.length; i2++) {
                if (i2 > 0) {
                    pw.print(", ");
                }
                pw.print(this.mStartedUserArray[i2]);
            }
            pw.println("]");
            pw.print("  mUserLru: [");
            for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
                if (i3 > 0) {
                    pw.print(", ");
                }
                pw.print(this.mUserLru.get(i3));
            }
            pw.println("]");
            if (this.mUserProfileGroupIds.size() > 0) {
                pw.println("  mUserProfileGroupIds:");
                for (int i4 = 0; i4 < this.mUserProfileGroupIds.size(); i4++) {
                    pw.print("    User #");
                    pw.print(this.mUserProfileGroupIds.keyAt(i4));
                    pw.print(" -> profile #");
                    pw.println(this.mUserProfileGroupIds.valueAt(i4));
                }
            }
            pw.println("  mCurrentUserId:" + this.mCurrentUserId);
            pw.println("  mLastActiveUsers:" + this.mLastActiveUsers);
        }
    }

    public boolean handleMessage(Message msg) {
        long startedTime = SystemClock.elapsedRealtime();
        switch (msg.what) {
            case 10:
                dispatchUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 20:
                continueUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 30:
                timeoutUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 40:
                startProfiles();
                return false;
            case 50:
                this.mInjector.batteryStatsServiceNoteEvent(32775, Integer.toString(msg.arg1), msg.arg1);
                this.mInjector.getSystemServiceManager().startUser(msg.arg1);
                Slog.i("ActivityManager", "_StartUser Handle SYSTEM_USER_START_MSG userid:" + msg.arg1 + " cost " + (SystemClock.elapsedRealtime() - startedTime) + " ms");
                return false;
            case SYSTEM_USER_CURRENT_MSG /*{ENCODED_INT: 60}*/:
                this.mInjector.batteryStatsServiceNoteEvent(16392, Integer.toString(msg.arg2), msg.arg2);
                this.mInjector.batteryStatsServiceNoteEvent(32776, Integer.toString(msg.arg1), msg.arg1);
                this.mInjector.getSystemServiceManager().switchUser(msg.arg1);
                Slog.i("ActivityManager", "_StartUser Handle SYSTEM_USER_CURRENT_MSG userid:" + msg.arg1 + " cost " + (SystemClock.elapsedRealtime() - startedTime) + " ms");
                return false;
            case 70:
                dispatchForegroundProfileChanged(msg.arg1);
                return false;
            case 80:
                dispatchUserSwitchComplete(msg.arg1);
                return false;
            case USER_SWITCH_CALLBACKS_TIMEOUT_MSG /*{ENCODED_INT: 90}*/:
                timeoutUserSwitchCallbacks(msg.arg1, msg.arg2);
                return false;
            case 100:
                int userId = msg.arg1;
                this.mInjector.getSystemServiceManager().unlockUser(userId);
                FgThread.getHandler().post(new Runnable(userId) {
                    /* class com.android.server.am.$$Lambda$UserController$Xwi4wwUc7p4WMAGCrzXjdkhA4AI */
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        UserController.this.lambda$handleMessage$10$UserController(this.f$1);
                    }
                });
                finishUserUnlocked((UserState) msg.obj);
                Slog.i("ActivityManager", "_StartUser Handle SYSTEM_USER_UNLOCK_MSG userid:" + msg.arg1 + " cost " + (SystemClock.elapsedRealtime() - startedTime) + " ms");
                return false;
            case 110:
                dispatchLockedBootComplete(msg.arg1);
                return false;
            case START_USER_SWITCH_FG_MSG /*{ENCODED_INT: 120}*/:
                startUserInForeground(msg.arg1);
                return false;
            case 1000:
                showUserSwitchDialog((Pair) msg.obj);
                return false;
            default:
                return false;
        }
    }

    public /* synthetic */ void lambda$handleMessage$10$UserController(int userId) {
        this.mInjector.loadUserRecents(userId);
    }

    private static class UserProgressListener extends IProgressListener.Stub {
        private volatile long mUnlockStarted;

        private UserProgressListener() {
        }

        public void onStarted(int id, Bundle extras) throws RemoteException {
            Slog.d("ActivityManager", "Started unlocking user " + id);
            this.mUnlockStarted = SystemClock.uptimeMillis();
        }

        public void onProgress(int id, int progress, Bundle extras) throws RemoteException {
            Slog.d("ActivityManager", "Unlocking user " + id + " progress " + progress);
        }

        public void onFinished(int id, Bundle extras) throws RemoteException {
            long unlockTime = SystemClock.uptimeMillis() - this.mUnlockStarted;
            if (id == 0) {
                new TimingsTraceLog("SystemServerTiming", 524288).logDuration("SystemUserUnlock", unlockTime);
                return;
            }
            TimingsTraceLog timingsTraceLog = new TimingsTraceLog("SystemServerTiming", 524288);
            timingsTraceLog.logDuration("User" + id + "Unlock", unlockTime);
        }
    }

    @VisibleForTesting
    static class Injector {
        /* access modifiers changed from: private */
        public final ActivityManagerService mService;
        private UserManagerService mUserManager;
        private UserManagerInternal mUserManagerInternal;

        Injector(ActivityManagerService service) {
            this.mService = service;
        }

        /* access modifiers changed from: protected */
        public Handler getHandler(Handler.Callback callback) {
            return new Handler(this.mService.mHandlerThread.getLooper(), callback);
        }

        /* access modifiers changed from: protected */
        public Handler getUiHandler(Handler.Callback callback) {
            return new Handler(this.mService.mUiHandler.getLooper(), callback);
        }

        /* access modifiers changed from: protected */
        public Context getContext() {
            return this.mService.mContext;
        }

        /* access modifiers changed from: protected */
        public LockPatternUtils getLockPatternUtils() {
            return new LockPatternUtils(getContext());
        }

        /* access modifiers changed from: protected */
        public int broadcastIntent(Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean ordered, boolean sticky, int callingPid, int callingUid, int realCallingUid, int realCallingPid, int userId) {
            int broadcastIntentLocked;
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    broadcastIntentLocked = this.mService.broadcastIntentLocked(null, null, intent, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, appOp, bOptions, ordered, sticky, callingPid, callingUid, realCallingUid, realCallingPid, userId);
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            return broadcastIntentLocked;
        }

        /* access modifiers changed from: package-private */
        public int checkCallingPermission(String permission) {
            return this.mService.checkCallingPermission(permission);
        }

        /* access modifiers changed from: package-private */
        public WindowManagerService getWindowManager() {
            return this.mService.mWindowManager;
        }

        /* access modifiers changed from: package-private */
        public void activityManagerOnUserStopped(int userId) {
            ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).onUserStopped(userId);
        }

        /* access modifiers changed from: package-private */
        public void systemServiceManagerCleanupUser(int userId) {
            this.mService.mSystemServiceManager.cleanupUser(userId);
        }

        /* access modifiers changed from: protected */
        public UserManagerService getUserManager() {
            if (this.mUserManager == null) {
                this.mUserManager = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            }
            return this.mUserManager;
        }

        /* access modifiers changed from: package-private */
        public UserManagerInternal getUserManagerInternal() {
            if (this.mUserManagerInternal == null) {
                this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            }
            return this.mUserManagerInternal;
        }

        /* access modifiers changed from: package-private */
        public KeyguardManager getKeyguardManager() {
            return (KeyguardManager) this.mService.mContext.getSystemService(KeyguardManager.class);
        }

        /* access modifiers changed from: package-private */
        public void batteryStatsServiceNoteEvent(int code, String name, int uid) {
            this.mService.mBatteryStatsService.noteEvent(code, name, uid);
        }

        /* access modifiers changed from: package-private */
        public boolean isRuntimeRestarted() {
            return this.mService.mSystemServiceManager.isRuntimeRestarted();
        }

        /* access modifiers changed from: package-private */
        public SystemServiceManager getSystemServiceManager() {
            return this.mService.mSystemServiceManager;
        }

        /* access modifiers changed from: package-private */
        public boolean isFirstBootOrUpgrade() {
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                return pm.isFirstBoot() || pm.isDeviceUpgrading();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        /* access modifiers changed from: package-private */
        public void sendPreBootBroadcast(int userId, boolean quiet, final Runnable onFinish) {
            new PreBootBroadcaster(this.mService, userId, null, quiet) {
                /* class com.android.server.am.UserController.Injector.AnonymousClass1 */

                @Override // com.android.server.am.PreBootBroadcaster
                public void onFinished() {
                    onFinish.run();
                }
            }.sendNext();
        }

        /* access modifiers changed from: package-private */
        public void activityManagerForceStopPackage(int userId, String reason) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.forceStopPackageLocked(null, -1, false, false, true, false, false, userId, reason);
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        /* access modifiers changed from: package-private */
        public int checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
            ActivityManagerService activityManagerService = this.mService;
            return ActivityManagerService.checkComponentPermission(permission, pid, uid, owningUid, exported);
        }

        /* access modifiers changed from: protected */
        public void startHomeActivity(int userId, String reason) {
            this.mService.mAtmInternal.startHomeActivity(userId, reason);
        }

        /* access modifiers changed from: package-private */
        public void startUserWidgets(int userId) {
            AppWidgetManagerInternal awm = (AppWidgetManagerInternal) LocalServices.getService(AppWidgetManagerInternal.class);
            if (awm != null) {
                FgThread.getHandler().post(new Runnable(awm, userId) {
                    /* class com.android.server.am.$$Lambda$UserController$Injector$MYTLl7MOQKjyMJknWdxPeBLoPCc */
                    private final /* synthetic */ AppWidgetManagerInternal f$0;
                    private final /* synthetic */ int f$1;

                    {
                        this.f$0 = r1;
                        this.f$1 = r2;
                    }

                    public final void run() {
                        this.f$0.unlockUser(this.f$1);
                    }
                });
            }
        }

        /* access modifiers changed from: package-private */
        public void updateUserConfiguration() {
            this.mService.mAtmInternal.updateUserConfiguration();
        }

        /* access modifiers changed from: package-private */
        public void clearBroadcastQueueForUser(int userId) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.clearBroadcastQueueForUserLocked(userId);
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void loadUserRecents(int userId) {
            this.mService.mAtmInternal.loadRecentTasksForUser(userId);
        }

        /* access modifiers changed from: package-private */
        public void startPersistentApps(int matchFlags) {
            this.mService.startPersistentApps(matchFlags);
        }

        /* access modifiers changed from: package-private */
        public void installEncryptionUnawareProviders(int userId) {
            this.mService.installEncryptionUnawareProviders(userId);
        }

        /* access modifiers changed from: package-private */
        public void showUserSwitchingDialog(UserInfo fromUser, UserInfo toUser, String switchingFromSystemUserMessage, String switchingToSystemUserMessage) {
            Dialog d;
            if (!this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                ActivityManagerService activityManagerService = this.mService;
                d = new UserSwitchingDialog(activityManagerService, activityManagerService.mContext, fromUser, toUser, true, switchingFromSystemUserMessage, switchingToSystemUserMessage);
            } else {
                ActivityManagerService activityManagerService2 = this.mService;
                d = new CarUserSwitchingDialog(activityManagerService2, activityManagerService2.mContext, fromUser, toUser, true, switchingFromSystemUserMessage, switchingToSystemUserMessage);
            }
            d.show();
        }

        /* access modifiers changed from: package-private */
        public void updatePersistentConfiguration(Configuration config) {
            this.mService.updatePersistentConfiguration(config);
        }

        /* access modifiers changed from: package-private */
        public void reportGlobalUsageEventLocked(int event) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.reportGlobalUsageEventLocked(event);
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void reportCurWakefulnessUsageEvent() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.reportCurWakefulnessUsageEventLocked();
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void stackSupervisorRemoveUser(int userId) {
            this.mService.mAtmInternal.removeUser(userId);
        }

        /* access modifiers changed from: protected */
        public boolean stackSupervisorSwitchUser(int userId, UserState uss) {
            return this.mService.mAtmInternal.switchUser(userId, uss);
        }

        /* access modifiers changed from: protected */
        public void stackSupervisorResumeFocusedStackTopActivity() {
            this.mService.mAtmInternal.resumeTopActivities(false);
        }

        /* access modifiers changed from: protected */
        public void clearAllLockedTasks(String reason) {
            this.mService.mAtmInternal.clearLockedTasks(reason);
        }

        /* access modifiers changed from: protected */
        public boolean isCallerRecents(int callingUid) {
            return this.mService.mAtmInternal.isCallerRecents(callingUid);
        }

        /* access modifiers changed from: private */
        public boolean shouldSkipKeyguard(UserInfo first, UserInfo second) {
            return this.mService.mHwAMSEx.isHiddenSpaceSwitch(first, second) && getWindowManager().isKeyguardLocked();
        }

        /* access modifiers changed from: package-private */
        public void cleanAppForHiddenSpace() {
            this.mService.mHwAMSEx.cleanAppForHiddenSpace();
        }

        /* access modifiers changed from: protected */
        public IStorageManager getStorageManager() {
            return IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        }
    }
}
