package org.apache.hadoop.hdfs.remoteProxies;

public interface ImmutableCollectionInterface<E> {
    boolean addAll(java.util.Collection<? extends E> arg0);
    java.util.Iterator<E> iterator();
    int internalArrayStart();
    java.lang.Object[] internalArray();
    int internalArrayEnd();
//    java.util.Spliterator<T> spliterator();
    boolean isPartialView();
    boolean containsAll(java.util.Collection<?> arg0);
    boolean remove(java.lang.Object arg0);
    int copyIntoArray(java.lang.Object[] arg0, int arg1);
    boolean removeIf(java.util.function.Predicate<? super E> arg0);
    <T> T[] toArray(T[] arg0);
    void clear();
    java.util.stream.Stream<E> parallelStream();
    boolean retainAll(java.util.Collection<?> arg0);
//    java.util.Iterator<T> iterator();
    java.lang.Object writeReplace();
    <T> T[] toArray(java.util.function.IntFunction<T[]> arg0);
    java.lang.String toString();
    java.util.stream.Stream<E> stream();
    boolean add(E arg0);
    boolean removeAll(java.util.Collection<?> arg0);
    <T> T[] finishToArray(T[] arg0, java.util.Iterator<?> arg1);
    java.lang.Object[] toArray();
//    UnmodifiableIteratorInterface<E> iterator();
    ImmutableListInterface<E> asList();
    int hugeCapacity(int arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    int size();
    boolean contains(java.lang.Object arg0);
    boolean isEmpty();
    java.util.Spliterator<E> spliterator();
}