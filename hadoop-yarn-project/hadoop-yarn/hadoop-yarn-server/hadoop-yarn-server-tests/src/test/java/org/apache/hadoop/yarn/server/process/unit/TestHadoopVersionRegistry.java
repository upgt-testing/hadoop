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
import java.util.Collection;

import org.apache.hadoop.yarn.server.process.HadoopDistribution;
import org.apache.hadoop.yarn.server.process.HadoopVersionRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for HadoopVersionRegistry.
 */
public class TestHadoopVersionRegistry {

  private HadoopVersionRegistry registry;
  private File testDir;
  private String savedStartHome;
  private String savedUpgradeHome;

  @Before
  public void setUp() {
    registry = new HadoopVersionRegistry();

    // Create test directory structure
    testDir = new File(System.getProperty("java.io.tmpdir"),
        "test-version-registry-" + System.currentTimeMillis());
    testDir.mkdirs();

    // Save existing system properties
    savedStartHome = System.getProperty("hadoop.start.home");
    savedUpgradeHome = System.getProperty("hadoop.upgrade.home");
  }

  @After
  public void tearDown() {
    if (registry != null) {
      registry.clear();
    }

    if (testDir != null && testDir.exists()) {
      deleteRecursive(testDir);
    }

    // Restore system properties
    restoreSystemProperty("hadoop.start.home", savedStartHome);
    restoreSystemProperty("hadoop.upgrade.home", savedUpgradeHome);
  }

  @Test
  public void testRegisterAndGet() throws IOException {
    File hadoopHome = createMockDistribution("hadoop-3.3.6");

    HadoopDistribution dist = registry.register("3.3.6",
        hadoopHome.getAbsolutePath());

    Assert.assertNotNull("Registered distribution should not be null", dist);
    Assert.assertEquals("Hadoop home should match",
        hadoopHome.getAbsolutePath(), dist.getHadoopHome());

    HadoopDistribution retrieved = registry.get("3.3.6");
    Assert.assertSame("Retrieved distribution should be same instance",
        dist, retrieved);
  }

  @Test
  public void testRegisterMultipleVersions() throws IOException {
    File home336 = createMockDistribution("hadoop-3.3.6");
    File home340 = createMockDistribution("hadoop-3.4.0");

    registry.register("3.3.6", home336.getAbsolutePath());
    registry.register("3.4.0", home340.getAbsolutePath());

    Assert.assertNotNull("3.3.6 should be registered", registry.get("3.3.6"));
    Assert.assertNotNull("3.4.0 should be registered", registry.get("3.4.0"));
    Assert.assertEquals("Should have 2 versions", 2, registry.getRegisteredKeys().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterNullVersion() throws IOException {
    File hadoopHome = createMockDistribution("hadoop-3.3.6");
    registry.register(null, hadoopHome.getAbsolutePath());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterEmptyVersion() throws IOException {
    File hadoopHome = createMockDistribution("hadoop-3.3.6");
    registry.register("", hadoopHome.getAbsolutePath());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterNullHadoopHome() throws IOException {
    registry.register("3.3.6", null);
  }

  @Test(expected = IllegalStateException.class)
  public void testRegisterDuplicateVersion() throws IOException {
    File home1 = createMockDistribution("hadoop-3.3.6");
    File home2 = createMockDistribution("hadoop-3.3.6-alt");

    registry.register("3.3.6", home1.getAbsolutePath());
    registry.register("3.3.6", home2.getAbsolutePath()); // Should throw
  }

  @Test
  public void testRegisterWithAlias() throws IOException {
    File hadoopHome = createMockDistribution("hadoop-3.3.6");

    registry.register("start-version", hadoopHome.getAbsolutePath());

    HadoopDistribution dist = registry.get("start-version");
    Assert.assertNotNull("Alias should resolve to distribution", dist);
    Assert.assertEquals("Hadoop home should match",
        hadoopHome.getAbsolutePath(), dist.getHadoopHome());
  }

  @Test
  public void testGetNonExistentVersion() {
    HadoopDistribution dist = registry.get("non-existent");
    Assert.assertNull("Non-existent version should return null", dist);
  }

  @Test
  public void testHasVersion() throws IOException {
    File hadoopHome = createMockDistribution("hadoop-3.3.6");

    Assert.assertFalse("Should not have version before registration",
        registry.contains("3.3.6"));

    registry.register("3.3.6", hadoopHome.getAbsolutePath());

    Assert.assertTrue("Should have version after registration",
        registry.contains("3.3.6"));
    Assert.assertFalse("Should not have unregistered version",
        registry.contains("3.4.0"));
  }

  @Test
  public void testGetAllVersions() throws IOException {
    Assert.assertTrue("Initially should have no versions",
        registry.getRegisteredKeys().isEmpty());

    File home336 = createMockDistribution("hadoop-3.3.6");
    File home340 = createMockDistribution("hadoop-3.4.0");

    registry.register("3.3.6", home336.getAbsolutePath());
    registry.register("3.4.0", home340.getAbsolutePath());
    registry.register("start", home336.getAbsolutePath());

    Collection<String> versions = registry.getRegisteredKeys();
    Assert.assertEquals("Should have 3 entries", 3, versions.size());
    Assert.assertTrue("Should contain 3.3.6", versions.contains("3.3.6"));
    Assert.assertTrue("Should contain 3.4.0", versions.contains("3.4.0"));
    Assert.assertTrue("Should contain alias", versions.contains("start"));
  }

  @Test
  public void testClear() throws IOException {
    File home336 = createMockDistribution("hadoop-3.3.6");
    File home340 = createMockDistribution("hadoop-3.4.0");

    registry.register("3.3.6", home336.getAbsolutePath());
    registry.register("3.4.0", home340.getAbsolutePath());

    Assert.assertEquals("Should have 2 versions", 2, registry.getRegisteredKeys().size());

    registry.clear();

    Assert.assertTrue("After clear should have no versions",
        registry.getRegisteredKeys().isEmpty());
    Assert.assertNull("3.3.6 should not be found", registry.get("3.3.6"));
    Assert.assertNull("3.4.0 should not be found", registry.get("3.4.0"));
  }

  @Test
  public void testFromSystemPropertiesBothSet() throws IOException {
    File startHome = createMockDistribution("hadoop-3.3.6");
    File upgradeHome = createMockDistribution("hadoop-3.4.0");

    System.setProperty("hadoop.start.home", startHome.getAbsolutePath());
    System.setProperty("hadoop.upgrade.home", upgradeHome.getAbsolutePath());

    HadoopVersionRegistry reg = HadoopVersionRegistry.fromSystemProperties();

    Assert.assertNotNull("Should create registry from system properties", reg);
    Assert.assertTrue("Should have start-version",
        reg.contains("start-version"));
    Assert.assertTrue("Should have upgrade-version",
        reg.contains("upgrade-version"));

    HadoopDistribution startDist = reg.get("start-version");
    Assert.assertEquals("Start version should match system property",
        startHome.getAbsolutePath(), startDist.getHadoopHome());

    HadoopDistribution upgradeDist = reg.get("upgrade-version");
    Assert.assertEquals("Upgrade version should match system property",
        upgradeHome.getAbsolutePath(), upgradeDist.getHadoopHome());
  }

  @Test
  public void testFromSystemPropertiesOnlyStartSet() throws IOException {
    File startHome = createMockDistribution("hadoop-3.3.6");

    System.setProperty("hadoop.start.home", startHome.getAbsolutePath());
    System.clearProperty("hadoop.upgrade.home");

    HadoopVersionRegistry reg = HadoopVersionRegistry.fromSystemProperties();

    Assert.assertNotNull("Should create registry", reg);
    Assert.assertTrue("Should have start-version", reg.contains("start-version"));
    Assert.assertFalse("Should not have upgrade-version",
        reg.contains("upgrade-version"));
  }

  @Test
  public void testFromSystemPropertiesOnlyUpgradeSet() throws IOException {
    File upgradeHome = createMockDistribution("hadoop-3.4.0");

    System.clearProperty("hadoop.start.home");
    System.setProperty("hadoop.upgrade.home", upgradeHome.getAbsolutePath());

    HadoopVersionRegistry reg = HadoopVersionRegistry.fromSystemProperties();

    Assert.assertNotNull("Should create registry", reg);
    Assert.assertFalse("Should not have start-version",
        reg.contains("start-version"));
    Assert.assertTrue("Should have upgrade-version",
        reg.contains("upgrade-version"));
  }

  @Test
  public void testFromSystemPropertiesNoneSet() {
    System.clearProperty("hadoop.start.home");
    System.clearProperty("hadoop.upgrade.home");

    HadoopVersionRegistry reg = HadoopVersionRegistry.fromSystemProperties();

    Assert.assertNotNull("Should create empty registry", reg);
    Assert.assertTrue("Registry should be empty", reg.getRegisteredKeys().isEmpty());
  }

  @Test
  public void testThreadSafety() throws Exception {
    final int numThreads = 5;
    final int versionsPerThread = 3;
    Thread[] threads = new Thread[numThreads];

    // Pre-create distributions
    File[][] distributions = new File[numThreads][versionsPerThread];
    for (int i = 0; i < numThreads; i++) {
      for (int j = 0; j < versionsPerThread; j++) {
        distributions[i][j] = createMockDistribution(
            "hadoop-3." + i + "." + j);
      }
    }

    for (int i = 0; i < numThreads; i++) {
      final int threadIndex = i;
      threads[i] = new Thread(() -> {
        try {
          for (int j = 0; j < versionsPerThread; j++) {
            String version = "3." + threadIndex + "." + j;
            registry.register(version,
                distributions[threadIndex][j].getAbsolutePath());

            // Immediately try to retrieve it
            HadoopDistribution dist = registry.get(version);
            Assert.assertNotNull("Should retrieve registered version", dist);
          }
        } catch (IOException e) {
          Assert.fail("Thread " + threadIndex + " failed: " + e.getMessage());
        }
      });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all versions were registered
    Assert.assertEquals("Should have all registered versions",
        numThreads * versionsPerThread, registry.getRegisteredKeys().size());

    // Verify all versions are retrievable
    for (int i = 0; i < numThreads; i++) {
      for (int j = 0; j < versionsPerThread; j++) {
        String version = "3." + i + "." + j;
        Assert.assertNotNull("Version " + version + " should be registered",
            registry.get(version));
      }
    }
  }

  @Test
  public void testToString() throws IOException {
    File hadoopHome = createMockDistribution("hadoop-3.3.6");
    registry.register("3.3.6", hadoopHome.getAbsolutePath());

    String str = registry.toString();
    Assert.assertTrue("toString should contain class name",
        str.contains("HadoopVersionRegistry"));
    Assert.assertTrue("toString should contain version count info",
        str.contains("versions=1") || str.contains("size=1"));
  }

  @Test
  public void testVersionAliasIndependence() throws IOException {
    // Register same distribution with different version and alias
    File hadoopHome = createMockDistribution("hadoop-3.3.6");

    HadoopDistribution dist1 = registry.register("3.3.6",
        hadoopHome.getAbsolutePath());
    HadoopDistribution dist2 = registry.register("start-version",
        hadoopHome.getAbsolutePath());

    // Both should point to distributions (possibly same instance)
    Assert.assertNotNull("Version should be registered", dist1);
    Assert.assertNotNull("Alias should be registered", dist2);

    // Both should resolve
    Assert.assertNotNull("Should get by version", registry.get("3.3.6"));
    Assert.assertNotNull("Should get by alias", registry.get("start-version"));
  }

  @Test
  public void testCaseSensitiveVersions() throws IOException {
    File home1 = createMockDistribution("hadoop-3.3.6");
    File home2 = createMockDistribution("hadoop-3.3.6-SNAPSHOT");

    registry.register("3.3.6", home1.getAbsolutePath());
    registry.register("3.3.6-SNAPSHOT", home2.getAbsolutePath());

    Assert.assertEquals("Should have 2 different versions",
        2, registry.getRegisteredKeys().size());
    Assert.assertNotNull("Should get lowercase version", registry.get("3.3.6"));
    Assert.assertNotNull("Should get SNAPSHOT version",
        registry.get("3.3.6-SNAPSHOT"));
  }

  /**
   * Helper method to create a mock Hadoop distribution directory.
   */
  private File createMockDistribution(String name) throws IOException {
    File distDir = new File(testDir, name);
    File shareDir = new File(distDir, "share/hadoop/common");
    shareDir.mkdirs();
    return distDir;
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

  /**
   * Helper method to restore a system property.
   */
  private void restoreSystemProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
