package org.apache.hadoop.hdfs;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
import edu.illinois.instance.InstanceTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationJVMInterface;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.datanode.DataNodeInstance;
import org.apache.hadoop.hdfs.server.datanode.DataNodeJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.FSImageJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystemJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.NameNodeInstance;
import org.apache.hadoop.hdfs.server.namenode.NameNodeJVMInterface;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Author: Shuai Wang
 */
public class TestUpgtDemo {


    @Test
    public void testUpgradeFinalize() throws IOException {
        Configuration conf = new Configuration();
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(2).build();
        cluster.upgradeAllNameNodes();
        cluster.shutdown();
    }


    @Test
    public void testFileSystemUpgradeDNInJVM() throws IOException {
        Configuration conf2 = new HdfsConfiguration();
        MiniDFSClusterInJVM cluster2 = new MiniDFSClusterInJVM.Builder(conf2).numDataNodes(2).build();
        cluster2.upgradeAllDataNodes(true);
        FileSystem fs2 = FileSystem.get(conf2);
        Path TEST_ROOT2 = new Path("/TestUpgtDemo-ROOT2");
        fs2.mkdirs(TEST_ROOT2);
        System.out.println(conf2.get("fs.defaultFS"));
        cluster2.shutdown();
    }


    @Test
    public void testFileSystemUpgradeNNInJVM() throws IOException {
        Configuration conf2 = new HdfsConfiguration();
        MiniDFSClusterInJVM cluster2 = new MiniDFSClusterInJVM.Builder(conf2).numDataNodes(2).build();
        cluster2.upgradeAllNameNodes();
        FileSystem fs2 = FileSystem.get(conf2);
        Path TEST_ROOT2 = new Path("/TestUpgtDemo-ROOT2");
        fs2.mkdirs(TEST_ROOT2);
        System.out.println(conf2.get("fs.defaultFS"));
        cluster2.shutdown();
    }

    @Test
    public void testUpgradeFinalizeHA() throws IOException {

        int maxNNCount = 3;
        int STARTING_PORT = 20000;
        Configuration conf = new Configuration();

        MiniDFSNNTopology.NSConf nameservice = new MiniDFSNNTopology.NSConf("ns1");
        for (int i = 0; i < maxNNCount; i++) {
            nameservice.addNN(new MiniDFSNNTopology.NNConf("nn" + i).setHttpPort(STARTING_PORT + i + 1));
        }

        MiniDFSNNTopology topology = new MiniDFSNNTopology().addNameservice(nameservice);

        MiniDFSClusterInJVM cluster2 = new MiniDFSClusterInJVM.Builder(conf)
                .nnTopology(topology)
                .numDataNodes(0)
                .build();
        cluster2.waitActive();
        cluster2.transitionToActive(0);

        cluster2.upgradeAllNameNodes();
        cluster2.transitionToStandby(0);
        cluster2.transitionToActive(1); // NN1 will be active and upgrade the layoutVersion in VERSION file
        cluster2.transitionToStandby(1);
        cluster2.transitionToActive(2); // NN2 will be active and upgrade the layoutVersion in VERSION file
        cluster2.shutdown();
    }


    @Test
    public void testUpgrade() throws IOException {
        Configuration conf2 = new HdfsConfiguration();
        MiniDFSClusterInJVM cluster2 = new MiniDFSClusterInJVM.Builder(conf2).numDataNodes(2).build();
        System.out.println("Start restarting!!");
        cluster2.restartNodeForTesting(0);
        System.out.println("Finish restarting!!");

        System.out.println("Start upgrading!!");
        cluster2.upgradeNodeForTesting(0);
        System.out.println("Finish upgrading!!");

        
        FileSystem fs2 = FileSystem.get(conf2);
        Path TEST_ROOT2 = new Path("/TestUpgtDemo-ROOT2");
        fs2.mkdirs(TEST_ROOT2);
        System.out.println(conf2.get("fs.defaultFS"));
        cluster2.shutdown();
    }

    @Test
    public void testFileSystem() throws IOException {
        Configuration conf = new HdfsConfiguration();
        MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
        FileSystem fs = FileSystem.get(conf);
        Path TEST_ROOT = new Path("/TestUpgtDemo-ROOT1");
        fs.mkdirs(TEST_ROOT);
        System.out.println(conf.get("fs.defaultFS"));
        cluster.shutdown();
    }

    @Test
    public void testFileSystemInJVM() throws IOException {
        Configuration conf2 = new HdfsConfiguration();
        MiniDFSClusterInJVM cluster2 = new MiniDFSClusterInJVM.Builder(conf2).numDataNodes(2).build();
        System.out.println("Start restarting!!");
        cluster2.restartNodeForTesting(0);
        System.out.println("Finish restarting!!");
        FileSystem fs2 = FileSystem.get(conf2);
        Path TEST_ROOT2 = new Path("/TestUpgtDemo-ROOT2");
        fs2.mkdirs(TEST_ROOT2);
        System.out.println(conf2.get("fs.defaultFS"));
        cluster2.shutdown();
    }


    // Write a test file after verification
    public void writeTestFile(MiniDFSClusterInJVM cluster) throws IOException {
        DistributedFileSystem fs = cluster.getFileSystem();
        Path filePath = new Path("/testFile.dat");

        try (FSDataOutputStream out = fs.create(filePath)) {
            out.write("Hello, HDFS!".getBytes());
            out.hflush();
            System.out.println("✅ Successfully wrote file: " + filePath);
        }
        // Verify the file
        try (FSDataInputStream in = fs.open(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            String content = new String(buffer, 0, bytesRead);
            assertEquals("Hello, HDFS!", content);
            System.out.println("✅ Successfully verified file: " + filePath);
        }
    }


    //@Test
    public void testFSNamesystemFromMiniClusterInJVM() throws IOException {
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(new Configuration()).numDataNodes(1).build();
        cluster.restartDataNodeForTesting(0, true);
        FSNamesystemJVMInterface fsNamesystem = cluster.getNamesystem();
        System.out.println("FSNamesystem is loaded by " + fsNamesystem.getClass().getClassLoader());
        System.out.println("FSNamesystem is " + fsNamesystem);
        System.out.println("FSNamesysten HaEnabled is: " + fsNamesystem.isHaEnabled());
    }


    //@Test
    public void testInstances() {
        NameNodeInstance nameNodeInstance = new NameNodeInstance();
        DataNodeInstance dataNodeInstance = new DataNodeInstance();

        NameNodeJVMInterface nameNode = nameNodeInstance.createNameNode(new String[]{});
        DataNodeJVMInterface dataNode = dataNodeInstance.createDataNode();
        System.out.println(InstanceTable.printString());
        System.out.println("NameNode address: " + nameNode.getNameNodeAddress());
        System.out.println("DataNode address: " + dataNode.getXferAddress());

    }

    //@Test
    public void testNameNodeCreation() throws Exception {
        NameNodeJVMInterface nn = MiniDFSClusterHelper.createNameNode(null);

        FSImageJVMInterface fsImage = nn.getFSImage();
        System.out.println("FSImage is loaded by " + fsImage.getClass().getClassLoader());
        System.out.println("FSImageInterface is loaded by " + FSImageJVMInterface.class.getClassLoader());
        //System.out.println(fsImage.getBlockPoolID());
        DataNodeJVMInterface dn = MiniDFSClusterHelper.createDataNode();

        InetSocketAddress addr = nn.getServiceRpcAddress();
        assert addr.getPort() != 0;
        System.out.println("DN port is : " + dn.getIpcPort());
        //System.out.println("FSImage block ID is " + fsImage.getBlockPoolID());

        MiniDFSClusterHelper.createFileWithDFS();

        //createFileWithDFS(conf, nn.getClass().getClassLoader());
        //MiniDFSClusterHelper.createFileWithDFS();
        //System.out.println(nn.getFSImage().getClusterID());
    }


    //@Test
    public void testMiniCluster() throws IOException {
        //MiniDFSCluster cluster = new MiniDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
        //System.out.println(cluster.getNameNodeInterface(0).getClientNamenodeAddress());
    }

    //@Test
    public void testNamenode() {

        VersionSelector versionSelector = new VersionSelector();

        String classPath = System.getProperty("java.class.path");
        String[] versions = {"3.5.0-SNAPSHOT"};

        //print the classpath from the thread context class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("Classpath from the thread context class loader: " + cl);

        for (String version : versions) {
            File hcommonJar = versionSelector.getJarByVersion("hadoop-common", version);
            File hdfsJar = versionSelector.getJarByVersion("hadoop-hdfs", version);

            try {
                System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
                //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

                VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
                versionClassLoader.setCurrentThreadClassLoader();
                Class<?> configClass = versionClassLoader.loadClass("org.apache.hadoop.conf.Configuration");

                Constructor<?>[] configConstructors = configClass.getConstructors();
                // get the first constructor
                Constructor<?> configConstructor = null;
                for (Constructor<?> constructor : configConstructors) {
                    if (constructor.getParameterCount() == 0) {
                        configConstructor = constructor;
                        break;
                    }
                }

                // create an instance of Configuration
                assert configConstructor != null;
                ConfigurationJVMInterface conf = (ConfigurationJVMInterface) configConstructor.newInstance();
                //conf.set("hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
                // call conf.set function with key and value
                //Method setMethod = configClass.getMethod("set", String.class, String.class);
                //setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
                //NameNode nameNode = new NameNode(conf);
                conf.set("fs.defaultFS", "hdfs://localhost:9000");
                //
                //NameNode namenode = new NameNode(conf);


                // Reset MetricsSystem
                DefaultMetricsSystem.shutdown();
                DefaultMetricsSystem.initialize("NameNode");

                Class<?> nameNodeClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.namenode.NameNode");


                //Object nameNodeInstance = nameNodeClass.getMethod("createNameNode", String[].class, Configuration.class).invoke(null, args, hdfsConf);
                Method formatMethod = nameNodeClass.getMethod("format", configClass);
                formatMethod.invoke(null, conf); // Assuming the format method is static

                Class<?> DefaultMetricsSystemClass = versionClassLoader.loadClass("org.apache.hadoop.metrics2.lib.DefaultMetricsSystem");
                // call DefaultMetricsSystem.setMiniClusterMode(true) with reflection
                Method setMiniClusterModeMethod = DefaultMetricsSystemClass.getMethod("setMiniClusterMode", boolean.class);
                setMiniClusterModeMethod.invoke(null, true);


                Method createNameNodeMethod = nameNodeClass.getMethod("createNameNode", String[].class, configClass);
                NameNodeJVMInterface nameNodeInstance = (NameNodeJVMInterface) createNameNodeMethod.invoke(null, new String[]{}, conf);


                //Constructor<?> nameNodeConstructor = nameNodeClass.getConstructor(configClass); // Use the same Configuration class
                //NameNodeInterface nameNodeInstance = (NameNodeInterface) nameNodeConstructor.newInstance(conf); // Pass the Configuration object


                String s = nameNodeInstance.getClientNamenodeAddress();

                System.out.println("Client namenode address: " + s);

                System.out.println("nameNodeInstance object is loaded by class loader: " + nameNodeInstance.getClass().getClassLoader());
                System.out.println("NameNodeInterface class is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
                System.out.println("nameNodeClass Object is loaded by class loader: " + nameNodeClass.getClassLoader());
                System.out.println("Configuration Object is loaded by class loader: " + conf.getClass().getClassLoader());
                System.out.println("ConfigClass is loaded by class loader: " + configClass.getClassLoader());
                System.out.println("ConfigurationInterface is loaded by class loader: " + ConfigurationJVMInterface.class.getClassLoader());

                InetSocketAddress net = nameNodeInstance.getNameNodeAddress();
                System.out.println("NameNode address: " + net.getHostName() + ":" + net.getPort());

                //FSImageInterface fsImage = nameNodeInstance.getFSImage();
                //System.out.println("FSImage block ID is " + fsImage.getBlockPoolID());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //@Test
    public void testCreateCluster() throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, IOException {
        createCluster(4);
    }

    public void createCluster(int numNode) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, IOException {
        VersionSelector versionSelector = new VersionSelector();

        String classPath = System.getProperty("java.class.path");
        String[] versions = {"3.5.0-SNAPSHOT"};
        for (String version : versions) {
            File hcommonJar = versionSelector.getJarByVersion("hadoop-common", version);
            File hdfsJar = versionSelector.getJarByVersion("hadoop-hdfs", version);

            VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
            versionClassLoader.setCurrentThreadClassLoader();
            Class<?> configClass = versionClassLoader.loadClass("org.apache.hadoop.conf.Configuration");
            Class<?> builderClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.MiniDFSCluster$Builder");
            Class<?> miniClusterClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.MiniDFSCluster");

            // location of the configuration class
            System.out.println("Load class org.apache.hadoop.conf.Configuration from: " + configClass.getProtectionDomain().getCodeSource().getLocation());
            System.out.println("Class Loader of configClass is : " + configClass.getClassLoader());
            System.out.println("Load class org.apache.hadoop.hdfs.MiniDFSCluster from: " + miniClusterClass.getProtectionDomain().getCodeSource().getLocation());
            System.out.println("Load class org.apache.hadoop.hdfs.MiniDFSCluster$Builder from: " + builderClass.getProtectionDomain().getCodeSource().getLocation());

            Constructor<?>[] configConstructors = configClass.getConstructors();
            // get the first constructor
            Constructor<?> configConstructor = null;
            for (Constructor<?> constructor : configConstructors) {
                if (constructor.getParameterCount() == 0) {
                    configConstructor = constructor;
                    break;
                }
            }

            // create an instance of Configuration
            assert configConstructor != null;
            Object conf = configConstructor.newInstance();
            // call conf.set function with key and value
            Method setMethod = configClass.getMethod("set", String.class, String.class);
            setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");


            // list all constructors from builderClass
            System.out.println("Printing constructors from builderClass:");
            Constructor<?>[] constructors = builderClass.getConstructors();
            Constructor<?> builderConstructor = null;
            // get the constructor with one parameter
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == 1) {
                    builderConstructor = constructor;
                    break;
                }
            }

            assert builderConstructor != null;
            Object builder = builderConstructor.newInstance(conf);
            System.out.println("Builder created successfully: " + builder);
            Method numDataNodesMethod = builderClass.getMethod("numDataNodes", int.class);
            numDataNodesMethod.invoke(builder, numNode);
            //Method formatMethod = builderClass.getMethod("format", boolean.class);
            //formatMethod.invoke(builder, false);
            Method buildMethod = builderClass.getMethod("build");
            Object cluster = buildMethod.invoke(builder);

            System.out.println("MiniDFSCluster instance created successfully: " + cluster);

            // invoke the getFileSystem method
            Method getFileSystemMethod = miniClusterClass.getMethod("getFileSystem");
            Object fs = getFileSystemMethod.invoke(cluster);
            System.out.println("FileSystem created successfully: " + fs);

            // invoke the shutdown method
            Method shutdownMethod = miniClusterClass.getMethod("shutdown");
            shutdownMethod.invoke(cluster);
            System.out.println("MiniDFSCluster shutdown successfully.");

            versionClassLoader.resetCurrentThreadClassLoader();
        }
    }


}
