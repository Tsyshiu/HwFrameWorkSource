package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.AbstractECLookupTable;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat192;
import org.bouncycastle.util.encoders.Hex;

public class SecT131R1Curve extends ECCurve.AbstractF2m {
    /* access modifiers changed from: private */
    public static final ECFieldElement[] SECT131R1_AFFINE_ZS = {new SecT131FieldElement(ECConstants.ONE)};
    private static final int SECT131R1_DEFAULT_COORDS = 6;
    protected SecT131R1Point infinity = new SecT131R1Point(this, null, null);

    public SecT131R1Curve() {
        super(131, 2, 3, 8);
        this.a = fromBigInteger(new BigInteger(1, Hex.decodeStrict("07A11B09A76B562144418FF3FF8C2570B8")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decodeStrict("0217C05610884B63B9C6C7291678F9D341")));
        this.order = new BigInteger(1, Hex.decodeStrict("0400000000000000023123953A9464B54D"));
        this.cofactor = BigInteger.valueOf(2);
        this.coord = 6;
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.math.ec.ECCurve
    public ECCurve cloneCurve() {
        return new SecT131R1Curve();
    }

    @Override // org.bouncycastle.math.ec.ECCurve
    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final long[] jArr = new long[(i2 * 3 * 2)];
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            ECPoint eCPoint = eCPointArr[i + i4];
            Nat192.copy64(((SecT131FieldElement) eCPoint.getRawXCoord()).x, 0, jArr, i3);
            int i5 = i3 + 3;
            Nat192.copy64(((SecT131FieldElement) eCPoint.getRawYCoord()).x, 0, jArr, i5);
            i3 = i5 + 3;
        }
        return new AbstractECLookupTable() {
            /* class org.bouncycastle.math.ec.custom.sec.SecT131R1Curve.AnonymousClass1 */

            private ECPoint createPoint(long[] jArr, long[] jArr2) {
                return SecT131R1Curve.this.createRawPoint(new SecT131FieldElement(jArr), new SecT131FieldElement(jArr2), SecT131R1Curve.SECT131R1_AFFINE_ZS);
            }

            @Override // org.bouncycastle.math.ec.ECLookupTable
            public int getSize() {
                return i2;
            }

            @Override // org.bouncycastle.math.ec.ECLookupTable
            public ECPoint lookup(int i) {
                long[] create64 = Nat192.create64();
                long[] create642 = Nat192.create64();
                int i2 = 0;
                for (int i3 = 0; i3 < i2; i3++) {
                    long j = (long) (((i3 ^ i) - 1) >> 31);
                    for (int i4 = 0; i4 < 3; i4++) {
                        long j2 = create64[i4];
                        long[] jArr = jArr;
                        create64[i4] = j2 ^ (jArr[i2 + i4] & j);
                        create642[i4] = create642[i4] ^ (jArr[(i2 + 3) + i4] & j);
                    }
                    i2 += 6;
                }
                return createPoint(create64, create642);
            }

            @Override // org.bouncycastle.math.ec.AbstractECLookupTable, org.bouncycastle.math.ec.ECLookupTable
            public ECPoint lookupVar(int i) {
                long[] create64 = Nat192.create64();
                long[] create642 = Nat192.create64();
                int i2 = i * 3 * 2;
                for (int i3 = 0; i3 < 3; i3++) {
                    long[] jArr = jArr;
                    create64[i3] = jArr[i2 + i3];
                    create642[i3] = jArr[i2 + 3 + i3];
                }
                return createPoint(create64, create642);
            }
        };
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.math.ec.ECCurve
    public ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        return new SecT131R1Point(this, eCFieldElement, eCFieldElement2);
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.math.ec.ECCurve
    public ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
        return new SecT131R1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr);
    }

    @Override // org.bouncycastle.math.ec.ECCurve
    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecT131FieldElement(bigInteger);
    }

    @Override // org.bouncycastle.math.ec.ECCurve
    public int getFieldSize() {
        return 131;
    }

    @Override // org.bouncycastle.math.ec.ECCurve
    public ECPoint getInfinity() {
        return this.infinity;
    }

    public int getK1() {
        return 2;
    }

    public int getK2() {
        return 3;
    }

    public int getK3() {
        return 8;
    }

    public int getM() {
        return 131;
    }

    @Override // org.bouncycastle.math.ec.ECCurve.AbstractF2m
    public boolean isKoblitz() {
        return false;
    }

    public boolean isTrinomial() {
        return false;
    }

    @Override // org.bouncycastle.math.ec.ECCurve
    public boolean supportsCoordinateSystem(int i) {
        return i == 6;
    }
}
