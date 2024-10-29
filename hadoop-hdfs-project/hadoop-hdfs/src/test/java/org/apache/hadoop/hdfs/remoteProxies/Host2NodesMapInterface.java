package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface Host2NodesMapInterface {

    DatanodeDescriptorInterface getDatanodeByXferAddr(String ipAddr, int xferPort);

    String toString();
}
