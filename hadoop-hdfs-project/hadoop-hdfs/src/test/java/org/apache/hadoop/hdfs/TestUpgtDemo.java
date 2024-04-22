package org.apache.hadoop.hdfs;

import edu.illinois.VersionClassLoader;
import edu.illinois.VersionSelector;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Author: Shuai Wang
 */
public class TestUpgtDemo {

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


    /** ========================= Original Test Methods ========================= */
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
