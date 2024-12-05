package org.apache.hadoop.hdfs.server.datanode.fsdataset;

public interface FsDatasetSpiJVMInterface<V extends FsVolumeSpi> {
    FsVolumeReferencesJVMInterface getFsVolumeReferences();
    //Map<DatanodeStorage, BlockListAsLongs> getBlockReports(String bpid);
}
