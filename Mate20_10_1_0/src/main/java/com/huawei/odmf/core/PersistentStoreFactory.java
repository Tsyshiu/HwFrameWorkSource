package com.huawei.odmf.core;

import android.content.Context;
import android.net.Uri;
import java.util.Map;

public final class PersistentStoreFactory {
    private PersistentStoreFactory() {
    }

    public static int createPersistentStore(Uri uri, Configuration config, Context appCtx, String modelPath) {
        return PersistentStoreCoordinator.getDefault().createPersistentStore(uri, config, appCtx, modelPath);
    }

    /*  JADX ERROR: StackOverflowError in pass: MarkFinallyVisitor
        java.lang.StackOverflowError
        	at jadx.core.dex.instructions.IfNode.isSame(IfNode.java:122)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.sameInsns(MarkFinallyVisitor.java:451)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.compareBlocks(MarkFinallyVisitor.java:436)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.checkBlocksTree(MarkFinallyVisitor.java:408)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.checkBlocksTree(MarkFinallyVisitor.java:411)
        */
    public static int createEncryptedPersistentStore(Uri r8, Configuration r9, Context r10, String r11, byte[] r12) {
        /*
            r7 = 0
            if (r12 == 0) goto L_0x0006
            int r0 = r12.length
            if (r0 != 0) goto L_0x000e
        L_0x0006:
            com.huawei.odmf.exception.ODMFIllegalArgumentException r0 = new com.huawei.odmf.exception.ODMFIllegalArgumentException
            java.lang.String r1 = "encrypted database must be set a not null key"
            r0.<init>(r1)
            throw r0
        L_0x000e:
            com.huawei.odmf.core.PersistentStoreCoordinator r0 = com.huawei.odmf.core.PersistentStoreCoordinator.getDefault()     // Catch:{ all -> 0x0024 }
            r1 = r8
            r2 = r9
            r3 = r10
            r4 = r11
            r5 = r12
            int r0 = r0.createEncryptedPersistentStore(r1, r2, r3, r4, r5)     // Catch:{ all -> 0x0024 }
            r6 = 0
        L_0x001c:
            int r1 = r12.length
            if (r6 >= r1) goto L_0x002f
            r12[r6] = r7
            int r6 = r6 + 1
            goto L_0x001c
        L_0x0024:
            r0 = move-exception
            r6 = 0
        L_0x0026:
            int r1 = r12.length
            if (r6 >= r1) goto L_0x002e
            r12[r6] = r7
            int r6 = r6 + 1
            goto L_0x0026
        L_0x002e:
            throw r0
        L_0x002f:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.odmf.core.PersistentStoreFactory.createEncryptedPersistentStore(android.net.Uri, com.huawei.odmf.core.Configuration, android.content.Context, java.lang.String, byte[]):int");
    }

    public static int createCrossPersistentStore(Uri uri, Configuration config, Context appCtx, Map<Uri, byte[]> uriList) {
        return PersistentStoreCoordinator.getDefault().createCrossPersistentStore(uri, config, appCtx, uriList);
    }

    public static PersistentStore getPersistentStore(Uri uri) {
        return PersistentStoreCoordinator.getDefault().getPersistentStore(uri);
    }
}
