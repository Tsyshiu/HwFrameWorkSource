package defpackage;

import java.util.concurrent.Executor;

/* renamed from: s  reason: default package */
public final class s implements Executor {
    public final void execute(Runnable runnable) {
        runnable.run();
    }
}
