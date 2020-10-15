package com.huawei.android.feature.tasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Tasks {
    private Tasks() {
    }

    public static <TResult> TResult await(Task<TResult> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        } else if (task.isComplete()) {
            return (TResult) getResult(task);
        } else {
            aa aaVar = new aa((byte) 0);
            registerCountDownLatchManager(task, aaVar);
            aaVar.F.await();
            return (TResult) getResult(task);
        }
    }

    public static <TResult> TResult await(Task<TResult> task, long j, TimeUnit timeUnit) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        } else if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit must not be null");
        } else if (task.isComplete()) {
            return (TResult) getResult(task);
        } else {
            aa aaVar = new aa((byte) 0);
            registerCountDownLatchManager(task, aaVar);
            if (aaVar.F.await(j, timeUnit)) {
                return (TResult) getResult(task);
            }
            throw new TimeoutException("Timed out waiting for Task");
        }
    }

    private static <TResult> TResult getResult(Task<TResult> task) {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        throw new ExecutionException(task.getException());
    }

    public static <TResult> Task<TResult> makeTask(TResult tresult) {
        w wVar = new w();
        synchronized (wVar.r) {
            if (wVar.y) {
                throw new IllegalStateException("Task is already complete");
            }
            wVar.y = true;
            wVar.z = tresult;
        }
        wVar.x.b(wVar);
        return wVar;
    }

    private static void registerCountDownLatchManager(Task<?> task, aa aaVar) {
        task.addOnSuccessListener(TaskExecutors.SExecutor, aaVar);
        task.addOnFailureListener(TaskExecutors.SExecutor, aaVar);
    }
}
