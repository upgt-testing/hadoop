package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ExtendedBlockIdInterface {

    long getBlockId();

    String getBlockPoolId();

    boolean equals(Object o);

    int hashCode();

    String toString();
}
