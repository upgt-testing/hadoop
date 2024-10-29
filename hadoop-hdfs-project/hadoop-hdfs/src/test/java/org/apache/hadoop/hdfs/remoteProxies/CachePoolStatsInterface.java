package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface CachePoolStatsInterface {

    long getBytesNeeded();

    long getBytesCached();

    long getBytesOverlimit();

    long getFilesNeeded();

    long getFilesCached();

    String toString();
}
