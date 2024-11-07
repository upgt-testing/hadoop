package org.apache.hadoop.hdfs.remoteProxies;

public interface MetaRecoveryContextInterface {
    void quit();
    void editLogLoaderPrompt(java.lang.String arg0, MetaRecoveryContextInterface arg1, java.lang.String arg2) throws java.io.IOException, org.apache.hadoop.hdfs.server.namenode.MetaRecoveryContext.RequestStopException;
    void setForce(int arg0);
    java.lang.String ask(java.lang.String arg0, java.lang.String arg1, java.lang.String... arg2) throws java.io.IOException;
    int getForce();
}