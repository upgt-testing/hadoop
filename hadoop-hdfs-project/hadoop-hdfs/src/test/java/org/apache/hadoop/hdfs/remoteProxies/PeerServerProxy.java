package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.net.PeerServer;

public interface PeerServerProxy {
    int getReceiveBufferSize();
}
