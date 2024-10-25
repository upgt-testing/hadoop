package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockManagerProxy {
    void clear();
    BlockIdManagerProxy getBlockIdManager();

}
