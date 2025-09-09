package org.apache.hadoop.yarn.server.federation.store;

public interface FederationPolicyStoreJVMInterface {

    org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationResponseJVMInterface setPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.SetSubClusterPolicyConfigurationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationResponseJVMInterface getPolicyConfiguration_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPolicyConfigurationRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsResponseJVMInterface getPoliciesConfigurations_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterPoliciesConfigurationsRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;
}
