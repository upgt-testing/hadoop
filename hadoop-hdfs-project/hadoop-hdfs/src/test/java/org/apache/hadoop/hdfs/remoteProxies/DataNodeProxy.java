package org.apache.hadoop.hdfs.remoteProxies;

public interface DataNodeProxy {
    boolean testRMIPrint(String message);
    long getBalancerBandwidth();
    int getIpcPort();
    String getIpcHostName();
    DataXceiverServerProxy getXferServer();
    int getInfoPort();
    FsDatasetSpiProxy<?> getFSDataset();
    DataNodeMetricsProxy getMetrics();
    void shutdown();
    int getXferPort();
}
