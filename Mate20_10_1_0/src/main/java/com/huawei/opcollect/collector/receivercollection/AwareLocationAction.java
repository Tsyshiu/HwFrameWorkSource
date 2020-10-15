package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.huawei.nb.model.collectencrypt.RawPositionState;
import com.huawei.opcollect.collector.servicecollection.LocationRecordAction;
import com.huawei.opcollect.location.HwLocation;
import com.huawei.opcollect.location.ILocationListener;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.lang.ref.WeakReference;
import java.util.Date;

public class AwareLocationAction extends ReceiverAction implements ILocationListener {
    private static final String CHINA_COUNTRY_CODE = "CN";
    private static final String CHINA_COUNTRY_NUM = "156";
    private static final int LOCATION_COMPANY = 1;
    private static final int LOCATION_HOME = 0;
    private static final int LOCATION_HOMECITY = 3;
    private static final int LOCATION_OUTSIDE_CITY = 2;
    private static final int LOCATION_OVERSEA = 4;
    private static final int LOCATION_UNKNOWN = -1;
    private static final Object LOCK = new Object();
    private static final int MESSAGE_ON_CHANGE = 1;
    private static final String PLACE_RECOGNITION_ACTION = "com.huawei.placerecognition.action.PLACE_RECOG";
    private static final String PLACE_RECOGNITION_KEY = "placerecognition_key";
    private static final String PLACE_RECOGNITION_PERMISSION = "com.huawei.placerecognition.permission.PLACE_RECOG";
    private static final String SP_BC_FLAG = "intelligent_broadcast_flag";
    private static final String SP_CACHE_KEY = "CachedCity";
    private static final String SP_CACHE_XML = "CachedCity";
    private static final String TAG = "AwareLocationAction";
    private static AwareLocationAction instance = null;
    /* access modifiers changed from: private */
    public boolean isHomeOrOffice = false;
    /* access modifiers changed from: private */
    public boolean isNeedModifyCityCode = false;
    /* access modifiers changed from: private */
    public AwareHandler mHandler = null;
    /* access modifiers changed from: private */
    public int mLocationType = -1;

    private AwareLocationAction(Context context, String name) {
        super(context, name);
        OPCollectLog.r("AwareLocationAction", "AwareLocationAction");
        setDailyRecordNum(queryDailyRecordNum(RawPositionState.class));
    }

    public static AwareLocationAction getInstance(Context context) {
        AwareLocationAction awareLocationAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new AwareLocationAction(context, "AwareLocationAction");
            }
            awareLocationAction = instance;
        }
        return awareLocationAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyAwareLocationActionInstance();
        return true;
    }

    private static void destroyAwareLocationActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new AwareLocationReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PLACE_RECOGNITION_ACTION);
            this.mContext.registerReceiver(this.mReceiver, intentFilter, PLACE_RECOGNITION_PERMISSION, OdmfCollectScheduler.getInstance().getCtrlHandler());
            LocationRecordAction.getInstance(this.mContext).addLocationListener("AwareLocationAction", this);
        }
        if (this.mHandler == null) {
            this.mHandler = new AwareHandler(this);
        }
    }

    /* access modifiers changed from: private */
    public static class AwareHandler extends Handler {
        private final WeakReference<AwareLocationAction> service;

        AwareHandler(AwareLocationAction service2) {
            this.service = new WeakReference<>(service2);
        }

        public void handleMessage(Message msg) {
            AwareLocationAction action;
            super.handleMessage(msg);
            if (msg != null && msg.what == 1 && (action = this.service.get()) != null) {
                int unused = action.mLocationType = msg.arg1;
                action.perform();
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action, com.huawei.opcollect.collector.receivercollection.ReceiverAction
    public void disable() {
        super.disable();
        OdmfActionManager.getInstance().removeLocationListener("AwareLocationAction", this);
        if (this.mHandler != null) {
            this.mHandler = null;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        RawPositionState rawPositionState = new RawPositionState();
        rawPositionState.setMTimeStamp(new Date());
        rawPositionState.setMStatus(Integer.valueOf(this.mLocationType));
        rawPositionState.setMReservedText(OPCollectUtils.formatCurrentTime());
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawPositionState).sendToTarget();
        return true;
    }

    @Override // com.huawei.opcollect.location.ILocationListener
    public void onLocationSuccess(HwLocation hwLocation) {
        if (this.mContext == null) {
            OPCollectLog.e("AwareLocationAction", "context is null");
            return;
        }
        SharedPreferences sp = this.mContext.getSharedPreferences("CachedCity", 32768);
        if (sp.getInt(SP_BC_FLAG, -1) != 1) {
            OPCollectLog.e("AwareLocationAction", "not receive intelligent broadcast, return");
        } else if (hwLocation == null) {
            OPCollectLog.e("AwareLocationAction", "hwLocation is null");
        } else if (this.mHandler == null) {
            OPCollectLog.e("AwareLocationAction", "handler is null");
        } else {
            OPCollectLog.r("AwareLocationAction", "callback.");
            setCityCode(hwLocation.getCityCode());
            String curCountry = hwLocation.getCountry();
            String curCityCode = hwLocation.getCityCode();
            String cacheCityCode = sp.getString("CachedCity", "");
            if (!TextUtils.isEmpty(curCountry) && !CHINA_COUNTRY_CODE.equalsIgnoreCase(curCountry) && !CHINA_COUNTRY_NUM.equalsIgnoreCase(curCountry)) {
                this.mHandler.obtainMessage(1, 4, 0).sendToTarget();
            } else if (!TextUtils.isEmpty(curCityCode) && !TextUtils.isEmpty(cacheCityCode)) {
                if (!cacheCityCode.equals(curCityCode)) {
                    this.mHandler.obtainMessage(1, 2, 0).sendToTarget();
                } else if (!this.isHomeOrOffice) {
                    this.mHandler.obtainMessage(1, 3, 0).sendToTarget();
                }
            }
        }
    }

    class AwareLocationReceiver extends BroadcastReceiver {
        AwareLocationReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z;
            if (intent != null && AwareLocationAction.PLACE_RECOGNITION_ACTION.equalsIgnoreCase(intent.getAction())) {
                AwareLocationAction.this.setBroadcastFlag();
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    OPCollectLog.e("AwareLocationAction", "onReceive --> bundle is null");
                    return;
                }
                int eventId = bundle.getInt(AwareLocationAction.PLACE_RECOGNITION_KEY);
                OPCollectLog.r("AwareLocationAction", "eventID: " + eventId);
                if (eventId == -1) {
                    boolean unused = AwareLocationAction.this.isHomeOrOffice = false;
                    return;
                }
                AwareLocationAction awareLocationAction = AwareLocationAction.this;
                if (eventId == 0 || eventId == 1) {
                    z = true;
                } else {
                    z = false;
                }
                boolean unused2 = awareLocationAction.isNeedModifyCityCode = z;
                boolean unused3 = AwareLocationAction.this.isHomeOrOffice = AwareLocationAction.this.isNeedModifyCityCode;
                if (AwareLocationAction.this.mHandler == null) {
                    OPCollectLog.e("AwareLocationAction", "handler is null");
                } else {
                    AwareLocationAction.this.mHandler.obtainMessage(1, eventId, 0).sendToTarget();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void setBroadcastFlag() {
        if (this.mContext == null) {
            OPCollectLog.e("AwareLocationAction", "context is null");
            return;
        }
        SharedPreferences sp = this.mContext.getSharedPreferences("CachedCity", 32768);
        if (sp.getInt(SP_BC_FLAG, -1) != 1) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(SP_BC_FLAG, 1);
            editor.apply();
            OPCollectLog.i("AwareLocationAction", "receive intelligent broadcast, store it");
        }
    }

    private void setCityCode(String cityCode) {
        if (TextUtils.isEmpty(cityCode)) {
            OPCollectLog.e("AwareLocationAction", "cityCode is null or is empty");
        } else if (!this.isNeedModifyCityCode) {
            OPCollectLog.e("AwareLocationAction", "no need modify city code");
        } else {
            SharedPreferences.Editor editor = this.mContext.getSharedPreferences("CachedCity", 32768).edit();
            editor.putString("CachedCity", cityCode);
            editor.apply();
            this.isNeedModifyCityCode = false;
        }
    }
}
