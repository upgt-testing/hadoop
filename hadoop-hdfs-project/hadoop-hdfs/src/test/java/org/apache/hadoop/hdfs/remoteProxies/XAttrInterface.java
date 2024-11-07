package org.apache.hadoop.hdfs.remoteProxies;

public interface XAttrInterface {
    byte[] getValue();
    org.apache.hadoop.fs.XAttr.NameSpace getNameSpace();
    int hashCode();
    java.lang.String getName();
    boolean equals(java.lang.Object arg0);
    boolean equalsIgnoreValue(java.lang.Object arg0);
    java.lang.String toString();
}