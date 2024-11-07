package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockReportContextInterface {
    long getReportId();
    long getLeaseId();
    int getCurRpc();
    int getTotalRpcs();
}