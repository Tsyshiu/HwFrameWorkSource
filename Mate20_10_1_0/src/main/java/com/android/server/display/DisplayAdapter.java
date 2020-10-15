package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.view.Display;
import com.android.server.display.DisplayManagerService;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

abstract class DisplayAdapter {
    public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
    public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
    public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;
    private static final AtomicInteger NEXT_DISPLAY_MODE_ID = new AtomicInteger(1);
    private final Context mContext;
    private final Handler mHandler;
    /* access modifiers changed from: private */
    public final Listener mListener;
    private final String mName;
    private final DisplayManagerService.SyncRoot mSyncRoot;

    public interface Listener {
        void onDisplayDeviceEvent(DisplayDevice displayDevice, int i);

        void onTraversalRequested();
    }

    public DisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, Listener listener, String name) {
        this.mSyncRoot = syncRoot;
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mName = name;
    }

    public final DisplayManagerService.SyncRoot getSyncRoot() {
        return this.mSyncRoot;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final Handler getHandler() {
        return this.mHandler;
    }

    public final String getName() {
        return this.mName;
    }

    public void registerLocked() {
    }

    public void dumpLocked(PrintWriter pw) {
    }

    /* access modifiers changed from: protected */
    public final void sendDisplayDeviceEventLocked(final DisplayDevice device, final int event) {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.display.DisplayAdapter.AnonymousClass1 */

            public void run() {
                DisplayAdapter.this.mListener.onDisplayDeviceEvent(device, event);
            }
        });
    }

    /* access modifiers changed from: protected */
    public final void sendTraversalRequestLocked() {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.display.DisplayAdapter.AnonymousClass2 */

            public void run() {
                DisplayAdapter.this.mListener.onTraversalRequested();
            }
        });
    }

    public static Display.Mode createMode(int width, int height, float refreshRate) {
        return new Display.Mode(NEXT_DISPLAY_MODE_ID.getAndIncrement(), width, height, refreshRate);
    }
}
