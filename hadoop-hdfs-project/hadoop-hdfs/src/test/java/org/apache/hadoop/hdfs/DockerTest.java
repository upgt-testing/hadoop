package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Shuai Wang
 */
public class DockerTest {
    @Test
    public void testFileSystem() {
        try {
            DockerHDFSCluster dockerHDFSCluster = new DockerHDFSCluster(new Configuration());
            dockerHDFSCluster.startCluster(2);

            FileSystem fs = dockerHDFSCluster.getFileSystem();
            // check whether the file system is functional
            System.out.println("FileSystem URI: " + fs.getUri());
            System.out.println("Home directory: " + fs.getHomeDirectory());
            System.out.println("Working directory: " + fs.getWorkingDirectory());

            Path newDir = new Path("/new_directory");
            fs.mkdirs(newDir);

            // create a new file in the new directory
            Path newFile = new Path("/new_directory/new_file");
            try (OutputStream os = fs.create(newFile)) {
                os.write("UIUC".getBytes());
            }

            // list and print the content of the new directory
            System.out.println("Content of /new_directory:");
            for (FileStatus p : fs.listStatus(newDir)) {
                System.out.println(p.getPath());
            }

            // read the content of the new file
            try (InputStream is = fs.open(newFile);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                System.out.println("Content of /new_directory/new_file:");
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // delete the new file
            fs.delete(newFile, false);
            // delete the new directory
            fs.delete(newDir, false);

            // close the file system
            fs.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create file system", e);
        }
    }

    @Test
    public void testStartDockerCluster() {
        try {
            DockerHDFSCluster dockerHDFSCluster = new DockerHDFSCluster(new Configuration());
            dockerHDFSCluster.startCluster(2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testStartNameNodeFromCluster() {
         try {
             DockerHDFSCluster dockerHDFSCluster = new DockerHDFSCluster(new Configuration());
             GenericContainer<?> namenode = dockerHDFSCluster.startNameNode();
             GenericContainer<?> datanode1 = dockerHDFSCluster.startDataNode("1");
             GenericContainer<?> datanode2 = dockerHDFSCluster.startDataNode("2");
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    public GenericContainer<?> createDataNode(Network network, int ID) {
        List<String> dataNodePortBindings = new ArrayList<>();
        //dataNodePortBindings.add("50010:50010");
        //dataNodePortBindings.add("50075:50075");
        //dataNodePortBindings.add("50020:50020");
        // now let's start a datanode
        GenericContainer<?> datanode;
        try {
            datanode = new GenericContainer<>("hadoop:3.3.6")
                    .withNetwork(network)
                    .withNetworkAliases("datanode" + ID)
                    .withEnv("CLUSTER_NAME", "hdfs-cluster")
                    .withEnv("DATANODE_ID", String.valueOf(ID))
                    .withCommand("bash", "-c", "hdfs datanode")
                    .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                    //.withExposedPorts(50010, 50075, 50020)
                    .withAccessToHost(true);

            //datanode.setPortBindings(dataNodePortBindings);
            datanode.start();
            System.out.println("Datanode started");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for the cluster to start", e);
            }
            return datanode;
            //System.out.println("Datanode stopped");
            //datanode.stop();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void testStartNameNode() {
        Network network = Network.builder().build();
        List<String> nameNodePortBindings = new ArrayList<>();
        nameNodePortBindings.add("50070:50070");
        nameNodePortBindings.add("9000:9000");
        nameNodePortBindings.add("9870:9870");
        try (GenericContainer<?> namenode = new GenericContainer<>("hadoop:3.3.6")
                .withNetwork(network)
                .withNetworkAliases("namenode")
                .withEnv("CLUSTER_NAME", "hdfs-cluster")
                .withEnv("NAMENODE_ID", "0")
                .withCommand("bash", "-c", "hdfs namenode -format && hdfs namenode")
                .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                .withExposedPorts(9000, 50070)
                .withAccessToHost(true)
                ) {
            namenode.setPortBindings(nameNodePortBindings);
            namenode.start();

            // Get the mapped port for Namenode web interface
            Integer mappedPort = namenode.getMappedPort(50070);

            // Access the Namenode web interface
            String address = "http://" + namenode.getHost() + ":" + mappedPort;
            System.out.println("Namenode web interface is accessible at: " + address);

            // Add your test logic here to interact with the Namenode web interface
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for the cluster to start", e);
            }

            GenericContainer<?> datanode = createDataNode(network, 1);
            GenericContainer<?> datanode2 = createDataNode(network, 2);

            datanode.stop();
            datanode2.stop();
            System.out.println("Namenode stopped");
            namenode.stop();
        }
    }
}
