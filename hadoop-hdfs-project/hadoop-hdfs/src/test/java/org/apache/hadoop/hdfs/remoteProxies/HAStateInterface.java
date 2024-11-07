package org.apache.hadoop.hdfs.remoteProxies;

public interface HAStateInterface {
    void prepareToExitState(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0) throws org.apache.hadoop.ha.ServiceFailedException;
    void setStateInternal(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0, HAStateInterface arg1) throws org.apache.hadoop.ha.ServiceFailedException;
    void setState(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0, HAStateInterface arg1) throws org.apache.hadoop.ha.ServiceFailedException;
    void checkOperation(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0, org.apache.hadoop.hdfs.server.namenode.NameNode.OperationCategory arg1) throws org.apache.hadoop.ipc.StandbyException;
    org.apache.hadoop.ha.HAServiceProtocol.HAServiceState getServiceState();
    void updateLastHATransitionTime();
    boolean shouldPopulateReplQueues();
    void exitState(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0) throws org.apache.hadoop.ha.ServiceFailedException;
    java.lang.String toString();
    void prepareToEnterState(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0) throws org.apache.hadoop.ha.ServiceFailedException;
    long getLastHATransitionTime();
    void enterState(org.apache.hadoop.hdfs.server.namenode.ha.HAContext arg0) throws org.apache.hadoop.ha.ServiceFailedException;
}