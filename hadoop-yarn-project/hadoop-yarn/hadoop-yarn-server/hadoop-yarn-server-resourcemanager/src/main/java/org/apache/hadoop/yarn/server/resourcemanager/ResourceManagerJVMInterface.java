package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.service.CompositeServiceJVMInterface;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatRequest;
import org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatResponse;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.RecoverableJVMInterface;

import java.io.IOException;

public interface ResourceManagerJVMInterface extends CompositeServiceJVMInterface, RecoverableJVMInterface, ResourceManagerMXBeanJVMInterface {

    org.apache.hadoop.yarn.server.resourcemanager.ResourceTrackerServiceJVMInterface getResourceTrackerService();

    boolean isSecurityEnabled();

    java.lang.Object getResourceScheduler();

    java.lang.String getRMLoginUser();

    void recover_bridge(java.lang.Object arg0) throws java.lang.Exception;

    org.apache.hadoop.yarn.server.resourcemanager.RMContextJVMInterface getRMContext();

    org.apache.hadoop.yarn.server.resourcemanager.federation.FederationStateStoreServiceJVMInterface getFederationStateStoreService();

    org.apache.hadoop.util.curator.ZKCuratorManagerJVMInterface createAndStartZKManager_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.yarn.server.resourcemanager.ApplicationMasterServiceJVMInterface getApplicationMasterService();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    java.lang.String getZkRootNodePassword();

    org.apache.hadoop.yarn.server.resourcemanager.ClientRMServiceJVMInterface getClientRMService();

    org.apache.hadoop.yarn.server.resourcemanager.security.QueueACLsManagerJVMInterface getQueueACLsManager();

    org.apache.hadoop.util.curator.ZKCuratorManagerJVMInterface getZKManager();

    java.lang.Object getCurator();
}
