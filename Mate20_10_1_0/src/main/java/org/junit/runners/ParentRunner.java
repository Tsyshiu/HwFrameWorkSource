package org.junit.runners;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.rules.RuleMemberValidator;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.junit.validator.AnnotationsValidator;
import org.junit.validator.PublicClassValidator;
import org.junit.validator.TestClassValidator;

public abstract class ParentRunner<T> extends Runner implements Filterable, Sortable {
    private static final List<TestClassValidator> VALIDATORS = Arrays.asList(new AnnotationsValidator(), new PublicClassValidator());
    private final Object childrenLock = new Object();
    private volatile Collection<T> filteredChildren = null;
    private volatile RunnerScheduler scheduler = new RunnerScheduler() {
        /* class org.junit.runners.ParentRunner.AnonymousClass1 */

        @Override // org.junit.runners.model.RunnerScheduler
        public void schedule(Runnable childStatement) {
            childStatement.run();
        }

        @Override // org.junit.runners.model.RunnerScheduler
        public void finished() {
        }
    };
    private final TestClass testClass;

    /* access modifiers changed from: protected */
    public abstract Description describeChild(T t);

    /* access modifiers changed from: protected */
    public abstract List<T> getChildren();

    /* access modifiers changed from: protected */
    public abstract void runChild(T t, RunNotifier runNotifier);

    protected ParentRunner(Class<?> testClass2) throws InitializationError {
        this.testClass = createTestClass(testClass2);
        validate();
    }

    /* access modifiers changed from: protected */
    public TestClass createTestClass(Class<?> testClass2) {
        return new TestClass(testClass2);
    }

    /* access modifiers changed from: protected */
    public void collectInitializationErrors(List<Throwable> errors) {
        validatePublicVoidNoArgMethods(BeforeClass.class, true, errors);
        validatePublicVoidNoArgMethods(AfterClass.class, true, errors);
        validateClassRules(errors);
        applyValidators(errors);
    }

    private void applyValidators(List<Throwable> errors) {
        if (getTestClass().getJavaClass() != null) {
            for (TestClassValidator each : VALIDATORS) {
                errors.addAll(each.validateTestClass(getTestClass()));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {
        for (FrameworkMethod eachTestMethod : getTestClass().getAnnotatedMethods(annotation)) {
            eachTestMethod.validatePublicVoidNoArg(isStatic, errors);
        }
    }

    private void validateClassRules(List<Throwable> errors) {
        RuleMemberValidator.CLASS_RULE_VALIDATOR.validate(getTestClass(), errors);
        RuleMemberValidator.CLASS_RULE_METHOD_VALIDATOR.validate(getTestClass(), errors);
    }

    /* access modifiers changed from: protected */
    public Statement classBlock(RunNotifier notifier) {
        Statement statement = childrenInvoker(notifier);
        if (!areAllChildrenIgnored()) {
            return withClassRules(withAfterClasses(withBeforeClasses(statement)));
        }
        return statement;
    }

    private boolean areAllChildrenIgnored() {
        for (T child : getFilteredChildren()) {
            if (!isIgnored(child)) {
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public Statement withBeforeClasses(Statement statement) {
        List<FrameworkMethod> befores = this.testClass.getAnnotatedMethods(BeforeClass.class);
        if (befores.isEmpty()) {
            return statement;
        }
        return new RunBefores(statement, befores, null);
    }

    /* access modifiers changed from: protected */
    public Statement withAfterClasses(Statement statement) {
        List<FrameworkMethod> afters = this.testClass.getAnnotatedMethods(AfterClass.class);
        if (afters.isEmpty()) {
            return statement;
        }
        return new RunAfters(statement, afters, null);
    }

    private Statement withClassRules(Statement statement) {
        List<TestRule> classRules = classRules();
        if (classRules.isEmpty()) {
            return statement;
        }
        return new RunRules(statement, classRules, getDescription());
    }

    /* access modifiers changed from: protected */
    public List<TestRule> classRules() {
        List<TestRule> result = this.testClass.getAnnotatedMethodValues(null, ClassRule.class, TestRule.class);
        result.addAll(this.testClass.getAnnotatedFieldValues(null, ClassRule.class, TestRule.class));
        return result;
    }

    /* access modifiers changed from: protected */
    public Statement childrenInvoker(final RunNotifier notifier) {
        return new Statement() {
            /* class org.junit.runners.ParentRunner.AnonymousClass2 */

            @Override // org.junit.runners.model.Statement
            public void evaluate() {
                ParentRunner.this.runChildren(notifier);
            }
        };
    }

    /* access modifiers changed from: protected */
    public boolean isIgnored(T t) {
        return false;
    }

    /* access modifiers changed from: private */
    public void runChildren(final RunNotifier notifier) {
        RunnerScheduler currentScheduler = this.scheduler;
        try {
            for (final T each : getFilteredChildren()) {
                currentScheduler.schedule(new Runnable() {
                    /* class org.junit.runners.ParentRunner.AnonymousClass3 */

                    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: org.junit.runners.ParentRunner */
                    /* JADX WARN: Multi-variable type inference failed */
                    public void run() {
                        ParentRunner.this.runChild(each, notifier);
                    }
                });
            }
        } finally {
            currentScheduler.finished();
        }
    }

    /* access modifiers changed from: protected */
    public String getName() {
        return this.testClass.getName();
    }

    public final TestClass getTestClass() {
        return this.testClass;
    }

    /* access modifiers changed from: protected */
    public final void runLeaf(Statement statement, Description description, RunNotifier notifier) {
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        eachNotifier.fireTestStarted();
        try {
            statement.evaluate();
        } catch (AssumptionViolatedException e) {
            eachNotifier.addFailedAssumption(e);
        } catch (Throwable th) {
            eachNotifier.fireTestFinished();
            throw th;
        }
        eachNotifier.fireTestFinished();
    }

    /* access modifiers changed from: protected */
    public Annotation[] getRunnerAnnotations() {
        return this.testClass.getAnnotations();
    }

    @Override // org.junit.runner.Describable, org.junit.runner.Runner
    public Description getDescription() {
        Description description = Description.createSuiteDescription(getName(), getRunnerAnnotations());
        for (T child : getFilteredChildren()) {
            description.addChild(describeChild(child));
        }
        return description;
    }

    @Override // org.junit.runner.Runner
    public void run(RunNotifier notifier) {
        EachTestNotifier testNotifier = new EachTestNotifier(notifier, getDescription());
        try {
            classBlock(notifier).evaluate();
        } catch (AssumptionViolatedException e) {
            testNotifier.addFailedAssumption(e);
        } catch (StoppedByUserException e2) {
            throw e2;
        } catch (Throwable e3) {
            testNotifier.addFailure(e3);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: org.junit.runners.ParentRunner<T> */
    /* JADX DEBUG: Multi-variable search result rejected for r3v1, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // org.junit.runner.manipulation.Filterable
    public void filter(Filter filter) throws NoTestsRemainException {
        synchronized (this.childrenLock) {
            List<T> children = new ArrayList<>(getFilteredChildren());
            Iterator<T> iter = children.iterator();
            while (iter.hasNext()) {
                T each = iter.next();
                if (shouldRun(filter, each)) {
                    try {
                        filter.apply(each);
                    } catch (NoTestsRemainException e) {
                        iter.remove();
                    }
                } else {
                    iter.remove();
                }
            }
            this.filteredChildren = Collections.unmodifiableCollection(children);
            if (this.filteredChildren.isEmpty()) {
                throw new NoTestsRemainException();
            }
        }
    }

    @Override // org.junit.runner.manipulation.Sortable
    public void sort(Sorter sorter) {
        synchronized (this.childrenLock) {
            for (T each : getFilteredChildren()) {
                sorter.apply(each);
            }
            List<T> sortedChildren = new ArrayList<>(getFilteredChildren());
            Collections.sort(sortedChildren, comparator(sorter));
            this.filteredChildren = Collections.unmodifiableCollection(sortedChildren);
        }
    }

    private void validate() throws InitializationError {
        List<Throwable> errors = new ArrayList<>();
        collectInitializationErrors(errors);
        if (!errors.isEmpty()) {
            throw new InitializationError(errors);
        }
    }

    private Collection<T> getFilteredChildren() {
        if (this.filteredChildren == null) {
            synchronized (this.childrenLock) {
                if (this.filteredChildren == null) {
                    this.filteredChildren = Collections.unmodifiableCollection(getChildren());
                }
            }
        }
        return this.filteredChildren;
    }

    private boolean shouldRun(Filter filter, T each) {
        return filter.shouldRun(describeChild(each));
    }

    private Comparator<? super T> comparator(final Sorter sorter) {
        return new Comparator<T>() {
            /* class org.junit.runners.ParentRunner.AnonymousClass4 */

            @Override // java.util.Comparator
            public int compare(T o1, T o2) {
                return sorter.compare(ParentRunner.this.describeChild(o1), ParentRunner.this.describeChild(o2));
            }
        };
    }

    public void setScheduler(RunnerScheduler scheduler2) {
        this.scheduler = scheduler2;
    }
}
