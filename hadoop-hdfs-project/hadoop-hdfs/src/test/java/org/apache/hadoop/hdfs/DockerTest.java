package org.apache.hadoop.hdfs;

import org.apache.hadoop.DockerHDFSClusterTest;
import org.apache.hadoop.conf.Configuration;
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
    public void testStartNameNodeFromCluster() {
         try {
             DockerHDFSCluster dockerHDFSCluster = new DockerHDFSCluster(new Configuration());
             dockerHDFSCluster.startNameNodes();
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    @Test
    public void testStartNameNode() {
        Network network = Network.builder().build();
        List<String> nameNodePortBindings = new ArrayList<>();
        nameNodePortBindings.add("50070:50070");
        nameNodePortBindings.add("9000:9000");
        nameNodePortBindings.add("9870:9870");
        try (GenericContainer<?> namenode = new GenericContainer<>("hadoop:3.3.6")
                .withNetwork(network)
                .withNetworkAliases("namenode")
                .withEnv("CLUSTER_NAME", "hdfs-cluster")
                .withEnv("NAMENODE_ID", "0")
                .withCommand("bash", "-c", "hdfs namenode -format && hdfs namenode")
                .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                .withExposedPorts(9000, 50070)
                .withAccessToHost(true)
                ) {
            namenode.setPortBindings(nameNodePortBindings);
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


            List<String> dataNodePortBindings = new ArrayList<>();
            dataNodePortBindings.add("50010:50010");
            dataNodePortBindings.add("50075:50075");
            dataNodePortBindings.add("50020:50020");
            // now let's start a datanode
            try (GenericContainer<?> datanode = new GenericContainer<>("hadoop:3.3.6")
                    .withNetwork(network)
                    .withNetworkAliases("datanode")
                    .withEnv("CLUSTER_NAME", "hdfs-cluster")
                    .withEnv("DATANODE_ID", "0")
                    .withCommand("bash", "-c", "hdfs datanode")
                    .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                    //.withExposedPorts(50010, 50075, 50020)
                    .withAccessToHost(true)
            ) {
                datanode.setPortBindings(dataNodePortBindings);
                datanode.start();
                System.out.println("Datanode started");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for the cluster to start", e);
                }
                System.out.println("Datanode stopped");
                datanode.stop();
            }

            System.out.println("Namenode stopped");
            namenode.stop();
        }
    }
}
