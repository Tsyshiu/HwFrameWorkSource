package com.android.server.security.trustspace;

import android.os.Environment;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class TrustSpaceSettings {
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_PROTECTION = "protection";
    private static final int DEDAULT_COLLECTION_SIZE = 10;
    private static final int FLAG_APPEND_HARMFUL_APP = 7;
    private static final int FLAG_REMOVE_ALL_HARMFUL_APP = 8;
    private static final int FLAG_REMOVE_HARMFUL_APP = 6;
    private static final int FLAG_REPLACE_ALL_HARMFUL_APP = 5;
    private static final String TAG = "TrustSpaceSettings";
    private static final String TAG_HARMFUL_PACKAGES = "harmful-packages";
    private static final String TAG_ITEM = "item";
    private static final String TAG_PROTECTED_PACKAGES = "protected-packages";
    private static final String TAG_TRUSTED_PACKAGES = "trusted-packages";
    public static final int TYPE_ACTIVITY = 0;
    public static final int TYPE_BROADCAST = 1;
    public static final int TYPE_PROVIDER = 3;
    public static final int TYPE_SERVICE = 2;
    private final File mBackupProtectedPackageFile;
    private final ArraySet<String> mHarmfulApps = new ArraySet<>(10);
    final ArrayMap<String, ProtectedPackage> mPackages = new ArrayMap<>();
    private final File mPreviousProtectedPackageFile;
    private final File mProtectedPackageFile;
    final ArraySet<String> mProtectionHighApps = new ArraySet<>();
    final ArraySet<String> mProtectionNormalApps = new ArraySet<>();
    final ArraySet<String> mTrustApps = new ArraySet<>();

    public static String componentTypeToString(int type) {
        if (type == 0) {
            return BigMemoryConstant.BIGMEMINFO_ITEM_TAG;
        }
        if (type == 1) {
            return "broadcast";
        }
        if (type == 2) {
            return AwareAppMngSort.ADJTYPE_SERVICE;
        }
        if (type != 3) {
            return "????";
        }
        return "provider";
    }

    private class ProtectedPackage {
        String packageName;
        int protectionLevel;

        public ProtectedPackage(String packageName2, int protection) {
            this.packageName = packageName2;
            this.protectionLevel = protection;
        }
    }

    TrustSpaceSettings() {
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mProtectedPackageFile = new File(systemDir, "trustspace.xml");
        this.mBackupProtectedPackageFile = new File(systemDir, "trustspace-backup.xml");
        this.mPreviousProtectedPackageFile = new File(systemDir, "trustspace.list");
    }

    /* access modifiers changed from: package-private */
    public void readPackages() {
        int type;
        if (!this.mProtectedPackageFile.exists()) {
            readPreviousFile();
            return;
        }
        FileInputStream str = null;
        if (this.mBackupProtectedPackageFile.exists()) {
            try {
                str = new FileInputStream(this.mBackupProtectedPackageFile);
                Slog.i(TAG, "Need to read from backup settings file");
                if (this.mProtectedPackageFile.exists()) {
                    Slog.w(TAG, "Cleaning up settings file");
                    if (!this.mProtectedPackageFile.delete()) {
                        Slog.w(TAG, "Failed to clean up settings file");
                    }
                }
            } catch (IOException e) {
            }
        }
        if (str == null) {
            try {
                if (!this.mProtectedPackageFile.exists()) {
                    Slog.i(TAG, "No settings file found");
                    IoUtils.closeQuietly(str);
                    return;
                }
                str = new FileInputStream(this.mProtectedPackageFile);
            } catch (XmlPullParserException e2) {
                Slog.e(TAG, "read settings error duing to XmlPullParserException");
            } catch (IOException e3) {
                Slog.e(TAG, "read settings error duing to IOException");
            } catch (Throwable th) {
                IoUtils.closeQuietly(str);
                throw th;
            }
        }
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(str, StandardCharsets.UTF_8.name());
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            Slog.i(TAG, "No start tag found in settings file");
            IoUtils.closeQuietly(str);
            return;
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (!(type2 == 3 || type2 == 4)) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_PROTECTED_PACKAGES)) {
                    readProtectedPackages(parser);
                } else if (tagName.equals(TAG_TRUSTED_PACKAGES)) {
                    readTrustedPackages(parser);
                } else if (tagName.equals(TAG_HARMFUL_PACKAGES)) {
                    readHarmfulPackages(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <packages>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        IoUtils.closeQuietly(str);
    }

    private void readPreviousFile() {
        if (readPreviousApps()) {
            if (!this.mPreviousProtectedPackageFile.delete()) {
                Slog.w(TAG, "Failed to clean up previous settings file");
            }
            writePackages();
            Slog.i(TAG, "Update from previous settings");
        }
    }

    private String readLine(InputStream in, StringBuffer sb) throws IOException {
        sb.setLength(0);
        while (true) {
            int ch = in.read();
            if (ch == -1) {
                if (sb.length() == 0) {
                    return null;
                }
                throw new IOException("Unexpected EOF");
            } else if (ch == 10) {
                return sb.toString();
            } else {
                sb.append((char) ch);
            }
        }
    }

    private boolean readPreviousApps() {
        try {
            BufferedInputStream in = new BufferedInputStream(new AtomicFile(this.mPreviousProtectedPackageFile).openRead());
            StringBuffer sb = new StringBuffer();
            while (true) {
                String line = readLine(in, sb);
                if (line != null) {
                    this.mPackages.put(line, new ProtectedPackage(line, 1));
                    this.mProtectionNormalApps.add(line);
                } else {
                    IoUtils.closeQuietly(in);
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            Slog.d(TAG, "Previous settings file not find");
        } catch (IOException e2) {
            Slog.w(TAG, "read previous settings error duing to IOException");
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
        IoUtils.closeQuietly((AutoCloseable) null);
        return false;
    }

    private void readProtectedPackages(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4 || !parser.getName().equals(TAG_ITEM))) {
                String packName = parser.getAttributeValue(null, ATTR_PACKAGE);
                int protection = readInt(parser, null, ATTR_PROTECTION, -1);
                if (!TextUtils.isEmpty(packName)) {
                    this.mPackages.put(packName, new ProtectedPackage(packName, protection));
                    int level = protection & 255;
                    if (level == 1) {
                        this.mProtectionNormalApps.add(packName);
                    } else if (level == 2) {
                        this.mProtectionHighApps.add(packName);
                    }
                }
            }
        }
    }

    private void readTrustedPackages(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4 || !parser.getName().equals(TAG_ITEM))) {
                String packName = parser.getAttributeValue(null, ATTR_PACKAGE);
                if (!TextUtils.isEmpty(packName)) {
                    this.mTrustApps.add(packName);
                }
            }
        }
    }

    private void readHarmfulPackages(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4 || !parser.getName().equals(TAG_ITEM))) {
                String packName = parser.getAttributeValue(null, ATTR_PACKAGE);
                if (!TextUtils.isEmpty(packName)) {
                    this.mHarmfulApps.add(packName);
                }
            }
        }
    }

    private int readInt(XmlPullParser parser, String ns, String name, int defValue) {
        String v = parser.getAttributeValue(ns, name);
        if (v == null) {
            return defValue;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void writePackages() {
        if (this.mProtectedPackageFile.exists()) {
            if (this.mBackupProtectedPackageFile.exists()) {
                if (this.mProtectedPackageFile.delete()) {
                    Slog.i(TAG, "Failed to clean up settings file");
                }
                Slog.w(TAG, "Preserving older settings backup file");
            } else if (!this.mProtectedPackageFile.renameTo(this.mBackupProtectedPackageFile)) {
                Slog.e(TAG, "Unable to backup settings,  current changes will be lost at reboot");
                return;
            }
        }
        try {
            FileOutputStream fstr = new FileOutputStream(this.mProtectedPackageFile);
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(str, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
            serializer.startTag(null, TAG_PROTECTED_PACKAGES);
            for (ProtectedPackage app : this.mPackages.values()) {
                serializer.startTag(null, TAG_ITEM);
                XmlUtils.writeStringAttribute(serializer, ATTR_PACKAGE, app.packageName);
                XmlUtils.writeIntAttribute(serializer, ATTR_PROTECTION, convertLevel(app.protectionLevel));
                serializer.endTag(null, TAG_ITEM);
            }
            serializer.endTag(null, TAG_PROTECTED_PACKAGES);
            serializer.startTag(null, TAG_TRUSTED_PACKAGES);
            Iterator<String> it = this.mTrustApps.iterator();
            while (it.hasNext()) {
                serializer.startTag(null, TAG_ITEM);
                XmlUtils.writeStringAttribute(serializer, ATTR_PACKAGE, it.next());
                serializer.endTag(null, TAG_ITEM);
            }
            serializer.endTag(null, TAG_TRUSTED_PACKAGES);
            serializer.startTag(null, TAG_HARMFUL_PACKAGES);
            Iterator<String> it2 = this.mHarmfulApps.iterator();
            while (it2.hasNext()) {
                serializer.startTag(null, TAG_ITEM);
                XmlUtils.writeStringAttribute(serializer, ATTR_PACKAGE, it2.next());
                serializer.endTag(null, TAG_ITEM);
            }
            serializer.endTag(null, TAG_HARMFUL_PACKAGES);
            serializer.endTag(null, HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
            serializer.endDocument();
            str.flush();
            FileUtils.sync(fstr);
            if (this.mBackupProtectedPackageFile.exists() && !this.mBackupProtectedPackageFile.delete()) {
                Slog.i(TAG, "Failed to clean up backup file");
            }
            IoUtils.closeQuietly(fstr);
            IoUtils.closeQuietly(str);
        } catch (IOException e) {
            Slog.e(TAG, "Unable to write settings, current changes will be lost at reboot", e);
            IoUtils.closeQuietly((AutoCloseable) null);
            IoUtils.closeQuietly((AutoCloseable) null);
            if (this.mProtectedPackageFile.exists() && !this.mProtectedPackageFile.delete()) {
                Slog.w(TAG, "Failed to clean up settings file");
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private void clearIntentProtectedApp(String packageName) {
        this.mPackages.remove(packageName);
        this.mProtectionNormalApps.remove(packageName);
        this.mProtectionHighApps.remove(packageName);
    }

    /* access modifiers changed from: package-private */
    public void addIntentProtectedApp(String packageName, int flags) {
        if (packageName != null) {
            int level = flags & 255;
            if (level == 1) {
                clearIntentProtectedApp(packageName);
                this.mProtectionNormalApps.add(packageName);
                this.mPackages.put(packageName, new ProtectedPackage(packageName, flags));
            } else if (level == 2) {
                clearIntentProtectedApp(packageName);
                this.mProtectionHighApps.add(packageName);
                this.mPackages.put(packageName, new ProtectedPackage(packageName, flags));
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void removeIntentProtectedApp(String packageName) {
        clearIntentProtectedApp(packageName);
    }

    /* access modifiers changed from: package-private */
    public List<String> getIntentProtectedApps(int flags) {
        if ((flags & 1) != 0) {
            return new ArrayList(this.mPackages.keySet());
        }
        ArraySet<String> apps = new ArraySet<>();
        if ((flags & 4) != 0) {
            apps.addAll(this.mProtectionHighApps);
        }
        if ((flags & 2) != 0) {
            apps.addAll(this.mProtectionNormalApps);
        }
        return new ArrayList(apps);
    }

    /* access modifiers changed from: package-private */
    public boolean isIntentProtectedApp(String packageName) {
        return this.mPackages.containsKey(packageName);
    }

    /* access modifiers changed from: package-private */
    public void removeIntentProtectedApps(List<String> packages, int flags) {
        if ((flags & 1) == 0) {
            if ((flags & 4) != 0) {
                if (packages == null) {
                    this.mPackages.removeAll(this.mProtectionHighApps);
                    this.mProtectionHighApps.clear();
                } else {
                    this.mPackages.removeAll(packages);
                    this.mProtectionHighApps.removeAll(packages);
                }
            }
            if ((flags & 2) == 0) {
                return;
            }
            if (packages == null) {
                this.mPackages.removeAll(this.mProtectionNormalApps);
                this.mProtectionNormalApps.clear();
                return;
            }
            this.mPackages.removeAll(packages);
            this.mProtectionNormalApps.removeAll(packages);
        } else if (packages == null) {
            this.mPackages.clear();
            this.mProtectionNormalApps.clear();
            this.mProtectionHighApps.clear();
        } else {
            this.mPackages.removeAll(packages);
            this.mProtectionNormalApps.removeAll(packages);
            this.mProtectionHighApps.removeAll(packages);
        }
    }

    /* access modifiers changed from: package-private */
    public void removeTrustApp(String packageName) {
        this.mTrustApps.remove(packageName);
    }

    /* access modifiers changed from: package-private */
    public void updateTrustApps(List<String> packages, int flag) {
        switch (flag) {
            case 1:
                if (packages != null) {
                    this.mTrustApps.addAll(packages);
                    return;
                }
                return;
            case 2:
                this.mTrustApps.clear();
                if (packages != null) {
                    this.mTrustApps.addAll(packages);
                    return;
                }
                return;
            case 3:
                if (packages != null) {
                    this.mTrustApps.removeAll(packages);
                    return;
                }
                return;
            case 4:
                this.mTrustApps.clear();
                return;
            case 5:
                this.mHarmfulApps.clear();
                if (packages != null) {
                    this.mHarmfulApps.addAll(packages);
                    return;
                }
                return;
            case 6:
                if (packages != null) {
                    this.mHarmfulApps.removeAll(packages);
                    return;
                }
                return;
            case 7:
                if (packages != null) {
                    this.mHarmfulApps.addAll(packages);
                    return;
                }
                return;
            case 8:
                this.mHarmfulApps.clear();
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isTrustApp(String packageName) {
        return this.mTrustApps.contains(packageName);
    }

    /* access modifiers changed from: package-private */
    public boolean isHarmfulApp(String packageName) {
        return this.mHarmfulApps.contains(packageName);
    }

    /* access modifiers changed from: package-private */
    public int getProtectionLevel(String packageName) {
        ProtectedPackage ts = this.mPackages.get(packageName);
        if (ts != null) {
            return convertLevel(ts.protectionLevel);
        }
        return 0;
    }

    private int convertLevel(int level) {
        if (level == 2) {
            return 1;
        }
        return level;
    }
}
