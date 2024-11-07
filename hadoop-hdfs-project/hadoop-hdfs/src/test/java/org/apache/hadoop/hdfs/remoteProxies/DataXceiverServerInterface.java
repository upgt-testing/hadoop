package org.apache.hadoop.hdfs.remoteProxies;

public interface DataXceiverServerInterface {
    void releasePeer(org.apache.hadoop.hdfs.net.Peer arg0);
    org.apache.hadoop.hdfs.net.PeerServer getPeerServer();
    void restartNotifyPeers();
    void sendOOBToPeers();
    void closePeer(org.apache.hadoop.hdfs.net.Peer arg0);
    int getMaxXceiverCount();
    DataTransferThrottlerInterface getWriteThrottler();
    void kill();
    int getNumPeersXceiver();
    void setMaxXceiverCount(int arg0);
    void stopWriters();
    boolean updateBalancerMaxConcurrentMovers(int arg0);
    int getNumPeers();
    void addPeer(org.apache.hadoop.hdfs.net.Peer arg0, java.lang.Thread arg1, DataXceiverInterface arg2) throws java.io.IOException;
    void closeAllPeers();
    boolean waitAllPeers(long arg0, java.util.concurrent.TimeUnit arg1);
    void setMaxReconfigureWaitTime(int arg0);
    DataTransferThrottlerInterface getTransferThrottler();
    void run();
}