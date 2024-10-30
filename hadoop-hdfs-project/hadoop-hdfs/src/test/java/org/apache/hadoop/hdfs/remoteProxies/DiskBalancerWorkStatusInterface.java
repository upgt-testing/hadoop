package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.datanode.DiskBalancerWorkStatus;

import java.util.*;
import java.io.*;

public interface DiskBalancerWorkStatusInterface {

    ResultInterface getResult();

    String getPlanID();

    String getPlanFile();

    List<DiskBalancerWorkStatus.DiskBalancerWorkEntry> getCurrentState();

    String currentStateString();

    String toJsonString();

    void addWorkEntry(DiskBalancerWorkStatus.DiskBalancerWorkEntry entry);
}
