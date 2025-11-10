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
import java.util.List;

import org.apache.hadoop.yarn.server.process.HadoopDistribution;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for HadoopDistribution.
 */
public class TestHadoopDistribution {

  private File testDir;
  private File hadoopHome;

  @Before
  public void setUp() throws IOException {
    // Create a mock Hadoop distribution structure
    testDir = new File(System.getProperty("java.io.tmpdir"),
        "test-hadoop-dist-" + System.currentTimeMillis());
    testDir.mkdirs();

    hadoopHome = new File(testDir, "hadoop-3.3.6");
    hadoopHome.mkdirs();
  }

  @After
  public void tearDown() {
    if (testDir != null && testDir.exists()) {
      deleteRecursive(testDir);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullPath() {
    new HadoopDistribution(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyPath() throws IOException {
    new HadoopDistribution("");
  }

  @Test
  public void testConstructorValidPath() throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    Assert.assertNotNull("Distribution should not be null", dist);
    Assert.assertEquals("Hadoop home should match",
        hadoopHome.getAbsolutePath(), dist.getHadoopHome());
  }

  @Test(expected = IOException.class)
  public void testValidateNonExistentDirectory() throws IOException {
    File nonExistent = new File(testDir, "non-existent");
    HadoopDistribution dist = new HadoopDistribution(nonExistent.getAbsolutePath());
    dist.validate();
  }

  @Test(expected = IOException.class)
  public void testValidateMissingShareDirectory() throws IOException {
    // hadoopHome exists but share/hadoop doesn't
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    dist.validate();
  }

  @Test
  public void testValidateValidDistribution() throws IOException {
    // Create minimal valid structure
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    File commonLibDir = new File(commonDir, "lib");
    commonLibDir.mkdirs();

    // Create a sample JAR
    File jarFile = new File(commonLibDir, "hadoop-common-3.3.6.jar");
    jarFile.createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    dist.validate(); // Should not throw
  }

  @Test
  public void testBuildClasspathEmpty() throws IOException {
    // Create structure but no JARs
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    File commonLibDir = new File(commonDir, "lib");
    commonLibDir.mkdirs();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    List<File> classpath = dist.buildClasspath();

    Assert.assertNotNull("Classpath should not be null", classpath);
    // Empty list is acceptable - no JARs found
  }

  @Test
  public void testBuildClasspathWithJars() throws IOException {
    // Create structure with multiple JARs
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    File commonLibDir = new File(commonDir, "lib");
    commonLibDir.mkdirs();

    File yarnDir = new File(shareDir, "yarn");
    File yarnLibDir = new File(yarnDir, "lib");
    yarnLibDir.mkdirs();

    // Create sample JARs
    new File(commonLibDir, "hadoop-common-3.3.6.jar").createNewFile();
    new File(commonLibDir, "guava-27.0-jre.jar").createNewFile();
    new File(yarnLibDir, "hadoop-yarn-server-3.3.6.jar").createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    List<File> classpath = dist.buildClasspath();

    Assert.assertNotNull("Classpath should not be null", classpath);
    Assert.assertTrue("Classpath should contain JARs", classpath.size() >= 3);

    // Verify all files in classpath are JARs
    for (File file : classpath) {
      Assert.assertTrue("All classpath entries should be JARs: " + file.getName(),
          file.getName().endsWith(".jar"));
    }
  }

  @Test
  public void testBuildClasspathString() throws IOException {
    // Create structure with JARs
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    File commonLibDir = new File(commonDir, "lib");
    commonLibDir.mkdirs();

    new File(commonLibDir, "hadoop-common-3.3.6.jar").createNewFile();
    new File(commonLibDir, "guava-27.0-jre.jar").createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    String cpString = dist.buildClasspathString();

    Assert.assertNotNull("Classpath string should not be null", cpString);
    Assert.assertTrue("Classpath should contain hadoop-common",
        cpString.contains("hadoop-common-3.3.6.jar"));
    Assert.assertTrue("Classpath should contain guava",
        cpString.contains("guava-27.0-jre.jar"));
    Assert.assertTrue("Classpath should contain path separator",
        cpString.contains(File.pathSeparator));
  }

  @Test
  public void testVersionDetectionFromJar() throws IOException {
    // Create structure with version in JAR name
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    File commonLibDir = new File(commonDir, "lib");
    commonLibDir.mkdirs();

    new File(commonLibDir, "hadoop-common-3.3.6.jar").createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    String version = dist.getVersion();

    Assert.assertNotNull("Version should not be null", version);
    Assert.assertEquals("Version should match JAR", "3.3.6", version);
  }

  @Test
  public void testVersionDetectionFromDirectory() throws IOException {
    // Create minimal structure, version from directory name
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    commonDir.mkdirs();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    String version = dist.getVersion();

    Assert.assertNotNull("Version should not be null", version);
    Assert.assertEquals("Version should match directory", "3.3.6", version);
  }

  @Test
  public void testVersionDetectionUnknown() throws IOException {
    // Create structure with no version info
    File unknownHome = new File(testDir, "hadoop");
    File shareDir = new File(unknownHome, "share/hadoop");
    File commonDir = new File(shareDir, "common");
    commonDir.mkdirs();

    HadoopDistribution dist = new HadoopDistribution(unknownHome.getAbsolutePath());
    String version = dist.getVersion();

    Assert.assertEquals("Version should be unknown", "unknown", version);
  }

  @Test
  public void testGetHadoopHome() throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    Assert.assertEquals("Hadoop home should match",
        hadoopHome.getAbsolutePath(), dist.getHadoopHome());
  }

  @Test
  public void testGetBinDirectory() throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    File binDir = dist.getBinDirectory();

    Assert.assertNotNull("Bin directory should not be null", binDir);
    Assert.assertEquals("Bin directory should be correct",
        new File(hadoopHome, "bin").getAbsolutePath(),
        binDir.getAbsolutePath());
  }

  @Test
  public void testGetConfDirectory() throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    File confDir = dist.getConfDirectory();

    Assert.assertNotNull("Conf directory should not be null", confDir);
    Assert.assertEquals("Conf directory should be correct",
        new File(hadoopHome, "etc/hadoop").getAbsolutePath(),
        confDir.getAbsolutePath());
  }

  @Test
  public void testGetShareDirectory() throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    File shareDir = dist.getShareDirectory();

    Assert.assertNotNull("Share directory should not be null", shareDir);
    Assert.assertEquals("Share directory should be correct",
        new File(hadoopHome, "share/hadoop").getAbsolutePath(),
        shareDir.getAbsolutePath());
  }

  @Test
  public void testToString() throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    String str = dist.toString();

    Assert.assertTrue("toString should contain class name",
        str.contains("HadoopDistribution"));
    Assert.assertTrue("toString should contain hadoop home",
        str.contains(hadoopHome.getAbsolutePath()));
  }

  @Test
  public void testMultipleSubdirectories() throws IOException {
    // Create structure with all standard subdirectories
    File shareDir = new File(hadoopHome, "share/hadoop");
    String[] subdirs = {"common", "common/lib", "hdfs", "hdfs/lib",
        "yarn", "yarn/lib", "mapreduce", "mapreduce/lib"};

    for (String subdir : subdirs) {
      new File(shareDir, subdir).mkdirs();
    }

    // Add JARs in different locations
    new File(shareDir, "common/lib/hadoop-common-3.3.6.jar").createNewFile();
    new File(shareDir, "hdfs/lib/hadoop-hdfs-3.3.6.jar").createNewFile();
    new File(shareDir, "yarn/lib/hadoop-yarn-server-3.3.6.jar").createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    List<File> classpath = dist.buildClasspath();

    Assert.assertTrue("Classpath should contain multiple JARs",
        classpath.size() >= 3);

    // Verify JARs from different subdirectories are included
    boolean hasCommon = false;
    boolean hasHdfs = false;
    boolean hasYarn = false;

    for (File jar : classpath) {
      String name = jar.getName();
      if (name.equals("hadoop-common-3.3.6.jar")) {
        hasCommon = true;
      }
      if (name.equals("hadoop-hdfs-3.3.6.jar")) {
        hasHdfs = true;
      }
      if (name.equals("hadoop-yarn-server-3.3.6.jar")) {
        hasYarn = true;
      }
    }

    Assert.assertTrue("Classpath should include common JAR", hasCommon);
    Assert.assertTrue("Classpath should include hdfs JAR", hasHdfs);
    Assert.assertTrue("Classpath should include yarn JAR", hasYarn);
  }

  @Test
  public void testNonJarFilesIgnored() throws IOException {
    // Create structure with JARs and non-JAR files
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonLibDir = new File(shareDir, "common/lib");
    commonLibDir.mkdirs();

    new File(commonLibDir, "hadoop-common-3.3.6.jar").createNewFile();
    new File(commonLibDir, "README.txt").createNewFile();
    new File(commonLibDir, "config.xml").createNewFile();
    new File(commonLibDir, "lib.so").createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    List<File> classpath = dist.buildClasspath();

    // Only JAR should be included
    Assert.assertEquals("Only JAR files should be in classpath", 1, classpath.size());
    Assert.assertTrue("Classpath should contain only hadoop-common JAR",
        classpath.get(0).getName().equals("hadoop-common-3.3.6.jar"));
  }

  @Test
  public void testDifferentVersionFormats() throws IOException {
    // Test various version number formats
    String[] versions = {"3.3.6", "3.4.0", "3.10.5", "4.0.0-alpha1"};

    for (String version : versions) {
      File versionHome = new File(testDir, "hadoop-" + version);
      File shareDir = new File(versionHome, "share/hadoop");
      File commonLibDir = new File(shareDir, "common/lib");
      commonLibDir.mkdirs();

      new File(commonLibDir, "hadoop-common-" + version + ".jar").createNewFile();

      HadoopDistribution dist = new HadoopDistribution(versionHome.getAbsolutePath());
      String detected = dist.getVersion();

      Assert.assertNotNull("Version should be detected for " + version, detected);
      // Should detect at least the numeric part
      Assert.assertTrue("Version should contain version number: " + detected,
          detected.contains(version.split("-")[0]));

      deleteRecursive(versionHome);
    }
  }

  @Test
  public void testCaseSensitivity() throws IOException {
    // Test that file operations respect case sensitivity
    File shareDir = new File(hadoopHome, "share/hadoop");
    File commonLibDir = new File(shareDir, "common/lib");
    commonLibDir.mkdirs();

    // Create JARs with different cases
    new File(commonLibDir, "hadoop-common-3.3.6.jar").createNewFile();
    new File(commonLibDir, "Guava-27.0.jar").createNewFile();

    HadoopDistribution dist = new HadoopDistribution(hadoopHome.getAbsolutePath());
    List<File> classpath = dist.buildClasspath();

    Assert.assertEquals("Both JAR files should be found", 2, classpath.size());
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
