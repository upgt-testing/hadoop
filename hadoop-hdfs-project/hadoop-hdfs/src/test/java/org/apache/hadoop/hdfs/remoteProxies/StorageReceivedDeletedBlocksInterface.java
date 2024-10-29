package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface StorageReceivedDeletedBlocksInterface {

    String getStorageID();

    DatanodeStorageInterface getStorage();

    ReceivedDeletedBlockInfoInterface[] getBlocks();

    String toString();
}
