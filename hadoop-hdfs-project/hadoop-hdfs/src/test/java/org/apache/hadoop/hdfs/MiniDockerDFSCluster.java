package org.apache.hadoop.hdfs;

import edu.illinois.NodeRole;
import edu.illinois.UpgradableClusterException;
import edu.illinois.docker.DockerCluster;
import edu.illinois.docker.DockerNode;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;



public class MiniDockerDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(DockerHDFSCluster.class);

    /** Whether to enable debug logging. */
    private final boolean DEBUG = true; // Boolean.getBoolean("DEBUG_DOCKER");
    /** The upgradable cluster */
    private final DockerCluster cluster;
    /** The configuration. */
    private final Configuration conf;
    /** The network for the docker containers. */
    private final Network network;
    /** The list of datanodes. */
    private final Map<Integer, DockerNode> dataNodes = new HashMap();
    /** The nameNode */
    private DockerNode nameNode;
    private final List<Integer> nameNodePorts = Arrays.asList(9000, 50070);
    private final List<Integer> dataNodePorts = Arrays.asList(50010, 50075, 50020);

    private final String HDFS_SITE_MODIFIER = "modify_hdfs_site.sh";

    public static class Builder {
        private Configuration conf;
        private String dockerImageVersion = "hadoop:3.3.5"; // default version of the Hadoop Docker image
        private int numDataNodes = 1; // default number of data nodes is 1

        public Builder(Configuration conf) {
            this.conf = conf;
        }

        public Builder startDockerImageVersion(String dockerImageVersion) {
            this.dockerImageVersion = dockerImageVersion;
            return this;
        }

        public Builder numDataNodes(int numDataNodes) {
            this.numDataNodes = numDataNodes;
            return this;
        }

        public MiniDockerDFSCluster build() {
            return new MiniDockerDFSCluster(this);
        }
    }


    public MiniDockerDFSCluster(Builder builder) {
        this.conf = builder.conf;
        this.network = Network.builder().build();
        String[] nameNodeCommand = {"bash", "-c", "hdfs namenode -format && hdfs namenode"};
        cluster = new DockerCluster();
        //withNode(NodeRole.MASTER, builder.dockerImageVersion, nameNodeCommand, Arrays.asList(9000, 50070), new HashMap<>(), 5000, new HashMap<>());
        try {
            // bind port 9000 and 50070 to the same port on the host for namenode
            Map<Integer, Integer> boundPorts = new HashMap<>();
            for (int port : nameNodePorts) {
                boundPorts.put(port, port);
            }
            nameNode = cluster.createNode(NodeRole.MASTER, builder.dockerImageVersion, new String[]{"namenode"}, nameNodeCommand, nameNodePorts, boundPorts, 5000, new HashMap<>());
            for (int i = 0; i < builder.numDataNodes; i++) {
                String[] dataNodeCommand = {"bash", "-c", "bash modify_hdfs_site.sh " + i + " && hdfs datanode"};
                Map<Integer, Integer> boundPortsDataNode = new HashMap<>();
                // binds the port of the "dataNodePorts + nodeID" to the same port on the host
                // for example, for nodeID = 1, bind 50011:50011, 50076:50076, 50021:50021
                for (int port : dataNodePorts) {
                    int targetPort = port + i;
                    boundPortsDataNode.put(targetPort, targetPort);
                }
                DockerNode dataNode = cluster.createNode(NodeRole.WORKER, builder.dockerImageVersion, new String[]{"datanode_" + i}, dataNodeCommand, dataNodePorts, boundPortsDataNode, 5000, new HashMap<>());
                dataNodes.put(i, dataNode);
            }
            cluster.start();


            String nameNodeAddress = "hdfs://" + nameNode.getIp() + ":" + nameNode.getMappedPort(9000);
            System.out.println("NameNode address: " + nameNodeAddress);
            // Use localhost and datanode hostname to make sure the Java client
            // can talk to the Hadoop cluster through the Docker network bridge
            this.conf.set("fs.defaultFS", nameNodeAddress);
            this.conf.setBoolean("dfs.client.use.datanode.hostname", true);
            System.setProperty("HADOOP_USER_NAME", "root");
            //TODO: maybe think about to put all the necessary configurations
            // here to overwrite the default configurations
        } catch (UpgradableClusterException e) {
            throw new RuntimeException("Failed to start the cluster", e);
        }
    }

    public DockerCluster getCluster() {
        return cluster;
    }

    @Override
    public void close() {
        try {
            cluster.stop();
        } catch (UpgradableClusterException e) {
            throw new RuntimeException("Failed to stop the cluster", e);
        }
    }

    /**
     * Get the file system.
     * @return the file system
     * @throws RuntimeException if an error occurs getting the file system
     */
    public DistributedFileSystem getFileSystem() {
        try {
            String fsURI = conf.get("fs.defaultFS");
            System.out.println("Accessing the file system at " + fsURI);
            return (DistributedFileSystem) FileSystem.get(new URI(fsURI), conf);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to get the file system", e);
        }
    }

    public DistributedFileSystem getDistributedFileSystem() {
        return (DistributedFileSystem) getFileSystem();
    }

    public DFSClient getDFSClient() {
        return getDistributedFileSystem().getClient();
    }

    public int getLiveDataNodeCount() {
        try {
            return getDFSClient().datanodeReport(HdfsConstants.DatanodeReportType.LIVE).length;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get the number of live data nodes", e);
        }
    }

    private void sleepForDebug(int sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sleeping for the cluster", e);
        }
    }
}
