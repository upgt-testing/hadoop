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
package org.apache.hadoop.hdfs.tools;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedStripedBlock;
import org.apache.hadoop.hdfs.protocol.SystemErasureCodingPolicies;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpi;
import org.apache.hadoop.hdfs.util.StripedBlockUtil;
import org.apache.hadoop.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

import static org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDebugAdmin_RestartInjected {

  static private final String TEST_ROOT_DIR =
      new File(System.getProperty("test.build.data", "/tmp"),
          TestDebugAdmin_RestartInjected.class.getSimpleName()).getAbsolutePath();
  private Configuration conf = new Configuration();
  private MiniDFSCluster cluster;
  private DebugAdmin admin;

  @Before
  public void setUp() throws Exception {
    final File testRoot = new File(TEST_ROOT_DIR);
    testRoot.delete();
    testRoot.mkdirs();
    admin = new DebugAdmin(conf);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  private String runCmd(String[] cmd) throws Exception {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bytes);
    final PrintStream oldErr = System.err;
    final PrintStream oldOut = System.out;
    System.setErr(out);
    System.setOut(out);
    int ret;
    try {
      ret = admin.run(cmd);
    } finally {
      System.setErr(oldErr);
      System.setOut(oldOut);
      IOUtils.closeStream(out);
    }
    return "ret: " + ret + ", " +
        bytes.toString().replaceAll(System.lineSeparator(), "");
  }

  @Test(timeout = 60000)
  public void testRecoverLease() throws Exception {
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();
    RestartFramework.at("after_cluster_active")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    assertEquals("ret: 1, You must supply a -path argument to recoverLease.",
        runCmd(new String[]{"recoverLease", "-retries", "1"}));
    DistributedFileSystem fs = cluster.getFileSystem();
    FSDataOutputStream out = fs.create(new Path("/foo"));
    out.write(123);
    out.close();
    RestartFramework.at("after_file_creation")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    assertEquals("ret: 0, recoverLease SUCCEEDED on /foo",
        runCmd(new String[]{"recoverLease", "-path", "/foo"}));
    RestartFramework.at("after_recover_lease")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test(timeout = 60000)
  public void testVerifyMetaCommand() throws Exception {
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();
    RestartFramework.at("after_cluster_active")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    DistributedFileSystem fs = cluster.getFileSystem();
    DataNode datanode = cluster.getDataNodes().get(0);
    DFSTestUtil.createFile(fs, new Path("/bar"), 1234, (short) 1, 0xdeadbeef);
    RestartFramework.at("after_file_creation")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    FsDatasetSpi<?> fsd = datanode.getFSDataset();
    ExtendedBlock block = DFSTestUtil.getFirstBlock(fs, new Path("/bar"));
    RestartFramework.at("after_block_retrieval")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    File blockFile = getBlockFile(fsd,
        block.getBlockPoolId(), block.getLocalBlock());
    assertEquals("ret: 1, You must specify a meta file with -meta", runCmd(
        new String[] {"verifyMeta", "-block", blockFile.getAbsolutePath()}));
    File metaFile = getMetaFile(fsd,
        block.getBlockPoolId(), block.getLocalBlock());
    assertEquals("ret: 0, Checksum type: " +
          "DataChecksum(type=CRC32C, chunkSize=512)",
        runCmd(new String[]{"verifyMeta",
            "-meta", metaFile.getAbsolutePath()}));
    assertEquals("ret: 0, Checksum type: " +
          "DataChecksum(type=CRC32C, chunkSize=512)" +
          "Checksum verification succeeded on block file " +
          blockFile.getAbsolutePath(),
        runCmd(new String[]{"verifyMeta",
            "-meta", metaFile.getAbsolutePath(),
            "-block", blockFile.getAbsolutePath()})
    );
    RestartFramework.at("after_verify_meta")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test(timeout = 60000)
  public void testComputeMetaCommand() throws Exception {
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();
    RestartFramework.at("after_cluster_active")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    DistributedFileSystem fs = cluster.getFileSystem();
    DataNode datanode = cluster.getDataNodes().get(0);
    DFSTestUtil.createFile(fs, new Path("/bar"), 1234, (short) 1, 0xdeadbeef);
    RestartFramework.at("after_file_creation")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    FsDatasetSpi<?> fsd = datanode.getFSDataset();
    ExtendedBlock block = DFSTestUtil.getFirstBlock(fs, new Path("/bar"));
    RestartFramework.at("after_block_retrieval")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    File blockFile = getBlockFile(fsd,
        block.getBlockPoolId(), block.getLocalBlock());

    assertEquals("ret: 1, computeMeta -block <block-file> -out "
            + "<output-metadata-file>  Compute HDFS metadata from the specified"
            + " block file, and save it to  the specified output metadata file."
            + "**NOTE: Use at your own risk! If the block file is corrupt"
            + " and you overwrite it's meta file,  it will show up"
            + " as good in HDFS, but you can't read the data."
            + " Only use as a last measure, and when you are 100% certain"
            + " the block file is good.",
        runCmd(new String[] {"computeMeta"}));
    assertEquals("ret: 2, You must specify a block file with -block",
        runCmd(new String[] {"computeMeta", "-whatever"}));
    assertEquals("ret: 3, Block file <bla> does not exist or is not a file",
        runCmd(new String[] {"computeMeta", "-block", "bla"}));
    assertEquals("ret: 4, You must specify a output file with -out", runCmd(
        new String[] {"computeMeta", "-block", blockFile.getAbsolutePath()}));
    assertEquals("ret: 5, output file already exists!", runCmd(
        new String[] {"computeMeta", "-block", blockFile.getAbsolutePath(),
            "-out", blockFile.getAbsolutePath()}));

    File outFile = new File(TEST_ROOT_DIR, "out.meta");
    outFile.delete();
    assertEquals("ret: 0, Checksum calculation succeeded on block file " +
        blockFile.getAbsolutePath() + " saved metadata to meta file " +
        outFile.getAbsolutePath(), runCmd(new String[] {"computeMeta", "-block",
        blockFile.getAbsolutePath(), "-out", outFile.getAbsolutePath()}));
    RestartFramework.at("after_compute_meta")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    assertTrue(outFile.exists());
    assertTrue(outFile.length() > 0);
  }

  @Test(timeout = 60000)
  public void testRecoverLeaseforFileNotFound() throws Exception {
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();
    RestartFramework.at("after_cluster_active")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    assertTrue(runCmd(new String[] {
        "recoverLease", "-path", "/foo", "-retries", "2" }).contains(
        "Giving up on recoverLease for /foo after 1 try"));
    RestartFramework.at("after_command_execution")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test(timeout = 60000)
  public void testVerifyECCommand() throws Exception {
    final ErasureCodingPolicy ecPolicy = SystemErasureCodingPolicies.getByID(
        SystemErasureCodingPolicies.RS_3_2_POLICY_ID);
    cluster = DFSTestUtil.setupCluster(conf, 6, 5, 0);
    cluster.waitActive();
    RestartFramework.at("after_cluster_active")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    DistributedFileSystem fs = cluster.getFileSystem();

    assertEquals("ret: 1, verifyEC -file <file>  Verify HDFS erasure coding on " +
        "all block groups of the file.", runCmd(new String[]{"verifyEC"}));

    assertEquals("ret: 1, File /bar does not exist.",
        runCmd(new String[]{"verifyEC", "-file", "/bar"}));

    fs.create(new Path("/bar")).close();
    RestartFramework.at("after_regular_file_creation")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    assertEquals("ret: 1, File /bar is not erasure coded.",
        runCmd(new String[]{"verifyEC", "-file", "/bar"}));


    final Path ecDir = new Path("/ec");
    fs.mkdir(ecDir, FsPermission.getDirDefault());
    fs.enableErasureCodingPolicy(ecPolicy.getName());
    fs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    RestartFramework.at("after_ec_policy_setup")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    assertEquals("ret: 1, File /ec is not a regular file.",
        runCmd(new String[]{"verifyEC", "-file", "/ec"}));

    fs.create(new Path(ecDir, "foo"));
    RestartFramework.at("after_ec_file_open")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    assertEquals("ret: 1, File /ec/foo is not closed.",
        runCmd(new String[]{"verifyEC", "-file", "/ec/foo"}));

    final short repl = 1;
    final long k = 1024;
    final long m = k * k;
    final long seed = 0x1234567L;
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_65535"), 65535, repl, seed);
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_65535"})
        .contains("All EC block group status: OK"));
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_256k"), 256 * k, repl, seed);
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_256k"})
        .contains("All EC block group status: OK"));
    RestartFramework.at("after_small_ec_files")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_1m"), m, repl, seed);
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_1m"})
        .contains("All EC block group status: OK"));
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_2m"), 2 * m, repl, seed);
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_2m"})
        .contains("All EC block group status: OK"));
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_3m"), 3 * m, repl, seed);
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_3m"})
        .contains("All EC block group status: OK"));
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_5m"), 5 * m, repl, seed);
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_5m"})
        .contains("All EC block group status: OK"));
    RestartFramework.at("after_large_ec_files")
        .on(cluster)
        .restart("datanode")
        .withIndex(1)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    DFSTestUtil.createFile(fs, new Path(ecDir, "foo_6m"), (int) k, 6 * m, m, repl, seed);
    assertEquals("ret: 0, Checking EC block group: blk_x;Status: OK" +
            "Checking EC block group: blk_x;Status: OK" +
            "All EC block group status: OK",
        runCmd(new String[]{"verifyEC", "-file", "/ec/foo_6m"})
            .replaceAll("blk_-[0-9]+", "blk_x;"));
    RestartFramework.at("after_multiblock_ec_file")
        .on(cluster)
        .restart("datanode")
        .withIndex(2)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    Path corruptFile = new Path(ecDir, "foo_corrupt");
    DFSTestUtil.createFile(fs, corruptFile, 5841961, repl, seed);
    List<LocatedBlock> blocks = DFSTestUtil.getAllBlocks(fs, corruptFile);
    assertEquals(1, blocks.size());
    LocatedStripedBlock blockGroup = (LocatedStripedBlock) blocks.get(0);
    LocatedBlock[] indexedBlocks = StripedBlockUtil.parseStripedBlockGroup(blockGroup,
        ecPolicy.getCellSize(), ecPolicy.getNumDataUnits(), ecPolicy.getNumParityUnits());
    LocatedBlock toCorruptLocatedBlock = indexedBlocks[0];
    ExtendedBlock toCorruptBlock = toCorruptLocatedBlock.getBlock();
    DataNode datanode = cluster.getDataNode(toCorruptLocatedBlock.getLocations()[0].getIpcPort());
    File blockFile = getBlockFile(datanode.getFSDataset(),
        toCorruptBlock.getBlockPoolId(), toCorruptBlock.getLocalBlock());
    File metaFile = getMetaFile(datanode.getFSDataset(),
        toCorruptBlock.getBlockPoolId(), toCorruptBlock.getLocalBlock());
    byte[] errorBytes = new byte[2097152];
    new Random(seed).nextBytes(errorBytes);
    FileUtils.writeByteArrayToFile(blockFile, errorBytes);
    metaFile.delete();
    runCmd(new String[]{"computeMeta", "-block", blockFile.getAbsolutePath(),
        "-out", metaFile.getAbsolutePath()});
    RestartFramework.at("after_block_corruption")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    assertTrue(runCmd(new String[]{"verifyEC", "-file", "/ec/foo_corrupt"})
        .contains("Status: ERROR, message: EC compute result not match."));
  }

}
