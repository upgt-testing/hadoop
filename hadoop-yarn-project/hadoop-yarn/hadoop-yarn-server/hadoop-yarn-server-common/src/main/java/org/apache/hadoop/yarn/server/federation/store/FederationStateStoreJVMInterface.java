package org.apache.hadoop.yarn.server.federation.store;

public interface FederationStateStoreJVMInterface extends FederationApplicationHomeSubClusterStoreJVMInterface, FederationMembershipStateStoreJVMInterface, FederationPolicyStoreJVMInterface {

    org.apache.hadoop.yarn.server.records.VersionJVMInterface loadVersion();

    void init_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    org.apache.hadoop.yarn.server.records.VersionJVMInterface getCurrentVersion();

    void close() throws java.lang.Exception;
}
