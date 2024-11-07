package org.apache.hadoop.hdfs.remoteProxies;

public interface DecoratedObjectFactoryInterface {
    <T> T decorate(T arg0);
    void clear();
    boolean removeDecorator(org.eclipse.jetty.util.Decorator arg0);
    java.util.Iterator<org.eclipse.jetty.util.Decorator> iterator();
    <T> T createInstance(java.lang.Class<T> arg0) throws java.lang.InstantiationException, java.lang.IllegalAccessException, java.lang.NoSuchMethodException, java.lang.reflect.InvocationTargetException;
//    java.util.Iterator<T> iterator();
    java.lang.String toString();
//    java.util.Spliterator<T> spliterator();
    void destroy(java.lang.Object arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    void setDecorators(java.util.List<? extends org.eclipse.jetty.util.Decorator> arg0);
    java.util.List<org.eclipse.jetty.util.Decorator> getDecorators();
    void addDecorator(org.eclipse.jetty.util.Decorator arg0);
}