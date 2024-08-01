package org.apache.hadoop.hdfs;

import edu.illinois.NodeRole;
import edu.illinois.UpgradableClusterException;
import edu.illinois.docker.DockerCluster;
import edu.illinois.docker.DockerNode;
import edu.illinois.util.CommonUtil;
import edu.illinois.util.config.HadoopXMLModifier;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;



public class MiniDockerDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(MiniDockerDFSCluster.class);

    /** Whether to enable debug logging. */
    private final boolean DEBUG = true; // Boolean.getBoolean("DEBUG_DOCKER");
    /** The upgradable cluster */
    private final DockerCluster cluster;
    /** The configuration. */
    private final Configuration conf;
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
        cluster = new DockerCluster();
        try {
            nameNode = cluster.nodeBuilder(builder.dockerImageVersion)
                    .withNodeRole(NodeRole.MASTER)
                    .withNetworkAliases("namenode")
                    .withCommand(CommonUtil.getBashCommand("hdfs namenode -format && hdfs namenode"))
                    .withExposedPorts(nameNodePorts.toArray(new Integer[0]))
                    .withBoundPorts(bindPorts(nameNodePorts, 0))
                    .withStartWaitTime(5000)
                    .withEnv(new HashMap<>());
            for (int i = 0; i < builder.numDataNodes; i++) {
                DockerNode dataNode = cluster.nodeBuilder(builder.dockerImageVersion)
                        .withNodeRole(NodeRole.WORKER)
                        .withNetworkAliases("datanode_" + i)
                        .withCommand(CommonUtil.getBashCommand("bash modify_hdfs_site.sh " + i + " && hdfs datanode"))
                        .withExposedPorts(dataNodePorts.toArray(new Integer[0]))
                        .withBoundPorts(bindPorts(dataNodePorts, i))
                        .withStartWaitTime(5000)
                        .withEnv(new HashMap<>());
                dataNodes.put(i, dataNode);
            }
            cluster.start();

            this.conf = builder.conf;
            String nameNodeAddress = "hdfs://" + nameNode.getHost() + ":" + nameNode.getMappedPort(9000);
            this.conf.set("fs.defaultFS", nameNodeAddress);
            LOG.info("Launching the MiniDockerDFSCluster with the NameNode address: {}", nameNodeAddress);
            // Use localhost and datanode hostname to make sure the Java client
            // can talk to the Hadoop cluster through the Docker network bridge
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
            //System.out.println("Accessing the file system at " + fsURI);
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

    private List<String> bindPorts(List<Integer> ports, int nodeID) {
        List<String> boundPorts = new LinkedList<>();
        for (int port : ports) {
            int targetPort = port + nodeID;
            boundPorts.add(targetPort + ":" + targetPort);
        }
        return boundPorts;
    }

    public File updateConfigFile(String filePath, Map<String, String> configMap) throws IOException {
        File originalConfigFile = new File(filePath);
        File newConfigFile = new File(originalConfigFile.getParent() + "/temp_" + originalConfigFile.getName());
        HadoopXMLModifier.appendPropertiesToFile(originalConfigFile, newConfigFile, configMap);
        return newConfigFile;
    }

    public void updateConfigToAllNodes(String fileName, Map<String, String> configMap) throws IOException {
        String originalFileName = new File(fileName).getName();
        File newConfigFile = updateConfigFile(fileName, configMap);
        cluster.copyFileToAllNodes(newConfigFile.getAbsolutePath(), "/opt/hadoop/etc/hadoop/" + originalFileName);
    }
}
