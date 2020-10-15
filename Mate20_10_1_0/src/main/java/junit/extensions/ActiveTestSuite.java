package junit.extensions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class ActiveTestSuite extends TestSuite {
    private volatile int fActiveTestDeathCount;

    public ActiveTestSuite() {
    }

    public ActiveTestSuite(Class<? extends TestCase> theClass) {
        super(theClass);
    }

    public ActiveTestSuite(String name) {
        super(name);
    }

    public ActiveTestSuite(Class<? extends TestCase> theClass, String name) {
        super(theClass, name);
    }

    @Override // junit.framework.TestSuite, junit.framework.Test
    public void run(TestResult result) {
        this.fActiveTestDeathCount = 0;
        super.run(result);
        waitUntilFinished();
    }

    @Override // junit.framework.TestSuite
    public void runTest(final Test test, final TestResult result) {
        new Thread() {
            /* class junit.extensions.ActiveTestSuite.AnonymousClass1 */

            public void run() {
                try {
                    test.run(result);
                } finally {
                    ActiveTestSuite.this.runFinished();
                }
            }
        }.start();
    }

    /* access modifiers changed from: package-private */
    public synchronized void waitUntilFinished() {
        while (this.fActiveTestDeathCount < testCount()) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public synchronized void runFinished() {
        this.fActiveTestDeathCount++;
        notifyAll();
    }
}
