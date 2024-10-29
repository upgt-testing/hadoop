package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface BlockReportContextInterface {

    int getTotalRpcs();

    int getCurRpc();

    long getReportId();

    long getLeaseId();
}
