package org.apache.hadoop.hdfs.remoteProxies;

public interface QuotaDeltaInterface {
    void add(QuotaCountsInterface arg0);
    java.util.Map<org.apache.hadoop.hdfs.server.namenode.INode, org.apache.hadoop.hdfs.server.namenode.QuotaCounts> getUpdateMap();
    void addUpdatePath(INodeReferenceInterface arg0, QuotaCountsInterface arg1);
    void setCounts(QuotaCountsInterface arg0);
    QuotaCountsInterface getCountsCopy();
    java.util.Map<org.apache.hadoop.hdfs.server.namenode.INodeDirectory, org.apache.hadoop.hdfs.server.namenode.QuotaCounts> getQuotaDirMap();
    long getNsDelta();
    void addQuotaDirUpdate(INodeDirectoryInterface arg0, QuotaCountsInterface arg1);
}