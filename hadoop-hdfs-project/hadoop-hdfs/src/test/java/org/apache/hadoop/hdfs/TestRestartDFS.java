/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A JUnit test for checking if restarting DFS preserves integrity.
 */
public class TestRestartDFS {

  @Test
  public void testLoadEverything() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    // print classpath
    System.out.println("Printing classpath:");
    System.out.println(System.getProperty("java.class.path"));
    // remove existing hadoop-common from classpath
    System.setProperty("java.class.path", System.getProperty("java.class.path").replace("/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-common/3.5.0-SNAPSHOT/hadoop-common-3.5.0-SNAPSHOT.jar", "/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-common/3.3.1/hadoop-common-3.3.1.jar"));
    // remove     // /Users/allenwang/Documents/xlab/cross-system/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test-classes:/Users/allenwang/Documents/xlab/cross-system/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/classes
    System.setProperty("java.class.path", System.getProperty("java.class.path").replace("/Users/allenwang/Documents/xlab/cross-system/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test-classes:/Users/allenwang/Documents/xlab/cross-system/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/classes:", ""));
    // print classpath again
    System.out.println("Printing classpath after removing hadoop-common-3.5.0-SNAPSHOT.jar:");
    System.out.println(System.getProperty("java.class.path"));


    String commonPath = "/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-common/3.3.1/hadoop-common-3.3.1.jar";
    String hdfsTestPath = "/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-hdfs/3.3.1/hadoop-hdfs-3.3.1-tests.jar";
    String hdfsPath = "/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-hdfs/3.3.1/hadoop-hdfs-3.3.1.jar";
    File myJar = new File(commonPath);
    File hdfsJar = new File(hdfsPath);
    File hdfsTestJar = new File(hdfsTestPath);
    // create a URL list with the jar file
    URL[] urls = {myJar.toURI().toURL(), hdfsJar.toURI().toURL(), hdfsTestJar.toURI().toURL()};
    // iterate java.class.path and add them to the URL list
    String[] classpath = System.getProperty("java.class.path").split(":");
    for (String path : classpath) {
      urls = Arrays.copyOf(urls, urls.length + 1);
      urls[urls.length - 1] = new File(path).toURI().toURL();
    }
    System.out.println("Printing everything from the URL list:");
    for (URL url : urls) {
      System.out.println(url.getFile());
    }

    URLClassLoader child = new URLClassLoader(
            //new URL[] {myJar.toURI().toURL()},
            //new URL[] {myJar.toURI().toURL(), hdfsJar.toURI().toURL()},
            // new URL = myJar + hdfsJar + java.class.path
            //new URL[] {myJar.toURI().toURL(), hdfsJar.toURI().toURL(), new URL("jar:file:" + System.getProperty("java.class.path"))},
            urls,
            null
    );
    Thread.currentThread().setContextClassLoader(child);

/**    List<URL> urls = Arrays.asList(((URLClassLoader) child).getURLs());
    System.out.println("Printing everything from the child class loader:");
    for (URL url : urls) {
      System.out.println(url.getFile());
    }**/
    // print everything from the current class loader
//    System.out.println("Printing everything from the current class loader:");
//    URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), null);
    // load configuration class with classLoader
    Class<?> configClass = child.loadClass("org.apache.hadoop.conf.Configuration");
    Class<?> builderClass = child.loadClass("org.apache.hadoop.hdfs.MiniDFSCluster$Builder");

    // location of the configuration class
    System.out.println("Load class org.apache.hadoop.conf.Configuration from: " + configClass.getProtectionDomain().getCodeSource().getLocation());
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

    Object builder = builderConstructor.newInstance(conf);
    System.out.println("Builder created successfully: " + builder);
    Method numDataNodesMethod = builderClass.getMethod("numDataNodes", int.class);
    numDataNodesMethod.invoke(builder, 1);
    Method formatMethod = builderClass.getMethod("format", boolean.class);
    formatMethod.invoke(builder, false);
    Method buildMethod = builderClass.getMethod("build");
    Object cluster = buildMethod.invoke(builder);
    System.out.println("MiniDFSCluster instance created successfully: " + cluster);
    // There are two constructors
    // public org.apache.hadoop.hdfs.MiniDFSCluster$Builder(org.apache.hadoop.conf.Configuration)
    //public org.apache.hadoop.hdfs.MiniDFSCluster$Builder(org.apache.hadoop.conf.Configuration,java.io.File)



/**
    // Obtain the constructor that takes a Configuration object
    Constructor<?> builderConstructor = builderClass.getConstructor(Configuration.class);

    // Create an instance of the Builder with the provided configuration
    Object builder = builderConstructor.newInstance(new Configuration());

    // Get the method for setting the number of data nodes and invoke it
    Method numDataNodesMethod = builderClass.getMethod("numDataNodes", int.class);
    numDataNodesMethod.invoke(builder, 1);

    Method formatMethod = builderClass.getMethod("format", boolean.class);
    formatMethod.invoke(builder, false);  // or false depending on your test setup

    // Build the MiniDFSCluster instance using the build method
    Method buildMethod = builderClass.getMethod("build");

**/

/**
    JarFile jarFile = new JarFile(pathToJar);
    Enumeration<JarEntry> e = jarFile.entries();

    URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
    URLClassLoader cl = URLClassLoader.newInstance(urls);

    while (e.hasMoreElements()) {
      JarEntry je = e.nextElement();
      if(je.isDirectory() || !je.getName().endsWith(".class")){
        continue;
      }
      // -6 because of .class
      String className = je.getName().substring(0,je.getName().length()-6);
      className = className.replace('/', '.');
      Class c = cl.loadClass(className);
      if (c.getName().contains("MiniDFSCluster")) {
        URL location = c.getProtectionDomain().getCodeSource().getLocation();
        System.out.print("Found class: " + c.getName());
        System.out.println("Loaded from: " + location);
      }
    }
 **/
  }

  /**
   * Load different version of Hadoop classes.
   */
  private ClassLoader loadHadoop(String version) throws MalformedURLException {
    System.out.println("Loading Hadoop classes from version " + version);

    File hdfsJar = new File("/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-hdfs/3.5.0-SNAPSHOT/hadoop-hdfs-3.5.0-SNAPSHOT-tests.jar");
    File commonJar = new File("/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-common/3.5.0-SNAPSHOT/hadoop-common-3.5.0-SNAPSHOT.jar");
    //File hdfsJar = new File(System.getProperty("user.home") + "/.m2/repository/org/apache/hadoop/hadoop-hdfs/" + version + "/hadoop-hdfs-" + version + "-tests.jar");
    //File commonJar = new File(System.getProperty("user.home") + "/.m2/repository/org/apache/hadoop/hadoop-common/" + version + "/hadoop-common-" + version + ".jar");
    if (!hdfsJar.exists()){
      System.out.println("Hadoop HDFS jar not found: " + hdfsJar.getAbsolutePath());
      return null;
    }
    if (!commonJar.exists()){
      System.out.println("Hadoop Common jar not found: " + commonJar.getAbsolutePath());
      return null;
    }

    URL[] urls = {hdfsJar.toURI().toURL(), commonJar.toURI().toURL()};
    //return new URLClassLoader(urls, getClass().getClassLoader());
    return new URLClassLoader(urls, null);
    //return getClass().getClassLoader();
  }

  /**
   * Create a MiniDFSCluster instance.
   */
  private Object createMiniDFSCluster(Configuration conf, int numDataNodes, boolean format, ClassLoader cl) throws Exception {

    // This class loader is used for loading shared dependencies
    ClassLoader sharedClassLoader = getClass().getClassLoader();

    File hdfsJar = new File("/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-hdfs/3.5.0-SNAPSHOT/hadoop-hdfs-3.5.0-SNAPSHOT-tests.jar");
    File commonJar = new File("/Users/allenwang/.m2/repository/org/apache/hadoop/hadoop-common/3.3.1/hadoop-common-3.3.1.jar");
    //File hdfsJar = new File(System.getProperty("user.home") + "/.m2/repository/org/apache/hadoop/hadoop-hdfs/" + version + "/hadoop-hdfs-" + version + "-tests.jar");
    //File commonJar = new File(System.getProperty("user.home") + "/.m2/repository/org/apache/hadoop/hadoop-common/" + version + "/hadoop-common-" + version + ".jar");
    if (!hdfsJar.exists()){
      System.out.println("Hadoop HDFS jar not found: " + hdfsJar.getAbsolutePath());
      return null;
    }
    if (!commonJar.exists()){
      System.out.println("Hadoop Common jar not found: " + commonJar.getAbsolutePath());
      return null;
    }

    // Load the Builder class dynamically
    Class<?> builderClass = cl.loadClass("org.apache.hadoop.hdfs.MiniDFSCluster$Builder");

    // Obtain the constructor that takes a Configuration object
    Constructor<?> builderConstructor = builderClass.getConstructor(Configuration.class);

    // Create an instance of the Builder with the provided configuration
    Object builder = builderConstructor.newInstance(conf);

    // Get the method for setting the number of data nodes and invoke it
    Method numDataNodesMethod = builderClass.getMethod("numDataNodes", int.class);
    numDataNodesMethod.invoke(builder, numDataNodes);

    Method formatMethod = builderClass.getMethod("format", boolean.class);
    formatMethod.invoke(builder, format);  // or false depending on your test setup

    // Build the MiniDFSCluster instance using the build method
    Method buildMethod = builderClass.getMethod("build");
    return buildMethod.invoke(builder);
  }

  @Test
  public void simpleTest() {
    Configuration conf = new Configuration();
    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    System.out.println("Builder created successfully: " + builder);
  }

  @Test
  public void testCreateDifferentMiniCluster() {
    try {
      // Load different version of Hadoop classes
      ClassLoader cl = loadHadoop("3.3.1");
      // check whether class loader has org.apache.hadoop.hdfs.MiniDFSCluster$Builder
      try {
          assert cl != null;
          cl.loadClass("org.apache.hadoop.hdfs.MiniDFSCluster$Builder");
      } catch (ClassNotFoundException e) {
          System.out.println("org.apache.hadoop.hdfs.MiniDFSCluster$Builder not found in class loader");
          return;
      }

      // Load different version of Hadoop classes
      ClassLoader cl2 = loadHadoop("3.3.3");
      // check whether class loader has org.apache.hadoop.hdfs.MiniDFSCluster$Builder
      try {
          assert cl2 != null;
          cl2.loadClass("org.apache.hadoop.hdfs.MiniDFSCluster$Builder");
      } catch (ClassNotFoundException e) {
          System.out.println("org.apache.hadoop.hdfs.MiniDFSCluster$Builder not found in class loader");
          return;
      }

      // Create a MiniDFSCluster instance
      Configuration conf = new Configuration();
      Object cluster = createMiniDFSCluster(conf, 1, true, cl);
      System.out.println("MiniDFSCluster instance with version 3.3.1 created.");
      Object cluster2 = createMiniDFSCluster(conf, 2, false, cl2);
      System.out.println("MiniDFSCluster instance with version 3.3.3 created.");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void runTests(Configuration conf, boolean serviceTest) throws Exception {
    MiniDFSCluster cluster = null;
    DFSTestUtil files = new DFSTestUtil.Builder().setName("TestRestartDFS").
        setNumFiles(20).build();

    final String dir = "/srcdat";
    final Path rootpath = new Path("/");
    final Path dirpath = new Path(dir);

    long rootmtime;
    FileStatus rootstatus;
    FileStatus dirstatus;

    try {
      if (serviceTest) {
        conf.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
                 "localhost:0");
      }
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(4).build();
      FileSystem fs = cluster.getFileSystem();
      files.createFiles(fs, dir);

      rootmtime = fs.getFileStatus(rootpath).getModificationTime();
      rootstatus = fs.getFileStatus(dirpath);
      dirstatus = fs.getFileStatus(dirpath);

      fs.setOwner(rootpath, rootstatus.getOwner() + "_XXX", null);
      fs.setOwner(dirpath, null, dirstatus.getGroup() + "_XXX");
    } finally {
      if (cluster != null) { cluster.shutdown(); }
    }
    try {
      if (serviceTest) {
        conf.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
                 "localhost:0");
      }
      // Here we restart the MiniDFScluster without formatting namenode
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(4).format(false).build(); 
      FileSystem fs = cluster.getFileSystem();
      assertTrue("Filesystem corrupted after restart.",
                 files.checkFiles(fs, dir));

      final FileStatus newrootstatus = fs.getFileStatus(rootpath);
      assertEquals(rootmtime, newrootstatus.getModificationTime());
      assertEquals(rootstatus.getOwner() + "_XXX", newrootstatus.getOwner());
      assertEquals(rootstatus.getGroup(), newrootstatus.getGroup());

      final FileStatus newdirstatus = fs.getFileStatus(dirpath);
      assertEquals(dirstatus.getOwner(), newdirstatus.getOwner());
      assertEquals(dirstatus.getGroup() + "_XXX", newdirstatus.getGroup());
      rootmtime = fs.getFileStatus(rootpath).getModificationTime();
    } finally {
      if (cluster != null) { cluster.shutdown(); }
    }
    try {
      if (serviceTest) {
        conf.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
                 "localhost:0");
      }
      // This is a second restart to check that after the first restart
      // the image written in parallel to both places did not get corrupted
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(4).format(false).build();
      FileSystem fs = cluster.getFileSystem();
      assertTrue("Filesystem corrupted after restart.",
                 files.checkFiles(fs, dir));

      final FileStatus newrootstatus = fs.getFileStatus(rootpath);
      assertEquals(rootmtime, newrootstatus.getModificationTime());
      assertEquals(rootstatus.getOwner() + "_XXX", newrootstatus.getOwner());
      assertEquals(rootstatus.getGroup(), newrootstatus.getGroup());

      final FileStatus newdirstatus = fs.getFileStatus(dirpath);
      assertEquals(dirstatus.getOwner(), newdirstatus.getOwner());
      assertEquals(dirstatus.getGroup() + "_XXX", newdirstatus.getGroup());

      files.cleanup(fs, dir);
    } finally {
      if (cluster != null) { cluster.shutdown(); }
    }
  }
  /** check if DFS remains in proper condition after a restart */
  @Test
  public void testRestartDFS() throws Exception {
    final Configuration conf = new HdfsConfiguration();
    runTests(conf, false);
  }
  
  /** check if DFS remains in proper condition after a restart 
   * this rerun is with 2 ports enabled for RPC in the namenode
   */
  @Test
   public void testRestartDualPortDFS() throws Exception {
     final Configuration conf = new HdfsConfiguration();
     runTests(conf, true);
   }
}
