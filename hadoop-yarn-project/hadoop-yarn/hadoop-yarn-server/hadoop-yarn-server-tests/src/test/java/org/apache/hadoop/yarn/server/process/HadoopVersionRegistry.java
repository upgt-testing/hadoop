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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for managing multiple Hadoop distributions. This class maintains
 * a registry of Hadoop installations that can be used to run YARN nodes
 * with different versions in separate processes.
 *
 * <p>The registry supports:</p>
 * <ul>
 *   <li>Registering multiple Hadoop distributions</li>
 *   <li>Looking up distributions by version or alias</li>
 *   <li>Validating distributions on registration</li>
 *   <li>Thread-safe concurrent access</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * HadoopVersionRegistry registry = new HadoopVersionRegistry();
 *
 * // Register distributions
 * registry.register("3.3.6", "/opt/hadoop-3.3.6");
 * registry.register("3.4.0", "/opt/hadoop-3.4.0");
 *
 * // Register with alias
 * registry.registerWithAlias("start-version", "3.3.6", "/opt/hadoop-3.3.6");
 * registry.registerWithAlias("upgrade-version", "3.4.0", "/opt/hadoop-3.4.0");
 *
 * // Lookup distribution
 * HadoopDistribution dist = registry.get("3.3.6");
 * HadoopDistribution startDist = registry.get("start-version");
 * </pre>
 *
 * @see HadoopDistribution
 */
public class HadoopVersionRegistry {

  private static final Logger LOG =
      LoggerFactory.getLogger(HadoopVersionRegistry.class);

  /** Map of version/alias to Hadoop distribution */
  private final Map<String, HadoopDistribution> distributions;

  /** Singleton instance (optional, for convenience) */
  private static HadoopVersionRegistry instance;

  /**
   * Constructs a new empty HadoopVersionRegistry.
   */
  public HadoopVersionRegistry() {
    this.distributions = new ConcurrentHashMap<>();
  }

  /**
   * Gets the singleton instance of HadoopVersionRegistry.
   *
   * @return singleton instance
   */
  public static synchronized HadoopVersionRegistry getInstance() {
    if (instance == null) {
      instance = new HadoopVersionRegistry();
    }
    return instance;
  }

  /**
   * Registers a Hadoop distribution with auto-detected version.
   * The version will be detected from the distribution itself.
   *
   * @param hadoopHome Path to Hadoop home directory
   * @return The registered HadoopDistribution
   * @throws IOException if distribution is invalid or registration fails
   */
  public HadoopDistribution register(String hadoopHome) throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    dist.validate();

    String version = dist.getVersion();
    if (distributions.containsKey(version)) {
      LOG.warn("Overwriting existing distribution for version: {}", version);
    }

    distributions.put(version, dist);
    LOG.info("Registered Hadoop distribution: {} -> {}", version, hadoopHome);

    return dist;
  }

  /**
   * Registers a Hadoop distribution with an explicit version or alias.
   *
   * @param versionOrAlias Version string or alias (e.g., "3.3.6" or "start-version")
   * @param hadoopHome Path to Hadoop home directory
   * @return The registered HadoopDistribution
   * @throws IOException if distribution is invalid or registration fails
   */
  public HadoopDistribution register(String versionOrAlias, String hadoopHome)
      throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    dist.validate();

    if (distributions.containsKey(versionOrAlias)) {
      LOG.warn("Overwriting existing distribution for: {}", versionOrAlias);
    }

    distributions.put(versionOrAlias, dist);
    LOG.info("Registered Hadoop distribution: {} -> {} (detected version: {})",
        versionOrAlias, hadoopHome, dist.getVersion());

    return dist;
  }

  /**
   * Registers a Hadoop distribution with both a version and an alias.
   * This allows lookup by either the version or the alias.
   *
   * @param alias Alias for this distribution (e.g., "start-version")
   * @param version Version string (e.g., "3.3.6")
   * @param hadoopHome Path to Hadoop home directory
   * @return The registered HadoopDistribution
   * @throws IOException if distribution is invalid or registration fails
   */
  public HadoopDistribution registerWithAlias(String alias, String version,
      String hadoopHome) throws IOException {
    HadoopDistribution dist = new HadoopDistribution(hadoopHome);
    dist.validate();

    // Register under both alias and version
    distributions.put(alias, dist);
    distributions.put(version, dist);

    LOG.info("Registered Hadoop distribution with alias: {} (version: {}) -> {}",
        alias, version, hadoopHome);

    return dist;
  }

  /**
   * Registers an already-created and validated HadoopDistribution.
   *
   * @param versionOrAlias Version string or alias
   * @param distribution The distribution to register
   * @throws IllegalArgumentException if distribution is not validated
   */
  public void register(String versionOrAlias, HadoopDistribution distribution) {
    if (!distribution.isValidated()) {
      throw new IllegalArgumentException(
          "Distribution must be validated before registration");
    }

    if (distributions.containsKey(versionOrAlias)) {
      LOG.warn("Overwriting existing distribution for: {}", versionOrAlias);
    }

    distributions.put(versionOrAlias, distribution);
    LOG.info("Registered pre-validated distribution: {} -> {}",
        versionOrAlias, distribution.getHadoopHome());
  }

  /**
   * Gets a Hadoop distribution by version or alias.
   *
   * @param versionOrAlias Version string or alias
   * @return The HadoopDistribution, or null if not found
   */
  public HadoopDistribution get(String versionOrAlias) {
    return distributions.get(versionOrAlias);
  }

  /**
   * Gets a Hadoop distribution by version or alias, throwing an exception
   * if not found.
   *
   * @param versionOrAlias Version string or alias
   * @return The HadoopDistribution
   * @throws IllegalArgumentException if distribution not found
   */
  public HadoopDistribution getRequired(String versionOrAlias) {
    HadoopDistribution dist = distributions.get(versionOrAlias);
    if (dist == null) {
      throw new IllegalArgumentException(
          "Hadoop distribution not found in registry: " + versionOrAlias +
          ". Available: " + distributions.keySet());
    }
    return dist;
  }

  /**
   * Checks if a distribution is registered.
   *
   * @param versionOrAlias Version string or alias
   * @return true if registered, false otherwise
   */
  public boolean contains(String versionOrAlias) {
    return distributions.containsKey(versionOrAlias);
  }

  /**
   * Unregisters a distribution.
   *
   * @param versionOrAlias Version string or alias
   * @return The removed distribution, or null if not found
   */
  public HadoopDistribution unregister(String versionOrAlias) {
    HadoopDistribution removed = distributions.remove(versionOrAlias);
    if (removed != null) {
      LOG.info("Unregistered Hadoop distribution: {}", versionOrAlias);
    }
    return removed;
  }

  /**
   * Clears all registered distributions.
   */
  public void clear() {
    int count = distributions.size();
    distributions.clear();
    LOG.info("Cleared {} registered distributions", count);
  }

  /**
   * Gets the number of registered distributions (including aliases).
   *
   * @return Number of registered entries
   */
  public int size() {
    return distributions.size();
  }

  /**
   * Checks if the registry is empty.
   *
   * @return true if no distributions are registered
   */
  public boolean isEmpty() {
    return distributions.isEmpty();
  }

  /**
   * Gets all registered version strings and aliases.
   *
   * @return Collection of version strings and aliases
   */
  public Collection<String> getRegisteredKeys() {
    return Collections.unmodifiableSet(distributions.keySet());
  }

  /**
   * Gets all registered distributions.
   *
   * @return Collection of distributions (may contain duplicates if aliases exist)
   */
  public Collection<HadoopDistribution> getDistributions() {
    return Collections.unmodifiableCollection(distributions.values());
  }

  /**
   * Creates a registry from system properties. Looks for:
   * <ul>
   *   <li>hadoop.start.home - Registered as "start-version"</li>
   *   <li>hadoop.upgrade.home - Registered as "upgrade-version"</li>
   * </ul>
   *
   * @return A new registry with distributions from system properties
   * @throws IOException if distributions are invalid
   */
  public static HadoopVersionRegistry fromSystemProperties()
      throws IOException {
    HadoopVersionRegistry registry = new HadoopVersionRegistry();

    String startHome = System.getProperty("hadoop.start.home");
    if (startHome != null && !startHome.isEmpty()) {
      LOG.info("Registering start version from system property: {}", startHome);
      registry.register("start-version", startHome);
    }

    String upgradeHome = System.getProperty("hadoop.upgrade.home");
    if (upgradeHome != null && !upgradeHome.isEmpty()) {
      LOG.info("Registering upgrade version from system property: {}",
          upgradeHome);
      registry.register("upgrade-version", upgradeHome);
    }

    return registry;
  }

  @Override
  public String toString() {
    return "HadoopVersionRegistry{" +
        "size=" + distributions.size() +
        ", keys=" + distributions.keySet() +
        '}';
  }
}
