package com.android.server.wifi.hotspot2;

import android.net.Network;
import android.util.Log;
import com.android.org.conscrypt.TrustManagerImpl;
import com.android.server.wifi.hotspot2.PasspointProvisioner.OsuServerCallbacks;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class OsuServerConnection {
    private static final int DNS_NAME = 2;
    private static final String TAG = "OsuServerConnection";
    private Network mNetwork;
    private OsuServerCallbacks mOsuServerCallbacks;
    private boolean mSetupComplete = false;
    private SSLSocketFactory mSocketFactory;
    private WFATrustManager mTrustManager;
    private URL mUrl;
    private HttpsURLConnection mUrlConnection = null;
    private boolean mVerboseLoggingEnabled = false;

    private class WFATrustManager implements X509TrustManager {
        private TrustManagerImpl mDelegate;
        private List<X509Certificate> mServerCerts;

        WFATrustManager(TrustManagerImpl trustManagerImpl) {
            this.mDelegate = trustManagerImpl;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                String str = OsuServerConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkClientTrusted ");
                stringBuilder.append(authType);
                Log.v(str, stringBuilder.toString());
            }
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                String str = OsuServerConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkServerTrusted ");
                stringBuilder.append(authType);
                Log.v(str, stringBuilder.toString());
            }
            boolean certsValid = false;
            try {
                this.mServerCerts = this.mDelegate.getTrustedChainForServer(chain, authType, (SSLSocket) null);
                certsValid = true;
            } catch (CertificateException e) {
                String str2 = OsuServerConnection.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to validate certs ");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
                if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                    e.printStackTrace();
                }
            }
            if (OsuServerConnection.this.mOsuServerCallbacks != null) {
                OsuServerConnection.this.mOsuServerCallbacks.onServerValidationStatus(OsuServerConnection.this.mOsuServerCallbacks.getSessionId(), certsValid);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuServerConnection.TAG, "getAcceptedIssuers ");
            }
            return null;
        }

        public X509Certificate getProviderCert() {
            if (this.mServerCerts == null || this.mServerCerts.size() <= 0) {
                return null;
            }
            X509Certificate providerCert = null;
            String fqdn = OsuServerConnection.this.mUrl.getHost();
            try {
                for (X509Certificate certificate : this.mServerCerts) {
                    Collection<List<?>> col = certificate.getSubjectAlternativeNames();
                    if (col != null) {
                        for (List<?> name : col) {
                            if (name != null) {
                                if (name.size() >= 2 && name.get(0).getClass() == Integer.class && name.get(1).toString().equals(fqdn)) {
                                    providerCert = certificate;
                                    if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                                        Log.v(OsuServerConnection.TAG, "OsuCert found");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (CertificateParsingException e) {
                String str = OsuServerConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to match certificate to ");
                stringBuilder.append(fqdn);
                Log.e(str, stringBuilder.toString());
                if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                    e.printStackTrace();
                }
            }
            return providerCert;
        }
    }

    public void setEventCallback(OsuServerCallbacks callbacks) {
        this.mOsuServerCallbacks = callbacks;
    }

    public void init(SSLContext tlsContext, TrustManagerImpl trustManagerImpl) {
        if (tlsContext != null) {
            try {
                this.mTrustManager = new WFATrustManager(trustManagerImpl);
                tlsContext.init(null, new TrustManager[]{this.mTrustManager}, null);
                this.mSocketFactory = tlsContext.getSocketFactory();
                this.mSetupComplete = true;
            } catch (KeyManagementException e) {
                Log.w(TAG, "Initialization failed");
                e.printStackTrace();
            }
        }
    }

    public boolean canValidateServer() {
        return this.mSetupComplete;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }

    public boolean connect(URL url, Network network) {
        this.mNetwork = network;
        this.mUrl = url;
        try {
            HttpsURLConnection urlConnection = (HttpsURLConnection) this.mNetwork.openConnection(this.mUrl);
            urlConnection.setSSLSocketFactory(this.mSocketFactory);
            urlConnection.connect();
            this.mUrlConnection = urlConnection;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to establish a URL connection");
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateProvider(String friendlyName) {
        if (this.mTrustManager.getProviderCert() != null) {
            return true;
        }
        Log.e(TAG, "Provider doesn't have valid certs");
        return false;
    }

    public void cleanup() {
        this.mUrlConnection.disconnect();
    }
}
