package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DatanodeLocalInfoInterface {

    String getSoftwareVersion();

    String getConfigVersion();

    long getUptime();

    String getDatanodeLocalReport();
}
