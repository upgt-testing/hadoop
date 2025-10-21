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
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for building classpaths for node processes in ProcessBasedMiniDFSCluster.
 *
 * This class constructs isolated classpaths for NameNode and DataNode processes,
 * ensuring each process has:
 * 1. Process launcher classes (for starting the subprocess)
 * 2. Hadoop distribution JARs (version-specific)
 * 3. Configuration directory
 * 4. Native library path
 *
 * The classpath isolation is critical for multi-version testing, where different
 * nodes may run different Hadoop versions with conflicting dependencies.
 */
public class ClasspathBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(ClasspathBuilder.class);

  /**
   * Builds a complete classpath for a node process.
   *
   * The classpath includes:
   * 1. Process launcher classes (from test classpath)
   * 2. Hadoop distribution core and dependency JARs
   * 3. Configuration directory (if provided)
   *
   * @param distribution The Hadoop distribution to use
   * @param configDir Configuration directory to add to classpath (can be null)
   * @return Classpath string with entries separated by File.pathSeparator
   */
  public static String buildNodeClasspath(HadoopDistribution distribution, File configDir) {
    if (distribution == null) {
      throw new IllegalArgumentException("Distribution cannot be null");
    }

    if (!distribution.isValid()) {
      throw new IllegalArgumentException(
          "Invalid distribution: " + distribution.getValidationError());
    }

    List<File> entries = new ArrayList<>();

    // 1. Add configuration directory first (highest priority)
    if (configDir != null && configDir.exists() && configDir.isDirectory()) {
      entries.add(configDir);
    }

    // 2. Add process launcher classes
    try {
      File launcherJar = findProcessLauncherJar();
      if (launcherJar != null) {
        entries.add(launcherJar);
        LOG.debug("Added process launcher JAR: {}", launcherJar);
      } else {
        LOG.warn("Could not find process launcher JAR, using current classpath");
        // In development mode, add current classpath
        addCurrentClasspath(entries);
      }
    } catch (Exception e) {
      LOG.warn("Error finding process launcher JAR: {}", e.getMessage());
      // Fallback: add current classpath
      addCurrentClasspath(entries);
    }

    // 3. Build classpath with distribution JARs
    return distribution.buildClasspath(entries);
  }

  /**
   * Builds a complete classpath for a node process (without config directory).
   *
   * @param distribution The Hadoop distribution to use
   * @return Classpath string
   */
  public static String buildNodeClasspath(HadoopDistribution distribution) {
    return buildNodeClasspath(distribution, null);
  }

  /**
   * Finds the JAR containing the process launcher classes.
   *
   * This attempts to locate the test JAR that contains the launcher classes
   * (NameNodeProcessLauncher, DataNodeProcessLauncher). In a built distribution,
   * this would be hadoop-hdfs-tests.jar.
   *
   * @return File pointing to the launcher JAR, or null if not found
   */
  private static File findProcessLauncherJar() {
    try {
      // Try to find the JAR containing NameNodeProcessLauncher
      Class<?> launcherClass = Class.forName(
          "org.apache.hadoop.hdfs.server.process.launcher.NameNodeProcessLauncher");

      CodeSource codeSource = launcherClass.getProtectionDomain().getCodeSource();
      if (codeSource != null) {
        URL location = codeSource.getLocation();
        File jar = new File(location.toURI());

        if (jar.exists()) {
          LOG.debug("Found process launcher JAR at: {}", jar);
          return jar;
        }
      }
    } catch (ClassNotFoundException e) {
      LOG.debug("NameNodeProcessLauncher class not found, launcher may not be compiled yet");
    } catch (URISyntaxException e) {
      LOG.warn("Error parsing JAR location: {}", e.getMessage());
    } catch (Exception e) {
      LOG.warn("Unexpected error finding launcher JAR: {}", e.getMessage());
    }

    return null;
  }

  /**
   * Adds the current JVM's classpath to the entries list.
   * This is a fallback for development/testing when JARs aren't built yet.
   */
  private static void addCurrentClasspath(List<File> entries) {
    String classpath = System.getProperty("java.class.path");
    if (classpath != null) {
      String[] paths = classpath.split(File.pathSeparator);
      for (String path : paths) {
        File entry = new File(path);
        if (entry.exists()) {
          entries.add(entry);
        }
      }
      LOG.debug("Added {} entries from current classpath", paths.length);
    }
  }

  /**
   * Gets the native library path for a distribution.
   *
   * @param distribution The Hadoop distribution
   * @return Native library path, or null if not available
   */
  public static String getNativeLibPath(HadoopDistribution distribution) {
    if (distribution == null || !distribution.isValid()) {
      return null;
    }

    File nativeLibPath = distribution.getNativeLibPath();
    return nativeLibPath != null ? nativeLibPath.getAbsolutePath() : null;
  }

  /**
   * Validates that a classpath string is not empty and contains expected components.
   *
   * @param classpath The classpath to validate
   * @return true if valid
   */
  public static boolean isValidClasspath(String classpath) {
    if (classpath == null || classpath.trim().isEmpty()) {
      return false;
    }

    String[] entries = classpath.split(File.pathSeparator);

    // Check that we have at least some JARs
    boolean hasHadoopCommon = false;
    boolean hasHadoopHdfs = false;

    for (String entry : entries) {
      String entryName = new File(entry).getName();
      if (entryName.startsWith("hadoop-common") && entryName.endsWith(".jar")) {
        hasHadoopCommon = true;
      }
      if (entryName.startsWith("hadoop-hdfs") && entryName.endsWith(".jar")) {
        hasHadoopHdfs = true;
      }
    }

    return hasHadoopCommon && hasHadoopHdfs;
  }

  /**
   * Splits a classpath string into individual file entries.
   *
   * @param classpath Classpath string
   * @return List of File objects
   */
  public static List<File> splitClasspath(String classpath) {
    List<File> files = new ArrayList<>();

    if (classpath == null || classpath.trim().isEmpty()) {
      return files;
    }

    String[] entries = classpath.split(File.pathSeparator);
    for (String entry : entries) {
      if (!entry.trim().isEmpty()) {
        files.add(new File(entry));
      }
    }

    return files;
  }

  /**
   * Joins file entries into a classpath string.
   *
   * @param files List of files/directories
   * @return Classpath string
   */
  public static String joinClasspath(List<File> files) {
    if (files == null || files.isEmpty()) {
      return "";
    }

    StringBuilder cp = new StringBuilder();
    for (File file : files) {
      if (cp.length() > 0) {
        cp.append(File.pathSeparator);
      }
      cp.append(file.getAbsolutePath());
    }

    return cp.toString();
  }

  /**
   * Returns a human-readable summary of classpath entries.
   *
   * @param classpath Classpath string
   * @return Summary string with entry count and key JARs
   */
  public static String summarizeClasspath(String classpath) {
    if (classpath == null || classpath.trim().isEmpty()) {
      return "Empty classpath";
    }

    String[] entries = classpath.split(File.pathSeparator);
    int jarCount = 0;
    int dirCount = 0;
    List<String> keyJars = new ArrayList<>();

    for (String entry : entries) {
      File file = new File(entry);
      if (file.isDirectory()) {
        dirCount++;
      } else if (file.getName().endsWith(".jar")) {
        jarCount++;
        String name = file.getName();
        if (name.startsWith("hadoop-common") || name.startsWith("hadoop-hdfs")
            || name.startsWith("hadoop-yarn") || name.startsWith("hadoop-mapreduce")) {
          keyJars.add(name);
        }
      }
    }

    return String.format("Classpath: %d entries (%d JARs, %d dirs), key JARs: %s",
        entries.length, jarCount, dirCount, keyJars);
  }
}
