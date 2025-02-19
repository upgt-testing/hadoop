package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.net.PeerServerJVMInterface;

public interface DataXceiverServerJVMInterface {
    PeerServerJVMInterface getPeerServer();
}
