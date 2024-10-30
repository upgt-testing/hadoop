package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.net.unix.DomainSocket;

import java.util.*;
import java.io.*;

public interface DomainSocketInterface {

    DomainSocket accept();

    boolean isOpen();

    String getPath();

    DomainSocket.DomainInputStream getInputStream();

    DomainSocket.DomainOutputStream getOutputStream();

    DomainSocket.DomainChannel getChannel();

    void setAttribute(int type, int size);

    int getAttribute(int type);

    void close();

    void shutdown();

    void sendFileDescriptors(FileDescriptor[] descriptors, byte[] jbuf, int offset, int length);

    int recvFileInputStreams(FileInputStream[] streams, byte[] buf, int offset, int length);

    String toString();
}
