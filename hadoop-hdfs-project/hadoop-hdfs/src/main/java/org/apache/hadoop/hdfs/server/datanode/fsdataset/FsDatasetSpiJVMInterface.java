package org.apache.hadoop.hdfs.server.datanode.fsdataset;

import org.apache.hadoop.hdfs.protocol.BlockJVMInterface;
import org.apache.hadoop.hdfs.protocol.BlockListAsLongsJVMInterface;
import org.apache.hadoop.hdfs.server.datanode.ReplicaInfoJVMInterface;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorageJVMInterface;
import org.apache.hadoop.hdfs.server.protocol.StorageReportJVMInterface;
import org.apache.hadoop.hdfs.server.protocol.VolumeFailureSummaryJVMInterface;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FsDatasetSpiJVMInterface<V extends FsVolumeSpi> {
    BlockJVMInterface getStoredBlock(String bpid, long blkid);
    FsVolumeReferencesJVMInterface getFsVolumeReferences();
    //Map<DatanodeStorage, BlockListAsLongs> getBlockReports(String bpid);
    List<? extends ReplicaInfoJVMInterface> getFinalizedBlocks(String bpid);
    long getDfsUsed() throws IOException;
    StorageReportJVMInterface[] getStorageReports(String bpid)
            throws IOException;
    long getRemaining() throws IOException;
    VolumeFailureSummaryJVMInterface getVolumeFailureSummary();
    String getStorageInfo();
    long getCacheUsed();
    long getNumBlocksCached();
    Map<? extends DatanodeStorageJVMInterface, ? extends BlockListAsLongsJVMInterface> getBlockReports(String bpid);
}
