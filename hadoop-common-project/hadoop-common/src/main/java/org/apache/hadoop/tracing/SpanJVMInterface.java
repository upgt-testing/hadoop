package org.apache.hadoop.tracing;

public interface SpanJVMInterface {

    org.apache.hadoop.tracing.SpanContextJVMInterface getContext();

    org.apache.hadoop.tracing.SpanJVMInterface addTimelineAnnotation(java.lang.String arg0);

    void finish();

    org.apache.hadoop.tracing.SpanJVMInterface addKVAnnotation(java.lang.String arg0, java.lang.String arg1);

    void close();
}
