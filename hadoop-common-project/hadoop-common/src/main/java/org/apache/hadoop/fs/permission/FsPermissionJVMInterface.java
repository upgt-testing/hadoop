package org.apache.hadoop.fs.permission;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FsPermissionJVMInterface extends WritableJVMInterface {

    int hashCode();

    boolean getAclBit();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    short toOctal();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    short toShort();

    boolean getStickyBit();

    void fromShort(short arg0);

    java.lang.Object getUserAction();

    boolean getEncryptedBit();

    org.apache.hadoop.fs.permission.FsPermissionJVMInterface applyUMask_bridge(org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg0);

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.Object getOtherAction();

    java.lang.Object getGroupAction();

    short toExtendedShort();
}
