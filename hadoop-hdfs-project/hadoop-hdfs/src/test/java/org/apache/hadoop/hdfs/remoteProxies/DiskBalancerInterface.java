package org.apache.hadoop.hdfs.remoteProxies;

public interface DiskBalancerInterface {
    void shutdown();
    void cancelPlan(java.lang.String arg0) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    java.lang.String getVolumeNames() throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void checkDiskBalancerEnabled() throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void executePlan();
    void shutdownExecutor();
    long getBandwidth() throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void createWorkPlan(VolumePairInterface arg0, org.apache.hadoop.hdfs.server.diskbalancer.planner.Step arg1) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void verifyNodeUUID(NodePlanInterface arg0) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void createWorkPlan(NodePlanInterface arg0) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi getFsVolume(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpi<?> arg0, java.lang.String arg1);
    NodePlanInterface verifyPlanHash(java.lang.String arg0, java.lang.String arg1) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    java.util.Map<java.lang.String, java.lang.String> getStorageIDToVolumeBasePathMap() throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    NodePlanInterface verifyPlan(java.lang.String arg0, long arg1, java.lang.String arg2, boolean arg3) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void verifyTimeStamp(NodePlanInterface arg0) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void submitPlan(java.lang.String arg0, long arg1, java.lang.String arg2, java.lang.String arg3, boolean arg4) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    void verifyPlanVersion(long arg0) throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
    DiskBalancerWorkStatusInterface queryWorkStatus() throws org.apache.hadoop.hdfs.server.diskbalancer.DiskBalancerException;
}