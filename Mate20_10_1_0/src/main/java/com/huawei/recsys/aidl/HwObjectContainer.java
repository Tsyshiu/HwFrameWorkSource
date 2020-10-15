package com.huawei.recsys.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HwObjectContainer<T> implements Parcelable {
    public static final Creator<HwObjectContainer> CREATOR = new Creator<HwObjectContainer>() {
        /* class com.huawei.recsys.aidl.HwObjectContainer.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public HwObjectContainer createFromParcel(Parcel in) {
            return new HwObjectContainer(in);
        }

        @Override // android.os.Parcelable.Creator
        public HwObjectContainer[] newArray(int size) {
            return new HwObjectContainer[size];
        }
    };
    private Class<T> clazz;
    private List<T> objects;

    public HwObjectContainer(Class<T> clazz2, List<T> objects2) {
        if (clazz2 == null || objects2 == null) {
            throw new IllegalArgumentException();
        }
        this.clazz = clazz2;
        this.objects = objects2;
    }

    public HwObjectContainer(Class<T> clazz2) {
        this(clazz2, new ArrayList());
    }

    public HwObjectContainer add(T object) {
        if (object != null) {
            this.objects.add(object);
        }
        return this;
    }

    public void clearObjects() {
        this.objects.clear();
    }

    protected HwObjectContainer(Parcel in) {
        this.clazz = (Class) in.readSerializable();
        Class<T> cls = this.clazz;
        if (cls != null) {
            this.objects = in.readArrayList(cls.getClassLoader());
        } else {
            this.objects = Collections.emptyList();
        }
    }

    public Class type() {
        return this.clazz;
    }

    public List<T> get() {
        return this.objects;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(this.clazz);
        dest.writeList(this.objects);
    }

    public int describeContents() {
        return 0;
    }
}
