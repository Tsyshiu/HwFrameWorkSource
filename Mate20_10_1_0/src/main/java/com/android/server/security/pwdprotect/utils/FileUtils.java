package com.android.server.security.pwdprotect.utils;

import android.text.TextUtils;
import android.util.Log;
import com.android.server.security.pwdprotect.model.PasswordIvsCache;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import libcore.io.IoUtils;

public class FileUtils {
    private static final int HASHLENGTH = 32;
    private static final int HASHVALUES = 5;
    private static final int IV_FIRST = 0;
    private static final int IV_FOUR = 3;
    private static final int IV_SECOND = 1;
    private static final int IV_THIRD = 2;
    private static final int KEYVALUES = 4;
    private static final int NOHASHVALUES = 8;
    private static final int PKLENGTH = 294;
    private static final int PUBLICKEY = 7;
    private static final String TAG = "PwdProtectService";

    public static byte[] readKeys(File file) {
        return readFile(file, 4);
    }

    private static byte[] readHashs(File file) {
        return readFile(file, 5);
    }

    public static byte[] readIvs(File file, int ivNo) {
        return readFile(file, ivNo);
    }

    private static byte[] readNoHashs(File file) {
        return readFile(file, 8);
    }

    public static byte[] readPublicKey() {
        return readFile(PasswordIvsCache.FILE_E_PIN2, 7);
    }

    /* JADX INFO: finally extract failed */
    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00c9, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00ca, code lost:
        $closeResource(r5, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00cd, code lost:
        throw r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x00d0, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x00d1, code lost:
        $closeResource(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x00d4, code lost:
        throw r5;
     */
    public static byte[] readFile(File file, int valueType) {
        if (!file.exists()) {
            Log.e(TAG, "The file doesn't exist");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] buf = new byte[4096];
            switch (valueType) {
                case 0:
                case 1:
                case 2:
                case 3:
                    while (bis.read(buf) != -1) {
                        baos.write(buf, 16 * valueType, 16);
                    }
                    break;
                case 4:
                    if (!file.getName().equals("E_SK2")) {
                        if (!file.getName().equals("E_PIN2")) {
                            while (true) {
                                int len = bis.read(buf);
                                if (len == -1) {
                                    break;
                                } else {
                                    baos.write(buf, 16 * 1, (len - 32) - (16 * 1));
                                }
                            }
                        } else {
                            while (true) {
                                int len2 = bis.read(buf);
                                if (len2 == -1) {
                                    break;
                                } else {
                                    baos.write(buf, 16 * 1, ((len2 - 32) - (16 * 1)) - PKLENGTH);
                                }
                            }
                        }
                    } else {
                        while (true) {
                            int len3 = bis.read(buf);
                            if (len3 == -1) {
                                break;
                            } else {
                                baos.write(buf, 16 * 4, (len3 - 32) - (16 * 4));
                            }
                        }
                    }
                case 5:
                    while (true) {
                        int len4 = bis.read(buf);
                        if (len4 == -1) {
                            break;
                        } else {
                            baos.write(buf, len4 - 32, 32);
                        }
                    }
                case 7:
                    while (true) {
                        int len5 = bis.read(buf);
                        if (len5 == -1) {
                            break;
                        } else {
                            baos.write(buf, (len5 - 32) - PKLENGTH, PKLENGTH);
                        }
                    }
                case 8:
                    while (true) {
                        int len6 = bis.read(buf);
                        if (len6 == -1) {
                            break;
                        } else {
                            baos.write(buf, 0, len6 - 32);
                        }
                    }
            }
            byte[] buffer = baos.toByteArray();
            $closeResource(null, bis);
            $closeResource(null, fis);
            IoUtils.closeQuietly(baos);
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "read file exception!" + e.getMessage());
            IoUtils.closeQuietly(baos);
            return new byte[0];
        } catch (Throwable th) {
            IoUtils.closeQuietly(baos);
            throw th;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    /* JADX INFO: finally extract failed */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0021, code lost:
        r3 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0022, code lost:
        $closeResource(r2, r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0025, code lost:
        throw r3;
     */
    public static boolean writeFile(byte[] values, File fileName) {
        mkdirHwSecurity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            byteArrayOutputStream.write(values);
            byteArrayOutputStream.writeTo(fileOutputStream);
            fileOutputStream.flush();
            $closeResource(null, fileOutputStream);
            IoUtils.closeQuietly(byteArrayOutputStream);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "write file exception! " + fileName.getName() + e.getMessage());
            IoUtils.closeQuietly(byteArrayOutputStream);
            return false;
        } catch (Throwable th) {
            IoUtils.closeQuietly(byteArrayOutputStream);
            throw th;
        }
    }

    public static File newFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        File screenFile = new File(fileName);
        try {
            boolean result = screenFile.createNewFile();
            Log.i(TAG, "new file " + screenFile.getName() + " result is :" + result);
            return screenFile;
        } catch (IOException e) {
            Log.e(TAG, "newFile: new file exception!");
            return null;
        }
    }

    private static Boolean verifyFile(File file) {
        if (Arrays.equals(readHashs(file), DeviceEncryptUtils.hmacSign(readNoHashs(file)))) {
            Log.i(TAG, "verify File " + file.getName() + " result is true");
            return true;
        }
        Log.e(TAG, "verify File " + file.getName() + " result is false");
        return false;
    }

    public static Boolean verifyFile() {
        if (!verifyFile(PasswordIvsCache.FILE_E_PWDQANSWER).booleanValue() || !verifyFile(PasswordIvsCache.FILE_E_PIN2).booleanValue() || !verifyFile(PasswordIvsCache.FILE_E_SK2).booleanValue() || !verifyFile(PasswordIvsCache.FILE_E_PWDQ).booleanValue()) {
            Log.e(TAG, "verify File is false");
            return false;
        }
        Log.i(TAG, "verify File is true");
        return true;
    }

    private static void mkdirHwSecurity() {
        File file = PasswordIvsCache.PWDPROTECT_DIR_PATH;
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "mkdirs file failed");
        }
    }
}
