package org.apache.hadoop.yarn.server.federation.store;

public interface FederationApplicationHomeSubClusterStoreJVMInterface {

    org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterResponseJVMInterface getApplicationsHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterResponseJVMInterface updateApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.UpdateApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterResponseJVMInterface getApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.GetApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterResponseJVMInterface addApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterResponseJVMInterface deleteApplicationHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.DeleteApplicationHomeSubClusterRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;
}
