package org.apache.http.impl.cookie;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicSecureHandler extends AbstractCookieAttributeHandler {
    @Override // org.apache.http.cookie.CookieAttributeHandler
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie != null) {
            cookie.setSecure(true);
            return;
        }
        throw new IllegalArgumentException("Cookie may not be null");
    }

    @Override // org.apache.http.cookie.CookieAttributeHandler, org.apache.http.impl.cookie.AbstractCookieAttributeHandler
    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (origin != null) {
            return !cookie.isSecure() || origin.isSecure();
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }
}
