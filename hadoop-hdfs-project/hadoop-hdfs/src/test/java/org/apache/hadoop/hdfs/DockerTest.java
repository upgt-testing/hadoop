package org.apache.hadoop.hdfs;

import org.apache.hadoop.hdfs.remoteProxies.*;
import org.apache.hadoop.hdfs.rmi.client.RemoteObjectProxy;
import org.apache.hadoop.hdfs.rmi.server.RemoteObject;
import edu.illinois.util.config.ConfigTracker;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NameNodeFake;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static org.junit.Assert.fail;

/**
 * Author: Shuai Wang
 */
public class DockerTest {

    @Test
    public void testConf() {
        MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
        NameNodeInterface nameNode = dockerHDFSCluster.getNameNode();
        DataNodeInterface dataNode = dockerHDFSCluster.getDataNode(0);

        Configuration conf = dataNode.getConf();
        conf.set("fake-from-test", "datanode-value");
        System.out.println("Configuration datanode after set: " + conf.get("fake-from-test"));

        Configuration sameConf = dataNode.getConf();
        //Assert.assertEquals(conf, sameConf);

        Configuration brandNewConf = dataNode.createConf();
        dataNode.setConf(brandNewConf);
        System.out.println("Configuration datanode after reset: " + dataNode.getConf().get("fake-from-test"));
    }

    @Test
    public void testInterface() throws IOException {
        MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
        NameNodeInterface nameNode = dockerHDFSCluster.getNameNode();
        DataNodeInterface dataNode = dockerHDFSCluster.getDataNode(0);
        System.out.println("Block ID: " + nameNode.getFSImage().getBlockPoolID());

        // test multiple same object access

        NameNodeFakeInterface fakeNN0 = nameNode.getFakeNameNode(0);
        NameNodeFakeInterface fakeNN0_5 = nameNode.getFakeNameNode(0);
        if (fakeNN0_5 == fakeNN0) {
            System.out.println("Fake NameNode 0 and Fake NameNode 0_5 are the same object");
        } else {
            System.out.println("Fake NameNode 0 and Fake NameNode 0_5 are different objects");
        }


        // TODO: create new wrapper only if the UUID is first seen locally
        NameNodeFakeInterface fakeNN1 = nameNode.getFakeNameNode(1);
        NameNodeFakeInterface fakeNN2 = nameNode.getFakeNameNode(2);
        System.out.println("Fake NameNode 0 ID: " + fakeNN0.getID());
        System.out.println("Fake NameNode 1 ID: " + fakeNN1.getID());
        System.out.println("Fake NameNode 2 ID: " + fakeNN2.getID());

        // test real object through UUID

        Configuration conf = dataNode.getConf();
        conf.set("fake-from-test", "datanode-value");

        // this test whether we get the same object from dataNode or not
        Configuration confFromDataNodeBefore = dataNode.getConf();
        System.out.println("Configuration datanode after set: " + confFromDataNodeBefore.get("fake-from-test"));
        Assert.assertEquals("datanode-value", confFromDataNodeBefore.get("fake-from-test"));

        // this test (1) whether we can create a new Configuration object;
        // (2) whether we reset the same old configuration or not
        Configuration brandNewConf = dataNode.createConf();
        dataNode.setConf(brandNewConf);
        System.out.println("Configuration datanode after reset: " + dataNode.getConf().get("fake-from-test"));
        Assert.assertNull(dataNode.getConf().get("fake-from-test"));

        // This test whether we can set the configuration from NameNode to DataNode
        // With the current implementation, the configuration get from NameNode does not exist in DataNode
        // So the code will fail due to NullPointerException when trying to get the object through UUID
        try {
            Configuration nnConf = nameNode.getConf();
            nnConf.set("fake-from-test", "namenode-value");
            dataNode.setConf(nnConf);
            System.out.println("Configuration datanode after set from NameNode: " + dataNode.getConf().get("fake-from-test"));
            fail("Should not be able to set configuration from NameNode");
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Set null configuration failed");
        }

        // For the things that we want to pass over multiple nodes, we may need to pass the real object and make the object serializable

        /*
        java.net.InetSocketAddress address = nameNode.getHttpAddress();
        DatanodeProtocolClientSideTranslatorPBInterface connection = dataNode.connectToNN(address);
        System.out.println(connection.toString());
        connection.close();*/

        dockerHDFSCluster.shutdown();
    }

    @Test
    public void testGetRMI() {
        try {
            MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
            FileSystem fs = dockerHDFSCluster.getFileSystem();
            System.setProperty("java.rmi.server.hostname", "localhost");
            //dockerHDFSCluster.getNameNode().getNamesystem().testRMIPrint("Hello RMI from FSNameSystem in Cluster");

            NameNodeInterface nameNode = dockerHDFSCluster.getNameNode();
            nameNode.testRMIPrint("Hello RMI from NameNode in Cluster");
            FSNamesystemInterface fsNameSystem = nameNode.getNamesystem();
            fsNameSystem.testRMIPrint("Hello RMI from FSNameSystem in Cluster");

            System.out.println("Get RMI test passed");
            dockerHDFSCluster.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testsRMI() {
        try {
            MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
            FileSystem fs = dockerHDFSCluster.getFileSystem();
            System.setProperty("java.rmi.server.hostname", "localhost");
            // Get the remote cluster object from the RMI registry
            Registry registry = LocateRegistry.getRegistry(1099);

            // Fake NameNode
            RemoteObject nameNodeFake = (RemoteObject) registry.lookup(NameNodeFake.class.getName());
            nameNodeFake.invoke("testRMIPrint", new Class<?>[]{String.class}, new Object[]{"Hello RMI"});

            NameNodeInterface namenodeFake = (NameNodeInterface) RemoteObjectProxy.newInstance(nameNodeFake, NameNodeInterface.class);
            namenodeFake.testRMIPrint("Hello RMI from Fake NameNode");

            // Actual NameNode
            RemoteObject nameNode = (RemoteObject) registry.lookup(NameNode.class.getName());
            nameNode.invoke("testRMIPrint", new Class<?>[]{String.class}, new Object[]{"Hello RMI"});
            // Create a dynamic proxy for the ClusterInterface
            NameNodeInterface namenode = (NameNodeInterface) RemoteObjectProxy.newInstance(nameNode, NameNodeInterface.class);
            namenode.testRMIPrint("Hello RMI from NameNode");
            Configuration conf = new Configuration();
            conf.set("fake-from-test", "fake-value");
            namenode.testRMIConf(conf);

            NameNodeInterface namenodeFromCluster = dockerHDFSCluster.getNameNode();
            namenodeFromCluster.testRMIPrint("Hello RMI from NameNode in Cluster");
            namenodeFromCluster.testRMIConf(conf);

            // Actual FSNameSystem
            RemoteObject fsNameSystem = (RemoteObject) registry.lookup(FSNamesystem.class.getName());
            fsNameSystem.invoke("testRMIPrint", new Class<?>[]{String.class}, new Object[]{"Hello RMI"});
            FSNamesystemInterface FSNamesystemInterface = RemoteObjectProxy.newInstance(fsNameSystem, FSNamesystemInterface.class);
            FSNamesystemInterface.testRMIPrint("Hello RMI from FSNameSystem");

            FSNamesystemInterface fsNameSystemFromCluster = dockerHDFSCluster.getFSNameSystem();
            fsNameSystemFromCluster.testRMIPrint("Hello RMI from FSNameSystem in Cluster");

            // Acutal DataNode
            Registry registry2 = LocateRegistry.getRegistry(1200);
            RemoteObject dataNode = (RemoteObject) registry2.lookup(DataNode.class.getName());
            dataNode.invoke("testRMIPrint", new Class<?>[]{String.class}, new Object[]{"Hello RMI"});
            DataNodeInterface DataNodeInterface = RemoteObjectProxy.newInstance(dataNode, DataNodeInterface.class);
            DataNodeInterface.testRMIPrint("Hello RMI from DataNode");

            DataNodeInterface dataNodeFromCluster = dockerHDFSCluster.getDataNode(0);
            dataNodeFromCluster.testRMIPrint("Hello RMI from DataNode in Cluster");

            dockerHDFSCluster.shutdown();

            System.out.println("RMI test passed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup() {
        ConfigTracker.clearSetParams();
    }

    @Test
    public void testFileSystem() {
        try {
            MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
            FileSystem fs = dockerHDFSCluster.getFileSystem();
            // check whether the file system is functional
            System.out.println("FileSystem URI: " + fs.getUri());
            System.out.println("Home directory: " + fs.getHomeDirectory());
            System.out.println("Working directory: " + fs.getWorkingDirectory());
            String dir = "/test-dir-2222";
            Path newDir = new Path(dir);
            fs.mkdirs(newDir);
            System.out.println("Before upgrade, you have 10 seconds to check the current datanode version");
            Thread.sleep(10000);
            // The datanode index starts from 0
            dockerHDFSCluster.upgradeDatanode(0);
            dockerHDFSCluster.upgradeDatanode(0);
            System.out.println("After upgrade, you have 10 seconds to check the current datanode version");
            Thread.sleep(10000);
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
            dockerHDFSCluster.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create file system", e);
        }
    }
}
