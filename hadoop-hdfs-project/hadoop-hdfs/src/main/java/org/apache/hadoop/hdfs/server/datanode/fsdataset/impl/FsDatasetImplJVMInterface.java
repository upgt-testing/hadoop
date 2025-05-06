package org.apache.hadoop.hdfs.server.datanode.fsdataset.impl;

import org.apache.hadoop.hdfs.server.datanode.ReplicaInfoJVMInterface;

import java.io.IOException;

public interface FsDatasetImplJVMInterface {
    ReplicaInfoJVMInterface getReplicaInfo(String bpid, long blockId) throws IOException;
}
