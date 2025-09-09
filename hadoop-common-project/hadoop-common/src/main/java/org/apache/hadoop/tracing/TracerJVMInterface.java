package org.apache.hadoop.tracing;

public interface TracerJVMInterface {

    org.apache.hadoop.tracing.TraceScopeJVMInterface newScope_bridge(java.lang.String arg0, org.apache.hadoop.tracing.SpanContextJVMInterface arg1);

    org.apache.hadoop.tracing.TraceScopeJVMInterface activateSpan_bridge(org.apache.hadoop.tracing.SpanJVMInterface arg0);

    org.apache.hadoop.tracing.SpanJVMInterface newSpan_bridge(java.lang.String arg0, org.apache.hadoop.tracing.SpanContextJVMInterface arg1);

    java.lang.String getName();

    org.apache.hadoop.tracing.TraceScopeJVMInterface newScope(java.lang.String arg0);

    void close();

    org.apache.hadoop.tracing.TraceScopeJVMInterface newScope_bridge(java.lang.String arg0, org.apache.hadoop.tracing.SpanContextJVMInterface arg1, boolean arg2);
}
