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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.ha.ServiceFailedException;
import org.apache.hadoop.hdfs.protocol.*;
import org.apache.hadoop.hdfs.rmi.client.RemoteObjectProxy;
import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerTestUtil;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.common.blockaliasmap.BlockAliasMap;
import org.apache.hadoop.hdfs.server.common.blockaliasmap.impl.InMemoryLevelDBAliasMapClient;
import org.apache.hadoop.hdfs.server.datanode.*;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpi;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NameNodeAdapter;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.thirdparty.com.google.common.base.Preconditions;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.hadoop.hdfs.DFSConfigKeys.*;
import static org.apache.hadoop.hdfs.client.HdfsClientConfigKeys.DFS_DATA_TRANSFER_PROTECTION_KEY;


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
    protected final int storagesPerDatanode;
    /**
     * For the Junit tests, this is the default value of the The amount of time
     * in milliseconds that the BlockScanner times out waiting for the
     * {@link VolumeScanner} thread to join during a shutdown call.
     */
    public static final long DEFAULT_SCANNER_VOLUME_JOIN_TIMEOUT_MSEC =
            TimeUnit.SECONDS.toMillis(30);

    private File base_dir;
    private File data_dir;

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
        this.storagesPerDatanode = builder.storagesPerDatanode;
        // Re-enable symlinks for tests, see HADOOP-10020 and HADOOP-10052
        FileSystem.enableSymlinks();
        base_dir = new File(determineDfsBaseDir());
        data_dir = new File(base_dir, "data");
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

    public void shutdown(boolean deleteBaseDir, boolean closeFileSystem) {
        close();
    }

    public void shutdown(boolean deleteBaseDir) {
        LOG.warn("The shutdown(boolean deleteBaseDir) function is not implemented yet.");
        System.out.println("The shutdown(boolean deleteBaseDir) function is not implemented yet.");
        close();
    }

    public String getDataDirectory() {
        return data_dir.getAbsolutePath();
    }

    public synchronized void startDataNodes(Configuration conf, int numDataNodes,
                                            boolean manageDfsDirs, HdfsServerConstants.StartupOption operation,
                                            String[] racks, String[] hosts,
                                            long[] simulatedCapacities) throws IOException {
        startDataNodes(conf, numDataNodes, manageDfsDirs, operation, racks,
                hosts, simulatedCapacities, false);
    }


    public synchronized void startDataNodes(Configuration conf, int numDataNodes,
                                            boolean manageDfsDirs, HdfsServerConstants.StartupOption operation,
                                            String[] racks, String[] hosts,
                                            long[] simulatedCapacities,
                                            boolean setupHostsFile) throws IOException {
        startDataNodes(conf, numDataNodes, null, manageDfsDirs, operation, racks, hosts, null,
                simulatedCapacities, setupHostsFile, false, false, null, null, null);
    }

    public synchronized void startDataNodes(Configuration conf, int numDataNodes,
                                            boolean manageDfsDirs, HdfsServerConstants.StartupOption operation,
                                            String[] racks, String[] hosts,
                                            long[] simulatedCapacities,
                                            boolean setupHostsFile,
                                            boolean checkDataNodeAddrConfig) throws IOException {
        startDataNodes(conf, numDataNodes, null, manageDfsDirs, operation, racks, hosts, null,
                simulatedCapacities, setupHostsFile, checkDataNodeAddrConfig, false, null, null, null);
    }

    public synchronized void startDataNodes(Configuration conf, int numDataNodes,
                                            StorageType[][] storageTypes, boolean manageDfsDirs, HdfsServerConstants.StartupOption operation,
                                            String[] racks, String[] hosts,
                                            long[][] storageCapacities,
                                            long[] simulatedCapacities,
                                            boolean setupHostsFile,
                                            boolean checkDataNodeAddrConfig,
                                            boolean checkDataNodeHostConfig,
                                            Configuration[] dnConfOverlays,
                                            int[] dnHttpPorts,
                                            int[] dnIpcPorts) throws IOException {

        LOG.warn("The startDataNodes function is not implemented yet.");
        System.out.println("The startDataNodes function is not implemented yet.");
        throw new UnsupportedOperationException("The startDataNodes function is not implemented yet.");
    }

    public void startDataNodes(Configuration conf, int numDataNodes,
                               boolean manageDfsDirs, HdfsServerConstants.StartupOption operation,
                               String[] racks
    ) throws IOException {
        startDataNodes(conf, numDataNodes, manageDfsDirs, operation, racks, null,
                null, false);
    }

    public void startDataNodes(Configuration conf, int numDataNodes,
                               boolean manageDfsDirs, HdfsServerConstants.StartupOption operation,
                               String[] racks,
                               long[] simulatedCapacities) throws IOException {
        startDataNodes(conf, numDataNodes, manageDfsDirs, operation, racks, null,
                simulatedCapacities, false);

    }

    public void shutdownDataNodes() {
        for (int i = dataNodes.size()-1; i >= 0; i--) {
            DockerNode dataNode = dataNodes.get(i);
            dataNode.stop();
            dataNodes.remove(i);
        }
    }

    public int getStoragesPerDatanode() {
        return storagesPerDatanode;
    }



    public void restartDataNode(int i, boolean b, boolean b1) {
        LOG.warn("The restartDataNode(int i, boolean b, boolean b1) function is not implemented yet.");
        System.out.println("The restartDataNode(int i, boolean b, boolean b1) function is not implemented yet.");
    }

    public boolean restartDataNodes(boolean deleteBaseDir) {
        LOG.warn("The restartDataNodes(boolean deleteBaseDir) function is not implemented yet.");
        System.out.println("The restartDataNodes(boolean deleteBaseDir) function is not implemented yet.");
        return true;
    }

    public boolean restartDataNodes() {
        LOG.warn("The restartDataNodes(boolean deleteBaseDir) function is not implemented yet.");
        System.out.println("The restartDataNodes(boolean deleteBaseDir) function is not implemented yet.");
        return true;
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

    public void waitActive(int nnIndex) throws BindException {
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

    private void checkSingleNameNode() {
        /**
        if (namenodes.size() != 1) {
            throw new IllegalArgumentException("Namenode index is needed");
        }**/
    }

    public synchronized void restartNameNode(String... args) throws IOException {
        //checkSingleNameNode();
        //restartNameNode(0, true, args);
        restartNameNode();
    }

    /**
     * Restart the namenode at a given index. Optionally wait for the cluster
     * to become active.
     */
    public synchronized void restartNameNode(int nnIndex, boolean waitActive,
                                             String... args) throws IOException {
        //checkSingleNameNode();
        restartNameNodes();
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

    public void transitionToStandby(int nnIndex) throws IOException,
            ServiceFailedException {
        getNameNode(nnIndex).getRpcServer().transitionToStandby(
                new HAServiceProtocol.StateChangeRequestInfo(HAServiceProtocol.RequestSource.REQUEST_BY_USER_FORCED));
    }

    private NameNodeInfo getNN(int nnIndex) {
        int count = 0;
        /**
        for (NameNodeInfo nn : namenodes.values()) {
            if (count == nnIndex) {
                return nn;
            }
            count++;
        }*/
        LOG.warn("The getNN(int nnIndex) always return the first NameNodeInfo for now.");
        System.out.println("The getNN(int nnIndex) always return the first NameNodeInfo for now.");
        throw new RuntimeException("The getNN(int nnIndex) always return the first NameNodeInfo for now.");
    }

    public Collection<URI> getNameDirs(int nnIndex) {
        return FSNamesystem.getNamespaceDirs(getNN(nnIndex).conf);
    }

    public NamenodeProtocols getNameNodeRpc() {
        return getNameNode().getRpcServer();
    }

    public NamenodeProtocols getNameNodeRpc(int idx) {
        return getNameNode().getRpcServer();
    }

    public FSNamesystemInterface getNamesystem() {
        return getFSNameSystem();
    }

    public FSNamesystemInterface getNamesystem(int idx) {
        LOG.warn("The getNamesystem(int idx) always return the first FSNameSystem for now.");
        System.out.println("The getNamesystem(int idx) always return the first FSNameSystem for now.");
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

    public DataNodeProperties stopDataNode(int idx) {
        dataNodes.get(idx).stop();
        return new DataNodeProperties(idx);
    }

    public synchronized DataNodeProperties stopDataNodeForUpgrade(int i)
            throws IOException {
        return stopDataNode(i);
    }

    public boolean restartDataNode(DataNodeProperties dp, Boolean keepPort ) throws IOException {
        int idx = dp.idx;
        restartDataNode(idx);
        return true;
    }

    public boolean restartDataNode(int idx, Boolean keepPort ) throws IOException {
        restartDataNode(idx);
        return true;
    }

    public boolean restartDataNode(int idx) throws IOException {
        LOG.warn("The restartDataNode(int idx) function is not implemented yet.");
        System.out.println("The restartDataNode(DataNodeProperties dnprop) function is not implemented yet.");
        throw new UnsupportedOperationException("The restartDataNode(DataNodeProperties dnprop) function is not implemented yet.");
    }

    public boolean restartDataNode(DataNodeProperties dnprop) throws IOException {
        LOG.warn("The restartDataNode(DataNodeProperties dnprop) function is not implemented yet.");
        System.out.println("The restartDataNode(DataNodeProperties dnprop) function is not implemented yet.");
        throw new UnsupportedOperationException("The restartDataNode(DataNodeProperties dnprop) function is not implemented yet.");
    }

    public boolean restartDataNode(String addr) throws IOException {
        LOG.warn("The restartDataNode(String addr) function is not implemented yet.");
        System.out.println("The restartDataNode(String addr) function is not implemented yet.");
        throw new UnsupportedOperationException("The restartDataNode(String addr) function is not implemented yet.");
    }

    public void setDataNodeDead(DatanodeIDInterface dnId) throws IOException {
        LOG.warn("The setDataNodeDead(DatanodeIDInterface dnId) function is not implemented yet.");
        System.out.println("The setDataNodeDead(DatanodeIDInterface dnId) function is not implemented yet.");
        throw new UnsupportedOperationException("The setDataNodeDead(DatanodeIDInterface dnId) function is not implemented yet.");
    }

    public void setDataNodeDead(DatanodeDescriptorInterface dnId) throws IOException {
        LOG.warn("The setDataNodeDead(DatanodeDescriptorInterface dnId) function is not implemented yet.");
        System.out.println("The setDataNodeDead(DatanodeDescriptorInterface dnId) function is not implemented yet.");
        throw new UnsupportedOperationException("The setDataNodeDead(DatanodeDescriptorInterface dnId) function is not implemented yet.");
    }

    public File getInstanceStorageDir(int dnIndex, int dirIndex) {
        return new File(base_dir, getStorageDirPath(dnIndex, dirIndex));
    }

    public boolean isClusterUp() {
        return nameNode.isHealthy();
    }


    public void setLeasePeriod(long soft, long hard) {
        NameNodeAdapter.setLeasePeriod(getNamesystem(), soft, hard);
    }

    public void setLeasePeriod(long soft, long hard, int nnIndex) {
        NameNodeAdapter.setLeasePeriod(getNamesystem(nnIndex), soft, hard);
    }


    public static List<File> getAllBlockFiles(File storageDir) {
        List<File> results = new ArrayList<File>();
        File[] files = storageDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f.getName().startsWith(Block.BLOCK_FILE_PREFIX) &&
                    !f.getName().endsWith(Block.METADATA_EXTENSION)) {
                results.add(f);
            } else if (f.isDirectory()) {
                List<File> subdirResults = getAllBlockFiles(f);
                if (subdirResults != null) {
                    results.addAll(subdirResults);
                }
            }
        }
        return results;
    }

    public static String getDNCurrentDir(File storageDir) {
        return storageDir + "/" + Storage.STORAGE_DIR_CURRENT + "/";
    }

    public static String getBPDir(File storageDir, String bpid) {
        return getDNCurrentDir(storageDir) + bpid + "/";
    }

    public static String getBPDir(File storageDir, String bpid, String dirName) {
        return getBPDir(storageDir, bpid) + dirName + "/";
    }

    public static File getFinalizedDir(File storageDir, String bpid) {
        return new File(getBPDir(storageDir, bpid, Storage.STORAGE_DIR_CURRENT)
                + DataStorage.STORAGE_DIR_FINALIZED );
    }


    public static File getBlockFile(File storageDir, ExtendedBlock blk) {
        return new File(DatanodeUtil.idToBlockDir(getFinalizedDir(storageDir,
                blk.getBlockPoolId()), blk.getBlockId()), blk.getBlockName());
    }

    /**
     * Get all files related to a block from all the datanodes
     * @param block block for which corresponding files are needed
     */
    public File[] getAllBlockFiles(ExtendedBlock block) {
        if (dataNodes.size() == 0) return new File[0];
        ArrayList<File> list = new ArrayList<File>();
        for (int i=0; i < dataNodes.size(); i++) {
            File blockFile = getBlockFile(i, block);
            if (blockFile != null) {
                list.add(blockFile);
            }
        }
        return list.toArray(new File[list.size()]);
    }

    /**
     * Get the block data file for a block from a given datanode
     * @param dnIndex Index of the datanode to get block files for
     * @param block block for which corresponding files are needed
     */
    public File getBlockFile(int dnIndex, ExtendedBlock block) {
        // Check for block file in the two storage directories of the datanode
        for (int i = 0; i <=1 ; i++) {
            File storageDir = getStorageDir(dnIndex, i);
            File blockFile = getBlockFile(storageDir, block);
            if (blockFile.exists()) {
                return blockFile;
            }
        }
        return null;
    }

    public static String getBaseDirectory() {
        return GenericTestUtils.getTestDir("dfs").getAbsolutePath()
                + File.separator;
    }

    protected String determineDfsBaseDir() {
        if (conf != null) {
            final String dfsdir = conf.get(HDFS_MINIDFS_BASEDIR, null);
            if (dfsdir != null) {
                return dfsdir;
            }
        }
        return getBaseDirectory();
    }

    private String getStorageDirPath(int dnIndex, int dirIndex) {
        return "data/data" + (storagesPerDatanode * dnIndex + 1 + dirIndex);
    }

    public File getStorageDir(int dnIndex, int dirIndex) {
        return new File(determineDfsBaseDir(),
                getStorageDirPath(dnIndex, dirIndex));
    }

    public synchronized DataNodeProperties stopDataNode(String addr) {
        LOG.warn("The stopDataNode(String addr) function is not implemented yet.");
        System.out.println("The stopDataNode(String addr) function is not implemented yet.");
        throw new UnsupportedOperationException("The stopDataNode(String addr) function is not implemented yet.");
    }

    public int corruptBlockOnDataNodesByDeletingBlockFile(ExtendedBlock block) {
        LOG.warn("The corruptBlockOnDataNodesByDeletingBlockFile function is not implemented yet.");
        System.out.println("The corruptBlockOnDataNodesByDeletingBlockFile function is not implemented yet.");
        throw new UnsupportedOperationException("The corruptBlockOnDataNodesByDeletingBlockFile function is not implemented yet.");
    }

    public int corruptBlockOnDataNodes(ExtendedBlock block) {
        LOG.warn("The corruptBlockOnDataNodes function is not implemented yet.");
        System.out.println("The corruptBlockOnDataNodes function is not implemented yet.");
        throw new UnsupportedOperationException("The corruptBlockOnDataNodes function is not implemented yet.");
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

    /** Wait until the given namenode gets first block reports from all the datanodes */
    public void waitFirstBRCompleted(int nnIndex, int timeout) throws
            IOException, TimeoutException, InterruptedException {
        if (nameNodeProxies.size() == 0) {// || getNN(nnIndex) == null || getNN(nnIndex).nameNode == null) {
            return;
        }

        final FSNamesystemInterface ns = getNamesystem(nnIndex);
        final DatanodeManagerInterface dm = ns.getBlockManager().getDatanodeManager();
        GenericTestUtils.waitFor(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                List<DatanodeDescriptorInterface> nodes = dm.getDatanodeListForReport
                        (HdfsConstants.DatanodeReportType.LIVE);
                for (DatanodeDescriptorInterface node : nodes) {
                    if (!node.checkBlockReportReceived()) {
                        return false;
                    }
                }
                return true;
            }
        }, 100, timeout);
    }

    /**
     * Get materialized replica that can be corrupted later.
     * @param i the index of DataNode.
     * @param blk name of the block.
     * @return a materialized replica.
     * @throws ReplicaNotFoundException if the replica does not exist on the
     * DataNode.
     */
    public FsDatasetTestUtils.MaterializedReplica getMaterializedReplica(
            int i, ExtendedBlockInterface blk) throws ReplicaNotFoundException {
        LOG.warn("The getMaterializedReplica(int i, ExtendedBlock blk) function is not implemented yet.");
        System.out.println("The getMaterializedReplica(int i, ExtendedBlock blk) function is not implemented yet.");
        throw new UnsupportedOperationException("The getMaterializedReplica(int i, ExtendedBlock blk) function is not implemented yet.");
    }

    public FsDatasetTestUtils.MaterializedReplica getMaterializedReplica(
            int i, ExtendedBlock blk) throws ReplicaNotFoundException {
        LOG.warn("The getMaterializedReplica(int i, ExtendedBlock blk) function is not implemented yet.");
        System.out.println("The getMaterializedReplica(int i, ExtendedBlock blk) function is not implemented yet.");
        throw new UnsupportedOperationException("The getMaterializedReplica(int i, ExtendedBlock blk) function is not implemented yet.");
    }

    /**
     * Get materialized replica that can be corrupted later.
     * @param dn the index of DataNode.
     * @param blk name of the block.
     * @return a materialized replica.
     * @throws ReplicaNotFoundException if the replica does not exist on the
     * DataNode.
     */
    public FsDatasetTestUtils.MaterializedReplica getMaterializedReplica(
            DataNodeInterface dn, ExtendedBlockInterface blk) throws ReplicaNotFoundException {
        LOG.warn("The getMaterializedReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
        System.out.println("The getMaterializedReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
        throw new UnsupportedOperationException("The getMaterializedReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
    }

    public FsDatasetTestUtils.MaterializedReplica getMaterializedReplica(
            DataNodeInterface dn, ExtendedBlock blk) throws ReplicaNotFoundException {
        LOG.warn("The getMaterializedReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
        System.out.println("The getMaterializedReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
        throw new UnsupportedOperationException("The getMaterializedReplica(DataNode dn, ExtendedBlock blk) function is not implemented yet.");
    }


    public FsDatasetTestUtils getFsDatasetTestUtils(int dnIdx) {
        Preconditions.checkArgument(dnIdx < dataNodes.size());
        return FsDatasetTestUtils.Factory.getFactory(conf)
                .newInstance(dataNodeProxies.get(dnIdx));
    }

    /**
     * Returns the corresponding FsDatasetTestUtils for a DataNode.
     * @param dn a DataNode
     * @return a FsDatasetTestUtils for the given DataNode.
     */
    public FsDatasetTestUtils getFsDatasetTestUtils(DataNodeInterface dn) {
        Preconditions.checkArgument(dn != null);
        return FsDatasetTestUtils.Factory.getFactory(conf)
                .newInstance(dn);
    }



    /**
     * Returns the current set of datanodes
     */
    DataNodeInterface[] listDataNodes() {
        return dataNodeProxies.toArray(new DataNodeInterface[0]);
    }

    public static class DataNodeProperties {
        final DataNodeInterface datanode;
        final Configuration conf;
        String[] dnArgs;
        final SecureDataNodeStarter.SecureResources secureResources;
        final int ipcPort;
        public int idx;

        DataNodeProperties(int idx) {
            this(null, null, null, null, 0, idx);
        }

        DataNodeProperties(DataNodeInterface node, Configuration conf, String[] args,
                           SecureDataNodeStarter.SecureResources secureResources, int ipcPort, int idx) {
            this.datanode = node;
            this.conf = conf;
            this.dnArgs = args;
            this.secureResources = secureResources;
            this.ipcPort = ipcPort;
            this.idx = idx;
        }

        public void setDnArgs(String ... args) {
            dnArgs = args;
        }

        public DataNodeInterface getDatanode() {
            return datanode;
        }

    }

    /**
     * Stores the information related to a namenode in the cluster
     */
    public static class NameNodeInfo {
        public NameNodeInterface nameNode;
        Configuration conf;
        String nameserviceId;
        String nnId;
        HdfsServerConstants.StartupOption startOpt;
        NameNodeInfo(NameNodeInterface nn, String nameserviceId, String nnId,
                     HdfsServerConstants.StartupOption startOpt, Configuration conf) {
            this.nameNode = nn;
            this.nameserviceId = nameserviceId;
            this.nnId = nnId;
            this.startOpt = startOpt;
            this.conf = conf;
        }

        public void setStartOpt(HdfsServerConstants.StartupOption startOpt) {
            this.startOpt = startOpt;
        }

        public String getNameserviceId() {
            return this.nameserviceId;
        }

        public String getNamenodeId() {
            return this.nnId;
        }
    }

    public void corruptMeta(int i, ExtendedBlock blk) throws IOException {
        getMaterializedReplica(i, blk).corruptMeta();
    }

    public void deleteMeta(int i, ExtendedBlock blk)
            throws IOException {
        getMaterializedReplica(i, blk).deleteMeta();
    }

    public void truncateMeta(int i, ExtendedBlock blk, int newSize)
            throws IOException {
        getMaterializedReplica(i, blk).truncateMeta(newSize);
    }

    /**
     * Finalize cluster for the namenode at the given index
     * @see MiniDFSCluster#finalizeCluster(Configuration)
     * @param nnIndex index of the namenode
     * @param conf configuration
     * @throws Exception
     */
    public void finalizeCluster(int nnIndex, Configuration conf) throws Exception {
        //finalizeNamenode(getNN(nnIndex).nameNode, getNN(nnIndex).conf);
    }

    /**
     * If the NameNode is running, attempt to finalize a previous upgrade.
     * When this method return, the NameNode should be finalized, but
     * DataNodes may not be since that occurs asynchronously.
     *
     * @throws IllegalStateException if the Namenode is not running.
     */
    public void finalizeCluster(Configuration conf) throws Exception {
        /*
        for (NameNodeInfo nnInfo : namenodes.values()) {
            if (nnInfo == null) {
                throw new IllegalStateException("Attempting to finalize "
                        + "Namenode but it is not running");
            }
            finalizeNamenode(nnInfo.nameNode, nnInfo.conf);
        }
         */
    }

    public List<Map<DatanodeStorage, BlockListAsLongs>> getAllBlockReports(String bpid) {
        /*
        int numDataNodes = dataNodes.size();
        final List<Map<DatanodeStorage, BlockListAsLongs>> result
                = new ArrayList<Map<DatanodeStorage, BlockListAsLongs>>(numDataNodes);
        for (int i = 0; i < numDataNodes; ++i) {
            result.add(getBlockReport(bpid, i));
        }
        return result;
         */
        LOG.warn("The getAllBlockReports function is not implemented yet.");
        System.out.println("The getAllBlockReports function is not implemented yet.");
        throw new UnsupportedOperationException("The getAllBlockReports function is not implemented yet.");
    }

    /**
     * Returns true if there is at least one DataNode running.
     */
    public boolean isDataNodeUp() {
        if (dataNodes.isEmpty()) {
            return false;
        }
        for (DockerNode dn : dataNodes.values()) {
            if (dn.isHealthy()) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is valid only if the data nodes have simulated data
     * @param dataNodeIndex - data node i which to inject - the index is same as for getDataNodes()
     * @param blocksToInject - the blocks
     * @param bpid - (optional) the block pool id to use for injecting blocks.
     *             If not supplied then it is queried from the in-process NameNode.
     * @throws IOException
     *              if not simulatedFSDataset
     *             if any of blocks already exist in the data node
     *
     */
    public void injectBlocks(int dataNodeIndex,
                             Iterable<Block> blocksToInject, String bpid) throws IOException {
        if (dataNodeIndex < 0 || dataNodeIndex > dataNodes.size()) {
            throw new IndexOutOfBoundsException();
        }
        final DataNodeInterface dn = dataNodeProxies.get(dataNodeIndex);
        final FsDatasetSpi<?> dataSet = DataNodeTestUtils.getFSDataset(dn);
        if (!(dataSet instanceof SimulatedFSDataset)) {
            throw new IOException("injectBlocks is valid only for" +
                    " SimulatedFSDataset");
        }
        if (bpid == null) {
            bpid = getNamesystem().getBlockPoolId();
        }
        SimulatedFSDataset sdataset = (SimulatedFSDataset) dataSet;
        sdataset.injectBlocks(bpid, blocksToInject);
        dataNodeProxies.get(dataNodeIndex).scheduleAllBlockReport(0);
    }

    /**
     * Multiple-NameNode version of injectBlocks.
     */
    public void injectBlocks(int nameNodeIndex, int dataNodeIndex,
                             Iterable<Block> blocksToInject) throws IOException {
        if (dataNodeIndex < 0 || dataNodeIndex > dataNodes.size()) {
            throw new IndexOutOfBoundsException();
        }
        final DataNodeInterface dn = dataNodeProxies.get(dataNodeIndex);
        final FsDatasetSpi<?> dataSet = DataNodeTestUtils.getFSDataset(dn);
        if (!(dataSet instanceof SimulatedFSDataset)) {
            throw new IOException("injectBlocks is valid only for" +
                    " SimulatedFSDataset");
        }
        String bpid = getNamesystem(nameNodeIndex).getBlockPoolId();
        SimulatedFSDataset sdataset = (SimulatedFSDataset) dataSet;
        sdataset.injectBlocks(bpid, blocksToInject);
        dataNodeProxies.get(dataNodeIndex).scheduleAllBlockReport(0);
    }

    private File[] getNameNodeDirectory(int nameserviceIndex, int nnIndex) {
        return getNameNodeDirectory(base_dir, nameserviceIndex, nnIndex);
    }

    public static File[] getNameNodeDirectory(String base_dir, int nsIndex, int nnIndex) {
        return getNameNodeDirectory(new File(base_dir), nsIndex, nnIndex);
    }

    public static File[] getNameNodeDirectory(File base_dir, int nsIndex, int nnIndex) {
        File[] files = new File[2];
        files[0] = new File(base_dir, "name-" + nsIndex + "-" + (2 * nnIndex + 1));
        files[1] = new File(base_dir, "name-" + nsIndex + "-" + (2 * nnIndex + 2));
        return files;
    }

    /**
     * Get the latest metadata file correpsonding to a block
     * @param storageDir storage directory
     * @param blk the block
     * @return metadata file corresponding to the block
     */
    public static File getBlockMetadataFile(File storageDir, ExtendedBlock blk) {
        return new File(DatanodeUtil.idToBlockDir(getFinalizedDir(storageDir,
                blk.getBlockPoolId()), blk.getBlockId()), blk.getBlockName() + "_" +
                blk.getGenerationStamp() + Block.METADATA_EXTENSION);
    }

    /**
     * Get the block metadata file for a block from a given datanode
     *
     * @param dnIndex Index of the datanode to get block files for
     * @param block block for which corresponding files are needed
     */
    public File getBlockMetadataFile(int dnIndex, ExtendedBlock block) {
        // Check for block file in the two storage directories of the datanode
        for (int i = 0; i <=1 ; i++) {
            File storageDir = getStorageDir(dnIndex, i);
            File blockMetaFile = getBlockMetadataFile(storageDir, block);
            if (blockMetaFile.exists()) {
                return blockMetaFile;
            }
        }
        return null;
    }

    public static void setupKerberosConfiguration(Configuration conf,
                                                  String userName, String realm, String keytab, String keystoresDir,
                                                  String sslConfDir) throws Exception {
        // Windows will not reverse name lookup "127.0.0.1" to "localhost".
        String krbInstance = Path.WINDOWS ? "127.0.0.1" : "localhost";
        String hdfsPrincipal = userName + "/" + krbInstance + "@" + realm;
        String spnegoPrincipal = "HTTP/" + krbInstance + "@" + realm;

        conf.set(DFS_NAMENODE_KERBEROS_PRINCIPAL_KEY, hdfsPrincipal);
        conf.set(DFS_NAMENODE_KEYTAB_FILE_KEY, keytab);
        conf.set(DFS_DATANODE_KERBEROS_PRINCIPAL_KEY, hdfsPrincipal);
        conf.set(DFS_DATANODE_KEYTAB_FILE_KEY, keytab);
        conf.set(DFS_WEB_AUTHENTICATION_KERBEROS_PRINCIPAL_KEY, spnegoPrincipal);
        conf.setBoolean(DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);
        conf.set(DFS_DATA_TRANSFER_PROTECTION_KEY, "authentication");

        conf.set(DFS_HTTP_POLICY_KEY, HttpConfig.Policy.HTTPS_ONLY.name());
        conf.set(DFS_NAMENODE_HTTPS_ADDRESS_KEY, "localhost:0");
        conf.set(DFS_DATANODE_HTTPS_ADDRESS_KEY, "localhost:0");
        conf.set(DFS_JOURNALNODE_HTTPS_ADDRESS_KEY, "localhost:0");
        conf.setInt(IPC_CLIENT_CONNECT_MAX_RETRIES_ON_SASL_KEY, 10);

        KeyStoreTestUtil.setupSSLConfig(keystoresDir, sslConfDir, conf, false);
        conf.set(DFS_CLIENT_HTTPS_KEYSTORE_RESOURCE_KEY,
                KeyStoreTestUtil.getClientSSLConfigFileName());
        conf.set(DFS_SERVER_HTTPS_KEYSTORE_RESOURCE_KEY,
                KeyStoreTestUtil.getServerSSLConfigFileName());

        KeyStoreTestUtil.setupSSLConfig(keystoresDir, sslConfDir, conf, false);
        conf.set(DFS_CLIENT_HTTPS_KEYSTORE_RESOURCE_KEY,
                KeyStoreTestUtil.getClientSSLConfigFileName());
        conf.set(DFS_SERVER_HTTPS_KEYSTORE_RESOURCE_KEY,
                KeyStoreTestUtil.getServerSSLConfigFileName());
    }

    /**
     * Setup the namenode-level PROVIDED configurations, using the
     * {@link InMemoryLevelDBAliasMapClient}.
     *
     * @param conf Configuration, which is modified, to enable provided storage.
     *        This cannot be null.
     */
    public static void setupNamenodeProvidedConfiguration(Configuration conf) {
        conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_PROVIDED_ENABLED, true);
        conf.setBoolean(DFSConfigKeys.DFS_PROVIDED_ALIASMAP_INMEMORY_ENABLED, true);
        conf.setClass(DFSConfigKeys.DFS_PROVIDED_ALIASMAP_CLASS,
                InMemoryLevelDBAliasMapClient.class, BlockAliasMap.class);
        File tempDirectory = new File(GenericTestUtils.getRandomizedTestDir(),
                "in-memory-alias-map");
        conf.set(DFSConfigKeys.DFS_PROVIDED_ALIASMAP_INMEMORY_LEVELDB_DIR,
                tempDirectory.getAbsolutePath());
        conf.setInt(DFSConfigKeys.DFS_PROVIDED_ALIASMAP_LOAD_RETRIES, 10);
        conf.set(DFSConfigKeys.DFS_PROVIDED_ALIASMAP_LEVELDB_PATH,
                tempDirectory.getAbsolutePath());
    }

    public void triggerDeletionReports()
            throws IOException {
        for (DataNodeInterface dn : getDataNodes()) {
            DataNodeTestUtils.triggerDeletionReport(dn);
        }
    }

    /**
     * @return a http URL
     */
    public String getHttpUri(int nnIndex) {
        return "http://"
                + getNN(nnIndex).conf
                .get(DFS_NAMENODE_HTTP_ADDRESS_KEY);
    }

    /**
     * Get finalized directory for a block pool
     * @param storageDir storage directory
     * @param bpid Block pool Id
     * @return finalized directory for a block pool
     */
    public static File getRbwDir(File storageDir, String bpid) {
        return new File(getBPDir(storageDir, bpid, Storage.STORAGE_DIR_CURRENT)
                + DataStorage.STORAGE_DIR_RBW );
    }

    public String readBlockOnDataNode(int i, ExtendedBlock block)
            throws IOException {
        assert (i >= 0 && i < dataNodes.size()) : "Invalid datanode "+i;
        File blockFile = getBlockFile(i, block);
        if (blockFile != null && blockFile.exists()) {
            return DFSTestUtil.readFile(blockFile);
        }
        return null;
    }

    public byte[] readBlockOnDataNodeAsBytes(int i, ExtendedBlock block)
            throws IOException {
        assert (i >= 0 && i < dataNodes.size()) : "Invalid datanode "+i;
        File blockFile = getBlockFile(i, block);
        if (blockFile != null && blockFile.exists()) {
            return DFSTestUtil.readFileAsBytes(blockFile);
        }
        return null;
    }

    public NameNodeInfo[] getNameNodeInfos() {
        LOG.warn("The getNameNodeInfos() function is not implemented yet.");
        System.out.println("The getNameNodeInfos() function is not implemented yet.");
        throw new UnsupportedOperationException("The getNameNodeInfos() function is not implemented yet.");
    }

    /**
     * @param nsIndex index of the namespace id to check
     * @return all the namenodes bound to the given namespace index
     */
    public NameNodeInfo[] getNameNodeInfos(int nsIndex) {
        LOG.warn("The getNameNodeInfos(int nsIndex) function is not implemented yet.");
        System.out.println("The getNameNodeInfos(int nsIndex) function is not implemented yet.");
        throw new UnsupportedOperationException("The getNameNodeInfos(int nsIndex) function is not implemented yet.");
    }

    /**
     * @param nameservice id of nameservice to read
     * @return all the namenodes bound to the given namespace index
     */
    public NameNodeInfo[] getNameNodeInfos(String nameservice) {
        LOG.warn("The getNameNodeInfos(String nameservice) function is not implemented yet.");
        System.out.println("The getNameNodeInfos(String nameservice) function is not implemented yet.");
        throw new UnsupportedOperationException("The getNameNodeInfos(String nameservice) function is not implemented yet.");
    }
}
