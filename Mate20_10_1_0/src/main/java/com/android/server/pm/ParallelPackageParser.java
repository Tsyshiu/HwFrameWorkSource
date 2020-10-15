package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.Trace;
import android.util.DisplayMetrics;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ConcurrentUtils;
import java.io.File;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

class ParallelPackageParser implements AutoCloseable {
    private static final int MAX_THREADS = 4;
    private static final int QUEUE_CAPACITY = 10;
    private final File mCacheDir;
    private volatile String mInterruptedInThread;
    private final DisplayMetrics mMetrics;
    private final boolean mOnlyCore;
    private final PackageParser.Callback mPackageParserCallback;
    private final BlockingQueue<ParseResult> mQueue = new ArrayBlockingQueue(10);
    private final String[] mSeparateProcesses;
    private final ExecutorService mService = ConcurrentUtils.newFixedThreadPool(4, "package-parsing-thread", -2);

    ParallelPackageParser(String[] separateProcesses, boolean onlyCoreApps, DisplayMetrics metrics, File cacheDir, PackageParser.Callback callback) {
        this.mSeparateProcesses = separateProcesses;
        this.mOnlyCore = onlyCoreApps;
        this.mMetrics = metrics;
        this.mCacheDir = cacheDir;
        this.mPackageParserCallback = callback;
    }

    static class ParseResult {
        PackageParser.Package pkg;
        File scanFile;
        Throwable throwable;

        ParseResult() {
        }

        public String toString() {
            return "ParseResult{pkg=" + this.pkg + ", scanFile=" + this.scanFile + ", throwable=" + this.throwable + '}';
        }
    }

    public ParseResult take() {
        try {
            if (this.mInterruptedInThread == null) {
                return this.mQueue.take();
            }
            throw new InterruptedException("Interrupted in " + this.mInterruptedInThread);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public void submit(File scanFile, int parseFlags) {
        this.mService.submit(new Runnable(scanFile, parseFlags) {
            /* class com.android.server.pm.$$Lambda$ParallelPackageParser$FTtinPrp068lVeI7K6bC1tNE3iM */
            private final /* synthetic */ File f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                ParallelPackageParser.this.lambda$submit$0$ParallelPackageParser(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$submit$0$ParallelPackageParser(File scanFile, int parseFlags) {
        ParseResult pr = new ParseResult();
        Trace.traceBegin(262144, "parallel parsePackage [" + scanFile + "]");
        try {
            PackageParser pp = new PackageParser();
            pp.setSeparateProcesses(this.mSeparateProcesses);
            pp.setOnlyCoreApps(this.mOnlyCore);
            pp.setDisplayMetrics(this.mMetrics);
            pp.setCacheDir(this.mCacheDir);
            pp.setCallback(this.mPackageParserCallback);
            pr.scanFile = scanFile;
            pr.pkg = parsePackage(pp, scanFile, parseFlags);
        } catch (Throwable th) {
            Trace.traceEnd(262144);
            throw th;
        }
        Trace.traceEnd(262144);
        try {
            this.mQueue.put(pr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.mInterruptedInThread = Thread.currentThread().getName();
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public PackageParser.Package parsePackage(PackageParser packageParser, File scanFile, int parseFlags) throws PackageParser.PackageParserException {
        return packageParser.parsePackage(scanFile, parseFlags, true);
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        List<Runnable> unfinishedTasks = this.mService.shutdownNow();
        if (!unfinishedTasks.isEmpty()) {
            throw new IllegalStateException("Not all tasks finished before calling close: " + unfinishedTasks);
        }
    }
}
