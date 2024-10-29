package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

public interface FileStatusInterface {

    long getLen();

    boolean isFile();

    boolean isDirectory();

    boolean isDir();

    boolean isSymlink();

    long getBlockSize();

    short getReplication();

    long getModificationTime();

    long getAccessTime();

    FsPermission getPermission();

    boolean hasAcl();

    boolean isEncrypted();

    boolean isErasureCoded();

    boolean isSnapshotEnabled();

    String getOwner();

    String getGroup();

    PathInterface getPath();

    void setPath(Path p);

    PathInterface getSymlink();

    void setSymlink(Path p);

    int compareTo(FileStatus o);

    int compareTo(Object o);

    boolean equals(Object o);

    int hashCode();

    String toString();

    void readFields(DataInput in);

    void write(DataOutput out);

    void validateObject();
}
