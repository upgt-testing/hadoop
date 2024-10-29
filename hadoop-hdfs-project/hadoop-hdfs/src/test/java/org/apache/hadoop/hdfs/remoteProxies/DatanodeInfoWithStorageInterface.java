package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;

public interface DatanodeInfoWithStorageInterface {

    String getStorageID();

    StorageType getStorageType();

    boolean equals(Object o);

    int hashCode();

    String toString();
}
