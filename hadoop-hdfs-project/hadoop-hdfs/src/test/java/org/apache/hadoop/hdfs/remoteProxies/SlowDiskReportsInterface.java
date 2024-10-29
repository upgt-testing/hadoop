package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.SlowDiskReports;

import java.util.*;
import java.io.*;

public interface SlowDiskReportsInterface {

    Map<String, Map<SlowDiskReports.DiskOp, Double>> getSlowDisks();

    boolean haveSlowDisks();

    boolean equals(Object o);

    int hashCode();
}
