package jcifs.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import jcifs.Config;
import jcifs.UniAddress;
import jcifs.dcerpc.msrpc.samr;
import jcifs.netbios.NbtAddress;
import jcifs.smb.DfsReferral;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbSession;
import jcifs.util.Base64;
import jcifs.util.LogStream;
import jcifs.util.MimeMap;

public class NetworkExplorer extends HttpServlet {
    private static LogStream log = LogStream.getInstance();
    private boolean credentialsSupplied;
    private String defaultDomain;
    private boolean enableBasic;
    private boolean insecureBasic;
    private MimeMap mimeMap;
    private NtlmSsp ntlmSsp;
    private String realm;
    private String style;

    public void init() throws ServletException {
        StringBuffer sb = new StringBuffer();
        byte[] buf = new byte[samr.ACB_AUTOLOCK];
        Config.setProperty("jcifs.smb.client.soTimeout", "600000");
        Config.setProperty("jcifs.smb.client.attrExpirationPeriod", "300000");
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (name.startsWith("jcifs.")) {
                Config.setProperty(name, getInitParameter(name));
            }
        }
        if (Config.getProperty("jcifs.smb.client.username") == null) {
            this.ntlmSsp = new NtlmSsp();
        } else {
            this.credentialsSupplied = true;
        }
        try {
            this.mimeMap = new MimeMap();
            InputStream is = getClass().getClassLoader().getResourceAsStream("jcifs/http/ne.css");
            while (true) {
                int n = is.read(buf);
                if (n == -1) {
                    break;
                }
                sb.append(new String(buf, 0, n, "ISO8859_1"));
            }
            this.style = sb.toString();
            this.enableBasic = Config.getBoolean("jcifs.http.enableBasic", false);
            this.insecureBasic = Config.getBoolean("jcifs.http.insecureBasic", false);
            this.realm = Config.getProperty("jcifs.http.basicRealm");
            if (this.realm == null) {
                this.realm = "jCIFS";
            }
            this.defaultDomain = Config.getProperty("jcifs.smb.client.domain");
            int level = Config.getInt("jcifs.util.loglevel", -1);
            if (level != -1) {
                LogStream.setLevel(level);
            }
            LogStream logStream = log;
            if (LogStream.level > 2) {
                try {
                    Config.store(log, "JCIFS PROPERTIES");
                } catch (IOException e2) {
                }
            }
        } catch (IOException ioe) {
            throw new ServletException(ioe.getMessage());
        }
    }

    /* access modifiers changed from: protected */
    public void doFile(HttpServletRequest req, HttpServletResponse resp, SmbFile file) throws IOException {
        String type;
        byte[] buf = new byte[8192];
        SmbFileInputStream in = new SmbFileInputStream(file);
        ServletOutputStream out = resp.getOutputStream();
        String url = file.getPath();
        resp.setContentType("text/plain");
        int n = url.lastIndexOf(46);
        if (n > 0 && (type = url.substring(n + 1)) != null && type.length() > 1 && type.length() < 6) {
            resp.setContentType(this.mimeMap.getMimeType(type));
        }
        resp.setHeader("Content-Length", file.length() + "");
        resp.setHeader("Accept-Ranges", "Bytes");
        while (true) {
            int n2 = in.read(buf);
            if (n2 != -1) {
                out.write(buf, 0, n2);
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public int compareNames(SmbFile f1, String f1name, SmbFile f2) throws IOException {
        if (f1.isDirectory() != f2.isDirectory()) {
            return f1.isDirectory() ? -1 : 1;
        }
        return f1name.compareToIgnoreCase(f2.getName());
    }

    /* access modifiers changed from: protected */
    public int compareSizes(SmbFile f1, String f1name, SmbFile f2) throws IOException {
        if (f1.isDirectory() != f2.isDirectory()) {
            return f1.isDirectory() ? -1 : 1;
        }
        if (f1.isDirectory()) {
            return f1name.compareToIgnoreCase(f2.getName());
        }
        long diff = f1.length() - f2.length();
        if (diff == 0) {
            return f1name.compareToIgnoreCase(f2.getName());
        }
        return diff <= 0 ? 1 : -1;
    }

    /* access modifiers changed from: protected */
    public int compareTypes(SmbFile f1, String f1name, SmbFile f2) throws IOException {
        if (f1.isDirectory() == f2.isDirectory()) {
            String f2name = f2.getName();
            if (f1.isDirectory()) {
                return f1name.compareToIgnoreCase(f2name);
            }
            int i = f1name.lastIndexOf(46);
            String t1 = i == -1 ? "" : f1name.substring(i + 1);
            int i2 = f2name.lastIndexOf(46);
            int i3 = t1.compareToIgnoreCase(i2 == -1 ? "" : f2name.substring(i2 + 1));
            if (i3 == 0) {
                return f1name.compareToIgnoreCase(f2name);
            }
            return i3;
        } else if (f1.isDirectory()) {
            return -1;
        } else {
            return 1;
        }
    }

    /* access modifiers changed from: protected */
    public int compareDates(SmbFile f1, String f1name, SmbFile f2) throws IOException {
        if (f1.isDirectory() != f2.isDirectory()) {
            return f1.isDirectory() ? -1 : 1;
        }
        if (f1.isDirectory()) {
            return f1name.compareToIgnoreCase(f2.getName());
        }
        return f1.lastModified() <= f2.lastModified() ? 1 : -1;
    }

    /* access modifiers changed from: protected */
    public void doDirectory(HttpServletRequest req, HttpServletResponse resp, SmbFile dir) throws IOException {
        String path;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/d/yy h:mm a");
        sdf.setCalendar(new GregorianCalendar());
        SmbFile[] dirents = dir.listFiles();
        LogStream logStream = log;
        if (LogStream.level > 2) {
            log.println(dirents.length + " items listed");
        }
        LinkedList sorted = new LinkedList();
        String fmt = req.getParameter("fmt");
        if (fmt == null) {
            fmt = "col";
        }
        int sort = 0;
        String str = req.getParameter("sort");
        if (str == null || str.equals("name")) {
            sort = 0;
        } else if (str.equals("size")) {
            sort = 1;
        } else if (str.equals("type")) {
            sort = 2;
        } else if (str.equals("date")) {
            sort = 3;
        }
        int fileCount = 0;
        int dirCount = 0;
        int maxLen = 28;
        for (int i = 0; i < dirents.length; i++) {
            try {
                if (dirents[i].getType() == 16) {
                }
            } catch (SmbAuthException sae) {
                LogStream logStream2 = log;
                if (LogStream.level > 2) {
                    sae.printStackTrace(log);
                }
            } catch (SmbException se) {
                LogStream logStream3 = log;
                if (LogStream.level > 2) {
                    se.printStackTrace(log);
                }
                if (se.getNtStatus() != -1073741823) {
                    throw se;
                }
            }
            if (dirents[i].isDirectory()) {
                dirCount++;
            } else {
                fileCount++;
            }
            String name = dirents[i].getName();
            LogStream logStream4 = log;
            if (LogStream.level > 3) {
                log.println(i + ": " + name);
            }
            int len = name.length();
            if (len > maxLen) {
                maxLen = len;
            }
            ListIterator iter = sorted.listIterator();
            int j = 0;
            while (iter.hasNext()) {
                if (sort != 0) {
                    if (sort != 1) {
                        if (sort != 2) {
                            if (sort == 3 && compareDates(dirents[i], name, (SmbFile) iter.next()) < 0) {
                                break;
                            }
                        } else if (compareTypes(dirents[i], name, (SmbFile) iter.next()) < 0) {
                            break;
                        }
                    } else if (compareSizes(dirents[i], name, (SmbFile) iter.next()) < 0) {
                        break;
                    }
                } else if (compareNames(dirents[i], name, (SmbFile) iter.next()) < 0) {
                    break;
                }
                j++;
            }
            sorted.add(j, dirents[i]);
        }
        if (maxLen > 50) {
            maxLen = 50;
        }
        int maxLen2 = maxLen * 9;
        PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        out.println("<html><head><title>Network Explorer</title>");
        out.println("<meta HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">");
        out.println("<style TYPE=\"text/css\">");
        out.println(this.style);
        if (dirents.length < 200) {
            out.println("    a:hover {");
            out.println("        background: #a2ff01;");
            out.println("    }");
        }
        out.println("</STYLE>");
        out.println("</head><body>");
        out.print("<a class=\"sort\" style=\"width: " + maxLen2 + ";\" href=\"?fmt=detail&sort=name\">Name</a>");
        out.println("<a class=\"sort\" href=\"?fmt=detail&sort=size\">Size</a>");
        out.println("<a class=\"sort\" href=\"?fmt=detail&sort=type\">Type</a>");
        out.println("<a class=\"sort\" style=\"width: 180\" href=\"?fmt=detail&sort=date\">Modified</a><br clear='all'><p>");
        String path2 = dir.getCanonicalPath();
        if (path2.length() < 7) {
            out.println("<b><big>smb://</big></b><br>");
            path = ".";
        } else {
            out.println("<b><big>" + path2 + "</big></b><br>");
            path = "../";
        }
        out.println((dirCount + fileCount) + " objects (" + dirCount + " directories, " + fileCount + " files)<br>");
        out.println("<b><a class=\"plain\" href=\".\">normal</a> | <a class=\"plain\" href=\"?fmt=detail\">detailed</a></b>");
        out.println("<p><table border='0' cellspacing='0' cellpadding='0'><tr><td>");
        out.print("<A style=\"width: " + maxLen2);
        out.print("; height: 18;\" HREF=\"");
        out.print(path);
        out.println("\"><b>&uarr;</b></a>");
        if (fmt.equals("detail")) {
            out.println("<br clear='all'>");
        }
        if (path.length() == 1 || dir.getType() != 2) {
            path = "";
        }
        ListIterator iter2 = sorted.listIterator();
        while (iter2.hasNext()) {
            SmbFile f = (SmbFile) iter2.next();
            String name2 = f.getName();
            if (fmt.equals("detail")) {
                out.print("<A style=\"width: " + maxLen2);
                out.print("; height: 18;\" HREF=\"");
                out.print(path);
                out.print(name2);
                if (f.isDirectory()) {
                    out.print("?fmt=detail\"><b>");
                    out.print(name2);
                    out.print("</b></a>");
                } else {
                    out.print("\"><b>");
                    out.print(name2);
                    out.print("</b></a><div align='right'>");
                    out.print((f.length() / 1024) + " KB </div><div>");
                    int i2 = name2.lastIndexOf(46) + 1;
                    if (i2 <= 1 || name2.length() - i2 >= 6) {
                        out.print("&nbsp;</div>");
                    } else {
                        out.print(name2.substring(i2).toUpperCase() + "</div class='ext'>");
                    }
                    out.print("<div style='width: 180'>");
                    out.print(sdf.format(new Date(f.lastModified())));
                    out.print("</div>");
                }
                out.println("<br clear='all'>");
            } else {
                out.print("<A style=\"width: " + maxLen2);
                if (f.isDirectory()) {
                    out.print("; height: 18;\" HREF=\"");
                    out.print(path);
                    out.print(name2);
                    out.print("\"><b>");
                    out.print(name2);
                    out.print("</b></a>");
                } else {
                    out.print(";\" HREF=\"");
                    out.print(path);
                    out.print(name2);
                    out.print("\"><b>");
                    out.print(name2);
                    out.print("</b><br><small>");
                    out.print((f.length() / 1024) + "KB <br>");
                    out.print(sdf.format(new Date(f.lastModified())));
                    out.print("</small>");
                    out.println("</a>");
                }
            }
        }
        out.println("</td></tr></table>");
        out.println("</BODY></HTML>");
        out.close();
    }

    private String parseServerAndShare(String pathInfo) {
        int i;
        int i2;
        char[] out = new char[256];
        if (pathInfo == null) {
            return null;
        }
        int len = pathInfo.length();
        int p = 0;
        while (p < len && pathInfo.charAt(p) == '/') {
            p++;
        }
        if (p == len) {
            return null;
        }
        int i3 = 0;
        while (p < len) {
            char ch = pathInfo.charAt(p);
            if (ch == '/') {
                break;
            }
            out[i3] = ch;
            p++;
            i3++;
        }
        while (p < len && pathInfo.charAt(p) == '/') {
            p++;
        }
        if (p < len) {
            int i4 = i3 + 1;
            out[i3] = '/';
            while (true) {
                i2 = i4 + 1;
                int p2 = p + 1;
                char ch2 = pathInfo.charAt(p);
                out[i4] = ch2;
                if (p2 >= len || ch2 == '/') {
                    i = i2;
                } else {
                    i4 = i2;
                    p = p2;
                }
            }
            i = i2;
        } else {
            i = i3;
        }
        return new String(out, 0, i);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        SmbFile file;
        String user;
        UniAddress dc;
        int i;
        String server = null;
        boolean possibleWorkgroup = true;
        NtlmPasswordAuthentication ntlm = null;
        HttpSession ssn = req.getSession(false);
        String pathInfo = req.getPathInfo();
        if (!(pathInfo == null || (server = parseServerAndShare(pathInfo)) == null || (i = server.indexOf(47)) <= 0)) {
            server = server.substring(0, i).toLowerCase();
            possibleWorkgroup = false;
        }
        String msg = req.getHeader("Authorization");
        boolean offerBasic = this.enableBasic && (this.insecureBasic || req.isSecure());
        if (msg != null && (msg.startsWith("NTLM ") || (offerBasic && msg.startsWith("Basic ")))) {
            if (msg.startsWith("NTLM ")) {
                if (pathInfo == null || server == null) {
                    dc = UniAddress.getByName(NbtAddress.getByName(NbtAddress.MASTER_BROWSER_NAME, 1, null).getHostAddress());
                } else {
                    dc = UniAddress.getByName(server, possibleWorkgroup);
                }
                req.getSession();
                ntlm = NtlmSsp.authenticate(req, resp, SmbSession.getChallenge(dc));
                if (ntlm == null) {
                    return;
                }
            } else {
                String auth = new String(Base64.decode(msg.substring(6)), "US-ASCII");
                int index = auth.indexOf(58);
                if (index != -1) {
                    user = auth.substring(0, index);
                } else {
                    user = auth;
                }
                String password = index != -1 ? auth.substring(index + 1) : "";
                int index2 = user.indexOf(92);
                if (index2 == -1) {
                    index2 = user.indexOf(47);
                }
                String domain = index2 != -1 ? user.substring(0, index2) : this.defaultDomain;
                if (index2 != -1) {
                    user = user.substring(index2 + 1);
                }
                ntlm = new NtlmPasswordAuthentication(domain, user, password);
            }
            req.getSession().setAttribute("npa-" + server, ntlm);
        } else if (!this.credentialsSupplied) {
            if (ssn != null) {
                ntlm = (NtlmPasswordAuthentication) ssn.getAttribute("npa-" + server);
            }
            if (ntlm == null) {
                resp.setHeader("WWW-Authenticate", "NTLM");
                if (offerBasic) {
                    resp.addHeader("WWW-Authenticate", "Basic realm=\"" + this.realm + "\"");
                }
                resp.setHeader("Connection", "close");
                resp.setStatus(401);
                resp.flushBuffer();
                return;
            }
        }
        if (ntlm != null) {
            try {
                file = new SmbFile("smb:/" + pathInfo, ntlm);
            } catch (SmbAuthException sae) {
                if (ssn != null) {
                    ssn.removeAttribute("npa-" + server);
                }
                if (sae.getNtStatus() == -1073741819) {
                    resp.sendRedirect(req.getRequestURL().toString());
                    return;
                }
                resp.setHeader("WWW-Authenticate", "NTLM");
                if (offerBasic) {
                    resp.addHeader("WWW-Authenticate", "Basic realm=\"" + this.realm + "\"");
                }
                resp.setHeader("Connection", "close");
                resp.setStatus(401);
                resp.flushBuffer();
                return;
            } catch (DfsReferral dr) {
                StringBuffer redir = req.getRequestURL();
                String qs = req.getQueryString();
                StringBuffer redir2 = new StringBuffer(redir.substring(0, redir.length() - req.getPathInfo().length()));
                redir2.append('/');
                redir2.append(dr.server);
                redir2.append('/');
                redir2.append(dr.share);
                redir2.append('/');
                if (qs != null) {
                    redir2.append(req.getQueryString());
                }
                resp.sendRedirect(redir2.toString());
                resp.flushBuffer();
                return;
            }
        } else if (server == null) {
            file = new SmbFile("smb://");
        } else {
            file = new SmbFile("smb:/" + pathInfo);
        }
        if (file.isDirectory()) {
            doDirectory(req, resp, file);
        } else {
            doFile(req, resp, file);
        }
    }
}
