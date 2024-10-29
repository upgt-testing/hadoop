package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.namenode.QuotaCounts;

import java.util.*;
import java.io.*;

public interface DirectoryWithQuotaFeatureInterface {

    void addSpaceConsumed2Cache(QuotaCounts delta);

    QuotaCountsInterface getSpaceAllowed();

    QuotaCountsInterface getSpaceConsumed();

    String toString();
}
