package org.apache.hadoop.hdfs;

import edu.illinois.NodeRole;
import edu.illinois.UpgradableClusterConfig;
import edu.illinois.UpgradableClusterException;
import edu.illinois.docker.DockerCluster;
import edu.illinois.docker.DockerNode;
import edu.illinois.util.CommonUtil;
import edu.illinois.util.config.ConfigTracker;
import edu.illinois.util.config.HadoopXMLModifier;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MiniDockerDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(MiniDockerDFSCluster.class);

    /** Whether to enable debug logging. */
    private final boolean DEBUG = true; // Boolean.getBoolean("DEBUG_DOCKER");
    /** The upgradable cluster */
    private final DockerCluster cluster;
    /** The configuration. */
    private final Configuration conf;
    /** The list of datanodes. */
    private final Map<Integer, DockerNode> dataNodes = new LinkedHashMap<>();
    /** The nameNode */
    private DockerNode nameNode;
    private final List<Integer> nameNodePorts = Arrays.asList(9000, 50070);
    private final List<Integer> dataNodePorts = Arrays.asList(50010, 50075, 50020);

    private final String HDFS_SITE_MODIFIER = "modify_hdfs_site.sh";

    String startVersion = System.getProperty("startVersion", "hadoop:3.3.5");
    String upgradeVersion = System.getProperty("upgradeVersion", "hadoop:3.3.6");

    private final Map<String, String> defaultCoreSite = Map.of(
            "fs.defaultFS", "hdfs://0.0.0.0:9000"
    );
    private final Map<String, String> defaultHDFSSite = Map.of(
            "dfs.namenode.rpc-address", "namenode:9000",
            "dfs.namenode.http-address", "namenode:50070",
            "dfs.datanode.address", "0.0.0.0:50010",
            "dfs.datanode.http.address", "0.0.0.0:50075",
            "dfs.datanode.ipc.address", "0.0.0.0:50020",
            "dfs.datanode.hostname", "localhost",
            "hadoop.security.authentication", "simple",
            "dfs.namenode.fs-limits.min-block-size", "0"
    );

    private List<File> generateConfigFiles() {

        List<File> configFiles = new ArrayList<>();
        try {
            // Ignore the defaultCoreSite and defaultHDFSSite from the ConfigTracker
            for (String key : defaultCoreSite.keySet()) {
                ConfigTracker.removeSetParam(key);
            }
            for (String key : defaultHDFSSite.keySet()) {
                ConfigTracker.removeSetParam(key);
            }
            Map<String, String> mutableDefaultCoreSite = new HashMap<>(defaultCoreSite);
            Map<String, String> mutableDefaultHDFSSite = new HashMap<>(defaultHDFSSite);
            File hdfsSite = new File(UpgradableClusterConfig.DEFAULT_TOOL_DIR + "hdfs-site.xml" + UUID.randomUUID());
            HadoopXMLModifier.mergeTwoConfigToNewFile(hdfsSite, mutableDefaultHDFSSite, ConfigTracker.getSetParams());
            File coreSite = new File(UpgradableClusterConfig.DEFAULT_TOOL_DIR + "core-site.xml" + UUID.randomUUID());
            HadoopXMLModifier.mergeTwoConfigToNewFile(coreSite, mutableDefaultCoreSite, ConfigTracker.getSetParams());
            configFiles.add(hdfsSite);
            configFiles.add(coreSite);
            return configFiles;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate the NameNode configuration file", e);
        }
    }

    private List<Integer> getNameNodePorts(File nnNodeLog) {
        // parse the nnNodeLog and search for key word:
        // (1) INFO namenode.NameNode: Clients should use 0.0.0.0:???? to access this namenode/service
        // (2) INFO hdfs.DFSUtil: Starting Web-server for hdfs at: http://namenode:????
        // I need to get the ???? and ????? port number by reading the log file and use regex to extract the port number
        // and return the list of ports

        List<Integer> ports = new ArrayList<>();
        Map<String, Integer> portMap = new HashMap<>();
        // Define the regular expressions to match the port numbers
        String regex1 = "Clients should use .*:(\\d+) to access this namenode/service";
        String regex2 = "Starting Web-server for hdfs at: http://.*:(\\d+)";

        Pattern pattern1 = Pattern.compile(regex1);
        Pattern pattern2 = Pattern.compile(regex2);

        try (BufferedReader br = new BufferedReader(new FileReader(nnNodeLog))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher1 = pattern1.matcher(line);
                Matcher matcher2 = pattern2.matcher(line);

                if (matcher1.find()) {
                    portMap.put("fs.defaultFS.port", Integer.parseInt(matcher1.group(1)));
                    System.out.println("Port for Namenode service: " + matcher1.group(1));
                }

                if (matcher2.find()) {
                    portMap.put("dfs.namenode.http-address.port", Integer.parseInt(matcher2.group(1)));
                    System.out.println("Port for HDFS Web-server: " + matcher2.group(1));
                }
            }
            ports.add(portMap.get("fs.defaultFS.port"));
            ports.add(portMap.get("dfs.namenode.http-address.port"));
            return ports;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the log file");
        }
    }

    private List<Integer> getDataNodePorts(File dnNodeLog) {
        // parse the dnNodeLog and search for key word:
        // (1) INFO datanode.DataNode: Opened streaming server at /0.0.0.0:50010
        // (2) INFO web.DatanodeHttpServer: Listening HTTP traffic on /0.0.0.0:50075
        // (3) INFO datanode.DataNode: Opened IPC server at /0.0.0.0:50020

        List<Integer> ports = new ArrayList<>();
        Map<String, Integer> portMap = new HashMap<>();
        // Define the regular expressions to match the port numbers
        String regex1 = "Opened streaming server at /\\d+\\.\\d+\\.\\d+\\.\\d+:(\\d+)";
        String regex2 = "Listening HTTP traffic on /\\d+\\.\\d+\\.\\d+\\.\\d+:(\\d+)";
        String regex3 = "Opened IPC server at /\\d+\\.\\d+\\.\\d+\\.\\d+:(\\d+)";

        Pattern pattern1 = Pattern.compile(regex1);
        Pattern pattern2 = Pattern.compile(regex2);
        Pattern pattern3 = Pattern.compile(regex3);

        try (BufferedReader br = new BufferedReader(new FileReader(dnNodeLog))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher1 = pattern1.matcher(line);
                Matcher matcher2 = pattern2.matcher(line);
                Matcher matcher3 = pattern3.matcher(line);

                if (matcher1.find()) {
                    portMap.put("dfs.datanode.address.port", Integer.parseInt(matcher1.group(1)));

                }

                if (matcher2.find()) {
                    portMap.put("dfs.datanode.http.address.port", Integer.parseInt(matcher2.group(1)));
                }

                if (matcher3.find()) {
                    portMap.put("dfs.datanode.ipc.address.port", Integer.parseInt(matcher3.group(1)));
                }
            }
            ports.add(portMap.get("dfs.datanode.address.port"));
            ports.add(portMap.get("dfs.datanode.http.address.port"));
            ports.add(portMap.get("dfs.datanode.ipc.address.port"));
            return ports;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the log file");
        }
    }

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

        public Builder format(boolean format) {
            // TODO: follow mini cluster and implement this
            return this;
        }

        public MiniDockerDFSCluster build() {
            return new MiniDockerDFSCluster(this);
        }
    }


    public MiniDockerDFSCluster(Builder builder) {
        this.conf = builder.conf;
        cluster = new DockerCluster();
        List<File> configFiles = generateConfigFiles();
        try {
            File nnNodeLog = new File(UpgradableClusterConfig.DEFAULT_TOOL_DIR + "namenode.log" + UUID.randomUUID());
            nameNode = cluster.nodeBuilder(builder.dockerImageVersion)
                    .withNodeRole(NodeRole.MASTER)
                    .withNetworkAliases("namenode")
                    .withCommand(CommonUtil.getBashCommand("cat /opt/hadoop/etc/hadoop/hdfs-site.xml && cat /opt/hadoop/etc/hadoop/core-site.xml && hdfs namenode -format && hdfs namenode"))
                    .withExposedPorts(nameNodePorts.toArray(new Integer[0]))
                    .withBoundPorts(bindPorts(nameNodePorts, 0))
                    //.waitingFor(Wait.forLogMessage("INFO blockmanagement.CacheReplicationMonitor: Starting CacheReplicationMonitor with interval", 1))
                    .withStartWaitTime(5000)
                    .withConfigFile(configFiles.get(0).getAbsolutePath(), "/opt/hadoop/etc/hadoop/hdfs-site.xml", new HashMap<>())
                    .withConfigFile(configFiles.get(1).getAbsolutePath(), "/opt/hadoop/etc/hadoop/core-site.xml", new HashMap<>())
                    .withLogConsumer(outputFrame -> {
                        try {
                            Files.write(nnNodeLog.toPath(), outputFrame.getBytes());
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to write the log to the file", e);
                        }
                    })
                    .withEnv(new HashMap<>());
            for (int i = 0; i < builder.numDataNodes; i++) {
                DockerNode dataNode = cluster.nodeBuilder(builder.dockerImageVersion)
                        .withNodeRole(NodeRole.WORKER)
                        .withNetworkAliases("datanode_" + i)
                        .withCommand(CommonUtil.getBashCommand("cat /opt/hadoop/etc/hadoop/hdfs-site.xml && cat /opt/hadoop/etc/hadoop/core-site.xml && bash modify_hdfs_site.sh " + i + " && hdfs datanode"))
                        .withExposedPorts(exposedPorts(dataNodePorts, i))
                        .withBoundPorts(bindPorts(dataNodePorts, i))
                        .withConfigFile(configFiles.get(0).getAbsolutePath(), "/opt/hadoop/etc/hadoop/hdfs-site.xml", new HashMap<>())
                        .withConfigFile(configFiles.get(1).getAbsolutePath(), "/opt/hadoop/etc/hadoop/core-site.xml", new HashMap<>())
                        .withStartWaitTime(5000)
                        .withEnv(new HashMap<>());
                dataNodes.put(i, dataNode);
            }
            cluster.start();


            /**
            // TODO: here we need a logic to parse the namenode address
            List<Integer> nameNodePorts = getNameNodePorts(nnNodeLog);
            Integer nnFSPort = nameNodePorts.get(0);
            Integer nnHTTPPort = nameNodePorts.get(1);
            // TODO: use getMappedPort() to get the FS address and port
            conf.set("fs.defaultFS",  "hdfs://" + nameNode.getHost() + ":" + nameNode.getMappedPort(nnFSPort));
            LOG.info("Launching the MiniDockerDFSCluster with the NameNode address: {}", conf.get("fs.defaultFS"));
            //conf.set("dfs.namenode.http-address", nameNode.getHost() + ":" + nameNode.getMappedPort(nnHTTPPort));

            // TODO: set it to the configuration of the datanode

             **/



            String nameNodeAddress = "hdfs://" + nameNode.getHost() + ":" + nameNode.getMappedPort(9000);
            this.conf.set("fs.defaultFS", nameNodeAddress);

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

    public void shutdown() {
        close();
    }

    public FileContext getFileContext() {
        try {
            String fsURI = conf.get("fs.defaultFS");
            return FileContext.getFileContext(new URI(fsURI), conf);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to get the file context", e);
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

    private Integer[] exposedPorts(List<Integer> dataNodePorts, int nodeId) {
        List<Integer> exposedPorts = new LinkedList<>();
        for (int port : dataNodePorts) {
            int targetPort = port + nodeId;
            exposedPorts.add(targetPort);
        }
        return exposedPorts.toArray(new Integer[0]);
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

    public void upgradeDatanode(int index) {
        if (dataNodes.containsKey(index)) {
            dataNodes.get(index).upgradeTo(upgradeVersion);
        } else{
            LOG.warn("There is no datanode at index {}, skip the upgrade", index);
        }
    }

    public void waitActive() {
        return;
    }

    public void waitClusterUp() {
        return;
    }

    public URI getURI() {
        try {
            return new URI(conf.get("fs.defaultFS"));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get the URI", e);
        }
    }

    public InetSocketAddress getNameNodeHttpAddress() {
        return new InetSocketAddress(nameNode.getHost(), nameNode.getMappedPort(50070));
    }


    public int getNameNodePort() {
        return nameNode.getMappedPort(9000);
    }

    public void restartNameNode() {
        cluster.getMasterNode().restart();
    }
}
