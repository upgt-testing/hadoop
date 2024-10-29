package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.server.namenode.QuotaCounts;

public interface QuotaCountsInterface {

    QuotaCountsInterface add(QuotaCounts that);

    QuotaCountsInterface subtract(QuotaCounts that);

    QuotaCountsInterface negation();

    long getNameSpace();

    void setNameSpace(long nameSpaceCount);

    void addNameSpace(long nsDelta);

    long getStorageSpace();

    void setStorageSpace(long spaceCount);

    void addStorageSpace(long dsDelta);

    EnumCountersInterface getTypeSpaces();

    void addTypeSpace(StorageType type, long delta);

    boolean anyNsSsCountGreaterOrEqual(long val);

    boolean anyTypeSpaceCountGreaterOrEqual(long val);

    String toString();

    boolean equals(Object obj);

    int hashCode();
}
