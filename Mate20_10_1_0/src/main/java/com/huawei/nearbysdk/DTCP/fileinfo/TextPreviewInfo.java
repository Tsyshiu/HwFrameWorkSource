package com.huawei.nearbysdk.DTCP.fileinfo;

import android.os.Parcel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TextPreviewInfo extends BasePreviewInfo {
    private String mActualContent;

    public TextPreviewInfo() {
        this.mInfoType = 3;
    }

    public TextPreviewInfo(String sender, String type, String content, String actualContent, String source) {
        super(sender, type, content, source);
        this.mActualContent = actualContent;
        this.mInfoType = 3;
    }

    public String getActualContent() {
        return this.mActualContent;
    }

    public void setActualContent(String actualContent) {
        this.mActualContent = actualContent;
    }

    @Override // com.huawei.nearbysdk.DTCP.fileinfo.BasePreviewInfo
    public String toString() {
        return super.toString() + ", mActualContent=" + this.mActualContent;
    }

    @Override // com.huawei.nearbysdk.DTCP.fileinfo.BasePreviewInfo
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mInfoType);
        super.writeToParcel(dest, flags);
        dest.writeString(this.mActualContent);
    }

    protected TextPreviewInfo(Parcel in) {
        super(in);
        this.mActualContent = in.readString();
        this.mInfoType = 3;
    }

    @Override // com.huawei.nearbysdk.DTCP.fileinfo.IDTCPSerialize, com.huawei.nearbysdk.DTCP.fileinfo.BasePreviewInfo
    public void writeToDTCPStream(DataOutputStream dos, int dtcpVersion) throws IOException {
        super.writeToDTCPStream(dos, dtcpVersion);
        writeStrToDTCPStream(this.mActualContent, dos, dtcpVersion);
    }

    @Override // com.huawei.nearbysdk.DTCP.fileinfo.IDTCPSerialize, com.huawei.nearbysdk.DTCP.fileinfo.BasePreviewInfo
    public void readFromDTCPStream(DataInputStream dis, int dtcpVersion) throws IOException {
        super.readFromDTCPStream(dis, dtcpVersion);
        this.mActualContent = dis.readUTF();
    }
}
