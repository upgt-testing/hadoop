package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface MetricsTagInterface {

    String name();

    String description();

    MetricsInfoInterface info();

    String value();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
