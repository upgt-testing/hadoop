package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.fs.ParentNotDirectoryException;
import org.apache.hadoop.fs.UnresolvedLinkException;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerJVMInterface;
import org.apache.hadoop.security.AccessControlException;

import java.io.IOException;

public interface FSDirectoryJVMInterface {
    int getInodeXAttrsLimit();
    void enableQuotaChecks();
    void disableQuotaChecks();
    void writeUnlock();
    void updateCountForQuota();
    void writeLock();
    BlockManagerJVMInterface getBlockManager();
    int getInodeMapSize();
    long getLastInodeId();
    long getYieldCount();
    INodeJVMInterface getINode(String src) throws IOException;
    INodeJVMInterface getINode(String src, FSDirectory.DirOp dirOp) throws IOException;
    //INodeJVMInterface getINode(String src, FSDirectory.DirOp dirOp) throws UnresolvedLinkException, AccessControlException, ParentNotDirectoryException;
    INodeJVMInterface getINode4Write(String src) throws Exception;
    INodeDirectoryJVMInterface getRoot();
    long totalInodes();
}
