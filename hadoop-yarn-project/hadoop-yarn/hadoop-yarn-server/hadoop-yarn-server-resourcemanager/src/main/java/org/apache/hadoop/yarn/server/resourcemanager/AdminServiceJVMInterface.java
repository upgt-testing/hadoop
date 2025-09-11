package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.ha.HAServiceStatus;

import java.io.IOException;

public interface AdminServiceJVMInterface {
    HAServiceStatus getServiceStatus() throws IOException;
    void transitionToActive(HAServiceProtocol.StateChangeRequestInfo reqInfo) throws IOException;
}
