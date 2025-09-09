package org.apache.hadoop.tracing;

public interface TraceScopeJVMInterface {

    void addKVAnnotation(java.lang.String arg0, java.lang.Number arg1);

    org.apache.hadoop.tracing.SpanJVMInterface span();

    org.apache.hadoop.tracing.SpanJVMInterface getSpan();

    void reattach();

    void addTimelineAnnotation(java.lang.String arg0);

    void detach();

    void addKVAnnotation(java.lang.String arg0, java.lang.String arg1);

    void close();
}
