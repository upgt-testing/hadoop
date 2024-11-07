package org.apache.hadoop.hdfs.remoteProxies;

public interface AutoRenewalForUserCredsRunnableInterface {
    void run();
    void relogin() throws java.io.IOException;
    void setRunRenewalLoop(boolean arg0);
}