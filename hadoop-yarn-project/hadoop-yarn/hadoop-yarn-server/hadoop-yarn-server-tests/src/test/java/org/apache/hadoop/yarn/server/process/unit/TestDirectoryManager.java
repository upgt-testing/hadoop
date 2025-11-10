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

package org.apache.hadoop.yarn.server.process.unit;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.yarn.server.process.DirectoryManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for DirectoryManager.
 */
public class TestDirectoryManager {

  private DirectoryManager dirManager;

  @Before
  public void setUp() {
    dirManager = new DirectoryManager();
  }

  @After
  public void tearDown() throws IOException {
    if (dirManager != null) {
      dirManager.cleanup();
    }
  }

  @Test
  public void testCreateClusterRoot() throws IOException {
    File root = dirManager.createClusterRoot();

    Assert.assertNotNull("Cluster root should not be null", root);
    Assert.assertTrue("Cluster root should exist", root.exists());
    Assert.assertTrue("Cluster root should be directory", root.isDirectory());
    Assert.assertTrue("Cluster root name should contain prefix",
        root.getName().startsWith("process-miniyarn-"));
  }

  @Test
  public void testCreateClusterRootWithBaseDir() throws IOException {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File root = dirManager.createClusterRoot(baseDir);

    Assert.assertNotNull("Cluster root should not be null", root);
    Assert.assertTrue("Cluster root should exist", root.exists());
    Assert.assertTrue("Cluster root should be in base dir",
        root.getParentFile().equals(baseDir));
  }

  @Test
  public void testCreateClusterRootIdempotent() throws IOException {
    File root1 = dirManager.createClusterRoot();
    File root2 = dirManager.createClusterRoot();

    Assert.assertEquals("Should return same root on repeated calls",
        root1, root2);
  }

  @Test
  public void testCreateResourceManagerDir() throws IOException {
    dirManager.createClusterRoot();
    File rmDir = dirManager.createResourceManagerDir(0);

    Assert.assertNotNull("RM dir should not be null", rmDir);
    Assert.assertTrue("RM dir should exist", rmDir.exists());
    Assert.assertTrue("RM dir should be directory", rmDir.isDirectory());
    Assert.assertTrue("RM dir name should be rm0",
        rmDir.getName().equals("rm0"));

    // Verify subdirectories
    File confDir = new File(rmDir, "conf");
    File dataDir = new File(rmDir, "data");
    File logsDir = new File(rmDir, "logs");
    File stateStoreDir = new File(rmDir, "data/rm-state-store");

    Assert.assertTrue("conf dir should exist", confDir.exists());
    Assert.assertTrue("data dir should exist", dataDir.exists());
    Assert.assertTrue("logs dir should exist", logsDir.exists());
    Assert.assertTrue("state store dir should exist", stateStoreDir.exists());
  }

  @Test
  public void testCreateMultipleResourceManagerDirs() throws IOException {
    dirManager.createClusterRoot();

    File rm0 = dirManager.createResourceManagerDir(0);
    File rm1 = dirManager.createResourceManagerDir(1);
    File rm2 = dirManager.createResourceManagerDir(2);

    Assert.assertTrue("rm0 should exist", rm0.exists());
    Assert.assertTrue("rm1 should exist", rm1.exists());
    Assert.assertTrue("rm2 should exist", rm2.exists());

    Assert.assertNotEquals("RM dirs should be different", rm0, rm1);
    Assert.assertNotEquals("RM dirs should be different", rm0, rm2);
    Assert.assertNotEquals("RM dirs should be different", rm1, rm2);
  }

  @Test
  public void testCreateNodeManagerDir() throws IOException {
    dirManager.createClusterRoot();
    File nmDir = dirManager.createNodeManagerDir(0);

    Assert.assertNotNull("NM dir should not be null", nmDir);
    Assert.assertTrue("NM dir should exist", nmDir.exists());
    Assert.assertTrue("NM dir should be directory", nmDir.isDirectory());
    Assert.assertTrue("NM dir name should be nm0",
        nmDir.getName().equals("nm0"));

    // Verify subdirectories
    File confDir = new File(nmDir, "conf");
    File logsDir = new File(nmDir, "logs");
    File localDirs = new File(nmDir, "local-dirs");
    File logDirs = new File(nmDir, "log-dirs");
    File nmLocalDir = new File(nmDir, "local-dirs/nm-local-dir");
    File usercache = new File(nmDir, "local-dirs/usercache");

    Assert.assertTrue("conf dir should exist", confDir.exists());
    Assert.assertTrue("logs dir should exist", logsDir.exists());
    Assert.assertTrue("local-dirs should exist", localDirs.exists());
    Assert.assertTrue("log-dirs should exist", logDirs.exists());
    Assert.assertTrue("nm-local-dir should exist", nmLocalDir.exists());
    Assert.assertTrue("usercache should exist", usercache.exists());
  }

  @Test(expected = IOException.class)
  public void testCreateRMDirWithoutClusterRoot() throws IOException {
    // Should throw because cluster root not created
    dirManager.createResourceManagerDir(0);
  }

  @Test(expected = IOException.class)
  public void testCreateNMDirWithoutClusterRoot() throws IOException {
    // Should throw because cluster root not created
    dirManager.createNodeManagerDir(0);
  }

  @Test
  public void testGetConfDir() throws IOException {
    dirManager.createClusterRoot();
    File rmDir = dirManager.createResourceManagerDir(0);
    File confDir = dirManager.getConfDir(rmDir);

    Assert.assertNotNull("Conf dir should not be null", confDir);
    Assert.assertEquals("Conf dir should be named 'conf'",
        "conf", confDir.getName());
    Assert.assertEquals("Conf dir parent should be RM dir",
        rmDir, confDir.getParentFile());
  }

  @Test
  public void testGetDataDir() throws IOException {
    dirManager.createClusterRoot();
    File rmDir = dirManager.createResourceManagerDir(0);
    File dataDir = dirManager.getDataDir(rmDir);

    Assert.assertNotNull("Data dir should not be null", dataDir);
    Assert.assertEquals("Data dir should be named 'data'",
        "data", dataDir.getName());
    Assert.assertEquals("Data dir parent should be RM dir",
        rmDir, dataDir.getParentFile());
  }

  @Test
  public void testGetLogsDir() throws IOException {
    dirManager.createClusterRoot();
    File nmDir = dirManager.createNodeManagerDir(0);
    File logsDir = dirManager.getLogsDir(nmDir);

    Assert.assertNotNull("Logs dir should not be null", logsDir);
    Assert.assertEquals("Logs dir should be named 'logs'",
        "logs", logsDir.getName());
    Assert.assertEquals("Logs dir parent should be NM dir",
        nmDir, logsDir.getParentFile());
  }

  @Test
  public void testGetNMLocalDirs() throws IOException {
    dirManager.createClusterRoot();
    File nmDir = dirManager.createNodeManagerDir(0);
    File localDirs = dirManager.getNMLocalDirs(nmDir);

    Assert.assertNotNull("Local dirs should not be null", localDirs);
    Assert.assertEquals("Local dirs should be named 'local-dirs'",
        "local-dirs", localDirs.getName());
    Assert.assertTrue("Local dirs should exist", localDirs.exists());
  }

  @Test
  public void testGetNMLogDirs() throws IOException {
    dirManager.createClusterRoot();
    File nmDir = dirManager.createNodeManagerDir(0);
    File logDirs = dirManager.getNMLogDirs(nmDir);

    Assert.assertNotNull("Log dirs should not be null", logDirs);
    Assert.assertEquals("Log dirs should be named 'log-dirs'",
        "log-dirs", logDirs.getName());
    Assert.assertTrue("Log dirs should exist", logDirs.exists());
  }

  @Test
  public void testGetPidFile() throws IOException {
    dirManager.createClusterRoot();
    File rmDir = dirManager.createResourceManagerDir(0);
    File pidFile = dirManager.getPidFile(rmDir);

    Assert.assertNotNull("PID file should not be null", pidFile);
    Assert.assertEquals("PID file should be named 'pid'",
        "pid", pidFile.getName());
    Assert.assertEquals("PID file parent should be RM dir",
        rmDir, pidFile.getParentFile());
  }

  @Test
  public void testWritePid() throws IOException {
    dirManager.createClusterRoot();
    File rmDir = dirManager.createResourceManagerDir(0);

    long pid = 12345L;
    dirManager.writePid(rmDir, pid);

    File pidFile = dirManager.getPidFile(rmDir);
    Assert.assertTrue("PID file should exist after writing",
        pidFile.exists());
    Assert.assertTrue("PID file should be a file",
        pidFile.isFile());
  }

  @Test
  public void testGetClusterRoot() throws IOException {
    Assert.assertNull("Cluster root should be null initially",
        dirManager.getClusterRoot());

    File root = dirManager.createClusterRoot();
    Assert.assertEquals("getClusterRoot should return created root",
        root, dirManager.getClusterRoot());
  }

  @Test
  public void testSetDeleteOnCleanup() throws IOException {
    dirManager.setDeleteOnCleanup(false);
    File root = dirManager.createClusterRoot();

    dirManager.cleanup();

    // With deleteOnCleanup=false, directory should still exist
    Assert.assertTrue("Root should still exist when deleteOnCleanup=false",
        root.exists());

    // Clean up manually
    deleteRecursive(root);
  }

  @Test
  public void testCleanup() throws IOException {
    dirManager.setDeleteOnCleanup(true);
    File root = dirManager.createClusterRoot();
    dirManager.createResourceManagerDir(0);
    dirManager.createNodeManagerDir(0);

    Assert.assertTrue("Root should exist before cleanup", root.exists());

    dirManager.cleanup();

    Assert.assertFalse("Root should not exist after cleanup",
        root.exists());
    Assert.assertNull("Cluster root should be null after cleanup",
        dirManager.getClusterRoot());
  }

  @Test
  public void testCleanupNode() throws IOException {
    dirManager.setDeleteOnCleanup(true);
    dirManager.createClusterRoot();
    File nmDir = dirManager.createNodeManagerDir(0);

    Assert.assertTrue("NM dir should exist", nmDir.exists());

    dirManager.cleanupNode(nmDir);

    Assert.assertFalse("NM dir should not exist after cleanup",
        nmDir.exists());
  }

  @Test
  public void testCleanupIdempotent() throws IOException {
    dirManager.createClusterRoot();
    dirManager.cleanup();

    // Second cleanup should not throw
    dirManager.cleanup();
  }

  @Test
  public void testToString() throws IOException {
    String str = dirManager.toString();
    Assert.assertTrue("toString should contain class name",
        str.contains("DirectoryManager"));

    dirManager.createClusterRoot();
    str = dirManager.toString();
    Assert.assertTrue("toString should contain cluster root after creation",
        str.contains("clusterRoot="));
  }

  /**
   * Helper method to recursively delete a directory.
   */
  private void deleteRecursive(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursive(child);
        }
      }
    }
    file.delete();
  }
}
