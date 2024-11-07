package org.apache.hadoop.hdfs.remoteProxies;

public interface TraceScopeInterface {
    void addKVAnnotation(java.lang.String arg0, java.lang.Number arg1);
    void close();
    void addKVAnnotation(java.lang.String arg0, java.lang.String arg1);
    void detach();
    void reattach();
    SpanInterface getSpan();
    SpanInterface span();
    void addTimelineAnnotation(java.lang.String arg0);
}