package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.OutlierMetrics;

import java.util.*;
import java.io.*;

public interface DataNodePeerMetricsInterface {

    String name();

    long getMinOutlierDetectionSamples();

    void addSendPacketDownstream(String peerAddr, long elapsedMs);

    String dumpSendPacketDownstreamAvgInfoAsJson();

    void collectThreadLocalStates();

    Map<String, OutlierMetrics> getOutliers();

    void setTestOutliers(Map<String, OutlierMetrics> outlier);

    MutableRollingAveragesInterface getSendPacketDownstreamRollingAverages();

    void setMinOutlierDetectionNodes(long minNodes);

    long getMinOutlierDetectionNodes();

    void setLowThresholdMs(long thresholdMs);

    long getLowThresholdMs();

    void setMinOutlierDetectionSamples(long minSamples);

    OutlierDetectorInterface getSlowNodeDetector();
}
