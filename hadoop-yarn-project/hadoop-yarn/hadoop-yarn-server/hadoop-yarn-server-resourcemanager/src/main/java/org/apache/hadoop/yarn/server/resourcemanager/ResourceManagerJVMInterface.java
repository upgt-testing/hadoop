package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.service.CompositeServiceJVMInterface;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.RecoverableJVMInterface;

public interface ResourceManagerJVMInterface extends CompositeServiceJVMInterface, RecoverableJVMInterface {

    void handleTransitionToStandBy();

    org.apache.hadoop.yarn.server.resourcemanager.RMContextJVMInterface getRMContext();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    org.apache.hadoop.yarn.server.resourcemanager.ApplicationMasterServiceJVMInterface getApplicationMasterService();

    org.apache.hadoop.yarn.server.resourcemanager.ResourceTrackerServiceJVMInterface getResourceTrackerService();

    java.lang.String getZkRootNodePassword();

    org.apache.hadoop.yarn.server.resourcemanager.ClientRMServiceJVMInterface getClientRMService();

    org.apache.hadoop.yarn.server.resourcemanager.security.QueueACLsManagerJVMInterface getQueueACLsManager();

    java.lang.Object getResourceScheduler();

    java.lang.Object createAndStartCurator_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.lang.Exception;

    void recover_bridge(java.lang.Object arg0) throws java.lang.Exception;

    java.lang.Object getCurator();
}
