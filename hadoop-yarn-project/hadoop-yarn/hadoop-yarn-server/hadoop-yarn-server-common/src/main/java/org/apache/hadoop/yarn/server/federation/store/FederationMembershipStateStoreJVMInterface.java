package org.apache.hadoop.yarn.server.federation.store;

public interface FederationMembershipStateStoreJVMInterface {

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoResponseJVMInterface getSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClusterInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterResponseJVMInterface deregisterSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterDeregisterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoResponseJVMInterface getSubClusters_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetSubClustersInfoRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterResponseJVMInterface registerSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterRegisterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatResponseJVMInterface subClusterHeartbeat_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterHeartbeatRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;
}
