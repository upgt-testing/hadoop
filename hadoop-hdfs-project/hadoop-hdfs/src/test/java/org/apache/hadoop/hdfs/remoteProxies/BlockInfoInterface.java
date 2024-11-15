package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockInfoInterface extends BlockInterface {
    void readId(java.io.DataInput arg0) throws java.io.IOException;
    void delete();
    BlockUnderConstructionFeatureInterface getUnderConstructionFeature();
    void readHelper(java.io.DataInput arg0) throws java.io.IOException;
    short getReplication();
    long getBlockCollectionId();
    BlockInfoInterface moveBlockToHead(BlockInfoInterface arg0, DatanodeStorageInfoInterface arg1, int arg2, int arg3);
    void setBlockId(long arg0);
    void setReplication(short arg0);
    void setStorageInfo(int arg0, DatanodeStorageInfoInterface arg1);
    long getGenerationStamp();
    long filename2id(java.lang.String arg0);
    int hashCode();
    boolean isComplete();
    java.lang.String toString(BlockInterface arg0);
    DatanodeStorageInfoInterface findStorageInfo(DatanodeDescriptorInterface arg0);
    long getNumBytes();
//    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.ReplicaUnderConstruction> setGenerationStampAndVerifyReplicas(long arg0);
    BlockInfoInterface setPrevious(int arg0, BlockInfoInterface arg1);
    boolean isProvided();
    BlockInfoInterface listInsert(BlockInfoInterface arg0, DatanodeStorageInfoInterface arg1);
    long getBlockId(java.lang.String arg0);
    int findStorageInfo(DatanodeStorageInfoInterface arg0);
    boolean isCompleteOrCommitted();
    boolean removeStorage(DatanodeStorageInfoInterface arg0);
    BlockInfoInterface setNext(int arg0, BlockInfoInterface arg1);
    void setNumBytes(long arg0);
    long getBlockId();
    void appendStringTo(java.lang.StringBuilder arg0);
    boolean isDeleted();
    boolean matchingIdAndGenStamp(BlockInterface arg0, BlockInterface arg1);
    int numNodes();
    org.apache.hadoop.hdfs.protocol.BlockType getBlockType();
    void set(long arg0, long arg1, long arg2);
    java.io.File metaToBlockFile(java.io.File arg0);
    java.lang.String toString();
    void convertToBlockUnderConstruction(org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState arg0, DatanodeStorageInfoInterface[] arg1);
    long getGenerationStamp(java.lang.String arg0);
    org.apache.hadoop.util.LightWeightGSet.LinkedElement getNext();
    boolean isMetaFilename(java.lang.String arg0);
    BlockInfoInterface getPrevious(int arg0);
    void convertToCompleteBlock();
    boolean isBlockFilename(java.io.File arg0);
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void setBlockCollectionId(long arg0);
    boolean isUnderRecovery();
    boolean isStriped();
    void setGenerationStamp(long arg0);
    java.util.Iterator<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> getStorageInfos();
    boolean hasNoStorage();
    java.lang.String getBlockName();
    boolean equals(java.lang.Object arg0);
    void setNext(org.apache.hadoop.util.LightWeightGSet.LinkedElement arg0);
    void writeId(java.io.DataOutput arg0) throws java.io.IOException;
    boolean addStorage(DatanodeStorageInfoInterface arg0, BlockInterface arg1);
    DatanodeStorageInfoInterface getStorageInfo(int arg0);
    BlockInfoInterface listRemove(BlockInfoInterface arg0, DatanodeStorageInfoInterface arg1);
    void writeHelper(java.io.DataOutput arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState getBlockUCState();
//    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.ReplicaUnderConstruction> commitBlock(BlockInterface arg0) throws java.io.IOException;
    DatanodeDescriptorInterface getDatanode(int arg0);
    int compareTo(BlockInterface arg0);
    int getCapacity();
    BlockInfoInterface getNext(int arg0);
}