package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.server.resourcemanager.scheduler.common.PendingAsk;

public interface PendingAskUpdateResultInterface {

    PendingAskInterface getLastPendingAsk();

    PendingAskInterface getNewPendingAsk();

    String getLastNodePartition();

    String getNewNodePartition();

    String toString();
}
