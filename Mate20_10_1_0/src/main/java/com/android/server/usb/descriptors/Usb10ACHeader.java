package com.android.server.usb.descriptors;

import com.android.server.devicepolicy.HwLog;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACHeader extends UsbACHeaderInterface {
    private static final String TAG = "Usb10ACHeader";
    private byte mControls;
    private byte[] mInterfaceNums = null;
    private byte mNumInterfaces = 0;

    public Usb10ACHeader(int length, byte type, byte subtype, int subclass, int spec) {
        super(length, type, subtype, subclass, spec);
    }

    public byte getNumInterfaces() {
        return this.mNumInterfaces;
    }

    public byte[] getInterfaceNums() {
        return this.mInterfaceNums;
    }

    public byte getControls() {
        return this.mControls;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        this.mTotalLength = stream.unpackUsbShort();
        if (this.mADCRelease >= 512) {
            this.mControls = stream.getByte();
        } else {
            this.mNumInterfaces = stream.getByte();
            this.mInterfaceNums = new byte[this.mNumInterfaces];
            for (int index = 0; index < this.mNumInterfaces; index++) {
                this.mInterfaceNums[index] = stream.getByte();
            }
        }
        return this.mLength;
    }

    @Override // com.android.server.usb.descriptors.report.Reporting, com.android.server.usb.descriptors.UsbACHeaderInterface, com.android.server.usb.descriptors.UsbACInterface, com.android.server.usb.descriptors.UsbDescriptor
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        int numInterfaces = getNumInterfaces();
        StringBuilder sb = new StringBuilder();
        sb.append("" + numInterfaces + " Interfaces");
        if (numInterfaces > 0) {
            sb.append(" [");
            byte[] interfaceNums = getInterfaceNums();
            if (interfaceNums != null) {
                for (int index = 0; index < numInterfaces; index++) {
                    sb.append("" + ((int) interfaceNums[index]));
                    if (index < numInterfaces - 1) {
                        sb.append(HwLog.PREFIX);
                    }
                }
            }
            sb.append("]");
        }
        canvas.writeListItem(sb.toString());
        canvas.writeListItem("Controls: " + ReportCanvas.getHexString(getControls()));
        canvas.closeList();
    }
}
