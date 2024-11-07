package org.apache.hadoop.hdfs.remoteProxies;

public interface ContentSummaryComputationContextInterface {
    java.lang.String getErasureCodingPolicyName(INodeInterface arg0);
    ContentCountsInterface getCounts();
    long getYieldCount();
    void checkPermission(INodeDirectoryInterface arg0, int arg1, org.apache.hadoop.fs.permission.FsAction arg2) throws org.apache.hadoop.security.AccessControlException;
    boolean yield();
    ContentCountsInterface getSnapshotCounts();
    BlockStoragePolicySuiteInterface getBlockStoragePolicySuite();
}