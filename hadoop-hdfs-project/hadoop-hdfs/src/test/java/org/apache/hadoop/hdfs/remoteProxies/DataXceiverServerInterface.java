package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.util.DataTransferThrottler;

public interface DataXceiverServerInterface {

    void run();

    void sendOOBToPeers();

    void stopWriters();

    DataTransferThrottlerInterface getTransferThrottler();

    DataTransferThrottler getWriteThrottler();

    boolean updateBalancerMaxConcurrentMovers(int movers);

    void setMaxXceiverCount(int xceiverCount);

    int getMaxXceiverCount();
}
