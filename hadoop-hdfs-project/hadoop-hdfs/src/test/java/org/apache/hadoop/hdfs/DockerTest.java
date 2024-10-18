package org.apache.hadoop.hdfs;

import edu.illinois.util.config.ConfigTracker;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Shuai Wang
 */
public class DockerTest {

    @Before
    public void setup() {
        ConfigTracker.clearSetParams();
    }

    @Test
    public void testMXBeanRMI() throws IOException {
        MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
        int res = dockerHDFSCluster.getNameNode().fakePrintMXBean();
        System.out.println("Call function fakePrintMXBean and the Result: " + res);
        int res2 = dockerHDFSCluster.getDataNodes().get(0).fetchInfoPort();
        System.out.println("Call function getIpcPort and the Result: " + res2);
        System.out.println("fetch X fer Port " + dockerHDFSCluster.getDataNodes().get(0).fetchXferPort());
        System.out.println("metrics name " + dockerHDFSCluster.getDataNodes().get(0).fetchMetricsName());
        System.out.println("get cache used " + dockerHDFSCluster.getDataNodes().get(0).fetchFSDatasetGetCacheUsed());
        System.out.println("get num block cached " + dockerHDFSCluster.getDataNodes().get(0).fetchFSDatasetGetNumBlocksCached());
        System.out.println("get info port " + dockerHDFSCluster.getDataNodes().get(0).fetchInfoPort());
        System.out.println("receive buffer size " + dockerHDFSCluster.getDataNodes().get(0).fetchXferServerGetPeerServerGetReceiveBufferSize());
        System.out.println("FSDatasetGetDfsUsed " + dockerHDFSCluster.getDataNodes().get(0).fetchFSDatasetGetDfsUsed());
        System.out.println("AddressHostName " + dockerHDFSCluster.getDataNodes().get(0).fetchIPCServerListenerAddressHostName());
        System.out.println("fetch IPC Port " + dockerHDFSCluster.getDataNodes().get(0).fetchIpcPort());
        System.out.println("balancer bandwidth " + dockerHDFSCluster.getDataNodes().get(0).fetchBalancerBandwidth());
    }

    @Test
    public void testMXBean() {
        MiniDockerDFSCluster dockerHDFSCluster = new MiniDockerDFSCluster.Builder(new Configuration()).numDataNodes(1).build();
        String host = "localhost"; // Updated to 'localhost'
        int port = 9090;

        // Create a JMX service URL
        String urlString = String.format(
                "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi",
                host, port
        );
        try {
            JMXServiceURL serviceURL = new JMXServiceURL(urlString);
            Map<String, Object> env = new HashMap<>();

            // Connect to the JMX server
            JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
            MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
            ObjectName mxbeanName = new ObjectName(
                    "Hadoop:service=NameNode,name=NameNodeStatus");

            // Get attribute "NNRole"
            String nnRole = (String)mbs.getAttribute(mxbeanName, "NNRole");
            //Assert.assertEquals(nn.getNNRole(), nnRole);
            System.out.println("NNRole: " + nnRole);
            dockerHDFSCluster.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JMX connection", e);
        }
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
