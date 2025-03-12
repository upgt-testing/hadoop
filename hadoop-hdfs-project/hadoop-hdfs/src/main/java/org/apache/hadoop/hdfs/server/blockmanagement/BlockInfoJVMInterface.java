package org.apache.hadoop.hdfs.server.blockmanagement;

public interface BlockInfoJVMInterface {
    boolean isStriped();
    boolean isComplete();
    long getNumBytes();
    short getReplication();
    BlockUnderConstructionFeatureJVMInterface getUnderConstructionFeature();
}
