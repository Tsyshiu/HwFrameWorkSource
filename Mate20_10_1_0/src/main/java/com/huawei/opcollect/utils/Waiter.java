package com.huawei.opcollect.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Waiter {
    private boolean completed = false;
    private Condition condition = this.lock.newCondition();
    private boolean interrupted = false;
    private ReentrantLock lock = new ReentrantLock();

    public boolean await(long timeout) {
        long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        this.lock.lock();
        while (!this.interrupted) {
            try {
                if (this.completed) {
                    this.completed = false;
                    this.interrupted = false;
                    this.lock.unlock();
                    return true;
                } else if (nanos <= 0) {
                    return false;
                } else {
                    nanos = this.condition.awaitNanos(nanos);
                }
            } catch (InterruptedException e) {
                return false;
            } finally {
                this.completed = false;
                this.interrupted = false;
                this.lock.unlock();
            }
        }
        this.completed = false;
        this.interrupted = false;
        this.lock.unlock();
        return false;
    }

    public void interrupt() {
        this.lock.lock();
        try {
            this.interrupted = true;
            this.condition.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public void signal() {
        this.lock.lock();
        try {
            this.completed = true;
            this.condition.signal();
        } finally {
            this.lock.unlock();
        }
    }
}
