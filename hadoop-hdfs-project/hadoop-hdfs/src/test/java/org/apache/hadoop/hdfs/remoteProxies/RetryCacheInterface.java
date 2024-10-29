package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface RetryCacheInterface {

    void lock();

    void unlock();

    LightWeightGSetInterface getCacheSet();

    RetryCacheMetricsInterface getMetricsForTests();

    String getCacheName();

    void addCacheEntry(byte[] clientId, int callId);

    void addCacheEntryWithPayload(byte[] clientId, int callId, Object payload);
}
