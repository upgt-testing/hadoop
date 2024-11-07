package org.apache.hadoop.hdfs.remoteProxies;

public interface MachineListInterface {
    boolean includes(java.lang.String arg0);
    boolean includes(java.net.InetAddress arg0);
    java.util.Collection<java.lang.String> getCollection();
}