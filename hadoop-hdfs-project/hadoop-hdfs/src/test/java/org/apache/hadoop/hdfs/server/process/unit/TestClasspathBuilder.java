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

import org.apache.hadoop.hdfs.server.process.ClasspathBuilder;
import org.apache.hadoop.hdfs.server.process.HadoopDistribution;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.Assert.*;

/**
 * Unit tests for ClasspathBuilder utility.
 *
 * These tests verify:
 * - Classpath construction for node processes
 * - Configuration directory handling
 * - Classpath validation
 * - Path manipulation utilities
 */
public class TestClasspathBuilder {

  private File testDir;

  @Before
  public void setUp() throws IOException {
    // Create a temporary test directory
    testDir = new File(System.getProperty("test.build.data", "target/test/data"),
        "TestClasspathBuilder-" + System.currentTimeMillis());
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
   * Tests building basic node classpath.
   */
  @Test
  public void testBuildNodeClasspath() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    String classpath = ClasspathBuilder.buildNodeClasspath(dist);

    assertNotNull(classpath);
    assertFalse(classpath.isEmpty());
    assertTrue(classpath.contains("hadoop-common"));
    assertTrue(classpath.contains("hadoop-hdfs"));
  }

  /**
   * Tests building node classpath with config directory.
   */
  @Test
  public void testBuildNodeClasspathWithConfig() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    File configDir = new File(testDir, "config");
    configDir.mkdirs();

    String classpath = ClasspathBuilder.buildNodeClasspath(dist, configDir);

    assertNotNull(classpath);
    // Config directory should be first (highest priority)
    assertTrue(classpath.startsWith(configDir.getAbsolutePath()));
    assertTrue(classpath.contains("hadoop-common"));
  }

  /**
   * Tests building classpath with non-existent config directory.
   */
  @Test
  public void testBuildNodeClasspathWithNonExistentConfig() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    File configDir = new File(testDir, "non-existent-config");

    String classpath = ClasspathBuilder.buildNodeClasspath(dist, configDir);

    assertNotNull(classpath);
    // Should not contain the non-existent directory
    assertFalse(classpath.contains(configDir.getAbsolutePath()));
  }

  /**
   * Tests building classpath with null distribution.
   */
  @Test
  public void testBuildNodeClasspathNullDistribution() {
    try {
      ClasspathBuilder.buildNodeClasspath(null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null"));
    }
  }

  /**
   * Tests building classpath with invalid distribution.
   */
  @Test
  public void testBuildNodeClasspathInvalidDistribution() throws IOException {
    File hadoopHome = new File(testDir, "hadoop-invalid");
    hadoopHome.mkdirs();
    // Don't create required JARs

    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    assertFalse(dist.isValid());

    try {
      ClasspathBuilder.buildNodeClasspath(dist);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Invalid distribution"));
    }
  }

  /**
   * Tests getting native library path.
   */
  @Test
  public void testGetNativeLibPath() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    // Create native lib directory
    File hadoopHome = dist.getHadoopHome();
    File nativeLib = new File(hadoopHome, "lib/native");
    nativeLib.mkdirs();

    // Need to recreate distribution to pick up native lib
    dist = new HadoopDistribution(hadoopHome);

    String nativeLibPath = ClasspathBuilder.getNativeLibPath(dist);
    assertNotNull(nativeLibPath);
    assertEquals(nativeLib.getAbsolutePath(), nativeLibPath);
  }

  /**
   * Tests getting native library path when it doesn't exist.
   */
  @Test
  public void testGetNativeLibPathNotExists() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    String nativeLibPath = ClasspathBuilder.getNativeLibPath(dist);
    assertNull(nativeLibPath);
  }

  /**
   * Tests classpath validation with valid classpath.
   */
  @Test
  public void testIsValidClasspathValid() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");
    String classpath = dist.buildClasspath();

    assertTrue(ClasspathBuilder.isValidClasspath(classpath));
  }

  /**
   * Tests classpath validation with null classpath.
   */
  @Test
  public void testIsValidClasspathNull() {
    assertFalse(ClasspathBuilder.isValidClasspath(null));
  }

  /**
   * Tests classpath validation with empty classpath.
   */
  @Test
  public void testIsValidClasspathEmpty() {
    assertFalse(ClasspathBuilder.isValidClasspath(""));
    assertFalse(ClasspathBuilder.isValidClasspath("   "));
  }

  /**
   * Tests classpath validation with incomplete classpath.
   */
  @Test
  public void testIsValidClasspathIncomplete() {
    // Only has hadoop-common, missing hadoop-hdfs
    String classpath = "/path/to/hadoop-common-3.3.5.jar";
    assertFalse(ClasspathBuilder.isValidClasspath(classpath));
  }

  /**
   * Tests splitting classpath string.
   */
  @Test
  public void testSplitClasspath() {
    String classpath = "/path/to/file1.jar" + File.pathSeparator + "/path/to/file2.jar";

    List<File> files = ClasspathBuilder.splitClasspath(classpath);

    assertEquals(2, files.size());
    assertEquals(new File("/path/to/file1.jar"), files.get(0));
    assertEquals(new File("/path/to/file2.jar"), files.get(1));
  }

  /**
   * Tests splitting empty classpath.
   */
  @Test
  public void testSplitClasspathEmpty() {
    List<File> files = ClasspathBuilder.splitClasspath("");
    assertTrue(files.isEmpty());

    files = ClasspathBuilder.splitClasspath(null);
    assertTrue(files.isEmpty());
  }

  /**
   * Tests joining classpath from files.
   */
  @Test
  public void testJoinClasspath() {
    List<File> files = Arrays.asList(
        new File("/path/to/file1.jar"),
        new File("/path/to/file2.jar")
    );

    String classpath = ClasspathBuilder.joinClasspath(files);

    assertNotNull(classpath);
    assertTrue(classpath.contains("file1.jar"));
    assertTrue(classpath.contains("file2.jar"));
    assertTrue(classpath.contains(File.pathSeparator));
  }

  /**
   * Tests joining empty file list.
   */
  @Test
  public void testJoinClasspathEmpty() {
    String classpath = ClasspathBuilder.joinClasspath(null);
    assertEquals("", classpath);

    classpath = ClasspathBuilder.joinClasspath(Arrays.<File>asList());
    assertEquals("", classpath);
  }

  /**
   * Tests round-trip split and join.
   */
  @Test
  public void testSplitJoinRoundtrip() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");
    String originalClasspath = dist.buildClasspath();

    List<File> files = ClasspathBuilder.splitClasspath(originalClasspath);
    String rebuiltClasspath = ClasspathBuilder.joinClasspath(files);

    assertEquals(originalClasspath, rebuiltClasspath);
  }

  /**
   * Tests classpath summarization.
   */
  @Test
  public void testSummarizeClasspath() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    // Add some dependencies
    File commonLib = new File(dist.getHadoopHome(), "share/hadoop/common/lib");
    commonLib.mkdirs();
    createJarFile(new File(commonLib, "guava-27.0.jar"));
    createJarFile(new File(commonLib, "protobuf-java-3.7.1.jar"));

    // Recreate distribution to pick up new files
    dist = new HadoopDistribution(dist.getHadoopHome());

    String classpath = dist.buildClasspath();
    String summary = ClasspathBuilder.summarizeClasspath(classpath);

    assertNotNull(summary);
    assertTrue(summary.contains("entries"));
    assertTrue(summary.contains("JARs"));
    assertTrue(summary.contains("hadoop-common") || summary.contains("hadoop-hdfs"));
  }

  /**
   * Tests summarizing empty classpath.
   */
  @Test
  public void testSummarizeClasspathEmpty() {
    String summary = ClasspathBuilder.summarizeClasspath("");
    assertEquals("Empty classpath", summary);

    summary = ClasspathBuilder.summarizeClasspath(null);
    assertEquals("Empty classpath", summary);
  }

  /**
   * Tests classpath with mixed files and directories.
   */
  @Test
  public void testClasspathWithMixedEntries() throws IOException {
    HadoopDistribution dist = createValidDistribution("hadoop-3.3.5");

    File configDir = new File(testDir, "config");
    configDir.mkdirs();
    File extraJar = new File(testDir, "extra.jar");
    createJarFile(extraJar);

    List<File> additional = Arrays.asList(configDir, extraJar);
    String classpath = dist.buildClasspath(additional);

    String summary = ClasspathBuilder.summarizeClasspath(classpath);
    assertTrue(summary.contains("dirs"));
    assertTrue(summary.contains("JARs"));
  }

  // Helper methods

  /**
   * Creates a valid Hadoop distribution for testing.
   */
  private HadoopDistribution createValidDistribution(String dirName) throws IOException {
    File hadoopHome = new File(testDir, dirName);
    File commonDir = new File(hadoopHome, "share/hadoop/common");
    File hdfsDir = new File(hadoopHome, "share/hadoop/hdfs");
    commonDir.mkdirs();
    hdfsDir.mkdirs();

    String version = dirName.replace("hadoop-", "");
    createJarFile(new File(commonDir, "hadoop-common-" + version + ".jar"));
    createJarFile(new File(hdfsDir, "hadoop-hdfs-" + version + ".jar"));

    return new HadoopDistribution(hadoopHome);
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
