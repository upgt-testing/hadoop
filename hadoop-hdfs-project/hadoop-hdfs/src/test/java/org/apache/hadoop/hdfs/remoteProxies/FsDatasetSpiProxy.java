package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi;

public interface FsDatasetSpiProxy<V extends FsVolumeSpi> {
    long getCacheUsed();
    long getNumBlocksCached();
    long getDfsUsed();
}
