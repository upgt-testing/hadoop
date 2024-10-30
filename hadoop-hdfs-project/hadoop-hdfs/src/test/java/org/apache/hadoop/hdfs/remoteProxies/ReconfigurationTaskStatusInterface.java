package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.conf.ReconfigurationUtil;

import java.util.*;
import java.io.*;

public interface ReconfigurationTaskStatusInterface {

    boolean hasTask();

    boolean stopped();

    long getStartTime();

    long getEndTime();

    Map<ReconfigurationUtil.PropertyChange, Optional<String>> getStatus();
}
