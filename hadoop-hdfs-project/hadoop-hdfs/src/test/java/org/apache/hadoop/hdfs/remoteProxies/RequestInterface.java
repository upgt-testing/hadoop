package org.apache.hadoop.hdfs.remoteProxies;

public interface RequestInterface {
    void recycle();
    java.util.Iterator<org.eclipse.jetty.http.HttpField> iterator();
    void setContentLength(long arg0);
    void setHttpVersion(org.eclipse.jetty.http.HttpVersion arg0);
    void setMethod(java.lang.String arg0);
    org.eclipse.jetty.http.HttpVersion getHttpVersion();
    java.lang.String toString();
    org.eclipse.jetty.http.HttpVersion getVersion();
    java.util.function.Supplier<org.eclipse.jetty.http.HttpFields> getTrailerSupplier();
    HttpURIInterface getURI();
    boolean isResponse();
    HttpFieldsInterface getFields();
//    java.util.Iterator<T> iterator();
    java.lang.String getMethod();
//    void forEach(java.util.function.Consumer<? super T> arg0);
    long getContentLength();
//    java.util.Spliterator<T> spliterator();
    java.lang.String getURIString();
    boolean isRequest();
    void setTrailerSupplier(java.util.function.Supplier<org.eclipse.jetty.http.HttpFields> arg0);
    void setURI(HttpURIInterface arg0);
}