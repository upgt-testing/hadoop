package org.apache.hadoop.hdfs.remoteProxies;

public interface SpanInterface {
    void finish();
    SpanContextInterface getContext();
    void close();
    SpanInterface addTimelineAnnotation(java.lang.String arg0);
    SpanInterface addKVAnnotation(java.lang.String arg0, java.lang.String arg1);
}