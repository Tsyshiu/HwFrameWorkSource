package sun.security.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import sun.net.www.ParseUtil;

public class PropertyExpander {

    public static class ExpandException extends GeneralSecurityException {
        private static final long serialVersionUID = -7941948581406161702L;

        public ExpandException(String msg) {
            super(msg);
        }
    }

    public static String expand(String value) throws ExpandException {
        return expand(value, false);
    }

    public static String expand(String value, boolean encodeURL) throws ExpandException {
        if (value == null) {
            return null;
        }
        int i = 0;
        int p = value.indexOf("${", 0);
        if (p == -1) {
            return value;
        }
        StringBuffer sb = new StringBuffer(value.length());
        int max = value.length();
        while (p < max) {
            if (p > i) {
                sb.append(value.substring(i, p));
                i = p;
            }
            int pe = p + 2;
            if (pe >= max || value.charAt(pe) != '{') {
                while (pe < max && value.charAt(pe) != '}') {
                    pe++;
                }
                if (pe == max) {
                    sb.append(value.substring(p, pe));
                    break;
                }
                String prop = value.substring(p + 2, pe);
                if (prop.equals("/")) {
                    sb.append(File.separatorChar);
                } else {
                    String val = System.getProperty(prop);
                    if (val != null) {
                        if (encodeURL) {
                            try {
                                if (sb.length() > 0 || !new URI(val).isAbsolute()) {
                                    val = ParseUtil.encodePath(val);
                                }
                            } catch (URISyntaxException e) {
                                val = ParseUtil.encodePath(val);
                            }
                        }
                        sb.append(val);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to expand property ");
                        stringBuilder.append(prop);
                        throw new ExpandException(stringBuilder.toString());
                    }
                }
            }
            pe = value.indexOf("}}", pe);
            if (pe == -1 || pe + 2 == max) {
                sb.append(value.substring(p));
                break;
            }
            pe++;
            sb.append(value.substring(p, pe + 1));
            i = pe + 1;
            p = value.indexOf("${", i);
            if (p == -1) {
                if (i < max) {
                    sb.append(value.substring(i, max));
                }
            }
        }
        return sb.toString();
    }
}
