package org.apache.hadoop.yarn.server.resourcemanager;

public interface EmbeddedElectorJVMInterface {
    /**
     * Leave and rejoin leader election.
     */
    void rejoinElection();

    /**
     * Get information about the elector's connection to Zookeeper.
     *
     * @return zookeeper connection state
     */
    String getZookeeperConnectionState();
}
