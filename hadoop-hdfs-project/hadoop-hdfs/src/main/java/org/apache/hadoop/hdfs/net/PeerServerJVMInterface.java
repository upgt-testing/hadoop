package org.apache.hadoop.hdfs.net;

import java.io.IOException;

public interface PeerServerJVMInterface {
    int getReceiveBufferSize() throws IOException;
}
