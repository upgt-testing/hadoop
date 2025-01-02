package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.protocol.DatanodeIDJVMInterface;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpiJVMInterface;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistrationJVMInterface;
import org.apache.hadoop.hdfs.server.protocol.StorageReport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public interface DataNodeJVMInterface {
    int getXceiverCount();
    boolean isBPServiceAlive(String bpid);
    String getDatanodeUuid();
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

    FsDatasetSpiJVMInterface<?> getFSDataset();
    List<? extends BPOfferServiceJVMInterface> getAllBpOs();
    //RPC.Server getIpcServer();
    DatanodeIDJVMInterface getDatanodeId();
    //FsDatasetSpiInterface getFSDataset();
    //void refreshNamenodes(Configuration conf) throws IOException;
    DatanodeRegistrationJVMInterface getDNRegistrationForBP(String bpid) throws IOException;
    void setHeartbeatsDisabledForTests(boolean heartbeatsDisabledForTests);
}
