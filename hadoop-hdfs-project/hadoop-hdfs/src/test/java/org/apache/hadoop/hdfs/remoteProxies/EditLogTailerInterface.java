package org.apache.hadoop.hdfs.remoteProxies;

public interface EditLogTailerInterface {
    void sleep(long arg0) throws java.lang.InterruptedException;
    boolean tooLongSinceLastLoad();
    long doTailEdits() throws java.io.IOException, java.lang.InterruptedException;
    FSEditLogInterface getEditLog();
    void triggerActiveLogRoll();
    long getLastLoadTimeMs();
    java.util.concurrent.Callable<java.lang.Void> getNameNodeProxy();
    void start();
    void stop() throws java.io.IOException;
    void catchupDuringFailover() throws java.io.IOException;
    void setEditLog(FSEditLogInterface arg0);
}