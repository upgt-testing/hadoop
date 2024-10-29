package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.Path;

public interface CacheDirectiveInfoInterface {

    Long getId();

    Path getPath();

    Short getReplication();

    String getPool();

    //Expiration getExpiration();

    boolean equals(Object o);

    int hashCode();

    String toString();
}
