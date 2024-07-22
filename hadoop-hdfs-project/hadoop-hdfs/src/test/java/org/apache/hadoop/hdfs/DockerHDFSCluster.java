package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DockerHDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(DockerHDFSCluster.class);

    private Configuration conf;
    private Network network;
    private List<GenericContainer<?>> dataNodes = new ArrayList<>();
    private List<GenericContainer<?>> nameNodes = new ArrayList<>();
    private boolean waitSafeMode = true;

    public DockerHDFSCluster(Configuration conf) {
        this.conf = conf;
        this.network = Network.builder().build();
    }

    public void startCluster(int numNode) throws IOException {
        int numDataNodes = numNode - 1;
        startNameNodes();
        startDataNodes(numDataNodes);
        // sleep for a while to allow the cluster to start
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for the cluster to start", e);
        }
        waitClusterUp();
    }

    // We only allow one NameNode for now
    public void startNameNodes() throws IOException {
        //Network network = Network.builder().build();
        List<String> portBindings = new ArrayList<>();
        portBindings.add("50070:50070");
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
            namenode.setPortBindings(portBindings);
            namenode.start();

            // Get the mapped port for Namenode web interface
            //Integer mappedPort = namenode.getMappedPort(50070);

            // Access the Namenode web interface
            //String address = "http://" + namenode.getHost() + ":" + mappedPort;
            //System.out.println("Now the namenode web interface is accessible at: " + address);

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

    private void startDataNodes(int numDataNodes) throws IOException {
        for (int i = 0; i < numDataNodes; i++) {
            GenericContainer<?> dnContainer = new GenericContainer<>(DockerImageName.parse("hadoop:3.3.6"))
                    .withNetwork(network)
                    .withNetworkAliases("datanode-" + i)
                    .withEnv("CLUSTER_NAME", "hdfs-cluster")
                    .withEnv("DATANODE_ID", String.valueOf(i))
                    .withCommand("bash", "-c", "hdfs datanode")
                    .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()));
                    //.withExposedPorts(50010, 50075, 50020);
            dnContainer.start();
            dataNodes.add(dnContainer);
        }
    }

    public void waitClusterUp() throws IOException {
        for (GenericContainer<?> nnContainer : nameNodes) {
            waitNameNodeUp(nnContainer);
        }
        for (GenericContainer<?> dnContainer : dataNodes) {
            waitDataNodeUp(dnContainer);
        }
    }

    private void waitNameNodeUp(GenericContainer<?> nnContainer) throws IOException {
        while (!nnContainer.isRunning()) {
            try {
                LOG.warn("Waiting for NameNode to start...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for NameNode to start", e);
            }
        }
    }

    private void waitDataNodeUp(GenericContainer<?> dnContainer) throws IOException {
        while (!dnContainer.isRunning()) {
            try {
                LOG.warn("Waiting for DataNode to start...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for DataNode to start", e);
            }
        }
    }

    public void shutdown() {
        for (GenericContainer<?> nnContainer : nameNodes) {
            nnContainer.stop();
        }
        for (GenericContainer<?> dnContainer : dataNodes) {
            dnContainer.stop();
        }
        network.close();
    }


    @Override
    public void close() {
        shutdown();
    }

    public FileSystem getFileSystem() throws IOException, URISyntaxException {
        return FileSystem.get(new URI("hdfs://" + nameNodes.get(0).getHost() + ":9000"), conf);
    }

}
