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

import static org.apache.hadoop.fs.viewfs.Constants.CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME;
import static org.apache.hadoop.fs.viewfs.Constants.CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME_DEFAULT;
import static org.junit.Assume.assumeNotNull;

import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestViewFileSystemOverloadSchemeHdfsFileSystemContract}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Only testRenameRootDirForbidden is transformed.
 *
 * @see TestViewFileSystemOverloadSchemeHdfsFileSystemContract Original test using MiniDFSCluster
 */
public class TestViewFileSystemOverloadSchemeHdfsFileSystemContract_ProcessBased {

  @Test(expected = AccessControlException.class, timeout = 60000)
  public void testRenameRootDirForbidden() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    Configuration conf = new HdfsConfiguration();
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "062");

    ProcessBasedMiniDFSCluster cluster = null;
    try {
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
          .numDataNodes(2)
          .allNodesHadoopDistribution(hadoopHome)
          .format(true)
          .build();
      cluster.waitClusterUp();

      // Configure ViewFileSystemOverloadScheme
      conf.set(String.format("fs.%s.impl", "hdfs"),
          ViewFileSystemOverloadScheme.class.getName());
      conf.set(String.format(
          FsConstants.FS_VIEWFS_OVERLOAD_SCHEME_TARGET_FS_IMPL_PATTERN,
          "hdfs"),
          DistributedFileSystem.class.getName());
      conf.setBoolean(CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME,
          CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME_DEFAULT);

      URI defaultFSURI =
          URI.create(conf.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY));
      String defaultWorkingDirectory =
          "/user/" + UserGroupInformation.getCurrentUser().getShortUserName();

      ConfigUtil.addLink(conf, defaultFSURI.getAuthority(), "/user",
          defaultFSURI);
      ConfigUtil.addLink(conf, defaultFSURI.getAuthority(),
          "/FileSystemContractBaseTest/",
          new URI(defaultFSURI.toString() + "/FileSystemContractBaseTest/"));

      FileSystem fs = FileSystem.get(conf);

      // Test that renaming root directory is forbidden
      // This should throw AccessControlException
      fs.rename(new Path("/"), new Path("/testRenameRootDirForbidden"));
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }
}
