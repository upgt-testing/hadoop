package org.apache.hadoop.hdfs;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
import edu.illinois.instance.InstanceTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationJVMInterface;
import org.apache.hadoop.hdfs.server.datanode.DataNodeInstance;
import org.apache.hadoop.hdfs.server.datanode.DataNodeJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.FSImageInterface;
import org.apache.hadoop.hdfs.server.namenode.NameNodeInstance;
import org.apache.hadoop.hdfs.server.namenode.NameNodeJVMInterface;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
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
    @Before
    public void setUp() {
        //DefaultMetricsSystem.setMiniClusterMode(true);
    }

    //TODO: (1) The argument should be interface or not??? (2) Write a test to test the FSNamesystemInterface got from MiniDFSClusterInJVM

    /*
    @Test
    public void testDiffLoaderCommunication() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://127.0.0.1:9000");
        conf.set("dfs.namenode.rpc-address", "127.0.0.1:9000");
        conf.set("dfs.namenode.http-address", "127.0.0.1:50070");
        conf.set("dfs.datanode.address", "127.0.0.1:50010");
        conf.set("dfs.datanode.http.address", "127.0.0.1:50075");
        conf.set("dfs.datanode.ipc.address", "127.0.0.1:50040");
        conf.set("dfs.datanode.hostname", "localhost");
        //conf.set("dfs.datanode.data.dir", "/Users/allenwang/data");
        conf.set("dfs.replication", "1");
        NameNodeInstance nameNodeInstance = new NameNodeInstance();
        NameNodeJVMInterface nameNode = nameNodeInstance.createNameNode(new String[]{});
        ConfigurationJVMInterface configurationJVMInterface = (ConfigurationJVMInterface) conf;
        InetSocketAddress addr = nameNode.getRpcServerAddress(configurationJVMInterface);
        System.out.println("nameNode is loaded by " + nameNode.getClass().getClassLoader());
        System.out.println("Configuration is loaded by " + conf.getClass().getClassLoader());
        System.out.println("NameNode address: " + addr.getHostName() + ":" + addr.getPort());
    }

    @Test
    public void testFSNamesystemFromMiniClusterInJVM() throws IOException {
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(new Configuration()).numDataNodes(1).build();
        FSNamesystemJVMInterface fsNamesystem = cluster.getNamesystem();
        System.out.println("FSNamesystem is loaded by " + fsNamesystem.getClass().getClassLoader());
        System.out.println("FSNamesystem is " + fsNamesystem);
        System.out.println("FSNamesysten HaEnabled is: " + fsNamesystem.isHaEnabled());
    }
     */
    @Test
    public void testMiniClusterInJVM2() throws IOException {
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(new Configuration()).numDataNodes(2).build();
        System.out.println("NameNode address: " + cluster.fakeGetNameNode().getHostAndPort());
        System.out.println("DataNode1 address: " + cluster.getDataNodes().get(0).getDatanodeHostname() + ":" + cluster.getDataNodes().get(0).getIpcPort());
        System.out.println("DataNode2 address: " + cluster.getDataNodes().get(1).getDatanodeHostname() + ":" + cluster.getDataNodes().get(1).getIpcPort());
        System.out.println(InstanceTable.printString());
    }

    @Test
    public void testMiniClusterInJVM() throws IOException {
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(new Configuration()).numDataNodes(1).build();
        System.out.println("NameNode address: " + cluster.fakeGetNameNode().getHostAndPort());
        System.out.println("DataNode address: " + cluster.getDataNodes().get(0).getDatanodeHostname() + ":" + cluster.getDataNodes().get(0).getIpcPort());
        System.out.println(InstanceTable.printString());
    }

    @Test
    public void testInstances() {
        NameNodeInstance nameNodeInstance = new NameNodeInstance();
        DataNodeInstance dataNodeInstance = new DataNodeInstance();

        NameNodeJVMInterface nameNode = nameNodeInstance.createNameNode(new String[]{});
        DataNodeJVMInterface dataNode = dataNodeInstance.createDataNode();
        System.out.println(InstanceTable.printString());
        System.out.println("NameNode address: " + nameNode.getNameNodeAddress());
        System.out.println("DataNode address: " + dataNode.getXferAddress());

    }

    @Test
    public void testNameNodeCreation() throws Exception {
        NameNodeJVMInterface nn = MiniDFSClusterHelper.createNameNode(null);

        FSImageInterface fsImage = nn.getFSImage();
        System.out.println("FSImage is loaded by " + fsImage.getClass().getClassLoader());
        System.out.println("FSImageInterface is loaded by " + FSImageInterface.class.getClassLoader());
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


    @Test
    public void testMiniCluster() throws IOException {
        //MiniDFSCluster cluster = new MiniDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
        //System.out.println(cluster.getNameNodeInterface(0).getClientNamenodeAddress());
    }

    @Test
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

    @Test
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
