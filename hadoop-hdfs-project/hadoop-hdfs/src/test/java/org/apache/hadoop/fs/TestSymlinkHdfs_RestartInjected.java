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
package org.apache.hadoop.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.web.WebHdfsConstants;
import org.apache.hadoop.hdfs.web.WebHdfsFileSystem;
import org.apache.hadoop.hdfs.web.WebHdfsTestUtil;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

/**
 * Test symbolic links in Hdfs.
 */
abstract public class TestSymlinkHdfs_RestartInjected extends SymlinkBaseTest {

  {
    GenericTestUtils.setLogLevel(NameNode.stateChangeLog, Level.ALL);
  }

  protected static MiniDFSCluster cluster;
  protected static WebHdfsFileSystem webhdfs;
  protected static DistributedFileSystem dfs;

  @Override
  protected String getScheme() {
    return "hdfs";
  }

  @Override
  protected String testBaseDir1() throws IOException {
    return "/test1";
  }
  
  @Override
  protected String testBaseDir2() throws IOException {
    return "/test2";
  }

  @Override
  protected URI testURI() {
    return cluster.getURI(0);
  }

  @Override
  protected IOException unwrapException(IOException e) {
    if (e instanceof RemoteException) {
      return ((RemoteException)e).unwrapRemoteException();
    }
    return e;
  }

  @BeforeClass
  public static void beforeClassSetup() throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.set(FsPermission.UMASK_LABEL, "000");
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_MAX_COMPONENT_LENGTH_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).build();
    webhdfs = WebHdfsTestUtil.getWebHdfsFileSystem(conf, WebHdfsConstants.WEBHDFS_SCHEME);
    dfs = cluster.getFileSystem();
  }

  @AfterClass
  public static void afterClassTeardown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  @Test
  /** Access a file using a link that spans Hdfs to LocalFs */
  public void testLinkAcrossFileSystems() throws IOException {
    Path localDir = new Path("file://" + wrapper.getAbsoluteTestRootDir()
        + "/test");
    Path localFile = new Path("file://" + wrapper.getAbsoluteTestRootDir()
        + "/test/file");
    Path link      = new Path(testBaseDir1(), "linkToFile");
    FSTestWrapper localWrapper = wrapper.getLocalFSWrapper();
    localWrapper.delete(localDir, true);
    localWrapper.mkdir(localDir, FileContext.DEFAULT_PERM, true);
    localWrapper.setWorkingDirectory(localDir);
    assertEquals(localDir, localWrapper.getWorkingDirectory());
    createAndWriteFile(localWrapper, localFile);

    RestartFramework.at("after_create_local_file")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.createSymlink(localFile, link, false);

    RestartFramework.at("after_create_symlink")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    readFile(link);
    assertEquals(fileSize, wrapper.getFileStatus(link).getLen());
  }

  @Test
  /** Test renaming a file across two file systems using a link */
  public void testRenameAcrossFileSystemsViaLink() throws IOException {
    Path localDir = new Path("file://" + wrapper.getAbsoluteTestRootDir()
        + "/test");
    Path hdfsFile    = new Path(testBaseDir1(), "file");
    Path link        = new Path(testBaseDir1(), "link");
    Path hdfsFileNew = new Path(testBaseDir1(), "fileNew");
    Path hdfsFileNewViaLink = new Path(link, "fileNew");
    FSTestWrapper localWrapper = wrapper.getLocalFSWrapper();
    localWrapper.delete(localDir, true);
    localWrapper.mkdir(localDir, FileContext.DEFAULT_PERM, true);
    localWrapper.setWorkingDirectory(localDir);
    createAndWriteFile(hdfsFile);

    RestartFramework.at("after_create_hdfs_file")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.createSymlink(localDir, link, false);

    RestartFramework.at("after_create_symlink_to_dir")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    // Rename hdfs://test1/file to hdfs://test1/link/fileNew
    // which renames to file://TEST_ROOT/test/fileNew which
    // spans AbstractFileSystems and therefore fails.
    try {
      wrapper.rename(hdfsFile, hdfsFileNewViaLink);
      fail("Renamed across file systems");
    } catch (InvalidPathException ipe) {
      // Expected from FileContext
    } catch (IllegalArgumentException e) {
      // Expected from Filesystem
      GenericTestUtils.assertExceptionContains("Wrong FS: ", e);
    }
    // Now rename hdfs://test1/link/fileNew to hdfs://test1/fileNew
    // which renames file://TEST_ROOT/test/fileNew to hdfs://test1/fileNew
    // which spans AbstractFileSystems and therefore fails.
    createAndWriteFile(hdfsFileNewViaLink);
    try {
      wrapper.rename(hdfsFileNewViaLink, hdfsFileNew);
      fail("Renamed across file systems");
    } catch (InvalidPathException ipe) {
      // Expected from FileContext
    } catch (IllegalArgumentException e) {
      // Expected from Filesystem
      GenericTestUtils.assertExceptionContains("Wrong FS: ", e);
    }
  }

  @Test
  /** Test create symlink to / */
  public void testCreateLinkToSlash() throws IOException {
    Path dir  = new Path(testBaseDir1());
    Path file = new Path(testBaseDir1(), "file");
    Path link = new Path(testBaseDir1(), "linkToSlash");
    Path fileViaLink = new Path(testBaseDir1()+"/linkToSlash"+
                                testBaseDir1()+"/file");
    createAndWriteFile(file);

    RestartFramework.at("after_create_file")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.setWorkingDirectory(dir);
    wrapper.createSymlink(new Path("/"), link, false);

    RestartFramework.at("after_create_symlink_to_slash")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    readFile(fileViaLink);
    assertEquals(fileSize, wrapper.getFileStatus(fileViaLink).getLen());
    // Ditto when using another file context since the file system
    // for the slash is resolved according to the link's parent.
    if (wrapper instanceof FileContextTestWrapper) {
      FSTestWrapper localWrapper = wrapper.getLocalFSWrapper();
      Path linkQual = new Path(cluster.getURI(0).toString(), fileViaLink);
      assertEquals(fileSize, localWrapper.getFileStatus(linkQual).getLen());
    }
  }
  
  
  @Test
  /** setPermission affects the target not the link */
  public void testSetPermissionAffectsTarget() throws IOException {
    Path file       = new Path(testBaseDir1(), "file");
    Path dir        = new Path(testBaseDir2());
    Path linkToFile = new Path(testBaseDir1(), "linkToFile");
    Path linkToDir  = new Path(testBaseDir1(), "linkToDir");
    createAndWriteFile(file);

    RestartFramework.at("after_create_file_for_permission")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.createSymlink(file, linkToFile, false);
    wrapper.createSymlink(dir, linkToDir, false);

    RestartFramework.at("after_create_symlinks")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    // Changing the permissions using the link does not modify
    // the permissions of the link..
    FsPermission perms = wrapper.getFileLinkStatus(linkToFile).getPermission();
    wrapper.setPermission(linkToFile, new FsPermission((short)0664));
    wrapper.setOwner(linkToFile, "user", "group");

    RestartFramework.at("after_set_permissions")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    assertEquals(perms, wrapper.getFileLinkStatus(linkToFile).getPermission());
    // but the file's permissions were adjusted appropriately
    FileStatus stat = wrapper.getFileStatus(file);
    assertEquals(0664, stat.getPermission().toShort());
    assertEquals("user", stat.getOwner());
    assertEquals("group", stat.getGroup());
    // Getting the file's permissions via the link is the same
    // as getting the permissions directly.
    assertEquals(stat.getPermission(), 
                 wrapper.getFileStatus(linkToFile).getPermission());

    // Ditto for a link to a directory
    perms = wrapper.getFileLinkStatus(linkToDir).getPermission();
    wrapper.setPermission(linkToDir, new FsPermission((short)0664));
    wrapper.setOwner(linkToDir, "user", "group");
    assertEquals(perms, wrapper.getFileLinkStatus(linkToDir).getPermission());
    stat = wrapper.getFileStatus(dir);
    assertEquals(0664, stat.getPermission().toShort());
    assertEquals("user", stat.getOwner());
    assertEquals("group", stat.getGroup());
    assertEquals(stat.getPermission(), 
                 wrapper.getFileStatus(linkToDir).getPermission());
  }  

  @Test
  /** Create a symlink using a path with scheme but no authority */
  public void testCreateWithPartQualPathFails() throws IOException {
    Path fileWoAuth = new Path("hdfs:///test/file");
    Path linkWoAuth = new Path("hdfs:///test/link");

    RestartFramework.at("before_test_partial_path")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    try {
      createAndWriteFile(fileWoAuth);
      fail("HDFS requires URIs with schemes have an authority");
    } catch (RuntimeException e) {
      // Expected
    }
    try {
      wrapper.createSymlink(new Path("foo"), linkWoAuth, false);
      fail("HDFS requires URIs with schemes have an authority");
    } catch (RuntimeException e) {
      // Expected
    }
  }

  @Test
  /** setReplication affects the target not the link */
  public void testSetReplication() throws IOException {
    Path file = new Path(testBaseDir1(), "file");
    Path link = new Path(testBaseDir1(), "linkToFile");
    createAndWriteFile(file);

    RestartFramework.at("after_create_file_for_replication")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.createSymlink(file, link, false);

    RestartFramework.at("after_create_symlink_for_replication")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.setReplication(link, (short)2);
    assertEquals(0, wrapper.getFileLinkStatus(link).getReplication());
    assertEquals(2, wrapper.getFileStatus(link).getReplication());
    assertEquals(2, wrapper.getFileStatus(file).getReplication());
  }
  
  @Test
  /** Test create symlink with a max len name */
  public void testCreateLinkMaxPathLink() throws IOException {
    Path dir  = new Path(testBaseDir1());
    Path file = new Path(testBaseDir1(), "file");
    final int maxPathLen = HdfsServerConstants.MAX_PATH_LENGTH;
    final int dirLen     = dir.toString().length() + 1;
    int   len            = maxPathLen - dirLen;
    
    // Build a MAX_PATH_LENGTH path
    StringBuilder sb = new StringBuilder("");
    for (int i = 0; i < (len / 10); i++) {
      sb.append("0123456789");
    }
    for (int i = 0; i < (len % 10); i++) {
      sb.append("x");
    }
    Path link = new Path(sb.toString());
    assertEquals(maxPathLen, dirLen + link.toString().length()); 
    
    // Check that it works
    createAndWriteFile(file);

    RestartFramework.at("after_create_file_for_max_path")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.setWorkingDirectory(dir);
    wrapper.createSymlink(file, link, false);

    RestartFramework.at("after_create_max_path_symlink")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    readFile(link);
    
    // Now modify the path so it's too large
    link = new Path(sb.toString()+"x");
    try {
      wrapper.createSymlink(file, link, false);
      fail("Path name should be too long");
    } catch (IOException x) {
      // Expected
    }
  }

  @Test
  /** Test symlink owner */
  public void testLinkOwner() throws IOException {
    Path file = new Path(testBaseDir1(), "file");
    Path link = new Path(testBaseDir1(), "symlinkToFile");
    createAndWriteFile(file);

    RestartFramework.at("after_create_file_for_owner")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.createSymlink(file, link, false);

    RestartFramework.at("after_create_symlink_for_owner")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    FileStatus statFile = wrapper.getFileStatus(file);
    FileStatus statLink = wrapper.getFileStatus(link);
    assertEquals(statLink.getOwner(), statFile.getOwner());
  }

  @Test
  /** Test WebHdfsFileSystem.createSymlink(..). */
  public void testWebHDFS() throws IOException {
    Path file = new Path(testBaseDir1(), "file");
    Path link = new Path(testBaseDir1(), "linkToFile");
    createAndWriteFile(file);

    RestartFramework.at("after_create_file_for_webhdfs")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    webhdfs.createSymlink(file, link, false);

    RestartFramework.at("after_create_webhdfs_symlink")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    wrapper.setReplication(link, (short)2);
    assertEquals(0, wrapper.getFileLinkStatus(link).getReplication());
    assertEquals(2, wrapper.getFileStatus(link).getReplication());
    assertEquals(2, wrapper.getFileStatus(file).getReplication());
  }

  @Test
  /** Test craeteSymlink(..) with quota. */
  public void testQuota() throws IOException {
    final Path dir = new Path(testBaseDir1());
    dfs.setQuota(dir, 3, HdfsConstants.QUOTA_DONT_SET);

    RestartFramework.at("after_set_quota")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    final Path file = new Path(dir, "file");
    createAndWriteFile(file);

    RestartFramework.at("after_create_file_for_quota")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    //creating the first link should succeed
    final Path link1 = new Path(dir, "link1");
    wrapper.createSymlink(file, link1, false);

    RestartFramework.at("after_create_first_symlink")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    try {
      //creating the second link should fail with QuotaExceededException.
      final Path link2 = new Path(dir, "link2");
      wrapper.createSymlink(file, link2, false);
      fail("Created symlink despite quota violation");
    } catch(QuotaExceededException qee) {
      //expected
    }
  }
}
