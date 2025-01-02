package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeIDJVMInterface;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.UnregisteredNodeException;

import java.util.List;

public interface DatanodeManagerJVMInterface {
    DatanodeDescriptorJVMInterface getDatanode(DatanodeIDJVMInterface nodeID) throws UnregisteredNodeException;
    void markAllDatanodesStaleAndSetKeyUpdateIfNeed();
    List<? extends DatanodeDescriptorJVMInterface> getDatanodeListForReport(final HdfsConstants.DatanodeReportType type);
    //void fetchDatanodes(final List<DatanodeDescriptor> live,
      //                  final List<DatanodeDescriptor> dead, final boolean removeDecommissionNode);
}
