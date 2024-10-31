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
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.rmi.client.RemoteObjectProxy;
import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.test.GenericTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.apache.hadoop.hdfs.remoteProxies.*;

import java.io.*;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.hadoop.hdfs.DFSConfigKeys.*;


public class MiniDockerDFSCluster implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(MiniDockerDFSCluster.class);

    // Changing this default may break some tests that assume it is 2.
    private static final int DEFAULT_STORAGES_PER_DATANODE = 2;
    public static final String PROP_TEST_BUILD_DATA =
            GenericTestUtils.SYSPROP_TEST_DATA_DIR;
    /** Configuration option to set the data dir: {@value} */
    public static final String HDFS_MINIDFS_BASEDIR = "hdfs.minidfs.basedir";
    /** Configuration option to set the provided data dir: {@value} */
    public static final String HDFS_MINIDFS_BASEDIR_PROVIDED =
            "hdfs.minidfs.basedir.provided";
    public static final String  DFS_NAMENODE_SAFEMODE_EXTENSION_TESTING_KEY
            = DFS_NAMENODE_SAFEMODE_EXTENSION_KEY + ".testing";
    public static final String  DFS_NAMENODE_DECOMMISSION_INTERVAL_TESTING_KEY
            = DFS_NAMENODE_DECOMMISSION_INTERVAL_KEY + ".testing";

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

    private final int nnRMIConnectionPort = 1099;
    private final int nnRMIObjectPort = 1100;

    private final int dnRMIConnectionPort = 1200;
    private final int dnRMIObjectPort = 1300;

    private final ArrayList<DataNodeInterface> dataNodeProxies = new ArrayList<>();
    private final ArrayList<NameNodeInterface> nameNodeProxies = new ArrayList<>();

    private final List<Integer> nameNodePorts = Arrays.asList(9000, 50070, nnRMIConnectionPort, nnRMIObjectPort);
    private final List<Integer> dataNodePorts = Arrays.asList(50010, 50075, 50040, dnRMIConnectionPort, dnRMIObjectPort);

    private final String HDFS_SITE_MODIFIER = "modify_hdfs_site.sh";

    //String startVersion = System.getProperty("startVersion", "hadoop:3.3.5");
    String upgradeVersion = System.getProperty("upgradeVersion", "hadoop:3.3.6");

    private static final Map<String, String> defaultCoreSite = new LinkedHashMap<>();
    private static final Map<String, String> defaultHDFSSite = new LinkedHashMap<>();

    static {
        defaultCoreSite.put("fs.defaultFS", "hdfs://0.0.0.0:9000");

        defaultHDFSSite.put("dfs.namenode.rpc-address", "namenode:9000");
        defaultHDFSSite.put("dfs.namenode.http-address", "namenode:50070");
        defaultHDFSSite.put("dfs.datanode.address", "0.0.0.0:50010");
        defaultHDFSSite.put("dfs.datanode.http.address", "0.0.0.0:50075");
        defaultHDFSSite.put("dfs.datanode.ipc.address", "0.0.0.0:50040");
        defaultHDFSSite.put("dfs.datanode.hostname", "localhost");
        defaultHDFSSite.put("hadoop.security.authentication", "simple");
        defaultHDFSSite.put("dfs.namenode.fs-limits.min-block-size", "0");
    }

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
        // (3) INFO datanode.DataNode: Opened IPC server at /0.0.0.0:50040

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
        private String dockerImageVersion = System.getProperty("startVersion", "hadoop:3.3.5"); // default version is 3.3.5
        private int numDataNodes = 1; // default number of data nodes is 1
        private int nameNodePort = 9000;
        private int nameNodeHttpPort = 50070;
        private int[] dnHttpPorts = null;
        private int[] dnIpcPorts = null;
        private StorageType[][] storageTypes = null;
        private StorageType[] storageTypes1D = null;
        private int storagesPerDatanode = DEFAULT_STORAGES_PER_DATANODE;
        private boolean format = true;
        private boolean manageNameDfsDirs = true;
        private boolean manageNameDfsSharedDirs = true;
        private boolean enableManagedDfsDirsRedundancy = true;
        private boolean manageDataDfsDirs = true;
        private HdfsServerConstants.StartupOption option = null;
        private HdfsServerConstants.StartupOption dnOption = null;
        private String[] racks = null;
        private String [] hosts = null;
        private long [] simulatedCapacities = null;
        private long [][] storageCapacities = null;
        private long [] storageCapacities1D = null;
        private String clusterId = null;
        private boolean waitSafeMode = true;
        private boolean setupHostsFile = false;
        private MiniDFSNNTopology nnTopology = null;
        private boolean checkExitOnShutdown = true;
        private boolean checkDataNodeAddrConfig = false;
        private boolean checkDataNodeHostConfig = false;
        private Configuration[] dnConfOverlays;
        private boolean skipFsyncForTesting = true;
        private boolean useConfiguredTopologyMappingClass = false;


        public Builder(Configuration conf) {
            //TODO: here actually we have to sync this configurations with the docker cluster configuration
            this.conf = conf;
        }

        public Builder(Configuration conf, File baseDir) {
            this.conf = conf;
            // TODO: here we have to set the base directory for the docker cluster
            LOG.warn("The base directory in Builder is not implemented yet");
            System.out.println("The base directory in Builder is not implemented yet");
        }

        public Builder startDockerImageVersion(String dockerImageVersion) {
            this.dockerImageVersion = dockerImageVersion;
            return this;
        }

        /**
         * Default: 1
         */
        public Builder numDataNodes(int numDataNodes) {
            this.numDataNodes = numDataNodes;
            return this;
        }

        public MiniDockerDFSCluster build() {
            return new MiniDockerDFSCluster(this);
        }

        // From original MiniDFSCluster

        /**
         * Default: 0
         */
        public Builder nameNodePort(int val) {
            this.nameNodePort = val;
            return this;
        }

        /**
         * Default: 0
         */
        public Builder nameNodeHttpPort(int val) {
            this.nameNodeHttpPort = val;
            return this;
        }

        public Builder setDnHttpPorts(int... ports) {
            this.dnHttpPorts = ports;
            return this;
        }

        public Builder setDnIpcPorts(int... ports) {
            this.dnIpcPorts = ports;
            return this;
        }

        /**
         * Default: DEFAULT_STORAGES_PER_DATANODE
         */
        public Builder storagesPerDatanode(int numStorages) {
            this.storagesPerDatanode = numStorages;
            return this;
        }

        /**
         * Set the same storage type configuration for each datanode.
         * If storageTypes is uninitialized or passed null then
         * StorageType.DEFAULT is used.
         */
        public Builder storageTypes(StorageType[] types) {
            this.storageTypes1D = types;
            return this;
        }

        /**
         * Set custom storage type configuration for each datanode.
         * If storageTypes is uninitialized or passed null then
         * StorageType.DEFAULT is used.
         */
        public Builder storageTypes(StorageType[][] types) {
            this.storageTypes = types;
            return this;
        }

        /**
         * Set the same storage capacity configuration for each datanode.
         * If storageTypes is uninitialized or passed null then
         * StorageType.DEFAULT is used.
         */
        public Builder storageCapacities(long[] capacities) {
            this.storageCapacities1D = capacities;
            return this;
        }

        /**
         * Set custom storage capacity configuration for each datanode.
         * If storageCapacities is uninitialized or passed null then
         * capacity is limited by available disk space.
         */
        public Builder storageCapacities(long[][] capacities) {
            this.storageCapacities = capacities;
            return this;
        }

        /**
         * Default: true
         */
        public Builder format(boolean val) {
            this.format = val;
            return this;
        }

        /**
         * Default: true
         */
        public Builder manageNameDfsDirs(boolean val) {
            this.manageNameDfsDirs = val;
            return this;
        }

        /**
         * Default: true
         */
        public Builder manageNameDfsSharedDirs(boolean val) {
            this.manageNameDfsSharedDirs = val;
            return this;
        }

        /**
         * Default: true
         */
        public Builder enableManagedDfsDirsRedundancy(boolean val) {
            this.enableManagedDfsDirsRedundancy = val;
            return this;
        }

        /**
         * Default: true
         */
        public Builder manageDataDfsDirs(boolean val) {
            this.manageDataDfsDirs = val;
            return this;
        }

        /**
         * Default: null
         */
        public Builder startupOption(HdfsServerConstants.StartupOption val) {
            this.option = val;
            return this;
        }

        /**
         * Default: null
         */
        public Builder dnStartupOption(HdfsServerConstants.StartupOption val) {
            this.dnOption = val;
            return this;
        }

        /**
         * Default: null
         */
        public Builder racks(String[] val) {
            this.racks = val;
            return this;
        }

        /**
         * Default: null
         */
        public Builder hosts(String[] val) {
            this.hosts = val;
            return this;
        }

        /**
         * Use SimulatedFSDataset and limit the capacity of each DN per
         * the values passed in val.
         *
         * For limiting the capacity of volumes with real storage, see
         * {@link FsVolumeImpl#setCapacityForTesting}
         * Default: null
         */
        public Builder simulatedCapacities(long[] val) {
            this.simulatedCapacities = val;
            return this;
        }

        /**
         * Default: true
         */
        public Builder waitSafeMode(boolean val) {
            this.waitSafeMode = val;
            return this;
        }

        /**
         * Default: true
         */
        public Builder checkExitOnShutdown(boolean val) {
            this.checkExitOnShutdown = val;
            return this;
        }

        /**
         * Default: false
         */
        public Builder checkDataNodeAddrConfig(boolean val) {
            this.checkDataNodeAddrConfig = val;
            return this;
        }

        /**
         * Default: false
         */
        public Builder checkDataNodeHostConfig(boolean val) {
            this.checkDataNodeHostConfig = val;
            return this;
        }

        /**
         * Default: null
         */
        public Builder clusterId(String cid) {
            this.clusterId = cid;
            return this;
        }

        /**
         * Default: false
         * When true the hosts file/include file for the cluster is setup
         */
        public Builder setupHostsFile(boolean val) {
            this.setupHostsFile = val;
            return this;
        }

        /**
         * Default: a single namenode.
         * See {@link MiniDFSNNTopology#simpleFederatedTopology(int)} to set up
         * federated nameservices
         */
        public Builder nnTopology(MiniDFSNNTopology topology) {
            this.nnTopology = topology;
            return this;
        }

        /**
         * Default: null
         *
         * An array of {@link Configuration} objects that will overlay the
         * global MiniDFSCluster Configuration for the corresponding DataNode.
         *
         * Useful for setting specific per-DataNode configuration parameters.
         */
        public Builder dataNodeConfOverlays(Configuration[] dnConfOverlays) {
            this.dnConfOverlays = dnConfOverlays;
            return this;
        }

        /**
         * Default: true
         * When true, we skip fsync() calls for speed improvements.
         */
        public Builder skipFsyncForTesting(boolean val) {
            this.skipFsyncForTesting = val;
            return this;
        }

        public Builder useConfiguredTopologyMappingClass(
                boolean useConfiguredTopologyMappingClass) {
            this.useConfiguredTopologyMappingClass =
                    useConfiguredTopologyMappingClass;
            return this;
        }

        /**
         * set the value of DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY in the config
         * file.
         */
        public Builder setNNRedundancyConsiderLoad(final boolean val) {
            conf.setBoolean(DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY, val);
            return this;
        }

    }

    public MiniDockerDFSCluster(Builder builder) {
        // Re-enable symlinks for tests, see HADOOP-10020 and HADOOP-10052
        FileSystem.enableSymlinks();

        this.conf = builder.conf;
        cluster = new DockerCluster();
        List<File> configFiles = generateConfigFiles();
        try {
            File nnNodeLog = new File(UpgradableClusterConfig.DEFAULT_TOOL_DIR + "namenode.log" + UUID.randomUUID());
            nameNode = cluster.nodeBuilder(builder.dockerImageVersion)
                    .withNodeRole(NodeRole.MASTER)
                    .withNetworkAliases("namenode")
                    //.withCommand(CommonUtil.getBashCommand("cat /opt/hadoop/etc/hadoop/hdfs-site.xml && cat /opt/hadoop/etc/hadoop/core-site.xml && hdfs namenode -format && bash loop.sh"))
                    .withCommand(CommonUtil.getBashCommand("cat /opt/hadoop/etc/hadoop/hdfs-site.xml && cat /opt/hadoop/etc/hadoop/core-site.xml && " +
                            "bash modify_hdfs_env.sh " + nnRMIConnectionPort + " " + nnRMIObjectPort + " && hdfs namenode -format && bash loop.sh"))
                    //.withExposedPorts(nameNodePorts.toArray(new Integer[0]))
                    .withBoundPorts(bindPorts(nameNodePorts, 0))
                    //.waitingFor(Wait.forLogMessage("INFO blockmanagement.CacheReplicationMonitor: Starting CacheReplicationMonitor with interval", 1))
                    .withStartWaitTime(5000)
                    .withConfigFile(configFiles.get(0).getAbsolutePath(), "/opt/hadoop/etc/hadoop/hdfs-site.xml", new HashMap<>())
                    .withConfigFile(configFiles.get(1).getAbsolutePath(), "/opt/hadoop/etc/hadoop/core-site.xml", new HashMap<>())
                    // .withLogConsumer(outputFrame -> {
                    //     try {
                    //         Files.write(nnNodeLog.toPath(), outputFrame.getBytes());
                    //     } catch (IOException e) {
                    //         throw new RuntimeException("Failed to write the log to the file", e);
                    //     }
                    // })
                    .withEnv(new HashMap<>());
            nameNode.start();
            new Thread(() -> {
                try {
                    LOG.info("Start the namenode in a new Thread.");
                    nameNode.execInContainer(CommonUtil.getBashCommand("bash start_namenode.sh >> namenode.log 2>&1"));
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            Thread.sleep(5000);
            nameNode.setExposedPorts(nameNodePorts);
            // TODO: change this later when we have multiple namenodes
            nameNodeProxies.add(getNameNode());

            for (int i = 0; i < builder.numDataNodes; i++) {
                DockerNode dataNode = cluster.nodeBuilder(builder.dockerImageVersion)
                        .withNodeRole(NodeRole.WORKER)
                        .withNetworkAliases("datanode_" + i)
                        //.withCommand(CommonUtil.getBashCommand("cat /opt/hadoop/etc/hadoop/hdfs-site.xml && cat /opt/hadoop/etc/hadoop/core-site.xml && bash modify_hdfs_site.sh " + i + " && bash start_datanode.sh"))
                        .withCommand(CommonUtil.getBashCommand("cat /opt/hadoop/etc/hadoop/hdfs-site.xml && cat /opt/hadoop/etc/hadoop/core-site.xml && bash loop.sh"))
                        //.withExposedPorts(exposedPortsArray(dataNodePorts, i))
                        .withBoundPorts(bindPorts(dataNodePorts, i))
                        .withConfigFile(configFiles.get(0).getAbsolutePath(), "/opt/hadoop/etc/hadoop/hdfs-site.xml", new HashMap<>())
                        .withConfigFile(configFiles.get(1).getAbsolutePath(), "/opt/hadoop/etc/hadoop/core-site.xml", new HashMap<>())
                        .withStartWaitTime(5000)
                        .withEnv(new HashMap<>());
                dataNodes.put(i, dataNode);
                dataNode.start();
                int finalI = i;
                new Thread(() -> {
                    try {
                        LOG.info("Start the datanode in a new Thread.");
                        //dataNode.execInContainer(CommonUtil.getBashCommand("bash modify_hdfs_site.sh " + finalI + " && bash start_datanode.sh >> datanode_" + finalI + ".log 2>&1"));
                        dataNode.execInContainer(CommonUtil.getBashCommand("bash modify_hdfs_env.sh "
                                + (dnRMIConnectionPort + finalI) + " " +  (dnRMIObjectPort + finalI) + " && bash modify_hdfs_site.sh " + finalI + " && bash start_datanode.sh >> datanode_" + finalI + ".log 2>&1"));
                    } catch (InterruptedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                Thread.sleep(5000);
                dataNode.setExposedPorts(exposedPortsList(dataNodePorts, i));
                dataNodeProxies.add(getDataNode(i));
            }
            //cluster.start();

            /**
            for (int i = 0; i < builder.numDataNodes; i++) {
                DockerNode dataNode = dataNodes.get(i);
                dataNode.execInContainer(CommonUtil.getBashCommand(" bash modify_hdfs_site.sh " + i + " && bash start_datanode.sh"));
                Thread.sleep(5000);
                dataNode.setExposedPorts(Arrays.stream(exposedPorts(dataNodePorts, i)).collect(Collectors.toList()));
            }**/

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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public DockerCluster getCluster() {
        return cluster;
    }

    public Configuration getConfiguration(int index) {
        LOG.warn("getConfiguration is not implemented yet");
        System.out.println("getConfiguration is not implemented yet");
        return conf;
    }

    public void transitionToActive(int index) {
        LOG.warn("transitionToActive is not implemented yet");
        System.out.println("transitionToActive is not implemented yet");
    }

    @Override
    public void close() {
        try {
            // print all the logs
            // first print the namenode log
            /**
            String randomUUID = UUID.randomUUID().toString();
            LOG.info("Print the logs of the namenode");
            File namenodeLog = new File("./namenode" + randomUUID + ".log");
            nameNode.copyFileFromContainer("namenode.log", namenodeLog.getAbsolutePath());
            LOG.info(new String(readAllBytes(namenodeLog.toPath())));
            namenodeLog.delete();

            // then print the datanodes logs
            LOG.info("Print the logs of the datanodes");
            for (int i = 0; i < dataNodes.size(); i++) {
                LOG.info("Print the logs of the datanode at index {}", i);
                File datanodeLog = new File("./datanode_" + i + randomUUID + ".log");
                dataNodes.get(i).copyFileFromContainer("datanode_" + i + ".log", datanodeLog.getAbsolutePath());
                LOG.info(new String(readAllBytes(datanodeLog.toPath())));
                datanodeLog.delete();
            }
             **/
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

    public DistributedFileSystem getFileSystem(int idx) {
        LOG.warn("The getFileSystem(int idx) function is not implemented yet.");
        System.out.println("The getFileSystem(int idx) function is not implemented yet.");
        return getFileSystem();
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
        return getFileSystem();
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

    private Integer[] exposedPortsArray(List<Integer> dataNodePorts, int nodeId) {
        return exposedPortsList(dataNodePorts, nodeId).toArray(new Integer[0]);
    }

    private List<Integer> exposedPortsList(List<Integer> dataNodePorts, int nodeId) {
        List<Integer> exposedPorts = new LinkedList<>();
        for (int port : dataNodePorts) {
            int targetPort = port + nodeId;
            exposedPorts.add(targetPort);
        }
        return exposedPorts;
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
        if (UpgradableClusterConfig.SKIP_UPGRADE) {
            LOG.info("Skip the upgrade of the datanode at index {}", index);
            return;
        }
        if (dataNodes.containsKey(index)) {
            //dataNodes.get(index).upgradeTo(upgradeVersion);
            // TODO: This is for test only -- instead of launch a new container, we just stop the datanode process and restart it
            DockerNode dataNode = dataNodes.get(index);
            try {
                dataNode.execInContainer(CommonUtil.getBashCommand("kill -9 $(ps aux | grep 'proc_datanode' | grep -v \"grep\" | awk '{print $2}')"));
                LOG.info("Kill the datanode process at index {}", index);
                Thread.sleep(5000);
                // create a new thread and call dataNode.execInContainer("hdfs datanode") to restart the datanode
                int finalI = index;
                new Thread(() -> {
                    try {
                        LOG.info("Restart the datanode process at index {} in a new Thread.", finalI);
                        dataNode.execInContainer(CommonUtil.getBashCommand("bash start_datanode.sh >> datanode_" + finalI + ".log 2>&1"));
                    } catch (InterruptedException | IOException e) {
                        LOG.info("Failed to restart the datanode process due to {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                }).start();
                Thread.sleep(10000);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else{
            LOG.warn("There is no datanode at index {}, skip the upgrade", index);
        }
    }

    public void waitActive() throws BindException {
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

    public URI getURI(int index) {
        LOG.warn("The getURI(int index) function is not implemented yet.");
        System.out.println("The getURI(int index) function is not implemented yet.");
        return getURI();
    }

    public InetSocketAddress getNameNodeHttpAddress() {
        return new InetSocketAddress(nameNode.getHost(), nameNode.getMappedPort(50070));
    }


    public int getNameNodePort() {
        return nameNode.getMappedPort(9000);
    }

    public void restartNameNode() {
        //cluster.getMasterNode().restart();

        try {
            DockerNode nameNode = cluster.getMasterNode();
            Container.ExecResult res = nameNode.execInContainer(CommonUtil.getBashCommand("kill -9 $(ps aux | grep 'namenode' | grep -v \"grep\" | awk '{print $2}')"));
            LOG.info(res.toString());
            LOG.info("Kill the namenode process");
            Thread.sleep(10000);
            // create a new thread and call dataNode.execInContainer("hdfs datanode") to restart the datanode
            new Thread(() -> {
                try {
                    LOG.info("Restart the namenode process in a new Thread.");
                    nameNode.execInContainer(CommonUtil.getBashCommand("bash start_namenode.sh >> namenode.log 2>&1"));
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            Thread.sleep(10000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //======================= RMI related functions ========================
    public Registry getNNRegistry() throws RemoteException {
        return LocateRegistry.getRegistry(nnRMIConnectionPort);
    }

    public Registry getDNRegistry(int dnIndex) throws RemoteException {
        return LocateRegistry.getRegistry(dnRMIConnectionPort + dnIndex);
    }

    //======================= RMI related object getter functions ========================
    public NameNodeInterface getNameNode() {
        try {
            RemoteObject nameNode = (RemoteObject) getNNRegistry().lookup(NameNode.class.getName());
            return RemoteObjectProxy.newInstance(nameNode, NameNodeInterface.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the NameNode remote object through RMI", e);
        }
    }

    public NameNodeInterface getNameNode(int idx) {
        LOG.warn("The getNameNode(int idx) always return the first NameNode for now.");
        System.out.println("The getNameNode(int idx) always return the first NameNode for now.");
        try {
            RemoteObject nameNode = (RemoteObject) getNNRegistry().lookup(NameNode.class.getName());
            return RemoteObjectProxy.newInstance(nameNode, NameNodeInterface.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the NameNode remote object through RMI", e);
        }
    }


    public NamenodeProtocolsInterface getNameNodeRpc() {
        return getNameNode().getRpcServer();
    }

    public FSNamesystemInterface getNamesystem() {
        return getFSNameSystem();
    }

    public FSNamesystemInterface getFSNameSystem() {
        try {
            RemoteObject fsNameSystem = (RemoteObject) getNNRegistry().lookup(FSNamesystem.class.getName());
            return RemoteObjectProxy.newInstance(fsNameSystem, FSNamesystemInterface.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the FSNameSystem remote object through RMI", e);
        }
    }

    public DataNodeInterface getDataNode(int dnIndex) {
        try {
            RemoteObject dataNode = (RemoteObject) getDNRegistry(dnIndex).lookup(DataNode.class.getName());
            return RemoteObjectProxy.newInstance(dataNode, DataNodeInterface.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the DataNode remote object through RMI", e);
        }
    }

    public ArrayList<DataNodeInterface> getDataNodes() {
        return dataNodeProxies;
    }

    public ArrayList<NameNodeInterface> getNameNodes() {
        return nameNodeProxies;
    }

    public int getNumNameNodes() {
        return nameNodeProxies.size();
    }

    public void shutdownNameNode(int idx) {
        nameNodeProxies.get(idx).stop();
    }

    public void restartNameNode(int idx) {
        LOG.warn("The restartNameNode(int idx) function is not implemented yet.");
        System.out.println("The restartNameNode(int idx) function is not implemented yet.");
        throw new UnsupportedOperationException("The restartNameNode(int idx) function is not implemented yet.");
    }

    public void restartNameNode(boolean restart) {
        if (restart) {
            restartNameNode();
        }
    }

    public void shutdownNameNodes() {
        for (NameNodeInterface nameNode : nameNodeProxies) {
            nameNode.stop();
        }
    }

    public void restartNameNodes() {
        for (int i = 0; i < nameNodeProxies.size(); i++) {
            restartNameNode(i);
        }
    }

    public void stopDataNode(int idx) {
        dataNodes.get(idx).stop();
    }

    public void stopDataNode(String addr) {
        LOG.warn("The stopDataNode(String addr) function is not implemented yet.");
        System.out.println("The stopDataNode(String addr) function is not implemented yet.");
        throw new UnsupportedOperationException("The stopDataNode(String addr) function is not implemented yet.");
    }

    public int corruptBlockOnDataNodesByDeletingBlockFile(ExtendedBlock block) {
        LOG.warn("The corruptBlockOnDataNodesByDeletingBlockFile function is not implemented yet.");
        System.out.println("The corruptBlockOnDataNodesByDeletingBlockFile function is not implemented yet.");
        throw new UnsupportedOperationException("The corruptBlockOnDataNodesByDeletingBlockFile function is not implemented yet.");
    }

    public void corruptReplica(int i, ExtendedBlock blk) {
        LOG.warn("The corruptReplica(int i, ExtendedBlock blk) function is not implemented yet.");
        System.out.println("The corruptReplica(int i, ExtendedBlock blk) function is not implemented yet.");
        throw new UnsupportedOperationException("The corruptReplica(int i, ExtendedBlock blk) function is not implemented yet.");
    }

    public void corruptReplica(DataNodeInterface dn, ExtendedBlock blk) {
        LOG.warn("The corruptReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
        System.out.println("The corruptReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
        throw new UnsupportedOperationException("The corruptReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
    }

    public void triggerHeartbeats() {
        LOG.warn("The triggerHeartbeats function is not implemented yet.");
        System.out.println("The triggerHeartbeats function is not implemented yet.");
        throw new UnsupportedOperationException("The triggerHeartbeats function is not implemented yet.");
    }

    public void triggerBlockReports() {
        LOG.warn("The triggerBlockReports function is not implemented yet.");
        System.out.println("The triggerBlockReports function is not implemented yet.");
        throw new UnsupportedOperationException("The triggerBlockReports function is not implemented yet.");
    }
}
