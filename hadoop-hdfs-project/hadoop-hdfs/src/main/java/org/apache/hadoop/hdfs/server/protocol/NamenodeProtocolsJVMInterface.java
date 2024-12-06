package org.apache.hadoop.hdfs.server.protocol;

import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.ha.ServiceFailedException;
import org.apache.hadoop.hdfs.server.namenode.CheckpointSignature;
import org.apache.hadoop.hdfs.server.namenode.CheckpointSignatureJVMInterface;
import org.apache.hadoop.security.AccessControlException;

import java.io.IOException;

public interface NamenodeProtocolsJVMInterface {
    void transitionToObserver(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    void transitionToActive(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    void transitionToStandby(HAServiceProtocol.StateChangeRequestInfo req) throws ServiceFailedException, AccessControlException, IOException;
    CheckpointSignatureJVMInterface rollEditLog() throws IOException;
}
