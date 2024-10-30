package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DataTransferThrottlerInterface {

    long getBandwidth();

    void setBandwidth(long bytesPerSecond);

    void throttle(long numOfBytes);

    void throttle(long numOfBytes, CancelerInterface canceler);
}
