package org.apache.http.impl.client;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.params.HttpProtocolParams;

@Deprecated
public class RequestWrapper extends AbstractHttpMessage implements HttpUriRequest {
    private int execCount;
    private String method;
    private final HttpRequest original;
    private URI uri;
    private ProtocolVersion version;

    public RequestWrapper(HttpRequest request) throws ProtocolException {
        if (request != null) {
            this.original = request;
            setParams(request.getParams());
            if (request instanceof HttpUriRequest) {
                this.uri = ((HttpUriRequest) request).getURI();
                this.method = ((HttpUriRequest) request).getMethod();
                this.version = null;
            } else {
                RequestLine requestLine = request.getRequestLine();
                try {
                    this.uri = new URI(requestLine.getUri());
                    this.method = requestLine.getMethod();
                    this.version = request.getProtocolVersion();
                } catch (URISyntaxException ex) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid request URI: ");
                    stringBuilder.append(requestLine.getUri());
                    throw new ProtocolException(stringBuilder.toString(), ex);
                }
            }
            this.execCount = 0;
            return;
        }
        throw new IllegalArgumentException("HTTP request may not be null");
    }

    public void resetHeaders() {
        this.headergroup.clear();
        setHeaders(this.original.getAllHeaders());
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        if (method != null) {
            this.method = method;
            return;
        }
        throw new IllegalArgumentException("Method name may not be null");
    }

    public ProtocolVersion getProtocolVersion() {
        if (this.version != null) {
            return this.version;
        }
        return HttpProtocolParams.getVersion(getParams());
    }

    public void setProtocolVersion(ProtocolVersion version) {
        this.version = version;
    }

    public URI getURI() {
        return this.uri;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    public RequestLine getRequestLine() {
        String method = getMethod();
        ProtocolVersion ver = getProtocolVersion();
        String uritext = null;
        if (this.uri != null) {
            uritext = this.uri.toASCIIString();
        }
        if (uritext == null || uritext.length() == 0) {
            uritext = "/";
        }
        return new BasicRequestLine(method, uritext, ver);
    }

    public void abort() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean isAborted() {
        return false;
    }

    public HttpRequest getOriginal() {
        return this.original;
    }

    public boolean isRepeatable() {
        return true;
    }

    public int getExecCount() {
        return this.execCount;
    }

    public void incrementExecCount() {
        this.execCount++;
    }
}
