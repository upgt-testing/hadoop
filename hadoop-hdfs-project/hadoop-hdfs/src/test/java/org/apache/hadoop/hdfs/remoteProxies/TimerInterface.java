package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface TimerInterface {

    void startTime();

    void stopTime();

    long getIntervalMillis();

    void printlnWithTimestamp(String message);

    String formatTime(long millis);

    String getIntervalString();

    String formatCurrentTime();
}
