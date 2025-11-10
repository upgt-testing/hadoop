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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts YARN configuration between different Hadoop versions. This class
 * handles version-specific configuration key mappings, deprecated properties,
 * and version-specific defaults to enable compatibility testing across
 * different Hadoop versions.
 *
 * <p>Hadoop configuration keys and defaults may change between versions.
 * This adapter ensures that configurations generated for one version work
 * correctly when applied to a different version.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Config key mapping for renamed properties</li>
 *   <li>Deprecated property handling</li>
 *   <li>Version-specific defaults</li>
 *   <li>Bidirectional compatibility (3.3.x ↔ 3.4.x)</li>
 *   <li>Extensible for future versions</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Create adapter for target version
 * VersionConfigAdapter adapter =
 *     VersionConfigAdapter.forVersion("3.4.0");
 *
 * // Adapt configuration from 3.3.x to 3.4.x
 * YarnConfiguration adapted = adapter.adapt(config33x, "3.3.6");
 *
 * // Apply version-specific defaults
 * adapter.applyDefaults(config);
 * </pre>
 *
 * @see HadoopDistribution
 * @see ProcessConfigurationGenerator
 */
public class VersionConfigAdapter {

  private static final Logger LOG =
      LoggerFactory.getLogger(VersionConfigAdapter.class);

  /** Pattern to extract major.minor version (e.g., "3.3" from "3.3.6") */
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("^(\\d+)\\.(\\d+)");

  /** Target Hadoop version for this adapter */
  private final String targetVersion;

  /** Target major.minor version (e.g., "3.3", "3.4") */
  private final String targetMajorMinor;

  /** Configuration key mappings: old key -> new key */
  private final Map<String, String> keyMappings;

  /** Deprecated keys that should be removed */
  private final Map<String, String> deprecatedKeys;

  /** Version-specific default values */
  private final Map<String, String> versionDefaults;

  /**
   * Creates a VersionConfigAdapter for the specified target version.
   *
   * @param targetVersion Target Hadoop version (e.g., "3.4.0")
   */
  public VersionConfigAdapter(String targetVersion) {
    if (targetVersion == null || targetVersion.isEmpty()) {
      throw new IllegalArgumentException("Target version cannot be null or empty");
    }

    this.targetVersion = targetVersion;
    this.targetMajorMinor = extractMajorMinor(targetVersion);
    this.keyMappings = new HashMap<>();
    this.deprecatedKeys = new HashMap<>();
    this.versionDefaults = new HashMap<>();

    initializeMappings();
    initializeDefaults();

    LOG.debug("Created VersionConfigAdapter for version: {} ({})",
        targetVersion, targetMajorMinor);
  }

  /**
   * Factory method to create an adapter for a specific version.
   *
   * @param targetVersion Target Hadoop version
   * @return VersionConfigAdapter for the target version
   */
  public static VersionConfigAdapter forVersion(String targetVersion) {
    return new VersionConfigAdapter(targetVersion);
  }

  /**
   * Adapts a configuration from a source version to the target version.
   * This handles key mappings, deprecated properties, and version-specific
   * value adjustments.
   *
   * @param sourceConfig Source configuration
   * @param sourceVersion Source Hadoop version
   * @return Adapted configuration for target version
   */
  public YarnConfiguration adapt(Configuration sourceConfig,
      String sourceVersion) {
    String sourceMajorMinor = extractMajorMinor(sourceVersion);

    LOG.info("Adapting configuration: {} -> {}",
        sourceMajorMinor, targetMajorMinor);

    // Create new configuration
    YarnConfiguration adapted = new YarnConfiguration(sourceConfig);

    // Apply key mappings if versions differ
    if (!sourceMajorMinor.equals(targetMajorMinor)) {
      applyKeyMappings(adapted, sourceMajorMinor, targetMajorMinor);
      removeDeprecatedKeys(adapted, sourceMajorMinor, targetMajorMinor);
    }

    // Apply version-specific defaults
    applyDefaults(adapted);

    return adapted;
  }

  /**
   * Applies version-specific default values to a configuration.
   *
   * @param config Configuration to modify
   */
  public void applyDefaults(Configuration config) {
    int applied = 0;
    for (Map.Entry<String, String> entry : versionDefaults.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      // Only set if not already configured
      if (config.get(key) == null) {
        config.set(key, value);
        applied++;
        LOG.trace("Applied default: {}={}", key, value);
      }
    }

    if (applied > 0) {
      LOG.debug("Applied {} version-specific defaults for version {}",
          applied, targetMajorMinor);
    }
  }

  /**
   * Applies configuration key mappings from source to target version.
   *
   * @param config Configuration to modify
   * @param sourceMajorMinor Source version (major.minor)
   * @param targetMajorMinor Target version (major.minor)
   */
  private void applyKeyMappings(Configuration config,
      String sourceMajorMinor, String targetMajorMinor) {
    int mapped = 0;

    for (Map.Entry<String, String> entry : keyMappings.entrySet()) {
      String oldKey = entry.getKey();
      String newKey = entry.getValue();

      // Check if old key is set
      String value = config.get(oldKey);
      if (value != null) {
        // Set new key and unset old key
        config.set(newKey, value);
        config.unset(oldKey);
        mapped++;
        LOG.debug("Mapped config key: {} -> {} (value={})",
            oldKey, newKey, value);
      }
    }

    if (mapped > 0) {
      LOG.info("Mapped {} configuration keys: {} -> {}",
          mapped, sourceMajorMinor, targetMajorMinor);
    }
  }

  /**
   * Removes deprecated configuration keys.
   *
   * @param config Configuration to modify
   * @param sourceMajorMinor Source version (major.minor)
   * @param targetMajorMinor Target version (major.minor)
   */
  private void removeDeprecatedKeys(Configuration config,
      String sourceMajorMinor, String targetMajorMinor) {
    int removed = 0;

    for (Map.Entry<String, String> entry : deprecatedKeys.entrySet()) {
      String key = entry.getKey();
      String replacement = entry.getValue();

      if (config.get(key) != null) {
        config.unset(key);
        removed++;
        LOG.debug("Removed deprecated key: {} (use {} instead)",
            key, replacement);
      }
    }

    if (removed > 0) {
      LOG.info("Removed {} deprecated keys: {} -> {}",
          removed, sourceMajorMinor, targetMajorMinor);
    }
  }

  /**
   * Initializes configuration key mappings for known version differences.
   * This method is called during construction to set up mappings between
   * different Hadoop versions.
   */
  private void initializeMappings() {
    // Example mappings (add actual version-specific mappings as needed)

    // Hadoop 3.3.x -> 3.4.x example mappings
    // Note: These are examples - actual mappings depend on real version differences
    if (targetMajorMinor.equals("3.4")) {
      // Example: If a property was renamed in 3.4.x
      // keyMappings.put("yarn.old.property.name", "yarn.new.property.name");

      // Example: Deprecated properties in 3.4.x
      // deprecatedKeys.put("yarn.deprecated.property", "yarn.replacement.property");
    }

    // Hadoop 3.4.x -> 3.3.x backward compatibility
    if (targetMajorMinor.equals("3.3")) {
      // Reverse mappings for backward compatibility
      // keyMappings.put("yarn.new.property.name", "yarn.old.property.name");
    }

    LOG.debug("Initialized {} key mappings and {} deprecated keys",
        keyMappings.size(), deprecatedKeys.size());
  }

  /**
   * Initializes version-specific default values.
   * This method is called during construction to set up version-specific
   * defaults that differ from the standard Hadoop defaults.
   */
  private void initializeDefaults() {
    // Version-specific defaults for testing/process-based clusters

    if (targetMajorMinor.equals("3.3")) {
      // Hadoop 3.3.x specific defaults
      addTestingDefaults();
    } else if (targetMajorMinor.equals("3.4")) {
      // Hadoop 3.4.x specific defaults
      addTestingDefaults();

      // Example: New property introduced in 3.4.x
      // versionDefaults.put("yarn.new.feature.enabled", "true");
    }

    LOG.debug("Initialized {} version-specific defaults",
        versionDefaults.size());
  }

  /**
   * Adds common testing defaults that are appropriate for process-based
   * test clusters regardless of version.
   */
  private void addTestingDefaults() {
    // These are test-friendly defaults that work across versions

    // Fast timeouts for testing
    versionDefaults.put(
        YarnConfiguration.RESOURCEMANAGER_CONNECT_RETRY_INTERVAL_MS,
        "1000");

    versionDefaults.put(
        YarnConfiguration.CLIENT_FAILOVER_SLEEPTIME_BASE_MS,
        "100");

    versionDefaults.put(
        YarnConfiguration.CLIENT_FAILOVER_SLEEPTIME_MAX_MS,
        "1000");

    // Disable timeline service by default in tests
    versionDefaults.put(
        YarnConfiguration.TIMELINE_SERVICE_ENABLED,
        "false");

    // Enable minicluster mode
    versionDefaults.put(
        YarnConfiguration.IS_MINI_YARN_CLUSTER,
        "true");
  }

  /**
   * Extracts major.minor version from a full version string.
   *
   * @param version Full version string (e.g., "3.3.6", "3.4.0-SNAPSHOT")
   * @return Major.minor version (e.g., "3.3", "3.4")
   */
  private String extractMajorMinor(String version) {
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (matcher.find()) {
      return matcher.group(1) + "." + matcher.group(2);
    }
    LOG.warn("Could not extract major.minor from version: {}", version);
    return version;
  }

  /**
   * Checks if two versions are compatible (same major.minor version).
   *
   * @param version1 First version
   * @param version2 Second version
   * @return true if versions are compatible
   */
  public static boolean areCompatible(String version1, String version2) {
    VersionConfigAdapter adapter1 = new VersionConfigAdapter(version1);
    VersionConfigAdapter adapter2 = new VersionConfigAdapter(version2);

    return adapter1.targetMajorMinor.equals(adapter2.targetMajorMinor);
  }

  /**
   * Gets the target version for this adapter.
   *
   * @return Target Hadoop version
   */
  public String getTargetVersion() {
    return targetVersion;
  }

  /**
   * Gets the target major.minor version.
   *
   * @return Major.minor version (e.g., "3.3", "3.4")
   */
  public String getTargetMajorMinor() {
    return targetMajorMinor;
  }

  /**
   * Gets the number of configured key mappings.
   *
   * @return Number of key mappings
   */
  public int getKeyMappingCount() {
    return keyMappings.size();
  }

  /**
   * Gets the number of version-specific defaults.
   *
   * @return Number of defaults
   */
  public int getDefaultsCount() {
    return versionDefaults.size();
  }

  @Override
  public String toString() {
    return "VersionConfigAdapter{" +
        "targetVersion='" + targetVersion + '\'' +
        ", targetMajorMinor='" + targetMajorMinor + '\'' +
        ", keyMappings=" + keyMappings.size() +
        ", defaults=" + versionDefaults.size() +
        '}';
  }
}
