package com.huawei.nb.model.pengine;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class AppRoutineHelper extends AEntityHelper<AppRoutine> {
    private static final AppRoutineHelper INSTANCE = new AppRoutineHelper();

    private AppRoutineHelper() {
    }

    public static AppRoutineHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, AppRoutine object) {
        Integer id = object.getId();
        if (id != null) {
            statement.bindLong(1, (long) id.intValue());
        } else {
            statement.bindNull(1);
        }
        String timestamp = object.getTimestamp();
        if (timestamp != null) {
            statement.bindString(2, timestamp);
        } else {
            statement.bindNull(2);
        }
        String startTime = object.getStartTime();
        if (startTime != null) {
            statement.bindString(3, startTime);
        } else {
            statement.bindNull(3);
        }
        String endTime = object.getEndTime();
        if (endTime != null) {
            statement.bindString(4, endTime);
        } else {
            statement.bindNull(4);
        }
        String poi = object.getPoi();
        if (poi != null) {
            statement.bindString(5, poi);
        } else {
            statement.bindNull(5);
        }
        String packageName = object.getPackageName();
        if (packageName != null) {
            statement.bindString(6, packageName);
        } else {
            statement.bindNull(6);
        }
        String support = object.getSupport();
        if (support != null) {
            statement.bindString(7, support);
        } else {
            statement.bindNull(7);
        }
        String column0 = object.getColumn0();
        if (column0 != null) {
            statement.bindString(8, column0);
        } else {
            statement.bindNull(8);
        }
        String column1 = object.getColumn1();
        if (column1 != null) {
            statement.bindString(9, column1);
        } else {
            statement.bindNull(9);
        }
        String column2 = object.getColumn2();
        if (column2 != null) {
            statement.bindString(10, column2);
        } else {
            statement.bindNull(10);
        }
        String column3 = object.getColumn3();
        if (column3 != null) {
            statement.bindString(11, column3);
        } else {
            statement.bindNull(11);
        }
        String column4 = object.getColumn4();
        if (column4 != null) {
            statement.bindString(12, column4);
        } else {
            statement.bindNull(12);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public AppRoutine readObject(Cursor cursor, int offset) {
        return new AppRoutine(cursor);
    }

    public void setPrimaryKeyValue(AppRoutine object, long value) {
        object.setId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, AppRoutine object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
