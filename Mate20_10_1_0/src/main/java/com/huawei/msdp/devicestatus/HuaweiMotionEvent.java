package com.huawei.msdp.devicestatus;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class HuaweiMotionEvent implements Parcelable {
    public static final Creator<HuaweiMotionEvent> CREATOR = new Creator<HuaweiMotionEvent>() {
        /* class com.huawei.msdp.devicestatus.HuaweiMotionEvent.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public HuaweiMotionEvent createFromParcel(Parcel source) {
            return new HuaweiMotionEvent(source);
        }

        @Override // android.os.Parcelable.Creator
        public HuaweiMotionEvent[] newArray(int size) {
            return new HuaweiMotionEvent[size];
        }
    };
    private int mActivityRunSteps = 0;
    private int mActivityState = 0;
    private int mActivityTotalSteps = 0;
    private int mActivityWalkSteps = 0;
    private int mMotionDirection = 0;
    public Bundle mMotionExtras = null;
    private int mMotionRecoResult = 0;
    private int mMotionType = 0;

    public Bundle getMotionExtras() {
        return this.mMotionExtras;
    }

    public void setMotionExtras(Bundle Extras) {
        this.mMotionExtras = Extras;
    }

    public int getMotionType() {
        return this.mMotionType;
    }

    public void setMotionType(int motionType) {
        this.mMotionType = motionType;
    }

    public int getMotionRecoResult() {
        return this.mMotionRecoResult;
    }

    public void setMotionRecoResult(int motionRecoResult) {
        this.mMotionRecoResult = motionRecoResult;
    }

    public int getMotionDirection() {
        return this.mMotionDirection;
    }

    public void setMotionDirection(int motionDirect) {
        this.mMotionDirection = motionDirect;
    }

    public int getActivityState() {
        return this.mActivityState;
    }

    public void setActivityState(int activityState) {
        this.mActivityState = activityState;
    }

    public int getTotalSteps() {
        return this.mActivityTotalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.mActivityTotalSteps = totalSteps;
    }

    public int getActivityWalkSteps() {
        return this.mActivityWalkSteps;
    }

    public void setActivityWalkSteps(int walkSteps) {
        this.mActivityWalkSteps = walkSteps;
    }

    public int getActivityRunSteps() {
        return this.mActivityRunSteps;
    }

    public void setActivityRunSteps(int runSteps) {
        this.mActivityRunSteps = runSteps;
    }

    public HuaweiMotionEvent() {
    }

    public HuaweiMotionEvent(Parcel in) {
        this.mMotionType = in.readInt();
        this.mMotionRecoResult = in.readInt();
        this.mMotionDirection = in.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMotionType);
        dest.writeInt(this.mMotionRecoResult);
        dest.writeInt(this.mMotionDirection);
    }

    public int describeContents() {
        return 0;
    }
}
