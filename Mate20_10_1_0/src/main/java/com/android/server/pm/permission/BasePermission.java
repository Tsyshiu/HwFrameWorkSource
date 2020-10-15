package com.android.server.pm.permission;

import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.devicepolicy.HwLog;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.PackageSettingBase;
import com.android.server.pm.Settings;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class BasePermission {
    static final String TAG = "PackageManager";
    public static final int TYPE_BUILTIN = 1;
    public static final int TYPE_DYNAMIC = 2;
    public static final int TYPE_NORMAL = 0;
    private int[] gids;
    final String name;
    PermissionInfo pendingPermissionInfo;
    private boolean perUser;
    PackageParser.Permission perm;
    int protectionLevel = 2;
    String sourcePackageName;
    PackageSettingBase sourcePackageSetting;
    final int type;
    int uid;

    @Retention(RetentionPolicy.SOURCE)
    public @interface PermissionType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtectionLevel {
    }

    public BasePermission(String _name, String _sourcePackageName, int _type) {
        this.name = _name;
        this.sourcePackageName = _sourcePackageName;
        this.type = _type;
    }

    public String toString() {
        return "BasePermission{" + Integer.toHexString(System.identityHashCode(this)) + HwLog.PREFIX + this.name + "}";
    }

    public String getName() {
        return this.name;
    }

    public int getProtectionLevel() {
        return this.protectionLevel;
    }

    public String getSourcePackageName() {
        return this.sourcePackageName;
    }

    public PackageSettingBase getSourcePackageSetting() {
        return this.sourcePackageSetting;
    }

    public Signature[] getSourceSignatures() {
        return this.sourcePackageSetting.getSignatures();
    }

    public int getType() {
        return this.type;
    }

    public int getUid() {
        return this.uid;
    }

    public void setGids(int[] gids2, boolean perUser2) {
        this.gids = gids2;
        this.perUser = perUser2;
    }

    public void setPermission(PackageParser.Permission perm2) {
        this.perm = perm2;
    }

    public void setSourcePackageSetting(PackageSettingBase sourcePackageSetting2) {
        this.sourcePackageSetting = sourcePackageSetting2;
    }

    public int[] computeGids(int userId) {
        if (!this.perUser) {
            return this.gids;
        }
        int[] userGids = new int[this.gids.length];
        int i = 0;
        while (true) {
            int[] iArr = this.gids;
            if (i >= iArr.length) {
                return userGids;
            }
            userGids[i] = UserHandle.getUid(userId, iArr[i]);
            i++;
        }
    }

    public int calculateFootprint(BasePermission perm2) {
        if (this.uid == perm2.uid) {
            return perm2.name.length() + perm2.perm.info.calculateFootprint();
        }
        return 0;
    }

    public boolean isPermission(PackageParser.Permission perm2) {
        return this.perm == perm2;
    }

    public boolean isDynamic() {
        return this.type == 2;
    }

    public boolean isNormal() {
        return (this.protectionLevel & 15) == 0;
    }

    public boolean isRuntime() {
        return (this.protectionLevel & 15) == 1;
    }

    public boolean isRemoved() {
        PackageParser.Permission permission = this.perm;
        return (permission == null || permission.info == null || (this.perm.info.flags & 2) == 0) ? false : true;
    }

    public boolean isSoftRestricted() {
        PackageParser.Permission permission = this.perm;
        return (permission == null || permission.info == null || (this.perm.info.flags & 8) == 0) ? false : true;
    }

    public boolean isHardRestricted() {
        PackageParser.Permission permission = this.perm;
        return (permission == null || permission.info == null || (this.perm.info.flags & 4) == 0) ? false : true;
    }

    public boolean isHardOrSoftRestricted() {
        PackageParser.Permission permission = this.perm;
        return (permission == null || permission.info == null || (this.perm.info.flags & 12) == 0) ? false : true;
    }

    public boolean isImmutablyRestricted() {
        PackageParser.Permission permission = this.perm;
        return (permission == null || permission.info == null || (this.perm.info.flags & 16) == 0) ? false : true;
    }

    public boolean isSignature() {
        return (this.protectionLevel & 15) == 2;
    }

    public boolean isAppOp() {
        return (this.protectionLevel & 64) != 0;
    }

    public boolean isDevelopment() {
        return isSignature() && (this.protectionLevel & 32) != 0;
    }

    public boolean isInstaller() {
        return (this.protectionLevel & 256) != 0;
    }

    public boolean isInstant() {
        return (this.protectionLevel & 4096) != 0;
    }

    public boolean isOEM() {
        return (this.protectionLevel & DumpState.DUMP_KEYSETS) != 0;
    }

    public boolean isPre23() {
        return (this.protectionLevel & 128) != 0;
    }

    public boolean isPreInstalled() {
        return (this.protectionLevel & 1024) != 0;
    }

    public boolean isPrivileged() {
        return (this.protectionLevel & 16) != 0;
    }

    public boolean isRuntimeOnly() {
        return (this.protectionLevel & 8192) != 0;
    }

    public boolean isSetup() {
        return (this.protectionLevel & 2048) != 0;
    }

    public boolean isVerifier() {
        return (this.protectionLevel & 512) != 0;
    }

    public boolean isVendorPrivileged() {
        return (this.protectionLevel & 32768) != 0;
    }

    public boolean isSystemTextClassifier() {
        return (this.protectionLevel & 65536) != 0;
    }

    public boolean isWellbeing() {
        return (this.protectionLevel & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0;
    }

    public boolean isDocumenter() {
        return (this.protectionLevel & DumpState.DUMP_DOMAIN_PREFERRED) != 0;
    }

    public boolean isConfigurator() {
        return (this.protectionLevel & DumpState.DUMP_FROZEN) != 0;
    }

    public boolean isIncidentReportApprover() {
        return (this.protectionLevel & DumpState.DUMP_DEXOPT) != 0;
    }

    public boolean isAppPredictor() {
        return (this.protectionLevel & DumpState.DUMP_COMPILER_STATS) != 0;
    }

    public void transfer(String origPackageName, String newPackageName) {
        if (origPackageName.equals(this.sourcePackageName)) {
            this.sourcePackageName = newPackageName;
            this.sourcePackageSetting = null;
            this.perm = null;
            PermissionInfo permissionInfo = this.pendingPermissionInfo;
            if (permissionInfo != null) {
                permissionInfo.packageName = newPackageName;
            }
            this.uid = 0;
            setGids(null, false);
        }
    }

    public boolean addToTree(int protectionLevel2, PermissionInfo info, BasePermission tree) {
        PackageParser.Permission permission;
        boolean changed = this.protectionLevel != protectionLevel2 || (permission = this.perm) == null || this.uid != tree.uid || !permission.owner.equals(tree.perm.owner) || !comparePermissionInfos(this.perm.info, info);
        this.protectionLevel = protectionLevel2;
        PermissionInfo info2 = new PermissionInfo(info);
        info2.protectionLevel = protectionLevel2;
        this.perm = new PackageParser.Permission(tree.perm.owner, info2);
        this.perm.info.packageName = tree.perm.info.packageName;
        this.uid = tree.uid;
        return changed;
    }

    public void updateDynamicPermission(Collection<BasePermission> permissionTrees) {
        BasePermission tree;
        PackageParser.Permission permission;
        if (PackageManagerService.DEBUG_SETTINGS) {
            Log.v(TAG, "Dynamic permission: name=" + getName() + " pkg=" + getSourcePackageName() + " info=" + this.pendingPermissionInfo);
        }
        if (this.sourcePackageSetting == null && this.pendingPermissionInfo != null && (tree = findPermissionTree(permissionTrees, this.name)) != null && (permission = tree.perm) != null) {
            this.sourcePackageSetting = tree.sourcePackageSetting;
            this.perm = new PackageParser.Permission(permission.owner, new PermissionInfo(this.pendingPermissionInfo));
            this.perm.info.packageName = tree.perm.info.packageName;
            this.perm.info.name = this.name;
            this.uid = tree.uid;
        }
    }

    static BasePermission createOrUpdate(BasePermission bp, PackageParser.Permission p, PackageParser.Package pkg, Collection<BasePermission> permissionTrees, boolean chatty) {
        PackageSettingBase pkgSetting = (PackageSettingBase) pkg.mExtras;
        if (bp != null && !Objects.equals(bp.sourcePackageName, p.info.packageName)) {
            PackageParser.Permission permission = bp.perm;
            boolean currentOwnerIsSystem = permission != null && permission.owner.isSystem();
            if (p.owner.isSystem()) {
                if (bp.type == 1 && bp.perm == null) {
                    bp.sourcePackageSetting = pkgSetting;
                    bp.perm = p;
                    bp.uid = pkg.applicationInfo.uid;
                    bp.sourcePackageName = p.info.packageName;
                    p.info.flags |= 1073741824;
                } else if (!currentOwnerIsSystem) {
                    PackageManagerService.reportSettingsProblem(5, "New decl " + p.owner + " of permission  " + p.info.name + " is system; overriding " + bp.sourcePackageName);
                    bp = null;
                }
            }
        }
        if (bp == null) {
            bp = new BasePermission(p.info.name, p.info.packageName, 0);
        }
        StringBuilder r = null;
        if (bp.perm == null) {
            String str = bp.sourcePackageName;
            if (str == null || str.equals(p.info.packageName)) {
                BasePermission tree = findPermissionTree(permissionTrees, p.info.name);
                if (tree == null || tree.sourcePackageName.equals(p.info.packageName)) {
                    bp.sourcePackageSetting = pkgSetting;
                    bp.perm = p;
                    bp.uid = pkg.applicationInfo.uid;
                    bp.sourcePackageName = p.info.packageName;
                    PermissionInfo permissionInfo = p.info;
                    permissionInfo.flags = 1073741824 | permissionInfo.flags;
                    if (chatty) {
                        if (0 == 0) {
                            r = new StringBuilder(256);
                        } else {
                            r.append(' ');
                        }
                        r.append(p.info.name);
                    }
                } else {
                    Slog.w(TAG, "Permission " + p.info.name + " from package " + p.info.packageName + " ignored: base tree " + tree.name + " is from package " + tree.sourcePackageName);
                }
            } else {
                Slog.w(TAG, "Permission " + p.info.name + " from package " + p.info.packageName + " ignored: original from " + bp.sourcePackageName);
            }
        } else if (chatty) {
            if (0 == 0) {
                r = new StringBuilder(256);
            } else {
                r.append(' ');
            }
            r.append("DUP:");
            r.append(p.info.name);
        }
        if (bp.perm == p) {
            bp.protectionLevel = p.info.protectionLevel;
        }
        if (PackageManagerService.DEBUG_PACKAGE_SCANNING && r != null) {
            Log.d(TAG, "  Permissions: " + ((Object) r));
        }
        return bp;
    }

    static BasePermission enforcePermissionTree(Collection<BasePermission> permissionTrees, String permName, int callingUid) {
        BasePermission bp;
        if (permName == null || (bp = findPermissionTree(permissionTrees, permName)) == null) {
            throw new SecurityException("No permission tree found for " + permName);
        } else if (bp.uid == UserHandle.getAppId(callingUid)) {
            return bp;
        } else {
            throw new SecurityException("Calling uid " + callingUid + " is not allowed to add to permission tree " + bp.name + " owned by uid " + bp.uid);
        }
    }

    public void enforceDeclaredUsedAndRuntimeOrDevelopment(PackageParser.Package pkg) {
        PermissionsState permsState = ((PackageSetting) pkg.mExtras).getPermissionsState();
        int index = pkg.requestedPermissions.indexOf(this.name);
        if (!permsState.hasRequestedPermission(this.name) && index == -1) {
            throw new SecurityException("Package " + pkg.packageName + " has not requested permission " + this.name);
        } else if (!isRuntime() && !isDevelopment()) {
            throw new SecurityException("Permission " + this.name + " requested by " + pkg.packageName + " is not a changeable permission type");
        }
    }

    private static BasePermission findPermissionTree(Collection<BasePermission> permissionTrees, String permName) {
        for (BasePermission bp : permissionTrees) {
            if (permName.startsWith(bp.name) && permName.length() > bp.name.length() && permName.charAt(bp.name.length()) == '.') {
                return bp;
            }
        }
        return null;
    }

    public PermissionInfo generatePermissionInfo(String groupName, int flags) {
        if (groupName == null) {
            PackageParser.Permission permission = this.perm;
            if (permission == null || permission.info.group == null) {
                return generatePermissionInfo(this.protectionLevel, flags);
            }
            return null;
        }
        PackageParser.Permission permission2 = this.perm;
        if (permission2 == null || !groupName.equals(permission2.info.group)) {
            return null;
        }
        return PackageParser.generatePermissionInfo(this.perm, flags);
    }

    public PermissionInfo generatePermissionInfo(int adjustedProtectionLevel, int flags) {
        if (this.perm != null) {
            boolean protectionLevelChanged = this.protectionLevel != adjustedProtectionLevel;
            PermissionInfo permissionInfo = PackageParser.generatePermissionInfo(this.perm, flags);
            if (!protectionLevelChanged || permissionInfo != this.perm.info) {
                return permissionInfo;
            }
            PermissionInfo permissionInfo2 = new PermissionInfo(permissionInfo);
            permissionInfo2.protectionLevel = adjustedProtectionLevel;
            return permissionInfo2;
        }
        PermissionInfo permissionInfo3 = new PermissionInfo();
        String str = this.name;
        permissionInfo3.name = str;
        permissionInfo3.packageName = this.sourcePackageName;
        permissionInfo3.nonLocalizedLabel = str;
        permissionInfo3.protectionLevel = this.protectionLevel;
        return permissionInfo3;
    }

    public static boolean readLPw(Map<String, BasePermission> out, XmlPullParser parser) {
        if (!parser.getName().equals(Settings.TAG_ITEM)) {
            return false;
        }
        String name2 = parser.getAttributeValue(null, Settings.ATTR_NAME);
        String sourcePackage = parser.getAttributeValue(null, "package");
        String ptype = parser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
        if (name2 == null || sourcePackage == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: permissions has no name at " + parser.getPositionDescription());
            return false;
        }
        boolean dynamic = "dynamic".equals(ptype);
        BasePermission bp = out.get(name2);
        if (bp == null || bp.type != 1) {
            bp = new BasePermission(name2.intern(), sourcePackage, dynamic ? 2 : 0);
        }
        bp.protectionLevel = readInt(parser, null, "protection", 0);
        bp.protectionLevel = PermissionInfo.fixProtectionLevel(bp.protectionLevel);
        if (dynamic) {
            PermissionInfo pi = new PermissionInfo();
            pi.packageName = sourcePackage.intern();
            pi.name = name2.intern();
            pi.icon = readInt(parser, null, "icon", 0);
            pi.nonLocalizedLabel = parser.getAttributeValue(null, "label");
            pi.protectionLevel = bp.protectionLevel;
            bp.pendingPermissionInfo = pi;
        }
        out.put(bp.name, bp);
        return true;
    }

    private static int readInt(XmlPullParser parser, String ns, String name2, int defValue) {
        String v = parser.getAttributeValue(ns, name2);
        if (v == null) {
            return defValue;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: attribute " + name2 + " has bad integer value " + v + " at " + parser.getPositionDescription());
            return defValue;
        }
    }

    public void writeLPr(XmlSerializer serializer) throws IOException {
        if (this.sourcePackageName != null) {
            serializer.startTag(null, Settings.TAG_ITEM);
            serializer.attribute(null, Settings.ATTR_NAME, this.name);
            serializer.attribute(null, "package", this.sourcePackageName);
            int i = this.protectionLevel;
            if (i != 0) {
                serializer.attribute(null, "protection", Integer.toString(i));
            }
            if (this.type == 2) {
                PackageParser.Permission permission = this.perm;
                PermissionInfo pi = permission != null ? permission.info : this.pendingPermissionInfo;
                if (pi != null) {
                    serializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "dynamic");
                    if (pi.icon != 0) {
                        serializer.attribute(null, "icon", Integer.toString(pi.icon));
                    }
                    if (pi.nonLocalizedLabel != null) {
                        serializer.attribute(null, "label", pi.nonLocalizedLabel.toString());
                    }
                }
            }
            serializer.endTag(null, Settings.TAG_ITEM);
        }
    }

    private static boolean compareStrings(CharSequence s1, CharSequence s2) {
        if (s1 == null) {
            if (s2 == null) {
                return true;
            }
            return false;
        } else if (s2 != null && s1.getClass() == s2.getClass()) {
            return s1.equals(s2);
        } else {
            return false;
        }
    }

    private static boolean comparePermissionInfos(PermissionInfo pi1, PermissionInfo pi2) {
        if (pi1.icon == pi2.icon && pi1.logo == pi2.logo && pi1.protectionLevel == pi2.protectionLevel && compareStrings(pi1.name, pi2.name) && compareStrings(pi1.nonLocalizedLabel, pi2.nonLocalizedLabel) && compareStrings(pi1.packageName, pi2.packageName)) {
            return true;
        }
        return false;
    }

    public boolean dumpPermissionsLPr(PrintWriter pw, String packageName, Set<String> permissionNames, boolean readEnforced, boolean printedSomething, DumpState dumpState) {
        if (packageName != null && !packageName.equals(this.sourcePackageName)) {
            return false;
        }
        if (permissionNames != null && !permissionNames.contains(this.name)) {
            return false;
        }
        if (!printedSomething) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            pw.println("Permissions:");
        }
        pw.print("  Permission [");
        pw.print(this.name);
        pw.print("] (");
        pw.print(Integer.toHexString(System.identityHashCode(this)));
        pw.println("):");
        pw.print("    sourcePackage=");
        pw.println(this.sourcePackageName);
        pw.print("    uid=");
        pw.print(this.uid);
        pw.print(" gids=");
        pw.print(Arrays.toString(computeGids(0)));
        pw.print(" type=");
        pw.print(this.type);
        pw.print(" prot=");
        pw.println(PermissionInfo.protectionToString(this.protectionLevel));
        if (this.perm != null) {
            pw.print("    perm=");
            pw.println(this.perm);
            if ((this.perm.info.flags & 1073741824) == 0 || (this.perm.info.flags & 2) != 0) {
                pw.print("    flags=0x");
                pw.println(Integer.toHexString(this.perm.info.flags));
            }
        }
        if (this.sourcePackageSetting != null) {
            pw.print("    packageSetting=");
            pw.println(this.sourcePackageSetting);
        }
        if (!"android.permission.READ_EXTERNAL_STORAGE".equals(this.name)) {
            return true;
        }
        pw.print("    enforced=");
        pw.println(readEnforced);
        return true;
    }
}
