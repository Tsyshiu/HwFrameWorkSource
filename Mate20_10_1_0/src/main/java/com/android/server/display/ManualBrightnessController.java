package com.android.server.display;

import android.util.Log;

public class ManualBrightnessController {
    private static final boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String TAG = "ManualBrightnessController";
    protected final ManualBrightnessCallbacks mCallbacks;

    public interface ManualBrightnessCallbacks {
        void updateManualBrightnessForLux();
    }

    public ManualBrightnessController(ManualBrightnessCallbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    public void updateBrightnesCallbacks() {
        this.mCallbacks.updateManualBrightnessForLux();
    }

    public int getMaxBrightnessForSeekbar() {
        return 255;
    }

    public void updateManualBrightness(int brightness) {
    }

    public int getManualBrightness() {
        return 255;
    }

    public void updatePowerState(int state, boolean enable) {
    }

    public boolean getManualModeAnimationEnable() {
        return false;
    }

    public boolean getManualModeEnable() {
        return false;
    }

    public boolean getManualPowerSavingAnimationEnable() {
        return false;
    }

    public void setManualPowerSavingAnimationEnable(boolean manualPowerSavingAnimationEnable) {
    }

    public boolean getManualThermalModeEnable() {
        return false;
    }

    public boolean getManualThermalModeAnimationEnable() {
        return false;
    }

    public void setManualThermalModeAnimationEnable(boolean thermalModeAnimationEnable) {
    }

    public void setMaxBrightnessFromThermal(int brightness) {
    }

    public boolean getBrightnessSetByAppEnable() {
        return false;
    }

    public boolean getBrightnessSetByAppAnimationEnable() {
        return false;
    }

    public void setBrightnessNoLimit(int brightness, int time) {
    }

    public void updateCurrentUserId(int userId) {
    }

    public boolean needFastestRateForManualBrightness() {
        return false;
    }

    public void setMaxBrightnessNitFromThermal(int brightnessNit) {
    }

    public int getAmbientLux() {
        return 0;
    }

    public int getBrightnessLevel(int lux) {
        return 0;
    }

    public void setFrontCameraAppEnableState(boolean enable) {
    }

    public boolean getFrontCameraDimmingEnable() {
        return false;
    }
}
