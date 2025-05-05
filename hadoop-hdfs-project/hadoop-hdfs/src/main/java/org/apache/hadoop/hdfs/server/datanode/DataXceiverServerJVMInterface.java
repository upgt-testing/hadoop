package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.net.PeerServerJVMInterface;
import org.apache.hadoop.hdfs.util.DataTransferThrottlerJVMInterface;

public interface DataXceiverServerJVMInterface {
    PeerServerJVMInterface getPeerServer();
}
