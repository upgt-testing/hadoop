package org.apache.hadoop.hdfs;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationJVMInterface;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.server.datanode.DataNodeJVMInterface;
import org.apache.hadoop.hdfs.server.datanode.SecureResourcesJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.NameNodeJVMInterface;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

public class MiniDFSClusterHelper {
    private static VersionClassLoader nnClassLoader = null;
    private static VersionClassLoader dnClassLoader = null;
    private static NameNodeJVMInterface nnInstance = null;
    private static DataNodeJVMInterface dnInstance = null;
    private static ConfigurationJVMInterface NameNodeConfiguration = null;
    private static Class<?> FileSystemClass = null;

    /*
    static String base_dir_str = GenericTestUtils.getTestDir("dfs").getAbsolutePath() + File.separator;
    static File base_dir = new File(base_dir_str);
    static File data_dir = new File(base_dir, "data");

    static {
        if (base_dir.exists()) {
            FileUtil.fullyDelete(base_dir);
        }
        if (!data_dir.mkdirs()) {
            throw new RuntimeException("Cannot create base directory: " + data_dir);
        }
    } */


    public static DataNodeJVMInterface createDataNode() {

        VersionSelector versionSelector = new VersionSelector();

        String classPath = System.getProperty("java.class.path");
        //String[] versions = {"3.5.0-SNAPSHOT"};
        String version = "3.5.0-SNAPSHOT";

        //print the classpath from the thread context class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("Classpath from the thread context class loader: " + cl);


        //for (String version : versions) {
            File hcommonJar = versionSelector.getJarByVersion("hadoop-common", version);
            File hdfsJar = versionSelector.getJarByVersion("hadoop-hdfs", version);

            try {
                System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
                //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

                VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
                dnClassLoader = versionClassLoader;
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
                conf.set("fs.defaultFS", "hdfs://127.0.0.1:9000");
                conf.set("dfs.namenode.rpc-address", "127.0.0.1:9000");
                conf.set("dfs.namenode.http-address", "127.0.0.1:50070");
                conf.set("dfs.datanode.address", "127.0.0.1:50010");
                conf.set("dfs.datanode.http.address", "127.0.0.1:50075");
                conf.set("dfs.datanode.ipc.address", "127.0.0.1:50040");
                conf.set("dfs.datanode.hostname", "localhost");
                //conf.set("dfs.datanode.data.dir", "/Users/allenwang/data");
                conf.set("dfs.replication", "1");

                // get the HdfsConfiguration class and create an instance of it

                Class<?> HdfsConfigurationClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.HdfsConfiguration");
                Constructor<?> HdfsConfigurationConstructor = HdfsConfigurationClass.getConstructor(configClass);
                ConfigurationJVMInterface dnConf = (ConfigurationJVMInterface) HdfsConfigurationConstructor.newInstance(conf);

                // get SecureDataNodeStarter.SecureResources  and create an instance of it

                // get SecureDataNodeStarter.SecureResources class
                Class<?> SecureResourcesClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter$SecureResources");

                Class<?> SecureDataNodeStarterClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter");
                Method getSecureResourcesMethod = SecureDataNodeStarterClass.getMethod("getSecureResources", configClass);
                SecureResourcesJVMInterface secureResources = (SecureResourcesJVMInterface) getSecureResourcesMethod.invoke(null, dnConf);

                // get the DataNode class and create an instance of it
                Class<?> DataNodeClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.DataNode");
                // call the static method instantiateDataNode
                Method instantiateDataNodeMethod = DataNodeClass.getMethod("instantiateDataNode", String[].class, configClass, SecureResourcesClass);
                DataNodeJVMInterface dn = (DataNodeJVMInterface) instantiateDataNodeMethod.invoke(null, null, dnConf, secureResources);
                System.out.println("DataNode class is loaded by class loader: " + DataNodeClass.getClassLoader());
                System.out.println("DataNode class is located at: " + DataNodeClass.getProtectionDomain().getCodeSource().getLocation());
                System.out.println("DataNode Port is: " + dn.getIpcPort());
                dnInstance = dn;
                versionClassLoader.resetCurrentThreadClassLoader();
                Thread.sleep(5000);
                return dn;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        //}
        //return null;
    }

    public static DataNodeJVMInterface createDataNodeForInJVMCluster(String[] dnArgs, Configuration hdfsConf, boolean setSecureResources) {

        VersionSelector versionSelector = new VersionSelector();

        String classPath = System.getProperty("java.class.path");
        //String[] versions = {"3.5.0-SNAPSHOT"};
        String version = "3.5.0-SNAPSHOT";

        //print the classpath from the thread context class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("Classpath from the thread context class loader: " + cl);

        //for (String version : versions) {
        File hcommonJar = versionSelector.getJarByVersion("hadoop-common", version);
        File hdfsJar = versionSelector.getJarByVersion("hadoop-hdfs", version);

        try {
            System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
            //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

            VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
            dnClassLoader = versionClassLoader;
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
            Map<String, String> hdfsConfMap = hdfsConf.getSetParameters();
            conf.setAllParameters(hdfsConfMap);

            // get the HdfsConfiguration class and create an instance of it

            Class<?> HdfsConfigurationClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.HdfsConfiguration");
            Constructor<?> HdfsConfigurationConstructor = HdfsConfigurationClass.getConstructor(configClass);
            ConfigurationJVMInterface dnConf = (ConfigurationJVMInterface) HdfsConfigurationConstructor.newInstance(conf);

            // get SecureDataNodeStarter.SecureResources  and create an instance of it

            // get SecureDataNodeStarter.SecureResources class
            Class<?> SecureResourcesClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter$SecureResources");

            // get the DataNode class and create an instance of it
            Class<?> DataNodeClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.DataNode");
            // call the static method DataNode.instantiateDataNode(dnArgs, dnConf, secureResources);
            Method instantiateDataNodeMethod = DataNodeClass.getMethod("instantiateDataNode", String[].class, configClass, SecureResourcesClass);

            DataNodeJVMInterface dn = null;
            if (setSecureResources) {
                Class<?> SecureDataNodeStarterClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter");
                Method getSecureResourcesMethod = SecureDataNodeStarterClass.getMethod("getSecureResources", configClass);
                SecureResourcesJVMInterface secureResources = (SecureResourcesJVMInterface) getSecureResourcesMethod.invoke(null, dnConf);
                dn = (DataNodeJVMInterface) instantiateDataNodeMethod.invoke(null, dnArgs, dnConf, secureResources);
            }else {
                dn = (DataNodeJVMInterface) instantiateDataNodeMethod.invoke(null, dnArgs, dnConf, null);
            }

            System.out.println("DataNode class is loaded by class loader: " + DataNodeClass.getClassLoader());
            System.out.println("DataNode class is located at: " + DataNodeClass.getProtectionDomain().getCodeSource().getLocation());
            System.out.println("DataNode Port is: " + dn.getIpcPort());
            dnInstance = dn;
            versionClassLoader.resetCurrentThreadClassLoader();
            Thread.sleep(5000);
            return dn;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static NameNodeJVMInterface createNameNodeForInJVMCluster(String[] args, Configuration hdfsConf) throws IOException {

        VersionSelector versionSelector = new VersionSelector();

        String classPath = System.getProperty("java.class.path");
        //String[] versions = {"3.5.0-SNAPSHOT"};
        String version = "3.5.0-SNAPSHOT";

        //print the classpath from the thread context class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("Classpath from the thread context class loader: " + cl);

        //for (String version : versions) {
        File hcommonJar = versionSelector.getJarByVersion("hadoop-common", version);
        File hdfsJar = versionSelector.getJarByVersion("hadoop-hdfs", version);

        try {
            System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
            //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

            VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
            nnClassLoader = versionClassLoader;
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
            NameNodeConfiguration = conf;
            Map<String, String> hdfsConfMap = hdfsConf.getSetParameters();
            conf.setAllParameters(hdfsConfMap);

            //
            //NameNode namenode = new NameNode(conf);



            // Reset MetricsSystem
            DefaultMetricsSystem.shutdown();
            DefaultMetricsSystem.initialize("NameNode");

            Class<?> nameNodeClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.namenode.NameNode");

            // Call the NameNode.createNameNode(args, hdfsConf);
            Method createNameNodeMethod = nameNodeClass.getMethod("createNameNode", String[].class, configClass);
            NameNodeJVMInterface nameNodeInstance = (NameNodeJVMInterface) createNameNodeMethod.invoke(null, args, conf);



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

            nnInstance = nameNodeInstance;
            versionClassLoader.resetCurrentThreadClassLoader();
            Thread.sleep(5000);
            return nameNodeInstance;
        } catch (Exception e) {
            throw new IOException(e);
        }
        //}
        //return null;
    }

    public static NameNodeJVMInterface createNameNode(String[] args) {

        VersionSelector versionSelector = new VersionSelector();

        String classPath = System.getProperty("java.class.path");
        //String[] versions = {"3.5.0-SNAPSHOT"};
        String version = "3.5.0-SNAPSHOT";

        //print the classpath from the thread context class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("Classpath from the thread context class loader: " + cl);

        //for (String version : versions) {
            File hcommonJar = versionSelector.getJarByVersion("hadoop-common", version);
            File hdfsJar = versionSelector.getJarByVersion("hadoop-hdfs", version);

            try {
                System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
                //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

                VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
                nnClassLoader = versionClassLoader;
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
                NameNodeConfiguration = conf;
                conf.set("fs.defaultFS", "hdfs://127.0.0.1:9000");
                conf.set("dfs.namenode.rpc-address", "127.0.0.1:9000");
                conf.set("dfs.namenode.http-address", "127.0.0.1:50070");
                conf.set("dfs.datanode.address", "127.0.0.1:50010");
                conf.set("dfs.datanode.http.address", "127.0.0.1:50075");
                conf.set("dfs.datanode.ipc.address", "127.0.0.1:50040");
                conf.set("dfs.datanode.hostname", "localhost");
                //conf.set("dfs.datanode.data.dir", "/Users/allenwang/data");
                conf.set("dfs.replication", "1");

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



                nnInstance = nameNodeInstance;
                versionClassLoader.resetCurrentThreadClassLoader();
                Thread.sleep(5000);
                return nameNodeInstance;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        //}
        //return null;
    }



    public static void createFileWithDFS() throws Exception {
        // Use nnClassLoader to load FileSystem and call the FileSystem.get method to create fs
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

        DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(new URI("hdfs://localhost:9000"), conf);

        System.out.println("FileSystem URI: " + fs.getUri());
        System.out.println("Home directory: " + fs.getHomeDirectory());
        System.out.println("Working directory: " + fs.getWorkingDirectory());

        String dir = "/test-dir-2222";
        Path newDir = new Path(dir);
        fs.mkdirs(newDir);

        // list and print the content of the new directory
        System.out.println("Content of" + dir);
        for (FileStatus p : fs.listStatus(newDir)) {
            System.out.println(p.getPath());
            // read this
            Path filePath = p.getPath();
            try (InputStream is = fs.open(filePath);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                System.out.println("Content of " + filePath);
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
        // create a new file in the new directory
        Path newFile = new Path(dir + "/new_file");
        try (OutputStream os = fs.create(newFile)) {
            os.write("UIUC".getBytes());
        }
        // read the content of the new file
        try (InputStream is = fs.open(newFile);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            System.out.println("Content of new_file:");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
        // close the file system
        fs.close();

        /*
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        nnClassLoader.setCurrentThreadClassLoader();

        String fsURI = NameNodeConfiguration.get("fs.defaultFS");
        //DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(new URI(fsURI), conf);
        Class<?> configClass = nnClassLoader.loadClass("org.apache.hadoop.conf.Configuration");
        Class<?> fileSystemClass = nnClassLoader.loadClass("org.apache.hadoop.fs.FileSystem");
        FileSystemClass = fileSystemClass;

        //currentThread.setContextClassLoader(nnClassLoader.getUrlClassLoader());
        Method getMethod = fileSystemClass.getMethod("get", URI.class, configClass);
        DistributedFileSystemInterface fs = (DistributedFileSystemInterface) getMethod.invoke(null, new URI(fsURI), NameNodeConfiguration);
        //currentThread.setContextClassLoader(originalClassLoader);

        //DistributedFileSystem fs = null;
        // create a new directory and file
        String dir = "/test";

        // Load Path class with the nnClassLoader
        Class<?> pathClass = nnClassLoader.loadClass("org.apache.hadoop.fs.Path");
        //PathInterface newDir = new Path(dir);
        Constructor<?> pathConstructor = pathClass.getConstructor(String.class);
        PathInterface newDir = (PathInterface) pathConstructor.newInstance(dir);
        fs.mkdirs(newDir);
        // list and print the content of the new directory
        System.out.println("Content of" + dir);

        for (FileStatusInterface p : fs.listStatus(newDir)) {
            System.out.println(p.getPath());
            // read this
            PathInterface filePath = p.getPath();
            try (InputStream is = fs.open(filePath);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                System.out.println("Content of " + filePath);
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
        // create a new file in the new directory
        //Path newFile = new Path(dir + "/new_file");
        PathInterface newFile = (PathInterface) pathConstructor.newInstance(dir + "/new_file");
        try (OutputStream os = fs.create(newFile)) {
            os.write("UIUC".getBytes());
        }
        // read the content of the new file
        try (InputStream is = fs.open(newFile);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            System.out.println("Content of new_file:");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
        // close the file system
        fs.close();
        //currentThread.setContextClassLoader(originalClassLoader);

         */
    }



    public static Class<?> getFileSystemClass() {
        return FileSystemClass;
    }
}
