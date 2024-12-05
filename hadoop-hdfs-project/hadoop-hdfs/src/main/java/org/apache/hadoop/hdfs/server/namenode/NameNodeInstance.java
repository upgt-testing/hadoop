package org.apache.hadoop.hdfs.server.namenode;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
import edu.illinois.instance.Instance;
import org.apache.hadoop.classification.VisibleForTesting;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationInterface;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class NameNodeInstance extends Instance {
    private final static String NameNodeClassName = "org.apache.hadoop.hdfs.server.namenode.NameNode";
    private final static String ConfigurationClassName = "org.apache.hadoop.conf.Configuration";
    Class<?> nameNodeClass = null;

    public NameNodeInstance() {
        super();
        createVersionClassLoader();
        setMiniClusterTestingMode();
    }

    public VersionClassLoader createVersionClassLoader() {
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

        VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar), true);
        this.setClassLoader(versionClassLoader);

        return versionClassLoader;
    }

    public void setMiniClusterTestingMode() {
        //DefaultMetricsSystem.setMiniClusterMode(true);
        try {
            Class<?> DefaultMetricsSystemClass = versionClassLoader.loadClass("org.apache.hadoop.metrics2.lib.DefaultMetricsSystem");
            // call DefaultMetricsSystem.setMiniClusterMode(true) with reflection
            Method setMiniClusterModeMethod = DefaultMetricsSystemClass.getMethod("setMiniClusterMode", boolean.class);
            setMiniClusterModeMethod.invoke(null, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void format(Configuration targetConf) {
        // use the version class loader to load the NameNode class and call the format method
        try {
            versionClassLoader.setCurrentThreadClassLoader();
            Class<?> nameNodeClass = versionClassLoader.loadClass(NameNodeClassName);
            Class<?> configClass = versionClassLoader.loadClass(ConfigurationClassName);

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
            ConfigurationInterface conf = (ConfigurationInterface) configConstructor.newInstance();
            //conf.set("hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            // call conf.set function with key and value
            //Method setMethod = configClass.getMethod("set", String.class, String.class);
            //setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            //NameNode nameNode = new NameNode(conf);
            Map<String, String> targetConfMap = targetConf.getSetParameters();
            conf.setAllParameters(targetConfMap);

            // invoke NameNode.format(new Configuration(conf));
            Method formatMethod = nameNodeClass.getMethod("format", configClass);
            formatMethod.invoke(null, conf);
            versionClassLoader.resetCurrentThreadClassLoader();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<URI> getNamespaceDirs(Configuration targetConf) {
        //FSNamesystem.getNamespaceDirs(conf);
        // First load the FSNamesystem class and configuration class
        try {
            versionClassLoader.setCurrentThreadClassLoader();
            Class<?> fsNamesystemClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.namenode.FSNamesystem");
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
            ConfigurationInterface conf = (ConfigurationInterface) configConstructor.newInstance();
            //conf.set("hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            // call conf.set function with key and value
            //Method setMethod = configClass.getMethod("set", String.class, String.class);
            //setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            //NameNode nameNode = new NameNode(conf);
            Map<String, String> targetConfMap = targetConf.getSetParameters();
            conf.setAllParameters(targetConfMap);

            // invoke FSNamesystem.getNamespaceDirs(conf);
            Method getNamespaceDirsMethod = fsNamesystemClass.getMethod("getNamespaceDirs", configClass);
            Collection<URI> namespaceDirs = (Collection<URI>) getNamespaceDirsMethod.invoke(null, conf);
            versionClassLoader.resetCurrentThreadClassLoader();
            return namespaceDirs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public NameNodeInterface createNameNodeForInJVMCluster(String[] args, Configuration hdfsConf) throws IOException {
        try {
            System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeInterface.class.getClassLoader());
            //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

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
            ConfigurationInterface conf = (ConfigurationInterface) configConstructor.newInstance();
            //conf.set("hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            // call conf.set function with key and value
            //Method setMethod = configClass.getMethod("set", String.class, String.class);
            //setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            //NameNode nameNode = new NameNode(conf);
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
            NameNodeInterface nameNodeInstance = (NameNodeInterface) createNameNodeMethod.invoke(null, args, conf);



            String s = nameNodeInstance.getClientNamenodeAddress();

            System.out.println("Client namenode address: " + s);

            System.out.println("nameNodeInstance object is loaded by class loader: " + nameNodeInstance.getClass().getClassLoader());
            System.out.println("NameNodeInterface class is loaded by class loader: " + NameNodeInterface.class.getClassLoader());
            System.out.println("nameNodeClass Object is loaded by class loader: " + nameNodeClass.getClassLoader());
            System.out.println("Configuration Object is loaded by class loader: " + conf.getClass().getClassLoader());
            System.out.println("ConfigClass is loaded by class loader: " + configClass.getClassLoader());
            System.out.println("ConfigurationInterface is loaded by class loader: " + ConfigurationInterface.class.getClassLoader());

            InetSocketAddress net = nameNodeInstance.getNameNodeAddress();
            System.out.println("NameNode address: " + net.getHostName() + ":" + net.getPort());

            //FSImageInterface fsImage = nameNodeInstance.getFSImage();
            //System.out.println("FSImage block ID is " + fsImage.getBlockPoolID());

            versionClassLoader.resetCurrentThreadClassLoader();
            Thread.sleep(5000);
            return nameNodeInstance;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }



    // This is for upgt testing, not for actual MiniCluster usage.
    public NameNodeInterface createNameNode(String[] args) {
        try {
            System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeInterface.class.getClassLoader());
            //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

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
            ConfigurationInterface conf = (ConfigurationInterface) configConstructor.newInstance();
            //conf.set("hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            // call conf.set function with key and value
            //Method setMethod = configClass.getMethod("set", String.class, String.class);
            //setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
            //NameNode nameNode = new NameNode(conf);

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
            NameNodeInterface nameNodeInstance = (NameNodeInterface) createNameNodeMethod.invoke(null, new String[]{}, conf);


            //Constructor<?> nameNodeConstructor = nameNodeClass.getConstructor(configClass); // Use the same Configuration class
            //NameNodeInterface nameNodeInstance = (NameNodeInterface) nameNodeConstructor.newInstance(conf); // Pass the Configuration object


            String s = nameNodeInstance.getClientNamenodeAddress();

            System.out.println("Client namenode address: " + s);

            System.out.println("nameNodeInstance object is loaded by class loader: " + nameNodeInstance.getClass().getClassLoader());
            System.out.println("NameNodeInterface class is loaded by class loader: " + NameNodeInterface.class.getClassLoader());
            System.out.println("nameNodeClass Object is loaded by class loader: " + nameNodeClass.getClassLoader());
            System.out.println("Configuration Object is loaded by class loader: " + conf.getClass().getClassLoader());
            System.out.println("ConfigClass is loaded by class loader: " + configClass.getClassLoader());
            System.out.println("ConfigurationInterface is loaded by class loader: " + ConfigurationInterface.class.getClassLoader());

            InetSocketAddress net = nameNodeInstance.getNameNodeAddress();
            System.out.println("NameNode address: " + net.getHostName() + ":" + net.getPort());

            //FSImageInterface fsImage = nameNodeInstance.getFSImage();
            //System.out.println("FSImage block ID is " + fsImage.getBlockPoolID());



            versionClassLoader.resetCurrentThreadClassLoader();
            Thread.sleep(5000);
            return nameNodeInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //}
        //return null;
    }


    @Override
    public String toString() {
        return "NameNodeInstance{" +
                "id=" + getId() +
                ", versionClassLoader=" + getVersionClassLoader() +
                ", customClassLoader=" + getVersionClassLoader().getUrlClassLoader() +
                ", parentClassLoader=" + getVersionClassLoader().getUrlClassLoader().getParentClassLoader() +
                '}';
    }
}
