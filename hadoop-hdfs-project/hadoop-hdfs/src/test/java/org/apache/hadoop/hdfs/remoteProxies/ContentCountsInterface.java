package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.server.namenode.Content;
import org.apache.hadoop.hdfs.server.namenode.ContentCounts;
import org.apache.hadoop.hdfs.util.EnumCounters;

public interface ContentCountsInterface {

    long getFileCount();

    long getDirectoryCount();

    long getSymlinkCount();

    long getLength();

    long getStoragespace();

    long getSnapshotCount();

    long getSnapshotableDirectoryCount();

    long[] getTypeSpaces();

    long getTypeSpace(StorageType t);

    void addContent(Content c, long val);

    void addContents(ContentCounts that);

    void addTypeSpace(StorageType t, long val);

    void addTypeSpaces(EnumCounters<StorageType> that);
}
