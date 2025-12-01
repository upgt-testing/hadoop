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
package org.apache.hadoop.hdfs.server.namenode;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.apache.hadoop.hdfs.server.common.Util.fileAsURI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory;
import org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeDirType;
import org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile;
import org.apache.hadoop.util.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestStartup_RestartInjected {
  public static final String NAME_NODE_HOST = "localhost:";
  public static final String WILDCARD_HTTP_HOST = "0.0.0.0:";
  private static final org.slf4j.Logger LOG =
      LoggerFactory.getLogger(TestStartup_RestartInjected.class.getName());
  private Configuration config;
  private File hdfsDir=null;

  @Before
  public void setUp() throws Exception {
    config = new HdfsConfiguration();
    hdfsDir = new File(MiniDFSCluster.getBaseDirectory());

    if (hdfsDir.exists() && !FileUtil.fullyDelete(hdfsDir)) {
      throw new IOException("Could not delete hdfs directory '" + hdfsDir + "'");
    }
    LOG.info("--hdfsdir is " + hdfsDir.getAbsolutePath());
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_DATANODE_DATA_DIR_KEY,
        new File(hdfsDir, "data").getPath());
    config.set(DFSConfigKeys.DFS_DATANODE_ADDRESS_KEY, "0.0.0.0:0");
    config.set(DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_KEY, "0.0.0.0:0");
    config.set(DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_KEY, "0.0.0.0:0");
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "secondary")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_SECONDARY_HTTP_ADDRESS_KEY,
	       WILDCARD_HTTP_HOST + "0");
    config.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);

    FileSystem.setDefaultUri(config, "hdfs://"+NAME_NODE_HOST + "0");
  }

  @After
  public void tearDown() throws Exception {
    if ( hdfsDir.exists() && !FileUtil.fullyDelete(hdfsDir) ) {
      throw new IOException("Could not delete hdfs directory in tearDown '" + hdfsDir + "'");
    }
  }

  private void verifyDifferentDirs(FSImage img, long expectedImgSize, long expectedEditsSize) {
    StorageDirectory sd =null;
    for (Iterator<StorageDirectory> it = img.getStorage().dirIterator(); it.hasNext();) {
      sd = it.next();

      if(sd.getStorageDirType().isOfType(NameNodeDirType.IMAGE)) {
        img.getStorage();
        File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
        LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length() + "; expected = " + expectedImgSize);
        assertEquals(expectedImgSize, imf.length());
      } else if(sd.getStorageDirType().isOfType(NameNodeDirType.EDITS)) {
        img.getStorage();
        File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
        LOG.info("-- edits file " + edf.getAbsolutePath() + "; len = " + edf.length()  + "; expected = " + expectedEditsSize);
        assertEquals(expectedEditsSize, edf.length());
      } else {
        fail("Image/Edits directories are not different");
      }
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_NAMENODE_GRACEFUL() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_NAMENODE_CRASH() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_SINGLE_DATANODE_GRACEFUL() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_SINGLE_DATANODE_CRASH() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_ALL_DATANODES_GRACEFUL() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_ALL_DATANODES_CRASH() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_RANDOM_DATANODE_GRACEFUL() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_RANDOM_DATANODE_CRASH() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_NAMENODE_AND_DATANODES_GRACEFUL() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }

  @Test
  public void testSNNStartup_AfterCheckpoint_NAMENODE_AND_DATANODES_CRASH() throws Exception{
    LOG.info("--starting SecondNN startup test");
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "name")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_EDITS_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt_edits")).toString());
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        fileAsURI(new File(hdfsDir, "chkpt")).toString());

    LOG.info("--starting NN ");
    MiniDFSCluster cluster = null;
    SecondaryNameNode sn = null;
    NameNode nn = null;
    try {
      cluster = new MiniDFSCluster.Builder(config).manageDataDfsDirs(false)
                                                  .manageNameDfsDirs(false)
                                                  .build();
      cluster.waitActive();
      nn = cluster.getNameNode();
      assertNotNull(nn);

      LOG.info("--starting SecondNN");
      sn = new SecondaryNameNode(config);
      assertNotNull(sn);

      LOG.info("--doing checkpoint");
      sn.doCheckpoint();
      LOG.info("--done checkpoint");

      FileSystem fs = cluster.getFileSystem();
      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      FSImage image = nn.getFSImage();
      StorageDirectory sd = image.getStorage().getStorageDir(0);
      assertEquals(sd.getStorageDirType(), NameNodeDirType.IMAGE_AND_EDITS);
      image.getStorage();
      File imf = NNStorage.getStorageFile(sd, NameNodeFile.IMAGE, 0);
      image.getStorage();
      File edf = NNStorage.getStorageFile(sd, NameNodeFile.EDITS, 0);
      LOG.info("--image file " + imf.getAbsolutePath() + "; len = " + imf.length());
      LOG.info("--edits file " + edf.getAbsolutePath() + "; len = " + edf.length());

      FSImage chkpImage = sn.getFSImage();
      verifyDifferentDirs(chkpImage, imf.length(), edf.length());

    } catch (IOException e) {
      fail(StringUtils.stringifyException(e));
      System.err.println("checkpoint failed");
      throw e;
    } finally {
      if(sn!=null)
        sn.shutdown();
      if(cluster!=null)
        cluster.shutdown();
    }
  }
}
