package org.apache.hadoop.hdfs.server.namenode;

import java.io.PrintWriter;

public interface INodeJVMInterface {
    long getId();
    String getFullPathName();
    AclFeatureJVMInterface getAclFeature();
    boolean isReference();
    INodeFileJVMInterface asFile();
    boolean isDirectory();
    boolean isQuotaSet();
    INodeDirectoryJVMInterface asDirectory();
    XAttrFeatureJVMInterface getXAttrFeature();
    void dumpTreeRecursively(PrintWriter out, StringBuilder prefix,
                                  int snapshotId);
    StringBuffer dumpTreeRecursively();
}
