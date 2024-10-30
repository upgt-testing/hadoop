package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface TraceScopeInterface {

    void addKVAnnotation(String key, String value);

    void addKVAnnotation(String key, Number value);

    void addTimelineAnnotation(String msg);

    SpanInterface span();

    SpanInterface getSpan();

    void reattach();

    void detach();

    void close();
}
