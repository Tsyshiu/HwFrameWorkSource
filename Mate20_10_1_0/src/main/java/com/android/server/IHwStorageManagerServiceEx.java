package com.android.server;

import android.os.storage.VolumeInfo;

public interface IHwStorageManagerServiceEx {
    void onCheckVolumeCompleted(String str, String str2, String str3, int i);

    void startCheckVolume(VolumeInfo volumeInfo);

    int startTurboZoneAdaptation();

    int stopTurboZoneAdaptation();
}
