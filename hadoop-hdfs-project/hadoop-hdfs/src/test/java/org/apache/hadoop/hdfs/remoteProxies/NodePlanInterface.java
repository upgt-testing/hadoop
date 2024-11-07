package org.apache.hadoop.hdfs.remoteProxies;

public interface NodePlanInterface {
    java.util.List<org.apache.hadoop.hdfs.server.diskbalancer.planner.Step> getVolumeSetPlans();
    NodePlanInterface parseJson(java.lang.String arg0) throws java.io.IOException;
    java.lang.String getNodeName();
    void setNodeUUID(java.lang.String arg0);
    long getTimeStamp();
    void setTimeStamp(long arg0);
    void setVolumeSetPlans(java.util.List<org.apache.hadoop.hdfs.server.diskbalancer.planner.Step> arg0);
    java.lang.String getNodeUUID();
    int getPort();
    void setURI(java.lang.String arg0);
    java.lang.String toJson() throws java.io.IOException;
    void setPort(int arg0);
    void addStep(org.apache.hadoop.hdfs.server.diskbalancer.planner.Step arg0);
    void setNodeName(java.lang.String arg0);
}