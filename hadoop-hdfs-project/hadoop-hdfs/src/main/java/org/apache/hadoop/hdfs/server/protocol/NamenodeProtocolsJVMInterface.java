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
    void saveNamespace() throws IOException;
    boolean restoreFailedStorage(String arg0) throws IOException;
    boolean setReplication(String src, short replication) throws IOException;
    DatanodeInfoJVMInterface[] getDatanodeReport(HdfsConstants.DatanodeReportType type) throws IOException;
    NamespaceInfoJVMInterface versionRequest() throws IOException;
    void renewLease(String clientName) throws IOException;
    void transitionToActive(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    void transitionToStandby(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    CheckpointSignatureJVMInterface rollEditLog() throws IOException;
    boolean setSafeMode(HdfsConstants.SafeModeAction action, boolean isChecked) throws IOException;
    LocatedBlocksJVMInterface getBlockLocations(String src, final long offset, final long length) throws IOException;
    long getTransactionID() throws IOException;
    ContentSummaryJVMInterface getContentSummary(String path) throws IOException;
    void deleteSnapshot(String snapshotRoot, String snapshotName) throws IOException;
    DirectoryListingJVMInterface getListing(String src, byte[] startAfter, boolean needLocation) throws IOException;
    boolean delete(String src, boolean recursive) throws IOException;
    boolean rename(String src, String dst) throws IOException;
}
