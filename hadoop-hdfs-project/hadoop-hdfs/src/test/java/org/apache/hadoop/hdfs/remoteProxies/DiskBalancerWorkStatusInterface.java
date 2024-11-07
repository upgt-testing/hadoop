package org.apache.hadoop.hdfs.remoteProxies;

public interface DiskBalancerWorkStatusInterface {
    java.lang.String getPlanID();
    java.lang.String currentStateString() throws java.io.IOException;
    java.lang.String toJsonString() throws java.io.IOException;
    org.apache.hadoop.hdfs.server.datanode.DiskBalancerWorkStatus.Result getResult();
    java.lang.String getPlanFile();
    void addWorkEntry(DiskBalancerWorkEntryInterface arg0);
    DiskBalancerWorkStatusInterface parseJson(java.lang.String arg0) throws java.io.IOException;
    java.util.List<org.apache.hadoop.hdfs.server.datanode.DiskBalancerWorkStatus.DiskBalancerWorkEntry> getCurrentState();
}