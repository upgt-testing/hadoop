package org.apache.hadoop.hdfs;

import edu.illinois.util.config.ConfigTracker;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public void testMXBean() {
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
