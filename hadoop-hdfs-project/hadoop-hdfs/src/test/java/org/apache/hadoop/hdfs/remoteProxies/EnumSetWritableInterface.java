package org.apache.hadoop.hdfs.remoteProxies;

public interface EnumSetWritableInterface<E> {
    <T> T[] toArray(T[] arg0);
    java.util.Spliterator<E> spliterator();
    boolean equals(java.lang.Object arg0);
    boolean isEmpty();
//    java.util.Spliterator<T> spliterator();
    <T> T[] toArray(java.util.function.IntFunction<T[]> arg0);
//    void set(java.util.EnumSet<E> arg0, java.lang.Class<E> arg1);
    java.lang.String toString();
    boolean removeIf(java.util.function.Predicate<? super E> arg0);
    boolean containsAll(java.util.Collection<?> arg0);
    void setConf(ConfigurationInterface arg0);
    int size();
    ConfigurationInterface getConf();
    <T> T[] finishToArray(T[] arg0, java.util.Iterator<?> arg1);
    java.util.stream.Stream<E> stream();
    java.util.stream.Stream<E> parallelStream();
    boolean retainAll(java.util.Collection<?> arg0);
    void clear();
    boolean removeAll(java.util.Collection<?> arg0);
    int hashCode();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    boolean addAll(java.util.Collection<? extends E> arg0);
    boolean add(E arg0);
    int hugeCapacity(int arg0);
    boolean contains(java.lang.Object arg0);
    boolean remove(java.lang.Object arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    java.util.Iterator<E> iterator();
//    java.util.Iterator<T> iterator();
    java.lang.Object[] toArray();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    java.lang.Class<E> getElementType();
//    java.util.EnumSet<E> get();
}