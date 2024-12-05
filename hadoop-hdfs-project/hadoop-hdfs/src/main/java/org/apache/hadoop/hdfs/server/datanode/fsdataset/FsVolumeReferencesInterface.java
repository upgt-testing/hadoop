package org.apache.hadoop.hdfs.server.datanode.fsdataset;

import java.io.Closeable;

public interface FsVolumeReferencesInterface extends Closeable, Iterable<FsVolumeSpi> {
    int size();
}
