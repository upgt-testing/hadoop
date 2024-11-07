package org.apache.hadoop.hdfs.remoteProxies;

public interface EditLogOutputStreamInterface {
    void setCurrentLogVersion(int arg0);
    void abort() throws java.io.IOException;
    void close() throws java.io.IOException;
    boolean shouldForceSync();
    void flush() throws java.io.IOException;
    java.lang.String generateReport();
    void writeRaw(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void flushAndSync(boolean arg0) throws java.io.IOException;
    void setReadyToFlush() throws java.io.IOException;
    void flush(boolean arg0) throws java.io.IOException;
    long getTotalSyncTime();
    void create(int arg0) throws java.io.IOException;
    long getNumSync();
    long getLastJournalledTxId();
    void write(FSEditLogOpInterface arg0) throws java.io.IOException;
    int getCurrentLogVersion();
}