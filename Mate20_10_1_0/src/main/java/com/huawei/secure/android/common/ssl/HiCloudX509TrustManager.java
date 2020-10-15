package com.huawei.secure.android.common.ssl;

import com.huawei.secure.android.common.util.IOUtil;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class HiCloudX509TrustManager implements X509TrustManager {
    private static final String KEY_TYPE = "bks";
    private static final String TAG = "HiCloudX509TrustManager";
    private static final String TRUST_MANAGER_TYPE = "X509";
    protected List<X509TrustManager> m509TrustManager = new ArrayList();

    public HiCloudX509TrustManager(InputStream is, String trustPwd) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, IllegalArgumentException {
        if (is == null || trustPwd == null) {
            throw new IllegalArgumentException("inputstream or trustPwd is null");
        }
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TRUST_MANAGER_TYPE);
            KeyStore trustKeyStore = KeyStore.getInstance(KEY_TYPE);
            trustKeyStore.load(is, trustPwd.toCharArray());
            trustManagerFactory.init(trustKeyStore);
            TrustManager[] tms = trustManagerFactory.getTrustManagers();
            for (int i = 0; i < tms.length; i++) {
                if (tms[i] instanceof X509TrustManager) {
                    this.m509TrustManager.add((X509TrustManager) tms[i]);
                }
            }
            if (this.m509TrustManager.isEmpty()) {
                throw new CertificateException("X509TrustManager is empty");
            }
        } finally {
            IOUtil.closeSecure(is);
        }
    }

    @Override // javax.net.ssl.X509TrustManager
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (!this.m509TrustManager.isEmpty()) {
            this.m509TrustManager.get(0).checkClientTrusted(chain, authType);
            return;
        }
        throw new CertificateException("checkClientTrusted CertificateException");
    }

    @Override // javax.net.ssl.X509TrustManager
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (!this.m509TrustManager.isEmpty()) {
            this.m509TrustManager.get(0).checkServerTrusted(chain, authType);
            return;
        }
        throw new CertificateException("checkServerTrusted CertificateException");
    }

    public X509Certificate[] getAcceptedIssuers() {
        ArrayList<X509Certificate> list = new ArrayList<>();
        for (X509TrustManager tm : this.m509TrustManager) {
            list.addAll(Arrays.asList(tm.getAcceptedIssuers()));
        }
        return (X509Certificate[]) list.toArray(new X509Certificate[list.size()]);
    }
}
