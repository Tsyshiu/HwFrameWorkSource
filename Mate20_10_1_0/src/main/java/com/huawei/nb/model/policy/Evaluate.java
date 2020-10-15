package com.huawei.nb.model.policy;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class Evaluate extends AManagedObject {
    public static final Parcelable.Creator<Evaluate> CREATOR = new Parcelable.Creator<Evaluate>() {
        /* class com.huawei.nb.model.policy.Evaluate.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public Evaluate createFromParcel(Parcel in) {
            return new Evaluate(in);
        }

        @Override // android.os.Parcelable.Creator
        public Evaluate[] newArray(int size) {
            return new Evaluate[size];
        }
    };
    private Integer stub;

    public Evaluate(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.stub = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
    }

    public Evaluate(Parcel in) {
        super(in);
        if (in.readByte() == 0) {
            this.stub = null;
            in.readInt();
            return;
        }
        this.stub = Integer.valueOf(in.readInt());
    }

    private Evaluate(Integer stub2) {
        this.stub = stub2;
    }

    public Evaluate() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Integer getStub() {
        return this.stub;
    }

    public void setStub(Integer stub2) {
        this.stub = stub2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.stub != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.stub.intValue());
            return;
        }
        out.writeByte((byte) 0);
        out.writeInt(1);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<Evaluate> getHelper() {
        return EvaluateHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.policy.Evaluate";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Evaluate { stub: ").append(this.stub);
        sb.append(" }");
        return sb.toString();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.12";
    }

    public int getDatabaseVersionCode() {
        return 12;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
