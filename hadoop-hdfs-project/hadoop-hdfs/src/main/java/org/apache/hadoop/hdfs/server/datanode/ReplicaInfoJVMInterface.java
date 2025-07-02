package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.protocol.BlockJVMInterface;

import java.net.URI;

public interface ReplicaInfoJVMInterface extends BlockJVMInterface {
    URI getBlockURI();
    URI getMetadataURI();
}
