package org.apache.http.params;

@Deprecated
public final class DefaultedHttpParams extends AbstractHttpParams {
    private final HttpParams defaults;
    private final HttpParams local;

    public DefaultedHttpParams(HttpParams local2, HttpParams defaults2) {
        if (local2 != null) {
            this.local = local2;
            this.defaults = defaults2;
            return;
        }
        throw new IllegalArgumentException("HTTP parameters may not be null");
    }

    public HttpParams copy() {
        return new DefaultedHttpParams(this.local.copy(), this.defaults);
    }

    public Object getParameter(String name) {
        HttpParams httpParams;
        Object obj = this.local.getParameter(name);
        if (obj != null || (httpParams = this.defaults) == null) {
            return obj;
        }
        return httpParams.getParameter(name);
    }

    public boolean removeParameter(String name) {
        return this.local.removeParameter(name);
    }

    public HttpParams setParameter(String name, Object value) {
        return this.local.setParameter(name, value);
    }

    public HttpParams getDefaults() {
        return this.defaults;
    }
}
