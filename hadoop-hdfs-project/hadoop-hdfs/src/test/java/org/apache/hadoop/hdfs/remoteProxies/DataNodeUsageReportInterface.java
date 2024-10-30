package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DataNodeUsageReportInterface {

    String toString();

    int hashCode();

    boolean equals(Object o);

    long getBytesWrittenPerSec();

    long getBytesReadPerSec();

    long getWriteTime();

    long getReadTime();

    long getBlocksWrittenPerSec();

    long getBlocksReadPerSec();

    long getTimestamp();
}
