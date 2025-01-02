package org.apache.hadoop.hdfs.server.namenode;

public interface FSDirectoryJVMInterface {
    INodeJVMInterface getINode(String src, FSDirectory.DirOp dirOp) throws Exception;
}
