package com.huawei.nb.model.authority;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ZTupleAuthorityGrant extends AManagedObject {
    public static final Parcelable.Creator<ZTupleAuthorityGrant> CREATOR = new Parcelable.Creator<ZTupleAuthorityGrant>() {
        /* class com.huawei.nb.model.authority.ZTupleAuthorityGrant.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public ZTupleAuthorityGrant createFromParcel(Parcel in) {
            return new ZTupleAuthorityGrant(in);
        }

        @Override // android.os.Parcelable.Creator
        public ZTupleAuthorityGrant[] newArray(int size) {
            return new ZTupleAuthorityGrant[size];
        }
    };
    private Integer authority;
    private Long id;
    private String packageName;
    private Long packageUid;
    private String reserved;
    private Boolean supportGroupAuthority = true;
    private Long tableId;
    private String tableName;
    private Long tupleId;
    private String tupleName;

    public ZTupleAuthorityGrant(Cursor cursor) {
        Boolean bool = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.tableId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.tableName = cursor.getString(3);
        this.tupleId = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.tupleName = cursor.getString(5);
        this.packageUid = cursor.isNull(6) ? null : Long.valueOf(cursor.getLong(6));
        this.packageName = cursor.getString(7);
        this.authority = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        if (!cursor.isNull(9)) {
            bool = Boolean.valueOf(cursor.getInt(9) != 0);
        }
        this.supportGroupAuthority = bool;
        this.reserved = cursor.getString(10);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public ZTupleAuthorityGrant(Parcel in) {
        super(in);
        Boolean valueOf;
        String str = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.tableId = in.readByte() == 0 ? null : Long.valueOf(in.readLong());
        this.tableName = in.readByte() == 0 ? null : in.readString();
        this.tupleId = in.readByte() == 0 ? null : Long.valueOf(in.readLong());
        this.tupleName = in.readByte() == 0 ? null : in.readString();
        this.packageUid = in.readByte() == 0 ? null : Long.valueOf(in.readLong());
        this.packageName = in.readByte() == 0 ? null : in.readString();
        this.authority = in.readByte() == 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() == 0) {
            valueOf = null;
        } else {
            valueOf = Boolean.valueOf(in.readByte() != 0);
        }
        this.supportGroupAuthority = valueOf;
        this.reserved = in.readByte() != 0 ? in.readString() : str;
    }

    private ZTupleAuthorityGrant(Long id2, Long tableId2, String tableName2, Long tupleId2, String tupleName2, Long packageUid2, String packageName2, Integer authority2, Boolean supportGroupAuthority2, String reserved2) {
        this.id = id2;
        this.tableId = tableId2;
        this.tableName = tableName2;
        this.tupleId = tupleId2;
        this.tupleName = tupleName2;
        this.packageUid = packageUid2;
        this.packageName = packageName2;
        this.authority = authority2;
        this.supportGroupAuthority = supportGroupAuthority2;
        this.reserved = reserved2;
    }

    public ZTupleAuthorityGrant() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id2) {
        this.id = id2;
        setValue();
    }

    public Long getTableId() {
        return this.tableId;
    }

    public void setTableId(Long tableId2) {
        this.tableId = tableId2;
        setValue();
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName2) {
        this.tableName = tableName2;
        setValue();
    }

    public Long getTupleId() {
        return this.tupleId;
    }

    public void setTupleId(Long tupleId2) {
        this.tupleId = tupleId2;
        setValue();
    }

    public String getTupleName() {
        return this.tupleName;
    }

    public void setTupleName(String tupleName2) {
        this.tupleName = tupleName2;
        setValue();
    }

    public Long getPackageUid() {
        return this.packageUid;
    }

    public void setPackageUid(Long packageUid2) {
        this.packageUid = packageUid2;
        setValue();
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName2) {
        this.packageName = packageName2;
        setValue();
    }

    public Integer getAuthority() {
        return this.authority;
    }

    public void setAuthority(Integer authority2) {
        this.authority = authority2;
        setValue();
    }

    public Boolean getSupportGroupAuthority() {
        return this.supportGroupAuthority;
    }

    public void setSupportGroupAuthority(Boolean supportGroupAuthority2) {
        this.supportGroupAuthority = supportGroupAuthority2;
        setValue();
    }

    public String getReserved() {
        return this.reserved;
    }

    public void setReserved(String reserved2) {
        this.reserved = reserved2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public void writeToParcel(Parcel out, int ignored) {
        byte b;
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.tableId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.tableId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tableName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tableName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tupleId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.tupleId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tupleName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tupleName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageUid != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.packageUid.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.packageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.authority != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.authority.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.supportGroupAuthority != null) {
            out.writeByte((byte) 1);
            if (this.supportGroupAuthority.booleanValue()) {
                b = 1;
            } else {
                b = 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved);
            return;
        }
        out.writeByte((byte) 0);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<ZTupleAuthorityGrant> getHelper() {
        return ZTupleAuthorityGrantHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.authority.ZTupleAuthorityGrant";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ZTupleAuthorityGrant { id: ").append(this.id);
        sb.append(", tableId: ").append(this.tableId);
        sb.append(", tableName: ").append(this.tableName);
        sb.append(", tupleId: ").append(this.tupleId);
        sb.append(", tupleName: ").append(this.tupleName);
        sb.append(", packageUid: ").append(this.packageUid);
        sb.append(", packageName: ").append(this.packageName);
        sb.append(", authority: ").append(this.authority);
        sb.append(", supportGroupAuthority: ").append(this.supportGroupAuthority);
        sb.append(", reserved: ").append(this.reserved);
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
        return "0.0.17";
    }

    public int getDatabaseVersionCode() {
        return 17;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
