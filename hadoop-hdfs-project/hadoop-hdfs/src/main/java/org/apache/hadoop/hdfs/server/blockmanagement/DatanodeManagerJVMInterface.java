package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.protocol.HdfsConstants;

import java.util.List;

public interface DatanodeManagerJVMInterface {
    List<? extends DatanodeDescriptorJVMInterface> getDatanodeListForReport(final HdfsConstants.DatanodeReportType type);
    //void fetchDatanodes(final List<DatanodeDescriptor> live,
      //                  final List<DatanodeDescriptor> dead, final boolean removeDecommissionNode);
}
