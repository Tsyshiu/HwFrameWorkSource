package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.freeform.HwFreeFormManager;
import android.freeform.HwFreeFormUtils;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.Pools;
import android.util.Slog;
import android.view.IApplicationToken;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.HeavyWeightSwitcherActivity;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.am.PendingIntentRecord;
import com.android.server.os.HwBootCheck;
import com.android.server.pm.InstantAppResolver;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.ActivityStackSupervisor;
import com.android.server.wm.LaunchParamsController;
import com.android.server.wm.WindowManagerService;
import com.huawei.server.wm.IHwActivityStarterEx;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ActivityStarter extends AbsActivityStarter {
    private static final String ACTION_HWCHOOSER = "com.huawei.intent.action.hwCHOOSER";
    private static final String EXTRA_ALWAYS_USE_OPTION = "alwaysUseOption";
    private static final String HWPCEXPLORER_PACKAGE_NAME = "com.huawei.desktop.explorer";
    private static final String INCALLUI_ACTIVITY_CLASS_NAME = "com.android.incallui/.InCallActivity";
    private static final int INVALID_LAUNCH_MODE = -1;
    protected static final String SUW_FRP_STATE = "hw_suw_frp_state";
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_CONFIGURATION = ("ActivityTaskManager" + ActivityTaskManagerDebugConfig.POSTFIX_CONFIGURATION);
    private static final String TAG_FOCUS = "ActivityTaskManager";
    private static final String TAG_RESULTS = "ActivityTaskManager";
    private static final String TAG_USER_LEAVING = "ActivityTaskManager";
    private boolean mAddingToTask;
    private boolean mAvoidMoveToFront;
    private int mCallingUid;
    private final ActivityStartController mController;
    private boolean mDoResume;
    private IHwActivityStarterEx mHwActivityStarterEx;
    private TaskRecord mInTask;
    private Intent mIntent;
    private boolean mIntentDelivered;
    private final ActivityStartInterceptor mInterceptor;
    private boolean mKeepCurTransition;
    private final ActivityRecord[] mLastStartActivityRecord = new ActivityRecord[1];
    private int mLastStartActivityResult;
    private long mLastStartActivityTimeMs;
    private String mLastStartReason;
    private int mLaunchFlags;
    private int mLaunchMode;
    private LaunchParamsController.LaunchParams mLaunchParams = new LaunchParamsController.LaunchParams();
    private boolean mLaunchTaskBehind;
    private boolean mMovedToFront;
    private ActivityInfo mNewTaskInfo;
    private Intent mNewTaskIntent;
    private boolean mNoAnimation;
    private ActivityRecord mNotTop;
    protected ActivityOptions mOptions;
    private int mPreferredDisplayId;
    private Request mRequest = new Request();
    private boolean mRestrictedBgActivity;
    private TaskRecord mReuseTask;
    private final RootActivityContainer mRootActivityContainer;
    final ActivityTaskManagerService mService;
    private boolean mShouldSkipStartingWindow;
    private ActivityRecord mSourceRecord;
    private ActivityStack mSourceStack;
    private ActivityRecord mStartActivity;
    private int mStartFlags;
    final ActivityStackSupervisor mSupervisor;
    private ActivityStack mTargetStack;
    private IVoiceInteractor mVoiceInteractor;
    private IVoiceInteractionSession mVoiceSession;

    @VisibleForTesting
    interface Factory {
        ActivityStarter obtain();

        void recycle(ActivityStarter activityStarter);

        void setController(ActivityStartController activityStartController);
    }

    static class DefaultFactory implements Factory {
        private final int MAX_STARTER_COUNT = 3;
        private ActivityStartController mController;
        private ActivityStartInterceptor mInterceptor;
        private ActivityTaskManagerService mService;
        private Pools.SynchronizedPool<ActivityStarter> mStarterPool = new Pools.SynchronizedPool<>(3);
        private ActivityStackSupervisor mSupervisor;

        DefaultFactory(ActivityTaskManagerService service, ActivityStackSupervisor supervisor, ActivityStartInterceptor interceptor) {
            this.mService = service;
            this.mSupervisor = supervisor;
            this.mInterceptor = interceptor;
        }

        @Override // com.android.server.wm.ActivityStarter.Factory
        public void setController(ActivityStartController controller) {
            this.mController = controller;
        }

        @Override // com.android.server.wm.ActivityStarter.Factory
        public ActivityStarter obtain() {
            ActivityStarter starter = (ActivityStarter) this.mStarterPool.acquire();
            if (starter != null) {
                return starter;
            }
            HwServiceFactory.IHwActivityStarter iActivitySt = HwServiceFactory.getHwActivityStarter();
            if (iActivitySt != null) {
                return iActivitySt.getInstance(this.mController, this.mService, this.mSupervisor, this.mInterceptor);
            }
            return new ActivityStarter(this.mController, this.mService, this.mSupervisor, this.mInterceptor);
        }

        @Override // com.android.server.wm.ActivityStarter.Factory
        public void recycle(ActivityStarter starter) {
            starter.reset(true);
            this.mStarterPool.release(starter);
        }
    }

    private static class Request {
        private static final int DEFAULT_CALLING_PID = 0;
        private static final int DEFAULT_CALLING_UID = -1;
        static final int DEFAULT_REAL_CALLING_PID = 0;
        static final int DEFAULT_REAL_CALLING_UID = -1;
        ActivityInfo activityInfo;
        SafeActivityOptions activityOptions;
        boolean allowBackgroundActivityStart;
        boolean allowPendingRemoteAnimationRegistryLookup;
        boolean avoidMoveToFront;
        IApplicationThread caller;
        String callingPackage;
        int callingPid = 0;
        int callingUid = -1;
        boolean componentSpecified;
        Intent ephemeralIntent;
        int filterCallingUid;
        Configuration globalConfig;
        boolean ignoreTargetSecurity;
        TaskRecord inTask;
        Intent intent;
        boolean mayWait;
        PendingIntentRecord originatingPendingIntent;
        ActivityRecord[] outActivity;
        ProfilerInfo profilerInfo;
        int realCallingPid = 0;
        int realCallingUid = -1;
        String reason;
        int requestCode;
        ResolveInfo resolveInfo;
        String resolvedType;
        IBinder resultTo;
        String resultWho;
        int startFlags;
        int userId;
        IVoiceInteractor voiceInteractor;
        IVoiceInteractionSession voiceSession;
        WaitResult waitResult;

        Request() {
            reset();
        }

        /* access modifiers changed from: package-private */
        public void reset() {
            this.caller = null;
            this.intent = null;
            this.ephemeralIntent = null;
            this.resolvedType = null;
            this.activityInfo = null;
            this.resolveInfo = null;
            this.voiceSession = null;
            this.voiceInteractor = null;
            this.resultTo = null;
            this.resultWho = null;
            this.requestCode = 0;
            this.callingPid = 0;
            this.callingUid = -1;
            this.callingPackage = null;
            this.realCallingPid = 0;
            this.realCallingUid = -1;
            this.startFlags = 0;
            this.activityOptions = null;
            this.ignoreTargetSecurity = false;
            this.componentSpecified = false;
            this.outActivity = null;
            this.inTask = null;
            this.reason = null;
            this.profilerInfo = null;
            this.globalConfig = null;
            this.userId = 0;
            this.waitResult = null;
            this.mayWait = false;
            this.avoidMoveToFront = false;
            this.allowPendingRemoteAnimationRegistryLookup = true;
            this.filterCallingUid = -10000;
            this.originatingPendingIntent = null;
            this.allowBackgroundActivityStart = false;
        }

        /* access modifiers changed from: package-private */
        public void set(Request request) {
            this.caller = request.caller;
            this.intent = request.intent;
            this.ephemeralIntent = request.ephemeralIntent;
            this.resolvedType = request.resolvedType;
            this.activityInfo = request.activityInfo;
            this.resolveInfo = request.resolveInfo;
            this.voiceSession = request.voiceSession;
            this.voiceInteractor = request.voiceInteractor;
            this.resultTo = request.resultTo;
            this.resultWho = request.resultWho;
            this.requestCode = request.requestCode;
            this.callingPid = request.callingPid;
            this.callingUid = request.callingUid;
            this.callingPackage = request.callingPackage;
            this.realCallingPid = request.realCallingPid;
            this.realCallingUid = request.realCallingUid;
            this.startFlags = request.startFlags;
            this.activityOptions = request.activityOptions;
            this.ignoreTargetSecurity = request.ignoreTargetSecurity;
            this.componentSpecified = request.componentSpecified;
            this.outActivity = request.outActivity;
            this.inTask = request.inTask;
            this.reason = request.reason;
            this.profilerInfo = request.profilerInfo;
            this.globalConfig = request.globalConfig;
            this.userId = request.userId;
            this.waitResult = request.waitResult;
            this.mayWait = request.mayWait;
            this.avoidMoveToFront = request.avoidMoveToFront;
            this.allowPendingRemoteAnimationRegistryLookup = request.allowPendingRemoteAnimationRegistryLookup;
            this.filterCallingUid = request.filterCallingUid;
            this.originatingPendingIntent = request.originatingPendingIntent;
            this.allowBackgroundActivityStart = request.allowBackgroundActivityStart;
        }
    }

    ActivityStarter(ActivityStartController controller, ActivityTaskManagerService service, ActivityStackSupervisor supervisor, ActivityStartInterceptor interceptor) {
        this.mController = controller;
        this.mService = service;
        this.mRootActivityContainer = service.mRootActivityContainer;
        this.mSupervisor = supervisor;
        this.mInterceptor = interceptor;
        reset(true);
        this.mHwActivityStarterEx = HwServiceExFactory.getHwActivityStarterEx(service);
    }

    /* access modifiers changed from: package-private */
    public void set(ActivityStarter starter) {
        this.mStartActivity = starter.mStartActivity;
        this.mIntent = starter.mIntent;
        this.mCallingUid = starter.mCallingUid;
        this.mOptions = starter.mOptions;
        this.mRestrictedBgActivity = starter.mRestrictedBgActivity;
        this.mLaunchTaskBehind = starter.mLaunchTaskBehind;
        this.mLaunchFlags = starter.mLaunchFlags;
        this.mLaunchMode = starter.mLaunchMode;
        this.mLaunchParams.set(starter.mLaunchParams);
        this.mNotTop = starter.mNotTop;
        this.mDoResume = starter.mDoResume;
        this.mStartFlags = starter.mStartFlags;
        this.mSourceRecord = starter.mSourceRecord;
        this.mPreferredDisplayId = starter.mPreferredDisplayId;
        this.mInTask = starter.mInTask;
        this.mAddingToTask = starter.mAddingToTask;
        this.mReuseTask = starter.mReuseTask;
        this.mNewTaskInfo = starter.mNewTaskInfo;
        this.mNewTaskIntent = starter.mNewTaskIntent;
        this.mSourceStack = starter.mSourceStack;
        this.mTargetStack = starter.mTargetStack;
        this.mMovedToFront = starter.mMovedToFront;
        this.mNoAnimation = starter.mNoAnimation;
        this.mKeepCurTransition = starter.mKeepCurTransition;
        this.mAvoidMoveToFront = starter.mAvoidMoveToFront;
        this.mVoiceSession = starter.mVoiceSession;
        this.mVoiceInteractor = starter.mVoiceInteractor;
        this.mIntentDelivered = starter.mIntentDelivered;
        this.mRequest.set(starter.mRequest);
    }

    /* access modifiers changed from: package-private */
    public ActivityRecord getStartActivity() {
        return this.mStartActivity;
    }

    /* access modifiers changed from: package-private */
    public boolean relatedToPackage(String packageName) {
        ActivityRecord activityRecord;
        ActivityRecord[] activityRecordArr = this.mLastStartActivityRecord;
        if ((activityRecordArr[0] == null || !packageName.equals(activityRecordArr[0].packageName)) && ((activityRecord = this.mStartActivity) == null || !packageName.equals(activityRecord.packageName))) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public int execute() {
        try {
            if (this.mRequest.mayWait) {
                return startActivityMayWait(this.mRequest.caller, this.mRequest.callingUid, this.mRequest.callingPackage, this.mRequest.realCallingPid, this.mRequest.realCallingUid, this.mRequest.intent, this.mRequest.resolvedType, this.mRequest.voiceSession, this.mRequest.voiceInteractor, this.mRequest.resultTo, this.mRequest.resultWho, this.mRequest.requestCode, this.mRequest.startFlags, this.mRequest.profilerInfo, this.mRequest.waitResult, this.mRequest.globalConfig, this.mRequest.activityOptions, this.mRequest.ignoreTargetSecurity, this.mRequest.userId, this.mRequest.inTask, this.mRequest.reason, this.mRequest.allowPendingRemoteAnimationRegistryLookup, this.mRequest.originatingPendingIntent, this.mRequest.allowBackgroundActivityStart);
            }
            int startActivity = startActivity(this.mRequest.caller, this.mRequest.intent, this.mRequest.ephemeralIntent, this.mRequest.resolvedType, this.mRequest.activityInfo, this.mRequest.resolveInfo, this.mRequest.voiceSession, this.mRequest.voiceInteractor, this.mRequest.resultTo, this.mRequest.resultWho, this.mRequest.requestCode, this.mRequest.callingPid, this.mRequest.callingUid, this.mRequest.callingPackage, this.mRequest.realCallingPid, this.mRequest.realCallingUid, this.mRequest.startFlags, this.mRequest.activityOptions, this.mRequest.ignoreTargetSecurity, this.mRequest.componentSpecified, this.mRequest.outActivity, this.mRequest.inTask, this.mRequest.reason, this.mRequest.allowPendingRemoteAnimationRegistryLookup, this.mRequest.originatingPendingIntent, this.mRequest.allowBackgroundActivityStart);
            onExecutionComplete();
            return startActivity;
        } finally {
            onExecutionComplete();
        }
    }

    /* access modifiers changed from: package-private */
    public int startResolvedActivity(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask) {
        try {
            this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunching(r.intent);
            this.mLastStartReason = "startResolvedActivity";
            this.mLastStartActivityTimeMs = System.currentTimeMillis();
            this.mLastStartActivityRecord[0] = r;
            this.mLastStartActivityResult = startActivity(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, this.mLastStartActivityRecord, false);
            this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(this.mLastStartActivityResult, this.mLastStartActivityRecord[0]);
            return this.mLastStartActivityResult;
        } finally {
            onExecutionComplete();
        }
    }

    /* access modifiers changed from: package-private */
    public int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, SafeActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity, TaskRecord inTask, String reason, boolean allowPendingRemoteAnimationRegistryLookup, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        if (!TextUtils.isEmpty(reason)) {
            this.mLastStartReason = reason;
            this.mLastStartActivityTimeMs = System.currentTimeMillis();
            ActivityRecord[] activityRecordArr = this.mLastStartActivityRecord;
            activityRecordArr[0] = null;
            this.mLastStartActivityResult = startActivity(caller, intent, ephemeralIntent, resolvedType, aInfo, rInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity, componentSpecified, activityRecordArr, inTask, allowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, allowBackgroundActivityStart);
            if (outActivity != null) {
                outActivity[0] = this.mLastStartActivityRecord[0];
            }
            return getExternalResult(this.mLastStartActivityResult);
        }
        throw new IllegalArgumentException("Need to specify a reason.");
    }

    static int getExternalResult(int result) {
        if (result != 102) {
            return result;
        }
        return 0;
    }

    private void onExecutionComplete() {
        this.mController.onExecutionComplete(this);
    }

    /* JADX WARNING: Removed duplicated region for block: B:183:0x0443  */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0447  */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x048c  */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x04b6  */
    /* JADX WARNING: Removed duplicated region for block: B:194:0x04c5 A[ADDED_TO_REGION] */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x04de  */
    /* JADX WARNING: Removed duplicated region for block: B:231:0x066a A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x066c  */
    private int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, SafeActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity, TaskRecord inTask, boolean allowPendingRemoteAnimationRegistryLookup, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        int callingPid2;
        Bundle verificationBundle;
        int i;
        String str;
        int callingUid2;
        WindowProcessController callerApp;
        String str2;
        WindowProcessController callerApp2;
        String str3;
        ActivityRecord sourceRecord;
        int requestCode2;
        ActivityRecord resultRecord;
        String resultWho2;
        ActivityStack activityStack;
        int err;
        String callingPackage2;
        boolean restrictedBgActivity;
        String str4;
        ActivityRecord sourceRecord2;
        String str5;
        ActivityInfo aInfo2;
        int i2;
        WindowProcessController callerApp3;
        ActivityOptions checkedOptions;
        ActivityOptions checkedOptions2;
        String callingPackage3;
        boolean abort;
        boolean isIntercepted;
        ActivityOptions checkedOptions3;
        TaskRecord inTask2;
        int callingUid3;
        int callingPid3;
        String resolvedType2;
        ResolveInfo rInfo2;
        int i3;
        int userId;
        Intent intent2;
        boolean z;
        ActivityInfo aInfo3;
        String str6;
        ResolveInfo rInfo3;
        ActivityRecord sourceRecord3;
        boolean z2;
        int callingPid4;
        String resolvedType3;
        Bundle verificationBundle2;
        int callingUid4;
        Intent intent3;
        ActivityInfo aInfo4;
        ActivityStarter activityStarter;
        ActivityRecord sourceRecord4;
        String str7;
        ResolveInfo rInfo4;
        long j;
        String startActivityInfo;
        Intent intent4 = intent;
        String callingPackage4 = callingPackage;
        this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunching(intent4);
        int err2 = 0;
        Bundle verificationBundle3 = options != null ? options.popAppVerificationBundle() : null;
        if (aInfo != null) {
            this.mService.mHwATMSEx.noteActivityStart(aInfo.applicationInfo.packageName, aInfo.processName, intent.getComponent() != null ? intent.getComponent().getClassName() : "NULL", 0, aInfo.applicationInfo.uid, true);
        }
        if (!this.mHwActivityStarterEx.isAbleToLaunchInVr(this.mService.mContext, intent4, callingPackage4, aInfo)) {
            return 0;
        }
        if (caller != null) {
            WindowProcessController callerApp4 = this.mService.getProcessController(caller);
            if (callerApp4 != null) {
                int callingPid5 = callerApp4.getPid();
                int callingUid5 = callerApp4.mInfo.uid;
                long restoreCurId = Binder.clearCallingIdentity();
                try {
                    try {
                        verificationBundle = verificationBundle3;
                        i = 0;
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(restoreCurId);
                        throw th;
                    }
                    try {
                        if (!this.mService.mHwATMSEx.isAllowToStartActivity(this.mService.mContext, callerApp4.mInfo.packageName, aInfo, this.mService.isSleepingLocked(), ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getLastResumedActivity())) {
                            Binder.restoreCallingIdentity(restoreCurId);
                            return 0;
                        }
                        Binder.restoreCallingIdentity(restoreCurId);
                        callingUid2 = callingUid5;
                        callerApp = callerApp4;
                        str = "ActivityTaskManager";
                        callingPid2 = callingPid5;
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(restoreCurId);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Binder.restoreCallingIdentity(restoreCurId);
                    throw th;
                }
            } else {
                verificationBundle = verificationBundle3;
                i = 0;
                str = "ActivityTaskManager";
                Slog.w(str, "Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting: " + intent.toString());
                err2 = -94;
                callingUid2 = callingUid;
                callingPid2 = callingPid;
                callerApp = callerApp4;
            }
        } else {
            str = "ActivityTaskManager";
            verificationBundle = verificationBundle3;
            i = 0;
            callingUid2 = callingUid;
            callerApp = null;
            callingPid2 = callingPid;
        }
        HwFreeFormManager.getInstance(this.mService.mContext).removeFloatListView();
        int userId2 = (aInfo == null || aInfo.applicationInfo == null) ? i : UserHandle.getUserId(aInfo.applicationInfo.uid);
        if (err2 == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("START u");
            sb.append(userId2);
            sb.append(" {");
            str2 = " {";
            callerApp2 = callerApp;
            sb.append(intent4.toShortStringWithoutClip(true, true, true));
            sb.append("} from uid ");
            sb.append(callingUid2);
            Slog.i(str, sb.toString());
            if (!this.mService.mActivityIdle) {
                if (intent.getComponent() != null) {
                    startActivityInfo = "START u" + userId2 + " " + intent.getComponent().toShortString() + " from uid " + callingUid2;
                } else {
                    startActivityInfo = "start activity";
                }
                HwBootCheck.addBootInfo(startActivityInfo);
            }
            if (this.mService.mHwATMSEx.showIncompatibleAppDialog(aInfo, callingPackage4)) {
                return WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT;
            }
            this.mSupervisor.recognitionMaliciousApp(caller, intent4, userId2);
            if (ACTION_HWCHOOSER.equals(intent.getAction()) && intent4.getBooleanExtra(EXTRA_ALWAYS_USE_OPTION, false) && !HWPCEXPLORER_PACKAGE_NAME.equals(callingPackage4)) {
                intent4.putExtra(EXTRA_ALWAYS_USE_OPTION, false);
            }
            this.mHwActivityStarterEx.effectiveIawareToLaunchApp(intent4, aInfo, this.mService.getActivityStartController().mCurActivityPkName);
        } else {
            str2 = " {";
            callerApp2 = callerApp;
        }
        ActivityRecord resultRecord2 = null;
        if (resultTo != null) {
            ActivityRecord sourceRecord5 = this.mRootActivityContainer.isInAnyStack(resultTo);
            if (ActivityTaskManagerDebugConfig.DEBUG_RESULTS) {
                StringBuilder sb2 = new StringBuilder();
                str3 = "START u";
                sb2.append("Will send result to ");
                sb2.append(resultTo);
                sb2.append(" ");
                sb2.append(sourceRecord5);
                Slog.v(str, sb2.toString());
            } else {
                str3 = "START u";
            }
            if (sourceRecord5 == null || requestCode < 0 || sourceRecord5.finishing) {
                sourceRecord = sourceRecord5;
            } else {
                resultRecord2 = sourceRecord5;
                sourceRecord = sourceRecord5;
            }
        } else {
            str3 = "START u";
            sourceRecord = null;
        }
        int launchFlags = intent.getFlags();
        if ((launchFlags & 33554432) == 0 || sourceRecord == null) {
            activityStack = null;
            resultWho2 = resultWho;
            requestCode2 = requestCode;
            resultRecord = resultRecord2;
        } else if (requestCode >= 0) {
            SafeActivityOptions.abort(options);
            return -93;
        } else {
            ActivityRecord resultRecord3 = sourceRecord.resultTo;
            if (resultRecord3 != null && !resultRecord3.isInStackLocked()) {
                resultRecord3 = null;
            }
            String resultWho3 = sourceRecord.resultWho;
            int requestCode3 = sourceRecord.requestCode;
            sourceRecord.resultTo = null;
            if (resultRecord3 != null) {
                resultRecord3.removeResultsLocked(sourceRecord, resultWho3, requestCode3);
            }
            if (sourceRecord.launchedFromUid == callingUid2) {
                resultRecord = resultRecord3;
                resultWho2 = resultWho3;
                requestCode2 = requestCode3;
                callingPackage4 = sourceRecord.launchedFromPackage;
                activityStack = null;
            } else {
                resultRecord = resultRecord3;
                resultWho2 = resultWho3;
                requestCode2 = requestCode3;
                activityStack = null;
            }
        }
        if (err2 == 0 && intent.getComponent() == null) {
            err2 = -91;
        }
        if (err2 == 0 && aInfo == null) {
            err2 = -92;
        }
        if (!(err2 != 0 || sourceRecord == null || sourceRecord.getTaskRecord().voiceSession == null || (launchFlags & 268435456) != 0 || sourceRecord.info.applicationInfo.uid == aInfo.applicationInfo.uid)) {
            try {
                intent4.addCategory("android.intent.category.VOICE");
                if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent4, resolvedType)) {
                    Slog.w(str, "Activity being started in current voice task does not support voice: " + intent4);
                    err2 = -97;
                }
            } catch (RemoteException e) {
                Slog.w(str, "Failure checking voice capabilities", e);
                err2 = -97;
            }
        }
        if (err2 != 0 || voiceSession == null) {
            err = err2;
        } else {
            try {
                if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent4, resolvedType)) {
                    Slog.w(str, "Activity being started in new voice task does not support: " + intent4);
                    err2 = -97;
                }
                err = err2;
            } catch (RemoteException e2) {
                Slog.w(str, "Failure checking voice capabilities", e2);
                err = -97;
            }
        }
        ActivityStack resultStack = resultRecord == null ? activityStack : resultRecord.getActivityStack();
        if (err != 0) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho2, requestCode2, 0, null);
            }
            SafeActivityOptions.abort(options);
            return err;
        }
        boolean abort2 = (!this.mSupervisor.checkStartAnyActivityPermission(intent, aInfo, resultWho2, requestCode2, callingPid2, callingUid2, callingPackage4, ignoreTargetSecurity, inTask != null, callerApp2, resultRecord, resultStack)) | (!this.mService.mIntentFirewall.checkStartActivity(intent, callingUid2, callingPid2, resolvedType, aInfo.applicationInfo)) | (!this.mService.getPermissionPolicyInternal().checkStartActivity(intent4, callingUid2, callingPackage4));
        if (!abort2) {
            try {
                Trace.traceBegin(64, "shouldAbortBackgroundActivityStart");
                j = 64;
                callingPackage2 = callingPackage4;
                str4 = str;
                str5 = str3;
                i2 = realCallingUid;
                sourceRecord2 = sourceRecord;
                aInfo2 = aInfo;
                try {
                    boolean restrictedBgActivity2 = shouldAbortBackgroundActivityStart(callingUid2, callingPid2, callingPackage4, realCallingUid, realCallingPid, callerApp2, originatingPendingIntent, allowBackgroundActivityStart, intent);
                    Trace.traceEnd(64);
                    restrictedBgActivity = restrictedBgActivity2;
                } catch (Throwable th4) {
                    th = th4;
                    Trace.traceEnd(j);
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                j = 64;
                Trace.traceEnd(j);
                throw th;
            }
        } else {
            callingPackage2 = callingPackage4;
            str4 = str;
            str5 = str3;
            sourceRecord2 = sourceRecord;
            i2 = realCallingUid;
            aInfo2 = aInfo;
            restrictedBgActivity = false;
        }
        if (options != null) {
            callerApp3 = callerApp2;
            checkedOptions = options.getOptions(intent4, aInfo2, callerApp3, this.mSupervisor);
        } else {
            callerApp3 = callerApp2;
            checkedOptions = null;
        }
        if (!abort2 && this.mService.mHwATMSEx.shouldPreventStartActivity(aInfo, callingUid2, callingPid2, callingPackage2, userId2, intent, callerApp3)) {
            abort2 = true;
        }
        if (allowPendingRemoteAnimationRegistryLookup) {
            callingPackage3 = callingPackage2;
            checkedOptions2 = this.mService.getActivityStartController().getPendingRemoteAnimationRegistry().overrideOptionsIfNeeded(callingPackage3, checkedOptions);
        } else {
            callingPackage3 = callingPackage2;
            checkedOptions2 = checkedOptions;
        }
        if (this.mService.mController != null) {
            try {
                abort = abort2 | (!this.mService.mController.activityStarting(intent.cloneFilter(), aInfo2.applicationInfo.packageName));
            } catch (RemoteException e3) {
                this.mService.mController = null;
            }
            if (!"startActivityAsCaller".equals(this.mLastStartReason)) {
                intent4.setCallingUid(callingUid2);
            } else if (!"PendingIntentRecord".equals(this.mLastStartReason) || intent.getCallingUid() == 0) {
                intent4.setCallingUid(i2);
            }
            this.mInterceptor.setStates(userId2, realCallingPid, realCallingUid, startFlags, callingPackage3);
            this.mInterceptor.setSourceRecord(sourceRecord2);
            if (!this.mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask, callingPid2, callingUid2, checkedOptions2)) {
                Intent intent5 = this.mInterceptor.mIntent;
                rInfo2 = this.mInterceptor.mRInfo;
                ActivityInfo aInfo5 = this.mInterceptor.mAInfo;
                resolvedType2 = this.mInterceptor.mResolvedType;
                TaskRecord inTask3 = this.mInterceptor.mInTask;
                callingPid3 = this.mInterceptor.mCallingPid;
                callingUid3 = this.mInterceptor.mCallingUid;
                intent4 = intent5;
                aInfo2 = aInfo5;
                inTask2 = inTask3;
                checkedOptions3 = this.mInterceptor.mActivityOptions;
                isIntercepted = true;
            } else {
                resolvedType2 = resolvedType;
                rInfo2 = rInfo;
                callingUid3 = callingUid2;
                isIntercepted = false;
                checkedOptions3 = checkedOptions2;
                callingPid3 = callingPid2;
                inTask2 = inTask;
            }
            if (!abort) {
                if (!(resultRecord == null || resultStack == null)) {
                    resultStack.sendActivityResultLocked(-1, resultRecord, resultWho2, requestCode2, 0, null);
                }
                ActivityOptions.abort(checkedOptions3);
                return WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT;
            } else if (!this.mHwActivityStarterEx.isAbleToLaunchVideoActivity(this.mService.mContext, intent4)) {
                SafeActivityOptions.abort(options);
                return 0;
            } else {
                if (aInfo2 != null) {
                    userId = userId2;
                    if (this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(aInfo2.packageName, userId)) {
                        IIntentSender target = this.mService.getIntentSenderLocked(2, callingPackage3, callingUid3, userId, null, null, 0, new Intent[]{intent4}, new String[]{resolvedType2}, 1342177280, null);
                        Intent newIntent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
                        int flags = intent4.getFlags() | 8388608;
                        if ((268959744 & flags) != 0) {
                            flags |= 134217728;
                        }
                        newIntent.setFlags(flags);
                        newIntent.putExtra("android.intent.extra.PACKAGE_NAME", aInfo2.packageName);
                        newIntent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
                        if (resultRecord != null) {
                            newIntent.putExtra("android.intent.extra.RESULT_NEEDED", true);
                        }
                        intent2 = newIntent;
                        resolvedType2 = null;
                        callingUid3 = realCallingUid;
                        callingPid3 = realCallingPid;
                        ResolveInfo rInfo5 = this.mSupervisor.resolveIntent(intent2, null, userId, 0, computeResolveFilterUid(callingUid3, i2, this.mRequest.filterCallingUid));
                        i3 = startFlags;
                        ActivityInfo aInfo6 = this.mSupervisor.resolveActivity(intent2, rInfo5, i3, null);
                        if (ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                            ActivityStack focusedStack = this.mRootActivityContainer.getTopDisplayFocusedStack();
                            StringBuilder sb3 = new StringBuilder();
                            rInfo4 = rInfo5;
                            sb3.append(str5);
                            sb3.append(userId);
                            sb3.append(str2);
                            sb3.append(intent2.toShortString(true, true, true, false));
                            sb3.append("} from uid ");
                            sb3.append(callingUid3);
                            sb3.append(" on display ");
                            sb3.append(focusedStack == null ? 0 : focusedStack.mDisplayId);
                            String sb4 = sb3.toString();
                            str6 = str4;
                            Slog.i(str6, sb4);
                        } else {
                            rInfo4 = rInfo5;
                            str6 = str4;
                        }
                        rInfo3 = rInfo4;
                        aInfo3 = aInfo6;
                        z = true;
                        if (rInfo3 != null || rInfo3.auxiliaryInfo == null) {
                            z2 = z;
                            activityStarter = this;
                            verificationBundle2 = verificationBundle;
                            sourceRecord3 = sourceRecord2;
                            aInfo4 = aInfo3;
                            resolvedType3 = resolvedType2;
                            callingPid4 = callingPid3;
                            callingUid4 = callingUid3;
                            intent3 = intent2;
                        } else {
                            activityStarter = this;
                            z2 = z;
                            verificationBundle2 = verificationBundle;
                            sourceRecord3 = sourceRecord2;
                            Intent intent6 = createLaunchIntent(rInfo3.auxiliaryInfo, ephemeralIntent, callingPackage3, verificationBundle2, resolvedType2, userId);
                            aInfo4 = activityStarter.mSupervisor.resolveActivity(intent6, rInfo3, i3, null);
                            resolvedType3 = null;
                            callingPid4 = realCallingPid;
                            callingUid4 = realCallingUid;
                            intent3 = intent6;
                        }
                        Slog.i(str6, "ActivityRecord info: " + aInfo4);
                        if (!isNeedToStartExSplash(intent3, callingPackage3, requestCode2, restrictedBgActivity, isIntercepted, aInfo4, checkedOptions3, callingUid4, userId, resolvedType3)) {
                            return 0;
                        }
                        ActivityTaskManagerService activityTaskManagerService = activityStarter.mService;
                        ActivityRecord r = HwServiceFactory.createActivityRecord(activityTaskManagerService, callerApp3, callingPid4, callingUid4, callingPackage3, intent3, resolvedType3, aInfo4, activityTaskManagerService.getGlobalConfiguration(), resultRecord, resultWho2, requestCode2, componentSpecified, voiceSession != null ? z2 : false, activityStarter.mSupervisor, checkedOptions3, sourceRecord3);
                        if (outActivity != null) {
                            outActivity[0] = r;
                        }
                        activityStarter.mService.mHwATMSEx.setCallingPkg(callingPackage3);
                        if (r.appTimeTracker == null) {
                            sourceRecord4 = sourceRecord3;
                            if (sourceRecord4 != null) {
                                r.appTimeTracker = sourceRecord4.appTimeTracker;
                            }
                        } else {
                            sourceRecord4 = sourceRecord3;
                        }
                        ActivityStack stack = activityStarter.mRootActivityContainer.getTopDisplayFocusedStack();
                        if (voiceSession == null) {
                            if (stack.getResumedActivity() != null) {
                                if (stack.getResumedActivity().info.applicationInfo.uid == realCallingUid) {
                                }
                            }
                            if (!activityStarter.mService.checkAppSwitchAllowedLocked(callingPid4, callingUid4, realCallingPid, realCallingUid, "Activity start")) {
                                if (!restrictedBgActivity || !activityStarter.handleBackgroundActivityAbort(r)) {
                                    activityStarter.mController.addPendingActivityLaunch(new ActivityStackSupervisor.PendingActivityLaunch(r, sourceRecord4, startFlags, stack, callerApp3));
                                }
                                ActivityOptions.abort(checkedOptions3);
                                return 100;
                            }
                        }
                        activityStarter.mService.onStartActivitySetDidAppSwitch();
                        activityStarter.mController.doPendingActivityLaunches(false);
                        int res = startActivity(r, sourceRecord4, voiceSession, voiceInteractor, startFlags, true, checkedOptions3, inTask2, outActivity, restrictedBgActivity);
                        activityStarter.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res, outActivity[0]);
                        return res;
                    }
                    i3 = startFlags;
                    str7 = str4;
                    z = true;
                } else {
                    userId = userId2;
                    i3 = startFlags;
                    str7 = str4;
                    z = true;
                }
                rInfo3 = rInfo2;
                intent2 = intent4;
                aInfo3 = aInfo2;
                if (rInfo3 != null) {
                }
                z2 = z;
                activityStarter = this;
                verificationBundle2 = verificationBundle;
                sourceRecord3 = sourceRecord2;
                aInfo4 = aInfo3;
                resolvedType3 = resolvedType2;
                callingPid4 = callingPid3;
                callingUid4 = callingUid3;
                intent3 = intent2;
                Slog.i(str6, "ActivityRecord info: " + aInfo4);
                if (!isNeedToStartExSplash(intent3, callingPackage3, requestCode2, restrictedBgActivity, isIntercepted, aInfo4, checkedOptions3, callingUid4, userId, resolvedType3)) {
                }
            }
        }
        abort = abort2;
        if (!"startActivityAsCaller".equals(this.mLastStartReason)) {
        }
        this.mInterceptor.setStates(userId2, realCallingPid, realCallingUid, startFlags, callingPackage3);
        this.mInterceptor.setSourceRecord(sourceRecord2);
        if (!this.mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask, callingPid2, callingUid2, checkedOptions2)) {
        }
        if (!abort) {
        }
    }

    private boolean isNeedToStartExSplash(Intent intent, String callingPackage, int requestCode, boolean isRestrictedBgActivity, boolean isIntercepted, ActivityInfo info, ActivityOptions checkedOptions, int callingUid, int userId, String resolvedType) {
        if (intent != null) {
            if (intent.getComponent() != null) {
                String packageName = intent.getComponent().getPackageName();
                Bundle checkBundle = new Bundle();
                checkBundle.putString("exsplash_callingpackage", callingPackage);
                checkBundle.putString("exsplash_package", packageName);
                checkBundle.putInt("exsplash_requestcode", requestCode);
                checkBundle.putBoolean("exsplash_isintercepted", isRestrictedBgActivity || isIntercepted);
                checkBundle.putParcelable("exsplash_info", info);
                if (!this.mService.mHwATMSEx.isExSplashEnable(checkBundle)) {
                    return false;
                }
                long restoreCurId = Binder.clearCallingIdentity();
                try {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("android.intent.extra.INTENT", intent);
                    bundle.putString("exsplash_callingpackage", callingPackage);
                    try {
                        bundle.putInt("exsplash_callingUid", callingUid);
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(restoreCurId);
                        throw th;
                    }
                    try {
                        bundle.putInt("exsplash_userId", userId);
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(restoreCurId);
                        throw th;
                    }
                    try {
                        bundle.putString("exsplash_resolvedType", resolvedType);
                        try {
                            this.mService.mHwATMSEx.startExSplash(bundle, checkedOptions);
                            Binder.restoreCallingIdentity(restoreCurId);
                            return true;
                        } catch (Throwable th3) {
                            th = th3;
                            Binder.restoreCallingIdentity(restoreCurId);
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        Binder.restoreCallingIdentity(restoreCurId);
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    Binder.restoreCallingIdentity(restoreCurId);
                    throw th;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean shouldAbortBackgroundActivityStart(int callingUid, int callingPid, String callingPackage, int realCallingUid, int realCallingPid, WindowProcessController callerApp, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart, Intent intent) {
        int realCallingUidProcState;
        boolean realCallingUidHasAnyVisibleWindow;
        boolean isRealCallingUidForeground;
        boolean isRealCallingUidPersistentSystemProcess;
        boolean z;
        int callerAppUid;
        WindowProcessController callerApp2;
        int callingUserId;
        int realCallingUidProcState2;
        boolean z2;
        int callingAppId = UserHandle.getAppId(callingUid);
        if (callingUid == 0 || callingAppId == 1000) {
            return false;
        }
        if (callingAppId == 1027) {
            return false;
        }
        int callingUidProcState = this.mService.getUidState(callingUid);
        boolean callingUidHasAnyVisibleWindow = this.mService.mWindowManager.mRoot.isAnyNonToastWindowVisibleForUid(callingUid);
        boolean isCallingUidForeground = callingUidHasAnyVisibleWindow || callingUidProcState == 2 || callingUidProcState == 4;
        boolean isCallingUidPersistentSystemProcess = callingUidProcState <= 1;
        if (callingUidHasAnyVisibleWindow) {
            return false;
        }
        if (isCallingUidPersistentSystemProcess) {
            return false;
        }
        if (callingUid == realCallingUid) {
            realCallingUidProcState = callingUidProcState;
        } else {
            realCallingUidProcState = this.mService.getUidState(realCallingUid);
        }
        if (callingUid == realCallingUid) {
            realCallingUidHasAnyVisibleWindow = callingUidHasAnyVisibleWindow;
        } else {
            realCallingUidHasAnyVisibleWindow = this.mService.mWindowManager.mRoot.isAnyNonToastWindowVisibleForUid(realCallingUid);
        }
        if (callingUid == realCallingUid) {
            isRealCallingUidForeground = isCallingUidForeground;
        } else {
            isRealCallingUidForeground = realCallingUidHasAnyVisibleWindow || realCallingUidProcState == 2;
        }
        int realCallingAppId = UserHandle.getAppId(realCallingUid);
        if (callingUid == realCallingUid) {
            isRealCallingUidPersistentSystemProcess = isCallingUidPersistentSystemProcess;
        } else {
            isRealCallingUidPersistentSystemProcess = realCallingAppId == 1000 || realCallingUidProcState <= 1;
        }
        if (realCallingUid == callingUid) {
            z = false;
        } else if (realCallingUidHasAnyVisibleWindow) {
            return false;
        } else {
            z = false;
            if ((isRealCallingUidPersistentSystemProcess && allowBackgroundActivityStart) || this.mService.isAssociatedCompanionApp(UserHandle.getUserId(realCallingUid), realCallingUid)) {
                return false;
            }
        }
        ActivityTaskManagerService activityTaskManagerService = this.mService;
        if (ActivityTaskManagerService.checkPermission("android.permission.START_ACTIVITIES_FROM_BACKGROUND", callingPid, callingUid) == 0 || this.mSupervisor.mRecentTasks.isCallerRecents(callingUid) || this.mService.isDeviceOwner(callingUid)) {
            return z;
        }
        int callingUserId2 = UserHandle.getUserId(callingUid);
        if (this.mService.isAssociatedCompanionApp(callingUserId2, callingUid)) {
            return z;
        }
        if (callerApp == null) {
            callerApp2 = this.mService.getProcessController(realCallingPid, realCallingUid);
            callerAppUid = realCallingUid;
        } else {
            callerApp2 = callerApp;
            callerAppUid = callingUid;
        }
        if (callerApp2 == null) {
            realCallingUidProcState2 = realCallingUidProcState;
            callingUserId = callingUserId2;
            z2 = true;
        } else if (callerApp2.areBackgroundActivityStartsAllowed()) {
            return false;
        } else {
            realCallingUidProcState2 = realCallingUidProcState;
            ArraySet<WindowProcessController> uidProcesses = this.mService.mProcessMap.getProcesses(callerAppUid);
            if (uidProcesses != null) {
                z2 = true;
                callingUserId = callingUserId2;
                int i = uidProcesses.size() - 1;
                while (i >= 0) {
                    WindowProcessController proc = uidProcesses.valueAt(i);
                    if (proc != callerApp2 && proc.areBackgroundActivityStartsAllowed()) {
                        return false;
                    }
                    i--;
                    uidProcesses = uidProcesses;
                }
            } else {
                callingUserId = callingUserId2;
                z2 = true;
            }
        }
        if (this.mService.hasSystemAlertWindowPermission(callingUid, callingPid, callingPackage)) {
            Slog.w("ActivityTaskManager", "Background activity start for " + callingPackage + " allowed because SYSTEM_ALERT_WINDOW permission is granted.");
            return false;
        }
        Slog.w("ActivityTaskManager", "Background activity start [callingPackage: " + callingPackage + "; callingUid: " + callingUid + "; isCallingUidForeground: " + isCallingUidForeground + "; isCallingUidPersistentSystemProcess: " + isCallingUidPersistentSystemProcess + "; realCallingUid: " + realCallingUid + "; isRealCallingUidForeground: " + isRealCallingUidForeground + "; isRealCallingUidPersistentSystemProcess: " + isRealCallingUidPersistentSystemProcess + "; originatingPendingIntent: " + originatingPendingIntent + "; isBgStartWhitelisted: " + allowBackgroundActivityStart + "; intent: " + intent + "; callerApp: " + callerApp2 + "]");
        if (!this.mService.isActivityStartsLoggingEnabled()) {
            return z2;
        }
        this.mSupervisor.getActivityMetricsLogger().logAbortedBgActivityStart(intent, callerApp2, callingUid, callingPackage, callingUidProcState, callingUidHasAnyVisibleWindow, realCallingUid, realCallingUidProcState2, realCallingUidHasAnyVisibleWindow, originatingPendingIntent != null ? z2 : false);
        return z2;
    }

    private Intent createLaunchIntent(AuxiliaryResolveInfo auxiliaryResponse, Intent originalIntent, String callingPackage, Bundle verificationBundle, String resolvedType, int userId) {
        if (auxiliaryResponse != null && auxiliaryResponse.needsPhaseTwo) {
            this.mService.getPackageManagerInternalLocked().requestInstantAppResolutionPhaseTwo(auxiliaryResponse, originalIntent, resolvedType, callingPackage, verificationBundle, userId);
        }
        Intent sanitizeIntent = InstantAppResolver.sanitizeIntent(originalIntent);
        List list = null;
        Intent intent = auxiliaryResponse == null ? null : auxiliaryResponse.failureIntent;
        ComponentName componentName = auxiliaryResponse == null ? null : auxiliaryResponse.installFailureActivity;
        String str = auxiliaryResponse == null ? null : auxiliaryResponse.token;
        boolean z = auxiliaryResponse != null && auxiliaryResponse.needsPhaseTwo;
        if (auxiliaryResponse != null) {
            list = auxiliaryResponse.filters;
        }
        return InstantAppResolver.buildEphemeralInstallerIntent(originalIntent, sanitizeIntent, intent, callingPackage, verificationBundle, resolvedType, userId, componentName, str, z, list);
    }

    /* access modifiers changed from: package-private */
    public void postStartActivityProcessing(ActivityRecord r, int result, ActivityStack startedActivityStack) {
        ActivityStack homeStack;
        if (!ActivityManager.isStartResultFatalError(result)) {
            this.mSupervisor.reportWaitingActivityLaunchedIfNeeded(r, result);
            if (startedActivityStack != null) {
                boolean clearedTask = (this.mLaunchFlags & 268468224) == 268468224 && this.mReuseTask != null;
                if (result == 2 || result == 3 || clearedTask) {
                    int windowingMode = startedActivityStack.getWindowingMode();
                    if (windowingMode == 2) {
                        this.mService.getTaskChangeNotificationController().notifyPinnedActivityRestartAttempt(clearedTask);
                    } else if (windowingMode == 3 && (homeStack = startedActivityStack.getDisplay().getHomeStack()) != null && homeStack.shouldBeVisible(null)) {
                        this.mService.mWindowManager.showRecentApps();
                    }
                }
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v16, resolved type: com.android.server.wm.ActivityTaskManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v15, types: [boolean] */
    /* JADX WARN: Type inference failed for: r0v58 */
    /* JADX WARN: Type inference failed for: r0v89 */
    /* JADX WARN: Type inference failed for: r0v90 */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x0200  */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x0221 A[SYNTHETIC, Splitter:B:107:0x0221] */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x04ab  */
    /* JADX WARNING: Removed duplicated region for block: B:193:0x04d7 A[Catch:{ all -> 0x04d2, all -> 0x056a }] */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x04ea A[SYNTHETIC, Splitter:B:197:0x04ea] */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0124  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0145  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x016e  */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x018d  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0196  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x01c3 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x01c6  */
    public int startActivityMayWait(IApplicationThread caller, int callingUid, String callingPackage, int requestRealCallingPid, int requestRealCallingUid, Intent intent, String resolvedType, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, WaitResult outResult, Configuration globalConfig, SafeActivityOptions options, boolean ignoreTargetSecurity, int userId, TaskRecord inTask, String reason, boolean allowPendingRemoteAnimationRegistryLookup, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        int realCallingPid;
        int realCallingUid;
        int callingUid2;
        int callingUid3;
        boolean componentSpecified;
        int realCallingUid2;
        ResolveInfo rInfo;
        ActivityInfo aInfo;
        WindowManagerGlobalLock windowManagerGlobalLock;
        boolean z;
        long origId;
        boolean componentSpecified2;
        int callingUid4;
        int callingPid;
        ResolveInfo rInfo2;
        ActivityInfo aInfo2;
        ActivityStack stack;
        String resolvedType2;
        Intent intent2;
        IApplicationThread caller2;
        int realCallingUid3;
        ?? r0;
        ActivityStarter activityStarter;
        ActivityInfo aInfo3;
        ResolveInfo rInfo3;
        WindowProcessController heavy;
        int appCallingUid;
        ActivityStack stack2;
        int callingUid5;
        int callingPid2;
        ActivityInfo aInfo4;
        ResolveInfo rInfo4;
        boolean profileLockedAndParentUnlockingOrUnlocked;
        if (intent == null || !intent.hasFileDescriptors()) {
            this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunching(intent);
            boolean componentSpecified3 = intent.getComponent() != null;
            if (requestRealCallingPid != 0) {
                realCallingPid = requestRealCallingPid;
            } else {
                realCallingPid = Binder.getCallingPid();
            }
            if (requestRealCallingUid != -1) {
                realCallingUid = requestRealCallingUid;
            } else {
                realCallingUid = Binder.getCallingUid();
            }
            if (callingUid >= 0) {
                callingUid2 = callingUid;
                callingUid3 = -1;
            } else if (caller == null) {
                callingUid2 = realCallingUid;
                callingUid3 = realCallingPid;
            } else {
                callingUid3 = -1;
                callingUid2 = -1;
            }
            Intent ephemeralIntent = new Intent(intent);
            Intent intent3 = new Intent(intent);
            if (componentSpecified3 && ((!"android.intent.action.VIEW".equals(intent3.getAction()) || intent3.getData() != null) && !"android.intent.action.INSTALL_INSTANT_APP_PACKAGE".equals(intent3.getAction()) && !"android.intent.action.RESOLVE_INSTANT_APP_PACKAGE".equals(intent3.getAction()) && this.mService.getPackageManagerInternalLocked().isInstantAppInstallerComponent(intent3.getComponent()))) {
                intent3.setComponent(null);
                componentSpecified3 = false;
            }
            ResolveInfo rInfo5 = this.mSupervisor.resolveIntent(intent3, resolvedType, userId, 0, computeResolveFilterUid(callingUid2, realCallingUid, this.mRequest.filterCallingUid));
            if (standardizeHomeIntent(rInfo5, intent3)) {
                componentSpecified = false;
            } else {
                componentSpecified = componentSpecified3;
            }
            if (rInfo5 == null) {
                UserInfo userInfo = this.mSupervisor.getUserInfo(userId);
                if (userInfo == null) {
                    rInfo4 = rInfo5;
                    realCallingUid2 = realCallingUid;
                } else if (userInfo.isManagedProfile() || userInfo.isClonedProfile()) {
                    UserManager userManager = UserManager.get(this.mService.mContext);
                    long token = Binder.clearCallingIdentity();
                    try {
                        UserInfo parent = userManager.getProfileParent(userId);
                        if (parent != null) {
                            try {
                                if (userManager.isUserUnlockingOrUnlocked(parent.id) && !userManager.isUserUnlockingOrUnlocked(userId)) {
                                    profileLockedAndParentUnlockingOrUnlocked = true;
                                    Binder.restoreCallingIdentity(token);
                                    if (!profileLockedAndParentUnlockingOrUnlocked) {
                                        realCallingUid2 = realCallingUid;
                                        rInfo = this.mSupervisor.resolveIntent(intent3, resolvedType, userId, 786432, computeResolveFilterUid(callingUid2, realCallingUid, this.mRequest.filterCallingUid));
                                        aInfo = this.mSupervisor.resolveActivity(intent3, rInfo, startFlags, profilerInfo);
                                        if (aInfo != null) {
                                            this.mHwActivityStarterEx.preloadApplication(aInfo.applicationInfo, callingPackage);
                                        }
                                        if (!(aInfo == null || aInfo.applicationInfo == null || callingPackage == null || !callingPackage.equals(aInfo.applicationInfo.packageName))) {
                                            Jlog.d(335, aInfo.applicationInfo.packageName + "/" + (intent3.getComponent() == null ? intent3.getComponent().getClassName() : "NULL"), "");
                                        }
                                        if (!this.mHwActivityStarterEx.isAppDisabledByMdmNoComponent(aInfo, intent3, resolvedType, this.mSupervisor)) {
                                            return -92;
                                        }
                                        WindowManagerGlobalLock windowManagerGlobalLock2 = this.mService.mGlobalLock;
                                        synchronized (windowManagerGlobalLock2) {
                                            try {
                                                WindowManagerService.boostPriorityForLockedSection();
                                                ActivityStack stack3 = this.mRootActivityContainer.getTopDisplayFocusedStack();
                                                if (globalConfig != null) {
                                                    try {
                                                        if (this.mService.getGlobalConfiguration().diff(globalConfig) != 0) {
                                                            z = true;
                                                            stack3.mConfigWillChange = z;
                                                            if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                                                                Slog.v(TAG_CONFIGURATION, "Starting activity when config will change = " + stack3.mConfigWillChange);
                                                            }
                                                            origId = Binder.clearCallingIdentity();
                                                            if (aInfo != null) {
                                                                try {
                                                                    if ((aInfo.applicationInfo.privateFlags & 2) != 0 && this.mService.mHasHeavyWeightFeature) {
                                                                        if (aInfo.processName.equals(aInfo.applicationInfo.packageName)) {
                                                                            WindowProcessController heavy2 = this.mService.mHeavyWeightProcess;
                                                                            if (heavy2 == null) {
                                                                                windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                                stack = stack3;
                                                                                aInfo3 = aInfo;
                                                                                rInfo3 = rInfo;
                                                                                realCallingUid3 = realCallingUid2;
                                                                                heavy = null;
                                                                            } else if (heavy2.mInfo.uid != aInfo.applicationInfo.uid || !heavy2.mName.equals(aInfo.processName)) {
                                                                                if (caller != null) {
                                                                                    WindowProcessController callerApp = this.mService.getProcessController(caller);
                                                                                    if (callerApp != null) {
                                                                                        stack2 = stack3;
                                                                                        appCallingUid = callerApp.mInfo.uid;
                                                                                    } else {
                                                                                        Slog.w("ActivityTaskManager", "Unable to find app for caller " + caller + " (pid=" + callingUid3 + ") when starting: " + intent3.toString());
                                                                                        SafeActivityOptions.abort(options);
                                                                                    }
                                                                                } else {
                                                                                    stack2 = stack3;
                                                                                    appCallingUid = callingUid2;
                                                                                }
                                                                                IIntentSender target = this.mService.getIntentSenderLocked(2, "android", appCallingUid, userId, null, null, 0, new Intent[]{intent3}, new String[]{resolvedType}, 1342177280, null);
                                                                                Intent newIntent = new Intent();
                                                                                if (requestCode >= 0) {
                                                                                    try {
                                                                                        newIntent.putExtra("has_result", true);
                                                                                    } catch (Throwable th) {
                                                                                        th = th;
                                                                                        windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                                    }
                                                                                }
                                                                                newIntent.putExtra("intent", new IntentSender(target));
                                                                                heavy2.updateIntentForHeavyWeightActivity(newIntent);
                                                                                newIntent.putExtra("new_app", aInfo.packageName);
                                                                                newIntent.setFlags(intent3.getFlags());
                                                                                newIntent.setClassName("android", HeavyWeightSwitcherActivity.class.getName());
                                                                                try {
                                                                                    callingUid5 = Binder.getCallingUid();
                                                                                    callingPid2 = Binder.getCallingPid();
                                                                                    componentSpecified = true;
                                                                                    windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                                    stack = stack2;
                                                                                    r0 = 0;
                                                                                    r0 = 0;
                                                                                } catch (Throwable th2) {
                                                                                    th = th2;
                                                                                    windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                                    while (true) {
                                                                                        try {
                                                                                            break;
                                                                                        } catch (Throwable th3) {
                                                                                            th = th3;
                                                                                        }
                                                                                    }
                                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                                    throw th;
                                                                                }
                                                                                try {
                                                                                    ResolveInfo rInfo6 = this.mSupervisor.resolveIntent(newIntent, null, userId, 0, computeResolveFilterUid(callingUid5, realCallingUid2, ((Request) this.mRequest).filterCallingUid));
                                                                                    if (rInfo6 != null) {
                                                                                        try {
                                                                                            aInfo4 = rInfo6.activityInfo;
                                                                                        } catch (Throwable th4) {
                                                                                            th = th4;
                                                                                        }
                                                                                    } else {
                                                                                        aInfo4 = null;
                                                                                    }
                                                                                    if (aInfo4 != null) {
                                                                                        try {
                                                                                            realCallingUid3 = realCallingUid2;
                                                                                            try {
                                                                                                aInfo2 = this.mService.mAmInternal.getActivityInfoForUser(aInfo4, userId);
                                                                                                resolvedType2 = null;
                                                                                                callingPid = callingPid2;
                                                                                                intent2 = newIntent;
                                                                                                rInfo2 = rInfo6;
                                                                                                callingUid4 = callingUid5;
                                                                                                componentSpecified2 = true;
                                                                                                caller2 = null;
                                                                                            } catch (Throwable th5) {
                                                                                                th = th5;
                                                                                                while (true) {
                                                                                                    break;
                                                                                                }
                                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                                throw th;
                                                                                            }
                                                                                        } catch (Throwable th6) {
                                                                                            th = th6;
                                                                                            while (true) {
                                                                                                break;
                                                                                            }
                                                                                            WindowManagerService.resetPriorityAfterLockedSection();
                                                                                            throw th;
                                                                                        }
                                                                                    } else {
                                                                                        realCallingUid3 = realCallingUid2;
                                                                                        resolvedType2 = null;
                                                                                        callingPid = callingPid2;
                                                                                        intent2 = newIntent;
                                                                                        aInfo2 = aInfo4;
                                                                                        rInfo2 = rInfo6;
                                                                                        callingUid4 = callingUid5;
                                                                                        componentSpecified2 = true;
                                                                                        caller2 = null;
                                                                                    }
                                                                                    ActivityRecord[] outRecord = new ActivityRecord[1];
                                                                                    int res = startActivity(caller2, intent2, ephemeralIntent, resolvedType2, aInfo2, rInfo2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid4, callingPackage, realCallingPid, realCallingUid3, startFlags, options, ignoreTargetSecurity, componentSpecified2, outRecord, inTask, reason, allowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, allowBackgroundActivityStart);
                                                                                    Binder.restoreCallingIdentity(origId);
                                                                                    if (!stack.mConfigWillChange) {
                                                                                        activityStarter = this;
                                                                                        try {
                                                                                            activityStarter.mService.mAmInternal.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
                                                                                            stack.mConfigWillChange = r0;
                                                                                            if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                                                                                                try {
                                                                                                    Slog.v(TAG_CONFIGURATION, "Updating to new configuration after starting activity.");
                                                                                                } catch (Throwable th7) {
                                                                                                    th = th7;
                                                                                                }
                                                                                            }
                                                                                            activityStarter.mService.updateConfigurationLocked(globalConfig, null, r0);
                                                                                        } catch (Throwable th8) {
                                                                                            th = th8;
                                                                                            while (true) {
                                                                                                break;
                                                                                            }
                                                                                            WindowManagerService.resetPriorityAfterLockedSection();
                                                                                            throw th;
                                                                                        }
                                                                                    } else {
                                                                                        activityStarter = this;
                                                                                    }
                                                                                    activityStarter.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res, outRecord[r0]);
                                                                                    if (outResult != null) {
                                                                                        try {
                                                                                            outResult.result = res;
                                                                                            ActivityRecord r = outRecord[r0];
                                                                                            if (res != 0) {
                                                                                                int i = 3;
                                                                                                if (res == 2) {
                                                                                                    if (!r.attachedToProcess()) {
                                                                                                        i = 1;
                                                                                                    }
                                                                                                    outResult.launchState = i;
                                                                                                    if (!r.nowVisible || !r.isState(ActivityStack.ActivityState.RESUMED)) {
                                                                                                        activityStarter.mSupervisor.waitActivityVisible(r.mActivityComponent, outResult, SystemClock.uptimeMillis());
                                                                                                        do {
                                                                                                            try {
                                                                                                                activityStarter.mService.mGlobalLock.wait();
                                                                                                            } catch (InterruptedException e) {
                                                                                                            }
                                                                                                            if (outResult.timeout) {
                                                                                                                break;
                                                                                                            }
                                                                                                        } while (outResult.who == null);
                                                                                                    } else {
                                                                                                        outResult.timeout = r0;
                                                                                                        outResult.who = r.mActivityComponent;
                                                                                                        outResult.totalTime = 0;
                                                                                                    }
                                                                                                } else if (res == 3) {
                                                                                                    outResult.timeout = r0;
                                                                                                    outResult.who = r.mActivityComponent;
                                                                                                    outResult.totalTime = 0;
                                                                                                }
                                                                                            } else {
                                                                                                activityStarter.mSupervisor.mWaitingActivityLaunched.add(outResult);
                                                                                                do {
                                                                                                    try {
                                                                                                        activityStarter.mService.mGlobalLock.wait();
                                                                                                    } catch (InterruptedException e2) {
                                                                                                    }
                                                                                                    if (outResult.result == 2 || outResult.timeout) {
                                                                                                    }
                                                                                                } while (outResult.who == null);
                                                                                                if (outResult.result == 2) {
                                                                                                    res = 2;
                                                                                                }
                                                                                            }
                                                                                        } catch (Throwable th9) {
                                                                                            th = th9;
                                                                                            while (true) {
                                                                                                break;
                                                                                            }
                                                                                            WindowManagerService.resetPriorityAfterLockedSection();
                                                                                            throw th;
                                                                                        }
                                                                                    }
                                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                                    return res;
                                                                                } catch (Throwable th10) {
                                                                                    th = th10;
                                                                                    while (true) {
                                                                                        break;
                                                                                    }
                                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                                    throw th;
                                                                                }
                                                                            } else {
                                                                                windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                                stack = stack3;
                                                                                aInfo3 = aInfo;
                                                                                rInfo3 = rInfo;
                                                                                realCallingUid3 = realCallingUid2;
                                                                                heavy = null;
                                                                            }
                                                                        } else {
                                                                            windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                            stack = stack3;
                                                                            aInfo3 = aInfo;
                                                                            rInfo3 = rInfo;
                                                                            realCallingUid3 = realCallingUid2;
                                                                            heavy = null;
                                                                        }
                                                                        resolvedType2 = resolvedType;
                                                                        callingPid = callingUid3;
                                                                        intent2 = intent3;
                                                                        callingUid4 = callingUid2;
                                                                        rInfo2 = rInfo3;
                                                                        aInfo2 = aInfo3;
                                                                        componentSpecified2 = componentSpecified;
                                                                        caller2 = caller;
                                                                        r0 = heavy;
                                                                        ActivityRecord[] outRecord2 = new ActivityRecord[1];
                                                                        int res2 = startActivity(caller2, intent2, ephemeralIntent, resolvedType2, aInfo2, rInfo2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid4, callingPackage, realCallingPid, realCallingUid3, startFlags, options, ignoreTargetSecurity, componentSpecified2, outRecord2, inTask, reason, allowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, allowBackgroundActivityStart);
                                                                        Binder.restoreCallingIdentity(origId);
                                                                        if (!stack.mConfigWillChange) {
                                                                        }
                                                                        activityStarter.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res2, outRecord2[r0]);
                                                                        if (outResult != null) {
                                                                        }
                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                        return res2;
                                                                    }
                                                                } catch (Throwable th11) {
                                                                    th = th11;
                                                                    windowManagerGlobalLock = windowManagerGlobalLock2;
                                                                    while (true) {
                                                                        break;
                                                                    }
                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                    throw th;
                                                                }
                                                            }
                                                            windowManagerGlobalLock = windowManagerGlobalLock2;
                                                            stack = stack3;
                                                            aInfo3 = aInfo;
                                                            rInfo3 = rInfo;
                                                            realCallingUid3 = realCallingUid2;
                                                            heavy = null;
                                                            resolvedType2 = resolvedType;
                                                            callingPid = callingUid3;
                                                            intent2 = intent3;
                                                            callingUid4 = callingUid2;
                                                            rInfo2 = rInfo3;
                                                            aInfo2 = aInfo3;
                                                            componentSpecified2 = componentSpecified;
                                                            caller2 = caller;
                                                            r0 = heavy;
                                                            ActivityRecord[] outRecord22 = new ActivityRecord[1];
                                                            try {
                                                                int res22 = startActivity(caller2, intent2, ephemeralIntent, resolvedType2, aInfo2, rInfo2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid4, callingPackage, realCallingPid, realCallingUid3, startFlags, options, ignoreTargetSecurity, componentSpecified2, outRecord22, inTask, reason, allowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, allowBackgroundActivityStart);
                                                                Binder.restoreCallingIdentity(origId);
                                                                if (!stack.mConfigWillChange) {
                                                                }
                                                                activityStarter.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res22, outRecord22[r0]);
                                                                if (outResult != null) {
                                                                }
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                return res22;
                                                            } catch (Throwable th12) {
                                                                th = th12;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        }
                                                    } catch (Throwable th13) {
                                                        th = th13;
                                                        windowManagerGlobalLock = windowManagerGlobalLock2;
                                                        while (true) {
                                                            break;
                                                        }
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                z = false;
                                                stack3.mConfigWillChange = z;
                                                if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                                                }
                                                origId = Binder.clearCallingIdentity();
                                                if (aInfo != null) {
                                                }
                                                windowManagerGlobalLock = windowManagerGlobalLock2;
                                                stack = stack3;
                                                aInfo3 = aInfo;
                                                rInfo3 = rInfo;
                                                realCallingUid3 = realCallingUid2;
                                                heavy = null;
                                                resolvedType2 = resolvedType;
                                                callingPid = callingUid3;
                                                intent2 = intent3;
                                                callingUid4 = callingUid2;
                                                rInfo2 = rInfo3;
                                                aInfo2 = aInfo3;
                                                componentSpecified2 = componentSpecified;
                                                caller2 = caller;
                                                r0 = heavy;
                                                try {
                                                    ActivityRecord[] outRecord222 = new ActivityRecord[1];
                                                    int res222 = startActivity(caller2, intent2, ephemeralIntent, resolvedType2, aInfo2, rInfo2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid4, callingPackage, realCallingPid, realCallingUid3, startFlags, options, ignoreTargetSecurity, componentSpecified2, outRecord222, inTask, reason, allowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, allowBackgroundActivityStart);
                                                    Binder.restoreCallingIdentity(origId);
                                                    if (!stack.mConfigWillChange) {
                                                    }
                                                    activityStarter.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res222, outRecord222[r0]);
                                                    if (outResult != null) {
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    return res222;
                                                } catch (Throwable th14) {
                                                    th = th14;
                                                    while (true) {
                                                        break;
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            } catch (Throwable th15) {
                                                th = th15;
                                                windowManagerGlobalLock = windowManagerGlobalLock2;
                                                while (true) {
                                                    break;
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        return -94;
                                    }
                                    rInfo4 = rInfo5;
                                    realCallingUid2 = realCallingUid;
                                }
                            } catch (Throwable th16) {
                                th = th16;
                                Binder.restoreCallingIdentity(token);
                                throw th;
                            }
                        }
                        profileLockedAndParentUnlockingOrUnlocked = false;
                        Binder.restoreCallingIdentity(token);
                        if (!profileLockedAndParentUnlockingOrUnlocked) {
                        }
                    } catch (Throwable th17) {
                        th = th17;
                        Binder.restoreCallingIdentity(token);
                        throw th;
                    }
                } else {
                    rInfo4 = rInfo5;
                    realCallingUid2 = realCallingUid;
                }
            } else {
                rInfo4 = rInfo5;
                realCallingUid2 = realCallingUid;
            }
            rInfo = rInfo4;
            aInfo = this.mSupervisor.resolveActivity(intent3, rInfo, startFlags, profilerInfo);
            if (aInfo != null) {
            }
            if (intent3.getComponent() == null) {
            }
            Jlog.d(335, aInfo.applicationInfo.packageName + "/" + (intent3.getComponent() == null ? intent3.getComponent().getClassName() : "NULL"), "");
            if (!this.mHwActivityStarterEx.isAppDisabledByMdmNoComponent(aInfo, intent3, resolvedType, this.mSupervisor)) {
            }
        } else {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
    }

    static int computeResolveFilterUid(int customCallingUid, int actualCallingUid, int filterCallingUid) {
        if (filterCallingUid != -10000) {
            return filterCallingUid;
        }
        return customCallingUid >= 0 ? customCallingUid : actualCallingUid;
    }

    /* JADX INFO: finally extract failed */
    private int startActivity(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask, ActivityRecord[] outActivity, boolean restrictedBgActivity) {
        try {
            this.mService.mWindowManager.deferSurfaceLayout();
            int result = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, outActivity, restrictedBgActivity);
            try {
                ActivityStack currentStack = r.getActivityStack();
                ActivityStack startedActivityStack = currentStack != null ? currentStack : this.mTargetStack;
                if (!ActivityManager.isStartResultSuccessful(result)) {
                    ActivityStack stack = this.mStartActivity.getActivityStack();
                    if (stack != null) {
                        stack.finishActivityLocked(this.mStartActivity, 0, null, "startActivity", true);
                    }
                    if (startedActivityStack != null && startedActivityStack.isAttached() && startedActivityStack.numActivities() == 0 && !startedActivityStack.isActivityTypeHome()) {
                        startedActivityStack.remove();
                    }
                } else if (startedActivityStack != null) {
                    ActivityRecord currentTop = startedActivityStack.topRunningActivityLocked();
                    if (currentTop != null && currentTop.shouldUpdateConfigForDisplayChanged()) {
                        this.mRootActivityContainer.ensureVisibilityAndConfig(currentTop, currentTop.getDisplayId(), true, false);
                    }
                }
                this.mService.mWindowManager.continueSurfaceLayout();
                if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                    Slog.v("ActivityTaskManager", "startActivity result is " + result);
                }
                postStartActivityProcessing(r, result, startedActivityStack);
                return result;
            } catch (Throwable th) {
                throw th;
            }
        } catch (Throwable th2) {
            ActivityStack currentStack2 = r.getActivityStack();
            ActivityStack startedActivityStack2 = currentStack2 != null ? currentStack2 : this.mTargetStack;
            if (!ActivityManager.isStartResultSuccessful(-96)) {
                ActivityStack stack2 = this.mStartActivity.getActivityStack();
                if (stack2 != null) {
                    stack2.finishActivityLocked(this.mStartActivity, 0, null, "startActivity", true);
                }
                if (startedActivityStack2 != null && startedActivityStack2.isAttached() && startedActivityStack2.numActivities() == 0 && !startedActivityStack2.isActivityTypeHome()) {
                    startedActivityStack2.remove();
                }
            } else if (startedActivityStack2 != null) {
                ActivityRecord currentTop2 = startedActivityStack2.topRunningActivityLocked();
                if (currentTop2 != null && currentTop2.shouldUpdateConfigForDisplayChanged()) {
                    this.mRootActivityContainer.ensureVisibilityAndConfig(currentTop2, currentTop2.getDisplayId(), true, false);
                }
            }
            throw th2;
        } finally {
            this.mService.mWindowManager.continueSurfaceLayout();
        }
    }

    private boolean handleBackgroundActivityAbort(ActivityRecord r) {
        if (!(!this.mService.isBackgroundActivityStartsEnabled())) {
            return false;
        }
        ActivityRecord resultRecord = r.resultTo;
        String resultWho = r.resultWho;
        int requestCode = r.requestCode;
        if (resultRecord != null) {
            resultRecord.getActivityStack().sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
        }
        ActivityOptions.abort(r.pendingOptions);
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:126:0x0232  */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x0269  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x008e  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x00b3  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0109  */
    private int startActivityUnchecked(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask, ActivityRecord[] outActivity, boolean restrictedBgActivity) {
        ActivityRecord reusedActivity;
        int i;
        ActivityRecord reusedActivity2;
        int result;
        ActivityStack activityStack;
        ActivityRecord activityRecord;
        ActivityRecord activityRecord2;
        setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor, restrictedBgActivity);
        int preferredWindowingMode = this.mLaunchParams.mWindowingMode;
        computeLaunchingTaskFlags();
        computeSourceStack();
        this.mIntent.setFlags(this.mLaunchFlags);
        ActivityRecord reusedActivity3 = getReusableIntentActivity();
        if (options != null && options.getLaunchWindowingMode() == 12) {
            reusedActivity3 = null;
        }
        this.mService.mHwATMSEx.noteActivityInitializing(this.mStartActivity, reusedActivity3);
        Flog.i(101, "ReusedActivity is " + reusedActivity3);
        Bundle ret = this.mHwActivityStarterEx.checkActivityStartedOnDisplay(this.mStartActivity, this.mPreferredDisplayId, options, reusedActivity3);
        if (ret != null) {
            if (ret.getBoolean("skipStart", false)) {
                return ret.getInt("startResult", 0);
            }
            if (ret.getBoolean("skipReuse", false)) {
                reusedActivity = null;
                this.mSupervisor.getLaunchParamsController().calculate(reusedActivity == null ? reusedActivity.getTaskRecord() : this.mInTask, r.info.windowLayout, r, sourceRecord, options, 2, this.mLaunchParams);
                if (!this.mLaunchParams.hasPreferredDisplay()) {
                    i = this.mLaunchParams.mPreferredDisplayId;
                } else {
                    i = 0;
                }
                this.mPreferredDisplayId = i;
                if (r.isActivityTypeHome() || this.mRootActivityContainer.canStartHomeOnDisplay(r.info, this.mPreferredDisplayId, true)) {
                    if (HwFreeFormUtils.isFreeFormEnable() && !HwFreeFormUtils.getFreeFormStackVisible()) {
                        if (reusedActivity == null && reusedActivity.inFreeformWindowingMode() && this.mLaunchParams.mWindowingMode != 5) {
                            this.mSupervisor.mHwActivityStackSupervisorEx.handleFreeFormWindow(reusedActivity.task);
                        }
                    }
                    if (reusedActivity != null || reusedActivity.getTaskRecord() == null) {
                        reusedActivity2 = reusedActivity;
                    } else {
                        if (this.mService.getLockTaskController().isLockTaskModeViolation(reusedActivity.getTaskRecord(), (this.mLaunchFlags & 268468224) == 268468224)) {
                            Slog.e("ActivityTaskManager", "startActivityUnchecked: Attempt to violate Lock Task Mode");
                            return 101;
                        }
                        boolean clearTopAndResetStandardLaunchMode = (this.mLaunchFlags & 69206016) == 69206016 && this.mLaunchMode == 0;
                        if (this.mStartActivity.getTaskRecord() == null && !clearTopAndResetStandardLaunchMode) {
                            this.mStartActivity.setTask(reusedActivity.getTaskRecord());
                        }
                        if (reusedActivity.getTaskRecord().intent == null) {
                            reusedActivity.getTaskRecord().setIntent(this.mStartActivity);
                        } else {
                            if ((this.mStartActivity.intent.getFlags() & 16384) != 0) {
                                reusedActivity.getTaskRecord().intent.addFlags(16384);
                            } else {
                                reusedActivity.getTaskRecord().intent.removeFlags(16384);
                            }
                        }
                        int i2 = this.mLaunchFlags;
                        if ((67108864 & i2) != 0 || isDocumentLaunchesIntoExisting(i2) || isLaunchModeOneOf(3, 2)) {
                            TaskRecord task = reusedActivity.getTaskRecord();
                            ActivityRecord top = task.performClearTaskForReuseLocked(this.mStartActivity, this.mLaunchFlags);
                            if (reusedActivity.getTaskRecord() == null) {
                                reusedActivity.setTask(task);
                            }
                            if (top != null) {
                                if (top.frontOfTask) {
                                    top.getTaskRecord().setIntent(this.mStartActivity);
                                }
                                deliverNewIntent(top);
                            }
                        }
                        this.mRootActivityContainer.sendPowerHintForLaunchStartIfNeeded(false, reusedActivity);
                        reusedActivity2 = setTargetStackAndMoveToFrontIfNeeded(reusedActivity);
                        ActivityRecord outResult = (outActivity == null || outActivity.length <= 0) ? null : outActivity[0];
                        if (outResult != null && (outResult.finishing || outResult.noDisplay)) {
                            outActivity[0] = reusedActivity2;
                        }
                        if ((this.mStartFlags & 1) != 0) {
                            resumeTargetStackIfNeeded();
                            return 1;
                        } else if (reusedActivity2 != null) {
                            setTaskFromIntentActivity(reusedActivity2);
                            if (!this.mAddingToTask && this.mReuseTask == null) {
                                resumeTargetStackIfNeeded();
                                if (outActivity != null && outActivity.length > 0) {
                                    outActivity[0] = (!reusedActivity2.finishing || reusedActivity2.getTaskRecord() == null) ? reusedActivity2 : reusedActivity2.getTaskRecord().getTopActivity();
                                }
                                return this.mMovedToFront ? 2 : 3;
                            }
                        }
                    }
                    if (this.mStartActivity.packageName != null) {
                        ActivityStack sourceStack = this.mStartActivity.resultTo != null ? this.mStartActivity.resultTo.getActivityStack() : null;
                        if (sourceStack != null) {
                            sourceStack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
                        }
                        ActivityOptions.abort(this.mOptions);
                        return -92;
                    }
                    ActivityStack topStack = this.mRootActivityContainer.getTopDisplayFocusedStack();
                    ActivityRecord topFocused = topStack.getTopActivity();
                    ActivityRecord top2 = topStack.topRunningNonDelayedActivityLocked(this.mNotTop);
                    if (top2 != null && this.mStartActivity.resultTo == null && top2.mActivityComponent.equals(this.mStartActivity.mActivityComponent) && top2.mUserId == this.mStartActivity.mUserId && top2.attachedToProcess() && ((this.mLaunchFlags & 536870912) != 0 || isLaunchModeOneOf(1, 2)) && (!top2.isActivityTypeHome() || top2.getDisplayId() == this.mPreferredDisplayId)) {
                        topStack.mLastPausedActivity = null;
                        if (this.mDoResume) {
                            this.mRootActivityContainer.resumeFocusedStacksTopActivities();
                        }
                        ActivityOptions.abort(this.mOptions);
                        if ((this.mStartFlags & 1) != 0) {
                            return 1;
                        }
                        deliverNewIntent(top2);
                        this.mSupervisor.handleNonResizableTaskIfNeeded(top2.getTaskRecord(), preferredWindowingMode, this.mPreferredDisplayId, topStack);
                        return 3;
                    }
                    boolean newTask = false;
                    TaskRecord taskToAffiliate = (!this.mLaunchTaskBehind || (activityRecord2 = this.mSourceRecord) == null) ? null : activityRecord2.getTaskRecord();
                    if ((this.mStartActivity.resultTo == null || ((activityRecord = this.mSourceRecord) != null && activityRecord.inHwMagicWindowingMode() && this.mStartActivity.mIsMwNewTask)) && this.mInTask == null && !this.mAddingToTask && (this.mLaunchFlags & 268435456) != 0) {
                        newTask = true;
                        result = setTaskFromReuseOrCreateNewTask(taskToAffiliate);
                    } else if (this.mSourceRecord != null) {
                        if (HwPCUtils.isPcCastModeInServer() && this.mSourceRecord.getTaskRecord() == null && this.mSourceRecord.getActivityStack() == null) {
                            Slog.i("ActivityTaskManager", "ActivityStarter startActivityUnchecked task and stack null");
                            setTaskToCurrentTopOrCreateNewTask();
                        } else if (!HwPCUtils.isHiCarCastMode() || !HwPCUtils.isValidExtDisplayId(this.mPreferredDisplayId) || (activityStack = this.mSourceStack) == null || HwPCUtils.isExtDynamicStack(activityStack.mStackId)) {
                            result = setTaskFromSourceRecord();
                        } else {
                            HwPCUtils.log("ActivityTaskManager", "ActivityStarter startActivityUnchecked in hicar mode.");
                            setTaskToCurrentTopOrCreateNewTask();
                        }
                        result = 0;
                    } else if (this.mInTask != null) {
                        result = setTaskFromInTask();
                    } else {
                        result = setTaskToCurrentTopOrCreateNewTask();
                    }
                    this.mHwActivityStarterEx.handleFreeFormStackIfNeed(this.mStartActivity);
                    if (result != 0) {
                        return result;
                    }
                    if (!this.mHwActivityStarterEx.checkActivityStartForPCMode(this.mOptions, this.mStartActivity, this.mTargetStack)) {
                        return -96;
                    }
                    if (this.mCallingUid == 1000 && this.mIntent.getCallingUid() != 0) {
                        this.mCallingUid = this.mIntent.getCallingUid();
                        this.mIntent.setCallingUid(0);
                    }
                    this.mService.mUgmInternal.grantUriPermissionFromIntent(this.mCallingUid, this.mStartActivity.packageName, this.mIntent, this.mStartActivity.getUriPermissionsLocked(), this.mStartActivity.mUserId);
                    this.mService.getPackageManagerInternalLocked().grantEphemeralAccess(this.mStartActivity.mUserId, this.mIntent, UserHandle.getAppId(this.mStartActivity.appInfo.uid), UserHandle.getAppId(this.mCallingUid));
                    if (newTask) {
                        EventLog.writeEvent(30004, Integer.valueOf(this.mStartActivity.mUserId), Integer.valueOf(this.mStartActivity.getTaskRecord().taskId));
                    }
                    ActivityRecord activityRecord3 = this.mStartActivity;
                    ActivityStack.logStartActivity(30005, activityRecord3, activityRecord3.getTaskRecord());
                    IApplicationToken.Stub stub = null;
                    this.mTargetStack.mLastPausedActivity = null;
                    this.mRootActivityContainer.sendPowerHintForLaunchStartIfNeeded(false, this.mStartActivity);
                    if (HwMwUtils.ENABLED && topFocused != null) {
                        boolean isCreateNewTask = newTask && this.mReuseTask == null;
                        Object[] objArr = new Object[5];
                        ActivityRecord activityRecord4 = this.mSourceRecord;
                        objArr[0] = activityRecord4 != null ? activityRecord4.appToken : topFocused.appToken;
                        objArr[1] = this.mStartActivity;
                        if (reusedActivity2 != null) {
                            stub = reusedActivity2.appToken;
                        }
                        objArr[2] = stub;
                        objArr[3] = Boolean.valueOf(isCreateNewTask);
                        objArr[4] = options;
                        HwMwUtils.performPolicy(0, objArr);
                    }
                    this.mTargetStack.startActivityLocked(this.mStartActivity, topFocused, newTask, this.mKeepCurTransition, this.mOptions);
                    if (this.mDoResume) {
                        ActivityRecord topTaskActivity = this.mStartActivity.getTaskRecord().topRunningActivityLocked();
                        if (!this.mTargetStack.isFocusable() || !(topTaskActivity == null || !topTaskActivity.mTaskOverlay || this.mStartActivity == topTaskActivity)) {
                            this.mTargetStack.ensureActivitiesVisibleLocked(this.mStartActivity, 0, false);
                            this.mTargetStack.getDisplay().mDisplayContent.executeAppTransition();
                        } else {
                            if (this.mTargetStack.isFocusable() && !this.mRootActivityContainer.isTopDisplayFocusedStack(this.mTargetStack)) {
                                this.mTargetStack.moveToFront("startActivityUnchecked");
                            }
                            this.mRootActivityContainer.resumeFocusedStacksTopActivities(this.mTargetStack, this.mStartActivity, this.mOptions);
                        }
                    } else if (this.mStartActivity != null) {
                        this.mSupervisor.mRecentTasks.add(this.mStartActivity.getTaskRecord());
                    }
                    this.mRootActivityContainer.updateUserStack(this.mStartActivity.mUserId, this.mTargetStack);
                    this.mSupervisor.handleNonResizableTaskIfNeeded(this.mStartActivity.getTaskRecord(), preferredWindowingMode, this.mPreferredDisplayId, this.mTargetStack);
                    return 0;
                }
                Slog.w("ActivityTaskManager", "Cannot launch home on display " + this.mPreferredDisplayId);
                return -96;
            }
        }
        reusedActivity = reusedActivity3;
        this.mSupervisor.getLaunchParamsController().calculate(reusedActivity == null ? reusedActivity.getTaskRecord() : this.mInTask, r.info.windowLayout, r, sourceRecord, options, 2, this.mLaunchParams);
        if (!this.mLaunchParams.hasPreferredDisplay()) {
        }
        this.mPreferredDisplayId = i;
        if (r.isActivityTypeHome()) {
        }
        if (reusedActivity == null && reusedActivity.inFreeformWindowingMode() && this.mLaunchParams.mWindowingMode != 5) {
        }
        if (reusedActivity != null) {
        }
        reusedActivity2 = reusedActivity;
        if (this.mStartActivity.packageName != null) {
        }
    }

    /* access modifiers changed from: package-private */
    public void reset(boolean clearRequest) {
        this.mStartActivity = null;
        this.mIntent = null;
        this.mCallingUid = -1;
        this.mOptions = null;
        this.mRestrictedBgActivity = false;
        this.mLaunchTaskBehind = false;
        this.mLaunchFlags = 0;
        this.mLaunchMode = -1;
        this.mLaunchParams.reset();
        this.mNotTop = null;
        this.mDoResume = false;
        this.mStartFlags = 0;
        this.mSourceRecord = null;
        this.mPreferredDisplayId = -1;
        this.mInTask = null;
        this.mAddingToTask = false;
        this.mReuseTask = null;
        this.mNewTaskInfo = null;
        this.mNewTaskIntent = null;
        this.mSourceStack = null;
        this.mTargetStack = null;
        this.mMovedToFront = false;
        this.mNoAnimation = false;
        this.mKeepCurTransition = false;
        this.mAvoidMoveToFront = false;
        this.mVoiceSession = null;
        this.mVoiceInteractor = null;
        this.mIntentDelivered = false;
        if (clearRequest) {
            this.mRequest.reset();
        }
    }

    /* access modifiers changed from: protected */
    public void setInitialState(ActivityRecord r, ActivityOptions options, TaskRecord inTask, boolean doResume, int startFlags, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean restrictedBgActivity) {
        int i;
        reset(false);
        if (options != null && WindowConfiguration.isHwMultiStackWindowingMode(options.getLaunchWindowingMode()) && !r.packageName.equals("com.huawei.systemmanager") && (r.intent.getFlags() & 268435456) != 0 && !this.mService.mHwATMSEx.isHwResizableApp(r.packageName, r.info.resizeMode)) {
            options.setLaunchWindowingMode(1);
        }
        this.mStartActivity = r;
        this.mIntent = r.intent;
        this.mOptions = options;
        this.mCallingUid = r.launchedFromUid;
        this.mSourceRecord = sourceRecord;
        this.mVoiceSession = voiceSession;
        this.mVoiceInteractor = voiceInteractor;
        this.mRestrictedBgActivity = restrictedBgActivity;
        this.mLaunchParams.reset();
        this.mSupervisor.getLaunchParamsController().calculate(inTask, r.info.windowLayout, r, sourceRecord, options, 0, this.mLaunchParams);
        if (this.mLaunchParams.hasPreferredDisplay()) {
            i = this.mLaunchParams.mPreferredDisplayId;
        } else {
            i = 0;
        }
        this.mPreferredDisplayId = i;
        this.mLaunchMode = r.launchMode;
        this.mLaunchFlags = adjustLaunchFlagsToDocumentMode(r, 3 == this.mLaunchMode, 2 == this.mLaunchMode, this.mIntent.getFlags());
        this.mLaunchTaskBehind = r.mLaunchTaskBehind && !isLaunchModeOneOf(2, 3) && (this.mLaunchFlags & 524288) != 0;
        sendNewTaskResultRequestIfNeeded();
        if ((this.mLaunchFlags & 524288) != 0 && r.resultTo == null) {
            this.mLaunchFlags |= 268435456;
        }
        if ((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchTaskBehind || r.info.documentLaunchMode == 2)) {
            this.mLaunchFlags |= 134217728;
        }
        this.mSupervisor.mUserLeaving = (this.mLaunchFlags & 262144) == 0;
        if (ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING) {
            Slog.v("ActivityTaskManager", "startActivity() => mUserLeaving=" + this.mSupervisor.mUserLeaving);
        }
        this.mDoResume = doResume;
        if (!doResume || !r.okToShowLocked()) {
            r.delayedResume = true;
            this.mDoResume = false;
        }
        ActivityOptions activityOptions = this.mOptions;
        if (activityOptions != null) {
            if (activityOptions.getLaunchTaskId() != -1 && this.mOptions.getTaskOverlay()) {
                r.mTaskOverlay = true;
                if (!this.mOptions.canTaskOverlayResume()) {
                    TaskRecord task = this.mRootActivityContainer.anyTaskForId(this.mOptions.getLaunchTaskId());
                    ActivityRecord top = task != null ? task.getTopActivity() : null;
                    if (top != null && !top.isState(ActivityStack.ActivityState.RESUMED)) {
                        this.mDoResume = false;
                        this.mAvoidMoveToFront = true;
                    }
                }
            } else if (this.mOptions.getAvoidMoveToFront()) {
                this.mDoResume = false;
                this.mAvoidMoveToFront = true;
            }
        }
        this.mNotTop = (this.mLaunchFlags & 16777216) != 0 ? sourceRecord : null;
        this.mInTask = inTask;
        if (inTask != null && !inTask.inRecents) {
            Slog.w("ActivityTaskManager", "Starting activity in task not in recents: " + inTask);
            this.mInTask = null;
        }
        this.mStartFlags = startFlags;
        if ((startFlags & 1) != 0) {
            ActivityRecord checkedCaller = sourceRecord;
            if (checkedCaller == null) {
                checkedCaller = this.mRootActivityContainer.getTopDisplayFocusedStack().topRunningNonDelayedActivityLocked(this.mNotTop);
            }
            if (!checkedCaller.mActivityComponent.equals(r.mActivityComponent)) {
                this.mStartFlags &= -2;
            }
        }
        this.mNoAnimation = (this.mLaunchFlags & 65536) != 0;
        if (this.mRestrictedBgActivity && !this.mService.isBackgroundActivityStartsEnabled()) {
            this.mAvoidMoveToFront = true;
            this.mDoResume = false;
        }
    }

    private void sendNewTaskResultRequestIfNeeded() {
        ActivityStack sourceStack = this.mStartActivity.resultTo != null ? this.mStartActivity.resultTo.getActivityStack() : null;
        if (sourceStack != null && (this.mLaunchFlags & 268435456) != 0) {
            if (isInSkipCancelResultList(this.mStartActivity.shortComponentName) || (sourceStack.inHwMagicWindowingMode() && this.mStartActivity.mIsMwNewTask)) {
                Slog.w("ActivityTaskManager", "we skip cancelling activity result from activity " + this.mStartActivity.shortComponentName);
                return;
            }
            Slog.w("ActivityTaskManager", "Activity is launching as a new task, so cancelling activity result.");
            sourceStack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
            this.mStartActivity.resultTo = null;
        }
    }

    private void computeLaunchingTaskFlags() {
        ActivityRecord activityRecord;
        TaskRecord taskRecord;
        if (this.mSourceRecord != null || (taskRecord = this.mInTask) == null || taskRecord.getStack() == null) {
            this.mInTask = null;
            if ((this.mStartActivity.isResolverActivity() || this.mStartActivity.noDisplay) && (activityRecord = this.mSourceRecord) != null && activityRecord.inFreeformWindowingMode()) {
                this.mAddingToTask = true;
            }
        } else {
            Intent baseIntent = this.mInTask.getBaseIntent();
            ActivityRecord root = this.mInTask.getRootActivity();
            if (baseIntent != null) {
                if (isLaunchModeOneOf(3, 2)) {
                    if (!baseIntent.getComponent().equals(this.mStartActivity.intent.getComponent())) {
                        ActivityOptions.abort(this.mOptions);
                        throw new IllegalArgumentException("Trying to launch singleInstance/Task " + this.mStartActivity + " into different task " + this.mInTask);
                    } else if (root != null) {
                        ActivityOptions.abort(this.mOptions);
                        throw new IllegalArgumentException("Caller with mInTask " + this.mInTask + " has root " + root + " but target is singleInstance/Task");
                    }
                }
                if (root == null) {
                    this.mLaunchFlags = (this.mLaunchFlags & -403185665) | (baseIntent.getFlags() & 403185664);
                    this.mIntent.setFlags(this.mLaunchFlags);
                    this.mInTask.setIntent(this.mStartActivity);
                    this.mAddingToTask = true;
                } else if ((this.mLaunchFlags & 268435456) != 0) {
                    this.mAddingToTask = false;
                } else {
                    this.mAddingToTask = true;
                }
                this.mReuseTask = this.mInTask;
            } else {
                ActivityOptions.abort(this.mOptions);
                throw new IllegalArgumentException("Launching into task without base intent: " + this.mInTask);
            }
        }
        TaskRecord taskRecord2 = this.mInTask;
        if (taskRecord2 == null) {
            ActivityRecord activityRecord2 = this.mSourceRecord;
            if (activityRecord2 == null) {
                if ((this.mLaunchFlags & 268435456) == 0 && taskRecord2 == null) {
                    Slog.w("ActivityTaskManager", "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent.toShortStringWithoutClip(true, true, true));
                    this.mLaunchFlags = this.mLaunchFlags | 268435456;
                }
            } else if (activityRecord2.launchMode == 3) {
                this.mLaunchFlags |= 268435456;
            } else if (isLaunchModeOneOf(3, 2)) {
                this.mLaunchFlags |= 268435456;
            }
        }
    }

    private void computeSourceStack() {
        ActivityRecord activityRecord = this.mSourceRecord;
        if (activityRecord == null) {
            this.mSourceStack = null;
        } else if (!activityRecord.finishing) {
            this.mSourceStack = this.mSourceRecord.getActivityStack();
        } else {
            if ((this.mLaunchFlags & 268435456) == 0) {
                Slog.w("ActivityTaskManager", "startActivity called from finishing " + this.mSourceRecord + "; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
                this.mLaunchFlags = this.mLaunchFlags | 268435456;
                this.mNewTaskInfo = this.mSourceRecord.info;
                TaskRecord sourceTask = this.mSourceRecord.getTaskRecord();
                this.mNewTaskIntent = sourceTask != null ? sourceTask.intent : null;
            }
            this.mSourceRecord = null;
            this.mSourceStack = null;
        }
    }

    private ActivityRecord getReusableIntentActivity() {
        int i = this.mLaunchFlags;
        boolean z = false;
        boolean putIntoExistingTask = (((268435456 & i) != 0 && (i & 134217728) == 0) || isLaunchModeOneOf(3, 2)) & (this.mInTask == null && this.mStartActivity.resultTo == null);
        ActivityRecord intentActivity = null;
        ActivityOptions activityOptions = this.mOptions;
        if (activityOptions != null && activityOptions.getLaunchTaskId() != -1) {
            TaskRecord task = this.mRootActivityContainer.anyTaskForId(this.mOptions.getLaunchTaskId());
            intentActivity = task != null ? task.getTopActivity() : null;
        } else if (putIntoExistingTask) {
            if (3 == this.mLaunchMode) {
                intentActivity = this.mRootActivityContainer.findActivity(this.mIntent, this.mStartActivity.info, this.mStartActivity.isActivityTypeHome());
            } else if ((this.mLaunchFlags & 4096) != 0) {
                RootActivityContainer rootActivityContainer = this.mRootActivityContainer;
                Intent intent = this.mIntent;
                ActivityInfo activityInfo = this.mStartActivity.info;
                if (2 != this.mLaunchMode) {
                    z = true;
                }
                intentActivity = rootActivityContainer.findActivity(intent, activityInfo, z);
            } else {
                intentActivity = this.mRootActivityContainer.findTask(this.mStartActivity, this.mPreferredDisplayId);
            }
        }
        if (intentActivity == null) {
            return intentActivity;
        }
        if ((this.mStartActivity.isActivityTypeHome() || intentActivity.isActivityTypeHome()) && intentActivity.getDisplayId() != this.mPreferredDisplayId) {
            return null;
        }
        return intentActivity;
    }

    private ActivityRecord setTargetStackAndMoveToFrontIfNeeded(ActivityRecord intentActivity) {
        boolean differentTopTask;
        TaskRecord intentTask;
        ActivityRecord activityRecord;
        this.mTargetStack = intentActivity.getActivityStack();
        ActivityStack activityStack = this.mTargetStack;
        activityStack.mLastPausedActivity = null;
        if (this.mPreferredDisplayId == activityStack.mDisplayId) {
            ActivityStack focusStack = this.mTargetStack.getDisplay().getFocusedStack();
            ActivityRecord curTop = focusStack == null ? null : focusStack.topRunningNonDelayedActivityLocked(this.mNotTop);
            TaskRecord topTask = curTop != null ? curTop.getTaskRecord() : null;
            differentTopTask = (topTask == intentActivity.getTaskRecord() && (focusStack == null || topTask == focusStack.topTask())) ? false : true;
        } else {
            differentTopTask = true;
        }
        if (differentTopTask && !this.mAvoidMoveToFront) {
            this.mStartActivity.intent.addFlags(4194304);
            if (this.mSourceRecord == null || (this.mSourceStack.getTopActivity() != null && this.mSourceStack.getTopActivity().getTaskRecord() == this.mSourceRecord.getTaskRecord())) {
                if (this.mLaunchTaskBehind && (activityRecord = this.mSourceRecord) != null) {
                    intentActivity.setTaskToAffiliateWith(activityRecord.getTaskRecord());
                }
                if (!((this.mLaunchFlags & 268468224) == 268468224)) {
                    ActivityRecord activityRecord2 = this.mStartActivity;
                    ActivityStack launchStack = getLaunchStack(activityRecord2, this.mLaunchFlags, activityRecord2.getTaskRecord(), this.mOptions);
                    TaskRecord intentTask2 = intentActivity.getTaskRecord();
                    if (launchStack == null) {
                        intentTask = intentTask2;
                    } else if (launchStack == this.mTargetStack) {
                        intentTask = intentTask2;
                    } else {
                        if (launchStack.inSplitScreenWindowingMode()) {
                            if ((this.mLaunchFlags & 4096) != 0) {
                                intentTask2.reparent(launchStack, true, 0, true, true, "launchToSide");
                            } else {
                                this.mTargetStack.moveTaskToFrontLocked(intentTask2, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringToFrontInsteadOfAdjacentLaunch");
                            }
                            this.mMovedToFront = launchStack != launchStack.getDisplay().getTopStackInWindowingMode(launchStack.getWindowingMode());
                        } else if (launchStack.mDisplayId != this.mTargetStack.mDisplayId) {
                            if (HwPCUtils.isPcCastModeInServer()) {
                                HwPCUtils.log("ActivityTaskManager", " the activity will reparentToDisplay because computer stack is:" + launchStack.mStackId + "#" + launchStack.mDisplayId + " target stack is " + this.mTargetStack.mStackId + "#" + this.mTargetStack.mDisplayId);
                            }
                            intentActivity.getTaskRecord().reparent(launchStack, true, 0, true, true, "reparentToDisplay");
                            this.mMovedToFront = true;
                        } else if (launchStack.isActivityTypeHome() && !this.mTargetStack.isActivityTypeHome()) {
                            intentActivity.getTaskRecord().reparent(launchStack, true, 0, true, true, "reparentingHome");
                            this.mMovedToFront = true;
                        }
                        if (launchStack != null && launchStack.getAllTasks().isEmpty() && HwPCUtils.isExtDynamicStack(launchStack.mStackId)) {
                            launchStack.remove();
                        }
                        this.mOptions = null;
                        Flog.i(301, "setTargetStackAndMoveToFront--->>>showStartingWindow for r:" + intentActivity);
                        if (!INCALLUI_ACTIVITY_CLASS_NAME.equals(intentActivity.shortComponentName) && !this.mShouldSkipStartingWindow) {
                            intentActivity.showStartingWindow(null, false, true);
                            this.mShouldSkipStartingWindow = false;
                        }
                    }
                    if (this.mTargetStack.getWindowingMode() == 12) {
                        this.mService.mHwATMSEx.exitCoordinationMode(false, true);
                    }
                    moveFreeFormFromFullscreen(launchStack, intentTask);
                    this.mService.mHwATMSEx.moveStackToFrontEx(this.mOptions, this.mTargetStack, this.mStartActivity);
                    this.mTargetStack.moveTaskToFrontLocked(intentTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringingFoundTaskToFront");
                    this.mMovedToFront = true;
                    launchStack.remove();
                    this.mOptions = null;
                    Flog.i(301, "setTargetStackAndMoveToFront--->>>showStartingWindow for r:" + intentActivity);
                    intentActivity.showStartingWindow(null, false, true);
                    this.mShouldSkipStartingWindow = false;
                }
            }
        }
        this.mTargetStack = intentActivity.getActivityStack();
        if (!this.mMovedToFront && this.mDoResume) {
            if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                Slog.d("ActivityTaskManager", "Bring to front target: " + this.mTargetStack + " from " + intentActivity);
            }
            if (differentTopTask) {
                this.mService.mHwATMSEx.moveStackToFrontEx(this.mOptions, this.mTargetStack, this.mStartActivity);
            }
            this.mTargetStack.moveToFront("intentActivityFound");
        }
        this.mSupervisor.handleNonResizableTaskIfNeeded(intentActivity.getTaskRecord(), 0, 0, this.mTargetStack);
        if ((this.mLaunchFlags & 2097152) != 0) {
            return this.mTargetStack.resetTaskIfNeededLocked(intentActivity, this.mStartActivity);
        }
        return intentActivity;
    }

    private void moveFreeFormFromFullscreen(ActivityStack launchStack, TaskRecord intentTask) {
        ActivityOptions activityOptions;
        if (HwFreeFormUtils.isFreeFormEnable() && launchStack != null && !this.mTargetStack.inFreeformWindowingMode() && (activityOptions = this.mOptions) != null && activityOptions.getLaunchWindowingMode() == 5) {
            ActivityStack nextStack = this.mService.mRootActivityContainer.getNextFocusableStack(this.mTargetStack, true);
            String underPkgName = (nextStack == null || nextStack.getTopActivity() == null) ? "" : nextStack.getTopActivity().packageName;
            HwFreeFormUtils.log("ams", "move reused activity to freeform from fullscreen above " + underPkgName);
            ActivityStack freeformStack = this.mTargetStack;
            intentTask.mIsReparenting = true;
            freeformStack.setFreeFormStackVisible(true);
            freeformStack.setCurrentPkgUnderFreeForm(underPkgName);
            freeformStack.setWindowingMode(5, false, false, false, true, false);
            updateBounds(intentTask, this.mLaunchParams.mBounds);
            intentTask.mIsReparenting = false;
            this.mShouldSkipStartingWindow = true;
        }
    }

    private void setTaskFromIntentActivity(ActivityRecord intentActivity) {
        int i = this.mLaunchFlags;
        if ((i & 268468224) == 268468224) {
            TaskRecord task = intentActivity.getTaskRecord();
            task.performClearTaskLocked();
            this.mReuseTask = task;
            this.mReuseTask.setIntent(this.mStartActivity);
        } else if ((i & 67108864) != 0 || isLaunchModeOneOf(3, 2)) {
            if (intentActivity.getTaskRecord().performClearTaskLocked(this.mStartActivity, this.mLaunchFlags) == null) {
                this.mAddingToTask = true;
                if (!isInSkipCancelResultList(this.mStartActivity.shortComponentName)) {
                    this.mStartActivity.setTask(null);
                }
                this.mSourceRecord = intentActivity;
                TaskRecord task2 = this.mSourceRecord.getTaskRecord();
                if (task2 != null && task2.getStack() == null) {
                    this.mTargetStack = computeStackFocus(this.mSourceRecord, false, this.mLaunchFlags, this.mOptions);
                    this.mTargetStack.addTask(task2, true ^ this.mLaunchTaskBehind, "startActivityUnchecked");
                }
            }
        } else if (this.mStartActivity.mActivityComponent.equals(intentActivity.getTaskRecord().realActivity)) {
            if (((this.mLaunchFlags & 536870912) != 0 || 1 == this.mLaunchMode) && intentActivity.mActivityComponent.equals(this.mStartActivity.mActivityComponent)) {
                if (intentActivity.frontOfTask) {
                    intentActivity.getTaskRecord().setIntent(this.mStartActivity);
                }
                deliverNewIntent(intentActivity);
            } else if (intentActivity.getTaskRecord().isSameIntentFilter(this.mStartActivity)) {
            } else {
                if (!this.mStartActivity.intent.filterEquals(intentActivity.intent) || !"android.intent.action.MAIN".equals(this.mStartActivity.intent.getAction())) {
                    this.mAddingToTask = true;
                    this.mSourceRecord = intentActivity;
                }
            }
        } else if ((this.mLaunchFlags & 2097152) == 0) {
            this.mAddingToTask = true;
            this.mSourceRecord = intentActivity;
        } else if (!intentActivity.getTaskRecord().rootWasReset) {
            intentActivity.getTaskRecord().setIntent(this.mStartActivity);
        }
    }

    private void resumeTargetStackIfNeeded() {
        if (this.mDoResume) {
            this.mRootActivityContainer.resumeFocusedStacksTopActivities(this.mTargetStack, null, this.mOptions);
        } else {
            ActivityOptions.abort(this.mOptions);
        }
        this.mRootActivityContainer.updateUserStack(this.mStartActivity.mUserId, this.mTargetStack);
    }

    private int setTaskFromReuseOrCreateNewTask(TaskRecord taskToAffiliate) {
        ActivityStack activityStack;
        ActivityOptions activityOptions;
        TaskRecord taskRecord;
        if (this.mRestrictedBgActivity && (((taskRecord = this.mReuseTask) == null || !taskRecord.containsAppUid(this.mCallingUid)) && handleBackgroundActivityAbort(this.mStartActivity))) {
            return WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT;
        }
        this.mTargetStack = computeStackFocus(this.mStartActivity, true, this.mLaunchFlags, this.mOptions);
        this.mHwActivityStarterEx.moveFreeFormToFullScreenStackIfNeed(this.mStartActivity, this.mTargetStack.inFreeformWindowingMode() || this.mLaunchParams.mWindowingMode == 5);
        TaskRecord taskRecord2 = this.mReuseTask;
        if (taskRecord2 == null) {
            ActivityStack activityStack2 = this.mTargetStack;
            int nextTaskIdForUserLocked = this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.mUserId);
            ActivityInfo activityInfo = this.mNewTaskInfo;
            if (activityInfo == null) {
                activityInfo = this.mStartActivity.info;
            }
            Intent intent = this.mNewTaskIntent;
            if (intent == null) {
                intent = this.mIntent;
            }
            addOrReparentStartingActivity(activityStack2.createTaskRecord(nextTaskIdForUserLocked, activityInfo, intent, this.mVoiceSession, this.mVoiceInteractor, !this.mLaunchTaskBehind, this.mStartActivity, this.mSourceRecord, this.mOptions), "setTaskFromReuseOrCreateNewTask - mReuseTask");
            if (((this.mTargetStack.inHwMultiStackWindowingMode() && !this.mTargetStack.inHwSplitScreenWindowingMode()) || this.mTargetStack.inCoordinationSecondaryWindowingMode()) && !this.mLaunchParams.mBounds.isEmpty()) {
                this.mTargetStack.resize(this.mLaunchParams.mBounds, null, null);
            }
            updateBounds(this.mStartActivity.getTaskRecord(), this.mLaunchParams.mBounds);
            if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                Slog.v("ActivityTaskManager", "Starting new activity " + this.mStartActivity + " in new task " + this.mStartActivity.getTaskRecord());
            }
        } else {
            addOrReparentStartingActivity(taskRecord2, "setTaskFromReuseOrCreateNewTask");
            if (HwFreeFormUtils.isFreeFormEnable() && (activityStack = this.mTargetStack) != null && !activityStack.inFreeformWindowingMode() && (activityOptions = this.mOptions) != null && activityOptions.getLaunchWindowingMode() == 5) {
                ActivityStack nextStack = this.mService.mRootActivityContainer.getNextFocusableStack(this.mTargetStack, true);
                String underPkgName = (nextStack == null || nextStack.getTopActivity() == null) ? "" : nextStack.getTopActivity().packageName;
                HwFreeFormUtils.log("ams", "move new create activity to freeform from fullscreen above " + underPkgName);
                ActivityStack freeformStack = this.mTargetStack.getDisplay().getOrCreateStack(5, this.mTargetStack.getActivityType(), true);
                this.mReuseTask.mIsReparenting = true;
                freeformStack.setFreeFormStackVisible(true);
                freeformStack.setCurrentPkgUnderFreeForm(underPkgName);
                this.mReuseTask.reparent(freeformStack, true, 1, true, true, "reparentToFreeForm");
                updateBounds(this.mReuseTask, this.mLaunchParams.mBounds);
                this.mReuseTask.mIsReparenting = false;
                this.mTargetStack = freeformStack;
            }
        }
        if (taskToAffiliate != null) {
            this.mStartActivity.setTaskToAffiliateWith(taskToAffiliate);
        }
        if (!this.mService.getLockTaskController().isLockTaskModeViolation(this.mStartActivity.getTaskRecord()) || (this.mCallingUid == 1000 && (this.mStartActivity.intent.getHwFlags() & 65536) != 0)) {
            if (this.mDoResume) {
                this.mTargetStack.moveToFront("reuseOrNewTask");
            }
            return 0;
        }
        Slog.e("ActivityTaskManager", "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
        return 101;
    }

    private void deliverNewIntent(ActivityRecord activity) {
        if (!this.mIntentDelivered) {
            ActivityStack.logStartActivity(30003, activity, activity.getTaskRecord());
            activity.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
            this.mIntentDelivered = true;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:79:0x0193  */
    private int setTaskFromSourceRecord() {
        int targetDisplayId;
        ActivityRecord top;
        ActivityStack activityStack;
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mSourceRecord.getTaskRecord())) {
            Slog.e("ActivityTaskManager", "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
            return 101;
        }
        TaskRecord sourceTask = this.mSourceRecord.getTaskRecord();
        ActivityStack sourceStack = this.mSourceRecord.getActivityStack();
        if (this.mRestrictedBgActivity && !sourceTask.containsAppUid(this.mCallingUid) && handleBackgroundActivityAbort(this.mStartActivity)) {
            return WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT;
        }
        ActivityStack activityStack2 = this.mTargetStack;
        if (activityStack2 != null) {
            targetDisplayId = activityStack2.mDisplayId;
        } else {
            targetDisplayId = sourceStack.mDisplayId;
        }
        if (sourceStack.topTask() != sourceTask || !this.mStartActivity.canBeLaunchedOnDisplay(targetDisplayId)) {
            ActivityRecord activityRecord = this.mStartActivity;
            this.mTargetStack = getLaunchStack(activityRecord, this.mLaunchFlags, activityRecord.getTaskRecord(), this.mOptions);
            if (this.mTargetStack == null && targetDisplayId != sourceStack.mDisplayId) {
                this.mTargetStack = this.mRootActivityContainer.getValidLaunchStackOnDisplay(sourceStack.mDisplayId, this.mStartActivity, this.mOptions, this.mLaunchParams);
            }
            if (this.mTargetStack == null) {
                this.mTargetStack = this.mRootActivityContainer.getNextValidLaunchStack(this.mStartActivity, -1);
            }
        }
        ActivityStack activityStack3 = this.mTargetStack;
        if (activityStack3 == null) {
            this.mTargetStack = sourceStack;
        } else if (activityStack3 != sourceStack) {
            sourceTask.reparent(activityStack3, true, 0, false, true, "launchToSide");
        }
        if (HwFreeFormUtils.isFreeFormEnable() && sourceStack.inFreeformWindowingMode() && (activityStack = this.mTargetStack) == sourceStack) {
            this.mHwActivityStarterEx.moveFreeFormToFullScreenStackIfNeed(this.mStartActivity, activityStack.inFreeformWindowingMode());
            this.mTargetStack = sourceStack;
        }
        if (this.mTargetStack.topTask() != sourceTask && !this.mAvoidMoveToFront) {
            this.mTargetStack.moveTaskToFrontLocked(sourceTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "sourceTaskToFront");
        } else if (this.mDoResume) {
            ActivityStack stack = this.mRootActivityContainer.getStack(5, 1);
            if (stack != null && stack.isTopActivityVisible()) {
                stack.getWindowConfiguration().setAlwaysOnTop(true);
                HwFreeFormUtils.log("ActivityTaskManager", "set freeformStack always on top.");
            }
            this.mTargetStack.moveToFront("sourceStackToFront");
        }
        if (!this.mAddingToTask) {
            int i = this.mLaunchFlags;
            if ((67108864 & i) != 0) {
                ActivityRecord top2 = sourceTask.performClearTaskLocked(this.mStartActivity, i);
                this.mKeepCurTransition = true;
                if (top2 != null) {
                    ActivityStack.logStartActivity(30003, this.mStartActivity, top2.getTaskRecord());
                    deliverNewIntent(top2);
                    this.mTargetStack.mLastPausedActivity = null;
                    if (this.mDoResume) {
                        this.mRootActivityContainer.resumeFocusedStacksTopActivities();
                    }
                    ActivityOptions.abort(this.mOptions);
                    return 3;
                }
                addOrReparentStartingActivity(sourceTask, "setTaskFromSourceRecord");
                if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                    Slog.v("ActivityTaskManager", "Starting new activity " + this.mStartActivity + " in existing task " + this.mStartActivity.getTaskRecord() + " from source " + this.mSourceRecord);
                }
                return 0;
            }
        }
        if (!(this.mAddingToTask || (this.mLaunchFlags & 131072) == 0 || (top = sourceTask.findActivityInHistoryLocked(this.mStartActivity)) == null)) {
            TaskRecord task = top.getTaskRecord();
            task.moveActivityToFrontLocked(top);
            top.updateOptionsLocked(this.mOptions);
            ActivityStack.logStartActivity(30003, this.mStartActivity, task);
            deliverNewIntent(top);
            this.mTargetStack.mLastPausedActivity = null;
            if (this.mDoResume) {
                this.mRootActivityContainer.resumeFocusedStacksTopActivities();
            }
            return 3;
        }
        addOrReparentStartingActivity(sourceTask, "setTaskFromSourceRecord");
        if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
        }
        return 0;
    }

    private int setTaskFromInTask() {
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mInTask)) {
            Slog.e("ActivityTaskManager", "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
            return 101;
        }
        this.mTargetStack = this.mInTask.getStack();
        ActivityRecord top = this.mInTask.getTopActivity();
        if (top != null && top.mActivityComponent.equals(this.mStartActivity.mActivityComponent) && top.mUserId == this.mStartActivity.mUserId && ((this.mLaunchFlags & 536870912) != 0 || isLaunchModeOneOf(1, 2))) {
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            deliverNewIntent(top);
            return 3;
        } else if (!this.mAddingToTask) {
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            ActivityOptions.abort(this.mOptions);
            return 2;
        } else {
            if (!this.mLaunchParams.mBounds.isEmpty()) {
                ActivityStack stack = this.mRootActivityContainer.getLaunchStack(null, null, this.mInTask, true);
                if (stack != this.mInTask.getStack()) {
                    this.mInTask.reparent(stack, true, 1, false, true, "inTaskToFront");
                    this.mTargetStack = this.mInTask.getStack();
                }
                updateBounds(this.mInTask, this.mLaunchParams.mBounds);
            }
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            addOrReparentStartingActivity(this.mInTask, "setTaskFromInTask");
            if (!ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                return 0;
            }
            Slog.v("ActivityTaskManager", "Starting new activity " + this.mStartActivity + " in explicit task " + this.mStartActivity.getTaskRecord());
            return 0;
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void updateBounds(TaskRecord task, Rect bounds) {
        if (!bounds.isEmpty()) {
            ActivityStack stack = task.getStack();
            if (stack == null || !stack.resizeStackWithLaunchBounds()) {
                task.updateOverrideConfiguration(bounds);
            } else {
                this.mService.resizeStack(stack.mStackId, bounds, true, false, true, -1);
            }
        }
    }

    private int setTaskToCurrentTopOrCreateNewTask() {
        this.mTargetStack = computeStackFocus(this.mStartActivity, false, this.mLaunchFlags, this.mOptions);
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("addingToTopTask");
        }
        ActivityRecord prev = this.mTargetStack.getTopActivity();
        if (this.mRestrictedBgActivity && prev == null && handleBackgroundActivityAbort(this.mStartActivity)) {
            return WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT;
        }
        TaskRecord task = prev != null ? prev.getTaskRecord() : this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.mUserId), this.mStartActivity.info, this.mIntent, null, null, true, this.mStartActivity, this.mSourceRecord, this.mOptions);
        if (this.mRestrictedBgActivity && prev != null && !task.containsAppUid(this.mCallingUid) && handleBackgroundActivityAbort(this.mStartActivity)) {
            return WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT;
        }
        addOrReparentStartingActivity(task, "setTaskToCurrentTopOrCreateNewTask");
        this.mTargetStack.positionChildWindowContainerAtTop(task);
        if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
            Slog.v("ActivityTaskManager", "Starting new activity " + this.mStartActivity + " in new guessed " + this.mStartActivity.getTaskRecord());
        }
        return 0;
    }

    private void addOrReparentStartingActivity(TaskRecord parent, String reason) {
        if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
            Slog.v("ActivityTaskManager", "addOrReparentStartingActivity reason: " + reason);
        }
        if (this.mStartActivity.getTaskRecord() == null || this.mStartActivity.getTaskRecord() == parent) {
            parent.addActivityToTop(this.mStartActivity);
        } else {
            this.mStartActivity.reparent(parent, parent.mActivities.size(), reason);
        }
    }

    private int adjustLaunchFlagsToDocumentMode(ActivityRecord r, boolean launchSingleInstance, boolean launchSingleTask, int launchFlags) {
        if ((launchFlags & 524288) == 0 || (!launchSingleInstance && !launchSingleTask)) {
            int i = r.info.documentLaunchMode;
            if (i == 0) {
                return launchFlags;
            }
            if (i == 1) {
                return launchFlags | 524288;
            }
            if (i == 2) {
                return launchFlags | 524288;
            }
            if (i != 3) {
                return launchFlags;
            }
            return launchFlags & -134217729;
        }
        Slog.i("ActivityTaskManager", "Ignoring FLAG_ACTIVITY_NEW_DOCUMENT, launchMode is \"singleInstance\" or \"singleTask\"");
        return launchFlags & -134742017;
    }

    private ActivityStack computeStackFocus(ActivityRecord r, boolean newTask, int launchFlags, ActivityOptions aOptions) {
        TaskRecord task = r.getTaskRecord();
        ActivityStack stack = getLaunchStack(r, launchFlags, task, aOptions);
        if (ActivityTaskManagerDebugConfig.DEBUG_FOCUS || ActivityTaskManagerDebugConfig.DEBUG_STACK) {
            Slog.d("ActivityTaskManager", "getLaunchStack stack:" + stack);
        }
        if (stack != null) {
            return stack;
        }
        ActivityStack currentStack = task != null ? task.getStack() : null;
        ActivityStack focusedStack = this.mRootActivityContainer.getTopDisplayFocusedStack();
        if (currentStack != null) {
            if (focusedStack != currentStack) {
                if (ActivityTaskManagerDebugConfig.DEBUG_FOCUS || ActivityTaskManagerDebugConfig.DEBUG_STACK) {
                    Slog.d("ActivityTaskManager", "computeStackFocus: Setting focused stack to r=" + r + " task=" + task);
                }
            } else if (ActivityTaskManagerDebugConfig.DEBUG_FOCUS || ActivityTaskManagerDebugConfig.DEBUG_STACK) {
                Slog.d("ActivityTaskManager", "computeStackFocus: Focused stack already=" + focusedStack);
            }
            return currentStack;
        } else if (canLaunchIntoFocusedStack(r, newTask)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_FOCUS || ActivityTaskManagerDebugConfig.DEBUG_STACK) {
                Slog.d("ActivityTaskManager", "computeStackFocus: Have a focused stack=" + focusedStack);
            }
            return focusedStack;
        } else {
            int i = this.mPreferredDisplayId;
            if (i != 0 && (stack = this.mRootActivityContainer.getValidLaunchStackOnDisplay(i, r, aOptions, this.mLaunchParams)) == null) {
                if (ActivityTaskManagerDebugConfig.DEBUG_FOCUS || ActivityTaskManagerDebugConfig.DEBUG_STACK) {
                    Slog.d("ActivityTaskManager", "computeStackFocus: Can't launch on mPreferredDisplayId=" + this.mPreferredDisplayId + ", looking on all displays.");
                }
                stack = this.mRootActivityContainer.getNextValidLaunchStack(r, this.mPreferredDisplayId);
            }
            if (stack == null) {
                stack = this.mRootActivityContainer.getLaunchStack(r, aOptions, task, true);
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_FOCUS || ActivityTaskManagerDebugConfig.DEBUG_STACK) {
                Slog.d("ActivityTaskManager", "computeStackFocus: New stack r=" + r + " stackId=" + stack.mStackId);
            }
            return stack;
        }
    }

    private boolean canLaunchIntoFocusedStack(ActivityRecord r, boolean newTask) {
        boolean canUseFocusedStack;
        ActivityStack focusedStack = this.mRootActivityContainer.getTopDisplayFocusedStack();
        if (focusedStack.isActivityTypeAssistant()) {
            canUseFocusedStack = r.isActivityTypeAssistant();
        } else {
            int windowingMode = focusedStack.getWindowingMode();
            if (windowingMode == 1) {
                canUseFocusedStack = true;
            } else if (windowingMode == 3 || windowingMode == 4) {
                canUseFocusedStack = r.supportsSplitScreenWindowingMode();
            } else if (windowingMode == 5) {
                canUseFocusedStack = r.supportsFreeform();
            } else if (HwPCUtils.isExtDynamicStack(focusedStack.mStackId)) {
                return false;
            } else {
                canUseFocusedStack = !focusedStack.isOnHomeDisplay() && r.canBeLaunchedOnDisplay(focusedStack.mDisplayId);
            }
        }
        return canUseFocusedStack && !newTask && this.mPreferredDisplayId == focusedStack.mDisplayId;
    }

    private ActivityStack getLaunchStack(ActivityRecord r, int launchFlags, TaskRecord task, ActivityOptions aOptions) {
        TaskRecord taskRecord = this.mReuseTask;
        if (taskRecord != null) {
            return taskRecord.getStack();
        }
        boolean onTop = true;
        if ((launchFlags & 4096) == 0 || this.mPreferredDisplayId != 0) {
            if (aOptions != null && aOptions.getAvoidMoveToFront()) {
                onTop = false;
            }
            moveFreeFormStackIfNeed(r, aOptions);
            return this.mRootActivityContainer.getLaunchStack(r, aOptions, task, onTop, this.mLaunchParams);
        }
        ActivityStack focusedStack = this.mRootActivityContainer.getTopDisplayFocusedStack();
        ActivityStack parentStack = task != null ? task.getStack() : focusedStack;
        if (parentStack != focusedStack) {
            return parentStack;
        }
        if (focusedStack != null && task == focusedStack.topTask()) {
            return focusedStack;
        }
        if (parentStack == null || !parentStack.inSplitScreenPrimaryWindowingMode()) {
            ActivityStack dockedStack = this.mRootActivityContainer.getDefaultDisplay().getSplitScreenPrimaryStack();
            if (dockedStack == null || dockedStack.shouldBeVisible(r)) {
                return dockedStack;
            }
            return this.mRootActivityContainer.getLaunchStack(r, aOptions, task, true);
        }
        return parentStack.getDisplay().getOrCreateStack(4, this.mRootActivityContainer.resolveActivityType(r, this.mOptions, task), true);
    }

    private void moveFreeFormStackIfNeed(ActivityRecord startActivity, ActivityOptions activityOptions) {
        if (startActivity != null && activityOptions != null && activityOptions.getLaunchWindowingMode() == 5) {
            boolean isFreeFormExist = false;
            ActivityStack freeFormStack = this.mService.getRootActivityContainer().getStack(5, 1);
            if (freeFormStack != null) {
                ActivityRecord topActivity = freeFormStack.topRunningActivityLocked();
                isFreeFormExist = true;
                if (topActivity != null && this.mService.isInFreeformWhiteList(startActivity.packageName) && this.mService.isInFreeformWhiteList(topActivity.packageName) && ((!startActivity.packageName.equals(topActivity.packageName) || startActivity.mUserId != topActivity.mUserId) && HwFreeFormUtils.getFreeFormStackVisible())) {
                    this.mSupervisor.mHwActivityStackSupervisorEx.removeFreeFromStackLocked();
                    isFreeFormExist = false;
                }
            }
            String activityTitle = startActivity.info.getComponentName().flattenToShortString();
            if (!isFreeFormExist && HwFreeFormUtils.sExitFreeformActivity.contains(activityTitle)) {
                activityOptions.setLaunchWindowingMode(1);
            }
        }
    }

    private boolean isLaunchModeOneOf(int mode1, int mode2) {
        int i = this.mLaunchMode;
        return mode1 == i || mode2 == i;
    }

    static boolean isDocumentLaunchesIntoExisting(int flags) {
        return (524288 & flags) != 0 && (134217728 & flags) == 0;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setIntent(Intent intent) {
        this.mRequest.intent = intent;
        return this;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public Intent getIntent() {
        return this.mRequest.intent;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public int getCallingUid() {
        return this.mRequest.callingUid;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setReason(String reason) {
        this.mRequest.reason = reason;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setCaller(IApplicationThread caller) {
        this.mRequest.caller = caller;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setEphemeralIntent(Intent intent) {
        this.mRequest.ephemeralIntent = intent;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setResolvedType(String type) {
        this.mRequest.resolvedType = type;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setActivityInfo(ActivityInfo info) {
        this.mRequest.activityInfo = info;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setResolveInfo(ResolveInfo info) {
        this.mRequest.resolveInfo = info;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setVoiceSession(IVoiceInteractionSession voiceSession) {
        this.mRequest.voiceSession = voiceSession;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setVoiceInteractor(IVoiceInteractor voiceInteractor) {
        this.mRequest.voiceInteractor = voiceInteractor;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setResultTo(IBinder resultTo) {
        this.mRequest.resultTo = resultTo;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setResultWho(String resultWho) {
        this.mRequest.resultWho = resultWho;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setRequestCode(int requestCode) {
        this.mRequest.requestCode = requestCode;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setCallingPid(int pid) {
        this.mRequest.callingPid = pid;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setCallingUid(int uid) {
        this.mRequest.callingUid = uid;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setCallingPackage(String callingPackage) {
        this.mRequest.callingPackage = callingPackage;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setRealCallingPid(int pid) {
        this.mRequest.realCallingPid = pid;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setRealCallingUid(int uid) {
        this.mRequest.realCallingUid = uid;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setStartFlags(int startFlags) {
        this.mRequest.startFlags = startFlags;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setActivityOptions(SafeActivityOptions options) {
        this.mRequest.activityOptions = options;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setActivityOptions(Bundle bOptions) {
        return setActivityOptions(SafeActivityOptions.fromBundle(bOptions));
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setIgnoreTargetSecurity(boolean ignoreTargetSecurity) {
        this.mRequest.ignoreTargetSecurity = ignoreTargetSecurity;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setFilterCallingUid(int filterCallingUid) {
        this.mRequest.filterCallingUid = filterCallingUid;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setComponentSpecified(boolean componentSpecified) {
        this.mRequest.componentSpecified = componentSpecified;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setOutActivity(ActivityRecord[] outActivity) {
        this.mRequest.outActivity = outActivity;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setInTask(TaskRecord inTask) {
        this.mRequest.inTask = inTask;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setWaitResult(WaitResult result) {
        this.mRequest.waitResult = result;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setProfilerInfo(ProfilerInfo info) {
        this.mRequest.profilerInfo = info;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setGlobalConfiguration(Configuration config) {
        this.mRequest.globalConfig = config;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setUserId(int userId) {
        this.mRequest.userId = userId;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setMayWait(int userId) {
        Request request = this.mRequest;
        request.mayWait = true;
        request.userId = userId;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setAllowPendingRemoteAnimationRegistryLookup(boolean allowLookup) {
        this.mRequest.allowPendingRemoteAnimationRegistryLookup = allowLookup;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setOriginatingPendingIntent(PendingIntentRecord originatingPendingIntent) {
        this.mRequest.originatingPendingIntent = originatingPendingIntent;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ActivityStarter setAllowBackgroundActivityStart(boolean allowBackgroundActivityStart) {
        this.mRequest.allowBackgroundActivityStart = allowBackgroundActivityStart;
        return this;
    }

    /* access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mCurrentUser=");
        pw.println(this.mRootActivityContainer.mCurrentUser);
        pw.print(prefix2);
        pw.print("mLastStartReason=");
        pw.println(this.mLastStartReason);
        pw.print(prefix2);
        pw.print("mLastStartActivityTimeMs=");
        pw.println(DateFormat.getDateTimeInstance().format(new Date(this.mLastStartActivityTimeMs)));
        pw.print(prefix2);
        pw.print("mLastStartActivityResult=");
        pw.println(this.mLastStartActivityResult);
        boolean z = false;
        ActivityRecord r = this.mLastStartActivityRecord[0];
        if (r != null) {
            pw.print(prefix2);
            pw.println("mLastStartActivityRecord:");
            r.dump(pw, prefix2 + "  ");
        }
        if (this.mStartActivity != null) {
            pw.print(prefix2);
            pw.println("mStartActivity:");
            this.mStartActivity.dump(pw, prefix2 + "  ");
        }
        if (this.mIntent != null) {
            pw.print(prefix2);
            pw.print("mIntent=");
            pw.println(this.mIntent);
        }
        if (this.mOptions != null) {
            pw.print(prefix2);
            pw.print("mOptions=");
            pw.println(this.mOptions);
        }
        pw.print(prefix2);
        pw.print("mLaunchSingleTop=");
        pw.print(1 == this.mLaunchMode);
        pw.print(" mLaunchSingleInstance=");
        pw.print(3 == this.mLaunchMode);
        pw.print(" mLaunchSingleTask=");
        if (2 == this.mLaunchMode) {
            z = true;
        }
        pw.println(z);
        pw.print(prefix2);
        pw.print("mLaunchFlags=0x");
        pw.print(Integer.toHexString(this.mLaunchFlags));
        pw.print(" mDoResume=");
        pw.print(this.mDoResume);
        pw.print(" mAddingToTask=");
        pw.println(this.mAddingToTask);
    }

    protected static boolean clearFrpRestricted(Context context, int userId) {
        return Settings.Secure.putIntForUser(context.getContentResolver(), SUW_FRP_STATE, 0, userId);
    }

    protected static boolean isFrpRestricted(Context context, int userId) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), SUW_FRP_STATE, 0, userId) == 1;
    }
}
