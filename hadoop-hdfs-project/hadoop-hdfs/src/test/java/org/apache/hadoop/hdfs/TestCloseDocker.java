package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;

import static org.junit.Assert.fail;

/**
 * Author: Shuai Wang
 */
public class TestCloseDocker {
    @Test
    public void testWriteAfterClose() throws IOException {
        Configuration conf = new Configuration();
        //MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        //        .build();
        DockerHDFSCluster cluster = new DockerHDFSCluster.Builder(conf)     // <-------- First modification
                .build();

        try {
            final byte[] data = "foo".getBytes();

            //FileSystem fs = FileSystem.get(conf);                          // <--------- Second modification
            FileSystem fs = cluster.getFileSystem();
            OutputStream out = fs.create(new Path("/test"));

            // Start upgrade
            System.out.println("Before upgrade, you have 10 seconds to check the current datanode version");
            Thread.sleep(10000);
            // The datanode index starts from 0
            cluster.upgradeDataNode("hadoop:3.3.6", 0);
            System.out.println("After upgrade, you have 10 seconds to check the current datanode version");
            Thread.sleep(10000);
            // Finish upgrade


            out.write(data);
            out.close();
            try {
                // Should fail.
                out.write(data);
                fail("Should not have been able to write more data after file is closed.");
            } catch (ClosedChannelException cce) {
                // We got the correct exception. Ignoring.
            }
            // Should succeed. Double closes are OK.
            out.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (cluster != null) {
                cluster.shutdown();
            }
        }
    }
}
