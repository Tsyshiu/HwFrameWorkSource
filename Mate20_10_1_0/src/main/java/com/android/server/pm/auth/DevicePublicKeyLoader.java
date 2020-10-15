package com.android.server.pm.auth;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Base64;
import com.android.server.pm.auth.util.CryptionUtils;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.xmlpull.v1.XmlPullParserException;

public class DevicePublicKeyLoader {
    public static final String EMUI10_PK = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx4nUogUyMCmzHhaEb420yvpw9zBs+ETzE9Qm77bGxl1Iml9JEkBkNTsUWOstLgUBajNhV+BAMVBHKMEdzoQbL5kIHkTgUVM65yewd+5+BhrcB9OQ3LHp+0BN6aLKZh71T4WvsvHFhfhQpShuGWkRkSaVGLFTHxX70kpWLzeZ3RtqiEUNIufPR2SFCH6EmecJ+HdkmBOh603IblCpGxwSWse0fDI98wZBEmV88RFaiYEgyiezLlWvXzqIj6I/xuyd5nGAegjH2y3cmoDE6CubecoB1jf4KdgACXgdiQ4Oc63MfLGTor3l6RCqeUk4APAMtyhK83jc72W1sdXMd/sj2wIDAQAB";
    private static final int HW_PUB_KEY_RES = 34340866;
    public static final String KEY = "key";
    private static final String PUBLIC_KEY = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100a3d269348ac59923f65e8111c337605e29a1d1bc54fa96c1445050dd14d8d63b10f9f0230bb87ef348183660bedcabfdec045e235ed96935799fcdb4af5c97717ff3b0954eaf1b723225b3a00f81cbd67ce6dc5a4c07f7741ad3bf1913a480c6e267ab1740f409edd2dc33c8b718a8e30e56d9a93f321723c1d0c9ea62115f996812ceef186954595e39a19b74245542c407f7dddb1d12e6eedcfc0bd7cd945ef7255ad0fc9e796258e0fb5e52a23013d15033a32b4071b65f3f924ae5c5761e22327b4d2ae60f4158a5eb15565ba079de29b81540f5fbb3be101a95357f367fc661d797074ff3826950029c52223e4594673a24a334cae62d63b838ba3df9770203010001";
    private static final String RSA = "RSA";
    public static final String TAG = "HwCertificationManager";
    public static final String VALUE = "value";
    private static PublicKey mPublicKey = null;

    public static PublicKey getPublicKey(Context context) {
        if (mPublicKey == null) {
            mPublicKey = loadDevicePublicKey(context);
        }
        return mPublicKey;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0072, code lost:
        if (0 == 0) goto L_0x0075;
     */
    private static PublicKey loadDevicePublicKey(Context context) {
        byte[] hwPubKeyBytes = null;
        XmlResourceParser xpp = null;
        try {
            xpp = context.getResources().getXml(HW_PUB_KEY_RES);
            String hwPubKeyString = null;
            int eventType = xpp.next();
            while (true) {
                if (eventType != 1) {
                    if (eventType == 2 && xpp.getName().equals(KEY)) {
                        hwPubKeyString = xpp.getAttributeValue(null, "value");
                        break;
                    }
                    eventType = xpp.next();
                } else {
                    break;
                }
            }
            if (hwPubKeyString != null) {
                hwPubKeyBytes = Utils.stringToBytes(hwPubKeyString);
            }
        } catch (XmlPullParserException e) {
            HwAuthLogger.e("HwCertificationManager", "XmlPullParserException:" + e, e);
        } catch (IOException ex) {
            HwAuthLogger.e("HwCertificationManager", "IOException:" + ex, ex);
            if (0 != 0) {
            }
        } catch (Throwable th) {
            if (0 != 0) {
                xpp.close();
            }
            throw th;
        }
        xpp.close();
        if (hwPubKeyBytes != null) {
            return CryptionUtils.getPublicKey(hwPubKeyBytes);
        }
        return CryptionUtils.getPublicKey(PUBLIC_KEY);
    }

    public static PublicKey getPublicKeyForBase64(String key) {
        try {
            return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(Base64.decode(key, 0)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            HwAuthLogger.e("HwCertificationManager", "get base64 pk failed");
            return null;
        }
    }
}
