package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpi;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpiInterface;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface DataNodeInterface {
    int getIpcPort();
    void runDatanodeDaemon() throws IOException;
    InetSocketAddress getXferAddress();
    boolean isDatanodeFullyStarted();
    String getDisplayName();
    void shutdownDatanode(boolean forUpgrade) throws IOException;
    void shutdown();
    boolean isDatanodeUp();
    boolean isConnectedToNN(InetSocketAddress addr);
    void scheduleAllBlockReport(long delay);
    String getDatanodeHostname();

    //List<BPOfferService> getAllBpOs();
    //RPC.Server getIpcServer();
    //DatanodeIDInterface getDatanodeIdInterface();
    //FsDatasetSpiInterface getFSDataset();
    //void refreshNamenodes(Configuration conf) throws IOException;
}
