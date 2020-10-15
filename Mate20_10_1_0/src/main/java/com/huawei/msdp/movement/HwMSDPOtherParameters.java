package com.huawei.msdp.movement;

import android.os.Parcel;
import android.os.Parcelable;

public class HwMSDPOtherParameters implements Parcelable {
    public static final Creator<HwMSDPOtherParameters> CREATOR = new Creator<HwMSDPOtherParameters>() {
        /* class com.huawei.msdp.movement.HwMSDPOtherParameters.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public HwMSDPOtherParameters createFromParcel(Parcel source) {
            return new HwMSDPOtherParameters(source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readString());
        }

        @Override // android.os.Parcelable.Creator
        public HwMSDPOtherParameters[] newArray(int size) {
            return new HwMSDPOtherParameters[size];
        }
    };
    private double mParam1;
    private double mParam2;
    private double mParam3;
    private double mParam4;
    private String mParam5;

    public HwMSDPOtherParameters(double param1, double param2, double param3, double param4, String param5) {
        this.mParam1 = param1;
        this.mParam2 = param2;
        this.mParam3 = param3;
        this.mParam4 = param4;
        this.mParam5 = param5;
    }

    public void setmParam1(double mParam12) {
        this.mParam1 = mParam12;
    }

    public void setmParam2(double mParam22) {
        this.mParam2 = mParam22;
    }

    public void setmParam3(double mParam32) {
        this.mParam3 = mParam32;
    }

    public void setmParam4(double mParam42) {
        this.mParam4 = mParam42;
    }

    public void setmParam5(String mParam52) {
        this.mParam5 = mParam52;
    }

    public double getmParam1() {
        return this.mParam1;
    }

    public double getmParam2() {
        return this.mParam2;
    }

    public double getmParam3() {
        return this.mParam3;
    }

    public double getmParam4() {
        return this.mParam4;
    }

    public String getmParam5() {
        return this.mParam5;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeDouble(this.mParam1);
        parcel.writeDouble(this.mParam2);
        parcel.writeDouble(this.mParam3);
        parcel.writeDouble(this.mParam4);
        parcel.writeString(this.mParam5);
    }

    public void readFromParcel(Parcel in) {
        this.mParam1 = in.readDouble();
        this.mParam2 = in.readDouble();
        this.mParam3 = in.readDouble();
        this.mParam4 = in.readDouble();
        this.mParam5 = in.readString();
    }

    public String toString() {
        return String.format("Param1=%f, Param2=%f, Param3=%f, Param4=%f, Param5=%s", Double.valueOf(this.mParam1), Double.valueOf(this.mParam2), Double.valueOf(this.mParam3), Double.valueOf(this.mParam4), this.mParam5);
    }
}
