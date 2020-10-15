package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;

public class ValidPeriodProcessor extends BaseProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty() || !line.startsWith(HwCertification.KEY_VALID_PERIOD)) {
            return false;
        }
        String key = line.substring(HwCertification.KEY_VALID_PERIOD.length() + 1);
        if (key == null || key.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "VP_RC empty");
            return false;
        }
        rawCert.mPeriodString = key;
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean parserCert(HwCertification rawCert) {
        String period = rawCert.mCertificationData.mPeriodString;
        if (period == null) {
            HwAuthLogger.e("HwCertificationManager", "VP_PC error time is null ");
            return false;
        }
        Matcher m = Pattern.compile("from " + "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})" + " " + HwCertification.KEY_DATE_TO + " " + "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").matcher(period);
        if (m.matches()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                rawCert.setFromDate(dateFormat.parse(m.group(1)));
                rawCert.setToDate(dateFormat.parse(m.group(2)));
                return true;
            } catch (ParseException e) {
                HwAuthLogger.e("HwCertificationManager", "VP_PC time parser exception");
                if (HwAuthLogger.getHWFLOW()) {
                    HwAuthLogger.i("HwCertificationManager", "period:" + period + "m.groupCount():" + m.groupCount());
                    for (int i = 1; i <= m.groupCount(); i++) {
                        HwAuthLogger.i("HwCertificationManager", "m.group" + i + AwarenessInnerConstants.COLON_KEY + m.group(i));
                    }
                }
                return false;
            }
        } else {
            if (HwAuthLogger.getHWFLOW()) {
                for (int i2 = 1; i2 <= m.groupCount(); i2++) {
                    HwAuthLogger.i("HwCertificationManager", "m.group" + i2 + AwarenessInnerConstants.COLON_KEY + m.group(i2));
                }
            }
            HwAuthLogger.e("HwCertificationManager", "VP_PC error");
            return false;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        if ("2".equals(cert.getVersion())) {
            return true;
        }
        Date fromDate = cert.getFromDate();
        Date toDate = cert.getToDate();
        if (fromDate == null || toDate == null) {
            return false;
        }
        if (!toDate.after(fromDate)) {
            HwAuthLogger.e("HwCertificationManager", "VP_VC date error from time is : " + fromDate + ", toTime is:" + toDate);
            return false;
        } else if (!isCurrentDataExpired(toDate)) {
            return true;
        } else {
            HwAuthLogger.e("HwCertificationManager", "VP_VC date expired " + cert.mCertificationData.mPeriodString);
            return false;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_VALID_PERIOD.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mPeriodString = parser.getAttributeValue(null, "value");
        return true;
    }

    private boolean isCurrentDataExpired(Date toDate) {
        return System.currentTimeMillis() > toDate.getTime();
    }
}
