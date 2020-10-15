package com.huawei.nb.model.pengine;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class CapTaskInstanceHelper extends AEntityHelper<CapTaskInstance> {
    private static final CapTaskInstanceHelper INSTANCE = new CapTaskInstanceHelper();

    private CapTaskInstanceHelper() {
    }

    public static CapTaskInstanceHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, CapTaskInstance object) {
        Integer taskId = object.getTaskId();
        if (taskId != null) {
            statement.bindLong(1, (long) taskId.intValue());
        } else {
            statement.bindNull(1);
        }
        Long jobId = object.getJobId();
        if (jobId != null) {
            statement.bindLong(2, jobId.longValue());
        } else {
            statement.bindNull(2);
        }
        String taskName = object.getTaskName();
        if (taskName != null) {
            statement.bindString(3, taskName);
        } else {
            statement.bindNull(3);
        }
        String status = object.getStatus();
        if (status != null) {
            statement.bindString(4, status);
        } else {
            statement.bindNull(4);
        }
        String result = object.getResult();
        if (result != null) {
            statement.bindString(5, result);
        } else {
            statement.bindNull(5);
        }
        String resultDesc = object.getResultDesc();
        if (resultDesc != null) {
            statement.bindString(6, resultDesc);
        } else {
            statement.bindNull(6);
        }
        Long createTime = object.getCreateTime();
        if (createTime != null) {
            statement.bindLong(7, createTime.longValue());
        } else {
            statement.bindNull(7);
        }
        Long lastModifyTime = object.getLastModifyTime();
        if (lastModifyTime != null) {
            statement.bindLong(8, lastModifyTime.longValue());
        } else {
            statement.bindNull(8);
        }
        String attrs = object.getAttrs();
        if (attrs != null) {
            statement.bindString(9, attrs);
        } else {
            statement.bindNull(9);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public CapTaskInstance readObject(Cursor cursor, int offset) {
        return new CapTaskInstance(cursor);
    }

    public void setPrimaryKeyValue(CapTaskInstance object, long value) {
        object.setTaskId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, CapTaskInstance object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
