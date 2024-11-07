package org.apache.hadoop.hdfs.remoteProxies;

public interface IncludeExcludeSetInterface<T, P> {
    void include(T arg0);
    java.lang.String toString();
    java.util.Set<T> getIncluded();
    boolean hasIncludes();
    boolean hasExcludes();
    java.util.function.Predicate<T> and(java.util.function.Predicate<? super T> arg0);
    <T> java.util.function.Predicate<T> isEqual(java.lang.Object arg0);
    java.util.function.Predicate<T> negate();
    void clear();
    void include(T... arg0);
    <T1, T2> boolean matchCombined(T1 arg0, IncludeExcludeSetInterface<?, T1> arg1, T2 arg2, IncludeExcludeSetInterface<?, T2> arg3);
    <T> java.util.function.Predicate<T> not(java.util.function.Predicate<? super T> arg0);
    java.lang.Boolean isIncludedAndNotExcluded(P arg0);
    void exclude(T arg0);
    java.util.function.Predicate<T> or(java.util.function.Predicate<? super T> arg0);
    boolean test(P arg0);
    boolean matches(P arg0);
    void exclude(T... arg0);
    boolean isEmpty();
    int size();
    java.util.Set<T> getExcluded();
}