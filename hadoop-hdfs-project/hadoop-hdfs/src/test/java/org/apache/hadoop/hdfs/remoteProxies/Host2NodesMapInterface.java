package org.apache.hadoop.hdfs.remoteProxies;

public interface Host2NodesMapInterface {
    DatanodeDescriptorInterface getDatanodeByXferAddr(java.lang.String arg0, int arg1);
    boolean remove(DatanodeDescriptorInterface arg0);
    DatanodeDescriptorInterface getDatanodeByHost(java.lang.String arg0);
    boolean contains(DatanodeDescriptorInterface arg0);
    DatanodeDescriptorInterface getDataNodeByHostName(java.lang.String arg0);
    java.lang.String toString();
    boolean add(DatanodeDescriptorInterface arg0);
}