package com.huawei.zxing.client.result;

import com.huawei.zxing.Result;

public final class SMTPResultParser extends ResultParser {
    public EmailAddressParsedResult parse(Result result) {
        String rawText = ResultParser.getMassagedText(result);
        if (!rawText.startsWith("smtp:") && !rawText.startsWith("SMTP:")) {
            return null;
        }
        String emailAddress = rawText.substring(5);
        String subject = null;
        String body = null;
        int colon = emailAddress.indexOf(58);
        if (colon >= 0) {
            subject = emailAddress.substring(colon + 1);
            emailAddress = emailAddress.substring(0, colon);
            colon = subject.indexOf(58);
            if (colon >= 0) {
                body = subject.substring(colon + 1);
                subject = subject.substring(0, colon);
            }
        }
        String mailtoURI = new StringBuilder();
        mailtoURI.append("mailto:");
        mailtoURI.append(emailAddress);
        return new EmailAddressParsedResult(emailAddress, subject, body, mailtoURI.toString());
    }
}