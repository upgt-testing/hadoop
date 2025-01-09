package org.apache.hadoop.hdfs.server.datanode;

import java.net.URI;

public interface ReplicaInfoJVMInterface {
    URI getBlockURI();
    URI getMetadataURI();
}
