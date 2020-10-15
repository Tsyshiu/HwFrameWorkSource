package com.android.server.audio;

import android.media.IAudioModeDispatcher;
import android.os.IBinder;
import java.util.Map;

public interface IHwAudioServiceEx {
    boolean bypassVolumeProcessForTV(int i, int i2, int i3, int i4);

    boolean checkMuteZenMode();

    boolean checkRecordActive(int i);

    void dipatchAudioModeChanged(int i);

    IBinder getDeviceSelectCallback();

    int getHwSafeUsbMediaVolumeIndex();

    int getRecordConcurrentType(String str);

    void hideHiResIconDueKilledAPP(boolean z, String str);

    boolean isHwKaraokeEffectEnable(String str);

    boolean isHwSafeUsbMediaVolumeEnabled();

    boolean isKaraokeWhiteListApp(String str);

    boolean isVirtualAudio(int i);

    void notifyHiResIcon(int i);

    void notifySendBroadcastForKaraoke(int i);

    void notifyStartDolbyDms(int i);

    void onSetSoundEffectState(int i, int i2);

    void processAudioServerRestart();

    boolean registerAudioDeviceSelectCallback(IBinder iBinder);

    void registerAudioModeCallback(IAudioModeDispatcher iAudioModeDispatcher);

    void removeKaraokeWhiteAppUIDByPkgName(String str);

    int removeVirtualAudio(String str, String str2, int i, Map<String, Object> map);

    void sendAudioRecordStateChangedIntent(String str, int i, int i2, String str2);

    boolean setDolbyEffect(int i);

    boolean setFmDeviceAvailable(int i, boolean z);

    void setKaraokeWhiteAppUIDByPkgName(String str);

    void setKaraokeWhiteListUID();

    int setSoundEffectState(boolean z, String str, boolean z2, String str2);

    void setSystemReady();

    int startVirtualAudio(String str, String str2, int i, Map<String, Object> map);

    boolean unregisterAudioDeviceSelectCallback(IBinder iBinder);

    void unregisterAudioModeCallback(IAudioModeDispatcher iAudioModeDispatcher);

    void updateTypeCNotify(int i, int i2, String str);
}
