package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface JvmPauseMonitorInterface {

    boolean isStarted();

    long getNumGcWarnThresholdExceeded();

    long getNumGcInfoThresholdExceeded();

    long getTotalGcExtraSleepTime();
}
