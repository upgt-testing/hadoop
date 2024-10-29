package org.apache.hadoop.hdfs.remoteProxies;


import org.apache.hadoop.hdfs.server.protocol.OutlierMetrics;

import java.util.*;
import java.io.*;

public interface SlowPeerTrackerInterface {

    boolean isSlowPeerTrackerEnabled();

    void addReport(String slowNode, String reportingNode, OutlierMetrics slowNodeMetrics);

    //Set<SlowPeerLatencyWithReportingNode> getReportsForNode(String slowNode);

    //Map<String, SortedSet<SlowPeerLatencyWithReportingNode>> getReportsForAllDataNodes();

    String getJson();

    List<String> getSlowNodes(int numNodes);
}
