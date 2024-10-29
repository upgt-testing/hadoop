package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;

public interface DatanodeStorageInterface {

    String getStorageID();

    StateInterface getState();

    StorageType getStorageType();

    String toString();

    boolean equals(Object other);

    int hashCode();
}
