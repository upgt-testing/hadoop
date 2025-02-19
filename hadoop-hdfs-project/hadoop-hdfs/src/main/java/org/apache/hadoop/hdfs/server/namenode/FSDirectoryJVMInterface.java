package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.fs.ParentNotDirectoryException;
import org.apache.hadoop.fs.UnresolvedLinkException;
import org.apache.hadoop.security.AccessControlException;

public interface FSDirectoryJVMInterface {
    INodeJVMInterface getINode(String src) throws Exception;
    INodeJVMInterface getINode(String src, FSDirectory.DirOp dirOp) throws UnresolvedLinkException, AccessControlException, ParentNotDirectoryException;
    INodeJVMInterface getINode4Write(String src) throws Exception;
    INodeDirectoryJVMInterface getRoot();
    long totalInodes();
}
