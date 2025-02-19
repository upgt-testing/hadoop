package org.apache.hadoop.hdfs.server.datanode.fsdataset;

import java.io.Closeable;

public interface FsVolumeReferencesJVMInterface extends Closeable, Iterable<FsVolumeSpi> {
    int size();
    FsVolumeSpiJVMInterface get(int index);

}
