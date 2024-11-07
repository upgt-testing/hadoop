package org.apache.hadoop.hdfs.remoteProxies;

public interface ChunkChecksumInterface {
    long getDataLength();
    byte[] getChecksum();
}