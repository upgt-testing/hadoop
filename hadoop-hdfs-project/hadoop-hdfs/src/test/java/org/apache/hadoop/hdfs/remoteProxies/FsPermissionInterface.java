package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import java.util.*;
import java.io.*;

public interface FsPermissionInterface {

    FsAction getUserAction();

    FsAction getGroupAction();

    FsAction getOtherAction();

    void fromShort(short n);

    void write(DataOutput out);

    void readFields(DataInput in);

    FsPermissionInterface getMasked();

    FsPermissionInterface getUnmasked();

    short toShort();

    short toExtendedShort();

    short toOctal();

    boolean equals(Object obj);

    int hashCode();

    String toString();

    FsPermissionInterface applyUMask(FsPermission umask);

    boolean getStickyBit();

    boolean getAclBit();

    boolean getEncryptedBit();

    boolean getErasureCodedBit();

    void validateObject();
}
