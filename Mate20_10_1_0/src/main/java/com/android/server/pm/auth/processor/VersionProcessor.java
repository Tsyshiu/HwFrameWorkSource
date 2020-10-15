package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.util.HwAuthLogger;
import org.xmlpull.v1.XmlPullParser;

public class VersionProcessor extends BaseProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "VPR_RC line is empty");
            return false;
        } else if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "VPR_RC cert is null");
            return false;
        } else if (line.startsWith("Version:")) {
            rawCert.mVersion = line.substring(HwCertification.KEY_VERSION.length() + 1);
            return true;
        } else {
            HwAuthLogger.e("HwCertificationManager", "VPR_RC error");
            return false;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parserCert(HwCertification rawCert) {
        if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "VPR_PC error cert is null");
            return false;
        }
        HwCertification.CertificationData certData = rawCert.mCertificationData;
        String version = certData.mVersion;
        if (version == null || version.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "VPR_PC error is empty");
            return false;
        }
        rawCert.setVersion(certData.mVersion);
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        if (pkg == null || cert == null) {
            HwAuthLogger.e("HwCertificationManager", "VPR_VC error package or cert is null");
            return false;
        }
        String version = cert.getVersion();
        if (version != null && !version.isEmpty()) {
            return true;
        }
        HwAuthLogger.e("HwCertificationManager", "VPR_VC error is empty");
        return false;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (HwCertification.KEY_VERSION.equals(tag)) {
            cert.mCertificationData.mVersion = parser.getAttributeValue(null, "value");
            return true;
        }
        HwAuthLogger.e("HwCertificationManager", "VPR_PX error");
        return false;
    }
}
