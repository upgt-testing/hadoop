package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface SpanInterface {

    SpanInterface addKVAnnotation(String key, String value);

    SpanInterface addTimelineAnnotation(String msg);

    SpanContextInterface getContext();

    void finish();

    void close();
}
