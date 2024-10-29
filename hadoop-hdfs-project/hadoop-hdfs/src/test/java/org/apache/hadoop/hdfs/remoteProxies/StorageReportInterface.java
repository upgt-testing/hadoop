package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface StorageReportInterface {

    DatanodeStorageInterface getStorage();

    boolean isFailed();

    long getCapacity();

    long getDfsUsed();

    long getNonDfsUsed();

    long getRemaining();

    long getBlockPoolUsed();
}
