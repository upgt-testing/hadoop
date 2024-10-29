package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface AbstractResourceUsageInterface {

    String toString();

    Set<String> getNodePartitionsSet();
}
