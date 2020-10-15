package com.android.server.pm;

public class DummyHwPackageServiceManager implements HwPackageServiceManager {
    private static HwPackageServiceManager mInstance = new DummyHwPackageServiceManager();

    public static HwPackageServiceManager getDefault() {
        return mInstance;
    }

    @Override // com.android.server.pm.HwPackageServiceManager
    public void addHwSharedUserLP(Object settings) {
    }
}
