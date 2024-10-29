package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface FsStatusInterface {

    long getCapacity();

    long getUsed();

    long getRemaining();

    void write(DataOutput out);

    void readFields(DataInput in);
}
