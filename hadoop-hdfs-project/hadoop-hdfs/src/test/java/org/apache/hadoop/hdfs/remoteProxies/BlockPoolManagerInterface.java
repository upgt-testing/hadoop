package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockPoolManagerInterface {
//    void shutDownAll(java.util.List<org.apache.hadoop.hdfs.server.datanode.BPOfferService> arg0) throws java.lang.InterruptedException;
    void addBlockPool(BPOfferServiceInterface arg0);
//    java.util.List<org.apache.hadoop.hdfs.server.datanode.BPOfferService> getAllNamenodeThreads();
    BPOfferServiceInterface createBPOS(java.lang.String arg0, java.util.List<java.lang.String> arg1, java.util.List<java.net.InetSocketAddress> arg2, java.util.List<java.net.InetSocketAddress> arg3);
    void joinAll();
    void refreshNamenodes(ConfigurationInterface arg0) throws java.io.IOException;
    void doRefreshNamenodes(java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.net.InetSocketAddress>> arg0, java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.net.InetSocketAddress>> arg1) throws java.io.IOException;
    BPOfferServiceInterface get(java.lang.String arg0);
    void remove(BPOfferServiceInterface arg0);
    void startAll() throws java.io.IOException;
}