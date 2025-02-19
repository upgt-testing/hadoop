package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeIDJVMInterface;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.UnregisteredNodeException;

import java.util.List;

public interface DatanodeManagerJVMInterface {
    void setHeartbeatExpireInterval(long expireInterval);
    HeartbeatManagerJVMInterface getHeartbeatManager();
    DatanodeDescriptorJVMInterface getDatanode(DatanodeIDJVMInterface nodeID) throws UnregisteredNodeException;
    DatanodeDescriptorJVMInterface getDatanode(String nodeID);
    void markAllDatanodesStale();
    List<? extends DatanodeDescriptorJVMInterface> getDatanodeListForReport(final HdfsConstants.DatanodeReportType type);
    //void fetchDatanodes(final List<DatanodeDescriptor> live,
      //                  final List<DatanodeDescriptor> dead, final boolean removeDecommissionNode);
    SlowDiskTrackerJVMInterface getSlowDiskTracker();
}
