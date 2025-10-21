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
package org.apache.hadoop.hdfs.server.process.unit;

import org.apache.hadoop.hdfs.server.process.DirectoryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for DirectoryManager.
 */
public class TestDirectoryManager {

  private DirectoryManager dirManager;

  @After
  public void tearDown() throws IOException {
    if (dirManager != null) {
      try {
        dirManager.cleanup();
      } catch (Exception e) {
        // Ignore cleanup errors in tests
      }
    }
  }

  @Test
  public void testCreateWithDefaultTempDir() throws IOException {
    dirManager = new DirectoryManager(false);

    File baseDir = dirManager.getClusterBaseDir();
    assertNotNull("Base directory should not be null", baseDir);
    assertTrue("Base directory should exist", baseDir.exists());
    assertTrue("Base directory should be a directory", baseDir.isDirectory());
    assertTrue("Base directory name should contain prefix",
        baseDir.getName().startsWith("process-minicluster-"));
  }

  @Test
  public void testCreateWithCustomBaseDir() throws IOException {
    File customBase = new File(System.getProperty("java.io.tmpdir"),
        "custom-test-dir-" + System.currentTimeMillis());

    dirManager = new DirectoryManager(customBase, false);

    File baseDir = dirManager.getClusterBaseDir();
    assertEquals("Base directory should match custom dir", customBase, baseDir);
    assertTrue("Custom base directory should exist", customBase.exists());
  }

  @Test
  public void testCreateNameNodeDirectory() throws IOException {
    dirManager = new DirectoryManager(false);

    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    assertNotNull("Node directory should not be null", nodeDir);
    assertTrue("Node base dir should exist", nodeDir.getNodeBaseDir().exists());
    assertTrue("Conf dir should exist", nodeDir.getConfDir().exists());
    assertTrue("Data dir should exist", nodeDir.getDataDir().exists());
    assertTrue("Logs dir should exist", nodeDir.getLogsDir().exists());

    assertEquals("Node base dir name should be nn0", "nn0",
        nodeDir.getNodeBaseDir().getName());
    assertEquals("Conf dir name should be conf", "conf",
        nodeDir.getConfDir().getName());
    assertEquals("Data dir name should be data", "data",
        nodeDir.getDataDir().getName());
    assertEquals("Logs dir name should be logs", "logs",
        nodeDir.getLogsDir().getName());
    assertEquals("PID file name should be pid", "pid",
        nodeDir.getPidFile().getName());
  }

  @Test
  public void testCreateDataNodeDirectory() throws IOException {
    dirManager = new DirectoryManager(false);

    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    assertNotNull("Node directory should not be null", nodeDir);
    assertTrue("Node base dir should exist", nodeDir.getNodeBaseDir().exists());
    assertTrue("Conf dir should exist", nodeDir.getConfDir().exists());
    assertTrue("Data dir should exist", nodeDir.getDataDir().exists());
    assertTrue("Logs dir should exist", nodeDir.getLogsDir().exists());

    assertEquals("Node base dir name should be dn0", "dn0",
        nodeDir.getNodeBaseDir().getName());
  }

  @Test
  public void testCreateMultipleNodeDirectories() throws IOException {
    dirManager = new DirectoryManager(false);

    DirectoryManager.NodeDirectory nn0 = dirManager.createNameNodeDirectory(0);
    DirectoryManager.NodeDirectory dn0 = dirManager.createDataNodeDirectory(0);
    DirectoryManager.NodeDirectory dn1 = dirManager.createDataNodeDirectory(1);
    DirectoryManager.NodeDirectory dn2 = dirManager.createDataNodeDirectory(2);

    // All should exist
    assertTrue("nn0 should exist", nn0.getNodeBaseDir().exists());
    assertTrue("dn0 should exist", dn0.getNodeBaseDir().exists());
    assertTrue("dn1 should exist", dn1.getNodeBaseDir().exists());
    assertTrue("dn2 should exist", dn2.getNodeBaseDir().exists());

    // All should be different
    assertNotEquals("nn0 and dn0 should be different dirs",
        nn0.getNodeBaseDir(), dn0.getNodeBaseDir());
    assertNotEquals("dn0 and dn1 should be different dirs",
        dn0.getNodeBaseDir(), dn1.getNodeBaseDir());

    // Check node dirs list
    assertEquals("Should track 4 node directories", 4, dirManager.getNodeDirs().size());
  }

  @Test
  public void testCreateClusterPropertiesFile() throws IOException {
    dirManager = new DirectoryManager(false);

    File propsFile = dirManager.createClusterPropertiesFile();

    assertNotNull("Properties file should not be null", propsFile);
    assertTrue("Properties file should exist", propsFile.exists());
    assertTrue("Properties file should be a file", propsFile.isFile());
    assertEquals("Properties file name should be cluster.properties",
        "cluster.properties", propsFile.getName());
    assertEquals("Properties file should be in base dir",
        dirManager.getClusterBaseDir(), propsFile.getParentFile());
  }

  @Test
  public void testCleanup() throws IOException {
    dirManager = new DirectoryManager(false);

    File baseDir = dirManager.getClusterBaseDir();
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    assertTrue("Base dir should exist before cleanup", baseDir.exists());
    assertTrue("Node dir should exist before cleanup",
        nodeDir.getNodeBaseDir().exists());

    dirManager.cleanup();

    assertFalse("Base dir should not exist after cleanup", baseDir.exists());
    assertFalse("Node dir should not exist after cleanup",
        nodeDir.getNodeBaseDir().exists());
  }

  @Test
  public void testCleanupWithFiles() throws IOException {
    dirManager = new DirectoryManager(false);

    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    // Create some files in the directories
    File testFile1 = new File(nodeDir.getConfDir(), "test.txt");
    File testFile2 = new File(nodeDir.getDataDir(), "data.txt");
    File testFile3 = new File(nodeDir.getLogsDir(), "log.txt");

    assertTrue("Test file 1 should be created", testFile1.createNewFile());
    assertTrue("Test file 2 should be created", testFile2.createNewFile());
    assertTrue("Test file 3 should be created", testFile3.createNewFile());

    assertTrue("Test file 1 should exist", testFile1.exists());
    assertTrue("Test file 2 should exist", testFile2.exists());
    assertTrue("Test file 3 should exist", testFile3.exists());

    dirManager.cleanup();

    assertFalse("Test file 1 should be deleted", testFile1.exists());
    assertFalse("Test file 2 should be deleted", testFile2.exists());
    assertFalse("Test file 3 should be deleted", testFile3.exists());
    assertFalse("Base dir should be deleted", dirManager.getClusterBaseDir().exists());
  }

  @Test
  public void testGetNodeDirs() throws IOException {
    dirManager = new DirectoryManager(false);

    assertEquals("Should start with no node dirs", 0, dirManager.getNodeDirs().size());

    dirManager.createNameNodeDirectory(0);
    assertEquals("Should have 1 node dir", 1, dirManager.getNodeDirs().size());

    dirManager.createDataNodeDirectory(0);
    assertEquals("Should have 2 node dirs", 2, dirManager.getNodeDirs().size());

    dirManager.createDataNodeDirectory(1);
    assertEquals("Should have 3 node dirs", 3, dirManager.getNodeDirs().size());
  }

  @Test
  public void testNodeDirectoryToString() throws IOException {
    dirManager = new DirectoryManager(false);
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    String str = nodeDir.toString();
    assertNotNull("toString should not be null", str);
    assertTrue("toString should contain class name",
        str.contains("NodeDirectory"));
    assertTrue("toString should contain nodeBaseDir",
        str.contains("nodeBaseDir"));
  }

  @Test
  public void testCreateDirectoryWithNestedStructure() throws IOException {
    dirManager = new DirectoryManager(false);
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    // Create nested directories in data dir
    File nestedDir = new File(nodeDir.getDataDir(), "nested/deep/structure");
    assertTrue("Should create nested directories", nestedDir.mkdirs());
    assertTrue("Nested dir should exist", nestedDir.exists());

    // Cleanup should remove everything
    dirManager.cleanup();
    assertFalse("Nested dir should be removed", nestedDir.exists());
    assertFalse("Data dir should be removed", nodeDir.getDataDir().exists());
  }

  @Test
  public void testMultipleNameNodes() throws IOException {
    dirManager = new DirectoryManager(false);

    DirectoryManager.NodeDirectory nn0 = dirManager.createNameNodeDirectory(0);
    DirectoryManager.NodeDirectory nn1 = dirManager.createNameNodeDirectory(1);

    assertEquals("nn0 should have correct name", "nn0",
        nn0.getNodeBaseDir().getName());
    assertEquals("nn1 should have correct name", "nn1",
        nn1.getNodeBaseDir().getName());

    assertNotEquals("nn0 and nn1 should be different",
        nn0.getNodeBaseDir(), nn1.getNodeBaseDir());
  }

  @Test
  public void testCleanupEmptyDirectory() throws IOException {
    dirManager = new DirectoryManager(false);

    File baseDir = dirManager.getClusterBaseDir();
    assertTrue("Base dir should exist", baseDir.exists());

    // Don't create any node directories, just cleanup the empty base
    dirManager.cleanup();

    assertFalse("Base dir should be removed", baseDir.exists());
  }

  @Test
  public void testGetNodeDirsReturnsCopy() throws IOException {
    dirManager = new DirectoryManager(false);
    dirManager.createNameNodeDirectory(0);

    java.util.List<File> dirs1 = dirManager.getNodeDirs();
    java.util.List<File> dirs2 = dirManager.getNodeDirs();

    assertNotSame("Should return different list instances", dirs1, dirs2);
    assertEquals("Lists should have same size", dirs1.size(), dirs2.size());
  }

  @Test
  public void testCreateSameNodeDirectoryTwice() throws IOException {
    dirManager = new DirectoryManager(false);

    DirectoryManager.NodeDirectory first = dirManager.createNameNodeDirectory(0);
    DirectoryManager.NodeDirectory second = dirManager.createNameNodeDirectory(0);

    // Both should reference the same directory
    assertEquals("Should reference same directory",
        first.getNodeBaseDir(), second.getNodeBaseDir());

    // Should track both creations
    assertEquals("Should track 2 entries", 2, dirManager.getNodeDirs().size());
  }

  @Test
  public void testDirectoryManagerWithExistingBaseDir() throws IOException {
    File existingBase = new File(System.getProperty("java.io.tmpdir"),
        "existing-base-" + System.currentTimeMillis());

    // Create the directory first
    assertTrue("Should create existing base", existingBase.mkdirs());
    assertTrue("Existing base should exist", existingBase.exists());

    dirManager = new DirectoryManager(existingBase, false);

    assertEquals("Should use existing base", existingBase,
        dirManager.getClusterBaseDir());

    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);
    assertTrue("Should create node dir in existing base",
        nodeDir.getNodeBaseDir().exists());
  }

  @Test
  public void testPidFileLocation() throws IOException {
    dirManager = new DirectoryManager(false);
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    File pidFile = nodeDir.getPidFile();
    assertEquals("PID file should be in node base dir",
        nodeDir.getNodeBaseDir(), pidFile.getParentFile());
    assertEquals("PID file name should be 'pid'", "pid", pidFile.getName());
  }
}
