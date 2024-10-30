package org.apache.hadoop.hdfs.remoteProxies;

import java.net.InetSocketAddress;
import java.util.*;
import java.io.*;

public interface BlockReportOptionsInterface {

    boolean isIncremental();

    InetSocketAddress getNamenodeAddr();

    String toString();
}
