package org.apache.hadoop.hdfs.server.datanode;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
import edu.illinois.instance.Instance;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.NameNodeJVMInterface;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class DataNodeInstance extends Instance {
    public DataNodeInstance() {
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

        VersionClassLoader versionClassLoader = new VersionClassLoader(classPath, Arrays.asList(hcommonJar, hdfsJar));
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

            // load and call FileSystem.enableSymlinks()
            Class<?> FileSystemClass = versionClassLoader.loadClass("org.apache.hadoop.fs.FileSystem");
            Method enableSymlinksMethod = FileSystemClass.getMethod("enableSymlinks");
            enableSymlinksMethod.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DataNodeJVMInterface createDataNodeForInJVMCluster(String[] dnArgs, Configuration hdfsConf, boolean setSecureResources) {

        try {
            System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
            //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

            this.setClassLoader(versionClassLoader);
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

            versionClassLoader.resetCurrentThreadClassLoader();
            Thread.sleep(5000);
            return dn;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // For testing purpose, not used in the actual implementation


    public DataNodeJVMInterface createDataNode() {
        try {
            System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeJVMInterface.class.getClassLoader());
            //System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

            this.setClassLoader(versionClassLoader);
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

            versionClassLoader.resetCurrentThreadClassLoader();
            Thread.sleep(5000);
            return dn;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //}
        //return null;
    }


    @Override
    public String toString() {
        return "DataNodeInstance{" +
                "id=" + getId() +
                ", versionClassLoader=" + getVersionClassLoader() +
                ", customClassLoader=" + getVersionClassLoader().getUrlClassLoader() +
                ", parentClassLoader=" + getVersionClassLoader().getUrlClassLoader().getParentClassLoader() +
                '}';
    }
}
