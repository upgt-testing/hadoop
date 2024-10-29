package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.namenode.FSEditLog;

public interface EditLogTailerInterface {

    void start();

    void stop();

    void setEditLog(FSEditLog editLog);

    void catchupDuringFailover();

    long doTailEdits();

    long getLastLoadTimeMs();
}
