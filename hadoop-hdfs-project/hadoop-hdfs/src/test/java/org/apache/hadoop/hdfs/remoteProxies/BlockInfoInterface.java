package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface BlockInfoInterface {

    void write(DataOutput dataOutput);

    void readFields(DataInput dataInput);

    boolean equals(Object o);

    int hashCode();
}
