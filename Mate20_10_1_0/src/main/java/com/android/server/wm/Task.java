package com.android.server.wm;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.freeform.HwFreeFormUtils;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.EventLog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.huawei.android.app.HwActivityManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

/* access modifiers changed from: package-private */
public class Task extends WindowContainer<AppWindowToken> implements ConfigurationContainerListener {
    static final String TAG = "WindowManager";
    private boolean mCanAffectSystemUiFlags = true;
    private boolean mDeferRemoval = false;
    private Dimmer mDimmer = new Dimmer(this);
    private int mDragResizeMode;
    private boolean mDragResizing;
    private int mLastRotationDisplayId = -1;
    private final Rect mOverrideDisplayedBounds = new Rect();
    final Rect mPreparedFrozenBounds = new Rect();
    final Configuration mPreparedFrozenMergedConfig = new Configuration();
    private boolean mPreserveNonFloatingState = false;
    private int mResizeMode;
    private int mRotation;
    TaskStack mStack;
    private boolean mSupportsPictureInPicture;
    private ActivityManager.TaskDescription mTaskDescription;
    final int mTaskId;
    TaskRecord mTaskRecord;
    private final Rect mTmpDimBoundsRect = new Rect();
    private Rect mTmpRect = new Rect();
    private Rect mTmpRect2 = new Rect();
    private Rect mTmpRect3 = new Rect();
    final int mUserId;

    Task(int taskId, TaskStack stack, int userId, WindowManagerService service, int resizeMode, boolean supportsPictureInPicture, ActivityManager.TaskDescription taskDescription, TaskRecord taskRecord) {
        super(service);
        this.mTaskId = taskId;
        this.mStack = stack;
        this.mUserId = userId;
        this.mResizeMode = resizeMode;
        this.mSupportsPictureInPicture = supportsPictureInPicture;
        this.mTaskRecord = taskRecord;
        TaskRecord taskRecord2 = this.mTaskRecord;
        if (taskRecord2 != null) {
            taskRecord2.registerConfigurationChangeListener(this);
        }
        setBounds(getRequestedOverrideBounds());
        this.mTaskDescription = taskDescription;
        setOrientation(-2);
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public DisplayContent getDisplayContent() {
        TaskStack taskStack = this.mStack;
        if (taskStack != null) {
            return taskStack.getDisplayContent();
        }
        return null;
    }

    private int getAdjustedAddPosition(int suggestedPosition) {
        int size = this.mChildren.size();
        if (suggestedPosition >= size) {
            return Math.min(size, suggestedPosition);
        }
        int pos = 0;
        while (pos < size && pos < suggestedPosition) {
            if (((AppWindowToken) this.mChildren.get(pos)).removed) {
                suggestedPosition++;
            }
            pos++;
        }
        return Math.min(size, suggestedPosition);
    }

    /* access modifiers changed from: package-private */
    public void addChild(AppWindowToken wtoken, int position) {
        super.addChild((WindowContainer) wtoken, getAdjustedAddPosition(position));
        this.mDeferRemoval = false;
    }

    /* access modifiers changed from: package-private */
    public void positionChildAt(int position, AppWindowToken child, boolean includingParents) {
        super.positionChildAt(getAdjustedAddPosition(position), (WindowContainer) child, includingParents);
        this.mDeferRemoval = false;
    }

    private boolean hasWindowsAlive() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((AppWindowToken) this.mChildren.get(i)).hasWindowsAlive()) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean shouldDeferRemoval() {
        return hasWindowsAlive() && this.mStack.isSelfOrChildAnimating();
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeIfPossible() {
        if (shouldDeferRemoval()) {
            this.mDeferRemoval = true;
        } else {
            removeImmediately();
        }
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeImmediately() {
        EventLog.writeEvent(31003, Integer.valueOf(this.mTaskId), "removeTask");
        this.mDeferRemoval = false;
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord != null) {
            taskRecord.unregisterConfigurationChangeListener(this);
        }
        super.removeImmediately();
    }

    /* access modifiers changed from: package-private */
    public void reparent(TaskStack stack, int position, boolean moveParents) {
        if (stack == this.mStack) {
            throw new IllegalArgumentException("task=" + this + " already child of stack=" + this.mStack);
        } else if (stack != null) {
            EventLog.writeEvent(31003, Integer.valueOf(this.mTaskId), "reParentTask");
            DisplayContent prevDisplayContent = getDisplayContent();
            if (stack.inPinnedWindowingMode()) {
                this.mPreserveNonFloatingState = true;
            } else {
                this.mPreserveNonFloatingState = false;
            }
            getParent().removeChild(this);
            stack.addTask(this, position, showForAllUsers(), moveParents);
            Slog.i(TAG, "reParentTask: removing taskId=" + this.mTaskId + " from stack=" + this.mStack + " to stack=" + stack);
            DisplayContent displayContent = stack.getDisplayContent();
            displayContent.setLayoutNeeded();
            if (prevDisplayContent != displayContent) {
                onDisplayChanged(displayContent);
                prevDisplayContent.setLayoutNeeded();
            }
            getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
        } else {
            throw new IllegalArgumentException("reparent: could not find stack.");
        }
    }

    /* access modifiers changed from: package-private */
    public void positionAt(int position) {
        this.mStack.positionChildAt(position, this, false);
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onParentChanged() {
        super.onParentChanged();
        adjustBoundsForDisplayChangeIfNeeded(getDisplayContent());
        if (getWindowConfiguration().windowsAreScaleable()) {
            forceWindowsScaleable(true);
        } else {
            forceWindowsScaleable(false);
        }
    }

    /* access modifiers changed from: package-private */
    public void removeChild(AppWindowToken token) {
        if (!this.mChildren.contains(token)) {
            Slog.e(TAG, "removeChild: token=" + this + " not found.");
            return;
        }
        super.removeChild((WindowContainer) token);
        if (this.mChildren.isEmpty()) {
            EventLog.writeEvent(31003, Integer.valueOf(this.mTaskId), "removeAppToken: last token");
            if (this.mDeferRemoval) {
                removeIfPossible();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setSendingToBottom(boolean toBottom) {
        for (int appTokenNdx = 0; appTokenNdx < this.mChildren.size(); appTokenNdx++) {
            ((AppWindowToken) this.mChildren.get(appTokenNdx)).sendingToBottom = toBottom;
        }
    }

    public int setBounds(Rect bounds, boolean forceResize) {
        int boundsChanged = setBounds(bounds);
        if (!forceResize || (boundsChanged & 2) == 2) {
            return boundsChanged;
        }
        onResize();
        return boundsChanged | 2;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public int setBounds(Rect bounds) {
        int rotation = 0;
        DisplayContent displayContent = this.mStack.getDisplayContent();
        if (displayContent != null) {
            rotation = displayContent.getDisplayInfo().rotation;
        } else if (bounds == null) {
            return super.setBounds(bounds);
        }
        if (equivalentRequestedOverrideBounds(bounds) && this.mRotation == rotation) {
            return super.setBounds(bounds);
        }
        int boundsChange = super.setBounds(bounds);
        this.mRotation = rotation;
        updateSurfacePosition();
        return boundsChange;
    }

    @Override // com.android.server.wm.WindowContainer
    public boolean onDescendantOrientationChanged(IBinder freezeDisplayToken, ConfigurationContainer requestingContainer) {
        if (super.onDescendantOrientationChanged(freezeDisplayToken, requestingContainer)) {
            return true;
        }
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.getParent() == null) {
            return false;
        }
        TaskRecord taskRecord2 = this.mTaskRecord;
        taskRecord2.onConfigurationChanged(taskRecord2.getParent().getConfiguration());
        return true;
    }

    /* access modifiers changed from: package-private */
    public void resize(boolean relayout, boolean forced) {
        if (setBounds(getRequestedOverrideBounds(), forced) != 0 && relayout) {
            getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
        }
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void onDisplayChanged(DisplayContent dc) {
        adjustBoundsForDisplayChangeIfNeeded(dc);
        super.onDisplayChanged(dc);
        this.mWmService.mAtmService.getTaskChangeNotificationController().notifyTaskDisplayChanged(this.mTaskId, dc != null ? dc.getDisplayId() : -1);
    }

    /* access modifiers changed from: package-private */
    public void setOverrideDisplayedBounds(Rect overrideDisplayedBounds) {
        if (overrideDisplayedBounds != null) {
            this.mOverrideDisplayedBounds.set(overrideDisplayedBounds);
        } else {
            this.mOverrideDisplayedBounds.setEmpty();
        }
        updateSurfacePosition();
    }

    /* access modifiers changed from: package-private */
    public Rect getOverrideDisplayedBounds() {
        return this.mOverrideDisplayedBounds;
    }

    /* access modifiers changed from: package-private */
    public void setResizeable(int resizeMode) {
        this.mResizeMode = resizeMode;
    }

    /* access modifiers changed from: package-private */
    public boolean isResizeable() {
        String str;
        IHwActivityTaskManagerServiceEx iHwActivityTaskManagerServiceEx = this.mWmService.mAtmService.mHwATMSEx;
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.realActivity == null) {
            str = null;
        } else {
            str = this.mTaskRecord.realActivity.getPackageName();
        }
        return iHwActivityTaskManagerServiceEx.isResizableApp(str, this.mResizeMode) || this.mSupportsPictureInPicture || this.mWmService.mForceResizableTasks;
    }

    /* access modifiers changed from: package-private */
    public boolean preserveOrientationOnResize() {
        int i = this.mResizeMode;
        return i == 6 || i == 5 || i == 7;
    }

    /* access modifiers changed from: package-private */
    public boolean cropWindowsToStackBounds() {
        return isResizeable();
    }

    /* access modifiers changed from: package-private */
    public void prepareFreezingBounds() {
        this.mPreparedFrozenBounds.set(getBounds());
        this.mPreparedFrozenMergedConfig.setTo(getConfiguration());
    }

    /* access modifiers changed from: package-private */
    public void alignToAdjustedBounds(Rect adjustedBounds, Rect tempInsetBounds, boolean alignBottom) {
        if (isResizeable() && !Configuration.EMPTY.equals(getRequestedOverrideConfiguration())) {
            getBounds(this.mTmpRect2);
            if (alignBottom) {
                this.mTmpRect2.offset(0, adjustedBounds.bottom - this.mTmpRect2.bottom);
            } else {
                this.mTmpRect2.offsetTo(adjustedBounds.left, adjustedBounds.top);
            }
            if (tempInsetBounds == null || tempInsetBounds.isEmpty()) {
                setOverrideDisplayedBounds(null);
                setBounds(this.mTmpRect2);
                return;
            }
            setOverrideDisplayedBounds(this.mTmpRect2);
            setBounds(tempInsetBounds);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    public Rect getDisplayedBounds() {
        if (this.mOverrideDisplayedBounds.isEmpty()) {
            return super.getDisplayedBounds();
        }
        return this.mOverrideDisplayedBounds;
    }

    private boolean getMaxVisibleBounds(Rect out) {
        WindowState win;
        boolean foundTop = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mChildren.get(i);
            if (!token.mIsExiting && !token.isClientHidden() && !token.hiddenRequested && (win = token.findMainWindow()) != null) {
                if (!foundTop) {
                    foundTop = true;
                    out.setEmpty();
                }
                win.getMaxVisibleBounds(out);
            }
        }
        return foundTop;
    }

    public void getDimBounds(Rect out) {
        TaskStack taskStack = this.mStack;
        if (taskStack != null) {
            DisplayContent displayContent = taskStack.getDisplayContent();
            boolean dockedResizing = displayContent != null && displayContent.mDividerControllerLocked.isResizing();
            if ((HwPCUtils.isExtDynamicStack(this.mStack.mStackId) && matchParentBounds()) || ((!inFreeformWindowingMode() && !inHwPCFreeformWindowingMode() && !inHwFreeFormWindowingMode()) || !getMaxVisibleBounds(out))) {
                if (matchParentBounds()) {
                    out.set(getBounds());
                } else if (dockedResizing) {
                    this.mStack.getBounds(out);
                } else {
                    this.mStack.getBounds(this.mTmpRect);
                    this.mTmpRect.intersect(getBounds());
                    out.set(this.mTmpRect);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setDragResizing(boolean dragResizing, int dragResizeMode) {
        if (this.mDragResizing == dragResizing) {
            return;
        }
        if (!dragResizing || DragResizeMode.isModeAllowedForStack(this.mStack, dragResizeMode)) {
            this.mDragResizing = dragResizing;
            this.mDragResizeMode = dragResizeMode;
            resetDragResizingChangeReported();
            return;
        }
        throw new IllegalArgumentException("Drag resize mode not allow for stack stackId=" + this.mStack.mStackId + " dragResizeMode=" + dragResizeMode + " mode=" + this.mStack.getWindowingMode());
    }

    /* access modifiers changed from: package-private */
    public boolean isDragResizing() {
        return this.mDragResizing;
    }

    /* access modifiers changed from: package-private */
    public int getDragResizeMode() {
        return this.mDragResizeMode;
    }

    public void setTaskDockedResizing(boolean resizing) {
        setDragResizing(resizing, 1);
    }

    private void adjustBoundsForDisplayChangeIfNeeded(DisplayContent displayContent) {
        TaskRecord taskRecord;
        if (displayContent != null) {
            if (matchParentBounds()) {
                setBounds(null);
                return;
            }
            int displayId = displayContent.getDisplayId();
            int newRotation = displayContent.getDisplayInfo().rotation;
            if (displayId != this.mLastRotationDisplayId) {
                this.mLastRotationDisplayId = displayId;
                this.mRotation = newRotation;
            } else if (this.mRotation != newRotation) {
                this.mTmpRect2.set(getBounds());
                if (!getWindowConfiguration().canResizeTask()) {
                    setBounds(this.mTmpRect2);
                    return;
                }
                displayContent.rotateBounds(this.mRotation, newRotation, this.mTmpRect2);
                if (HwFreeFormUtils.isFreeFormEnable() && inFreeformWindowingMode() && this.mTmpRect2.top < 0) {
                    Rect rect = this.mTmpRect2;
                    rect.offsetTo(rect.left, this.mWmService.mContext.getResources().getDimensionPixelSize(17105443));
                    HwFreeFormUtils.log(TAG, "avoid bounds conflict freeform offset to " + this.mTmpRect2);
                }
                if (setBounds(this.mTmpRect2) != 0 && (taskRecord = this.mTaskRecord) != null) {
                    taskRecord.requestResize(getBounds(), 1);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void cancelTaskWindowTransition() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((AppWindowToken) this.mChildren.get(i)).cancelAnimation();
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isSamePackageInTask() {
        int uid = -1;
        boolean ret = true;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken tmpToken = (AppWindowToken) this.mChildren.get(i);
            int i2 = -1;
            if (uid == -1) {
                if (!tmpToken.isEmpty()) {
                    i2 = ((WindowState) tmpToken.mChildren.peekLast()).mOwnerUid;
                }
                uid = i2;
            } else {
                if (!tmpToken.isEmpty()) {
                    i2 = ((WindowState) tmpToken.mChildren.peekLast()).mOwnerUid;
                }
                ret = uid == i2;
            }
            if (!ret) {
                return ret;
            }
        }
        return ret;
    }

    /* access modifiers changed from: package-private */
    public boolean showForAllUsers() {
        int tokensCount = this.mChildren.size();
        return tokensCount != 0 && ((AppWindowToken) this.mChildren.get(tokensCount + -1)).mShowForAllUsers;
    }

    /* access modifiers changed from: package-private */
    public boolean isFloating() {
        return getWindowConfiguration().tasksAreFloating() && !this.mStack.isAnimatingBoundsToFullscreen() && !this.mPreserveNonFloatingState;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable, com.android.server.wm.WindowContainer
    public SurfaceControl getAnimationLeashParent() {
        return getAppAnimationLayer(2);
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public SurfaceControl.Builder makeSurface() {
        return super.makeSurface().setMetadata(3, this.mTaskId);
    }

    /* access modifiers changed from: package-private */
    public boolean isTaskAnimating() {
        RecentsAnimationController recentsAnim = this.mWmService.getRecentsAnimationController();
        if (recentsAnim == null || !recentsAnim.isAnimatingTask(this)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public WindowState getTopVisibleAppMainWindow() {
        AppWindowToken token = getTopVisibleAppToken();
        if (token != null) {
            return token.findMainWindow();
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public AppWindowToken getTopFullscreenAppToken() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mChildren.get(i);
            WindowState win = token.findMainWindow();
            if (win != null && win.mAttrs.isFullscreen()) {
                return token;
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public AppWindowToken getTopVisibleAppToken() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mChildren.get(i);
            if (!token.mIsExiting && !token.isClientHidden() && !token.hiddenRequested) {
                return token;
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public void positionChildAtTop(AppWindowToken aToken) {
        positionChildAt(aToken, Integer.MAX_VALUE);
    }

    /* access modifiers changed from: package-private */
    public void positionChildAt(AppWindowToken aToken, int position) {
        if (aToken == null) {
            Slog.w(TAG, "Attempted to position of non-existing app");
        } else {
            positionChildAt(position, aToken, false);
        }
    }

    /* access modifiers changed from: package-private */
    public void forceWindowsScaleable(boolean force) {
        this.mWmService.openSurfaceTransaction();
        try {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                ((AppWindowToken) this.mChildren.get(i)).forceWindowsScaleableInTransaction(force);
            }
        } finally {
            this.mWmService.closeSurfaceTransaction("forceWindowsScaleable");
        }
    }

    /* access modifiers changed from: package-private */
    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        this.mTaskDescription = taskDescription;
    }

    /* access modifiers changed from: package-private */
    public void onSnapshotChanged(ActivityManager.TaskSnapshot snapshot) {
        this.mTaskRecord.onSnapshotChanged(snapshot);
    }

    /* access modifiers changed from: package-private */
    public ActivityManager.TaskDescription getTaskDescription() {
        return this.mTaskDescription;
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean fillsParent() {
        return matchParentBounds() || !getWindowConfiguration().canResizeTask();
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void forAllTasks(Consumer<Task> callback) {
        callback.accept(this);
    }

    /* access modifiers changed from: package-private */
    public void setCanAffectSystemUiFlags(boolean canAffectSystemUiFlags) {
        this.mCanAffectSystemUiFlags = canAffectSystemUiFlags;
    }

    /* access modifiers changed from: package-private */
    public boolean canAffectSystemUiFlags() {
        return this.mCanAffectSystemUiFlags;
    }

    /* access modifiers changed from: package-private */
    public void dontAnimateDimExit() {
        this.mDimmer.dontAnimateExit();
    }

    public String toString() {
        return "{taskId=" + this.mTaskId + " appTokens=" + this.mChildren + " mdr=" + this.mDeferRemoval + "}";
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public String getName() {
        return toShortString();
    }

    /* access modifiers changed from: package-private */
    public void clearPreserveNonFloatingState() {
        this.mPreserveNonFloatingState = false;
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public Dimmer getDimmer() {
        return this.mDimmer;
    }

    private void fixDimBoundsFreeform() {
        boolean isChildrenExist = true;
        if (!((getDisplayContent() == null || getDisplayContent().mInputMethodTarget == null || !hasChild(getDisplayContent().mInputMethodTarget.mAppToken)) ? false : true)) {
            this.mTmpDimBoundsRect.offsetTo(0, 0);
            return;
        }
        AppWindowToken appWindowToken = getTopVisibleAppToken();
        if (appWindowToken == null || appWindowToken.getTopChild() == null) {
            isChildrenExist = false;
        }
        if (!isChildrenExist) {
            this.mTmpDimBoundsRect.offsetTo(0, 0);
            return;
        }
        Rect outerBound = new Rect(getBounds());
        Iterator it = appWindowToken.mChildren.iterator();
        while (it.hasNext()) {
            Rect targetFrame = ((WindowState) it.next()).mWindowFrames.mFrame;
            outerBound.left = Math.min(outerBound.left, targetFrame.left);
            outerBound.top = Math.min(outerBound.top, targetFrame.top);
        }
        this.mTmpDimBoundsRect.offsetTo(outerBound.left - getBounds().left, outerBound.top - getBounds().top);
    }

    /* access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void prepareSurfaces() {
        this.mDimmer.resetDimStates();
        super.prepareSurfaces();
        getDimBounds(this.mTmpDimBoundsRect);
        if (inHwFreeFormWindowingMode()) {
            fixDimBoundsFreeform();
        } else {
            this.mTmpDimBoundsRect.offsetTo(0, 0);
        }
        if (this.mDimmer.updateDims(getPendingTransaction(), this.mTmpDimBoundsRect)) {
            if (this.mStack.mActivityStack.inHwFreeFormWindowingMode()) {
                this.mDimmer.updateDimsCornerRadius(getPendingTransaction(), (float) this.mStack.mActivityStack.mService.mContext.getResources().getDimensionPixelSize(34472524));
            }
            if (this.mStack.mActivityStack.inHwSplitScreenWindowingMode()) {
                this.mDimmer.updateDimsCornerRadius(getPendingTransaction(), HwActivityManager.IS_PHONE ? 0.0f : (float) this.mStack.mActivityStack.mService.mContext.getResources().getDimensionPixelSize(34472527));
            }
            scheduleAnimation();
        }
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void writeToProto(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel != 2 || isVisible()) {
            long token = proto.start(fieldId);
            super.writeToProto(proto, 1146756268033L, logLevel);
            proto.write(1120986464258L, this.mTaskId);
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                ((AppWindowToken) this.mChildren.get(i)).writeToProto(proto, 2246267895811L, logLevel);
            }
            proto.write(1133871366148L, matchParentBounds());
            getBounds().writeToProto(proto, 1146756268037L);
            this.mOverrideDisplayedBounds.writeToProto(proto, 1146756268038L);
            proto.write(1133871366151L, this.mDeferRemoval);
            proto.write(1120986464264L, this.mSurfaceControl.getWidth());
            proto.write(1120986464265L, this.mSurfaceControl.getHeight());
            proto.end(token);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        String doublePrefix = prefix + "  ";
        pw.println(prefix + "taskId=" + this.mTaskId);
        pw.println(doublePrefix + "mBounds=" + getBounds().toShortString());
        pw.println(doublePrefix + "mdr=" + this.mDeferRemoval);
        pw.println(doublePrefix + "appTokens=" + this.mChildren);
        pw.println(doublePrefix + "mDisplayedBounds=" + this.mOverrideDisplayedBounds.toShortString());
        String triplePrefix = doublePrefix + "  ";
        String quadruplePrefix = triplePrefix + "  ";
        for (int i = this.mChildren.size() - 1; i >= 0; i += -1) {
            AppWindowToken wtoken = (AppWindowToken) this.mChildren.get(i);
            pw.println(triplePrefix + "Activity #" + i + " " + wtoken);
            wtoken.dump(pw, quadruplePrefix, dumpAll);
        }
    }

    /* access modifiers changed from: package-private */
    public String toShortString() {
        return "Task=" + this.mTaskId;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.wm.WindowContainer
    public void onAnimationFinished() {
        super.onAnimationFinished();
        Slog.i(TAG, "onAnimationFinished task=" + this.mTaskId);
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                    new ArrayList<>(((AppWindowToken) this.mChildren.get(i)).mChildren).forEach($$Lambda$01bPtngJg5AqEoOWfW3rWfV7MH4.INSTANCE);
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
