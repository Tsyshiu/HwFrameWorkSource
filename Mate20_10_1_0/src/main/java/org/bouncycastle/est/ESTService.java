package org.bouncycastle.est;

import com.huawei.security.hccm.common.connection.HttpConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.est.CsrAttrs;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cmc.CMCException;
import org.bouncycastle.cmc.SimplePKIResponse;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

public class ESTService {
    protected static final String CACERTS = "/cacerts";
    protected static final String CSRATTRS = "/csrattrs";
    protected static final String FULLCMC = "/fullcmc";
    protected static final String SERVERGEN = "/serverkeygen";
    protected static final String SIMPLE_ENROLL = "/simpleenroll";
    protected static final String SIMPLE_REENROLL = "/simplereenroll";
    protected static final Set<String> illegalParts = new HashSet();
    private static final Pattern pathInValid = Pattern.compile("^[0-9a-zA-Z_\\-.~!$&'()*+,;:=]+");
    private final ESTClientProvider clientProvider;
    private final String server;

    static {
        illegalParts.add(CACERTS.substring(1));
        illegalParts.add(SIMPLE_ENROLL.substring(1));
        illegalParts.add(SIMPLE_REENROLL.substring(1));
        illegalParts.add(FULLCMC.substring(1));
        illegalParts.add(SERVERGEN.substring(1));
        illegalParts.add(CSRATTRS.substring(1));
    }

    ESTService(String str, String str2, ESTClientProvider eSTClientProvider) {
        String str3;
        String verifyServer = verifyServer(str);
        if (str2 != null) {
            str3 = "https://" + verifyServer + "/.well-known/est/" + verifyLabel(str2);
        } else {
            str3 = "https://" + verifyServer + "/.well-known/est";
        }
        this.server = str3;
        this.clientProvider = eSTClientProvider;
    }

    /* access modifiers changed from: private */
    public String annotateRequest(byte[] bArr) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        int i = 0;
        do {
            int i2 = i + 48;
            if (i2 < bArr.length) {
                printWriter.print(Base64.toBase64String(bArr, i, 48));
                i = i2;
            } else {
                printWriter.print(Base64.toBase64String(bArr, i, bArr.length - i));
                i = bArr.length;
            }
            printWriter.print('\n');
        } while (i < bArr.length);
        printWriter.flush();
        return stringWriter.toString();
    }

    public static X509CertificateHolder[] storeToArray(Store<X509CertificateHolder> store) {
        return storeToArray(store, null);
    }

    public static X509CertificateHolder[] storeToArray(Store<X509CertificateHolder> store, Selector<X509CertificateHolder> selector) {
        Collection<X509CertificateHolder> matches = store.getMatches(selector);
        return (X509CertificateHolder[]) matches.toArray(new X509CertificateHolder[matches.size()]);
    }

    private String verifyLabel(String str) {
        while (str.endsWith("/") && str.length() > 0) {
            str = str.substring(0, str.length() - 1);
        }
        while (str.startsWith("/") && str.length() > 0) {
            str = str.substring(1);
        }
        if (str.length() == 0) {
            throw new IllegalArgumentException("Label set but after trimming '/' is not zero length string.");
        } else if (!pathInValid.matcher(str).matches()) {
            throw new IllegalArgumentException("Server path " + str + " contains invalid characters");
        } else if (!illegalParts.contains(str)) {
            return str;
        } else {
            throw new IllegalArgumentException("Label " + str + " is a reserved path segment.");
        }
    }

    private String verifyServer(String str) {
        while (str.endsWith("/") && str.length() > 0) {
            try {
                str = str.substring(0, str.length() - 1);
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    throw ((IllegalArgumentException) e);
                }
                throw new IllegalArgumentException("Scheme and host is invalid: " + e.getMessage(), e);
            }
        }
        if (!str.contains("://")) {
            URL url = new URL("https://" + str);
            if (url.getPath().length() != 0) {
                if (!url.getPath().equals("/")) {
                    throw new IllegalArgumentException("Server contains path, must only be <dnsname/ipaddress>:port, a path of '/.well-known/est/<label>' will be added arbitrarily.");
                }
            }
            return str;
        }
        throw new IllegalArgumentException("Server contains scheme, must only be <dnsname/ipaddress>:port, https:// will be added arbitrarily.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:49:0x0177 A[Catch:{ all -> 0x0184 }] */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x017a A[Catch:{ all -> 0x0184 }] */
    public CACertsResponse getCACerts() throws Exception {
        ESTResponse eSTResponse;
        Store<X509CRLHolder> store;
        Store<X509CertificateHolder> store2;
        Store<X509CRLHolder> store3;
        Store<X509CertificateHolder> store4;
        String str;
        try {
            URL url = new URL(this.server + CACERTS);
            ESTClient makeClient = this.clientProvider.makeClient();
            ESTRequest build = new ESTRequestBuilder(HttpConnection.HttpHeaders.GET, url).withClient(makeClient).build();
            eSTResponse = makeClient.doRequest(build);
            try {
                if (eSTResponse.getStatusCode() == 200) {
                    if (!"application/pkcs7-mime".equals(eSTResponse.getHeaders().getFirstValue("Content-Type"))) {
                        if (eSTResponse.getHeaders().getFirstValue("Content-Type") != null) {
                            str = " got " + eSTResponse.getHeaders().getFirstValue("Content-Type");
                        } else {
                            str = " but was not present.";
                        }
                        throw new ESTException("Response : " + url.toString() + "Expecting application/pkcs7-mime " + str, null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                    }
                    try {
                        if (eSTResponse.getContentLength() == null || eSTResponse.getContentLength().longValue() <= 0) {
                            store4 = null;
                            store3 = null;
                        } else {
                            SimplePKIResponse simplePKIResponse = new SimplePKIResponse(ContentInfo.getInstance((ASN1Sequence) new ASN1InputStream(eSTResponse.getInputStream()).readObject()));
                            store4 = simplePKIResponse.getCertificates();
                            store3 = simplePKIResponse.getCRLs();
                        }
                        store2 = store4;
                        store = store3;
                    } catch (Throwable th) {
                        throw new ESTException("Decoding CACerts: " + url.toString() + " " + th.getMessage(), th, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                    }
                } else if (eSTResponse.getStatusCode() == 204) {
                    store2 = null;
                    store = null;
                } else {
                    throw new ESTException("Get CACerts: " + url.toString(), null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                }
                CACertsResponse cACertsResponse = new CACertsResponse(store2, store, build, eSTResponse.getSource(), this.clientProvider.isTrusted());
                try {
                    eSTResponse.close();
                    e = null;
                } catch (Exception e) {
                    e = e;
                }
                if (e == null) {
                    return cACertsResponse;
                }
                if (e instanceof ESTException) {
                    throw e;
                }
                throw new ESTException("Get CACerts: " + url.toString(), e, eSTResponse.getStatusCode(), null);
            } catch (Throwable th2) {
                th = th2;
                try {
                    if (!(th instanceof ESTException)) {
                    }
                } catch (Throwable th3) {
                    if (eSTResponse != null) {
                        try {
                            eSTResponse.close();
                        } catch (Exception e2) {
                        }
                    }
                    throw th3;
                }
            }
        } catch (Throwable th4) {
            th = th4;
            eSTResponse = null;
            if (!(th instanceof ESTException)) {
                throw th;
            }
            throw new ESTException(th.getMessage(), th);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x00aa  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0102 A[Catch:{ all -> 0x010f }] */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0105 A[Catch:{ all -> 0x010f }] */
    public CSRRequestResponse getCSRAttributes() throws ESTException {
        ESTResponse eSTResponse;
        ESTException th;
        CSRAttributesResponse cSRAttributesResponse;
        if (this.clientProvider.isTrusted()) {
            try {
                URL url = new URL(this.server + CSRATTRS);
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTRequest build = new ESTRequestBuilder(HttpConnection.HttpHeaders.GET, url).withClient(makeClient).build();
                eSTResponse = makeClient.doRequest(build);
                try {
                    int statusCode = eSTResponse.getStatusCode();
                    if (statusCode == 200) {
                        try {
                            if (eSTResponse.getContentLength() != null && eSTResponse.getContentLength().longValue() > 0) {
                                cSRAttributesResponse = new CSRAttributesResponse(CsrAttrs.getInstance(ASN1Sequence.getInstance(new ASN1InputStream(eSTResponse.getInputStream()).readObject())));
                                eSTResponse.close();
                                e = null;
                                if (e != null) {
                                    return new CSRRequestResponse(cSRAttributesResponse, eSTResponse.getSource());
                                }
                                if (e instanceof ESTException) {
                                    throw ((ESTException) e);
                                }
                                throw new ESTException(e.getMessage(), e, eSTResponse.getStatusCode(), null);
                            }
                        } catch (Throwable th2) {
                            throw new ESTException("Decoding CACerts: " + url.toString() + " " + th2.getMessage(), th2, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                        }
                    } else if (statusCode != 204) {
                        if (statusCode != 404) {
                            throw new ESTException("CSR Attribute request: " + build.getURL().toString(), null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                        }
                    }
                    cSRAttributesResponse = null;
                    try {
                        eSTResponse.close();
                        e = null;
                    } catch (Exception e) {
                        e = e;
                    }
                    if (e != null) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    try {
                        if (!(th instanceof ESTException)) {
                            throw th;
                        }
                        throw new ESTException(th.getMessage(), th);
                    } catch (Throwable th4) {
                        if (eSTResponse != null) {
                            try {
                                eSTResponse.close();
                            } catch (Exception e2) {
                            }
                        }
                        throw th4;
                    }
                }
            } catch (Throwable th5) {
                eSTResponse = null;
                th = th5;
                if (!(th instanceof ESTException)) {
                }
            }
        } else {
            throw new IllegalStateException("No trust anchors.");
        }
    }

    /* access modifiers changed from: protected */
    public EnrollmentResponse handleEnrollResponse(ESTResponse eSTResponse) throws IOException {
        long j;
        ESTRequest originalRequest = eSTResponse.getOriginalRequest();
        if (eSTResponse.getStatusCode() == 202) {
            String header = eSTResponse.getHeader("Retry-After");
            if (header != null) {
                try {
                    j = System.currentTimeMillis() + (Long.parseLong(header) * 1000);
                } catch (NumberFormatException e) {
                    try {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                        j = simpleDateFormat.parse(header).getTime();
                    } catch (Exception e2) {
                        throw new ESTException("Unable to parse Retry-After header:" + originalRequest.getURL().toString() + " " + e2.getMessage(), null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                    }
                }
                return new EnrollmentResponse(null, j, originalRequest, eSTResponse.getSource());
            }
            throw new ESTException("Got Status 202 but not Retry-After header from: " + originalRequest.getURL().toString());
        } else if (eSTResponse.getStatusCode() == 200) {
            try {
                return new EnrollmentResponse(new SimplePKIResponse(ContentInfo.getInstance(new ASN1InputStream(eSTResponse.getInputStream()).readObject())).getCertificates(), -1, null, eSTResponse.getSource());
            } catch (CMCException e3) {
                throw new ESTException(e3.getMessage(), e3.getCause());
            }
        } else {
            throw new ESTException("Simple Enroll: " + originalRequest.getURL().toString(), null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
        }
    }

    public EnrollmentResponse simpleEnroll(EnrollmentResponse enrollmentResponse) throws Exception {
        if (this.clientProvider.isTrusted()) {
            ESTResponse eSTResponse = null;
            try {
                ESTClient makeClient = this.clientProvider.makeClient();
                eSTResponse = makeClient.doRequest(new ESTRequestBuilder(enrollmentResponse.getRequestToRetry()).withClient(makeClient).build());
                EnrollmentResponse handleEnrollResponse = handleEnrollResponse(eSTResponse);
                if (eSTResponse != null) {
                    eSTResponse.close();
                }
                return handleEnrollResponse;
            } catch (Throwable th) {
                if (eSTResponse != null) {
                    eSTResponse.close();
                }
                throw th;
            }
        } else {
            throw new IllegalStateException("No trust anchors.");
        }
    }

    public EnrollmentResponse simpleEnroll(boolean z, PKCS10CertificationRequest pKCS10CertificationRequest, ESTAuth eSTAuth) throws IOException {
        if (this.clientProvider.isTrusted()) {
            ESTResponse eSTResponse = null;
            try {
                byte[] bytes = annotateRequest(pKCS10CertificationRequest.getEncoded()).getBytes();
                StringBuilder sb = new StringBuilder();
                sb.append(this.server);
                sb.append(z ? SIMPLE_REENROLL : SIMPLE_ENROLL);
                URL url = new URL(sb.toString());
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTRequestBuilder withClient = new ESTRequestBuilder(HttpConnection.HttpHeaders.POST, url).withData(bytes).withClient(makeClient);
                withClient.addHeader("Content-Type", "application/pkcs10");
                withClient.addHeader("Content-Length", "" + bytes.length);
                withClient.addHeader("Content-Transfer-Encoding", "base64");
                if (eSTAuth != null) {
                    eSTAuth.applyAuth(withClient);
                }
                ESTResponse doRequest = makeClient.doRequest(withClient.build());
                EnrollmentResponse handleEnrollResponse = handleEnrollResponse(doRequest);
                if (doRequest != null) {
                    doRequest.close();
                }
                return handleEnrollResponse;
            } catch (Throwable th) {
                if (0 != 0) {
                    eSTResponse.close();
                }
                throw th;
            }
        } else {
            throw new IllegalStateException("No trust anchors.");
        }
    }

    public EnrollmentResponse simpleEnrollPoP(boolean z, final PKCS10CertificationRequestBuilder pKCS10CertificationRequestBuilder, final ContentSigner contentSigner, ESTAuth eSTAuth) throws IOException {
        if (this.clientProvider.isTrusted()) {
            ESTResponse eSTResponse = null;
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(this.server);
                sb.append(z ? SIMPLE_REENROLL : SIMPLE_ENROLL);
                URL url = new URL(sb.toString());
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTRequestBuilder withConnectionListener = new ESTRequestBuilder(HttpConnection.HttpHeaders.POST, url).withClient(makeClient).withConnectionListener(new ESTSourceConnectionListener() {
                    /* class org.bouncycastle.est.ESTService.AnonymousClass1 */

                    @Override // org.bouncycastle.est.ESTSourceConnectionListener
                    public ESTRequest onConnection(Source source, ESTRequest eSTRequest) throws IOException {
                        if (source instanceof TLSUniqueProvider) {
                            TLSUniqueProvider tLSUniqueProvider = (TLSUniqueProvider) source;
                            if (tLSUniqueProvider.isTLSUniqueAvailable()) {
                                PKCS10CertificationRequestBuilder pKCS10CertificationRequestBuilder = new PKCS10CertificationRequestBuilder(pKCS10CertificationRequestBuilder);
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                pKCS10CertificationRequestBuilder.setAttribute(PKCSObjectIdentifiers.pkcs_9_at_challengePassword, new DERPrintableString(Base64.toBase64String(tLSUniqueProvider.getTLSUnique())));
                                byteArrayOutputStream.write(ESTService.this.annotateRequest(pKCS10CertificationRequestBuilder.build(contentSigner).getEncoded()).getBytes());
                                byteArrayOutputStream.flush();
                                ESTRequestBuilder withData = new ESTRequestBuilder(eSTRequest).withData(byteArrayOutputStream.toByteArray());
                                withData.setHeader("Content-Type", "application/pkcs10");
                                withData.setHeader("Content-Transfer-Encoding", "base64");
                                withData.setHeader("Content-Length", Long.toString((long) byteArrayOutputStream.size()));
                                return withData.build();
                            }
                        }
                        throw new IOException("Source does not supply TLS unique.");
                    }
                });
                if (eSTAuth != null) {
                    eSTAuth.applyAuth(withConnectionListener);
                }
                ESTResponse doRequest = makeClient.doRequest(withConnectionListener.build());
                EnrollmentResponse handleEnrollResponse = handleEnrollResponse(doRequest);
                if (doRequest != null) {
                    doRequest.close();
                }
                return handleEnrollResponse;
            } catch (Throwable th) {
                if (0 != 0) {
                    eSTResponse.close();
                }
                throw th;
            }
        } else {
            throw new IllegalStateException("No trust anchors.");
        }
    }
}
