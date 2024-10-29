package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.StorageType;

import java.util.*;
import java.io.*;

public interface ContentSummaryInterface {

    long getLength();

    long getSnapshotLength();

    long getDirectoryCount();

    long getSnapshotDirectoryCount();

    long getFileCount();

    long getSnapshotFileCount();

    long getSnapshotSpaceConsumed();

    String getErasureCodingPolicy();

    void write(DataOutput out);

    void readFields(DataInput in);

    boolean equals(Object to);

    int hashCode();

    String toString();

    String toString(boolean qOption);

    String toString(boolean qOption, boolean hOption);

    String toString(boolean qOption, boolean hOption, boolean xOption);

    String toString(boolean qOption, boolean hOption, boolean tOption, List<StorageType> types);

    String toString(boolean qOption, boolean hOption, boolean tOption, boolean xOption, List<StorageType> types);

    String toSnapshot(boolean hOption);
}
