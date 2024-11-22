package org.apache.hadoop.hdfs;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationInterface;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.DataNodeInterface;
import org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter;
import org.apache.hadoop.hdfs.server.datanode.SecureResourcesInterface;
import org.apache.hadoop.hdfs.server.namenode.FSImageInterface;
import org.apache.hadoop.hdfs.server.namenode.NameNodeInterface;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class MiniDFSClusterHelper {

    public static DataNodeInterface createDataNode() {

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
                System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeInterface.class.getClassLoader());
                System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

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
                ConfigurationInterface conf = (ConfigurationInterface) configConstructor.newInstance();
                //conf.set("hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
                // call conf.set function with key and value
                //Method setMethod = configClass.getMethod("set", String.class, String.class);
                //setMethod.invoke(conf, "hadoop.security.group.mapping", "org.apache.hadoop.security.JniBasedUnixGroupsMappingWithFallback");
                //NameNode nameNode = new NameNode(conf);
                conf.set("fs.defaultFS", "hdfs://localhost:9000");
                /**
                 * <configuration>
                 *     <property>
                 *         <name>dfs.namenode.rpc-address</name>
                 *         <value>namenode:9000</value>
                 *     </property>
                 *     <property>
                 *         <name>dfs.namenode.http-address</name>
                 *         <value>namenode:50070</value>
                 *     </property>
                 *     <property>
                 *         <name>dfs.datanode.address</name>
                 *         <value>0.0.0.0:50010</value>
                 *     </property>
                 *     <property>
                 *         <name>dfs.datanode.http.address</name>
                 *         <value>0.0.0.0:50075</value>
                 *     </property>
                 *     <property>
                 *         <name>dfs.datanode.ipc.address</name>
                 *         <value>0.0.0.0:50040</value>
                 *     </property>
                 *         <property>
                 *         <name>dfs.datanode.hostname</name>
                 *         <value>localhost</value>
                 *     </property>
                 * </configuration>
                 */
                conf.set("dfs.namenode.rpc-address", "localhost:9000");
                conf.set("dfs.namenode.http-address", "localhost:50070");
                conf.set("dfs.datanode.address", "localhost:50010");
                conf.set("dfs.datanode.http.address", "localhost:50075");
                conf.set("dfs.datanode.ipc.address", "localhost:50040");
                conf.set("dfs.datanode.hostname", "localhost");

                //
                //NameNode namenode = new NameNode(conf);

                // get the HdfsConfiguration class and create an instance of it

                Class<?> HdfsConfigurationClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.HdfsConfiguration");
                Constructor<?> HdfsConfigurationConstructor = HdfsConfigurationClass.getConstructor(configClass);
                ConfigurationInterface dnConf = (ConfigurationInterface) HdfsConfigurationConstructor.newInstance(conf);

                // get SecureDataNodeStarter.SecureResources  and create an instance of it

                // get SecureDataNodeStarter.SecureResources class
                Class<?> SecureResourcesClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter$SecureResources");

                Class<?> SecureDataNodeStarterClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter");
                Method getSecureResourcesMethod = SecureDataNodeStarterClass.getMethod("getSecureResources", configClass);
                SecureResourcesInterface secureResources = (SecureResourcesInterface) getSecureResourcesMethod.invoke(null, dnConf);

                // get the DataNode class and create an instance of it
                Class<?> DataNodeClass = versionClassLoader.loadClass("org.apache.hadoop.hdfs.server.datanode.DataNode");
                // call the static method instantiateDataNode
                Method instantiateDataNodeMethod = DataNodeClass.getMethod("instantiateDataNode", String[].class, configClass, SecureResourcesClass);
                DataNodeInterface dn = (DataNodeInterface) instantiateDataNodeMethod.invoke(null, null, dnConf, secureResources);
                System.out.println("DataNode Port is: " + dn.getIpcPort());
                return dn;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static NameNodeInterface createNameNode(String[] args, Configuration hdfsConf) {

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
                System.out.println("NameNodeInterface is loaded by class loader: " + NameNodeInterface.class.getClassLoader());
                System.out.println("Configuration Class is loaded by class loader: " + Configuration.class.getClassLoader());

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
                ConfigurationInterface conf = (ConfigurationInterface) configConstructor.newInstance();
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

                FSImageInterface fsImage = nameNodeInstance.getFSImage();
                System.out.println("FSImage block ID is " + fsImage.getBlockPoolID());
                return nameNodeInstance;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
