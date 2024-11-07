package org.apache.hadoop.hdfs.remoteProxies;

public interface ResponseInterface {
//    void forEach(java.util.function.Consumer<? super T> arg0);
    int getStatus();
    HttpFieldsInterface getFields();
    java.lang.String getReason();
    boolean isRequest();
    java.lang.String toString();
    org.eclipse.jetty.http.HttpVersion getVersion();
    void setTrailerSupplier(java.util.function.Supplier<org.eclipse.jetty.http.HttpFields> arg0);
    long getContentLength();
    java.util.Iterator<org.eclipse.jetty.http.HttpField> iterator();
    boolean isResponse();
    void setReason(java.lang.String arg0);
//    java.util.Iterator<T> iterator();
    void setStatus(int arg0);
    org.eclipse.jetty.http.HttpVersion getHttpVersion();
    void recycle();
    void setHttpVersion(org.eclipse.jetty.http.HttpVersion arg0);
    java.util.function.Supplier<org.eclipse.jetty.http.HttpFields> getTrailerSupplier();
//    java.util.Spliterator<T> spliterator();
    void setContentLength(long arg0);
}