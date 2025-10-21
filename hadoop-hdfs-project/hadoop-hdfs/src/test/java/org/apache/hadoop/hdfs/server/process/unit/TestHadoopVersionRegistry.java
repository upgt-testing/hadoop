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

import org.apache.hadoop.hdfs.server.process.HadoopDistribution;
import org.apache.hadoop.hdfs.server.process.HadoopVersionRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.Assert.*;

/**
 * Unit tests for HadoopVersionRegistry class.
 *
 * These tests verify:
 * - Distribution registration and retrieval
 * - Version aliasing
 * - Duplicate registration handling
 * - Validation of registered distributions
 */
public class TestHadoopVersionRegistry {

  private File testDir;
  private HadoopVersionRegistry registry;

  @Before
  public void setUp() throws IOException {
    // Create a temporary test directory
    testDir = new File(System.getProperty("test.build.data", "target/test/data"),
        "TestHadoopVersionRegistry-" + System.currentTimeMillis());
    testDir.mkdirs();

    registry = new HadoopVersionRegistry();
  }

  @After
  public void tearDown() {
    // Clean up test directory
    if (testDir != null && testDir.exists()) {
      deleteRecursive(testDir);
    }
  }

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
   * Tests basic registration and retrieval.
   */
  @Test
  public void testBasicRegistration() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    assertTrue(registry.isRegistered("3.3.5"));
    HadoopDistribution dist = registry.get("3.3.5");
    assertNotNull(dist);
    assertEquals("3.3.5", dist.getVersion());
    assertTrue(dist.isValid());
  }

  /**
   * Tests registration of multiple versions.
   */
  @Test
  public void testMultipleVersions() throws IOException {
    File home335 = createValidDistribution("hadoop-3.3.5");
    File home336 = createValidDistribution("hadoop-3.3.6");

    registry.register("3.3.5", home335.getAbsolutePath());
    registry.register("3.3.6", home336.getAbsolutePath());

    assertEquals(2, registry.size());
    assertTrue(registry.isRegistered("3.3.5"));
    assertTrue(registry.isRegistered("3.3.6"));

    Set<String> versions = registry.getRegisteredVersions();
    assertTrue(versions.contains("3.3.5"));
    assertTrue(versions.contains("3.3.6"));
  }

  /**
   * Tests custom aliasing.
   */
  @Test
  public void testCustomAlias() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    registry.register("3.3.5", hadoopHome.getAbsolutePath());
    registry.register("latest", hadoopHome.getAbsolutePath());

    assertTrue(registry.isRegistered("3.3.5"));
    assertTrue(registry.isRegistered("latest"));

    HadoopDistribution dist1 = registry.get("3.3.5");
    HadoopDistribution dist2 = registry.get("latest");

    // Should be the same distribution object
    assertSame(dist1, dist2);
  }

  /**
   * Tests that duplicate registration of same path is idempotent.
   */
  @Test
  public void testDuplicateRegistrationSamePath() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    registry.register("3.3.5", hadoopHome.getAbsolutePath());
    // Registering again with same path should not throw
    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    assertEquals(1, registry.size());
  }

  /**
   * Tests that duplicate registration with different path throws exception.
   */
  @Test
  public void testDuplicateRegistrationDifferentPath() throws IOException {
    File home1 = createValidDistribution("hadoop-3.3.5");
    File home2 = createValidDistribution("hadoop-3.3.5-alt");

    registry.register("3.3.5", home1.getAbsolutePath());

    try {
      registry.register("3.3.5", home2.getAbsolutePath());
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("already registered"));
    }
  }

  /**
   * Tests registration of invalid distribution.
   */
  @Test
  public void testInvalidDistribution() throws IOException {
    File hadoopHome = new File(testDir, "hadoop-invalid");
    hadoopHome.mkdirs();
    // Don't create required JARs - invalid distribution

    try {
      registry.register("invalid", hadoopHome.getAbsolutePath());
      fail("Should have thrown IOException for invalid distribution");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("Invalid Hadoop distribution"));
    }
  }

  /**
   * Tests registration with null version.
   */
  @Test
  public void testNullVersion() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    try {
      registry.register(null, hadoopHome.getAbsolutePath());
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null"));
    }
  }

  /**
   * Tests registration with empty version.
   */
  @Test
  public void testEmptyVersion() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    try {
      registry.register("", hadoopHome.getAbsolutePath());
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null or empty"));
    }
  }

  /**
   * Tests registration with null path.
   */
  @Test
  public void testNullPath() throws IOException {
    try {
      registry.register("3.3.5", null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null"));
    }
  }

  /**
   * Tests auto-registration.
   */
  @Test
  public void testAutoRegistration() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    String detectedVersion = registry.registerAuto(hadoopHome.getAbsolutePath());
    assertEquals("3.3.5", detectedVersion);
    assertTrue(registry.isRegistered("3.3.5"));
  }

  /**
   * Tests getRequired with valid version.
   */
  @Test
  public void testGetRequired() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");
    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    HadoopDistribution dist = registry.getRequired("3.3.5");
    assertNotNull(dist);
  }

  /**
   * Tests getRequired with invalid version.
   */
  @Test
  public void testGetRequiredNotFound() {
    try {
      registry.getRequired("non-existent");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("No Hadoop distribution registered"));
      assertTrue(e.getMessage().contains("Available versions"));
    }
  }

  /**
   * Tests get with non-existent version.
   */
  @Test
  public void testGetNonExistent() {
    HadoopDistribution dist = registry.get("non-existent");
    assertNull(dist);
  }

  /**
   * Tests getJars convenience method.
   */
  @Test
  public void testGetJars() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");
    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    List<File> jars = registry.getJars("3.3.5");
    assertNotNull(jars);
    assertEquals(2, jars.size()); // hadoop-common and hadoop-hdfs
  }

  /**
   * Tests getDependencies convenience method.
   */
  @Test
  public void testGetDependencies() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");

    // Add a dependency
    File commonLib = new File(hadoopHome, "share/hadoop/common/lib");
    commonLib.mkdirs();
    createJarFile(new File(commonLib, "guava-27.0.jar"));

    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    List<File> deps = registry.getDependencies("3.3.5");
    assertNotNull(deps);
    assertEquals(1, deps.size());
  }

  /**
   * Tests buildClasspath convenience method.
   */
  @Test
  public void testBuildClasspath() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");
    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    String classpath = registry.buildClasspath("3.3.5");
    assertNotNull(classpath);
    assertFalse(classpath.isEmpty());
    assertTrue(classpath.contains("hadoop-common"));
  }

  /**
   * Tests buildClasspath with additional entries.
   */
  @Test
  public void testBuildClasspathWithAdditional() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");
    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    File configDir = new File(testDir, "config");
    configDir.mkdirs();

    List<File> additional = java.util.Arrays.asList(configDir);
    String classpath = registry.buildClasspath("3.3.5", additional);

    assertTrue(classpath.startsWith(configDir.getAbsolutePath()));
    assertTrue(classpath.contains("hadoop-common"));
  }

  /**
   * Tests clear method.
   */
  @Test
  public void testClear() throws IOException {
    File hadoopHome = createValidDistribution("hadoop-3.3.5");
    registry.register("3.3.5", hadoopHome.getAbsolutePath());

    assertEquals(1, registry.size());
    assertTrue(registry.isRegistered("3.3.5"));

    registry.clear();

    assertEquals(0, registry.size());
    assertFalse(registry.isRegistered("3.3.5"));
  }

  /**
   * Tests toString method.
   */
  @Test
  public void testToString() throws IOException {
    File home1 = createValidDistribution("hadoop-3.3.5");
    File home2 = createValidDistribution("hadoop-3.3.6");

    registry.register("3.3.5", home1.getAbsolutePath());
    registry.register("3.3.6", home2.getAbsolutePath());

    String str = registry.toString();
    assertNotNull(str);
    assertTrue(str.contains("3.3.5"));
    assertTrue(str.contains("3.3.6"));
  }

  // Helper methods

  /**
   * Creates a valid Hadoop distribution for testing.
   */
  private File createValidDistribution(String dirName) throws IOException {
    File hadoopHome = new File(testDir, dirName);
    File commonDir = new File(hadoopHome, "share/hadoop/common");
    File hdfsDir = new File(hadoopHome, "share/hadoop/hdfs");
    commonDir.mkdirs();
    hdfsDir.mkdirs();

    String version = dirName.replace("hadoop-", "");
    createJarFile(new File(commonDir, "hadoop-common-" + version + ".jar"));
    createJarFile(new File(hdfsDir, "hadoop-hdfs-" + version + ".jar"));

    return hadoopHome;
  }

  /**
   * Creates a minimal JAR file for testing.
   */
  private void createJarFile(File jarFile) throws IOException {
    jarFile.getParentFile().mkdirs();

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

    JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);

    // Add a dummy entry so it's a valid JAR
    ZipEntry entry = new ZipEntry("dummy.txt");
    jos.putNextEntry(entry);
    jos.write("test".getBytes());
    jos.closeEntry();

    jos.close();
  }
}
