package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DockerHDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(DockerHDFSCluster.class);

    /** Whether to enable debug logging. */
    private final boolean DEBUG = Boolean.getBoolean("DEBUG_DOCKER");
    /** The configuration. */
    private final Configuration conf;
    /** The network for the docker containers. */
    private final Network network;
    /** The list of datanodes. */
    private final List<GenericContainer<?>> dataNodes = new ArrayList<>();
    /** The nameNode */
    private GenericContainer<?> nameNode;

    /**
     * Create a new DockerHDFSCluster with default configuration.
     */
    public DockerHDFSCluster() {
        this(new Configuration());
    }

    /**
     * Create a new DockerHDFSCluster with the given configuration.
     * @param conf the configuration
     */
    public DockerHDFSCluster(Configuration conf) {
        this.conf = conf;
        this.network = Network.builder().build();
    }

    /**
     * Start the cluster with one namenode and the given number of data nodes.
     * @param numDataNode the number of data nodes
     * @throws IOException if an error occurs starting the cluster
     */
    public void startCluster(int numDataNode) throws IOException {
        this.nameNode = startNameNode();
        if (!nameNode.isRunning()) {
            throw new RuntimeException("NameNode is not running");
        }
        startDataNodes(numDataNode);
        if (!checkDataNodesRunning()) {
            throw new RuntimeException("Not all nodes are running");
        }
    }


    /**
     * Start the NameNode.
     * @return the NameNode container
     */
    public GenericContainer<?> startNameNode() {
        List<String> portBindings = new ArrayList<>();
        portBindings.add("50070:50070");
        GenericContainer<?> nameNode;
        try {
            nameNode = new GenericContainer<>("hadoop:3.3.6")
                    .withNetwork(network)
                    .withNetworkAliases("namenode")
                    .withEnv("CLUSTER_NAME", "hdfs-cluster")
                    .withEnv("NAMENODE_ID", "0")
                    .withCommand("bash", "-c", "hdfs namenode -format && hdfs namenode")
                    .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                    .withExposedPorts(9000, 50070)
                    .withAccessToHost(true);

            nameNode.setPortBindings(portBindings);
            nameNode.start();
            if (DEBUG) {
                System.out.println("[SHUAI-DEBUG] NameNode started");
                Thread.sleep(5000);
            }
            return nameNode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NameNode", e);
        }
    }

    /**
     * Start a data node with the given node ID.
     * @param nodeID the data node ID
     * @return the data node container
     */
    public GenericContainer<?> startDataNode(String nodeID) {
        GenericContainer<?> datanode;
        try {
            datanode = new GenericContainer<>("hadoop:3.3.6")
                    .withNetwork(network)
                    .withNetworkAliases("datanode_" + nodeID)
                    .withEnv("CLUSTER_NAME", "hdfs-cluster")
                    .withEnv("DATANODE_ID_", nodeID)
                    .withCommand("bash", "-c", "hdfs datanode")
                    .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                    .withAccessToHost(true);

            datanode.start();
            if (DEBUG) {
                System.out.println("[SHUAI-DEBUG] DataNode_" + nodeID + " started");
                Thread.sleep(5000);
            }
            return datanode;
        }catch (Exception e) {
            throw new RuntimeException("Failed to start DataNode_" + nodeID, e);
        }
    }

    /**
     * Start the given number of data nodes.
     * @param numDataNodes the number of data nodes
     * @throws IOException if an error occurs starting the data nodes
     */
    private void startDataNodes(int numDataNodes) throws IOException {
        for (int i = 0; i < numDataNodes; i++) {
            GenericContainer<?> dnContainer = startDataNode(String.valueOf(i));
            dataNodes.add(dnContainer);
        }
    }

    /**
     * Check if all data nodes are running.
     * @return true if all data nodes are running
     */
    private boolean checkDataNodesRunning() {
        for (GenericContainer<?> dnContainer : dataNodes) {
            if (!dnContainer.isRunning()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Shut down the cluster.
     */
    public void shutdown() {
        // first shut down all data nodes
        for (GenericContainer<?> dnContainer : dataNodes) {
            dnContainer.stop();
        }
        // then shut down the name node
        nameNode.stop();
        // finally shut down the network
        network.close();
    }


    @Override
    public void close() {
        shutdown();
    }

    /**
     * Get the file system.
     * @return the file system
     * @throws IOException if an error occurs getting the file system
     * @throws URISyntaxException if an error occurs getting the file system
     */
    public FileSystem getFileSystem() throws IOException, URISyntaxException {
        return FileSystem.get(new URI("hdfs://" + nameNode.getHost() + ":9000"), conf);
    }
}
