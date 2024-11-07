package org.apache.hadoop.hdfs.remoteProxies;

public interface IntrusiveCollectionInterface<E> {
    java.util.Spliterator<E> spliterator();
    boolean retainAll(java.util.Collection<?> arg0);
    java.util.stream.Stream<E> parallelStream();
    org.apache.hadoop.util.IntrusiveCollection.Element removeElement(org.apache.hadoop.util.IntrusiveCollection.Element arg0);
    boolean addFirst(org.apache.hadoop.util.IntrusiveCollection.Element arg0);
    java.util.stream.Stream<E> stream();
//    java.util.Iterator<T> iterator();
    <T> T[] toArray(java.util.function.IntFunction<T[]> arg0);
    boolean removeAll(java.util.Collection<?> arg0);
    <T> T[] toArray(T[] arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    java.lang.Object[] toArray();
    boolean remove(java.lang.Object arg0);
    java.util.Iterator<E> iterator();
//    java.util.Spliterator<T> spliterator();
    int size();
    boolean contains(java.lang.Object arg0);
    void clear();
    boolean containsAll(java.util.Collection<?> arg0);
    boolean addAll(java.util.Collection<? extends E> arg0);
    boolean isEmpty();
    boolean removeIf(java.util.function.Predicate<? super E> arg0);
    boolean add(E arg0);
}