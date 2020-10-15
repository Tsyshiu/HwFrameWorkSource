package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaSysEvent extends AManagedObject {
    public static final Parcelable.Creator<MetaSysEvent> CREATOR = new Parcelable.Creator<MetaSysEvent>() {
        /* class com.huawei.nb.model.collectencrypt.MetaSysEvent.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public MetaSysEvent createFromParcel(Parcel in) {
            return new MetaSysEvent(in);
        }

        @Override // android.os.Parcelable.Creator
        public MetaSysEvent[] newArray(int size) {
            return new MetaSysEvent[size];
        }
    };
    private String mEventName;
    private String mEventParam;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public MetaSysEvent(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mEventName = cursor.getString(3);
        this.mEventParam = cursor.getString(4);
        this.mReservedInt = !cursor.isNull(5) ? Integer.valueOf(cursor.getInt(5)) : num;
        this.mReservedText = cursor.getString(6);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public MetaSysEvent(Parcel in) {
        super(in);
        String str = null;
        if (in.readByte() == 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == 0 ? null : new Date(in.readLong());
        this.mEventName = in.readByte() == 0 ? null : in.readString();
        this.mEventParam = in.readByte() == 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedText = in.readByte() != 0 ? in.readString() : str;
    }

    private MetaSysEvent(Integer mId2, Date mTimeStamp2, String mEventName2, String mEventParam2, Integer mReservedInt2, String mReservedText2) {
        this.mId = mId2;
        this.mTimeStamp = mTimeStamp2;
        this.mEventName = mEventName2;
        this.mEventParam = mEventParam2;
        this.mReservedInt = mReservedInt2;
        this.mReservedText = mReservedText2;
    }

    public MetaSysEvent() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId2) {
        this.mId = mId2;
        setValue();
    }

    public Date getMTimeStamp() {
        return this.mTimeStamp;
    }

    public void setMTimeStamp(Date mTimeStamp2) {
        this.mTimeStamp = mTimeStamp2;
        setValue();
    }

    public String getMEventName() {
        return this.mEventName;
    }

    public void setMEventName(String mEventName2) {
        this.mEventName = mEventName2;
        setValue();
    }

    public String getMEventParam() {
        return this.mEventParam;
    }

    public void setMEventParam(String mEventParam2) {
        this.mEventParam = mEventParam2;
        setValue();
    }

    public Integer getMReservedInt() {
        return this.mReservedInt;
    }

    public void setMReservedInt(Integer mReservedInt2) {
        this.mReservedInt = mReservedInt2;
        setValue();
    }

    public String getMReservedText() {
        return this.mReservedText;
    }

    public void setMReservedText(String mReservedText2) {
        this.mReservedText = mReservedText2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mTimeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTimeStamp.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEventName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventParam != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEventParam);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedInt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReservedInt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReservedText);
            return;
        }
        out.writeByte((byte) 0);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<MetaSysEvent> getHelper() {
        return MetaSysEventHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaSysEvent";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaSysEvent { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mEventName: ").append(this.mEventName);
        sb.append(", mEventParam: ").append(this.mEventParam);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
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
        return "0.0.14";
    }

    public int getDatabaseVersionCode() {
        return 14;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
