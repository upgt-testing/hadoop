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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.Assert.*;

/**
 * Unit tests for HadoopDistribution class.
 *
 * These tests verify:
 * - Version detection from directory names
 * - JAR discovery in Hadoop installation directories
 * - Validation of distribution completeness
 * - Classpath building
 */
public class TestHadoopDistribution {

  private File testDir;
  private File hadoopHome;

  @Before
  public void setUp() throws IOException {
    // Create a temporary test directory
    testDir = new File(System.getProperty("test.build.data", "target/test/data"),
        "TestHadoopDistribution-" + System.currentTimeMillis());
    testDir.mkdirs();
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
   * Tests that HadoopDistribution correctly detects version from directory name.
   */
  @Test
  public void testVersionDetectionFromDirectoryName() throws IOException {
    // Test with standard version format
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    hadoopHome.mkdirs();
    createMinimalValidDistribution(hadoopHome);

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertEquals("3.3.5", dist.getVersion());
    assertTrue(dist.isValid());
  }

  /**
   * Tests version detection with snapshot versions.
   */
  @Test
  public void testVersionDetectionWithSnapshot() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.4.0-SNAPSHOT");
    hadoopHome.mkdirs();
    createMinimalValidDistribution(hadoopHome);

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertEquals("3.4.0-SNAPSHOT", dist.getVersion());
  }

  /**
   * Tests that non-existent directory is marked as invalid.
   */
  @Test
  public void testNonExistentDirectory() throws IOException {
    hadoopHome = new File(testDir, "non-existent");

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertFalse(dist.isValid());
    assertNotNull(dist.getValidationError());
    assertTrue(dist.getValidationError().contains("does not exist"));
  }

  /**
   * Tests that a file (not directory) is marked as invalid.
   */
  @Test
  public void testFileInsteadOfDirectory() throws IOException {
    hadoopHome = new File(testDir, "not-a-directory");
    hadoopHome.createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertFalse(dist.isValid());
    assertNotNull(dist.getValidationError());
    assertTrue(dist.getValidationError().contains("not a directory"));
  }

  /**
   * Tests that distribution without required JARs is invalid.
   */
  @Test
  public void testMissingRequiredJars() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    hadoopHome.mkdirs();

    // Create directory structure but no JARs
    new File(hadoopHome, "share/hadoop/common").mkdirs();
    new File(hadoopHome, "share/hadoop/hdfs").mkdirs();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertFalse(dist.isValid());
    assertNotNull(dist.getValidationError());
    assertTrue(dist.getValidationError().contains("Missing hadoop-common"));
  }

  /**
   * Tests JAR discovery in common and hdfs directories.
   */
  @Test
  public void testJarDiscovery() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    hadoopHome.mkdirs();

    // Create required JARs
    File commonDir = new File(hadoopHome, "share/hadoop/common");
    File hdfsDir = new File(hadoopHome, "share/hadoop/hdfs");
    commonDir.mkdirs();
    hdfsDir.mkdirs();

    createJarFile(new File(commonDir, "hadoop-common-3.3.5.jar"));
    createJarFile(new File(hdfsDir, "hadoop-hdfs-3.3.5.jar"));

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertTrue(dist.isValid());

    List<File> coreJars = dist.getCoreJars();
    assertEquals(2, coreJars.size());
  }

  /**
   * Tests that test JARs and source JARs are excluded.
   */
  @Test
  public void testExcludeTestAndSourceJars() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    hadoopHome.mkdirs();

    File commonDir = new File(hadoopHome, "share/hadoop/common");
    File hdfsDir = new File(hadoopHome, "share/hadoop/hdfs");
    commonDir.mkdirs();
    hdfsDir.mkdirs();

    // Create regular JARs
    createJarFile(new File(commonDir, "hadoop-common-3.3.5.jar"));
    createJarFile(new File(hdfsDir, "hadoop-hdfs-3.3.5.jar"));

    // Create test and source JARs (should be excluded)
    createJarFile(new File(commonDir, "hadoop-common-3.3.5-tests.jar"));
    createJarFile(new File(commonDir, "hadoop-common-3.3.5-sources.jar"));
    createJarFile(new File(hdfsDir, "hadoop-hdfs-3.3.5-test-sources.jar"));

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertTrue(dist.isValid());

    List<File> coreJars = dist.getCoreJars();
    // Should only have 2 regular JARs, not the test/source ones
    assertEquals(2, coreJars.size());
  }

  /**
   * Tests dependency JAR discovery in lib/ directories.
   */
  @Test
  public void testDependencyJarDiscovery() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    createMinimalValidDistribution(hadoopHome);

    // Add dependency JARs
    File commonLib = new File(hadoopHome, "share/hadoop/common/lib");
    File hdfsLib = new File(hadoopHome, "share/hadoop/hdfs/lib");
    commonLib.mkdirs();
    hdfsLib.mkdirs();

    createJarFile(new File(commonLib, "guava-27.0.jar"));
    createJarFile(new File(commonLib, "protobuf-java-3.7.1.jar"));
    createJarFile(new File(hdfsLib, "leveldbjni-all-1.8.jar"));

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertTrue(dist.isValid());

    List<File> dependencies = dist.getDependencies();
    assertEquals(3, dependencies.size());
  }

  /**
   * Tests classpath building.
   */
  @Test
  public void testClasspathBuilding() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    createMinimalValidDistribution(hadoopHome);

    // Add a dependency
    File commonLib = new File(hadoopHome, "share/hadoop/common/lib");
    commonLib.mkdirs();
    createJarFile(new File(commonLib, "guava-27.0.jar"));

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    String classpath = dist.buildClasspath();

    assertNotNull(classpath);
    assertFalse(classpath.isEmpty());
    assertTrue(classpath.contains("hadoop-common-3.3.5.jar"));
    assertTrue(classpath.contains("hadoop-hdfs-3.3.5.jar"));
    assertTrue(classpath.contains("guava-27.0.jar"));
  }

  /**
   * Tests classpath building with additional entries.
   */
  @Test
  public void testClasspathBuildingWithAdditionalEntries() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    createMinimalValidDistribution(hadoopHome);

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);

    File configDir = new File(testDir, "config");
    configDir.mkdirs();
    File extraJar = new File(testDir, "extra.jar");
    createJarFile(extraJar);

    List<File> additional = java.util.Arrays.asList(configDir, extraJar);
    String classpath = dist.buildClasspath(additional);

    assertNotNull(classpath);
    // Additional entries should come first
    assertTrue(classpath.startsWith(configDir.getAbsolutePath()));
    assertTrue(classpath.contains(extraJar.getAbsolutePath()));
    assertTrue(classpath.contains("hadoop-common"));
  }

  /**
   * Tests native library path detection.
   */
  @Test
  public void testNativeLibPath() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    createMinimalValidDistribution(hadoopHome);

    // Create native lib directory
    File nativeLib = new File(hadoopHome, "lib/native");
    nativeLib.mkdirs();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertNotNull(dist.getNativeLibPath());
    assertEquals(nativeLib, dist.getNativeLibPath());
  }

  /**
   * Tests that missing native lib directory is handled.
   */
  @Test
  public void testMissingNativeLibPath() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    createMinimalValidDistribution(hadoopHome);

    // Don't create native lib directory
    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertNull(dist.getNativeLibPath());
  }

  /**
   * Tests toString method.
   */
  @Test
  public void testToString() throws IOException {
    hadoopHome = new File(testDir, "hadoop-3.3.5");
    createMinimalValidDistribution(hadoopHome);

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    String str = dist.toString();

    assertNotNull(str);
    assertTrue(str.contains("3.3.5"));
    assertTrue(str.contains("valid=true"));
  }

  // Helper methods

  /**
   * Creates a minimal valid Hadoop distribution for testing.
   */
  private void createMinimalValidDistribution(File hadoopHome) throws IOException {
    File commonDir = new File(hadoopHome, "share/hadoop/common");
    File hdfsDir = new File(hadoopHome, "share/hadoop/hdfs");
    commonDir.mkdirs();
    hdfsDir.mkdirs();

    String version = extractVersionFromPath(hadoopHome.getName());
    createJarFile(new File(commonDir, "hadoop-common-" + version + ".jar"));
    createJarFile(new File(hdfsDir, "hadoop-hdfs-" + version + ".jar"));
  }

  /**
   * Extracts version from path like "hadoop-3.3.5".
   */
  private String extractVersionFromPath(String path) {
    return path.replace("hadoop-", "");
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
