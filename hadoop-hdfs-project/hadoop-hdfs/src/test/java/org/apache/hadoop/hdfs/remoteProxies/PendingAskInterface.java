package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.service.api.records.Resource;

public interface PendingAskInterface {

    //Resource getPerAllocationResource();

    int getCount();

    String toString();
}
