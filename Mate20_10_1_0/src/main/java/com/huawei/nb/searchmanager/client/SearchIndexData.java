package com.huawei.nb.searchmanager.client;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;

public class SearchIndexData implements Parcelable {
    public static final Creator<SearchIndexData> CREATOR = new Creator<SearchIndexData>() {
        /* class com.huawei.nb.searchmanager.client.SearchIndexData.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public SearchIndexData createFromParcel(Parcel in) {
            return new SearchIndexData(in);
        }

        @Override // android.os.Parcelable.Creator
        public SearchIndexData[] newArray(int size) {
            return new SearchIndexData[size];
        }
    };
    private int dataType;
    private HashMap<String, String> fieldMap;
    private boolean status;

    public SearchIndexData() {
    }

    protected SearchIndexData(Parcel in) {
        this.dataType = in.readInt();
        this.status = in.readByte() != 0;
        this.fieldMap = in.readHashMap(HashMap.class.getClassLoader());
    }

    public HashMap<String, String> getFieldMap() {
        return this.fieldMap;
    }

    public void setFieldMap(HashMap<String, String> fieldMap2) {
        this.fieldMap = fieldMap2;
    }

    public int getDataType() {
        return this.dataType;
    }

    public void setDataType(int dataType2) {
        this.dataType = dataType2;
    }

    public boolean isStatus() {
        return this.status;
    }

    public void setStatus(boolean status2) {
        this.status = status2;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.dataType);
        dest.writeByte((byte) (this.status ? 1 : 0));
        dest.writeMap(this.fieldMap);
    }

    public int describeContents() {
        return 0;
    }
}
