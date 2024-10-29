package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface StorageStatisticsInterface {

    String getName();

    String getScheme();

    //Iterator<LongStatistic> getLongStatistics();

    Long getLong(String key);

    boolean isTracked(String key);

    void reset();
}
