package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface AclFeatureInterface {

    boolean equals(Object o);

    String toString();

    int hashCode();

    int getRefCount();

    int incrementAndGetRefCount();

    int decrementAndGetRefCount();
}
