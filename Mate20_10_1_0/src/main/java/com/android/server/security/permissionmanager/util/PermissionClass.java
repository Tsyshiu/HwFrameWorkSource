package com.android.server.security.permissionmanager.util;

import android.util.Slog;

public final class PermissionClass {
    public static final long ALWAYS_FORBIDDEN_PERMS = 8591241847L;
    private static final long CLASS_E = 1091575808;
    private static final long CLASS_F = 4294967296L;
    public static final long PERMISSION_GROUP_CALL_LOG = 8589967362L;
    public static final long PERMISSION_GROUP_CONTACT = 16897;
    public static final long PERMISSION_GROUP_MSG = 135204;
    public static final long PERMISSION_GROUP_PHONE = 65616;
    private static final String TAG = "PermissionClass";

    private PermissionClass() {
        Slog.d(TAG, "create PermissionClass");
    }

    public static boolean isClassEType(long permType) {
        return (CLASS_E & permType) != 0;
    }

    public static boolean isClassFType(long permType) {
        return (4294967296L & permType) != 0;
    }

    public static boolean isSmsGroup(long permType) {
        return (PERMISSION_GROUP_MSG & permType) != 0;
    }
}
