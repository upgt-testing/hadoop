package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;
//import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;

public interface DatanodeStorageInfoInterface {

    void updateFromStorage(DatanodeStorageInterface storage);

    int getBlockReportCount();

    boolean areBlockContentsStale();

    void setBlockContentsStale(boolean value);

    void setUtilizationForTesting(long capacity, long dfsUsed, long remaining, long blockPoolUsed);

    String getStorageID();

    StorageType getStorageType();

    //AddBlockResult addBlock(BlockInfo b, BlockInterface reportedBlock);

    //void insertToList(BlockInfo b);

    DatanodeDescriptorInterface getDatanodeDescriptor();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
