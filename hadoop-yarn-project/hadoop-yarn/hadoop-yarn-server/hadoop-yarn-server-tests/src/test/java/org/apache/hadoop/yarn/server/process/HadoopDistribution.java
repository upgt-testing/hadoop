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

package org.apache.hadoop.yarn.server.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Hadoop distribution installation. This class parses a Hadoop
 * installation directory and discovers all necessary JAR files for running
 * YARN nodes in separate processes.
 *
 * <p>A typical Hadoop distribution structure:</p>
 * <pre>
 * /opt/hadoop-3.3.6/
 * ├── bin/
 * ├── etc/hadoop/
 * ├── lib/
 * │   └── native/          # Native libraries
 * └── share/hadoop/
 *     ├── common/
 *     │   ├── *.jar
 *     │   └── lib/*.jar
 *     ├── hdfs/
 *     │   ├── *.jar
 *     │   └── lib/*.jar
 *     ├── yarn/
 *     │   ├── *.jar
 *     │   └── lib/*.jar
 *     └── mapreduce/
 *         └── *.jar
 * </pre>
 *
 * <p>Example usage:</p>
 * <pre>
 * HadoopDistribution dist = new HadoopDistribution("/opt/hadoop-3.3.6");
 * dist.validate(); // Throws IOException if invalid
 * List&lt;File&gt; classpath = dist.buildClasspath();
 * File nativeLibDir = dist.getNativeLibraryDir();
 * </pre>
 *
 * @see HadoopVersionRegistry
 */
public class HadoopDistribution {

  private static final Logger LOG =
      LoggerFactory.getLogger(HadoopDistribution.class);

  /** The Hadoop home directory */
  private final File hadoopHome;

  /** Hadoop version (e.g., "3.3.6"), detected from distribution */
  private String version;

  /** YARN JAR files */
  private List<File> yarnJars;

  /** YARN dependency JAR files */
  private List<File> yarnLibJars;

  /** Hadoop Common JAR files */
  private List<File> commonJars;

  /** Hadoop Common dependency JAR files */
  private List<File> commonLibJars;

  /** HDFS JAR files */
  private List<File> hdfsJars;

  /** HDFS dependency JAR files */
  private List<File> hdfsLibJars;

  /** MapReduce JAR files (optional, needed for some apps) */
  private List<File> mapreduceJars;

  /** Native library directory */
  private File nativeLibDir;

  /** Whether this distribution has been validated */
  private boolean validated = false;

  /**
   * Constructs a HadoopDistribution from a Hadoop installation directory.
   *
   * @param hadoopHome Path to Hadoop home directory (e.g., "/opt/hadoop-3.3.6")
   * @throws IOException if the directory does not exist or is not readable
   */
  public HadoopDistribution(String hadoopHome) throws IOException {
    this(new File(hadoopHome));
  }

  /**
   * Constructs a HadoopDistribution from a Hadoop installation directory.
   *
   * @param hadoopHome Hadoop home directory
   * @throws IOException if the directory does not exist or is not readable
   */
  public HadoopDistribution(File hadoopHome) throws IOException {
    if (!hadoopHome.exists()) {
      throw new IOException(
          "Hadoop home directory does not exist: " + hadoopHome);
    }

    if (!hadoopHome.isDirectory()) {
      throw new IOException(
          "Hadoop home is not a directory: " + hadoopHome);
    }

    if (!hadoopHome.canRead()) {
      throw new IOException(
          "Hadoop home directory is not readable: " + hadoopHome);
    }

    this.hadoopHome = hadoopHome;
    LOG.info("Created HadoopDistribution for: {}", hadoopHome);
  }

  /**
   * Validates this Hadoop distribution by checking for required directories
   * and JAR files. This method must be called before using the distribution.
   *
   * @throws IOException if validation fails
   */
  public void validate() throws IOException {
    LOG.info("Validating Hadoop distribution: {}", hadoopHome);

    // Detect version
    detectVersion();

    // Discover JAR files
    discoverJars();

    // Find native library directory
    discoverNativeLibraries();

    // Validate minimum requirements
    if (yarnJars.isEmpty()) {
      throw new IOException(
          "No YARN JAR files found in: " + hadoopHome);
    }

    if (commonJars.isEmpty()) {
      throw new IOException(
          "No Hadoop Common JAR files found in: " + hadoopHome);
    }

    validated = true;
    LOG.info("Successfully validated Hadoop distribution: {} (version: {})",
        hadoopHome, version);
  }

  /**
   * Detects the Hadoop version from this distribution.
   * Tries multiple methods to detect the version.
   */
  private void detectVersion() throws IOException {
    // Method 1: Try to read from hadoop-common JAR manifest or properties
    File commonDir = new File(hadoopHome, "share/hadoop/common");
    if (commonDir.exists()) {
      // Look for version in directory name
      File[] commonJars = commonDir.listFiles(
          (dir, name) -> name.startsWith("hadoop-common-") &&
                         name.endsWith(".jar") &&
                         !name.contains("test"));

      if (commonJars != null && commonJars.length > 0) {
        String jarName = commonJars[0].getName();
        // Extract version from jar name: hadoop-common-3.3.6.jar
        String versionPart = jarName.replace("hadoop-common-", "")
                                    .replace(".jar", "");
        this.version = versionPart;
        LOG.info("Detected Hadoop version from JAR name: {}", version);
        return;
      }
    }

    // Method 2: Use directory name as fallback
    String dirName = hadoopHome.getName();
    if (dirName.startsWith("hadoop-")) {
      this.version = dirName.replace("hadoop-", "");
      LOG.info("Detected Hadoop version from directory name: {}", version);
      return;
    }

    // Method 3: Default to unknown
    this.version = "unknown";
    LOG.warn("Could not detect Hadoop version for: {}", hadoopHome);
  }

  /**
   * Discovers all JAR files in the Hadoop distribution.
   */
  private void discoverJars() throws IOException {
    File shareDir = new File(hadoopHome, "share/hadoop");
    if (!shareDir.exists()) {
      throw new IOException(
          "share/hadoop directory not found in: " + hadoopHome);
    }

    // YARN JARs
    yarnJars = findJars(new File(shareDir, "yarn"));
    yarnLibJars = findJars(new File(shareDir, "yarn/lib"));
    LOG.debug("Found {} YARN JARs, {} YARN lib JARs",
        yarnJars.size(), yarnLibJars.size());

    // Common JARs
    commonJars = findJars(new File(shareDir, "common"));
    commonLibJars = findJars(new File(shareDir, "common/lib"));
    LOG.debug("Found {} Common JARs, {} Common lib JARs",
        commonJars.size(), commonLibJars.size());

    // HDFS JARs
    hdfsJars = findJars(new File(shareDir, "hdfs"));
    hdfsLibJars = findJars(new File(shareDir, "hdfs/lib"));
    LOG.debug("Found {} HDFS JARs, {} HDFS lib JARs",
        hdfsJars.size(), hdfsLibJars.size());

    // MapReduce JARs (optional)
    mapreduceJars = findJars(new File(shareDir, "mapreduce"));
    LOG.debug("Found {} MapReduce JARs", mapreduceJars.size());
  }

  /**
   * Finds all JAR files in a directory (non-recursive).
   *
   * @param dir Directory to search
   * @return List of JAR files (empty if directory doesn't exist)
   */
  private List<File> findJars(File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      return Collections.emptyList();
    }

    List<File> jars = new ArrayList<>();
    File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));

    if (files != null) {
      Collections.addAll(jars, files);
    }

    return jars;
  }

  /**
   * Discovers native library directories.
   */
  private void discoverNativeLibraries() {
    File nativeDir = new File(hadoopHome, "lib/native");
    if (nativeDir.exists() && nativeDir.isDirectory()) {
      this.nativeLibDir = nativeDir;
      LOG.debug("Found native library directory: {}", nativeDir);
    } else {
      LOG.warn("Native library directory not found: {}", nativeDir);
    }
  }

  /**
   * Builds a complete classpath for running YARN nodes from this distribution.
   * The classpath includes:
   * <ol>
   *   <li>YARN JARs (share/hadoop/yarn/*.jar)</li>
   *   <li>YARN dependencies (share/hadoop/yarn/lib/*.jar)</li>
   *   <li>Hadoop Common JARs (share/hadoop/common/*.jar)</li>
   *   <li>Hadoop Common dependencies (share/hadoop/common/lib/*.jar)</li>
   *   <li>HDFS JARs (share/hadoop/hdfs/*.jar)</li>
   *   <li>HDFS dependencies (share/hadoop/hdfs/lib/*.jar)</li>
   * </ol>
   *
   * @return List of JAR files for classpath
   * @throws IllegalStateException if distribution has not been validated
   */
  public List<File> buildClasspath() {
    ensureValidated();

    List<File> classpath = new ArrayList<>();

    // Add in order of precedence
    classpath.addAll(yarnJars);
    classpath.addAll(yarnLibJars);
    classpath.addAll(commonJars);
    classpath.addAll(commonLibJars);
    classpath.addAll(hdfsJars);
    classpath.addAll(hdfsLibJars);

    LOG.debug("Built classpath with {} JARs", classpath.size());
    return classpath;
  }

  /**
   * Builds a classpath including MapReduce JARs (needed for some applications).
   *
   * @param includeMapReduce Whether to include MapReduce JARs
   * @return List of JAR files for classpath
   */
  public List<File> buildClasspath(boolean includeMapReduce) {
    List<File> classpath = buildClasspath();

    if (includeMapReduce) {
      classpath.addAll(mapreduceJars);
    }

    return classpath;
  }

  /**
   * Builds a classpath string suitable for java -cp argument.
   *
   * @return Classpath string with platform-specific separator
   */
  public String buildClasspathString() {
    return buildClasspathString(false);
  }

  /**
   * Builds a classpath string suitable for java -cp argument.
   *
   * @param includeMapReduce Whether to include MapReduce JARs
   * @return Classpath string with platform-specific separator
   */
  public String buildClasspathString(boolean includeMapReduce) {
    List<File> classpath = buildClasspath(includeMapReduce);
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < classpath.size(); i++) {
      if (i > 0) {
        sb.append(File.pathSeparator);
      }
      sb.append(classpath.get(i).getAbsolutePath());
    }

    return sb.toString();
  }

  /**
   * Gets the Hadoop home directory.
   *
   * @return Hadoop home directory
   */
  public File getHadoopHome() {
    return hadoopHome;
  }

  /**
   * Gets the Hadoop version.
   *
   * @return Hadoop version string (e.g., "3.3.6")
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the native library directory.
   *
   * @return Native library directory, or null if not found
   */
  public File getNativeLibraryDir() {
    return nativeLibDir;
  }

  /**
   * Gets the YARN JAR files.
   *
   * @return List of YARN JARs
   */
  public List<File> getYarnJars() {
    ensureValidated();
    return Collections.unmodifiableList(yarnJars);
  }

  /**
   * Gets the Hadoop Common JAR files.
   *
   * @return List of Common JARs
   */
  public List<File> getCommonJars() {
    ensureValidated();
    return Collections.unmodifiableList(commonJars);
  }

  /**
   * Gets the HDFS JAR files.
   *
   * @return List of HDFS JARs
   */
  public List<File> getHdfsJars() {
    ensureValidated();
    return Collections.unmodifiableList(hdfsJars);
  }

  /**
   * Gets the MapReduce JAR files.
   *
   * @return List of MapReduce JARs
   */
  public List<File> getMapReduceJars() {
    ensureValidated();
    return Collections.unmodifiableList(mapreduceJars);
  }

  /**
   * Checks if this distribution has been validated.
   *
   * @return true if validated
   */
  public boolean isValidated() {
    return validated;
  }

  /**
   * Ensures this distribution has been validated.
   *
   * @throws IllegalStateException if not validated
   */
  private void ensureValidated() {
    if (!validated) {
      throw new IllegalStateException(
          "HadoopDistribution must be validated before use. Call validate() first.");
    }
  }

  @Override
  public String toString() {
    return "HadoopDistribution{" +
        "home=" + hadoopHome +
        ", version=" + version +
        ", validated=" + validated +
        '}';
  }
}
