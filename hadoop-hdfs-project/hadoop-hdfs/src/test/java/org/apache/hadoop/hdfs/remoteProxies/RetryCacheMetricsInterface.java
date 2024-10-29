package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface RetryCacheMetricsInterface {

    String getName();

    void incrCacheHit();

    void incrCacheCleared();

    void incrCacheUpdated();

    long getCacheHit();

    long getCacheCleared();

    long getCacheUpdated();
}
