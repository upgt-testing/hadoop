package org.apache.hadoop.hdfs;

import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Shuai Wang
 */
public class DockerTest {
    @Test
    public void testHadoopNamenode() {
        Network network = Network.builder().build();
        List<String> portBindings = new ArrayList<>();
        portBindings.add("50070:50070");
        try (GenericContainer<?> namenode = new GenericContainer<>("hadoop:3.3.6")
                .withNetwork(network)
                .withNetworkAliases("namenode")
                .withEnv("CLUSTER_NAME", "hdfs-cluster")
                .withEnv("NAMENODE_ID", "0")
                .withCommand("bash", "-c", "hdfs namenode -format && hdfs namenode")
                .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                .withAccessToHost(true)
                ) {
            namenode.setPortBindings(portBindings);
            namenode.start();

            // Get the mapped port for Namenode web interface
            Integer mappedPort = namenode.getMappedPort(50070);

            // Access the Namenode web interface
            String address = "http://" + namenode.getHost() + ":" + mappedPort;
            System.out.println("Namenode web interface is accessible at: " + address);

            // Add your test logic here to interact with the Namenode web interface
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for the cluster to start", e);
            }

            
            namenode.stop();
        }
    }
}
