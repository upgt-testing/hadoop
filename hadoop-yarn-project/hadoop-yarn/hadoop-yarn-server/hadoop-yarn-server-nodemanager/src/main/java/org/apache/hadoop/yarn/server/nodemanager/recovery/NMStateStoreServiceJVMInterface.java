package org.apache.hadoop.yarn.server.nodemanager.recovery;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface NMStateStoreServiceJVMInterface extends AbstractServiceJVMInterface {

    void finishResourceLocalization_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg1, java.lang.Object arg2) throws java.io.IOException;

    java.lang.Object loadApplicationsState() throws java.io.IOException;

    void storeNMTokenPreviousMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException;

    void serviceStop() throws java.io.IOException;

    void storeContainerTokenPreviousMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException;

    void storeNMTokenApplicationMasterKey_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException;

    void storeContainer_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, int arg1, org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequestJVMInterface arg2) throws java.io.IOException;

    void storeContainerLaunched_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException;

    java.lang.Object loadLocalizationState() throws java.io.IOException;

    void removeContainer_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException;

    void removeContainerToken_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException;

    java.lang.Object loadContainerTokensState() throws java.io.IOException;

    void serviceInit_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException;

    void storeContainerDiagnostics_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, java.lang.StringBuilder arg1) throws java.io.IOException;

    java.lang.Object loadLogDeleterState() throws java.io.IOException;

    java.util.List loadContainersState() throws java.io.IOException;

    void storeContainerKilled_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0) throws java.io.IOException;

    java.lang.Object loadDeletionServiceState() throws java.io.IOException;

    void storeContainerResourceChanged_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, int arg1, org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg2) throws java.io.IOException;

    java.lang.Object loadNMTokensState() throws java.io.IOException;

    void serviceStart() throws java.io.IOException;

    void removeNMTokenApplicationMasterKey_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0) throws java.io.IOException;

    void removeDeletionTask(int arg0) throws java.io.IOException;

    void removeApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) throws java.io.IOException;

    boolean canRecover();

    void storeDeletionTask_bridge(int arg0, java.lang.Object arg1) throws java.io.IOException;

    void storeNMTokenCurrentMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException;

    void storeApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException;

    void storeContainerToken_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, java.lang.Long arg1) throws java.io.IOException;

    void storeLogDeleter_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException;

    void storeContainerCompleted_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, int arg1) throws java.io.IOException;

    void removeLogDeleter_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0) throws java.io.IOException;

    void startResourceLocalization_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg1, java.lang.Object arg2, org.apache.hadoop.fs.PathJVMInterface arg3) throws java.io.IOException;

    void removeLocalizedResource_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg1, org.apache.hadoop.fs.PathJVMInterface arg2) throws java.io.IOException;

    void storeContainerTokenCurrentMasterKey_bridge(java.lang.Object arg0) throws java.io.IOException;

    boolean isNewlyCreated();
}
