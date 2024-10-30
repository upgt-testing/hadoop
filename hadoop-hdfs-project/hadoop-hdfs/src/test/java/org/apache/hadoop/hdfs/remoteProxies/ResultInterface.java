package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.shell.find.Result;

import java.util.*;
import java.io.*;

public interface ResultInterface {

    boolean isDescend();

    boolean isPass();

    ResultInterface combine(Result other);

    ResultInterface negate();

    String toString();

    int hashCode();

    boolean equals(Object obj);
}
