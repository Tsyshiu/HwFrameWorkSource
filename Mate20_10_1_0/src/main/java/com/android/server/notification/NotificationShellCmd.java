package com.android.server.notification;

import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Person;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.biometrics.face.V1_0.FaceAcquiredInfo;
import android.net.INetd;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Slog;
import com.android.server.TrustedUIService;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Collections;

public class NotificationShellCmd extends ShellCommand {
    public static final String CHANNEL_ID = "shellcmd";
    public static final int CHANNEL_IMP = 3;
    public static final String CHANNEL_NAME = "Shell command";
    public static final int NOTIFICATION_ID = 1138;
    public static final String NOTIFICATION_PACKAGE = "com.android.shell";
    private static final String NOTIFY_USAGE = "usage: cmd notification post [flags] <tag> <text>\n\nflags:\n  -h|--help\n  -v|--verbose\n  -t|--title <text>\n  -i|--icon <iconspec>\n  -I|--large-icon <iconspec>\n  -S|--style <style> [styleargs]\n  -c|--content-intent <intentspec>\n\nstyles: (default none)\n  bigtext\n  bigpicture --picture <iconspec>\n  inbox --line <text> --line <text> ...\n  messaging --conversation <title> --message <who>:<text> ...\n  media\n\nan <iconspec> is one of\n  file:///data/local/tmp/<img.png>\n  content://<provider>/<path>\n  @[<package>:]drawable/<img>\n  data:base64,<B64DATA==>\n\nan <intentspec> is (broadcast|service|activity) <args>\n  <args> are as described in `am start`";
    private static final String USAGE = "usage: cmd notification SUBCMD [args]\n\nSUBCMDs:\n  allow_listener COMPONENT [user_id (current user if not specified)]\n  disallow_listener COMPONENT [user_id (current user if not specified)]\n  allow_assistant COMPONENT [user_id (current user if not specified)]\n  remove_assistant COMPONENT [user_id (current user if not specified)]\n  allow_dnd PACKAGE [user_id (current user if not specified)]\n  disallow_dnd PACKAGE [user_id (current user if not specified)]\n  suspend_package PACKAGE\n  unsuspend_package PACKAGE\n  reset_assistant_user_set [user_id (current user if not specified)]\n  get_approved_assistant [user_id (current user if not specified)]\n  post [--help | flags] TAG TEXT";
    private final INotificationManager mBinderService;
    private final NotificationManagerService mDirectService;

    public NotificationShellCmd(NotificationManagerService service) {
        this.mDirectService = service;
        this.mBinderService = service.getBinderService();
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            String replace = cmd.replace('-', '_');
            switch (replace.hashCode()) {
                case -1325770982:
                    if (replace.equals("disallow_assistant")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -1039689911:
                    if (replace.equals("notify")) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case -506770550:
                    if (replace.equals("unsuspend_package")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -432999190:
                    if (replace.equals("allow_listener")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -429832618:
                    if (replace.equals("disallow_dnd")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -414550305:
                    if (replace.equals("get_approved_assistant")) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case 3446944:
                    if (replace.equals("post")) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case 372345636:
                    if (replace.equals("allow_dnd")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 393969475:
                    if (replace.equals("suspend_package")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 683492127:
                    if (replace.equals("reset_assistant_user_set")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case 1257269496:
                    if (replace.equals("disallow_listener")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 1570441869:
                    if (replace.equals("distract_package")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 2110474600:
                    if (replace.equals("allow_assistant")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    String packageName = getNextArgRequired();
                    int userId = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationPolicyAccessGrantedForUser(packageName, userId, true);
                    break;
                case 1:
                    String packageName2 = getNextArgRequired();
                    int userId2 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId2 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationPolicyAccessGrantedForUser(packageName2, userId2, false);
                    break;
                case 2:
                    ComponentName cn = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn != null) {
                        int userId3 = ActivityManager.getCurrentUser();
                        if (peekNextArg() != null) {
                            userId3 = Integer.parseInt(getNextArgRequired());
                        }
                        this.mBinderService.setNotificationListenerAccessGrantedForUser(cn, userId3, true);
                        break;
                    } else {
                        pw.println("Invalid listener - must be a ComponentName");
                        return -1;
                    }
                case 3:
                    ComponentName cn2 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn2 != null) {
                        int userId4 = ActivityManager.getCurrentUser();
                        if (peekNextArg() != null) {
                            userId4 = Integer.parseInt(getNextArgRequired());
                        }
                        this.mBinderService.setNotificationListenerAccessGrantedForUser(cn2, userId4, false);
                        break;
                    } else {
                        pw.println("Invalid listener - must be a ComponentName");
                        return -1;
                    }
                case 4:
                    ComponentName cn3 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn3 != null) {
                        int userId5 = ActivityManager.getCurrentUser();
                        if (peekNextArg() != null) {
                            userId5 = Integer.parseInt(getNextArgRequired());
                        }
                        this.mBinderService.setNotificationAssistantAccessGrantedForUser(cn3, userId5, true);
                        break;
                    } else {
                        pw.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    }
                case 5:
                    ComponentName cn4 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn4 != null) {
                        int userId6 = ActivityManager.getCurrentUser();
                        if (peekNextArg() != null) {
                            userId6 = Integer.parseInt(getNextArgRequired());
                        }
                        this.mBinderService.setNotificationAssistantAccessGrantedForUser(cn4, userId6, false);
                        break;
                    } else {
                        pw.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    }
                case 6:
                    this.mDirectService.simulatePackageSuspendBroadcast(true, getNextArgRequired());
                    break;
                case 7:
                    this.mDirectService.simulatePackageSuspendBroadcast(false, getNextArgRequired());
                    break;
                case '\b':
                    this.mDirectService.simulatePackageDistractionBroadcast(Integer.parseInt(getNextArgRequired()), getNextArgRequired().split(","));
                    break;
                case '\t':
                    int userId7 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId7 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mDirectService.resetAssistantUserSet(userId7);
                    break;
                case '\n':
                    int userId8 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId8 = Integer.parseInt(getNextArgRequired());
                    }
                    ComponentName approvedAssistant = this.mDirectService.getApprovedAssistant(userId8);
                    if (approvedAssistant != null) {
                        pw.println(approvedAssistant.flattenToString());
                        break;
                    } else {
                        pw.println("null");
                        break;
                    }
                case 11:
                case '\f':
                    doNotify(pw);
                    break;
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (Exception e) {
            pw.println("Error occurred. Check logcat for details. " + e.getMessage());
            Slog.e("NotificationService", "Error running shell command", e);
        }
        return 0;
    }

    /* access modifiers changed from: package-private */
    public void ensureChannel() throws RemoteException {
        int uid = Binder.getCallingUid();
        int userid = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mBinderService.getNotificationChannelForPackage(NOTIFICATION_PACKAGE, uid, CHANNEL_ID, false) == null) {
                NotificationChannel chan = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, 3);
                Slog.v("NotificationService", "creating shell channel for user " + userid + " uid " + uid + ": " + chan);
                this.mBinderService.createNotificationChannelsForPackage(NOTIFICATION_PACKAGE, uid, new ParceledListSlice(Collections.singletonList(chan)));
                StringBuilder sb = new StringBuilder();
                sb.append("created channel: ");
                sb.append(this.mBinderService.getNotificationChannelForPackage(NOTIFICATION_PACKAGE, uid, CHANNEL_ID, false));
                Slog.v("NotificationService", sb.toString());
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* access modifiers changed from: package-private */
    public Icon parseIcon(Resources res, String encoded) throws IllegalArgumentException {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        if (encoded.startsWith(SliceClientPermissions.SliceAuthority.DELIMITER)) {
            encoded = "file://" + encoded;
        }
        if (encoded.startsWith("http:") || encoded.startsWith("https:") || encoded.startsWith("content:") || encoded.startsWith("file:") || encoded.startsWith("android.resource:")) {
            return Icon.createWithContentUri(Uri.parse(encoded));
        }
        if (encoded.startsWith("@")) {
            int resid = res.getIdentifier(encoded.substring(1), "drawable", PackageManagerService.PLATFORM_PACKAGE_NAME);
            if (resid != 0) {
                return Icon.createWithResource(res, resid);
            }
        } else if (encoded.startsWith("data:")) {
            byte[] bits = Base64.decode(encoded.substring(encoded.indexOf(44) + 1), 0);
            return Icon.createWithData(bits, 0, bits.length);
        }
        return null;
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARNING: Removed duplicated region for block: B:188:0x03cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x03e6  */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x0408  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0414  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0422  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x047d  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x0559  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x0561  */
    /* JADX WARNING: Removed duplicated region for block: B:256:0x0467 A[SYNTHETIC] */
    private int doNotify(PrintWriter pw) throws RemoteException, URISyntaxException {
        char c;
        Notification.InboxStyle inboxStyle;
        Notification.MessagingStyle messagingStyle;
        Icon icon;
        Notification.MessagingStyle messagingStyle2;
        Notification.InboxStyle inboxStyle2;
        char c2;
        String intentKind;
        Intent intent;
        PendingIntent pi;
        char c3;
        NotificationShellCmd notificationShellCmd = this;
        Context context = notificationShellCmd.mDirectService.getContext();
        Resources res = context.getResources();
        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID);
        Notification.MessagingStyle messagingStyle3 = null;
        boolean verbose = false;
        Notification.BigPictureStyle bigPictureStyle = null;
        Notification.BigTextStyle bigTextStyle = null;
        Icon smallIcon = null;
        Notification.InboxStyle inboxStyle3 = null;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                boolean large = false;
                switch (opt.hashCode()) {
                    case -1954060697:
                        if (opt.equals("--message")) {
                            c = 25;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1613915119:
                        if (opt.equals("--style")) {
                            c = 19;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1613324104:
                        if (opt.equals("--title")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1210178960:
                        if (opt.equals("content-intent")) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1183762788:
                        if (opt.equals(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT)) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case -853380573:
                        if (opt.equals("--conversation")) {
                            c = 26;
                            break;
                        }
                        c = 65535;
                        break;
                    case -45879957:
                        if (opt.equals("--large-icon")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1468:
                        if (opt.equals("-I")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1478:
                        if (opt.equals("-S")) {
                            c = 18;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1494:
                        if (opt.equals("-c")) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 1499:
                        if (opt.equals("-h")) {
                            c = 27;
                            break;
                        }
                        c = 65535;
                        break;
                    case NetworkConstants.ETHER_MTU:
                        if (opt.equals("-i")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case 1511:
                        if (opt.equals("-t")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1513:
                        if (opt.equals("-v")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3226745:
                        if (opt.equals("icon")) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case 43017097:
                        if (opt.equals("--wtf")) {
                            c = 29;
                            break;
                        }
                        c = 65535;
                        break;
                    case 110371416:
                        if (opt.equals("title")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 704999290:
                        if (opt.equals("--big-text")) {
                            c = 22;
                            break;
                        }
                        c = 65535;
                        break;
                    case 705941520:
                        if (opt.equals("--content-intent")) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case 758833716:
                        if (opt.equals("largeicon")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case 808239966:
                        if (opt.equals("--picture")) {
                            c = 23;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1216250940:
                        if (opt.equals("--intent")) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1247228052:
                        if (opt.equals("--largeicon")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1270815917:
                        if (opt.equals("--bigText")) {
                            c = 20;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1271769229:
                        if (opt.equals("--bigtext")) {
                            c = 21;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333069025:
                        if (opt.equals("--help")) {
                            c = 28;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333096985:
                        if (opt.equals("--icon")) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333192084:
                        if (opt.equals("--line")) {
                            c = 24;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1737088994:
                        if (opt.equals("--verbose")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1993764811:
                        if (opt.equals("large-icon")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        verbose = true;
                        break;
                    case 2:
                    case 3:
                    case 4:
                        messagingStyle = messagingStyle3;
                        inboxStyle = inboxStyle3;
                        builder.setContentTitle(getNextArgRequired());
                        messagingStyle3 = messagingStyle;
                        inboxStyle3 = inboxStyle;
                        break;
                    case 5:
                    case 6:
                    case 7:
                    case '\b':
                    case '\t':
                        messagingStyle2 = messagingStyle3;
                        inboxStyle2 = inboxStyle3;
                        large = true;
                        String iconSpec = getNextArgRequired();
                        icon = notificationShellCmd.parseIcon(res, iconSpec);
                        if (icon == null) {
                            if (!large) {
                                smallIcon = icon;
                                messagingStyle3 = messagingStyle;
                                inboxStyle3 = inboxStyle;
                                break;
                            } else {
                                builder.setLargeIcon(icon);
                                messagingStyle3 = messagingStyle;
                                inboxStyle3 = inboxStyle;
                                break;
                            }
                        } else {
                            pw.println("error: invalid icon: " + iconSpec);
                            return -1;
                        }
                    case '\n':
                    case 11:
                    case '\f':
                        messagingStyle2 = messagingStyle3;
                        inboxStyle2 = inboxStyle3;
                        String iconSpec2 = getNextArgRequired();
                        icon = notificationShellCmd.parseIcon(res, iconSpec2);
                        if (icon == null) {
                        }
                        break;
                    case '\r':
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                        String peekNextArg = peekNextArg();
                        int hashCode = peekNextArg.hashCode();
                        if (hashCode != -1655966961) {
                            if (hashCode != -1618876223) {
                                if (hashCode == 1984153269 && peekNextArg.equals("service")) {
                                    c2 = 1;
                                    if (c2 != 0 || c2 == 1 || c2 == 2) {
                                        intentKind = getNextArg();
                                    } else {
                                        intentKind = null;
                                    }
                                    intent = Intent.parseCommandArgs(notificationShellCmd, null);
                                    if (intent.getData() != null) {
                                        StringBuilder sb = new StringBuilder();
                                        messagingStyle = messagingStyle3;
                                        sb.append("xyz:");
                                        inboxStyle = inboxStyle3;
                                        sb.append(System.currentTimeMillis());
                                        intent.setData(Uri.parse(sb.toString()));
                                    } else {
                                        messagingStyle = messagingStyle3;
                                        inboxStyle = inboxStyle3;
                                    }
                                    if (!INetd.IF_FLAG_BROADCAST.equals(intentKind)) {
                                        pi = PendingIntent.getBroadcastAsUser(context, 0, intent, 134217728, UserHandle.CURRENT);
                                    } else if ("service".equals(intentKind)) {
                                        pi = PendingIntent.getService(context, 0, intent, 134217728);
                                    } else {
                                        pi = PendingIntent.getActivityAsUser(context, 0, intent, 134217728, null, UserHandle.CURRENT);
                                    }
                                    builder.setContentIntent(pi);
                                    messagingStyle3 = messagingStyle;
                                    inboxStyle3 = inboxStyle;
                                    break;
                                }
                            } else if (peekNextArg.equals(INetd.IF_FLAG_BROADCAST)) {
                                c2 = 0;
                                if (c2 != 0) {
                                }
                                intentKind = getNextArg();
                                intent = Intent.parseCommandArgs(notificationShellCmd, null);
                                if (intent.getData() != null) {
                                }
                                if (!INetd.IF_FLAG_BROADCAST.equals(intentKind)) {
                                }
                                builder.setContentIntent(pi);
                                messagingStyle3 = messagingStyle;
                                inboxStyle3 = inboxStyle;
                            }
                        } else if (peekNextArg.equals("activity")) {
                            c2 = 2;
                            if (c2 != 0) {
                            }
                            intentKind = getNextArg();
                            intent = Intent.parseCommandArgs(notificationShellCmd, null);
                            if (intent.getData() != null) {
                            }
                            if (!INetd.IF_FLAG_BROADCAST.equals(intentKind)) {
                            }
                            builder.setContentIntent(pi);
                            messagingStyle3 = messagingStyle;
                            inboxStyle3 = inboxStyle;
                        }
                        c2 = 65535;
                        if (c2 != 0) {
                        }
                        intentKind = getNextArg();
                        intent = Intent.parseCommandArgs(notificationShellCmd, null);
                        if (intent.getData() != null) {
                        }
                        if (!INetd.IF_FLAG_BROADCAST.equals(intentKind)) {
                        }
                        builder.setContentIntent(pi);
                        messagingStyle3 = messagingStyle;
                        inboxStyle3 = inboxStyle;
                    case 18:
                    case FaceAcquiredInfo.FACE_OBSCURED:
                        String styleSpec = getNextArgRequired().toLowerCase();
                        switch (styleSpec.hashCode()) {
                            case -1440008444:
                                if (styleSpec.equals("messaging")) {
                                    c3 = 3;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case -114212307:
                                if (styleSpec.equals("bigtext")) {
                                    c3 = 0;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case -44548098:
                                if (styleSpec.equals("bigpicture")) {
                                    c3 = 1;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case 100344454:
                                if (styleSpec.equals("inbox")) {
                                    c3 = 2;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case 103772132:
                                if (styleSpec.equals("media")) {
                                    c3 = 4;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            default:
                                c3 = 65535;
                                break;
                        }
                        if (c3 == 0) {
                            bigTextStyle = new Notification.BigTextStyle();
                            builder.setStyle(bigTextStyle);
                        } else if (c3 == 1) {
                            bigPictureStyle = new Notification.BigPictureStyle();
                            builder.setStyle(bigPictureStyle);
                        } else if (c3 == 2) {
                            inboxStyle3 = new Notification.InboxStyle();
                            builder.setStyle(inboxStyle3);
                        } else if (c3 == 3) {
                            String name = "You";
                            if ("--user".equals(peekNextArg())) {
                                getNextArg();
                                name = getNextArgRequired();
                            }
                            messagingStyle3 = new Notification.MessagingStyle(new Person.Builder().setName(name).build());
                            builder.setStyle(messagingStyle3);
                        } else if (c3 == 4) {
                            builder.setStyle(new Notification.MediaStyle());
                        } else {
                            throw new IllegalArgumentException("unrecognized notification style: " + styleSpec);
                        }
                        break;
                    case 20:
                    case 21:
                    case FaceAcquiredInfo.VENDOR:
                        if (bigTextStyle != null) {
                            bigTextStyle.bigText(getNextArgRequired());
                            messagingStyle = messagingStyle3;
                            inboxStyle = inboxStyle3;
                            messagingStyle3 = messagingStyle;
                            inboxStyle3 = inboxStyle;
                            break;
                        } else {
                            throw new IllegalArgumentException("--bigtext requires --style bigtext");
                        }
                    case 23:
                        if (bigPictureStyle != null) {
                            String pictureSpec = getNextArgRequired();
                            Icon pictureAsIcon = notificationShellCmd.parseIcon(res, pictureSpec);
                            if (pictureAsIcon != null) {
                                Drawable d = pictureAsIcon.loadDrawable(context);
                                if (d instanceof BitmapDrawable) {
                                    bigPictureStyle.bigPicture(((BitmapDrawable) d).getBitmap());
                                    messagingStyle = messagingStyle3;
                                    inboxStyle = inboxStyle3;
                                    messagingStyle3 = messagingStyle;
                                    inboxStyle3 = inboxStyle;
                                    break;
                                } else {
                                    throw new IllegalArgumentException("not a bitmap: " + pictureSpec);
                                }
                            } else {
                                throw new IllegalArgumentException("bad picture spec: " + pictureSpec);
                            }
                        } else {
                            throw new IllegalArgumentException("--picture requires --style bigpicture");
                        }
                    case 24:
                        if (inboxStyle3 != null) {
                            inboxStyle3.addLine(getNextArgRequired());
                            messagingStyle = messagingStyle3;
                            inboxStyle = inboxStyle3;
                            messagingStyle3 = messagingStyle;
                            inboxStyle3 = inboxStyle;
                            break;
                        } else {
                            throw new IllegalArgumentException("--line requires --style inbox");
                        }
                    case 25:
                        if (messagingStyle3 != null) {
                            String[] parts = getNextArgRequired().split(":", 2);
                            if (parts.length > 1) {
                                messagingStyle3.addMessage(parts[1], System.currentTimeMillis(), parts[0]);
                                messagingStyle = messagingStyle3;
                                inboxStyle = inboxStyle3;
                            } else {
                                messagingStyle3.addMessage(parts[0], System.currentTimeMillis(), new String[]{messagingStyle3.getUserDisplayName().toString(), "Them"}[messagingStyle3.getMessages().size() % 2]);
                                messagingStyle = messagingStyle3;
                                inboxStyle = inboxStyle3;
                            }
                            messagingStyle3 = messagingStyle;
                            inboxStyle3 = inboxStyle;
                            break;
                        } else {
                            throw new IllegalArgumentException("--message requires --style messaging");
                        }
                    case TrustedUIService.TUI_POLL_FOLD:
                        if (messagingStyle3 != null) {
                            messagingStyle3.setConversationTitle(getNextArgRequired());
                            messagingStyle = messagingStyle3;
                            inboxStyle = inboxStyle3;
                            messagingStyle3 = messagingStyle;
                            inboxStyle3 = inboxStyle;
                            break;
                        } else {
                            throw new IllegalArgumentException("--conversation requires --style messaging");
                        }
                    default:
                        pw.println(NOTIFY_USAGE);
                        return 0;
                }
            } else {
                String tag = getNextArg();
                String text = getNextArg();
                if (tag != null) {
                    if (text != null) {
                        builder.setContentText(text);
                        if (smallIcon == null) {
                            builder.setSmallIcon(17301623);
                        } else {
                            builder.setSmallIcon(smallIcon);
                        }
                        ensureChannel();
                        Notification n = builder.build();
                        pw.println("posting:\n  " + n);
                        Slog.v("NotificationManager", "posting: " + n);
                        int userId = UserHandle.getCallingUserId();
                        long token = Binder.clearCallingIdentity();
                        try {
                            notificationShellCmd.mBinderService.enqueueNotificationWithTag(NOTIFICATION_PACKAGE, PackageManagerService.PLATFORM_PACKAGE_NAME, tag, (int) NOTIFICATION_ID, n, userId);
                            if (!verbose) {
                                return 0;
                            }
                            int tries = 3;
                            NotificationRecord nr = notificationShellCmd.mDirectService.findNotificationLocked(NOTIFICATION_PACKAGE, tag, NOTIFICATION_ID, userId);
                            while (true) {
                                int tries2 = tries - 1;
                                if (tries > 0 && nr == null) {
                                    try {
                                        pw.println("waiting for notification to post...");
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                    }
                                    nr = notificationShellCmd.mDirectService.findNotificationLocked(NOTIFICATION_PACKAGE, tag, NOTIFICATION_ID, userId);
                                    notificationShellCmd = this;
                                    tries = tries2;
                                } else if (nr != null) {
                                    pw.println("warning: couldn't find notification after enqueueing");
                                    return 0;
                                } else {
                                    pw.println("posted: ");
                                    nr.dump(pw, "  ", context, false);
                                    return 0;
                                }
                            }
                            if (nr != null) {
                            }
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                }
                pw.println(NOTIFY_USAGE);
                return -1;
            }
        }
    }

    public void onHelp() {
        getOutPrintWriter().println(USAGE);
    }
}
