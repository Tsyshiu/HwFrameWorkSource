package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IAwaredConnection;
import com.android.server.rms.iaware.IRDataRegister;
import java.nio.ByteBuffer;

public class NetworkFeature extends RFeature {
    private static final int BASE_VERSION = 2;
    private static final int DISABLE_NETWORK_FEATUE = 0;
    private static final int ENABLE_NETWORK_FEATUE = 1;
    private static final int MSG_NET_BASE_VALUE = 400;
    private static final int MSG_NET_TCP_SWITCH = 401;
    private static final int NUMBER_OF_INT = 2;
    private static final int SIZE_OF_INT = 4;
    private static final String TAG = "NetworkFeature";

    public NetworkFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        setNetworkSwitch(false);
        AwareLog.d(TAG, "NetworkFeature  disable");
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 2) {
            AwareLog.i(TAG, "enableFeatureEx failed, realVersion: " + realVersion + ", vsyncfirst baseVersion: " + 2);
            return false;
        }
        AwareLog.i(TAG, "enableFeatureEx iaware network feature!");
        setNetworkSwitch(true);
        return true;
    }

    private void sendPacket(ByteBuffer buffer) {
        if (buffer == null) {
            AwareLog.e(TAG, "sendPacket ByteBuffer null!");
        } else {
            IAwaredConnection.getInstance().sendPacket(buffer.array(), 0, buffer.position());
        }
    }

    private void setNetworkSwitch(boolean isEnable) {
        AwareLog.d(TAG, "setNetworkSwitch switch = " + isEnable);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(401);
        buffer.putInt(isEnable ? 1 : 0);
        sendPacket(buffer);
    }
}
