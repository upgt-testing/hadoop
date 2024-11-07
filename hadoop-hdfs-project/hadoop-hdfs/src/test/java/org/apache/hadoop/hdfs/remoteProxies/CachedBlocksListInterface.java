package org.apache.hadoop.hdfs.remoteProxies;

public interface CachedBlocksListInterface {
//    java.util.stream.Stream<E> parallelStream();
    java.lang.Object[] toArray();
//    java.util.Iterator<T> iterator();
    org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor.CachedBlocksList.Type getType();
    boolean addFirst(org.apache.hadoop.util.IntrusiveCollection.Element arg0);
//    java.util.stream.Stream<E> stream();
//    java.util.Spliterator<T> spliterator();
    boolean contains(java.lang.Object arg0);
    <T> T[] toArray(java.util.function.IntFunction<T[]> arg0);
    boolean retainAll(java.util.Collection<?> arg0);
//    java.util.Spliterator<E> spliterator();
    boolean removeAll(java.util.Collection<?> arg0);
    void clear();
    DatanodeDescriptorInterface getDatanode();
    boolean remove(java.lang.Object arg0);
//    boolean addAll(java.util.Collection<? extends E> arg0);
    org.apache.hadoop.util.IntrusiveCollection.Element removeElement(org.apache.hadoop.util.IntrusiveCollection.Element arg0);
//    java.util.Iterator<E> iterator();
//    boolean add(E arg0);
    boolean isEmpty();
    int size();
//    void forEach(java.util.function.Consumer<? super T> arg0);
//    boolean removeIf(java.util.function.Predicate<? super E> arg0);
    boolean containsAll(java.util.Collection<?> arg0);
    <T> T[] toArray(T[] arg0);
}