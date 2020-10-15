package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CustomUserProfiling extends AManagedObject {
    public static final Parcelable.Creator<CustomUserProfiling> CREATOR = new Parcelable.Creator<CustomUserProfiling>() {
        /* class com.huawei.nb.model.pengine.CustomUserProfiling.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public CustomUserProfiling createFromParcel(Parcel in) {
            return new CustomUserProfiling(in);
        }

        @Override // android.os.Parcelable.Creator
        public CustomUserProfiling[] newArray(int size) {
            return new CustomUserProfiling[size];
        }
    };
    private Integer id;
    private Integer level;
    private String parent;
    private Long timestamp;
    private String uriKey;
    private String uriValue;

    public CustomUserProfiling(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.parent = cursor.getString(2);
        this.level = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.uriKey = cursor.getString(4);
        this.uriValue = cursor.getString(5);
        this.timestamp = !cursor.isNull(6) ? Long.valueOf(cursor.getLong(6)) : l;
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public CustomUserProfiling(Parcel in) {
        super(in);
        Long l = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.parent = in.readByte() == 0 ? null : in.readString();
        this.level = in.readByte() == 0 ? null : Integer.valueOf(in.readInt());
        this.uriKey = in.readByte() == 0 ? null : in.readString();
        this.uriValue = in.readByte() == 0 ? null : in.readString();
        this.timestamp = in.readByte() != 0 ? Long.valueOf(in.readLong()) : l;
    }

    private CustomUserProfiling(Integer id2, String parent2, Integer level2, String uriKey2, String uriValue2, Long timestamp2) {
        this.id = id2;
        this.parent = parent2;
        this.level = level2;
        this.uriKey = uriKey2;
        this.uriValue = uriValue2;
        this.timestamp = timestamp2;
    }

    public CustomUserProfiling() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id2) {
        this.id = id2;
        setValue();
    }

    public String getParent() {
        return this.parent;
    }

    public void setParent(String parent2) {
        this.parent = parent2;
        setValue();
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(Integer level2) {
        this.level = level2;
        setValue();
    }

    public String getUriKey() {
        return this.uriKey;
    }

    public void setUriKey(String uriKey2) {
        this.uriKey = uriKey2;
        setValue();
    }

    public String getUriValue() {
        return this.uriValue;
    }

    public void setUriValue(String uriValue2) {
        this.uriValue = uriValue2;
        setValue();
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp2) {
        this.timestamp = timestamp2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.parent != null) {
            out.writeByte((byte) 1);
            out.writeString(this.parent);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.level != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.level.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.uriKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.uriKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.uriValue != null) {
            out.writeByte((byte) 1);
            out.writeString(this.uriValue);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timestamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timestamp.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<CustomUserProfiling> getHelper() {
        return CustomUserProfilingHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.pengine.CustomUserProfiling";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CustomUserProfiling { id: ").append(this.id);
        sb.append(", parent: ").append(this.parent);
        sb.append(", level: ").append(this.level);
        sb.append(", uriKey: ").append(this.uriKey);
        sb.append(", uriValue: ").append(this.uriValue);
        sb.append(", timestamp: ").append(this.timestamp);
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
        return "0.0.11";
    }

    public int getDatabaseVersionCode() {
        return 11;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
