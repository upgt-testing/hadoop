package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.StorageType;

import java.util.*;
import java.io.*;

public interface QuotaUsageInterface {

    long getFileAndDirectoryCount();

    long getQuota();

    long getSpaceConsumed();

    long getSpaceQuota();

    long getTypeQuota(StorageType type);

    long getTypeConsumed(StorageType type);

    boolean isTypeQuotaSet();

    boolean isTypeConsumedAvailable();

    int hashCode();

    boolean equals(Object obj);

    String toString();

    String toString(boolean hOption);

    String toString(boolean hOption, boolean tOption, List<StorageType> types);
}
