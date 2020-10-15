package com.android.server.wm;

import android.app.IActivityTaskManager;
import android.freeform.HwFreeFormUtils;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import com.android.internal.annotations.GuardedBy;
import com.android.server.input.InputManagerService;

class TaskPositioningController {
    private final IActivityTaskManager mActivityManager;
    private final Handler mHandler;
    private final InputManagerService mInputManager;
    private SurfaceControl mInputSurface;
    private DisplayContent mPositioningDisplay;
    private final WindowManagerService mService;
    @GuardedBy({"WindowManagerSerivce.mWindowMap"})
    private TaskPositioner mTaskPositioner;
    private final Rect mTmpClipRect = new Rect();
    private IBinder mTransferTouchFromToken;

    /* access modifiers changed from: package-private */
    public boolean isPositioningLocked() {
        return this.mTaskPositioner != null;
    }

    /* access modifiers changed from: package-private */
    public InputWindowHandle getDragWindowHandleLocked() {
        TaskPositioner taskPositioner = this.mTaskPositioner;
        if (taskPositioner != null) {
            return taskPositioner.mDragWindowHandle;
        }
        return null;
    }

    TaskPositioningController(WindowManagerService service, InputManagerService inputManager, IActivityTaskManager activityManager, Looper looper) {
        this.mService = service;
        this.mInputManager = inputManager;
        this.mActivityManager = activityManager;
        this.mHandler = new Handler(looper);
    }

    /* access modifiers changed from: package-private */
    public void hideInputSurface(SurfaceControl.Transaction t, int displayId) {
        SurfaceControl surfaceControl;
        DisplayContent displayContent = this.mPositioningDisplay;
        if (displayContent != null && displayContent.getDisplayId() == displayId && (surfaceControl = this.mInputSurface) != null) {
            t.hide(surfaceControl);
        }
    }

    /* access modifiers changed from: package-private */
    public void showInputSurface(SurfaceControl.Transaction t, int displayId) {
        DisplayContent displayContent = this.mPositioningDisplay;
        if (displayContent != null && displayContent.getDisplayId() == displayId) {
            DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
            if (this.mInputSurface == null) {
                this.mInputSurface = this.mService.makeSurfaceBuilder(dc.getSession()).setContainerLayer().setName("Drag and Drop Input Consumer").build();
            }
            InputWindowHandle h = getDragWindowHandleLocked();
            if (h == null) {
                Slog.w("WindowManager", "Drag is in progress but there is no drag window handle.");
                return;
            }
            t.show(this.mInputSurface);
            t.setInputWindowInfo(this.mInputSurface, h);
            t.setLayer(this.mInputSurface, Integer.MAX_VALUE);
            Display display = dc.getDisplay();
            Point p = new Point();
            display.getRealSize(p);
            this.mTmpClipRect.set(0, 0, p.x, p.y);
            t.setWindowCrop(this.mInputSurface, this.mTmpClipRect);
            t.transferTouchFocus(this.mTransferTouchFromToken, h.token);
            this.mTransferTouchFromToken = null;
            if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
                t.apply(true);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean startMovingTask(IWindow window, float startX, float startY) {
        Throwable th;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState win = this.mService.windowForClientLocked((Session) null, window, false);
                try {
                    if (!startPositioningLocked(win, false, false, startX, startY)) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    try {
                        this.mActivityManager.setFocusedTask(win.getTask().mTaskId);
                        return true;
                    } catch (RemoteException e) {
                        return true;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void handleTapOutsideTask(DisplayContent displayContent, int x, int y) {
        this.mHandler.post(new Runnable(x, y, displayContent) {
            /* class com.android.server.wm.$$Lambda$TaskPositioningController$WvS6bGwsoNKniWwQXf4LtUhPblY */
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ DisplayContent f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                TaskPositioningController.this.lambda$handleTapOutsideTask$0$TaskPositioningController(this.f$1, this.f$2, this.f$3);
            }
        });
    }

    public /* synthetic */ void lambda$handleTapOutsideTask$0$TaskPositioningController(int x, int y, DisplayContent displayContent) {
        if (HwPCUtils.isPcCastModeInServer()) {
            if (x == -1 && y == -1) {
                this.mService.setFocusedDisplay(displayContent.getDisplayId(), true, "handleTapOutsideTask-1-1");
                return;
            }
            this.mService.setFocusedDisplay(displayContent.getDisplayId(), false, "handleTapOutsideTaskXY");
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = displayContent.findTaskForResizePoint(x, y);
                if (task != null) {
                    if (startPositioningLocked(task.getTopVisibleAppMainWindow(), true, task.preserveOrientationOnResize(), (float) x, (float) y)) {
                        try {
                            this.mActivityManager.setFocusedTask(task.mTaskId);
                        } catch (RemoteException e) {
                        }
                    } else {
                        return;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private boolean startPositioningLocked(WindowState win, boolean resize, boolean preserveOrientation, float startX, float startY) {
        WindowState transferFocusFromWin;
        if (win == null || win.getAppToken() == null) {
            Slog.w("WindowManager", "startPositioningLocked: Bad window " + win);
            return false;
        } else if (win.mInputChannel == null) {
            Slog.wtf("WindowManager", "startPositioningLocked: " + win + " has no input channel,  probably being removed");
            return false;
        } else if (this.mTaskPositioner != null) {
            if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(win.getDisplayId())) {
                HwPCUtils.log("WindowManager", "cleanUpTaskPositioner");
                cleanUpTaskPositioner();
            }
            Slog.w("WindowManager", "Previous TaskPositioner exist " + this.mTaskPositioner);
            return false;
        } else {
            if (!((!HwFreeFormUtils.isFreeFormEnable() || win.getTask() == null || win.getTask().getTopVisibleAppToken() == null) ? false : true) || !"com.android.packageinstaller".equals(win.getTask().getTopVisibleAppToken().appPackageName)) {
                DisplayContent displayContent = win.getDisplayContent();
                if (displayContent == null) {
                    Slog.w("WindowManager", "startPositioningLocked: Invalid display content " + win);
                    return false;
                }
                this.mPositioningDisplay = displayContent;
                this.mTaskPositioner = TaskPositioner.create(this.mService);
                if (displayContent.mCurrentFocus == null || displayContent.mCurrentFocus == win || displayContent.mCurrentFocus.mAppToken != win.mAppToken) {
                    transferFocusFromWin = win;
                } else {
                    transferFocusFromWin = displayContent.mCurrentFocus;
                }
                this.mTransferTouchFromToken = transferFocusFromWin.mInputChannel.getToken();
                this.mTaskPositioner.register(displayContent);
                this.mTaskPositioner.startDrag(win, resize, preserveOrientation, startX, startY);
                return true;
            }
            HwFreeFormUtils.log("WindowManager", "Check permission do not resize freeform.");
            return false;
        }
    }

    public void finishTaskPositioning(IWindow window) {
        TaskPositioner taskPositioner = this.mTaskPositioner;
        if (taskPositioner != null && taskPositioner.mClientCallback == window.asBinder()) {
            finishTaskPositioning();
        }
    }

    /* access modifiers changed from: package-private */
    public void finishTaskPositioning() {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.wm.$$Lambda$TaskPositioningController$z3n1stJjOdhDbXXrvPlvlqmON6k */

            public final void run() {
                TaskPositioningController.this.lambda$finishTaskPositioning$1$TaskPositioningController();
            }
        });
    }

    public /* synthetic */ void lambda$finishTaskPositioning$1$TaskPositioningController() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                cleanUpTaskPositioner();
                this.mPositioningDisplay = null;
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void cleanUpTaskPositioner() {
        TaskPositioner positioner = this.mTaskPositioner;
        if (positioner != null) {
            this.mTaskPositioner = null;
            positioner.unregister();
        }
    }
}
