package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class DockerHDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(DockerHDFSCluster.class);

    /** Whether to enable debug logging. */
    private final boolean DEBUG = true; // Boolean.getBoolean("DEBUG_DOCKER");
    /** The configuration. */
    private final Configuration conf;
    /** The network for the docker containers. */
    private final Network network;
    /** The list of datanodes. */
    private final Map<Integer, GenericContainer<?>> dataNodes = new HashMap();
    /** The nameNode */
    private GenericContainer<?> nameNode;
    private final List<Integer> nameNodePorts = Arrays.asList(9000, 50070);
    private final List<Integer> dataNodePorts = Arrays.asList(50010, 50075, 50020);

    private final String HDFS_SITE_MODIFIER = "modify_hdfs_site.sh";

    /**
     * Create a new DockerHDFSCluster with default configuration.
     */
    public DockerHDFSCluster() {
        this(new Configuration());
    }

    /**
     * Create a new DockerHDFSCluster with the given configuration.
     * @param config the configuration
     */
    public DockerHDFSCluster(Configuration config) {
        this.conf = config;

        // Use localhost and datanode hostname to make sure the Java client
        // can talk to the Hadoop cluster through the Docker network bridge
        this.conf.set("fs.defaultFS", "hdfs://localhost:9000");
        this.conf.setBoolean("dfs.client.use.datanode.hostname", true);
        System.setProperty("HADOOP_USER_NAME", "root");

        this.network = Network.builder().build();
    }

    /**
     * Start the cluster with one namenode and the given number of data nodes.
     * @param numDataNode the number of data nodes
     * @throws IOException if an error occurs starting the cluster
     */
    public void startCluster(String dockerImageVersion, int numDataNode) throws IOException {
        this.nameNode = startNameNode();
        if (!nameNode.isRunning()) {
            throw new RuntimeException("NameNode is not running");
        }
        startDataNodes(dockerImageVersion, numDataNode);
        if (!checkDataNodesRunning()) {
            throw new RuntimeException("Not all nodes are running");
        }
    }

    /**
     * Start the NameNode.
     * @return the NameNode container
     */
    public GenericContainer<?> startNameNode() {
        // bind port 9000 and 50070 to the same port on the host for namenode
        List<String> portBindings = new ArrayList<>();
        for (int port : nameNodePorts) {
            portBindings.add(port + ":" + port);
        }

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
                    //.waitingFor(Wait.forHttp("http://localhost:50070/").forStatusCode(200))
                    .withAccessToHost(true);

            nameNode.setPortBindings(portBindings);
            nameNode.start();

            /**
            String nameNodeHost = nameNode.getHost();
            Integer nameNodePort = nameNode.getMappedPort(9000);
            conf.set("fs.defaultFS", "hdfs://" + nameNodeHost + ":" + nameNodePort);
            conf.set("dfs.namenode.rpc-address", nameNodeHost + ":" + nameNodePort);
            conf.set("dfs.namenode.http-address", nameNodeHost + ":" + nameNode.getMappedPort(50070));
            */

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
    public GenericContainer<?> startDataNode(String dockerImageVersion, int nodeID) {
        List<String> portBindings = new ArrayList<>();
        // binds the port of the "dataNodePorts + nodeID" to the same port on the host
        // for example, for nodeID = 1, bind 50011:50011, 50076:50076, 50021:50021
        for (int port : dataNodePorts) {
            int targetPort = port + nodeID;
            portBindings.add(targetPort + ":" + targetPort);
        }
        GenericContainer<?> datanode;
        try {
            String dataNodeHttpAddress = "http://localhost:" + (50075 + nodeID);
            datanode = new GenericContainer<>(dockerImageVersion)
                    .withNetwork(network)
                    .withNetworkAliases("datanode_" + nodeID)
                    .withEnv("CLUSTER_NAME", "hdfs-cluster")
                    .withEnv("DATANODE_ID_", String.valueOf(nodeID))
                    .withCommand("bash", "-c", "bash modify_hdfs_site.sh " + nodeID + " && hdfs datanode")
                    .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                    //.waitingFor(Wait.forHttp(dataNodeHttpAddress).forStatusCode(200))
                    .withAccessToHost(true);

            datanode.setPortBindings(portBindings);
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
     * Shutdown the given node.
     * @param node the node to shut down
     * @return true if the node was successfully shut down
     */
    public boolean shutdownNode(GenericContainer<?> node) {
        if (node != null) {
            node.stop();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for the cluster to start", e);
            }
            return !node.isRunning();
        }
        return false;
    }

    /**
     * Shutdown the name node.
     * @return true if the name node was successfully shut down
     */
    public boolean shutdownNameNode() {
        return shutdownNode(nameNode);
    }

    /**
     * Shutdown the given data node.
     * @param nodeID the data node ID
     * @return true if the data node was successfully shut down
     */
    public boolean shutdownDataNode(int nodeID) {
        GenericContainer<?> dataNode = dataNodes.get(nodeID);
        if (shutdownNode(dataNode)) {
            dataNodes.remove(nodeID);
            return true;
        }
        return false;
    }

    /**
     * Upgrade the given data node to the new Docker image version.
     * @param nodeID the data node ID
     * @param newDockerImageVersion the new Docker image version
     */
    public void upgradeDataNode(String newDockerImageVersion, int nodeID) {
        if (!dataNodes.containsKey(nodeID)) {
            throw new RuntimeException("DataNode_" + nodeID + " is not running");
        }
        if (!shutdownDataNode(nodeID)) {
            throw new RuntimeException("Failed to shutdown DataNode_" + nodeID);
        }
        GenericContainer<?> dataNode = startDataNode(newDockerImageVersion, nodeID);
        dataNodes.put(nodeID, dataNode);
    }

    /**
     * Start the given number of data nodes.
     * The data node IDs start from 0.
     * @param numDataNodes the number of data nodes
     * @throws IOException if an error occurs starting the data nodes
     */
    private void startDataNodes(String dockerImageVersion, int numDataNodes) throws IOException {
        for (int i = 0; i < numDataNodes; i++) {
            GenericContainer<?> dnContainer = startDataNode(dockerImageVersion, i);
            dataNodes.put(i, dnContainer);
        }
    }

    /**
     * Check if all data nodes are running.
     * @return true if all data nodes are running
     */
    private boolean checkDataNodesRunning() {
        for (GenericContainer<?> dnContainer : dataNodes.values()) {
            if (!dnContainer.isRunning()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shut down the cluster.
     */
    public void shutdownCluster() {
        // first shut down all data nodes
        for (GenericContainer<?> dnContainer : dataNodes.values()) {
            dnContainer.stop();
        }
        // then shut down the name node
        nameNode.stop();
        // finally shut down the network
        network.close();
    }


    @Override
    public void close() {
        shutdownCluster();
    }

    /**
     * Get the file system.
     * @return the file system
     * @throws IOException if an error occurs getting the file system
     * @throws URISyntaxException if an error occurs getting the file system
     */
    public FileSystem getFileSystem() throws IOException, URISyntaxException {
        return FileSystem.get(new URI("hdfs://localhost:9000"), conf);
    }
}
