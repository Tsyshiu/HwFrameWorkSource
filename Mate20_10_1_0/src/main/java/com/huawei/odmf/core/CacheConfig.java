package com.huawei.odmf.core;

import com.huawei.odmf.utils.ODMFCache;
import com.huawei.odmf.utils.Singleton;

public final class CacheConfig {
    private static final Singleton<CacheConfig> DEFAULT_CONFIG = new Singleton<CacheConfig>() {
        /* class com.huawei.odmf.core.CacheConfig.AnonymousClass1 */

        @Override // com.huawei.odmf.utils.Singleton
        public CacheConfig create() {
            return new CacheConfig();
        }
    };
    private boolean isOpenObjectCache;
    private final Object lock;
    private int objectCacheNum;

    private CacheConfig() {
        this.lock = new Object();
        this.isOpenObjectCache = false;
        this.objectCacheNum = 100;
    }

    public static CacheConfig getDefault() {
        return DEFAULT_CONFIG.get();
    }

    public boolean isOpenObjectCache() {
        boolean z;
        synchronized (this.lock) {
            z = this.isOpenObjectCache;
        }
        return z;
    }

    public void setOpenObjectCache(boolean openObjectCache) {
        synchronized (this.lock) {
            boolean current = this.isOpenObjectCache;
            this.isOpenObjectCache = openObjectCache;
            if (!current && !this.isOpenObjectCache) {
                return;
            }
            if (!current || !this.isOpenObjectCache) {
                ODMFCache cache = PersistentStoreCoordinator.getDefault().getObjectsCache();
                if (cache != null) {
                    cache.clear();
                }
                if (this.isOpenObjectCache) {
                    PersistentStoreCoordinator.getDefault().createObjectsCache();
                }
            }
        }
    }

    public void setOpenObjectCache(boolean openObjectCache, int objectCacheNum2) {
        synchronized (this.lock) {
            boolean current = this.isOpenObjectCache;
            this.isOpenObjectCache = openObjectCache;
            if (current || this.isOpenObjectCache) {
                ODMFCache cache = PersistentStoreCoordinator.getDefault().getObjectsCache();
                if (cache != null) {
                    cache.clear();
                }
                if (this.isOpenObjectCache) {
                    setObjectCacheNum(objectCacheNum2);
                    PersistentStoreCoordinator.getDefault().createObjectsCache();
                }
            }
        }
    }

    public int getObjectCacheNum() {
        int i;
        synchronized (this.lock) {
            i = this.objectCacheNum;
        }
        return i;
    }

    private void setObjectCacheNum(int objectCacheNum2) {
        synchronized (this.lock) {
            this.objectCacheNum = objectCacheNum2;
        }
    }
}
