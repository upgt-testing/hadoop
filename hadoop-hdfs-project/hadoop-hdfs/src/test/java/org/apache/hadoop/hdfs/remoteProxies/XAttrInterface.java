package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.XAttr;

import java.util.*;
import java.io.*;

public interface XAttrInterface {

    XAttr.NameSpace getNameSpace();

    String getName();

    byte[] getValue();

    int hashCode();

    boolean equals(Object obj);

    boolean equalsIgnoreValue(Object obj);

    String toString();
}
