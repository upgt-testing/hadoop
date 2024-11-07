package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectiveListInterface {
//    boolean add(E arg0);
//    java.util.Spliterator<E> spliterator();
//    void forEach(java.util.function.Consumer<? super T> arg0);
    org.apache.hadoop.util.IntrusiveCollection.Element removeElement(org.apache.hadoop.util.IntrusiveCollection.Element arg0);
    void clear();
//    java.util.Spliterator<T> spliterator();
    boolean remove(java.lang.Object arg0);
    boolean addFirst(org.apache.hadoop.util.IntrusiveCollection.Element arg0);
    <T> T[] toArray(T[] arg0);
    boolean contains(java.lang.Object arg0);
//    boolean removeIf(java.util.function.Predicate<? super E> arg0);
    boolean retainAll(java.util.Collection<?> arg0);
    int size();
//    boolean addAll(java.util.Collection<? extends E> arg0);
//    java.util.Iterator<E> iterator();
//    java.util.stream.Stream<E> stream();
//    java.util.Iterator<T> iterator();
    boolean removeAll(java.util.Collection<?> arg0);
//    java.util.stream.Stream<E> parallelStream();
    boolean isEmpty();
    CachePoolInterface getCachePool();
    boolean containsAll(java.util.Collection<?> arg0);
    java.lang.Object[] toArray();
    <T> T[] toArray(java.util.function.IntFunction<T[]> arg0);
}