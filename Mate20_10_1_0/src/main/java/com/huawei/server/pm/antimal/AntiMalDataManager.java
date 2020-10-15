package com.huawei.server.pm.antimal;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.security.deviceusage.HwOEMInfoAdapter;
import com.huawei.server.security.securitydiagnose.AntiMalApkInfo;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AntiMalDataManager {
    private static final String ANTIMAL_DATA_FILE = "AntiMalData.xml";
    private static final String APPS = "apps";
    private static final String BOOT_TIMES = "BootTimes";
    private static final String COMPONENT = "component";
    private static final String COUNTER = "counter";
    private static final int EXCEPTION_NUM = -1;
    private static final String FASTBOOT_STATUS = "ro.boot.flash.locked";
    private static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int LIST_SIZE = 10;
    private static final int LONG_BASE_SIZE = 8;
    private static final int MAX_BOOT_TIMES = 10;
    private static final int OEMINFO_ENABLE_RETREAD = 163;
    private static final int OEMINFO_ENABLE_RETREAD_SIZE = 40;
    private static final String PATH_SLANT = File.separator;
    private static final String STATUS = "status";
    private static final int STRING_BUFFER_SIZE = 16;
    private static final int STRING_BUILDER_SIZE = 16;
    private static final String TAG = "HW-AntiMalDataManager";
    private static final String TAG_ITEM = "item";
    private Status mCurAntiMalStatus = new Status();
    private ArrayList<AntiMalApkInfo> mCurApkInfoList = new ArrayList<>(10);
    private ArrayList<AntiMalComponentInfo> mCurComponentList = new ArrayList<>(10);
    private AntiMalCounter mCurCounter = new AntiMalCounter();
    private long mDeviceFirstUseTime;
    private boolean mIsAntiMalDataExist;
    private boolean mIsOtaBoot;
    private Status mOldAntiMalStatus;
    private ArrayList<AntiMalApkInfo> mOldApkInfoList;
    private ArrayList<AntiMalComponentInfo> mOldComponentList = new ArrayList<>(10);
    private AntiMalCounter mOldCounter;

    public AntiMalDataManager(boolean isOtaBoot) {
        this.mIsOtaBoot = isOtaBoot;
        readOldAntiMalData();
        getDeviceFirstUseTime();
        getCurrentStatus();
    }

    public void addAntiMalApkInfo(AntiMalApkInfo apkInfo) {
        if (apkInfo != null) {
            this.mCurApkInfoList.add(apkInfo);
        }
    }

    public void addComponentInfo(AntiMalComponentInfo componentInfo) {
        if (componentInfo != null) {
            this.mCurComponentList.add(componentInfo);
        }
    }

    public Bundle getAntimalComponentInfo() {
        Bundle bundle = new Bundle();
        if (this.mCurComponentList.size() > 0) {
            bundle.putParcelableArrayList(HwSecDiagnoseConstant.COMPONENT_LIST, this.mCurComponentList);
        } else {
            bundle.putParcelableArrayList(HwSecDiagnoseConstant.COMPONENT_LIST, this.mOldComponentList);
        }
        return bundle;
    }

    public ArrayList<AntiMalApkInfo> getOldApkInfoList() {
        return this.mOldApkInfoList;
    }

    public boolean isNeedReport() {
        return isDeviceStatusNormal() && isDataValid();
    }

    public boolean isNeedScanIllegalApks() {
        return this.mIsOtaBoot || isDeviceStatusNormal() || !this.mIsAntiMalDataExist || !isOldAntiMalResultNormal();
    }

    public Bundle collectData() {
        getCurCounter();
        Bundle antimalData = new Bundle();
        antimalData.putString(HwSecDiagnoseConstant.ANTIMAL_TIME, getCurrentTime());
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_ROOT_STATE, this.mCurAntiMalStatus.mRootStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_FASTBOOT_STATE, this.mCurAntiMalStatus.mFastbootStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_SYSTEM_STATE, this.mCurAntiMalStatus.mVerfybootStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_SELINUX_STATE, this.mCurAntiMalStatus.mSeLinuxStatus);
        antimalData.putString(HwSecDiagnoseConstant.ANTIMAL_USED_TIME, this.mCurAntiMalStatus.mDeviceFirstUseTimeStr);
        antimalData.putString("SecVer", this.mCurAntiMalStatus.mSecPatchVer);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_MAL_COUNT, this.mCurCounter.mAddCnt);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_DELETE_COUNT, this.mCurCounter.mDeleteCnt);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_TAMPER_COUNT, this.mCurCounter.mModifiedCnt);
        antimalData.putString("SecVer", null);
        if (this.mCurApkInfoList.size() > 0) {
            antimalData.putParcelableArrayList(HwSecDiagnoseConstant.ANTIMAL_APK_LIST, this.mCurApkInfoList);
        }
        return antimalData;
    }

    public void writeAntiMalData() {
        FileOutputStream fos = null;
        fos = null;
        fos = null;
        try {
            fos = new FileOutputStream(Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", ANTIMAL_DATA_FILE}), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, "antimal");
            writeStatus(out);
            writeCounter(out);
            writeApkInfoList(out);
            writeComponentInfoList(out);
            out.endTag(null, "antimal");
            out.endDocument();
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "writeAntiMalData IOException");
        } catch (Exception e2) {
            Log.e(TAG, "writeAntiMalData Other exception");
        } catch (Throwable th) {
            IoUtils.closeQuietly(fos);
            throw th;
        }
        IoUtils.closeQuietly(fos);
    }

    /* access modifiers changed from: package-private */
    public AntiMalComponentInfo getComponentByApkPath(String apkPath) {
        if (TextUtils.isEmpty(apkPath)) {
            return null;
        }
        String subApkPath = formatPath(apkPath);
        Iterator<AntiMalComponentInfo> it = this.mCurComponentList.iterator();
        while (it.hasNext()) {
            AntiMalComponentInfo componentInfo = it.next();
            if (subApkPath.startsWith(componentInfo.componentName)) {
                return componentInfo;
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public boolean isCurAntiMalResultNormal() {
        Iterator<AntiMalComponentInfo> it = this.mCurComponentList.iterator();
        while (it.hasNext()) {
            AntiMalComponentInfo tmpComp = it.next();
            if (!tmpComp.isNormal()) {
                if (!IS_HW_DEBUG) {
                    return false;
                }
                Log.d(TAG, "isCurAntiMalResultNormal tmpComp info : " + tmpComp);
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public boolean isApksAbnormal() {
        getCurCounter();
        return this.mCurCounter.hasAbnormalApks();
    }

    private String formatPath(String path) {
        if (path == null || !path.startsWith(PATH_SLANT)) {
            return path;
        }
        return path.substring(1, path.length());
    }

    private boolean isDataValid() {
        getCurCounter();
        return this.mCurCounter.hasAbnormalApks() && !antiMalDataEquals();
    }

    private boolean isOldAntiMalResultNormal() {
        Iterator<AntiMalComponentInfo> it = this.mOldComponentList.iterator();
        while (it.hasNext()) {
            AntiMalComponentInfo tmpComp = it.next();
            if (!tmpComp.isNormal()) {
                if (!IS_HW_DEBUG) {
                    return false;
                }
                Log.d(TAG, "isOldAntiMalResultNormal tmpComp info : " + tmpComp);
                return false;
            }
        }
        return true;
    }

    private boolean isDeviceStatusNormal() {
        Log.i(TAG, "isDeviceStatusNormal mDeviceFirstUseTime = " + formatTime(this.mDeviceFirstUseTime) + " mBootCnt = " + this.mCurCounter.mBootCnt);
        return this.mDeviceFirstUseTime == 0 && this.mCurCounter.mBootCnt <= 10;
    }

    private boolean apkInfoListEquals() {
        ArrayList<AntiMalApkInfo> arrayList = this.mOldApkInfoList;
        if (arrayList == null) {
            if (IS_HW_DEBUG) {
                Log.d(TAG, "apkInfoListEquals mOldApkInfoList is NULL!");
            }
            return this.mCurApkInfoList.size() == 0;
        } else if (arrayList.size() != this.mCurApkInfoList.size()) {
            if (IS_HW_DEBUG) {
                Log.d(TAG, "apkInfoListEquals size not equal!");
            }
            return false;
        } else if (this.mCurApkInfoList.size() == 0) {
            if (IS_HW_DEBUG) {
                Log.d(TAG, "apkInfoListEquals size is 0");
            }
            return true;
        } else {
            AntiMalApkInfo[] sampleArrays = new AntiMalApkInfo[this.mCurApkInfoList.size()];
            AntiMalApkInfo[] curApkArrays = (AntiMalApkInfo[]) this.mCurApkInfoList.toArray(sampleArrays);
            AntiMalApkInfo[] oldApkArrays = (AntiMalApkInfo[]) this.mOldApkInfoList.toArray(sampleArrays);
            Arrays.sort(curApkArrays);
            Arrays.sort(oldApkArrays);
            return Arrays.equals(oldApkArrays, curApkArrays);
        }
    }

    private boolean antiMalDataEquals() {
        boolean isApkCntEqual = this.mCurCounter.equals(this.mOldCounter);
        boolean isListEqual = apkInfoListEquals();
        if (IS_HW_DEBUG) {
            Log.d(TAG, " isApkCntEqual = " + isApkCntEqual + " isListEqual = " + isListEqual);
        }
        return isApkCntEqual && isListEqual;
    }

    private int stringToInt(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Catched NumberFormatException" + str);
            return -1;
        }
    }

    private void getCurCounter() {
        int addCnt = 0;
        int modifyCnt = 0;
        int deleteCnt = 0;
        if (this.mCurApkInfoList.size() != 0) {
            synchronized (this.mCurApkInfoList) {
                Iterator<AntiMalApkInfo> it = this.mCurApkInfoList.iterator();
                while (it.hasNext()) {
                    AntiMalApkInfo ai = it.next();
                    if (ai != null) {
                        int type = ai.getType();
                        if (type == 1) {
                            addCnt++;
                        } else if (type == 2) {
                            modifyCnt++;
                        } else if (type == 3) {
                            deleteCnt++;
                        }
                    }
                }
            }
            AntiMalCounter antiMalCounter = this.mCurCounter;
            antiMalCounter.mAddCnt = addCnt;
            antiMalCounter.mDeleteCnt = deleteCnt;
            antiMalCounter.mModifiedCnt = modifyCnt;
            if (IS_HW_DEBUG) {
                Log.d(TAG, "getCurCounter = " + this.mCurCounter);
            }
        }
    }

    private void getCurrentStatus() {
        int i = 0;
        int maskSysStatus = SystemProperties.getInt("persist.sys.root.status", 0);
        this.mCurAntiMalStatus.mRootStatus = maskSysStatus > 0 ? 1 : 0;
        this.mCurAntiMalStatus.mVerfybootStatus = (maskSysStatus & 128) > 0 ? 1 : 0;
        int seLinuxMask = maskSysStatus & 8;
        Status status = this.mCurAntiMalStatus;
        if (seLinuxMask > 0) {
            i = 1;
        }
        status.mSeLinuxStatus = i;
        this.mCurAntiMalStatus.mFastbootStatus = getCurFastbootStatus();
        this.mCurAntiMalStatus.mDeviceFirstUseTimeStr = formatTime(this.mDeviceFirstUseTime);
        this.mCurAntiMalStatus.mSecPatchVer = getSecurePatchVersion();
        if (IS_HW_DEBUG) {
            Log.d(TAG, "getCurrentStatus AntiMalStatus = " + this.mCurAntiMalStatus);
        }
    }

    private int getCurFastbootStatus() {
        int status = SystemProperties.getInt(FASTBOOT_STATUS, 1);
        if (IS_HW_DEBUG) {
            Log.d(TAG, "getCurFastbootStatus fastboot status = " + status);
        }
        return status;
    }

    private String getSecurePatchVersion() {
        String patch = Build.VERSION.SECURITY_PATCH;
        if (IS_HW_DEBUG) {
            Log.d(TAG, "getSecurePatchVersion patch = " + patch);
        }
        if (!TextUtils.isEmpty(patch)) {
            return formatData(patch);
        }
        return null;
    }

    private void getDeviceFirstUseTime() {
        byte[] renewBytes = HwOEMInfoAdapter.getByteArrayFromOeminfo(OEMINFO_ENABLE_RETREAD, 40);
        if (renewBytes == null || renewBytes.length < 40) {
            Log.d(TAG, "getDeviceFirstUseTime OEMINFO error!");
            return;
        }
        ByteBuffer dataBuffer = ByteBuffer.allocate(40);
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.clear();
        dataBuffer.put(renewBytes);
        this.mDeviceFirstUseTime = dataBuffer.getLong(32);
        Log.d(TAG, "mDeviceFirstUseTime = " + formatTime(this.mDeviceFirstUseTime));
    }

    private void readOldAntiMalData() {
        int type;
        long start = System.currentTimeMillis();
        File antimalFile = Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", ANTIMAL_DATA_FILE});
        if (verifyAntimalFile(antimalFile)) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(antimalFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(input, StandardCharsets.UTF_8.name());
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    Log.e(TAG, "readAntiMalData NO start tag!");
                    IoUtils.closeQuietly(input);
                    IoUtils.closeQuietly(input);
                    return;
                }
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    } else if (!(type2 == 3 || type2 == 4)) {
                        readOldDataByTag(parser, parser.getName());
                    }
                }
                IoUtils.closeQuietly(input);
                if (IS_HW_DEBUG) {
                    long end = System.currentTimeMillis();
                    Log.d(TAG, "readOldAntiMalData time = " + (end - start));
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "readAntiMalData FileNotFoundException");
            } catch (XmlPullParserException e2) {
                Log.e(TAG, "readAntiMalData XmlPullParserException: " + e2.getMessage());
            } catch (IOException e3) {
                Log.e(TAG, "readAntiMalData Other exception :" + e3.getMessage());
            } catch (Throwable th) {
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        }
    }

    private boolean verifyAntimalFile(File antimalFile) {
        if (antimalFile == null || !antimalFile.exists()) {
            Log.e(TAG, "readOldAntiMalData AntiMalData.xml File not exist!");
            this.mIsAntiMalDataExist = false;
            setBootCnt();
            return false;
        }
        this.mIsAntiMalDataExist = true;
        return true;
    }

    private void readOldDataByTag(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        if ("status".equals(tagName)) {
            readOldStatus(parser);
        } else if (COUNTER.equals(tagName)) {
            readOldCounter(parser);
        } else if (APPS.equals(tagName)) {
            readOldAntiMalApks(parser);
        } else if (COMPONENT.equals(tagName)) {
            readOldComponentInfo(parser);
        }
    }

    private void readOldStatus(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4)) {
                if (TAG_ITEM.equals(parser.getName())) {
                    this.mOldAntiMalStatus = new Status();
                    String rootStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_ROOT_STATE);
                    this.mOldAntiMalStatus.mRootStatus = stringToInt(rootStatusStr);
                    String fastbootStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_FASTBOOT_STATE);
                    this.mOldAntiMalStatus.mFastbootStatus = stringToInt(fastbootStatusStr);
                    String systemStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_SYSTEM_STATE);
                    this.mOldAntiMalStatus.mVerfybootStatus = stringToInt(systemStatusStr);
                    String seLinuxStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_SELINUX_STATE);
                    this.mOldAntiMalStatus.mSeLinuxStatus = stringToInt(seLinuxStatusStr);
                    this.mOldAntiMalStatus.mDeviceFirstUseTimeStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_USED_TIME);
                    this.mOldAntiMalStatus.mSecPatchVer = parser.getAttributeValue(null, "SecVer");
                    if (IS_HW_DEBUG) {
                        Log.d(TAG, "readStatus = " + this.mOldAntiMalStatus);
                    }
                }
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    private void readOldCounter(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4 || !TAG_ITEM.equals(parser.getName()))) {
                this.mOldCounter = new AntiMalCounter();
                String malCntStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_MAL_COUNT);
                this.mOldCounter.mAddCnt = stringToInt(malCntStr);
                String deleteCntStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_DELETE_COUNT);
                this.mOldCounter.mDeleteCnt = stringToInt(deleteCntStr);
                String modifyCntStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_TAMPER_COUNT);
                this.mOldCounter.mModifiedCnt = stringToInt(modifyCntStr);
                String bootCntStr = parser.getAttributeValue(null, BOOT_TIMES);
                this.mOldCounter.mBootCnt = stringToInt(bootCntStr);
                setBootCnt();
                if (IS_HW_DEBUG) {
                    Log.d(TAG, "readCounter = " + this.mOldCounter);
                }
            }
        }
    }

    private void setBootCnt() {
        AntiMalCounter antiMalCounter = this.mCurCounter;
        AntiMalCounter antiMalCounter2 = this.mOldCounter;
        int i = 1;
        if (antiMalCounter2 != null) {
            i = 1 + antiMalCounter2.mBootCnt;
            antiMalCounter2.mBootCnt = i;
        }
        antiMalCounter.mBootCnt = i;
    }

    private void readOldAntiMalApks(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        this.mOldApkInfoList = new ArrayList<>(10);
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4 || !TAG_ITEM.equals(parser.getName()))) {
                String typeStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_TYPE);
                String packageName = parser.getAttributeValue(null, "PackageName");
                String apkName = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_NAME);
                String apkPath = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_PATH);
                AntiMalApkInfo api = new AntiMalApkInfo.Builder().setPackageName(packageName).setPath(apkPath).setApkName(apkName).setType(stringToInt(typeStr)).setLastModifyTime(parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_LAST_MODIFY)).setFrom(null).setVersion(stringToInt(parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_VERSION))).build();
                this.mOldApkInfoList.add(api);
                if (IS_HW_DEBUG) {
                    Log.d(TAG, "readAntiMalApks : AntiMalApkInfo : " + api);
                }
            }
        }
    }

    private void readOldComponentInfo(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4 || !TAG_ITEM.equals(parser.getName()))) {
                String name = parser.getAttributeValue(null, "name");
                String verifyStatus = parser.getAttributeValue(null, AntiMalComponentInfo.VERIFY_STATUS);
                String antimalTypeMask = parser.getAttributeValue(null, AntiMalComponentInfo.ANTIMAL_TYPE_MASK);
                if (!TextUtils.isEmpty(name)) {
                    AntiMalComponentInfo acpi = new AntiMalComponentInfo(name, stringToInt(verifyStatus), stringToInt(antimalTypeMask));
                    this.mOldComponentList.add(acpi);
                    if (IS_HW_DEBUG) {
                        Log.d(TAG, "readOldComponentInfo AntiMalComponentInfo : " + acpi);
                    }
                }
            }
        }
    }

    private void writeStatus(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, "status");
        out.startTag(null, TAG_ITEM);
        StringBuffer stringBuffer = new StringBuffer(String.valueOf(this.mCurAntiMalStatus.mRootStatus).length());
        stringBuffer.append(this.mCurAntiMalStatus.mRootStatus);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_ROOT_STATE, stringBuffer.toString());
        StringBuffer stringBuffer2 = new StringBuffer(String.valueOf(this.mCurAntiMalStatus.mFastbootStatus).length());
        stringBuffer2.append(this.mCurAntiMalStatus.mFastbootStatus);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_FASTBOOT_STATE, stringBuffer2.toString());
        StringBuffer stringBuffer3 = new StringBuffer(String.valueOf(this.mCurAntiMalStatus.mVerfybootStatus).length());
        stringBuffer3.append(this.mCurAntiMalStatus.mVerfybootStatus);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_SYSTEM_STATE, stringBuffer3.toString());
        StringBuffer stringBuffer4 = new StringBuffer(String.valueOf(this.mCurAntiMalStatus.mSeLinuxStatus).length());
        stringBuffer4.append(this.mCurAntiMalStatus.mSeLinuxStatus);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_SELINUX_STATE, stringBuffer4.toString());
        if (!TextUtils.isEmpty(this.mCurAntiMalStatus.mSecPatchVer)) {
            out.attribute(null, "SecVer", this.mCurAntiMalStatus.mSecPatchVer);
        }
        if (!TextUtils.isEmpty(this.mCurAntiMalStatus.mDeviceFirstUseTimeStr)) {
            out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_USED_TIME, this.mCurAntiMalStatus.mDeviceFirstUseTimeStr);
        }
        out.endTag(null, TAG_ITEM);
        out.endTag(null, "status");
    }

    private void writeCounter(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, COUNTER);
        out.startTag(null, TAG_ITEM);
        StringBuffer stringBuffer = new StringBuffer(String.valueOf(this.mCurCounter.mBootCnt).length());
        stringBuffer.append(this.mCurCounter.mBootCnt);
        out.attribute(null, BOOT_TIMES, stringBuffer.toString());
        StringBuffer stringBuffer2 = new StringBuffer(String.valueOf(this.mCurCounter.mAddCnt).length());
        stringBuffer2.append(this.mCurCounter.mAddCnt);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_MAL_COUNT, stringBuffer2.toString());
        StringBuffer stringBuffer3 = new StringBuffer(String.valueOf(this.mCurCounter.mDeleteCnt).length());
        stringBuffer3.append(this.mCurCounter.mDeleteCnt);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_DELETE_COUNT, stringBuffer3.toString());
        StringBuffer stringBuffer4 = new StringBuffer(String.valueOf(this.mCurCounter.mModifiedCnt).length());
        stringBuffer4.append(this.mCurCounter.mModifiedCnt);
        out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_TAMPER_COUNT, stringBuffer4.toString());
        out.endTag(null, TAG_ITEM);
        out.endTag(null, COUNTER);
    }

    private void writeApkInfoList(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, APPS);
        Iterator<AntiMalApkInfo> it = this.mCurApkInfoList.iterator();
        while (it.hasNext()) {
            AntiMalApkInfo antiMalApkInfo = it.next();
            if (IS_HW_DEBUG) {
                Log.d(TAG, "writeAntiMalData AntiMalApkInfo : " + antiMalApkInfo);
            }
            if (antiMalApkInfo != null) {
                out.startTag(null, TAG_ITEM);
                StringBuffer stringBuffer = new StringBuffer(16);
                stringBuffer.append(antiMalApkInfo.getType());
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, stringBuffer.toString());
                out.attribute(null, "PackageName", antiMalApkInfo.getPackageName());
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_NAME, antiMalApkInfo.getApkName());
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_PATH, antiMalApkInfo.getPath());
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_LAST_MODIFY, antiMalApkInfo.getLastModifyTime());
                StringBuffer stringBuffer2 = new StringBuffer(16);
                stringBuffer2.append(antiMalApkInfo.getVersion());
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_VERSION, stringBuffer2.toString());
                out.endTag(null, TAG_ITEM);
            }
        }
        out.endTag(null, APPS);
    }

    private void writeComponentInfoList(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, COMPONENT);
        Iterator<AntiMalComponentInfo> it = this.mCurComponentList.iterator();
        while (it.hasNext()) {
            AntiMalComponentInfo componentInfo = it.next();
            if (IS_HW_DEBUG) {
                Log.d(TAG, "writeComponentInfoList AntiMalComponentInfo : " + componentInfo);
            }
            if (componentInfo != null) {
                out.startTag(null, TAG_ITEM);
                out.attribute(null, "name", componentInfo.componentName);
                StringBuffer stringBuffer = new StringBuffer(16);
                stringBuffer.append(componentInfo.mVerifyStatus);
                out.attribute(null, AntiMalComponentInfo.VERIFY_STATUS, stringBuffer.toString());
                StringBuffer stringBuffer2 = new StringBuffer(16);
                stringBuffer2.append(componentInfo.mAntimalTypeMask);
                out.attribute(null, AntiMalComponentInfo.ANTIMAL_TYPE_MASK, stringBuffer2.toString());
                out.endTag(null, TAG_ITEM);
            }
        }
        out.endTag(null, COMPONENT);
    }

    private String formatTime(long minSecond) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(minSecond));
    }

    private String formatData(String date) {
        if (TextUtils.isEmpty(date)) {
            return "";
        }
        try {
            return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy"), new SimpleDateFormat("yyyy-MM-dd").parse(date)).toString();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "formatData IllegalArgumentException!");
            return "";
        } catch (ParseException e2) {
            Log.e(TAG, "formatData ParseException!");
            return "";
        } catch (RuntimeException e3) {
            Log.e(TAG, "formatData other exception!");
            return "";
        }
    }

    private String getCurrentTime() {
        return formatTime(System.currentTimeMillis());
    }

    private static class Status {
        String mDeviceFirstUseTimeStr;
        int mFastbootStatus;
        int mRootStatus;
        int mSeLinuxStatus;
        String mSecPatchVer;
        int mVerfybootStatus;

        private Status() {
        }

        public boolean equals(Object in) {
            if (!(in instanceof Status)) {
                return false;
            }
            Status other = (Status) in;
            if (this.mRootStatus == other.mRootStatus && this.mFastbootStatus == other.mFastbootStatus && this.mVerfybootStatus == other.mVerfybootStatus && this.mSeLinuxStatus == other.mSeLinuxStatus) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder result = new StringBuilder(16);
            result.append("Root Status : ");
            result.append(this.mRootStatus);
            result.append(" Fastboot Status : ");
            result.append(this.mFastbootStatus);
            result.append(" Verifyboot Status : ");
            result.append(this.mVerfybootStatus);
            result.append(" SeLinux Status : ");
            result.append(this.mSeLinuxStatus);
            result.append(" FirstUseTime : ");
            result.append(this.mDeviceFirstUseTimeStr);
            result.append(" SecPatch Version : ");
            result.append(this.mSecPatchVer);
            return result.toString();
        }

        public int hashCode() {
            return this.mRootStatus + this.mFastbootStatus + this.mVerfybootStatus + this.mSeLinuxStatus + this.mDeviceFirstUseTimeStr.hashCode() + this.mSecPatchVer.hashCode();
        }
    }

    private static class AntiMalCounter {
        int mAddCnt;
        int mBootCnt;
        int mDeleteCnt;
        int mModifiedCnt;

        AntiMalCounter() {
        }

        AntiMalCounter(int deleteCnt, int addCnt, int modifyCnt, int bootCnt) {
            this.mDeleteCnt = deleteCnt;
            this.mModifiedCnt = modifyCnt;
            this.mAddCnt = addCnt;
            this.mBootCnt = bootCnt;
        }

        public boolean equals(Object in) {
            if (in == null || !(in instanceof AntiMalCounter)) {
                return false;
            }
            AntiMalCounter other = (AntiMalCounter) in;
            if (this.mAddCnt == other.mAddCnt && this.mDeleteCnt == other.mDeleteCnt && this.mModifiedCnt == other.mModifiedCnt) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder result = new StringBuilder(16);
            result.append("Delete : ");
            result.append(this.mDeleteCnt);
            result.append(" Modify : ");
            result.append(this.mModifiedCnt);
            result.append(" Add : ");
            result.append(this.mAddCnt);
            result.append(" Boot time : ");
            result.append(this.mBootCnt);
            return result.toString();
        }

        public int hashCode() {
            return this.mDeleteCnt + this.mModifiedCnt + this.mAddCnt + this.mBootCnt;
        }

        /* access modifiers changed from: package-private */
        public boolean hasAbnormalApks() {
            return (this.mDeleteCnt + this.mModifiedCnt) + this.mAddCnt > 0;
        }
    }
}
