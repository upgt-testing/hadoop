package org.apache.hadoop.hdfs.server.datanode.fsdataset;

import org.apache.hadoop.hdfs.server.datanode.ReplicaInfo;
import org.apache.hadoop.hdfs.server.datanode.ReplicaInfoJVMInterface;
import org.apache.hadoop.hdfs.server.protocol.StorageReportJVMInterface;

import java.io.IOException;
import java.util.List;

public interface FsDatasetSpiJVMInterface<V extends FsVolumeSpi> {
    FsVolumeReferencesJVMInterface getFsVolumeReferences();
    //Map<DatanodeStorage, BlockListAsLongs> getBlockReports(String bpid);
    List<? extends ReplicaInfoJVMInterface> getFinalizedBlocks(String bpid);
    long getDfsUsed() throws IOException;
    StorageReportJVMInterface[] getStorageReports(String bpid)
            throws IOException;
}
