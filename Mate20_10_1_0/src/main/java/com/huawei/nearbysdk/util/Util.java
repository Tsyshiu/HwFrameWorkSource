package com.huawei.nearbysdk.util;

public class Util {
    private static final String EMPTY_STRING = "";
    private static final String FILE_PATH_SEPARATOR = "/";
    private static final int HALF_LENGTH = 2;
    private static final int NO_SEPARATOR = -1;

    private Util() {
    }

    public static String toFrontHalfString(String str) {
        String strDevice = String.valueOf(str).replace(":", EMPTY_STRING);
        return strDevice.substring(0, strDevice.length() / 2);
    }

    public static String getFileNameByPath(String filePath) {
        if (filePath == null) {
            return EMPTY_STRING;
        }
        if (filePath.endsWith(FILE_PATH_SEPARATOR)) {
            return FILE_PATH_SEPARATOR;
        }
        int result = filePath.lastIndexOf(FILE_PATH_SEPARATOR);
        if (result != -1) {
            return filePath.substring(result + 1);
        }
        return filePath;
    }
}
