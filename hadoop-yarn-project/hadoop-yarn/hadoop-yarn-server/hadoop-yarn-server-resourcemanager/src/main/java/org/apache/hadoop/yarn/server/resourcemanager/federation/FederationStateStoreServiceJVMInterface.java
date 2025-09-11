package org.apache.hadoop.yarn.server.resourcemanager.federation;

import org.apache.hadoop.service.AbstractServiceJVMInterface;
import org.apache.hadoop.yarn.server.federation.store.FederationStateStoreJVMInterface;

public interface FederationStateStoreServiceJVMInterface extends AbstractServiceJVMInterface, FederationStateStoreJVMInterface {

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponseJVMInterface getSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.records.VersionJVMInterface loadVersion();

    java.lang.Object getStateStoreClient();

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponseJVMInterface getSubClusters_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponseJVMInterface getPoliciesConfigurations_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponseJVMInterface registerSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponseJVMInterface getApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponseJVMInterface deleteApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.records.VersionJVMInterface getCurrentVersion();

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponseJVMInterface deregisterSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponseJVMInterface getApplicationsHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponseJVMInterface setPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponseJVMInterface getPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.resourcemanager.federation.FederationStateStoreHeartbeatJVMInterface getStateStoreHeartbeatThread();

    org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponseJVMInterface updateApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponseJVMInterface addApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponseJVMInterface subClusterHeartbeat_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;
}
