package com.huawei.nb.model.authority;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class ZDatabaseAuthorityGrantHelper extends AEntityHelper<ZDatabaseAuthorityGrant> {
    private static final ZDatabaseAuthorityGrantHelper INSTANCE = new ZDatabaseAuthorityGrantHelper();

    private ZDatabaseAuthorityGrantHelper() {
    }

    public static ZDatabaseAuthorityGrantHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, ZDatabaseAuthorityGrant object) {
        Long id = object.getId();
        if (id != null) {
            statement.bindLong(1, id.longValue());
        } else {
            statement.bindNull(1);
        }
        Long dbId = object.getDbId();
        if (dbId != null) {
            statement.bindLong(2, dbId.longValue());
        } else {
            statement.bindNull(2);
        }
        String dbName = object.getDbName();
        if (dbName != null) {
            statement.bindString(3, dbName);
        } else {
            statement.bindNull(3);
        }
        Long packageUid = object.getPackageUid();
        if (packageUid != null) {
            statement.bindLong(4, packageUid.longValue());
        } else {
            statement.bindNull(4);
        }
        String packageName = object.getPackageName();
        if (packageName != null) {
            statement.bindString(5, packageName);
        } else {
            statement.bindNull(5);
        }
        Integer authority = object.getAuthority();
        if (authority != null) {
            statement.bindLong(6, (long) authority.intValue());
        } else {
            statement.bindNull(6);
        }
        Boolean supportGroupAuthority = object.getSupportGroupAuthority();
        if (supportGroupAuthority != null) {
            statement.bindLong(7, supportGroupAuthority.booleanValue() ? 1 : 0);
        } else {
            statement.bindNull(7);
        }
        String reserved = object.getReserved();
        if (reserved != null) {
            statement.bindString(8, reserved);
        } else {
            statement.bindNull(8);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public ZDatabaseAuthorityGrant readObject(Cursor cursor, int offset) {
        return new ZDatabaseAuthorityGrant(cursor);
    }

    public void setPrimaryKeyValue(ZDatabaseAuthorityGrant object, long value) {
        object.setId(Long.valueOf(value));
    }

    public Object getRelationshipObject(String field, ZDatabaseAuthorityGrant object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
