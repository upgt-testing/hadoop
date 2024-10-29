package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;

public interface BlockStoragePolicyInterface {

    List<StorageType> chooseStorageTypes(short replication);

    List<StorageType> chooseStorageTypes(short replication, Iterable<StorageType> chosen);

    List<StorageType> chooseStorageTypes(short replication, Iterable<StorageType> chosen, EnumSet<StorageType> unavailables, boolean isNewBlock);

    List<StorageType> chooseExcess(short replication, Iterable<StorageType> chosen);

    StorageType getCreationFallback(EnumSet<StorageType> unavailables);

    StorageType getReplicationFallback(EnumSet<StorageType> unavailables);

    int hashCode();

    boolean equals(Object obj);

    String toString();

    byte getId();

    String getName();

    StorageType[] getStorageTypes();

    StorageType[] getCreationFallbacks();

    StorageType[] getReplicationFallbacks();

    boolean isCopyOnCreateFile();
}
