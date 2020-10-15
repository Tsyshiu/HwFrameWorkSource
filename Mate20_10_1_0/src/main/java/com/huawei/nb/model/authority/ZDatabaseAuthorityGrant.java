package com.huawei.nb.model.authority;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ZDatabaseAuthorityGrant extends AManagedObject {
    public static final Parcelable.Creator<ZDatabaseAuthorityGrant> CREATOR = new Parcelable.Creator<ZDatabaseAuthorityGrant>() {
        /* class com.huawei.nb.model.authority.ZDatabaseAuthorityGrant.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public ZDatabaseAuthorityGrant createFromParcel(Parcel in) {
            return new ZDatabaseAuthorityGrant(in);
        }

        @Override // android.os.Parcelable.Creator
        public ZDatabaseAuthorityGrant[] newArray(int size) {
            return new ZDatabaseAuthorityGrant[size];
        }
    };
    private Integer authority;
    private Long dbId;
    private String dbName;
    private Long id;
    private String packageName;
    private Long packageUid;
    private String reserved;
    private Boolean supportGroupAuthority = true;

    public ZDatabaseAuthorityGrant(Cursor cursor) {
        Boolean bool = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.dbId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.dbName = cursor.getString(3);
        this.packageUid = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.packageName = cursor.getString(5);
        this.authority = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            bool = Boolean.valueOf(cursor.getInt(7) != 0);
        }
        this.supportGroupAuthority = bool;
        this.reserved = cursor.getString(8);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public ZDatabaseAuthorityGrant(Parcel in) {
        super(in);
        Boolean valueOf;
        String str = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.dbId = in.readByte() == 0 ? null : Long.valueOf(in.readLong());
        this.dbName = in.readByte() == 0 ? null : in.readString();
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

    private ZDatabaseAuthorityGrant(Long id2, Long dbId2, String dbName2, Long packageUid2, String packageName2, Integer authority2, Boolean supportGroupAuthority2, String reserved2) {
        this.id = id2;
        this.dbId = dbId2;
        this.dbName = dbName2;
        this.packageUid = packageUid2;
        this.packageName = packageName2;
        this.authority = authority2;
        this.supportGroupAuthority = supportGroupAuthority2;
        this.reserved = reserved2;
    }

    public ZDatabaseAuthorityGrant() {
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

    public Long getDbId() {
        return this.dbId;
    }

    public void setDbId(Long dbId2) {
        this.dbId = dbId2;
        setValue();
    }

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName2) {
        this.dbName = dbName2;
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
        if (this.dbId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.dbId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dbName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dbName);
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
    public AEntityHelper<ZDatabaseAuthorityGrant> getHelper() {
        return ZDatabaseAuthorityGrantHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.authority.ZDatabaseAuthorityGrant";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ZDatabaseAuthorityGrant { id: ").append(this.id);
        sb.append(", dbId: ").append(this.dbId);
        sb.append(", dbName: ").append(this.dbName);
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
