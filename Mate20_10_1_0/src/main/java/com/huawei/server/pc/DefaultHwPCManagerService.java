package com.huawei.server.pc;

import android.app.HwRecentTaskInfo;
import android.app.ITaskStackListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.pc.IHwPCManager;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.PointerIcon;
import com.android.server.am.ActivityManagerServiceEx;
import java.util.List;

public class DefaultHwPCManagerService extends IHwPCManager.Stub {
    public DefaultHwPCManagerService(Context context, ActivityManagerServiceEx ams) {
    }

    public void scheduleDisplayAdded(int displayId) {
    }

    public void scheduleDisplayChanged(int displayId) {
    }

    public void scheduleDisplayRemoved(int displayId) {
    }

    public boolean getCastMode() {
        return false;
    }

    public boolean isHiCarCastModeForClient() {
        return false;
    }

    public int getPackageSupportPcState(String packageName) {
        return -1;
    }

    public boolean checkPermissionForHwMultiDisplay(int uid) {
        return false;
    }

    public List<String> getAllSupportPcAppList() {
        return null;
    }

    public void relaunchIMEIfNecessary() {
    }

    public void hwRestoreTask(int taskId, float x, float y) {
    }

    public void hwResizeTask(int taskId, Rect bounds) {
    }

    public int getWindowState(IBinder token) {
        return 0;
    }

    public HwRecentTaskInfo getHwRecentTaskInfo(int taskId) {
        return null;
    }

    public void registerHwTaskStackListener(ITaskStackListener listener) {
    }

    public void unRegisterHwTaskStackListener(ITaskStackListener listener) {
    }

    public Bitmap getDisplayBitmap(int displayId, int width, int height) {
        return null;
    }

    public void registHwSystemUIController(Messenger messenger) {
    }

    public void showTopBar() {
    }

    public void showStartMenu() {
    }

    public void screenshotPc() {
    }

    public void closeTopWindow() {
    }

    public void triggerRecentTaskSplitView(int side, int triggerTaskId) {
    }

    public void triggerSplitWindowPreviewLayer(int side, int action) {
    }

    public void triggerSwitchTaskView(boolean show) {
    }

    public void toggleHome() {
    }

    public boolean injectInputEventExternal(InputEvent event, int mode) {
        return false;
    }

    public float[] getPointerCoordinateAxis() {
        return new float[2];
    }

    public Bitmap getTaskThumbnailEx(int id) {
        return null;
    }

    public void onTaskMovedToBack(int taskId) {
    }

    public void onTaskMovedToFront(int taskId) {
    }

    public void saveNeedRestartAppIntent(List<Intent> list) {
    }

    public boolean isDisallowLockScreenForHwMultiDisplay() {
        return false;
    }

    public boolean isInWindowsCastMode() {
        return false;
    }

    public boolean isInSinkWindowsCastMode() {
        return false;
    }

    public void setIsInSinkWindowsCastMode(boolean isInCastMode) {
    }

    public boolean isSinkHasKeyboard() {
        return false;
    }

    public void setIsSinkHasKeyboard(boolean isKeyboardExist) {
    }

    public int getFocusedDisplayId() {
        return 0;
    }

    public boolean isFocusedOnWindowsCastDisplay() {
        return false;
    }

    public void userActivityOnDesktop() {
    }

    public void lockScreen(boolean lock) {
    }

    public boolean isPackageRunningOnPCMode(String packageName, int uid) {
        return false;
    }

    public boolean isScreenPowerOn() {
        return true;
    }

    public void setScreenPower(boolean powerOn) {
    }

    public void dispatchKeyEventForExclusiveKeyboard(KeyEvent ke) {
    }

    public int forceDisplayMode(int mode) {
        return 0;
    }

    public void saveAppIntent(List<Intent> list) {
    }

    public boolean isConnectExtDisplayFromPkg(String pkgName) {
        return false;
    }

    public void showImeStatusIcon(int iconResId, String pkgName) {
    }

    public void hideImeStatusIcon(String pkgName) {
    }

    public void setPointerIconType(int iconId, boolean keep) {
    }

    public void setCustomPointerIcon(PointerIcon icon, boolean keep) {
    }

    public void notifyDpState(boolean dpState) {
    }

    public int getPCDisplayId() {
        return 0;
    }

    public void execVoiceCmd(Message message) {
    }

    public boolean shouldInterceptInputEvent(KeyEvent ev, boolean forScroll) {
        return false;
    }

    public void LaunchMKForWifiMode() {
    }

    public void showDialogForSwitchDisplay(int displayId, String pkgName) {
    }

    public boolean isAvoidShowDefaultKeyguard(int displayId) {
        return false;
    }

    public void updateFocusDisplayToWindowsCast() {
    }

    public void setNetworkReconnectionState(boolean isNetworkReconnecting) {
    }

    public void sendLockScreenShowViewMsg() {
    }

    public void setPadAssistant(boolean isAssistWithPad) {
    }

    public boolean isPadAssistantMode() {
        return false;
    }
}
