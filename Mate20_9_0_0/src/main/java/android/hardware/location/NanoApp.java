package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;

@SystemApi
@Deprecated
public class NanoApp implements Parcelable {
    public static final Creator<NanoApp> CREATOR = new Creator<NanoApp>() {
        public NanoApp createFromParcel(Parcel in) {
            return new NanoApp(in, null);
        }

        public NanoApp[] newArray(int size) {
            return new NanoApp[size];
        }
    };
    private final String TAG;
    private final String UNKNOWN;
    private byte[] mAppBinary;
    private long mAppId;
    private boolean mAppIdSet;
    private int mAppVersion;
    private String mName;
    private int mNeededExecMemBytes;
    private int mNeededReadMemBytes;
    private int[] mNeededSensors;
    private int mNeededWriteMemBytes;
    private int[] mOutputEvents;
    private String mPublisher;

    public NanoApp() {
        this(0, null);
        this.mAppIdSet = false;
    }

    @Deprecated
    public NanoApp(int appId, byte[] appBinary) {
        this.TAG = "NanoApp";
        this.UNKNOWN = "Unknown";
        Log.w("NanoApp", "NanoApp(int, byte[]) is deprecated, please use NanoApp(long, byte[]) instead.");
    }

    public NanoApp(long appId, byte[] appBinary) {
        this.TAG = "NanoApp";
        this.UNKNOWN = "Unknown";
        this.mPublisher = "Unknown";
        this.mName = "Unknown";
        this.mAppId = appId;
        this.mAppIdSet = true;
        this.mAppVersion = 0;
        this.mNeededReadMemBytes = 0;
        this.mNeededWriteMemBytes = 0;
        this.mNeededExecMemBytes = 0;
        this.mNeededSensors = new int[0];
        this.mOutputEvents = new int[0];
        this.mAppBinary = appBinary;
    }

    public void setPublisher(String publisher) {
        this.mPublisher = publisher;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setAppId(long appId) {
        this.mAppId = appId;
        this.mAppIdSet = true;
    }

    public void setAppVersion(int appVersion) {
        this.mAppVersion = appVersion;
    }

    public void setNeededReadMemBytes(int neededReadMemBytes) {
        this.mNeededReadMemBytes = neededReadMemBytes;
    }

    public void setNeededWriteMemBytes(int neededWriteMemBytes) {
        this.mNeededWriteMemBytes = neededWriteMemBytes;
    }

    public void setNeededExecMemBytes(int neededExecMemBytes) {
        this.mNeededExecMemBytes = neededExecMemBytes;
    }

    public void setNeededSensors(int[] neededSensors) {
        this.mNeededSensors = neededSensors;
    }

    public void setOutputEvents(int[] outputEvents) {
        this.mOutputEvents = outputEvents;
    }

    public void setAppBinary(byte[] appBinary) {
        this.mAppBinary = appBinary;
    }

    public String getPublisher() {
        return this.mPublisher;
    }

    public String getName() {
        return this.mName;
    }

    public long getAppId() {
        return this.mAppId;
    }

    public int getAppVersion() {
        return this.mAppVersion;
    }

    public int getNeededReadMemBytes() {
        return this.mNeededReadMemBytes;
    }

    public int getNeededWriteMemBytes() {
        return this.mNeededWriteMemBytes;
    }

    public int getNeededExecMemBytes() {
        return this.mNeededExecMemBytes;
    }

    public int[] getNeededSensors() {
        return this.mNeededSensors;
    }

    public int[] getOutputEvents() {
        return this.mOutputEvents;
    }

    public byte[] getAppBinary() {
        return this.mAppBinary;
    }

    private NanoApp(Parcel in) {
        this.TAG = "NanoApp";
        this.UNKNOWN = "Unknown";
        this.mPublisher = in.readString();
        this.mName = in.readString();
        this.mAppId = in.readLong();
        this.mAppVersion = in.readInt();
        this.mNeededReadMemBytes = in.readInt();
        this.mNeededWriteMemBytes = in.readInt();
        this.mNeededExecMemBytes = in.readInt();
        this.mNeededSensors = new int[in.readInt()];
        in.readIntArray(this.mNeededSensors);
        this.mOutputEvents = new int[in.readInt()];
        in.readIntArray(this.mOutputEvents);
        this.mAppBinary = new byte[in.readInt()];
        in.readByteArray(this.mAppBinary);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        StringBuilder stringBuilder;
        if (this.mAppBinary == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Must set non-null AppBinary for nanoapp ");
            stringBuilder.append(this.mName);
            throw new IllegalStateException(stringBuilder.toString());
        } else if (this.mAppIdSet) {
            out.writeString(this.mPublisher);
            out.writeString(this.mName);
            out.writeLong(this.mAppId);
            out.writeInt(this.mAppVersion);
            out.writeInt(this.mNeededReadMemBytes);
            out.writeInt(this.mNeededWriteMemBytes);
            out.writeInt(this.mNeededExecMemBytes);
            out.writeInt(this.mNeededSensors.length);
            out.writeIntArray(this.mNeededSensors);
            out.writeInt(this.mOutputEvents.length);
            out.writeIntArray(this.mOutputEvents);
            out.writeInt(this.mAppBinary.length);
            out.writeByteArray(this.mAppBinary);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Must set AppId for nanoapp ");
            stringBuilder.append(this.mName);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public String toString() {
        String retVal = new StringBuilder();
        retVal.append("Id : ");
        retVal.append(this.mAppId);
        retVal = retVal.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(retVal);
        stringBuilder.append(", Version : ");
        stringBuilder.append(this.mAppVersion);
        retVal = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(retVal);
        stringBuilder.append(", Name : ");
        stringBuilder.append(this.mName);
        retVal = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(retVal);
        stringBuilder.append(", Publisher : ");
        stringBuilder.append(this.mPublisher);
        return stringBuilder.toString();
    }
}
