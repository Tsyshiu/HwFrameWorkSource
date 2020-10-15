package com.android.server.wifi.HwWiTas;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.wifi.HwHiLog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiNative;
import com.huawei.hwwifiproservice.WifiHandover;

public class HwWiTasStateMachine extends StateMachine {
    private static HwWiTasStateMachine mWiTasStateMachine = null;
    /* access modifiers changed from: private */
    public int mAntIndexRecord = 0;
    /* access modifiers changed from: private */
    public int mAntRssiRecord = 0;
    /* access modifiers changed from: private */
    public int mAntRttRecord = 0;
    private Context mContext;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHasSelectAnt = false;
    /* access modifiers changed from: private */
    public State mIdleState = new WiTasIdleState();
    /* access modifiers changed from: private */
    public boolean mIs2GConnected = false;
    private boolean mIsForceAntRest = false;
    /* access modifiers changed from: private */
    public boolean mIsForceUseRssi = false;
    private boolean mIsGameStarted = false;
    private boolean mIsLandscapeOri = false;
    /* access modifiers changed from: private */
    public boolean mIsSisoMode = false;
    /* access modifiers changed from: private */
    public boolean mIsTelInCall = false;
    /* access modifiers changed from: private */
    public State mSteadyState = new WiTasSteadyState();
    /* access modifiers changed from: private */
    public boolean mTasScene = false;
    /* access modifiers changed from: private */
    public State mTransitionState = new WiTasTransitionState();
    /* access modifiers changed from: private */
    public HwWiTasArbitra mWiTasArbitra;
    /* access modifiers changed from: private */
    public HwWiTasChr mWiTasChr;
    private HwWiTasMonitor mWiTasMonitor;
    private WifiManager mWifiManager;

    class WiTasIdleState extends State {
        WiTasIdleState() {
        }

        public void enter() {
            HwHiLog.d(HwWiTasUtils.TAG, false, "Enter WiTasIdleState state", new Object[0]);
            boolean unused = HwWiTasStateMachine.this.mHasSelectAnt = false;
            boolean unused2 = HwWiTasStateMachine.this.mIsSisoMode = false;
            boolean unused3 = HwWiTasStateMachine.this.mIsForceUseRssi = false;
            HwWiTasStateMachine.this.mWiTasChr.recordState(0);
            if (HwWiTasStateMachine.this.mHandler.hasMessages(25)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(25);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(26)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(26);
            }
        }

        public void exit() {
            HwHiLog.d(HwWiTasUtils.TAG, false, "Exit WiTasIdleState state", new Object[0]);
        }

        public boolean processMessage(Message message) {
            HwHiLog.d(HwWiTasUtils.TAG, false, "WiTasIdleState, message: %{public}d", new Object[]{Integer.valueOf(message.what)});
            HwWiTasStateMachine.this.processWiTasDecisiveMessage(message);
            if (HwWiTasStateMachine.this.mIs2GConnected && HwWiTasStateMachine.this.mTasScene && !HwWiTasStateMachine.this.mIsTelInCall) {
                HwWiTasStateMachine.this.mWiTasArbitra.checkAndSetTestMode();
                HwWiTasStateMachine hwWiTasStateMachine = HwWiTasStateMachine.this;
                hwWiTasStateMachine.transitionTo(hwWiTasStateMachine.mTransitionState);
                HwWiTasStateMachine.this.mWiTasChr.increaseStartCnt();
                HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(25, 30000);
                if (HwWiTasStateMachine.this.mWiTasArbitra.isGameInWhiteList()) {
                    HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(26, 300000);
                }
            }
            return true;
        }
    }

    class WiTasTransitionState extends State {
        private int mDstAntIndex = 0;
        private int mDstAntRssi = 0;
        private boolean mIsCmdErr = false;
        private int mSrcAntIndex = 0;
        private int mSrcAntRssi = 0;

        WiTasTransitionState() {
        }

        public void enter() {
            HwHiLog.d(HwWiTasUtils.TAG, false, "Enter WiTasTransitionState state", new Object[0]);
            this.mIsCmdErr = false;
            this.mSrcAntIndex = HwWiTasStateMachine.this.mWiTasArbitra.getCurAntIndex();
            int i = this.mSrcAntIndex;
            this.mDstAntIndex = 1 - i;
            int unused = HwWiTasStateMachine.this.mAntIndexRecord = i;
            HwWiTasStateMachine hwWiTasStateMachine = HwWiTasStateMachine.this;
            int unused2 = hwWiTasStateMachine.mAntRttRecord = hwWiTasStateMachine.mWiTasArbitra.getGameDelay();
            HwWiTasStateMachine hwWiTasStateMachine2 = HwWiTasStateMachine.this;
            int unused3 = hwWiTasStateMachine2.mAntRssiRecord = hwWiTasStateMachine2.mWiTasArbitra.getCurRssiInfo();
            HwWiTasStateMachine.this.mWiTasChr.resetData();
            HwWiTasStateMachine.this.mWiTasChr.recordSrcIndex(this.mSrcAntIndex);
            HwWiTasStateMachine.this.mWiTasChr.recordTimestamp(System.currentTimeMillis());
            HwWiTasStateMachine.this.mHandler.sendEmptyMessage(10);
            HwHiLog.d(HwWiTasUtils.TAG, false, "srcAntIndex: %{public}d, dstAntIndex: %{public}d", new Object[]{Integer.valueOf(this.mSrcAntIndex), Integer.valueOf(this.mDstAntIndex)});
        }

        public void exit() {
            HwHiLog.d(HwWiTasUtils.TAG, false, "Exit WiTasTransitionState state", new Object[0]);
            HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(0);
            if (this.mSrcAntIndex != HwWiTasStateMachine.this.mWiTasArbitra.getCurAntIndex() && !this.mIsCmdErr) {
                HwWiTasStateMachine.this.mWiTasChr.increaseSrctAntChgCnt();
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(11)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(11);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(14)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(14);
            }
        }

        public boolean processMessage(Message message) {
            HwHiLog.d(HwWiTasUtils.TAG, false, "WiTasTransitionState, message: %{public}d", new Object[]{Integer.valueOf(message.what)});
            HwWiTasStateMachine.this.processWiTasDecisiveMessage(message);
            int i = message.what;
            if (i != 20) {
                switch (i) {
                    case 10:
                        HwWiTasStateMachine.this.mWiTasChr.recordState(1);
                        HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(1);
                        measureAntenna();
                        HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(11, (long) HwWiTasStateMachine.this.mWiTasArbitra.getTimeoutThr());
                        break;
                    case 11:
                        HwWiTasStateMachine.this.mWiTasChr.recordErrCode(2);
                        HwHiLog.d(HwWiTasUtils.TAG, false, "src antenna measure timeout", new Object[0]);
                        this.mIsCmdErr = true;
                        HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(0);
                        break;
                    case 12:
                        if (HwWiTasStateMachine.this.mHandler.hasMessages(11)) {
                            HwWiTasStateMachine.this.mHandler.removeMessages(11);
                        }
                        this.mSrcAntRssi = message.arg1;
                        HwWiTasStateMachine.this.mWiTasChr.recordSrcRssi(this.mSrcAntRssi);
                        HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(0);
                        HwWiTasStateMachine.this.mHandler.sendEmptyMessage(13);
                        break;
                    case 13:
                        HwWiTasStateMachine.this.mWiTasChr.recordState(2);
                        switchAntenna(this.mDstAntIndex);
                        HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(2);
                        measureAntenna();
                        HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(14, (long) HwWiTasStateMachine.this.mWiTasArbitra.getTimeoutThr());
                        break;
                    case 14:
                        HwWiTasStateMachine.this.mWiTasChr.recordErrCode(3);
                        HwHiLog.d(HwWiTasUtils.TAG, false, "dst antenna measure timeout", new Object[0]);
                        this.mIsCmdErr = true;
                        HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(0);
                        break;
                    case 15:
                        if (HwWiTasStateMachine.this.mHandler.hasMessages(14)) {
                            HwWiTasStateMachine.this.mHandler.removeMessages(14);
                        }
                        this.mDstAntRssi = message.arg1;
                        HwWiTasStateMachine.this.mWiTasChr.recordDstRssi(this.mDstAntRssi);
                        HwWiTasStateMachine.this.mWiTasArbitra.setMeasureState(0);
                        HwWiTasStateMachine.this.mHandler.sendEmptyMessage(16);
                        if (this.mSrcAntIndex != HwWiTasUtils.mDefaultAntIndex) {
                            HwWiTasStateMachine.this.mWiTasChr.recordTasAntMeasureDiff(this.mDstAntRssi - this.mSrcAntRssi);
                            break;
                        } else {
                            HwWiTasStateMachine.this.mWiTasChr.recordDefAntMeasureDiff(this.mDstAntRssi - this.mSrcAntRssi);
                            break;
                        }
                    case 16:
                        chooseAntenna();
                        boolean unused = HwWiTasStateMachine.this.mHasSelectAnt = true;
                        HwWiTasStateMachine hwWiTasStateMachine = HwWiTasStateMachine.this;
                        hwWiTasStateMachine.transitionTo(hwWiTasStateMachine.mSteadyState);
                        return true;
                }
            } else {
                HwHiLog.d(HwWiTasUtils.TAG, false, "wifi driver switch to siso mode, core0 is disable", new Object[0]);
                HwWiTasStateMachine.this.mWiTasChr.recordState(6);
                boolean unused2 = HwWiTasStateMachine.this.mIsSisoMode = true;
                HwWiTasStateMachine hwWiTasStateMachine2 = HwWiTasStateMachine.this;
                hwWiTasStateMachine2.transitionTo(hwWiTasStateMachine2.mSteadyState);
            }
            if (!HwWiTasStateMachine.this.mIs2GConnected || !HwWiTasStateMachine.this.mTasScene || HwWiTasStateMachine.this.mIsTelInCall) {
                HwWiTasStateMachine.this.mWiTasArbitra.switchAntenna(HwWiTasUtils.mDefaultAntIndex);
                HwWiTasStateMachine hwWiTasStateMachine3 = HwWiTasStateMachine.this;
                hwWiTasStateMachine3.transitionTo(hwWiTasStateMachine3.mIdleState);
                return true;
            } else if (!this.mIsCmdErr) {
                return true;
            } else {
                HwHiLog.e(HwWiTasUtils.TAG, false, "command error, transition to steady state", new Object[0]);
                HwWiTasStateMachine.this.mWiTasArbitra.switchAntenna(this.mSrcAntIndex);
                HwWiTasStateMachine.this.mWiTasArbitra.setFreezeTime(15000);
                HwWiTasStateMachine hwWiTasStateMachine4 = HwWiTasStateMachine.this;
                hwWiTasStateMachine4.transitionTo(hwWiTasStateMachine4.mSteadyState);
                return true;
            }
        }

        private void switchAntenna(int antIndex) {
            if (HwWiTasStateMachine.this.mWiTasArbitra.switchAntenna(antIndex) != 0) {
                this.mIsCmdErr = true;
            }
        }

        private void measureAntenna() {
            if (HwWiTasStateMachine.this.mWiTasArbitra.measureAntenna() != 0) {
                this.mIsCmdErr = true;
            }
        }

        private void chooseAntenna() {
            if (HwWiTasStateMachine.this.mWiTasArbitra.chooseAntenna(this.mSrcAntRssi, this.mDstAntRssi) != 0) {
                this.mIsCmdErr = true;
            }
        }
    }

    class WiTasSteadyState extends State {
        private boolean mIsFreezeState = false;
        private int mRecordRssi = 0;

        WiTasSteadyState() {
        }

        public void enter() {
            HwHiLog.d(HwWiTasUtils.TAG, false, "Enter WiTasSteadyState state", new Object[0]);
            this.mIsFreezeState = false;
            this.mRecordRssi = 0;
            if (!HwWiTasStateMachine.this.mIsSisoMode) {
                HwWiTasStateMachine.this.mHandler.sendEmptyMessage(17);
                HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(22, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
                HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(24, 10000);
            }
        }

        public void exit() {
            HwHiLog.d(HwWiTasUtils.TAG, false, "Exit WiTasSteadyState state", new Object[0]);
            if (HwWiTasStateMachine.this.mHandler.hasMessages(18)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(18);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(19)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(19);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(22)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(22);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(23)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(23);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(24)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(24);
            }
            if (HwWiTasStateMachine.this.mHandler.hasMessages(25)) {
                HwWiTasStateMachine.this.mHandler.removeMessages(25);
            }
        }

        public boolean processMessage(Message message) {
            if (!(message.what == 19 || message.what == 23)) {
                HwHiLog.d(HwWiTasUtils.TAG, false, "WiTasSteadyState, message: %{public}d", new Object[]{Integer.valueOf(message.what)});
            }
            HwWiTasStateMachine.this.processWiTasDecisiveMessage(message);
            if (message.what == 21) {
                HwHiLog.d(HwWiTasUtils.TAG, false, "wifi driver switch to mimo mode, core0 is enable", new Object[0]);
                boolean unused = HwWiTasStateMachine.this.mIsSisoMode = false;
                if (!HwWiTasStateMachine.this.mHasSelectAnt) {
                    HwWiTasStateMachine hwWiTasStateMachine = HwWiTasStateMachine.this;
                    hwWiTasStateMachine.transitionTo(hwWiTasStateMachine.mTransitionState);
                } else {
                    HwWiTasStateMachine.this.mHandler.sendEmptyMessage(18);
                }
                return true;
            }
            if (!HwWiTasStateMachine.this.mIsSisoMode) {
                int i = message.what;
                if (i != 9) {
                    switch (i) {
                        case 17:
                            int freezeTime = HwWiTasStateMachine.this.mWiTasArbitra.getFreezeTime();
                            HwHiLog.d(HwWiTasUtils.TAG, false, "enter freeze state, freeze time: %{public}d", new Object[]{Integer.valueOf(freezeTime)});
                            this.mIsFreezeState = true;
                            HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(18, (long) freezeTime);
                            HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(23, 1000);
                            break;
                        case 18:
                            HwHiLog.d(HwWiTasUtils.TAG, false, "exit freeze state", new Object[0]);
                            this.mIsFreezeState = false;
                            if (HwWiTasStateMachine.this.mHandler.hasMessages(23)) {
                                HwWiTasStateMachine.this.mHandler.removeMessages(23);
                            }
                            if (HwWiTasStateMachine.this.mWiTasArbitra.isGameInWhiteList() && !HwWiTasStateMachine.this.mIsForceUseRssi) {
                                if (HwWiTasStateMachine.this.mWiTasArbitra.isGameLag(HwWiTasStateMachine.this.mWiTasArbitra.getGameDelay())) {
                                    HwWiTasStateMachine.this.mHandler.sendEmptyMessage(9);
                                    break;
                                }
                            } else {
                                HwWiTasStateMachine.this.mHandler.sendEmptyMessage(19);
                                break;
                            }
                            break;
                        case 19:
                            if (HwWiTasStateMachine.this.mWiTasArbitra.isGameInWhiteList() && HwWiTasStateMachine.this.mWiTasArbitra.getGameDelay() != 0) {
                                boolean unused2 = HwWiTasStateMachine.this.mIsForceUseRssi = false;
                                break;
                            } else {
                                if (!HwWiTasStateMachine.this.mWiTasArbitra.isRssiBad(HwWiTasStateMachine.this.mWiTasArbitra.getCurRssiInfo())) {
                                    HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(19, 3000);
                                    break;
                                } else {
                                    HwWiTasStateMachine hwWiTasStateMachine2 = HwWiTasStateMachine.this;
                                    hwWiTasStateMachine2.transitionTo(hwWiTasStateMachine2.mTransitionState);
                                    return true;
                                }
                            }
                            break;
                        default:
                            switch (i) {
                                case 22:
                                    HwWiTasStateMachine.this.mWiTasChr.recordState(5);
                                    if (HwWiTasStateMachine.this.mAntIndexRecord != HwWiTasStateMachine.this.mWiTasArbitra.getCurAntIndex()) {
                                        int nCurrentRssi = HwWiTasStateMachine.this.mWiTasArbitra.getCurRssiInfo();
                                        if (HwWiTasStateMachine.this.mAntIndexRecord != HwWiTasUtils.mDefaultAntIndex) {
                                            HwWiTasStateMachine.this.mWiTasChr.recordTasToDefRssi(nCurrentRssi - HwWiTasStateMachine.this.mAntRssiRecord);
                                            break;
                                        } else {
                                            HwWiTasStateMachine.this.mWiTasChr.recordDefToTasRssi(nCurrentRssi - HwWiTasStateMachine.this.mAntRssiRecord);
                                            break;
                                        }
                                    }
                                    break;
                                case 23:
                                    int currentRssi = HwWiTasStateMachine.this.mWiTasArbitra.getCurRssiInfo();
                                    if (!HwWiTasStateMachine.this.mWiTasArbitra.isRssiUnSteady(currentRssi, this.mRecordRssi)) {
                                        this.mRecordRssi = currentRssi;
                                        if (this.mIsFreezeState) {
                                            HwWiTasStateMachine.this.mHandler.sendEmptyMessageDelayed(23, 1000);
                                            break;
                                        }
                                    } else {
                                        this.mIsFreezeState = false;
                                        HwWiTasStateMachine hwWiTasStateMachine3 = HwWiTasStateMachine.this;
                                        hwWiTasStateMachine3.transitionTo(hwWiTasStateMachine3.mTransitionState);
                                        return true;
                                    }
                                    break;
                                case 24:
                                    if (HwWiTasStateMachine.this.mAntIndexRecord != HwWiTasStateMachine.this.mWiTasArbitra.getCurAntIndex()) {
                                        int nCurrentRtt = HwWiTasStateMachine.this.mWiTasArbitra.getGameDelay();
                                        if (HwWiTasStateMachine.this.mAntIndexRecord != HwWiTasUtils.mDefaultAntIndex) {
                                            HwWiTasStateMachine.this.mWiTasChr.recordTasToDefRtt(nCurrentRtt - HwWiTasStateMachine.this.mAntRttRecord);
                                            break;
                                        } else {
                                            HwWiTasStateMachine.this.mWiTasChr.recordDefToTasRtt(nCurrentRtt - HwWiTasStateMachine.this.mAntRttRecord);
                                            break;
                                        }
                                    }
                                    break;
                                case 25:
                                    HwWiTasStateMachine.this.mWiTasChr.recordErrCode(7);
                                    break;
                                case 26:
                                    if (HwWiTasStateMachine.this.mWiTasArbitra.isGameInWhiteList() && HwWiTasStateMachine.this.mWiTasArbitra.getGameDelay() == 0) {
                                        Log.w(HwWiTasUtils.TAG, "whilte list app do not report rtt");
                                        boolean unused3 = HwWiTasStateMachine.this.mIsForceUseRssi = true;
                                        HwWiTasStateMachine.this.mHandler.sendEmptyMessage(19);
                                        break;
                                    }
                            }
                            break;
                    }
                } else if (!this.mIsFreezeState) {
                    boolean unused4 = HwWiTasStateMachine.this.mIsForceUseRssi = false;
                    int currentRssi2 = HwWiTasStateMachine.this.mWiTasArbitra.getCurRssiInfo();
                    if (HwWiTasStateMachine.this.mWiTasArbitra.isRssiBad(currentRssi2)) {
                        HwWiTasStateMachine hwWiTasStateMachine4 = HwWiTasStateMachine.this;
                        hwWiTasStateMachine4.transitionTo(hwWiTasStateMachine4.mTransitionState);
                        return true;
                    }
                    HwHiLog.d(HwWiTasUtils.TAG, false, "game is lag, rssi: %{public}d, don't change state", new Object[]{Integer.valueOf(currentRssi2)});
                }
            }
            if (!HwWiTasStateMachine.this.mIs2GConnected || !HwWiTasStateMachine.this.mTasScene || HwWiTasStateMachine.this.mIsTelInCall) {
                HwWiTasStateMachine.this.mWiTasArbitra.switchAntenna(HwWiTasUtils.mDefaultAntIndex);
                HwWiTasStateMachine hwWiTasStateMachine5 = HwWiTasStateMachine.this;
                hwWiTasStateMachine5.transitionTo(hwWiTasStateMachine5.mIdleState);
            }
            return true;
        }
    }

    public static synchronized HwWiTasStateMachine createWiTasStateMachine(Context context, WifiNative wifiNative) {
        HwWiTasStateMachine hwWiTasStateMachine;
        synchronized (HwWiTasStateMachine.class) {
            if (mWiTasStateMachine == null) {
                mWiTasStateMachine = new HwWiTasStateMachine(context, wifiNative);
            }
            hwWiTasStateMachine = mWiTasStateMachine;
        }
        return hwWiTasStateMachine;
    }

    private HwWiTasStateMachine(Context context, WifiNative wifiNative) {
        super("HwWiTasStateMachine");
        HwHiLog.d(HwWiTasUtils.TAG, false, "init HwWiTasStateMachine", new Object[0]);
        this.mContext = context;
        this.mHandler = getHandler();
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mWiTasArbitra = HwWiTasArbitra.getInstance(this.mWifiManager, wifiNative);
        this.mWiTasMonitor = HwWiTasMonitor.getInstance(context, this.mHandler, this.mWifiManager, wifiNative);
        int witasMode = HwWiTasUtils.getWitasMode();
        if (witasMode == 0) {
            HwWiTasUtils.mDefaultAntIndex = 0;
            HwWiTasUtils.mWitasAntIndex = 1;
            HwWiTasUtils.mCoreAntIndex = 0;
        } else if (witasMode == 1) {
            HwWiTasUtils.mDefaultAntIndex = 1;
            HwWiTasUtils.mWitasAntIndex = 0;
            HwWiTasUtils.mCoreAntIndex = 0;
        } else if (witasMode == 2) {
            HwWiTasUtils.mDefaultAntIndex = 0;
            HwWiTasUtils.mWitasAntIndex = 1;
            HwWiTasUtils.mCoreAntIndex = 1;
        } else if (witasMode != 3) {
            Log.i(HwWiTasUtils.TAG, "WitasMode is unknown!");
        } else {
            HwWiTasUtils.mDefaultAntIndex = 1;
            HwWiTasUtils.mWitasAntIndex = 0;
            HwWiTasUtils.mCoreAntIndex = 1;
        }
        HwWiTasUtils.sTasScene = HwWiTasUtils.getWitasScene();
        this.mWiTasMonitor.startMonitor();
        this.mWiTasChr = HwWiTasChr.getInstance(this.mWifiManager, wifiNative);
        this.mWiTasChr.initWitasChr();
        addState(this.mIdleState);
        addState(this.mTransitionState, this.mIdleState);
        addState(this.mSteadyState, this.mIdleState);
        setInitialState(this.mIdleState);
        start();
    }

    private void updateTasScene() {
        if (HwWiTasUtils.sTasScene == 1) {
            this.mTasScene = this.mIsLandscapeOri;
        } else {
            this.mTasScene = this.mIsGameStarted;
        }
    }

    /* access modifiers changed from: private */
    public void processWiTasDecisiveMessage(Message message) {
        int i = message.what;
        boolean z = false;
        if (i != 1) {
            if (i != 2) {
                if (i != 27) {
                    switch (i) {
                        case 4:
                            break;
                        case 5:
                            this.mIsTelInCall = true;
                            return;
                        case 6:
                            this.mIsTelInCall = false;
                            return;
                        case 7:
                            this.mIsGameStarted = true;
                            updateTasScene();
                            return;
                        case 8:
                            this.mIsGameStarted = false;
                            updateTasScene();
                            return;
                        default:
                            return;
                    }
                } else {
                    Context context = this.mContext;
                    if (!(context == null || context.getResources() == null || this.mContext.getResources().getConfiguration() == null)) {
                        if (this.mContext.getResources().getConfiguration().orientation == 2) {
                            z = true;
                        }
                        this.mIsLandscapeOri = z;
                    }
                    updateTasScene();
                    return;
                }
            }
            this.mIs2GConnected = false;
            this.mIsForceAntRest = true;
            return;
        }
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        if (wifiInfo == null || !wifiInfo.is24GHz()) {
            this.mIs2GConnected = false;
            return;
        }
        if (this.mIsForceAntRest) {
            this.mIsForceAntRest = false;
            this.mWiTasArbitra.switchAntenna(HwWiTasUtils.mDefaultAntIndex);
        }
        this.mIs2GConnected = true;
    }
}
