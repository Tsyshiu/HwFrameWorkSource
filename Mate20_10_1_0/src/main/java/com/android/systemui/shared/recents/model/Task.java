package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewDebug;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class Task {
    public static final String TAG = "Task";
    @ViewDebug.ExportedProperty(category = "recents")
    public int colorBackground;
    @ViewDebug.ExportedProperty(category = "recents")
    public int colorPrimary;
    public Drawable icon;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isDockable;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isHwTaskLocked;
    @ViewDebug.ExportedProperty(category = "recents")
    @Deprecated
    public boolean isLaunchTarget;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isLocked;
    @ViewDebug.ExportedProperty(category = "recents")
    @Deprecated
    public boolean isStackTask;
    @ViewDebug.ExportedProperty(category = "recents")
    @Deprecated
    public boolean isSystemApp;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "key_")
    public TaskKey key;
    @Deprecated
    private ArrayList<TaskCallbacks> mCallbacks = new ArrayList<>();
    @ViewDebug.ExportedProperty(category = "recents")
    public String packageName;
    @ViewDebug.ExportedProperty(category = "recents")
    @Deprecated
    public int resizeMode;
    public ActivityManager.TaskDescription taskDescription;
    @Deprecated
    public int temporarySortIndexInStack;
    public ThumbnailData thumbnail;
    @ViewDebug.ExportedProperty(category = "recents")
    @Deprecated
    public String title;
    @ViewDebug.ExportedProperty(category = "recents")
    public String titleDescription;
    @ViewDebug.ExportedProperty(category = "recents")
    public ComponentName topActivity;
    @ViewDebug.ExportedProperty(category = "recents")
    @Deprecated
    public boolean useLightOnPrimaryColor;

    @Deprecated
    public interface TaskCallbacks {
        void onTaskDataLoaded(Task task, ThumbnailData thumbnailData);

        void onTaskDataUnloaded();

        void onTaskWindowingModeChanged();
    }

    public static class TaskKey {
        @ViewDebug.ExportedProperty(category = "recents")
        public final Intent baseIntent;
        public Rect bounds;
        public int[] combinedTaskIds;
        @ViewDebug.ExportedProperty(category = "recents")
        public final int displayId;
        @ViewDebug.ExportedProperty(category = "recents")
        public final int id;
        @ViewDebug.ExportedProperty(category = "recents")
        public long lastActiveTime;
        private int mHashCode;
        public final ComponentName sourceComponent;
        @ViewDebug.ExportedProperty(category = "recents")
        public final int userId;
        @ViewDebug.ExportedProperty(category = "recents")
        public int windowingMode;

        public TaskKey(ActivityManager.RecentTaskInfo t) {
            ComponentName sourceComponent2;
            if (t.origActivity != null) {
                sourceComponent2 = t.origActivity;
            } else {
                sourceComponent2 = t.realActivity;
            }
            this.id = t.taskId;
            this.windowingMode = t.configuration.windowConfiguration.getWindowingMode();
            this.baseIntent = t.baseIntent;
            this.sourceComponent = sourceComponent2;
            this.userId = t.userId;
            this.lastActiveTime = t.lastActiveTime;
            this.displayId = t.displayId;
            this.combinedTaskIds = t.combinedTaskIds;
            this.bounds = t.bounds;
            updateHashCode();
        }

        public TaskKey(int id2, int windowingMode2, Intent intent, ComponentName sourceComponent2, int userId2, long lastActiveTime2) {
            this.id = id2;
            this.windowingMode = windowingMode2;
            this.baseIntent = intent;
            this.sourceComponent = sourceComponent2;
            this.userId = userId2;
            this.lastActiveTime = lastActiveTime2;
            this.displayId = 0;
            this.combinedTaskIds = null;
            this.bounds = null;
            updateHashCode();
        }

        public TaskKey(int id2, int windowingMode2, Intent intent, ComponentName sourceComponent2, int userId2, long lastActiveTime2, int displayId2) {
            this.id = id2;
            this.windowingMode = windowingMode2;
            this.baseIntent = intent;
            this.sourceComponent = sourceComponent2;
            this.userId = userId2;
            this.lastActiveTime = lastActiveTime2;
            this.displayId = displayId2;
            this.combinedTaskIds = null;
            this.bounds = null;
            updateHashCode();
        }

        public void setWindowingMode(int windowingMode2) {
            this.windowingMode = windowingMode2;
            updateHashCode();
        }

        public ComponentName getComponent() {
            return this.baseIntent.getComponent();
        }

        public String getPackageName() {
            if (this.baseIntent.getComponent() != null) {
                return this.baseIntent.getComponent().getPackageName();
            }
            return this.baseIntent.getPackage();
        }

        public boolean equals(Object o) {
            if (!(o instanceof TaskKey)) {
                return false;
            }
            TaskKey otherKey = (TaskKey) o;
            if (this.id == otherKey.id && this.windowingMode == otherKey.windowingMode && this.userId == otherKey.userId) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return this.mHashCode;
        }

        public String toString() {
            return "id=" + this.id + " windowingMode=" + this.windowingMode + " user=" + this.userId + " lastActiveTime=" + this.lastActiveTime;
        }

        private void updateHashCode() {
            this.mHashCode = Objects.hash(Integer.valueOf(this.id), Integer.valueOf(this.windowingMode), Integer.valueOf(this.userId));
        }
    }

    public Task() {
    }

    public Task(TaskKey key2) {
        this.key = key2;
        this.taskDescription = new ActivityManager.TaskDescription();
    }

    public Task(TaskKey key2, int colorPrimary2, int colorBackground2, boolean isDockable2, boolean isLocked2, ActivityManager.TaskDescription taskDescription2, ComponentName topActivity2) {
        this.key = key2;
        this.colorPrimary = colorPrimary2;
        this.colorBackground = colorBackground2;
        this.taskDescription = taskDescription2;
        this.isDockable = isDockable2;
        this.isLocked = isLocked2;
        this.topActivity = topActivity2;
    }

    @Deprecated
    public Task(TaskKey key2, Drawable icon2, ThumbnailData thumbnail2, String title2, String titleDescription2, int colorPrimary2, int colorBackground2, boolean isLaunchTarget2, boolean isStackTask2, boolean isSystemApp2, boolean isDockable2, ActivityManager.TaskDescription taskDescription2, int resizeMode2, ComponentName topActivity2, boolean isLocked2) {
        this.key = key2;
        this.icon = icon2;
        this.thumbnail = thumbnail2;
        this.title = title2;
        this.titleDescription = titleDescription2;
        this.colorPrimary = colorPrimary2;
        this.colorBackground = colorBackground2;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary, -1) > 3.0f;
        this.taskDescription = taskDescription2;
        this.isLaunchTarget = isLaunchTarget2;
        this.isStackTask = isStackTask2;
        this.isSystemApp = isSystemApp2;
        this.isDockable = isDockable2;
        this.resizeMode = resizeMode2;
        this.topActivity = topActivity2;
        this.isLocked = isLocked2;
    }

    @Deprecated
    public void copyFrom(Task o) {
        this.key = o.key;
        this.icon = o.icon;
        this.thumbnail = o.thumbnail;
        this.title = o.title;
        this.titleDescription = o.titleDescription;
        this.colorPrimary = o.colorPrimary;
        this.colorBackground = o.colorBackground;
        this.useLightOnPrimaryColor = o.useLightOnPrimaryColor;
        this.taskDescription = o.taskDescription;
        this.isLaunchTarget = o.isLaunchTarget;
        this.isStackTask = o.isStackTask;
        this.isSystemApp = o.isSystemApp;
        this.isDockable = o.isDockable;
        this.resizeMode = o.resizeMode;
        this.isLocked = o.isLocked;
        this.topActivity = o.topActivity;
        this.packageName = o.packageName;
        this.isHwTaskLocked = o.isHwTaskLocked;
    }

    @Deprecated
    public void addCallback(TaskCallbacks cb) {
        if (!this.mCallbacks.contains(cb)) {
            this.mCallbacks.add(cb);
        }
    }

    @Deprecated
    public void removeCallback(TaskCallbacks cb) {
        this.mCallbacks.remove(cb);
    }

    @Deprecated
    public void setWindowingMode(int windowingMode) {
        this.key.setWindowingMode(windowingMode);
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks.get(i).onTaskWindowingModeChanged();
        }
    }

    @Deprecated
    public void notifyTaskDataLoaded(ThumbnailData thumbnailData, Drawable applicationIcon) {
        this.icon = applicationIcon;
        this.thumbnail = thumbnailData;
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks.get(i).onTaskDataLoaded(this, thumbnailData);
        }
    }

    @Deprecated
    public void notifyTaskDataUnloaded(Drawable defaultApplicationIcon) {
        this.icon = defaultApplicationIcon;
        this.thumbnail = null;
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            this.mCallbacks.get(i).onTaskDataUnloaded();
        }
    }

    public ComponentName getTopComponent() {
        ComponentName componentName = this.topActivity;
        if (componentName != null) {
            return componentName;
        }
        return this.key.baseIntent.getComponent();
    }

    public boolean equals(Object o) {
        TaskKey taskKey;
        if (o == null || !(o instanceof Task) || (taskKey = ((Task) o).key) == null) {
            return false;
        }
        return this.key.equals(taskKey);
    }

    public String toString() {
        return "[" + this.key.toString() + "] " + this.title;
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.print(prefix);
        writer.print(this.key);
        if (!this.isDockable) {
            writer.print(" dockable=N");
        }
        if (this.isLaunchTarget) {
            writer.print(" launchTarget=Y");
        }
        if (this.isLocked) {
            writer.print(" locked=Y");
        }
        writer.print(" ");
        writer.print(this.title);
        writer.println();
    }

    public void setPakcageName(String packageName2, boolean isHwTaskLocked2) {
        this.packageName = packageName2;
        this.isHwTaskLocked = isHwTaskLocked2;
    }

    public void recycle() {
        Bitmap b;
        ThumbnailData thumbnailData = this.thumbnail;
        if (thumbnailData != null && (b = thumbnailData.thumbnail) != null && !b.isRecycled()) {
            b.recycle();
            this.thumbnail = null;
            Log.i(TAG, "Task recycle " + this.key);
        }
    }
}
