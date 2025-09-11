package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.service.CompositeServiceJVMInterface;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.RecoverableJVMInterface;

public interface ResourceManagerJVMInterface extends CompositeServiceJVMInterface, RecoverableJVMInterface {

    org.apache.hadoop.yarn.server.resourcemanager.ResourceTrackerServiceJVMInterface getResourceTrackerService();

    java.lang.Object getResourceScheduler();

    void recover_bridge(java.lang.Object arg0) throws java.lang.Exception;

    org.apache.hadoop.yarn.server.resourcemanager.RMContextJVMInterface getRMContext();

    org.apache.hadoop.yarn.server.resourcemanager.federation.FederationStateStoreServiceJVMInterface getFederationStateStoreService();

    org.apache.hadoop.util.curator.ZKCuratorManagerJVMInterface createAndStartZKManager_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    org.apache.hadoop.yarn.server.resourcemanager.ApplicationMasterServiceJVMInterface getApplicationMasterService();

    java.lang.String getZkRootNodePassword();

    org.apache.hadoop.yarn.server.resourcemanager.ClientRMServiceJVMInterface getClientRMService();

    org.apache.hadoop.yarn.server.resourcemanager.security.QueueACLsManagerJVMInterface getQueueACLsManager();

    org.apache.hadoop.util.curator.ZKCuratorManagerJVMInterface getZKManager();

    java.lang.Object getCurator();
}
