package org.apache.hadoop.hdfs.remoteProxies;

public interface EditLogInputStreamInterface {
    java.lang.String getName();
    void setMaxOpSize(int arg0);
    long scanNextOp() throws java.io.IOException;
    long getFirstTxId();
    void resync();
    boolean isInProgress();
    FSEditLogOpInterface nextValidOp();
    boolean skipUntil(long arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
    boolean isLocalLog();
    FSEditLogOpInterface getCachedOp();
    long getLastTxId();
    long getPosition();
    java.lang.String getCurrentStreamName();
    FSEditLogOpInterface readOp() throws java.io.IOException;
    int getVersion(boolean arg0) throws java.io.IOException;
    FSEditLogOpInterface nextOp() throws java.io.IOException;
    long length() throws java.io.IOException;
}