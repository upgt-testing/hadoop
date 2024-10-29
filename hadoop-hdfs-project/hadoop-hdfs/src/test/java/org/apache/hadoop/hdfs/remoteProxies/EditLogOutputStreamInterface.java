package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface EditLogOutputStreamInterface {

    long getLastJournalledTxId();

    void write(FSEditLogOpInterface op);

    void writeRaw(byte[] bytes, int offset, int length);

    void create(int layoutVersion);

    void close();

    void abort();

    void setReadyToFlush();

    void flush();

    void flush(boolean durable);

    boolean shouldForceSync();

    String generateReport();

    int getCurrentLogVersion();

    void setCurrentLogVersion(int logVersion);
}
