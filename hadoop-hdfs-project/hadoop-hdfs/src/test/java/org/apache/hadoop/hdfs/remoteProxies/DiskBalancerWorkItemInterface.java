package org.apache.hadoop.hdfs.remoteProxies;

public interface DiskBalancerWorkItemInterface {
    java.lang.String getErrMsg();
    void setStartTime(long arg0);
    void setBytesCopied(long arg0);
    long getTolerancePercent();
    void setBandwidth(long arg0);
    long getBytesToCopy();
    void setTolerancePercent(long arg0);
    void incBlocksCopied();
    void setBlocksCopied(long arg0);
    long getMaxDiskErrors();
    long getErrorCount();
    void setErrorCount(long arg0);
    void setSecondsElapsed(long arg0);
    long getBytesCopied();
    long getBlocksCopied();
    long getBandwidth();
    void setMaxDiskErrors(long arg0);
    void incCopiedSoFar(long arg0);
    void incErrorCount();
    void setErrMsg(java.lang.String arg0);
    DiskBalancerWorkItemInterface parseJson(java.lang.String arg0) throws java.io.IOException;
    long getStartTime();
    java.lang.String toJson() throws java.io.IOException;
    long getSecondsElapsed();
}