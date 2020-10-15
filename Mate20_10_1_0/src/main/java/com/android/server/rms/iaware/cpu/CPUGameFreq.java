package com.android.server.rms.iaware.cpu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.rms.iaware.AwareLog;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;

public final class CPUGameFreq {
    private static final int INVALID_VALUE = -1;
    private static final int MSG_SET_GAME_ENTER = 151;
    private static final int MSG_SET_GAME_LEVEL = 150;
    private static final String TAG = "AwareGameFreq";
    private TimerCleanHandler mTimerCleanHandler = new TimerCleanHandler();

    private static class TimerCleanHandler extends Handler {
        private TimerCleanHandler() {
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 150) {
                if (i == 151 && (msg.obj instanceof Bundle)) {
                    Bundle bundle = (Bundle) msg.obj;
                    boolean isEasEnable = true;
                    if (msg.arg1 != 1) {
                        isEasEnable = false;
                    }
                    IAwareMode.getInstance().gameEnter(bundle, isEasEnable);
                }
            } else if (msg.obj instanceof Bundle) {
                IAwareMode.getInstance().gameLevel((Bundle) msg.obj);
            }
        }
    }

    public int gameFreq(Bundle bundle, boolean isEasEnable) {
        if (bundle == null) {
            AwareLog.e(TAG, "empty bundle!");
            return -1;
        }
        int type = bundle.getInt(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, -1);
        if (type == 1) {
            Message msg = Message.obtain();
            msg.obj = bundle;
            msg.what = 151;
            msg.arg1 = isEasEnable ? 1 : 0;
            this.mTimerCleanHandler.sendMessage(msg);
        } else if (type == 3) {
            Message msg2 = Message.obtain();
            msg2.obj = bundle;
            msg2.what = 150;
            this.mTimerCleanHandler.sendMessage(msg2);
        }
        return 0;
    }
}
