package org.apache.hadoop.hdfs.server.protocol;

import org.apache.hadoop.fs.ContentSummaryJVMInterface;
import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.ha.ServiceFailedException;
import org.apache.hadoop.hdfs.protocol.*;
import org.apache.hadoop.hdfs.server.namenode.CheckpointSignature;
import org.apache.hadoop.hdfs.server.namenode.CheckpointSignatureJVMInterface;
import org.apache.hadoop.security.AccessControlException;

import java.io.IOException;

public interface NamenodeProtocolsJVMInterface {
    void transitionToObserver(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    void transitionToActive(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    void transitionToStandby(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    CheckpointSignatureJVMInterface rollEditLog() throws IOException;
    boolean setSafeMode(HdfsConstants.SafeModeAction action, boolean isChecked) throws IOException;
    boolean saveNamespace(long timeWindow, long txGap) throws IOException;
    LocatedBlocksJVMInterface getBlockLocations(String src, final long offset, final long length) throws IOException;
    long getTransactionID() throws IOException;
    ContentSummaryJVMInterface getContentSummary(String path) throws IOException;
    void deleteSnapshot(String snapshotRoot, String snapshotName) throws IOException;
    DirectoryListingJVMInterface getListing(String src, byte[] startAfter, boolean needLocation) throws IOException;
    boolean delete(String src, boolean recursive) throws IOException;
    boolean rename(String src, String dst) throws IOException;
}
