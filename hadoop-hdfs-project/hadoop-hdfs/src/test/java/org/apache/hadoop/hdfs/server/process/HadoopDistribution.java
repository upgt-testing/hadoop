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
package org.apache.hadoop.hdfs.server.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Hadoop distribution installation and provides utilities
 * for discovering JARs and building classpaths for process isolation.
 *
 * This class scans a Hadoop installation directory to find:
 * - Core Hadoop JARs (hadoop-common, hadoop-hdfs, etc.)
 * - Dependency JARs in lib/ directories
 * - Native library paths
 *
 * Used by ProcessBasedMiniDFSCluster to create isolated classpaths
 * for each node process, enabling multi-version testing.
 */
public class HadoopDistribution {
  private static final Logger LOG = LoggerFactory.getLogger(HadoopDistribution.class);

  // Version pattern: matches X.Y.Z or X.Y.Z-SNAPSHOT etc.
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("(\\d+\\.\\d+\\.\\d+(?:-[A-Za-z0-9]+)?)");

  private final String version;
  private final File hadoopHome;
  private final List<File> coreJars;
  private final List<File> dependencies;
  private final File nativeLibPath;
  private final boolean valid;
  private final String validationError;

  /**
   * Creates a HadoopDistribution by scanning the specified Hadoop home directory.
   *
   * @param hadoopHome Path to Hadoop installation (e.g., /opt/hadoop-3.3.5)
   * @throws IOException if the directory cannot be accessed
   */
  public HadoopDistribution(File hadoopHome) throws IOException {
    this.hadoopHome = hadoopHome;

    // Validate hadoopHome exists and is a directory
    if (!hadoopHome.exists()) {
      this.valid = false;
      this.validationError = "Hadoop home does not exist: " + hadoopHome;
      this.version = "unknown";
      this.coreJars = Collections.emptyList();
      this.dependencies = Collections.emptyList();
      this.nativeLibPath = null;
      return;
    }

    if (!hadoopHome.isDirectory()) {
      this.valid = false;
      this.validationError = "Hadoop home is not a directory: " + hadoopHome;
      this.version = "unknown";
      this.coreJars = Collections.emptyList();
      this.dependencies = Collections.emptyList();
      this.nativeLibPath = null;
      return;
    }

    // Try to detect version from directory name or VERSION file
    this.version = detectVersion();

    // Discover JARs
    List<File> discoveredCoreJars = new ArrayList<>();
    List<File> discoveredDependencies = new ArrayList<>();
    StringBuilder validationErrors = new StringBuilder();

    try {
      // Scan share/hadoop/common and share/hadoop/hdfs for core JARs
      discoverCoreJars(new File(hadoopHome, "share/hadoop/common"), discoveredCoreJars);
      discoverCoreJars(new File(hadoopHome, "share/hadoop/hdfs"), discoveredCoreJars);

      // Scan share/hadoop/common/lib and share/hadoop/hdfs/lib for dependencies
      discoverDependencyJars(new File(hadoopHome, "share/hadoop/common/lib"), discoveredDependencies);
      discoverDependencyJars(new File(hadoopHome, "share/hadoop/hdfs/lib"), discoveredDependencies);

      // Also add hadoop-annotations and hadoop-auth which might be needed
      discoverCoreJars(new File(hadoopHome, "share/hadoop/tools/lib"), discoveredCoreJars);

    } catch (Exception e) {
      validationErrors.append("Error discovering JARs: ").append(e.getMessage());
    }

    this.coreJars = Collections.unmodifiableList(discoveredCoreJars);
    this.dependencies = Collections.unmodifiableList(discoveredDependencies);

    // Check for native library path
    File nativeLib = new File(hadoopHome, "lib/native");
    this.nativeLibPath = nativeLib.exists() && nativeLib.isDirectory() ? nativeLib : null;

    // Validate we found essential JARs
    boolean hasHadoopCommon = false;
    boolean hasHadoopHdfs = false;

    for (File jar : coreJars) {
      String name = jar.getName();
      if (name.startsWith("hadoop-common-") && name.endsWith(".jar")) {
        hasHadoopCommon = true;
      }
      if (name.startsWith("hadoop-hdfs-") && name.endsWith(".jar")
          && !name.contains("client") && !name.contains("nfs")) {
        hasHadoopHdfs = true;
      }
    }

    if (!hasHadoopCommon) {
      validationErrors.append("Missing hadoop-common JAR. ");
    }
    if (!hasHadoopHdfs) {
      validationErrors.append("Missing hadoop-hdfs JAR. ");
    }

    if (validationErrors.length() > 0) {
      this.valid = false;
      this.validationError = validationErrors.toString();
    } else {
      this.valid = true;
      this.validationError = null;
    }

    LOG.info("Discovered Hadoop distribution at {}: version={}, coreJars={}, dependencies={}, valid={}",
        hadoopHome, version, coreJars.size(), dependencies.size(), valid);

    if (!valid) {
      LOG.warn("Hadoop distribution validation failed: {}", validationError);
    }
  }

  /**
   * Detects the Hadoop version from the installation.
   * First tries to parse from the directory name, then looks for VERSION file.
   */
  private String detectVersion() {
    // Try to extract version from directory name (e.g., "hadoop-3.3.5")
    String dirName = hadoopHome.getName();
    Matcher matcher = VERSION_PATTERN.matcher(dirName);
    if (matcher.find()) {
      return matcher.group(1);
    }

    // Try to read VERSION file
    File versionFile = new File(hadoopHome, "VERSION");
    if (versionFile.exists() && versionFile.isFile()) {
      // VERSION file format is not standardized, so just use directory name fallback
      LOG.debug("VERSION file exists but using directory name for version");
    }

    // Default to directory name
    return dirName.replace("hadoop-", "");
  }

  /**
   * Discovers core Hadoop JARs (hadoop-common, hadoop-hdfs, etc.) in a directory.
   */
  private void discoverCoreJars(File dir, List<File> jars) {
    if (!dir.exists() || !dir.isDirectory()) {
      LOG.debug("Core JAR directory does not exist: {}", dir);
      return;
    }

    File[] jarFiles = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File f) {
        String name = f.getName();
        return f.isFile() && name.endsWith(".jar")
            && !name.endsWith("-sources.jar")
            && !name.endsWith("-tests.jar")
            && !name.endsWith("-test-sources.jar");
      }
    });

    if (jarFiles != null && jarFiles.length > 0) {
      jars.addAll(Arrays.asList(jarFiles));
      LOG.debug("Found {} core JARs in {}", jarFiles.length, dir);
    }
  }

  /**
   * Discovers dependency JARs in a lib/ directory.
   */
  private void discoverDependencyJars(File libDir, List<File> jars) {
    if (!libDir.exists() || !libDir.isDirectory()) {
      LOG.debug("Dependency directory does not exist: {}", libDir);
      return;
    }

    File[] jarFiles = libDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.isFile() && f.getName().endsWith(".jar");
      }
    });

    if (jarFiles != null && jarFiles.length > 0) {
      jars.addAll(Arrays.asList(jarFiles));
      LOG.debug("Found {} dependency JARs in {}", jarFiles.length, libDir);
    }
  }

  /**
   * Builds a classpath string from core JARs and dependencies.
   *
   * @return Classpath string with File.pathSeparator between entries
   */
  public String buildClasspath() {
    StringBuilder cp = new StringBuilder();

    // Add core JARs first
    for (File jar : coreJars) {
      if (cp.length() > 0) {
        cp.append(File.pathSeparator);
      }
      cp.append(jar.getAbsolutePath());
    }

    // Add dependencies
    for (File jar : dependencies) {
      if (cp.length() > 0) {
        cp.append(File.pathSeparator);
      }
      cp.append(jar.getAbsolutePath());
    }

    return cp.toString();
  }

  /**
   * Builds a classpath string with additional entries prepended.
   *
   * @param additionalEntries Additional classpath entries to prepend
   * @return Classpath string
   */
  public String buildClasspath(List<File> additionalEntries) {
    StringBuilder cp = new StringBuilder();

    // Add additional entries first
    for (File entry : additionalEntries) {
      if (cp.length() > 0) {
        cp.append(File.pathSeparator);
      }
      cp.append(entry.getAbsolutePath());
    }

    // Add distribution classpath
    String distCp = buildClasspath();
    if (distCp.length() > 0) {
      if (cp.length() > 0) {
        cp.append(File.pathSeparator);
      }
      cp.append(distCp);
    }

    return cp.toString();
  }

  // Getters

  public String getVersion() {
    return version;
  }

  public File getHadoopHome() {
    return hadoopHome;
  }

  public List<File> getCoreJars() {
    return coreJars;
  }

  public List<File> getDependencies() {
    return dependencies;
  }

  public File getNativeLibPath() {
    return nativeLibPath;
  }

  public boolean isValid() {
    return valid;
  }

  public String getValidationError() {
    return validationError;
  }

  /**
   * Returns a description of this distribution for debugging.
   */
  @Override
  public String toString() {
    return String.format("HadoopDistribution{version=%s, home=%s, valid=%s, coreJars=%d, deps=%d}",
        version, hadoopHome, valid, coreJars.size(), dependencies.size());
  }
}
