package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class DisplayEngineHdr10MetadataParam {
    public int colorPrimaries;
    public int[] displayPrimariesX = new int[3];
    public int[] displayPrimariesY = new int[3];
    public int isAvailable;
    public int isModified;
    public int matrixCoeffs;
    public int maxDisplayMasteringLuminance;
    public int minDisplayMasteringLuminance;
    public int roiH;
    public int roiMode;
    public int roiW;
    public int roiX;
    public int roiY;
    public int transferCharacteristics;
    public byte videoFullRangeFlag;
    public int whitePointX;
    public int whitePointY;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != DisplayEngineHdr10MetadataParam.class) {
            return false;
        }
        DisplayEngineHdr10MetadataParam other = (DisplayEngineHdr10MetadataParam) otherObject;
        if (this.isAvailable == other.isAvailable && HidlSupport.deepEquals(this.displayPrimariesX, other.displayPrimariesX) && HidlSupport.deepEquals(this.displayPrimariesY, other.displayPrimariesY) && this.whitePointX == other.whitePointX && this.whitePointY == other.whitePointY && this.maxDisplayMasteringLuminance == other.maxDisplayMasteringLuminance && this.minDisplayMasteringLuminance == other.minDisplayMasteringLuminance && this.videoFullRangeFlag == other.videoFullRangeFlag && this.colorPrimaries == other.colorPrimaries && this.transferCharacteristics == other.transferCharacteristics && this.matrixCoeffs == other.matrixCoeffs && this.roiMode == other.roiMode && this.roiX == other.roiX && this.roiY == other.roiY && this.roiW == other.roiW && this.roiH == other.roiH && this.isModified == other.isModified) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.isAvailable))), Integer.valueOf(HidlSupport.deepHashCode(this.displayPrimariesX)), Integer.valueOf(HidlSupport.deepHashCode(this.displayPrimariesY)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.whitePointX))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.whitePointY))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxDisplayMasteringLuminance))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.minDisplayMasteringLuminance))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.videoFullRangeFlag))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.colorPrimaries))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.transferCharacteristics))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.matrixCoeffs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.roiMode))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.roiX))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.roiY))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.roiW))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.roiH))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.isModified))));
    }

    public final String toString() {
        return "{" + ".isAvailable = " + this.isAvailable + ", .displayPrimariesX = " + Arrays.toString(this.displayPrimariesX) + ", .displayPrimariesY = " + Arrays.toString(this.displayPrimariesY) + ", .whitePointX = " + this.whitePointX + ", .whitePointY = " + this.whitePointY + ", .maxDisplayMasteringLuminance = " + this.maxDisplayMasteringLuminance + ", .minDisplayMasteringLuminance = " + this.minDisplayMasteringLuminance + ", .videoFullRangeFlag = " + ((int) this.videoFullRangeFlag) + ", .colorPrimaries = " + this.colorPrimaries + ", .transferCharacteristics = " + this.transferCharacteristics + ", .matrixCoeffs = " + this.matrixCoeffs + ", .roiMode = " + this.roiMode + ", .roiX = " + this.roiX + ", .roiY = " + this.roiY + ", .roiW = " + this.roiW + ", .roiH = " + this.roiH + ", .isModified = " + this.isModified + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(84), 0);
    }

    public static final ArrayList<DisplayEngineHdr10MetadataParam> readVectorFromParcel(HwParcel parcel) {
        ArrayList<DisplayEngineHdr10MetadataParam> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 84), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            DisplayEngineHdr10MetadataParam _hidl_vec_element = new DisplayEngineHdr10MetadataParam();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 84));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.isAvailable = _hidl_blob.getInt32(0 + _hidl_offset);
        _hidl_blob.copyToInt32Array(4 + _hidl_offset, this.displayPrimariesX, 3);
        _hidl_blob.copyToInt32Array(16 + _hidl_offset, this.displayPrimariesY, 3);
        this.whitePointX = _hidl_blob.getInt32(28 + _hidl_offset);
        this.whitePointY = _hidl_blob.getInt32(32 + _hidl_offset);
        this.maxDisplayMasteringLuminance = _hidl_blob.getInt32(36 + _hidl_offset);
        this.minDisplayMasteringLuminance = _hidl_blob.getInt32(40 + _hidl_offset);
        this.videoFullRangeFlag = _hidl_blob.getInt8(44 + _hidl_offset);
        this.colorPrimaries = _hidl_blob.getInt32(48 + _hidl_offset);
        this.transferCharacteristics = _hidl_blob.getInt32(52 + _hidl_offset);
        this.matrixCoeffs = _hidl_blob.getInt32(56 + _hidl_offset);
        this.roiMode = _hidl_blob.getInt32(60 + _hidl_offset);
        this.roiX = _hidl_blob.getInt32(64 + _hidl_offset);
        this.roiY = _hidl_blob.getInt32(68 + _hidl_offset);
        this.roiW = _hidl_blob.getInt32(72 + _hidl_offset);
        this.roiH = _hidl_blob.getInt32(76 + _hidl_offset);
        this.isModified = _hidl_blob.getInt32(80 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(84);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<DisplayEngineHdr10MetadataParam> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 84);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 84));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.isAvailable);
        long _hidl_array_offset_0 = 4 + _hidl_offset;
        int[] _hidl_array_item_0 = this.displayPrimariesX;
        if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 3) {
            throw new IllegalArgumentException("Array element is not of the expected length");
        }
        _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
        long _hidl_array_offset_02 = 16 + _hidl_offset;
        int[] _hidl_array_item_02 = this.displayPrimariesY;
        if (_hidl_array_item_02 == null || _hidl_array_item_02.length != 3) {
            throw new IllegalArgumentException("Array element is not of the expected length");
        }
        _hidl_blob.putInt32Array(_hidl_array_offset_02, _hidl_array_item_02);
        _hidl_blob.putInt32(28 + _hidl_offset, this.whitePointX);
        _hidl_blob.putInt32(32 + _hidl_offset, this.whitePointY);
        _hidl_blob.putInt32(36 + _hidl_offset, this.maxDisplayMasteringLuminance);
        _hidl_blob.putInt32(40 + _hidl_offset, this.minDisplayMasteringLuminance);
        _hidl_blob.putInt8(44 + _hidl_offset, this.videoFullRangeFlag);
        _hidl_blob.putInt32(48 + _hidl_offset, this.colorPrimaries);
        _hidl_blob.putInt32(52 + _hidl_offset, this.transferCharacteristics);
        _hidl_blob.putInt32(56 + _hidl_offset, this.matrixCoeffs);
        _hidl_blob.putInt32(60 + _hidl_offset, this.roiMode);
        _hidl_blob.putInt32(64 + _hidl_offset, this.roiX);
        _hidl_blob.putInt32(68 + _hidl_offset, this.roiY);
        _hidl_blob.putInt32(72 + _hidl_offset, this.roiW);
        _hidl_blob.putInt32(76 + _hidl_offset, this.roiH);
        _hidl_blob.putInt32(80 + _hidl_offset, this.isModified);
    }
}
