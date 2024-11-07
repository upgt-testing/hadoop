package org.apache.hadoop.hdfs.remoteProxies;

public interface XAttrFeatureInterface {
    java.util.List<org.apache.hadoop.fs.XAttr> getXAttrs();
    boolean equals(java.lang.Object arg0);
    XAttrInterface getXAttr(java.lang.String arg0);
    int hashCode();
}