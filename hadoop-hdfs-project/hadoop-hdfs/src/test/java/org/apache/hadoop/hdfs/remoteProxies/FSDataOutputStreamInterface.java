package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.statistics.IOStatistics;

public interface FSDataOutputStreamInterface {

    long getPos();

    void close();

    String toString();

    OutputStream getWrappedStream();

    boolean hasCapability(String capability);

    void hflush();

    void hsync();

    void setDropBehind(Boolean dropBehind);

    IOStatistics getIOStatistics();

    //AbortableResult abort();
}
