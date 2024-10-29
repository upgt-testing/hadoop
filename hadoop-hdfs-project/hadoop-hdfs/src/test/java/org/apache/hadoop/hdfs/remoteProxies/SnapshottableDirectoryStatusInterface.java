package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.Path;

public interface SnapshottableDirectoryStatusInterface {

    int getSnapshotNumber();

    int getSnapshotQuota();

    byte[] getParentFullPath();

    HdfsFileStatusInterface getDirStatus();

    Path getFullPath();
}
