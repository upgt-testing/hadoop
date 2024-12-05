package org.apache.hadoop.hdfs.server.datanode.fsdataset;

import org.apache.hadoop.hdfs.protocol.BlockListAsLongs;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;

import java.util.Map;

public interface FsDatasetSpiInterface<V extends FsVolumeSpi> {
    FsVolumeReferencesInterface getFsVolumeReferences();
    //Map<DatanodeStorage, BlockListAsLongs> getBlockReports(String bpid);
}
