package com.android.server.pm;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Slog;
import com.android.server.am.HwActivityManagerService;
import java.io.File;
import java.util.List;

public class HwUserManagerService extends UserManagerService {
    private static final int CREATE_USER_STATUS = 0;
    private static final int DELETE_USER_STATUS = 1;
    private static final String TAG = "HwUserManagerService";
    private static boolean isSupportJni;
    private static HwUserManagerService sInstance = null;
    private Context mContext;

    private native void nativeSendUserChangedNotification(int i, int i2);

    static {
        isSupportJni = false;
        try {
            System.loadLibrary("hwtee_jni");
            isSupportJni = true;
        } catch (UnsatisfiedLinkError e) {
            Slog.e(TAG, "can not find lib hwtee_jni");
            isSupportJni = false;
        }
    }

    public HwUserManagerService(Context context, PackageManagerService pm, UserDataPreparer userDataPreparer, Object packagesLock) {
        super(context, pm, userDataPreparer, packagesLock);
        this.mContext = context;
        sInstance = this;
    }

    public static synchronized HwUserManagerService getInstance() {
        HwUserManagerService hwUserManagerService;
        synchronized (HwUserManagerService.class) {
            hwUserManagerService = sInstance;
        }
        return hwUserManagerService;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != 1001) {
            return HwUserManagerService.super.onTransact(code, data, reply, flags);
        }
        Flog.i(900, "onTransact MKDIR_FOR_USER_TRANSACTION.");
        if (data == null) {
            return false;
        }
        createUserDir(data.readInt());
        return true;
    }

    public UserInfo createProfileForUser(String name, int flags, int userId, String[] disallowedPackages) {
        if (isStorageLow()) {
            return null;
        }
        boolean isClonedProfile = (67108864 & flags) != 0;
        UserInfo parent = null;
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && isClonedProfile) {
            if ("1".equals(SystemProperties.get("persist.sys.primarysd", "0"))) {
                Slog.i(TAG, "current default location is external sdcard and forbid to create user");
                return null;
            } else if (userId != 0) {
                return null;
            } else {
                for (UserInfo user : HwUserManagerService.super.getProfiles(userId, true)) {
                    if (user.isClonedProfile()) {
                        return null;
                    }
                    if (user.id == userId) {
                        parent = user;
                        if (!parent.canHaveProfile()) {
                            return null;
                        }
                    }
                }
            }
        }
        UserInfo userInfo = HwUserManagerService.super.createProfileForUser(name, flags, userId, disallowedPackages);
        if (!(!isClonedProfile || parent == null || userInfo == null)) {
            pretreatClonedProfile(this.mPm, parent.id, userInfo.id);
        }
        if (userInfo != null) {
            hwCreateUser(userInfo.id);
        }
        return userInfo;
    }

    public UserInfo createUser(String name, int flags) {
        if (isStorageLow()) {
            return null;
        }
        if ((33554432 & flags) != 0) {
            for (UserInfo info : getUsers(true)) {
                if (info.isHwHiddenSpace()) {
                    Slog.e(TAG, "Hidden space already exist!");
                    return null;
                }
            }
        }
        UserInfo userInfo = HwUserManagerService.super.createUser(name, flags);
        if (userInfo == null) {
            return null;
        }
        if (userInfo.isGuest()) {
            Flog.i(900, "Create one guest, disable setup activity.");
            disableSetupActivity(userInfo.id);
        }
        hwCreateUser(userInfo.id);
        return userInfo;
    }

    /* access modifiers changed from: package-private */
    public void disableSetupActivity(int userId) {
        Intent mainIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT");
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            ResolveInfo info = pm.resolveIntent(mainIntent, mainIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0, userId);
            if (!(info == null || info.activityInfo == null)) {
                if (info.activityInfo.applicationInfo != null) {
                    if (info.priority <= 0) {
                        Flog.i(900, "disableSetupActivity did not found setup activity.");
                        return;
                    }
                    pm.setApplicationEnabledSetting(info.activityInfo.applicationInfo.packageName, 3, 0, userId, (String) null);
                    long identity = Binder.clearCallingIdentity();
                    Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userId);
                    Binder.restoreCallingIdentity(identity);
                    return;
                }
            }
            Flog.i(900, "disableSetupActivity found resolveinfo null.");
        } catch (RemoteException e) {
            Flog.i(900, "disableSetupActivity remote error " + e);
        }
    }

    /* access modifiers changed from: package-private */
    public void finishRemoveUser(int userHandle) {
        HwUserManagerService.super.finishRemoveUser(userHandle);
        hwRemoveUser(userHandle);
    }

    private void hwCreateUser(int userid) {
        if (userid > 0 && isSupportJni) {
            Slog.i(TAG, "native create user " + userid);
            nativeSendUserChangedNotification(0, userid);
        }
    }

    private void hwRemoveUser(int userid) {
        if (userid > 0 && isSupportJni) {
            Slog.i(TAG, "native remove user " + userid);
            nativeSendUserChangedNotification(1, userid);
        }
    }

    private void createUserDir(int userId) {
        File userDir = Environment.getUserSystemDirectory(userId);
        if (userDir != null && !userDir.exists() && !userDir.mkdir()) {
            Slog.w(TAG, "Failed to create user directory for " + userId);
        }
    }

    private boolean isStorageLow() {
        boolean isStorageLow = ServiceManager.getService("package").isStorageLow();
        Slog.i(TAG, "PackageManagerService.isStorageLow() = " + isStorageLow);
        return isStorageLow;
    }

    private void pretreatClonedProfile(PackageManagerService pm, int parentUserId, int clonedProfileUserId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            long callingId = Binder.clearCallingIdentity();
            try {
                pm.deleteNonRequiredAppsForClone(clonedProfileUserId, true);
                restoreDataForClone(pm, parentUserId, clonedProfileUserId);
                pm.flushPackageRestrictionsAsUser(clonedProfileUserId);
                HwUserManagerService.super.setUserRestriction("no_outgoing_calls", false, clonedProfileUserId);
                HwUserManagerService.super.setUserRestriction("no_sms", false, clonedProfileUserId);
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, clonedProfileUserId);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    private void restoreDataForClone(PackageManagerService pm, int parentUserId, int clonedProfileUserId) {
        if (parentUserId == 0 && pm != null) {
            String cloneAppList = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "clone_app_list", parentUserId);
            if (!TextUtils.isEmpty(cloneAppList)) {
                String[] pkgs = cloneAppList.split(";");
                for (String pkg : pkgs) {
                    if (!TextUtils.isEmpty(pkg) && pm.getPackageInfo(pkg, 0, parentUserId) != null) {
                        Slog.i(TAG, "Install existing package [" + pkg + "] as user " + clonedProfileUserId);
                        pm.installExistingPackageAsUser(pkg, clonedProfileUserId, 0, 0, (List) null);
                        pm.setPackageStoppedState(pkg, false, clonedProfileUserId);
                    }
                }
            }
        }
    }

    public boolean isClonedProfile(int userId) {
        if (userId == 0) {
            return false;
        }
        boolean isClonedProfile = false;
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = HwUserManagerService.super.getUserInfo(userId);
            if (userInfo != null) {
                isClonedProfile = userInfo.isClonedProfile();
            }
            return isClonedProfile;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public UserInfo getUserInfo(int userId) {
        int callingUserId = UserHandle.getUserId(Binder.getCallingUid());
        if (!isClonedProfile(userId) && !isClonedProfile(callingUserId)) {
            return HwUserManagerService.super.getUserInfo(userId);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return HwUserManagerService.super.getUserInfo(userId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
