package org.apache.hadoop.hdfs.remoteProxies;

public interface LeavingServiceStatusInterface {
    void set(int arg0, LightWeightHashSetInterface<java.lang.Long> arg1, int arg2, int arg3);
    LightWeightHashSetInterface<java.lang.Long> getOpenFiles();
    int getUnderReplicatedInOpenFiles();
    int getOutOfServiceOnlyReplicas();
    void setStartTime(long arg0);
    int getUnderReplicatedBlocks();
    long getStartTime();
}