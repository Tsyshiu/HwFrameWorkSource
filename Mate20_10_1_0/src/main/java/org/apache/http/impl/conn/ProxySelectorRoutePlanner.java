package org.apache.http.impl.conn;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class ProxySelectorRoutePlanner implements HttpRoutePlanner {
    protected ProxySelector proxySelector;
    protected SchemeRegistry schemeRegistry;

    public ProxySelectorRoutePlanner(SchemeRegistry schreg, ProxySelector prosel) {
        if (schreg != null) {
            this.schemeRegistry = schreg;
            this.proxySelector = prosel;
            return;
        }
        throw new IllegalArgumentException("SchemeRegistry must not be null.");
    }

    public ProxySelector getProxySelector() {
        return this.proxySelector;
    }

    public void setProxySelector(ProxySelector prosel) {
        this.proxySelector = prosel;
    }

    @Override // org.apache.http.conn.routing.HttpRoutePlanner
    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        if (request != null) {
            HttpRoute route = ConnRouteParams.getForcedRoute(request.getParams());
            if (route != null) {
                return route;
            }
            if (target != null) {
                InetAddress local = ConnRouteParams.getLocalAddress(request.getParams());
                HttpHost proxy = (HttpHost) request.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
                if (proxy == null) {
                    proxy = determineProxy(target, request, context);
                } else if (ConnRouteParams.NO_HOST.equals(proxy)) {
                    proxy = null;
                }
                boolean secure = this.schemeRegistry.getScheme(target.getSchemeName()).isLayered();
                if (proxy == null) {
                    return new HttpRoute(target, local, secure);
                }
                return new HttpRoute(target, local, proxy, secure);
            }
            throw new IllegalStateException("Target host must not be null.");
        }
        throw new IllegalStateException("Request must not be null.");
    }

    /* access modifiers changed from: protected */
    public HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        ProxySelector psel = this.proxySelector;
        if (psel == null) {
            psel = ProxySelector.getDefault();
        }
        if (psel == null) {
            return null;
        }
        try {
            Proxy p = chooseProxy(psel.select(new URI(target.toURI())), target, request, context);
            if (p.type() != Proxy.Type.HTTP) {
                return null;
            }
            if (p.address() instanceof InetSocketAddress) {
                InetSocketAddress isa = (InetSocketAddress) p.address();
                return new HttpHost(getHost(isa), isa.getPort());
            }
            throw new HttpException("Unable to handle non-Inet proxy address: " + p.address());
        } catch (URISyntaxException usx) {
            throw new HttpException("Cannot convert host to URI: " + target, usx);
        }
    }

    /* access modifiers changed from: protected */
    public String getHost(InetSocketAddress isa) {
        return isa.isUnresolved() ? isa.getHostName() : isa.getAddress().getHostAddress();
    }

    /* access modifiers changed from: protected */
    public Proxy chooseProxy(List<Proxy> proxies, HttpHost target, HttpRequest request, HttpContext context) {
        if (proxies == null || proxies.isEmpty()) {
            throw new IllegalArgumentException("Proxy list must not be empty.");
        }
        Proxy result = null;
        int i = 0;
        while (result == null && i < proxies.size()) {
            Proxy p = proxies.get(i);
            int i2 = AnonymousClass1.$SwitchMap$java$net$Proxy$Type[p.type().ordinal()];
            if (i2 == 1 || i2 == 2) {
                result = p;
            }
            i++;
        }
        if (result == null) {
            return Proxy.NO_PROXY;
        }
        return result;
    }

    /* renamed from: org.apache.http.impl.conn.ProxySelectorRoutePlanner$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$java$net$Proxy$Type = new int[Proxy.Type.values().length];

        static {
            try {
                $SwitchMap$java$net$Proxy$Type[Proxy.Type.DIRECT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$java$net$Proxy$Type[Proxy.Type.HTTP.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$java$net$Proxy$Type[Proxy.Type.SOCKS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }
}
