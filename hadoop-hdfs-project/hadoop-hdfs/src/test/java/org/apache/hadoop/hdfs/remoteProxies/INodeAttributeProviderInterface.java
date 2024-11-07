package org.apache.hadoop.hdfs.remoteProxies;

public interface INodeAttributeProviderInterface {
    org.apache.hadoop.hdfs.server.namenode.INodeAttributes getAttributes(byte[][] arg0, org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg1);
    org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider.AccessControlEnforcer getExternalAccessControlEnforcer(org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider.AccessControlEnforcer arg0);
    org.apache.hadoop.hdfs.server.namenode.INodeAttributes getAttributes(java.lang.String[] arg0, org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg1);
    org.apache.hadoop.hdfs.server.namenode.INodeAttributes getAttributes(java.lang.String arg0, org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg1);
    java.lang.String[] getPathElements(java.lang.String arg0);
    void stop();
    void start();
}