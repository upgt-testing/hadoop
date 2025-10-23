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
package org.apache.hadoop.fs.viewfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.test.PathUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.hadoop.fs.viewfs.Constants.CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME;
import static org.apache.hadoop.fs.viewfs.Constants.CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME_DEFAULT;
import static org.junit.Assume.assumeNotNull;


/**
 * Tests ViewFileSystemOverloadScheme with configured mount links.
 * ProcessBased version for upgrade testing.
 */
public class TestViewFileSystemOverloadSchemeWithHdfsScheme_ProcessBased {
  private static final String TEST_STRING = "Hello ViewFSOverloadedScheme!";
  private static final String FS_IMPL_PATTERN_KEY = "fs.%s.impl";
  private static final String HDFS_SCHEME = "hdfs";
  private Configuration conf = null;
  private static ProcessBasedMiniDFSCluster cluster = null;
  private URI defaultFSURI;
  private File localTargetDir;
  private static final String TEST_ROOT_DIR = PathUtils
      .getTestDirName(TestViewFileSystemOverloadSchemeWithHdfsScheme_ProcessBased.class);
  private static final String HDFS_USER_FOLDER = "/HDFSUser";
  private static final String LOCAL_FOLDER = "/local";
  private static Configuration baseConf;

  @BeforeClass
  public static void init() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    baseConf = new Configuration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(baseConf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();
  }

  /**
   * Sets up the configurations and starts the MiniDFSCluster.
   */
  @Before
  public void setUp() throws IOException {
    // Reset FileSystem cache to make sure no FS instances are cached with DistributedFileSysten instance.
    FileSystem.closeAll();
    
    Configuration config = getNewConf();
    config.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECT_MAX_RETRIES_KEY, 1);
    config.set(String.format(FS_IMPL_PATTERN_KEY, HDFS_SCHEME),
        ViewFileSystemOverloadScheme.class.getName());
    config.setBoolean(CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME,
        CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME_DEFAULT);
    setConf(config);
    defaultFSURI =
        URI.create(config.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY));
    localTargetDir = new File(TEST_ROOT_DIR, "/root/");
    localTargetDir.mkdirs();
    Assert.assertEquals(HDFS_SCHEME, defaultFSURI.getScheme()); // hdfs scheme.
  }

  @After
  public void cleanUp() throws IOException {
    if (cluster != null) {
      FileSystem fs = new DistributedFileSystem();
      fs.initialize(defaultFSURI, conf);
      try {
        FileStatus[] statuses = fs.listStatus(new Path("/"));
        for (FileStatus st : statuses) {
          Assert.assertTrue(fs.delete(st.getPath(), true));
        }
      } finally {
        fs.close();
      }
      FileSystem.closeAll();
    }
  }

  @AfterClass
  public static void tearDown() throws IOException {
    if (cluster != null) {
      FileSystem.closeAll();
      cluster.shutdown();
    }
  }

  /**
   * Adds the given mount links to config. sources contains mount link src and
   * the respective index location in targets contains the target uri.
   */
  void addMountLinks(String mountTable, String[] sources, String[] targets,
      Configuration config) throws IOException, URISyntaxException {
    ViewFsTestSetup.addMountLinksToConf(mountTable, sources, targets, config);
  }

  /**
   * Create mount links as follows
   * hdfs://localhost:xxx/HDFSUser --> hdfs://localhost:xxx/HDFSUser/
   * hdfs://localhost:xxx/local --> file://TEST_ROOT_DIR/root/
   * fallback --> hdfs://localhost:xxx/HDFSUser/
   *
   * Note: Above links created because to make fs initialization success.
   * Otherwise will not proceed if no mount links.
   *
   * Unset fs.viewfs.overload.scheme.target.hdfs.impl property.
   * So, OverloadScheme target fs initialization will fail.
   */
  @Test(expected = IOException.class, timeout = 30000)
  public void testInvalidOverloadSchemeTargetFS() throws Exception {
    final Path hdfsTargetPath = new Path(defaultFSURI + HDFS_USER_FOLDER);
    String mountTableIfSet = conf.get(Constants.CONFIG_VIEWFS_MOUNTTABLE_PATH);
    conf = new Configuration();
    if (mountTableIfSet != null) {
      conf.set(Constants.CONFIG_VIEWFS_MOUNTTABLE_PATH, mountTableIfSet);
    }
    addMountLinks(defaultFSURI.getHost(),
        new String[] {HDFS_USER_FOLDER, LOCAL_FOLDER,
            Constants.CONFIG_VIEWFS_LINK_FALLBACK },
        new String[] {hdfsTargetPath.toUri().toString(),
            localTargetDir.toURI().toString(),
            hdfsTargetPath.toUri().toString() },
        conf);
    conf.set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY,
        defaultFSURI.toString());
    conf.set(String.format(FS_IMPL_PATTERN_KEY, HDFS_SCHEME),
        ViewFileSystemOverloadScheme.class.getName());
    conf.unset(String.format(
        FsConstants.FS_VIEWFS_OVERLOAD_SCHEME_TARGET_FS_IMPL_PATTERN,
        HDFS_SCHEME));

    try (FileSystem fs = FileSystem.get(conf)) {
      fs.createNewFile(new Path("/onRootWhenFallBack"));
      Assert.fail("OverloadScheme target fs should be valid.");
    }
  }

  /**
   * Create mount links as follows
   * hdfs://localhost:xxx/HDFSUser0 --> hdfs://localhost:xxx/HDFSUser/
   * hdfs://localhost:xxx/HDFSUser1 --> hdfs://localhost:xxx/HDFSUser/
   *
   * 1. With cache, only one hdfs child file system instance should be there.
   * 2. Without cache, there should 2 hdfs instances.
   */
  @Test(timeout = 30000)
  public void testViewFsOverloadSchemeWithInnerCache()
      throws Exception {
    final Path hdfsTargetPath = new Path(defaultFSURI + HDFS_USER_FOLDER);
    addMountLinks(defaultFSURI.getAuthority(),
        new String[] {HDFS_USER_FOLDER + 0, HDFS_USER_FOLDER + 1 },
        new String[] {hdfsTargetPath.toUri().toString(),
            hdfsTargetPath.toUri().toString() },
        conf);

    // 1. Only 1 hdfs child file system should be there with cache.
    try (FileSystem vfs = FileSystem.get(conf)) {
      Assert.assertEquals(1, vfs.getChildFileSystems().length);
    }

    // 2. Two hdfs file systems should be there if no cache.
    conf.setBoolean(Constants.CONFIG_VIEWFS_ENABLE_INNER_CACHE, false);
    try (FileSystem vfs = FileSystem.get(conf)) {
      Assert.assertEquals(isFallBackExist(conf) ? 3 : 2,
          vfs.getChildFileSystems().length);
    }
  }

  // HDFS-15529: if any extended tests added fallback, then getChildFileSystems
  // will include fallback as well.
  private boolean isFallBackExist(Configuration config) {
    return config.get(ConfigUtil.getConfigViewFsPrefix(defaultFSURI
        .getAuthority()) + "." + Constants.CONFIG_VIEWFS_LINK_FALLBACK) != null;
  }

  /**
   * Create mount links as follows
   * hdfs://localhost:xxx/HDFSUser0 --> hdfs://localhost:xxx/HDFSUser/
   * hdfs://localhost:xxx/HDFSUser1 --> hdfs://localhost:xxx/HDFSUser/
   *
   * When InnerCache disabled, all matching ViewFileSystemOverloadScheme
   * initialized scheme file systems would not use FileSystem cache.
   */
  @Test(timeout = 3000)
  public void testViewFsOverloadSchemeWithNoInnerCacheAndHdfsTargets()
      throws Exception {
    final Path hdfsTargetPath = new Path(defaultFSURI + HDFS_USER_FOLDER);
    addMountLinks(defaultFSURI.getAuthority(),
        new String[] {HDFS_USER_FOLDER + 0, HDFS_USER_FOLDER + 1 },
        new String[] {hdfsTargetPath.toUri().toString(),
            hdfsTargetPath.toUri().toString() },
        conf);

    conf.setBoolean(Constants.CONFIG_VIEWFS_ENABLE_INNER_CACHE, false);
    // Two hdfs file systems should be there if no cache.
    try (FileSystem vfs = FileSystem.get(conf)) {
      Assert.assertEquals(isFallBackExist(conf) ? 3 : 2,
          vfs.getChildFileSystems().length);
    }
  }

  /**
   * Create mount links as follows
   * hdfs://localhost:xxx/local0 --> file://localPath/
   * hdfs://localhost:xxx/local1 --> file://localPath/
   *
   * When InnerCache disabled, all non matching ViewFileSystemOverloadScheme
   * initialized scheme file systems should continue to take advantage of
   * FileSystem cache.
   */
  @Test(timeout = 3000)
  public void testViewFsOverloadSchemeWithNoInnerCacheAndLocalSchemeTargets()
      throws Exception {
    final Path localTragetPath = new Path(localTargetDir.toURI());
    addMountLinks(defaultFSURI.getAuthority(),
        new String[] {LOCAL_FOLDER + 0, LOCAL_FOLDER + 1 },
        new String[] {localTragetPath.toUri().toString(),
            localTragetPath.toUri().toString() },
        conf);

    // Only one local file system should be there if no InnerCache, but fs
    // cache should work.
    conf.setBoolean(Constants.CONFIG_VIEWFS_ENABLE_INNER_CACHE, false);
    try (FileSystem vfs = FileSystem.get(conf)) {
      Assert.assertEquals(isFallBackExist(conf) ? 2 : 1,
          vfs.getChildFileSystems().length);
    }
  }

  /**
   * @return configuration.
   */
  public Configuration getConf() {
    return this.conf;
  }

  /**
   * @return configuration.
   */
  public Configuration getNewConf() {
    return new Configuration(baseConf);
  }

  /**
   * sets configuration.
   */
  public void setConf(Configuration config) {
    conf = config;
  }
}
