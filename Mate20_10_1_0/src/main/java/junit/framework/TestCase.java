package junit.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class TestCase extends Assert implements Test {
    private String fName;

    public TestCase() {
        this.fName = null;
    }

    public TestCase(String name) {
        this.fName = name;
    }

    @Override // junit.framework.Test
    public int countTestCases() {
        return 1;
    }

    /* access modifiers changed from: protected */
    public TestResult createResult() {
        return new TestResult();
    }

    public TestResult run() {
        TestResult result = createResult();
        run(result);
        return result;
    }

    @Override // junit.framework.Test
    public void run(TestResult result) {
        result.run(this);
    }

    public void runBare() throws Throwable {
        Throwable exception = null;
        setUp();
        try {
            runTest();
            try {
                tearDown();
            } catch (Throwable tearingDown) {
                if (0 == 0) {
                    exception = tearingDown;
                }
            }
        } catch (Throwable th) {
        }
        if (exception != null) {
            throw exception;
        }
    }

    /* access modifiers changed from: protected */
    public void runTest() throws Throwable {
        assertNotNull("TestCase.fName cannot be null", this.fName);
        Method runMethod = null;
        try {
            runMethod = getClass().getMethod(this.fName, null);
        } catch (NoSuchMethodException e) {
            fail("Method \"" + this.fName + "\" not found");
        }
        if (!Modifier.isPublic(runMethod.getModifiers())) {
            fail("Method \"" + this.fName + "\" should be public");
        }
        try {
            runMethod.invoke(this, new Object[0]);
        } catch (InvocationTargetException e2) {
            e2.fillInStackTrace();
            throw e2.getTargetException();
        } catch (IllegalAccessException e3) {
            e3.fillInStackTrace();
            throw e3;
        }
    }

    @Override // junit.framework.Assert
    public static void assertTrue(String message, boolean condition) {
        Assert.assertTrue(message, condition);
    }

    @Override // junit.framework.Assert
    public static void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    @Override // junit.framework.Assert
    public static void assertFalse(String message, boolean condition) {
        Assert.assertFalse(message, condition);
    }

    @Override // junit.framework.Assert
    public static void assertFalse(boolean condition) {
        Assert.assertFalse(condition);
    }

    @Override // junit.framework.Assert
    public static void fail(String message) {
        Assert.fail(message);
    }

    @Override // junit.framework.Assert
    public static void fail() {
        Assert.fail();
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, Object expected, Object actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(Object expected, Object actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, String expected, String actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String expected, String actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, double expected, double actual, double delta) {
        Assert.assertEquals(message, expected, actual, delta);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(double expected, double actual, double delta) {
        Assert.assertEquals(expected, actual, delta);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, float expected, float actual, float delta) {
        Assert.assertEquals(message, expected, actual, delta);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(float expected, float actual, float delta) {
        Assert.assertEquals(expected, actual, delta);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, long expected, long actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(long expected, long actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, boolean expected, boolean actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(boolean expected, boolean actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, byte expected, byte actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(byte expected, byte actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, char expected, char actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(char expected, char actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, short expected, short actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(short expected, short actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(String message, int expected, int actual) {
        Assert.assertEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertEquals(int expected, int actual) {
        Assert.assertEquals(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertNotNull(Object object) {
        Assert.assertNotNull(object);
    }

    @Override // junit.framework.Assert
    public static void assertNotNull(String message, Object object) {
        Assert.assertNotNull(message, object);
    }

    @Override // junit.framework.Assert
    public static void assertNull(Object object) {
        Assert.assertNull(object);
    }

    @Override // junit.framework.Assert
    public static void assertNull(String message, Object object) {
        Assert.assertNull(message, object);
    }

    @Override // junit.framework.Assert
    public static void assertSame(String message, Object expected, Object actual) {
        Assert.assertSame(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertSame(Object expected, Object actual) {
        Assert.assertSame(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertNotSame(String message, Object expected, Object actual) {
        Assert.assertNotSame(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void assertNotSame(Object expected, Object actual) {
        Assert.assertNotSame(expected, actual);
    }

    @Override // junit.framework.Assert
    public static void failSame(String message) {
        Assert.failSame(message);
    }

    @Override // junit.framework.Assert
    public static void failNotSame(String message, Object expected, Object actual) {
        Assert.failNotSame(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static void failNotEquals(String message, Object expected, Object actual) {
        Assert.failNotEquals(message, expected, actual);
    }

    @Override // junit.framework.Assert
    public static String format(String message, Object expected, Object actual) {
        return Assert.format(message, expected, actual);
    }

    /* access modifiers changed from: protected */
    public void setUp() throws Exception {
    }

    /* access modifiers changed from: protected */
    public void tearDown() throws Exception {
    }

    public String toString() {
        return getName() + "(" + getClass().getName() + ")";
    }

    public String getName() {
        return this.fName;
    }

    public void setName(String name) {
        this.fName = name;
    }
}
