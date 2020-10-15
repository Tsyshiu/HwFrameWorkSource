package vendor.huawei.hardware.eid.V1_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CUT_COORDINATE_S {
    public int down;
    public int left;
    public int right;
    public int up;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != CUT_COORDINATE_S.class) {
            return false;
        }
        CUT_COORDINATE_S other = (CUT_COORDINATE_S) otherObject;
        if (this.up == other.up && this.down == other.down && this.left == other.left && this.right == other.right) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.up))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.down))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.left))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.right))));
    }

    public final String toString() {
        return "{" + ".up = " + this.up + ", .down = " + this.down + ", .left = " + this.left + ", .right = " + this.right + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(16), 0);
    }

    public static final ArrayList<CUT_COORDINATE_S> readVectorFromParcel(HwParcel parcel) {
        ArrayList<CUT_COORDINATE_S> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 16), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            CUT_COORDINATE_S _hidl_vec_element = new CUT_COORDINATE_S();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 16));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.up = _hidl_blob.getInt32(0 + _hidl_offset);
        this.down = _hidl_blob.getInt32(4 + _hidl_offset);
        this.left = _hidl_blob.getInt32(8 + _hidl_offset);
        this.right = _hidl_blob.getInt32(12 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(16);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<CUT_COORDINATE_S> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 16);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 16));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.up);
        _hidl_blob.putInt32(4 + _hidl_offset, this.down);
        _hidl_blob.putInt32(8 + _hidl_offset, this.left);
        _hidl_blob.putInt32(12 + _hidl_offset, this.right);
    }
}
