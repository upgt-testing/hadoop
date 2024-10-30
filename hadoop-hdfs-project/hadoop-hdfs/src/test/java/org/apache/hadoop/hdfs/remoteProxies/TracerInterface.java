package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.tracing.Span;
import org.apache.hadoop.tracing.SpanContext;

import java.util.*;
import java.io.*;

public interface TracerInterface {

    TraceScopeInterface newScope(String description);

    SpanInterface newSpan(String description, SpanContext spanCtx);

    TraceScopeInterface newScope(String description, SpanContext spanCtx);

    TraceScopeInterface newScope(String description, SpanContext spanCtx, boolean finishSpanOnClose);

    TraceScopeInterface activateSpan(Span span);

    void close();

    String getName();
}
