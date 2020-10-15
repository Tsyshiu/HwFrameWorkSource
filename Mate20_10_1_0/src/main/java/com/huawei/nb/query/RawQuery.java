package com.huawei.nb.query;

import android.os.Parcel;
import android.os.Parcelable;

public class RawQuery implements IQuery {
    public static final Creator<RawQuery> CREATOR = new Creator<RawQuery>() {
        /* class com.huawei.nb.query.RawQuery.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public RawQuery createFromParcel(Parcel in) {
            return new RawQuery(in);
        }

        @Override // android.os.Parcelable.Creator
        public RawQuery[] newArray(int size) {
            return new RawQuery[size];
        }
    };
    private String dbName;
    private String rawSQL;

    private RawQuery() {
    }

    protected RawQuery(Parcel in) {
        this.dbName = in.readString();
        this.rawSQL = in.readString();
    }

    public static RawQuery select(String rawSQL2) {
        RawQuery query = new RawQuery();
        query.rawSQL = rawSQL2;
        return query;
    }

    public RawQuery from(String dbName2) {
        this.dbName = dbName2;
        return this;
    }

    public String getDbName() {
        return this.dbName;
    }

    public String getRawSQL() {
        return this.rawSQL;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.dbName);
        dest.writeString(this.rawSQL);
    }

    @Override // com.huawei.nb.query.IQuery
    public String getEntityName() {
        return null;
    }

    @Override // com.huawei.nb.query.IQuery
    public boolean isValid() {
        return true;
    }
}
