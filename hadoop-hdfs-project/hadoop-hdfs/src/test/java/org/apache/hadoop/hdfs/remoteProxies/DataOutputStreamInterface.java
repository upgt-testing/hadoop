package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DataOutputStreamInterface {

    boolean shortOfSpace(int length);

    boolean hasUnFlushedData();
}
