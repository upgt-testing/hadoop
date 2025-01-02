package org.apache.hadoop.hdfs.server.namenode;

public interface FSEditLogJVMInterface {
    long getLastWrittenTxId();
}
