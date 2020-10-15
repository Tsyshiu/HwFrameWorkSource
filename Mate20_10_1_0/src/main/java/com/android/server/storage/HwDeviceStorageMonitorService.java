package com.android.server.storage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hwnotification.HwNotificationResource;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;

public class HwDeviceStorageMonitorService extends DeviceStorageMonitorService {
    private static final String ACTION_INTERNAL_STORAGE_SYSTEMMANAGER = "com.huawei.systemmanager.spacecleanner.LowerMemTipActivity";
    private static final String SYSTEMMANAGER_PACKAGENAME = "com.huawei.systemmanager";
    static final String TAG = "HwDeviceStorageMonitorService";

    public HwDeviceStorageMonitorService(Context context) {
        super(context);
    }

    public boolean checkSystemManagerApkExist() {
        try {
            getContext().getPackageManager().getApplicationInfo("com.huawei.systemmanager", 8192);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(TAG, "Hw System Manager not install ,so use original notification");
            return false;
        }
    }

    public void sendNotificationHwSM(long freeMem, boolean isBootImageOnDisk, int notficationId, String tag) {
        int i;
        Context context = getContext();
        Slog.d(TAG, "HW Sending low memory notification, freeMem = " + freeMem);
        Intent lowMemIntent = new Intent().setComponent(new ComponentName("com.huawei.systemmanager", ACTION_INTERNAL_STORAGE_SYSTEMMANAGER));
        lowMemIntent.putExtra("memory", freeMem);
        lowMemIntent.addFlags(268435456);
        NotificationManager mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        CharSequence title = context.getText(33685830);
        if (isBootImageOnDisk) {
            i = 33685831;
        } else {
            i = 17040467;
        }
        CharSequence details = context.getText(i);
        Notification notification = new Notification.Builder(context, SystemNotificationChannels.ALERTS).setSmallIcon(33751439).setLargeIcon(BitmapFactory.decodeResource(context.getResources(), 33751439)).setTicker(title).setColor(context.getColor(17170460)).setContentTitle(title).setContentText(details).setContentIntent(PendingIntent.getActivityAsUser(context, 0, lowMemIntent, 0, null, UserHandle.CURRENT)).setStyle(new Notification.BigTextStyle().bigText(details)).setVisibility(1).setCategory("sys").setExtras(HwNotificationResource.getNotificationThemeData(33751440, -1, 2, 15)).build();
        notification.flags |= 32;
        mNotificationMgr.notifyAsUser(tag, notficationId, notification, UserHandle.ALL);
    }
}
