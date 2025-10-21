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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing multiple Hadoop distributions for multi-version testing.
 *
 * This class allows registering different Hadoop installations and retrieving
 * their distribution information by version or alias. It validates each
 * distribution upon registration to ensure it's complete and usable.
 *
 * Example usage:
 * <pre>
 * HadoopVersionRegistry registry = new HadoopVersionRegistry();
 * registry.register("3.3.1", "/opt/hadoop-3.3.1");
 * registry.register("3.3.5", "/opt/hadoop-3.3.5");
 *
 * HadoopDistribution dist = registry.get("3.3.5");
 * String classpath = dist.buildClasspath();
 * </pre>
 */
public class HadoopVersionRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(HadoopVersionRegistry.class);

  /**
   * Map of version/alias -> HadoopDistribution
   * Keys can be version strings like "3.3.5" or custom aliases
   */
  private final Map<String, HadoopDistribution> distributions;

  /**
   * Map of hadoopHome path -> version/alias for deduplication
   */
  private final Map<String, String> pathToVersion;

  public HadoopVersionRegistry() {
    this.distributions = new HashMap<>();
    this.pathToVersion = new HashMap<>();
  }

  /**
   * Registers a Hadoop distribution with a given version/alias.
   *
   * @param versionOrAlias Version string (e.g., "3.3.5") or custom alias (e.g., "latest")
   * @param hadoopHome Path to Hadoop installation directory
   * @throws IOException if the distribution cannot be loaded or is invalid
   * @throws IllegalArgumentException if version is already registered
   */
  public void register(String versionOrAlias, String hadoopHome) throws IOException {
    if (versionOrAlias == null || versionOrAlias.trim().isEmpty()) {
      throw new IllegalArgumentException("Version/alias cannot be null or empty");
    }

    if (hadoopHome == null || hadoopHome.trim().isEmpty()) {
      throw new IllegalArgumentException("Hadoop home path cannot be null or empty");
    }

    File hadoopHomeFile = new File(hadoopHome);

    // Check if this version is already registered
    if (distributions.containsKey(versionOrAlias)) {
      HadoopDistribution existing = distributions.get(versionOrAlias);
      if (existing.getHadoopHome().equals(hadoopHomeFile)) {
        LOG.debug("Version {} already registered with same path, skipping", versionOrAlias);
        return;
      } else {
        throw new IllegalArgumentException(
            "Version " + versionOrAlias + " is already registered with different path: "
                + existing.getHadoopHome() + " (new path: " + hadoopHome + ")");
      }
    }

    // Check if this hadoopHome is already registered under different version
    String canonicalPath = hadoopHomeFile.getAbsolutePath();
    if (pathToVersion.containsKey(canonicalPath)) {
      String existingVersion = pathToVersion.get(canonicalPath);
      LOG.debug("Hadoop home {} already registered as version {}, creating alias {}",
          canonicalPath, existingVersion, versionOrAlias);
      // Allow aliasing - same distribution, different name
      distributions.put(versionOrAlias, distributions.get(existingVersion));
      return;
    }

    // Create and validate distribution
    HadoopDistribution distribution = new HadoopDistribution(hadoopHomeFile);

    if (!distribution.isValid()) {
      throw new IOException(
          "Invalid Hadoop distribution at " + hadoopHome + ": " + distribution.getValidationError());
    }

    // Register the distribution
    distributions.put(versionOrAlias, distribution);
    pathToVersion.put(canonicalPath, versionOrAlias);

    LOG.info("Registered Hadoop distribution: {} -> {}", versionOrAlias, distribution);
  }

  /**
   * Registers a Hadoop distribution, auto-detecting the version from the directory.
   *
   * @param hadoopHome Path to Hadoop installation directory
   * @return The detected version string that was registered
   * @throws IOException if the distribution cannot be loaded or is invalid
   */
  public String registerAuto(String hadoopHome) throws IOException {
    File hadoopHomeFile = new File(hadoopHome);
    HadoopDistribution distribution = new HadoopDistribution(hadoopHomeFile);

    if (!distribution.isValid()) {
      throw new IOException(
          "Invalid Hadoop distribution at " + hadoopHome + ": " + distribution.getValidationError());
    }

    String version = distribution.getVersion();
    register(version, hadoopHome);
    return version;
  }

  /**
   * Retrieves a registered Hadoop distribution by version/alias.
   *
   * @param versionOrAlias Version string or alias
   * @return The HadoopDistribution, or null if not found
   */
  public HadoopDistribution get(String versionOrAlias) {
    return distributions.get(versionOrAlias);
  }

  /**
   * Retrieves a registered Hadoop distribution by version/alias.
   *
   * @param versionOrAlias Version string or alias
   * @return The HadoopDistribution
   * @throws IllegalArgumentException if not found
   */
  public HadoopDistribution getRequired(String versionOrAlias) {
    HadoopDistribution dist = distributions.get(versionOrAlias);
    if (dist == null) {
      throw new IllegalArgumentException(
          "No Hadoop distribution registered for version: " + versionOrAlias
              + ". Available versions: " + getRegisteredVersions());
    }
    return dist;
  }

  /**
   * Checks if a version/alias is registered.
   *
   * @param versionOrAlias Version string or alias
   * @return true if registered
   */
  public boolean isRegistered(String versionOrAlias) {
    return distributions.containsKey(versionOrAlias);
  }

  /**
   * Gets all registered version/alias strings.
   *
   * @return Set of registered versions and aliases
   */
  public Set<String> getRegisteredVersions() {
    return distributions.keySet();
  }

  /**
   * Gets the number of registered distributions.
   *
   * @return Number of registered distributions
   */
  public int size() {
    return distributions.size();
  }

  /**
   * Clears all registered distributions.
   */
  public void clear() {
    distributions.clear();
    pathToVersion.clear();
  }

  /**
   * Gets core JAR files for a registered distribution.
   *
   * @param versionOrAlias Version string or alias
   * @return List of core JAR files
   * @throws IllegalArgumentException if version not registered
   */
  public List<File> getJars(String versionOrAlias) {
    return getRequired(versionOrAlias).getCoreJars();
  }

  /**
   * Gets dependency JAR files for a registered distribution.
   *
   * @param versionOrAlias Version string or alias
   * @return List of dependency JAR files
   * @throws IllegalArgumentException if version not registered
   */
  public List<File> getDependencies(String versionOrAlias) {
    return getRequired(versionOrAlias).getDependencies();
  }

  /**
   * Builds a complete classpath for a registered distribution.
   *
   * @param versionOrAlias Version string or alias
   * @return Classpath string
   * @throws IllegalArgumentException if version not registered
   */
  public String buildClasspath(String versionOrAlias) {
    return getRequired(versionOrAlias).buildClasspath();
  }

  /**
   * Builds a complete classpath with additional entries prepended.
   *
   * @param versionOrAlias Version string or alias
   * @param additionalEntries Additional classpath entries
   * @return Classpath string
   * @throws IllegalArgumentException if version not registered
   */
  public String buildClasspath(String versionOrAlias, List<File> additionalEntries) {
    return getRequired(versionOrAlias).buildClasspath(additionalEntries);
  }

  /**
   * Returns a summary of registered distributions for debugging.
   */
  @Override
  public String toString() {
    return "HadoopVersionRegistry{distributions=" + distributions.keySet() + "}";
  }
}
