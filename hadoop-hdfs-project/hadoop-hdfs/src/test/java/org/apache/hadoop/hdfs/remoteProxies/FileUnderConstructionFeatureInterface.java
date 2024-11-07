package org.apache.hadoop.hdfs.remoteProxies;

public interface FileUnderConstructionFeatureInterface {
    void updateLengthOfLastBlock(INodeFileInterface arg0, long arg1) throws java.io.IOException;
    java.lang.String getClientMachine();
    void setClientName(java.lang.String arg0);
    java.lang.String getClientName();
    void cleanZeroSizeBlock(INodeFileInterface arg0, BlocksMapUpdateInfoInterface arg1);
}