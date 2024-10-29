package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.Path;

public interface ProvidedStorageLocationInterface {

    Path getPath();

    long getOffset();

    long getLength();

    byte[] getNonce();

    boolean equals(Object o);

    int hashCode();
}
