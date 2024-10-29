package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DatanodeStorageReportInterface {

    DatanodeInfoInterface getDatanodeInfo();

    StorageReportInterface[] getStorageReports();
}
