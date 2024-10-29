package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.OutlierMetrics;

import java.util.*;
import java.io.*;

public interface SlowPeerReportsInterface {

    Map<String, OutlierMetricsInterface> getSlowPeers();

    boolean haveSlowPeers();

    boolean equals(Object o);

    int hashCode();
}
