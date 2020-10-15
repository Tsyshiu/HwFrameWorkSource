package org.hamcrest.core;

import java.util.Arrays;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

public class AllOf<T> extends DiagnosingMatcher<T> {
    private final Iterable<Matcher<? super T>> matchers;

    public AllOf(Iterable<Matcher<? super T>> matchers2) {
        this.matchers = matchers2;
    }

    @Override // org.hamcrest.DiagnosingMatcher
    public boolean matches(Object o, Description mismatch) {
        for (Matcher<? super T> matcher : this.matchers) {
            if (!matcher.matches(o)) {
                mismatch.appendDescriptionOf(matcher).appendText(" ");
                matcher.describeMismatch(o, mismatch);
                return false;
            }
        }
        return true;
    }

    @Override // org.hamcrest.SelfDescribing
    public void describeTo(Description description) {
        description.appendList("(", " and ", ")", this.matchers);
    }

    public static <T> Matcher<T> allOf(Iterable<Matcher<? super T>> matchers2) {
        return new AllOf(matchers2);
    }

    @SafeVarargs
    public static <T> Matcher<T> allOf(Matcher<? super T>... matchers2) {
        return allOf(Arrays.asList(matchers2));
    }
}
