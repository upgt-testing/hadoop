package org.apache.hadoop.hdfs.server.namenode;

public interface INodeDirectoryJVMInterface {
    DirectoryWithQuotaFeatureJVMInterface getDirectoryWithQuotaFeature();
    boolean isQuotaSet();
    String getFullPathName();
    boolean isWithSnapshot();
    boolean isSnapshottable();
}
