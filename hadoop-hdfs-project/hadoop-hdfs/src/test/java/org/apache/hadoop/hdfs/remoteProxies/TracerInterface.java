package org.apache.hadoop.hdfs.remoteProxies;

public interface TracerInterface {
    SpanInterface getCurrentSpan();
    TraceScopeInterface activateSpan(SpanInterface arg0);
    TracerInterface curThreadTracer();
    TraceScopeInterface newScope(java.lang.String arg0);
    SpanInterface newSpan(java.lang.String arg0, SpanContextInterface arg1);
    TraceScopeInterface newScope(java.lang.String arg0, SpanContextInterface arg1);
    java.lang.String getName();
    TraceScopeInterface newScope(java.lang.String arg0, SpanContextInterface arg1, boolean arg2);
    void close();
}