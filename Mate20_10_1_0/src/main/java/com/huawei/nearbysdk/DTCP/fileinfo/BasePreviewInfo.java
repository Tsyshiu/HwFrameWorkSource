package com.huawei.nearbysdk.DTCP.fileinfo;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nearbysdk.DTCP.fileinfo.BaseShareInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class BasePreviewInfo implements Parcelable, Serializable, IDTCPSerialize {
    public static final Creator<BasePreviewInfo> CREATOR = new BasePreviewInfoCreator();
    private String mContent;
    protected int mInfoType = 0;
    private String mSender;
    private String mSource;
    private String mType;

    protected BasePreviewInfo() {
    }

    protected BasePreviewInfo(String sender, String type, String content, String source) {
        this.mSender = sender;
        this.mType = type;
        this.mContent = content;
        this.mSource = source;
    }

    protected BasePreviewInfo(Parcel in) {
        this.mSender = in.readString();
        this.mType = in.readString();
        this.mContent = in.readString();
        this.mSource = in.readString();
    }

    public String getContent() {
        return this.mContent;
    }

    public void setContent(String content) {
        this.mContent = content;
    }

    public int getInfoType() {
        return this.mInfoType;
    }

    /* access modifiers changed from: protected */
    public void setInfoType(int type) {
        this.mInfoType = type;
    }

    public String getSource() {
        return this.mSource;
    }

    public void setSource(String source) {
        this.mSource = source;
    }

    public String getSender() {
        return this.mSender;
    }

    public void setSender(String sender) {
        this.mSender = sender;
    }

    public String getType() {
        return this.mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "mSender=" + this.mSender + ", mType=" + this.mType + ", mContent=" + this.mContent + ", mSource=" + this.mSource;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mSender);
        dest.writeString(this.mType);
        dest.writeString(this.mContent);
        dest.writeString(this.mSource);
    }

    static class BasePreviewInfoCreator implements Creator<BasePreviewInfo> {
        BasePreviewInfoCreator() {
        }

        @Override // android.os.Parcelable.Creator
        public BasePreviewInfo createFromParcel(Parcel source) {
            int type = source.readInt();
            switch (type) {
                case 1:
                    return new MediaPreviewInfo(source);
                case 2:
                    return new FilePreviewInfo(source);
                case 3:
                    return new TextPreviewInfo(source);
                default:
                    throw new BaseShareInfo.ShareInfoException("Preview type: " + type + " not found!");
            }
        }

        @Override // android.os.Parcelable.Creator
        public BasePreviewInfo[] newArray(int size) {
            return new BasePreviewInfo[size];
        }
    }

    @Override // com.huawei.nearbysdk.DTCP.fileinfo.IDTCPSerialize
    public void writeToDTCPStream(DataOutputStream dos, int dtcpVersion) throws IOException {
        writeStrToDTCPStream(this.mSender, dos, dtcpVersion);
        writeStrToDTCPStream(this.mType, dos, dtcpVersion);
        writeStrToDTCPStream(this.mContent, dos, dtcpVersion);
        writeStrToDTCPStream(this.mSource, dos, dtcpVersion);
    }

    /* access modifiers changed from: protected */
    public void writeStrToDTCPStream(String str, DataOutputStream dos, int dtcpVersion) throws IOException {
        if (str != null) {
            dos.writeUTF(str);
        } else {
            dos.writeUTF("");
        }
    }

    @Override // com.huawei.nearbysdk.DTCP.fileinfo.IDTCPSerialize
    public void readFromDTCPStream(DataInputStream dis, int dtcpVersion) throws IOException {
        this.mSender = dis.readUTF();
        this.mType = dis.readUTF();
        this.mContent = dis.readUTF();
        this.mSource = dis.readUTF();
    }
}
