package com.huawei.android.feature.install.config;

import com.huawei.android.feature.BuildConfig;
import java.io.File;
import java.util.Collections;
import java.util.Set;

public class FeatureInstallConfig {
    public static final Set<String> support = Collections.unmodifiableSet(new i());

    public static String getSuffix(String str) {
        if (str == null) {
            return BuildConfig.FLAVOR;
        }
        for (String str2 : support) {
            if (str.endsWith(str2)) {
                return str2;
            }
        }
        return BuildConfig.FLAVOR;
    }

    public static boolean isFileEndWithConfig(File file) {
        for (String str : support) {
            if (file.getName().endsWith(str)) {
                return true;
            }
        }
        return false;
    }
}
