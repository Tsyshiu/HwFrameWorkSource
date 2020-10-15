package jcifs.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jcifs.Config;
import jcifs.dcerpc.msrpc.samr;
import jcifs.https.Handler;
import jcifs.ntlmssp.NtlmMessage;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;

public class NtlmHttpURLConnection extends HttpURLConnection {
    private static final String DEFAULT_DOMAIN;
    private static final int LM_COMPATIBILITY = Config.getInt("jcifs.smb.lmCompatibility", 0);
    private static final int MAX_REDIRECTS = Integer.parseInt(System.getProperty("http.maxRedirects", "20"));
    private String authMethod;
    private String authProperty;
    private ByteArrayOutputStream cachedOutput;
    private HttpURLConnection connection;
    private boolean handshakeComplete;
    private Map headerFields;
    private Map requestProperties = new HashMap();

    static {
        String domain = System.getProperty("http.auth.ntlm.domain");
        if (domain == null) {
            domain = Type3Message.getDefaultDomain();
        }
        DEFAULT_DOMAIN = domain;
    }

    public NtlmHttpURLConnection(HttpURLConnection connection2) {
        super(connection2.getURL());
        this.connection = connection2;
    }

    @Override // java.net.URLConnection
    public void connect() throws IOException {
        if (!this.connected) {
            this.connection.connect();
            this.connected = true;
        }
    }

    private void handshake() throws IOException {
        if (!this.handshakeComplete) {
            doHandshake();
            this.handshakeComplete = true;
        }
    }

    public URL getURL() {
        return this.connection.getURL();
    }

    public int getContentLength() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getContentLength();
    }

    public String getContentType() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getContentType();
    }

    public String getContentEncoding() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getContentEncoding();
    }

    public long getExpiration() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getExpiration();
    }

    public long getDate() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getDate();
    }

    public long getLastModified() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getLastModified();
    }

    @Override // java.net.URLConnection
    public String getHeaderField(String header) {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getHeaderField(header);
    }

    private Map getHeaderFields0() {
        if (this.headerFields != null) {
            return this.headerFields;
        }
        Map map = new HashMap();
        String key = this.connection.getHeaderFieldKey(0);
        String value = this.connection.getHeaderField(0);
        int i = 1;
        while (true) {
            if (key == null && value == null) {
                break;
            }
            List values = (List) map.get(key);
            if (values == null) {
                values = new ArrayList();
                map.put(key, values);
            }
            values.add(value);
            key = this.connection.getHeaderFieldKey(i);
            value = this.connection.getHeaderField(i);
            i++;
        }
        for (Map.Entry entry : map.entrySet()) {
            entry.setValue(Collections.unmodifiableList((List) entry.getValue()));
        }
        Map unmodifiableMap = Collections.unmodifiableMap(map);
        this.headerFields = unmodifiableMap;
        return unmodifiableMap;
    }

    @Override // java.net.URLConnection
    public Map getHeaderFields() {
        if (this.headerFields != null) {
            return this.headerFields;
        }
        try {
            handshake();
        } catch (IOException e) {
        }
        return getHeaderFields0();
    }

    public int getHeaderFieldInt(String header, int def) {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getHeaderFieldInt(header, def);
    }

    public long getHeaderFieldDate(String header, long def) {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getHeaderFieldDate(header, def);
    }

    public String getHeaderFieldKey(int index) {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getHeaderFieldKey(index);
    }

    @Override // java.net.HttpURLConnection, java.net.URLConnection
    public String getHeaderField(int index) {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getHeaderField(index);
    }

    @Override // java.net.URLConnection
    public Object getContent() throws IOException {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getContent();
    }

    @Override // java.net.URLConnection
    public Object getContent(Class[] classes) throws IOException {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getContent(classes);
    }

    @Override // java.net.HttpURLConnection, java.net.URLConnection
    public Permission getPermission() throws IOException {
        return this.connection.getPermission();
    }

    @Override // java.net.URLConnection
    public InputStream getInputStream() throws IOException {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getInputStream();
    }

    @Override // java.net.URLConnection
    public OutputStream getOutputStream() throws IOException {
        try {
            connect();
        } catch (IOException e) {
        }
        OutputStream output = this.connection.getOutputStream();
        this.cachedOutput = new ByteArrayOutputStream();
        return new CacheStream(output, this.cachedOutput);
    }

    public String toString() {
        return this.connection.toString();
    }

    public void setDoInput(boolean doInput) {
        this.connection.setDoInput(doInput);
        this.doInput = doInput;
    }

    public boolean getDoInput() {
        return this.connection.getDoInput();
    }

    public void setDoOutput(boolean doOutput) {
        this.connection.setDoOutput(doOutput);
        this.doOutput = doOutput;
    }

    public boolean getDoOutput() {
        return this.connection.getDoOutput();
    }

    public void setAllowUserInteraction(boolean allowUserInteraction) {
        this.connection.setAllowUserInteraction(allowUserInteraction);
        this.allowUserInteraction = allowUserInteraction;
    }

    public boolean getAllowUserInteraction() {
        return this.connection.getAllowUserInteraction();
    }

    public void setUseCaches(boolean useCaches) {
        this.connection.setUseCaches(useCaches);
        this.useCaches = useCaches;
    }

    public boolean getUseCaches() {
        return this.connection.getUseCaches();
    }

    public void setIfModifiedSince(long ifModifiedSince) {
        this.connection.setIfModifiedSince(ifModifiedSince);
        this.ifModifiedSince = ifModifiedSince;
    }

    public long getIfModifiedSince() {
        return this.connection.getIfModifiedSince();
    }

    public boolean getDefaultUseCaches() {
        return this.connection.getDefaultUseCaches();
    }

    public void setDefaultUseCaches(boolean defaultUseCaches) {
        this.connection.setDefaultUseCaches(defaultUseCaches);
    }

    public void setRequestProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException();
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(value);
        boolean found = false;
        Iterator entries = this.requestProperties.entrySet().iterator();
        while (true) {
            if (!entries.hasNext()) {
                break;
            }
            Map.Entry entry = (Map.Entry) entries.next();
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                entry.setValue(arrayList);
                found = true;
                break;
            }
        }
        if (!found) {
            this.requestProperties.put(key, arrayList);
        }
        this.connection.setRequestProperty(key, value);
    }

    public void addRequestProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException();
        }
        List values = null;
        Iterator entries = this.requestProperties.entrySet().iterator();
        while (true) {
            if (!entries.hasNext()) {
                break;
            }
            Map.Entry entry = (Map.Entry) entries.next();
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                values = (List) entry.getValue();
                values.add(value);
                break;
            }
        }
        if (values == null) {
            values = new ArrayList();
            values.add(value);
            this.requestProperties.put(key, values);
        }
        StringBuffer buffer = new StringBuffer();
        Iterator propertyValues = values.iterator();
        while (propertyValues.hasNext()) {
            buffer.append(propertyValues.next());
            if (propertyValues.hasNext()) {
                buffer.append(", ");
            }
        }
        this.connection.setRequestProperty(key, buffer.toString());
    }

    public String getRequestProperty(String key) {
        return this.connection.getRequestProperty(key);
    }

    @Override // java.net.URLConnection
    public Map getRequestProperties() {
        Map map = new HashMap();
        for (Map.Entry entry : this.requestProperties.entrySet()) {
            map.put(entry.getKey(), Collections.unmodifiableList((List) entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    public void setInstanceFollowRedirects(boolean instanceFollowRedirects) {
        this.connection.setInstanceFollowRedirects(instanceFollowRedirects);
    }

    public boolean getInstanceFollowRedirects() {
        return this.connection.getInstanceFollowRedirects();
    }

    @Override // java.net.HttpURLConnection
    public void setRequestMethod(String requestMethod) throws ProtocolException {
        this.connection.setRequestMethod(requestMethod);
        this.method = requestMethod;
    }

    public String getRequestMethod() {
        return this.connection.getRequestMethod();
    }

    @Override // java.net.HttpURLConnection
    public int getResponseCode() throws IOException {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getResponseCode();
    }

    @Override // java.net.HttpURLConnection
    public String getResponseMessage() throws IOException {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getResponseMessage();
    }

    public void disconnect() {
        this.connection.disconnect();
        this.handshakeComplete = false;
        this.connected = false;
    }

    public boolean usingProxy() {
        return this.connection.usingProxy();
    }

    public InputStream getErrorStream() {
        try {
            handshake();
        } catch (IOException e) {
        }
        return this.connection.getErrorStream();
    }

    private int parseResponseCode() throws IOException {
        try {
            String response = this.connection.getHeaderField(0);
            int index = response.indexOf(32);
            while (response.charAt(index) == ' ') {
                index++;
            }
            return Integer.parseInt(response.substring(index, index + 3));
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private void doHandshake() throws IOException {
        Type1Message type1;
        Type3Message type3;
        connect();
        try {
            int response = parseResponseCode();
            if ((response == 401 || response == 407) && (type1 = (Type1Message) attemptNegotiation(response)) != null) {
                int attempt = 0;
                while (attempt < MAX_REDIRECTS) {
                    this.connection.setRequestProperty(this.authProperty, this.authMethod + ' ' + Base64.encode(type1.toByteArray()));
                    this.connection.connect();
                    int response2 = parseResponseCode();
                    if ((response2 == 401 || response2 == 407) && (type3 = (Type3Message) attemptNegotiation(response2)) != null) {
                        this.connection.setRequestProperty(this.authProperty, this.authMethod + ' ' + Base64.encode(type3.toByteArray()));
                        this.connection.connect();
                        if (this.cachedOutput != null && this.doOutput) {
                            OutputStream output = this.connection.getOutputStream();
                            this.cachedOutput.writeTo(output);
                            output.flush();
                        }
                        int response3 = parseResponseCode();
                        if (response3 == 401 || response3 == 407) {
                            attempt++;
                            if (!this.allowUserInteraction || attempt >= MAX_REDIRECTS) {
                                break;
                            }
                            reconnect();
                        }
                    }
                }
                throw new IOException("Unable to negotiate NTLM authentication.");
            }
        } finally {
            this.cachedOutput = null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0045 A[ORIG_RETURN, RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0047  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0050  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x0030  */
    private NtlmMessage attemptNegotiation(int response) throws IOException {
        String authHeader;
        List methods;
        String password;
        String password2;
        this.authProperty = null;
        this.authMethod = null;
        InputStream errorStream = this.connection.getErrorStream();
        if (errorStream == null || errorStream.available() == 0) {
            if (response != 401) {
                authHeader = "WWW-Authenticate";
                this.authProperty = "Authorization";
            } else {
                authHeader = "Proxy-Authenticate";
                this.authProperty = "Proxy-Authorization";
            }
            String authorization = null;
            methods = (List) getHeaderFields0().get(authHeader);
            if (methods != null) {
                return null;
            }
            Iterator iterator = methods.iterator();
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                String currentAuthMethod = (String) iterator.next();
                if (currentAuthMethod.startsWith("NTLM")) {
                    if (currentAuthMethod.length() == 4) {
                        this.authMethod = "NTLM";
                        break;
                    } else if (currentAuthMethod.indexOf(32) == 4) {
                        this.authMethod = "NTLM";
                        authorization = currentAuthMethod.substring(5).trim();
                        break;
                    }
                } else if (!currentAuthMethod.startsWith("Negotiate")) {
                    continue;
                } else if (currentAuthMethod.length() == 9) {
                    this.authMethod = "Negotiate";
                    break;
                } else if (currentAuthMethod.indexOf(32) == 9) {
                    this.authMethod = "Negotiate";
                    authorization = currentAuthMethod.substring(10).trim();
                    break;
                }
            }
            if (this.authMethod == null) {
                return null;
            }
            NtlmMessage message = authorization != null ? new Type2Message(Base64.decode(authorization)) : null;
            reconnect();
            if (message == null) {
                NtlmMessage message2 = new Type1Message();
                if (LM_COMPATIBILITY <= 2) {
                    return message2;
                }
                message2.setFlag(4, true);
                return message2;
            }
            String domain = DEFAULT_DOMAIN;
            String user = Type3Message.getDefaultUser();
            String password3 = Type3Message.getDefaultPassword();
            String userInfo = this.url.getUserInfo();
            if (userInfo != null) {
                String userInfo2 = URLDecoder.decode(userInfo);
                int index = userInfo2.indexOf(58);
                if (index != -1) {
                    user = userInfo2.substring(0, index);
                } else {
                    user = userInfo2;
                }
                if (index != -1) {
                    password3 = userInfo2.substring(index + 1);
                }
                int index2 = user.indexOf(92);
                if (index2 == -1) {
                    index2 = user.indexOf(47);
                }
                if (index2 != -1) {
                    domain = user.substring(0, index2);
                }
                if (index2 != -1) {
                    user = user.substring(index2 + 1);
                }
                password = password3;
            } else {
                password = password3;
            }
            if (user != null) {
                password2 = password;
            } else if (!this.allowUserInteraction) {
                return null;
            } else {
                try {
                    URL url = getURL();
                    String protocol = url.getProtocol();
                    int port = url.getPort();
                    if (port == -1) {
                        port = "https".equalsIgnoreCase(protocol) ? Handler.DEFAULT_HTTPS_PORT : 80;
                    }
                    PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(null, port, protocol, "", this.authMethod);
                    if (auth == null) {
                        return null;
                    }
                    user = auth.getUserName();
                    password2 = new String(auth.getPassword());
                } catch (Exception e) {
                    password2 = password;
                }
            }
            return new Type3Message((Type2Message) message, password2, domain, user, Type3Message.getDefaultWorkstation(), 0);
        }
        do {
        } while (errorStream.read(new byte[samr.ACB_AUTOLOCK], 0, samr.ACB_AUTOLOCK) != -1);
        if (response != 401) {
        }
        String authorization2 = null;
        methods = (List) getHeaderFields0().get(authHeader);
        if (methods != null) {
        }
    }

    private void reconnect() throws IOException {
        this.connection = (HttpURLConnection) this.connection.getURL().openConnection();
        this.connection.setRequestMethod(this.method);
        this.headerFields = null;
        for (Map.Entry property : this.requestProperties.entrySet()) {
            String key = (String) property.getKey();
            StringBuffer value = new StringBuffer();
            Iterator values = ((List) property.getValue()).iterator();
            while (values.hasNext()) {
                value.append(values.next());
                if (values.hasNext()) {
                    value.append(", ");
                }
            }
            this.connection.setRequestProperty(key, value.toString());
        }
        this.connection.setAllowUserInteraction(this.allowUserInteraction);
        this.connection.setDoInput(this.doInput);
        this.connection.setDoOutput(this.doOutput);
        this.connection.setIfModifiedSince(this.ifModifiedSince);
        this.connection.setUseCaches(this.useCaches);
    }

    private static class CacheStream extends OutputStream {
        private final OutputStream collector;
        private final OutputStream stream;

        public CacheStream(OutputStream stream2, OutputStream collector2) {
            this.stream = stream2;
            this.collector = collector2;
        }

        @Override // java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            this.stream.close();
            this.collector.close();
        }

        @Override // java.io.OutputStream, java.io.Flushable
        public void flush() throws IOException {
            this.stream.flush();
            this.collector.flush();
        }

        @Override // java.io.OutputStream
        public void write(byte[] b) throws IOException {
            this.stream.write(b);
            this.collector.write(b);
        }

        @Override // java.io.OutputStream
        public void write(byte[] b, int off, int len) throws IOException {
            this.stream.write(b, off, len);
            this.collector.write(b, off, len);
        }

        @Override // java.io.OutputStream
        public void write(int b) throws IOException {
            this.stream.write(b);
            this.collector.write(b);
        }
    }
}
