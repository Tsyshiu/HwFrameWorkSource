package org.bouncycastle.math.ec.custom.gm;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractFp;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat256;

public class SM2P256V1Point extends AbstractFp {
    public SM2P256V1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public SM2P256V1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        super(eCCurve, eCFieldElement, eCFieldElement2);
        Object obj = null;
        Object obj2 = eCFieldElement == null ? 1 : null;
        if (eCFieldElement2 == null) {
            obj = 1;
        }
        if (obj2 == obj) {
            this.withCompression = z;
            return;
        }
        throw new IllegalArgumentException("Exactly one of the field elements is null");
    }

    SM2P256V1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
        this.withCompression = z;
    }

    public ECPoint add(ECPoint eCPoint) {
        if (isInfinity()) {
            return eCPoint;
        }
        if (eCPoint.isInfinity()) {
            return this;
        }
        if (this == eCPoint) {
            return twice();
        }
        int[] iArr;
        int[] iArr2;
        int[] iArr3;
        int[] iArr4;
        ECCurve curve = getCurve();
        SM2P256V1FieldElement sM2P256V1FieldElement = (SM2P256V1FieldElement) this.x;
        SM2P256V1FieldElement sM2P256V1FieldElement2 = (SM2P256V1FieldElement) this.y;
        SM2P256V1FieldElement sM2P256V1FieldElement3 = (SM2P256V1FieldElement) eCPoint.getXCoord();
        SM2P256V1FieldElement sM2P256V1FieldElement4 = (SM2P256V1FieldElement) eCPoint.getYCoord();
        SM2P256V1FieldElement sM2P256V1FieldElement5 = (SM2P256V1FieldElement) this.zs[0];
        SM2P256V1FieldElement sM2P256V1FieldElement6 = (SM2P256V1FieldElement) eCPoint.getZCoord(0);
        int[] createExt = Nat256.createExt();
        int[] create = Nat256.create();
        int[] create2 = Nat256.create();
        int[] create3 = Nat256.create();
        boolean isOne = sM2P256V1FieldElement5.isOne();
        if (isOne) {
            iArr = sM2P256V1FieldElement3.x;
            iArr2 = sM2P256V1FieldElement4.x;
        } else {
            SM2P256V1Field.square(sM2P256V1FieldElement5.x, create2);
            SM2P256V1Field.multiply(create2, sM2P256V1FieldElement3.x, create);
            SM2P256V1Field.multiply(create2, sM2P256V1FieldElement5.x, create2);
            SM2P256V1Field.multiply(create2, sM2P256V1FieldElement4.x, create2);
            iArr = create;
            iArr2 = create2;
        }
        boolean isOne2 = sM2P256V1FieldElement6.isOne();
        if (isOne2) {
            iArr3 = sM2P256V1FieldElement.x;
            iArr4 = sM2P256V1FieldElement2.x;
        } else {
            SM2P256V1Field.square(sM2P256V1FieldElement6.x, create3);
            SM2P256V1Field.multiply(create3, sM2P256V1FieldElement.x, createExt);
            SM2P256V1Field.multiply(create3, sM2P256V1FieldElement6.x, create3);
            SM2P256V1Field.multiply(create3, sM2P256V1FieldElement2.x, create3);
            iArr3 = createExt;
            iArr4 = create3;
        }
        int[] create4 = Nat256.create();
        SM2P256V1Field.subtract(iArr3, iArr, create4);
        SM2P256V1Field.subtract(iArr4, iArr2, create);
        if (Nat256.isZero(create4)) {
            return Nat256.isZero(create) ? twice() : curve.getInfinity();
        } else {
            SM2P256V1Field.square(create4, create2);
            iArr = Nat256.create();
            SM2P256V1Field.multiply(create2, create4, iArr);
            SM2P256V1Field.multiply(create2, iArr3, create2);
            SM2P256V1Field.negate(iArr, iArr);
            Nat256.mul(iArr4, iArr, createExt);
            SM2P256V1Field.reduce32(Nat256.addBothTo(create2, create2, iArr), iArr);
            sM2P256V1FieldElement2 = new SM2P256V1FieldElement(create3);
            SM2P256V1Field.square(create, sM2P256V1FieldElement2.x);
            SM2P256V1Field.subtract(sM2P256V1FieldElement2.x, iArr, sM2P256V1FieldElement2.x);
            ECFieldElement sM2P256V1FieldElement7 = new SM2P256V1FieldElement(iArr);
            SM2P256V1Field.subtract(create2, sM2P256V1FieldElement2.x, sM2P256V1FieldElement7.x);
            SM2P256V1Field.multiplyAddToExt(sM2P256V1FieldElement7.x, create, createExt);
            SM2P256V1Field.reduce(createExt, sM2P256V1FieldElement7.x);
            sM2P256V1FieldElement = new SM2P256V1FieldElement(create4);
            if (!isOne) {
                SM2P256V1Field.multiply(sM2P256V1FieldElement.x, sM2P256V1FieldElement5.x, sM2P256V1FieldElement.x);
            }
            if (!isOne2) {
                SM2P256V1Field.multiply(sM2P256V1FieldElement.x, sM2P256V1FieldElement6.x, sM2P256V1FieldElement.x);
            }
            return new SM2P256V1Point(curve, sM2P256V1FieldElement2, sM2P256V1FieldElement7, new ECFieldElement[]{sM2P256V1FieldElement}, this.withCompression);
        }
    }

    protected ECPoint detach() {
        return new SM2P256V1Point(null, getAffineXCoord(), getAffineYCoord());
    }

    public ECPoint negate() {
        return isInfinity() ? this : new SM2P256V1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }

    public ECPoint threeTimes() {
        return (isInfinity() || this.y.isZero()) ? this : twice().add(this);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        SM2P256V1FieldElement sM2P256V1FieldElement = (SM2P256V1FieldElement) this.y;
        if (sM2P256V1FieldElement.isZero()) {
            return curve.getInfinity();
        }
        SM2P256V1FieldElement sM2P256V1FieldElement2 = (SM2P256V1FieldElement) this.x;
        SM2P256V1FieldElement sM2P256V1FieldElement3 = (SM2P256V1FieldElement) this.zs[0];
        int[] create = Nat256.create();
        int[] create2 = Nat256.create();
        int[] create3 = Nat256.create();
        SM2P256V1Field.square(sM2P256V1FieldElement.x, create3);
        int[] create4 = Nat256.create();
        SM2P256V1Field.square(create3, create4);
        boolean isOne = sM2P256V1FieldElement3.isOne();
        int[] iArr = sM2P256V1FieldElement3.x;
        if (!isOne) {
            SM2P256V1Field.square(sM2P256V1FieldElement3.x, create2);
            iArr = create2;
        }
        SM2P256V1Field.subtract(sM2P256V1FieldElement2.x, iArr, create);
        SM2P256V1Field.add(sM2P256V1FieldElement2.x, iArr, create2);
        SM2P256V1Field.multiply(create2, create, create2);
        SM2P256V1Field.reduce32(Nat256.addBothTo(create2, create2, create2), create2);
        SM2P256V1Field.multiply(create3, sM2P256V1FieldElement2.x, create3);
        SM2P256V1Field.reduce32(Nat.shiftUpBits(8, create3, 2, 0), create3);
        SM2P256V1Field.reduce32(Nat.shiftUpBits(8, create4, 3, 0, create), create);
        ECFieldElement sM2P256V1FieldElement4 = new SM2P256V1FieldElement(create4);
        SM2P256V1Field.square(create2, sM2P256V1FieldElement4.x);
        SM2P256V1Field.subtract(sM2P256V1FieldElement4.x, create3, sM2P256V1FieldElement4.x);
        SM2P256V1Field.subtract(sM2P256V1FieldElement4.x, create3, sM2P256V1FieldElement4.x);
        ECFieldElement sM2P256V1FieldElement5 = new SM2P256V1FieldElement(create3);
        SM2P256V1Field.subtract(create3, sM2P256V1FieldElement4.x, sM2P256V1FieldElement5.x);
        SM2P256V1Field.multiply(sM2P256V1FieldElement5.x, create2, sM2P256V1FieldElement5.x);
        SM2P256V1Field.subtract(sM2P256V1FieldElement5.x, create, sM2P256V1FieldElement5.x);
        sM2P256V1FieldElement2 = new SM2P256V1FieldElement(create2);
        SM2P256V1Field.twice(sM2P256V1FieldElement.x, sM2P256V1FieldElement2.x);
        if (!isOne) {
            SM2P256V1Field.multiply(sM2P256V1FieldElement2.x, sM2P256V1FieldElement3.x, sM2P256V1FieldElement2.x);
        }
        return new SM2P256V1Point(curve, sM2P256V1FieldElement4, sM2P256V1FieldElement5, new ECFieldElement[]{sM2P256V1FieldElement2}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return this == eCPoint ? threeTimes() : isInfinity() ? eCPoint : eCPoint.isInfinity() ? twice() : this.y.isZero() ? eCPoint : twice().add(eCPoint);
    }
}
