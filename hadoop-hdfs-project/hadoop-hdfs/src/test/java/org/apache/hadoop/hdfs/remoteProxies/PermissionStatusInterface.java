package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface PermissionStatusInterface {

    String getUserName();

    String getGroupName();

    FsPermissionInterface getPermission();

    void readFields(DataInput in);

    void write(DataOutput out);

    String toString();
}
