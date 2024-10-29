package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;

public interface ContentSummaryComputationContextInterface {

    long getYieldCount();

    boolean yield();

    ContentCountsInterface getCounts();

    ContentCountsInterface getSnapshotCounts();

    BlockStoragePolicySuite getBlockStoragePolicySuite();

    String getErasureCodingPolicyName(INodeInterface inode);
}
