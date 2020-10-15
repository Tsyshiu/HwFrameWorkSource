package com.huawei.opcollect.collector.servicecollection;

import android.content.Context;
import com.huawei.msdp.movement.MovementConstant;
import com.huawei.nb.model.collectencrypt.RawARStatus;
import com.huawei.opcollect.activityrecognition.ARFromAwarenessImpl;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.AbsActionParam;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.lang.ref.WeakReference;
import java.util.Date;

public class ARStatusAction extends Action {
    private static final Object LOCK = new Object();
    public static final long REPORT_LATENCY_NS = 60000000000L;
    public static final long REPORT_LATENCY_NS_SCREEN_OFF = 200000000000L;
    private static final String TAG = "ARStatusAction";
    public static final int TYPE_SCREEN_OFF = 2;
    public static final int TYPE_SCREEN_ON = 1;
    private static ARStatusAction instance = null;
    private ARProvider mARProviderImpl;
    private final Object mLock = new Object();

    private ARStatusAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(queryDailyRecordNum(RawARStatus.class));
    }

    public static ARStatusAction getInstance(Context context) {
        ARStatusAction aRStatusAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ARStatusAction(context, OPCollectConstant.AR_ACTION_NAME);
            }
            aRStatusAction = instance;
        }
        return aRStatusAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        OPCollectLog.i(TAG, "enable");
        super.enable();
        synchronized (this.mLock) {
            if (this.mARProviderImpl == null) {
                this.mARProviderImpl = new ARFromAwarenessImpl(this.mContext, this);
            }
            this.mARProviderImpl.enable();
        }
    }

    public void enableAREvent(int type) {
        OPCollectLog.i(TAG, "enableAREvent");
        synchronized (this.mLock) {
            if (this.mARProviderImpl != null) {
                this.mARProviderImpl.enableAREvent(type);
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void disable() {
        OPCollectLog.i(TAG, "disable");
        super.disable();
        synchronized (this.mLock) {
            if (this.mARProviderImpl != null) {
                this.mARProviderImpl.disable();
                this.mARProviderImpl = null;
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyARStatusActionInstance();
        return true;
    }

    private static void destroyARStatusActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean performWithArgs(AbsActionParam absActionParam) {
        return super.performWithArgs(absActionParam);
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean executeWithArgs(AbsActionParam absActionParam) {
        if (absActionParam == null || !(absActionParam instanceof ARActionParam)) {
            return false;
        }
        ARActionParam actionParam = (ARActionParam) absActionParam;
        RawARStatus rawARStatus = new RawARStatus();
        rawARStatus.setMTimeStamp(new Date());
        rawARStatus.setMMotionType(Integer.valueOf(actionParam.getMotionType()));
        rawARStatus.setMStatus(Integer.valueOf(actionParam.getEventType()));
        rawARStatus.setMReservedText(OPCollectUtils.formatCurrentTime());
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawARStatus).sendToTarget();
        return true;
    }

    static class ARActionParam extends AbsActionParam {
        private int eventType;
        private int motionType;
        private long timestampNs;

        ARActionParam(int motionType2, int eventType2, long timestampNs2) {
            this.motionType = motionType2;
            this.eventType = eventType2;
            this.timestampNs = timestampNs2;
        }

        /* access modifiers changed from: package-private */
        public int getMotionType() {
            return this.motionType;
        }

        /* access modifiers changed from: package-private */
        public int getEventType() {
            return this.eventType;
        }

        /* access modifiers changed from: package-private */
        public long getTimestampNs() {
            return this.timestampNs;
        }

        @Override // com.huawei.opcollect.strategy.AbsActionParam
        public String toString() {
            return "ARActionParam{motionType=" + this.motionType + ", eventType=" + this.eventType + ", timestampNs=" + this.timestampNs + "} " + super.toString();
        }
    }

    public static abstract class ARProvider {
        private final WeakReference<ARStatusAction> service;

        public abstract void disable();

        public abstract void enable();

        public abstract boolean enableAREvent(int i);

        public ARProvider(ARStatusAction service2) {
            this.service = new WeakReference<>(service2);
        }

        /* access modifiers changed from: protected */
        public void storeARStatus(int motion, int event, long timestampNs) {
            ARStatusAction action = this.service.get();
            if (action != null) {
                action.performWithArgs(new ARActionParam(motion, event, timestampNs));
            }
        }
    }

    public enum ActivityType {
        ACTIVITY_IN_VEHICLE(0),
        ACTIVITY_ON_BICYCLE(1),
        ACTIVITY_WALKING(2),
        ACTIVITY_RUNNING(3),
        ACTIVITY_STILL(4),
        ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL(20),
        ACTIVITY_FAST_WALKING(24),
        ACTIVITY_UNKNOWN(63);
        
        private int type;

        private ActivityType(int type2) {
            this.type = type2;
        }

        public int getType() {
            return this.type;
        }
    }

    public enum ActivityName {
        ACTIVITY_IN_VEHICLE(MovementConstant.MSDP_MOVEMENT_IN_VEHICLE),
        ACTIVITY_ON_BICYCLE(MovementConstant.MSDP_MOVEMENT_ON_BICYCLE),
        ACTIVITY_WALKING(MovementConstant.MSDP_MOVEMENT_WALKING),
        ACTIVITY_RUNNING(MovementConstant.MSDP_MOVEMENT_RUNNING),
        ACTIVITY_STILL(MovementConstant.MSDP_MOVEMENT_STILL),
        ACTIVITY_FAST_WALKING(MovementConstant.MSDP_MOVEMENT_FAST_WALKING),
        ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL(MovementConstant.MSDP_MOVEMENT_HIGH_SPEED_RAIL);
        
        private String value;

        private ActivityName(String value2) {
            this.value = value2;
        }

        public String getValue() {
            return this.value;
        }
    }

    public static int activityName2Type(String activityName) {
        if (ActivityName.ACTIVITY_IN_VEHICLE.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_IN_VEHICLE.getType();
        }
        if (ActivityName.ACTIVITY_ON_BICYCLE.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_ON_BICYCLE.getType();
        }
        if (ActivityName.ACTIVITY_WALKING.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_WALKING.getType();
        }
        if (ActivityName.ACTIVITY_RUNNING.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_RUNNING.getType();
        }
        if (ActivityName.ACTIVITY_STILL.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_STILL.getType();
        }
        if (ActivityName.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL.getType();
        }
        if (ActivityName.ACTIVITY_FAST_WALKING.getValue().equals(activityName)) {
            return ActivityType.ACTIVITY_FAST_WALKING.getType();
        }
        return ActivityType.ACTIVITY_UNKNOWN.getType();
    }
}
