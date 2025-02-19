package org.apache.hadoop.hdfs.server.namenode;

public interface INodeJVMInterface {
    INodeFileJVMInterface asFile();
    boolean isDirectory();
    boolean isQuotaSet();
    INodeDirectoryJVMInterface asDirectory();
}
