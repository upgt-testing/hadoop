/*
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

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VersionConfigAdapter handles configuration differences between Hadoop versions.
 *
 * This class provides:
 * - Mapping of deprecated/renamed configuration keys
 * - Version-specific default values
 * - Configuration validation based on version
 * - Feature availability checking
 *
 * Supports Hadoop 2.x and 3.x series with special handling for:
 * - Configuration key renames between major versions
 * - Deprecated properties that still work in some versions
 * - Version-specific features that need different configuration
 */
public class VersionConfigAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(VersionConfigAdapter.class);

  // Version information
  private final String hadoopVersion;
  private final int majorVersion;
  private final int minorVersion;
  private final int patchVersion;

  // Configuration key mappings: old key -> new key
  private static final Map<String, String> KEY_MAPPINGS = new HashMap<>();

  // Deprecated keys that should be replaced
  private static final Set<String> DEPRECATED_KEYS = new HashSet<>();

  // Version-specific default values: key -> version range -> default value
  private static final Map<String, Map<VersionRange, String>> VERSION_DEFAULTS = new HashMap<>();

  static {
    initializeKeyMappings();
    initializeDeprecatedKeys();
    initializeVersionDefaults();
  }

  /**
   * Creates a version config adapter for the specified Hadoop version.
   *
   * @param hadoopVersion the Hadoop version string (e.g., "3.3.5", "2.10.2")
   * @throws IllegalArgumentException if version string is invalid
   */
  public VersionConfigAdapter(String hadoopVersion) {
    if (hadoopVersion == null || hadoopVersion.trim().isEmpty()) {
      throw new IllegalArgumentException("Hadoop version cannot be null or empty");
    }

    this.hadoopVersion = hadoopVersion;
    String[] parts = hadoopVersion.split("\\.");

    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid Hadoop version format: " + hadoopVersion);
    }

    try {
      this.majorVersion = Integer.parseInt(parts[0]);
      this.minorVersion = Integer.parseInt(parts[1]);
      this.patchVersion = parts.length > 2 ? Integer.parseInt(parts[2].split("-")[0]) : 0;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid Hadoop version format: " + hadoopVersion, e);
    }

    LOG.info("Created VersionConfigAdapter for Hadoop {}.{}.{}",
        majorVersion, minorVersion, patchVersion);
  }

  /**
   * Adapts a configuration object for the target Hadoop version.
   * This method:
   * - Replaces deprecated keys with their new equivalents
   * - Applies version-specific defaults
   * - Removes keys not supported in the target version
   *
   * @param config the configuration to adapt
   * @return a new adapted configuration
   */
  public Configuration adaptConfiguration(Configuration config) {
    Configuration adapted = new Configuration(config);

    // Replace deprecated keys
    for (Map.Entry<String, String> entry : KEY_MAPPINGS.entrySet()) {
      String oldKey = entry.getKey();
      String newKey = entry.getValue();

      if (config.get(oldKey) != null && config.get(newKey) == null) {
        String value = config.get(oldKey);
        adapted.set(newKey, value);
        adapted.unset(oldKey);
        LOG.debug("Mapped deprecated key '{}' to '{}' with value '{}'", oldKey, newKey, value);
      }
    }

    // Apply version-specific defaults
    applyVersionDefaults(adapted);

    // Remove unsupported keys
    removeUnsupportedKeys(adapted);

    return adapted;
  }

  /**
   * Checks if a configuration key is supported in this Hadoop version.
   *
   * @param key the configuration key
   * @return true if supported, false otherwise
   */
  public boolean isKeySupported(String key) {
    // For now, all keys are considered supported unless explicitly deprecated
    // In the future, we can add version-specific key support checking
    return !isKeyDeprecated(key);
  }

  /**
   * Checks if a configuration key is deprecated in this version.
   *
   * @param key the configuration key
   * @return true if deprecated, false otherwise
   */
  public boolean isKeyDeprecated(String key) {
    return DEPRECATED_KEYS.contains(key);
  }

  /**
   * Gets the current key name for a potentially deprecated key.
   * If the key has been renamed, returns the new name. Otherwise returns the original key.
   *
   * @param key the configuration key (may be old/deprecated)
   * @return the current key name to use
   */
  public String getCurrentKeyName(String key) {
    return KEY_MAPPINGS.getOrDefault(key, key);
  }

  /**
   * Checks if a feature is available in this Hadoop version.
   *
   * @param featureName the feature name
   * @return true if the feature is available, false otherwise
   */
  public boolean isFeatureAvailable(String featureName) {
    switch (featureName) {
      case "HDFS_ROUTER_FEDERATION":
        // Router-based federation available in 3.0.0+
        return majorVersion >= 3;

      case "ERASURE_CODING":
        // Erasure coding available in 3.0.0+
        return majorVersion >= 3;

      case "NAMENODE_HA":
        // NameNode HA available in 2.0.0+
        return majorVersion >= 2;

      case "HDFS_SNAPSHOTS":
        // Snapshots available in 2.1.0+
        return majorVersion > 2 || (majorVersion == 2 && minorVersion >= 1);

      case "HDFS_ENCRYPTION":
        // HDFS encryption available in 2.6.0+
        return majorVersion > 2 || (majorVersion == 2 && minorVersion >= 6);

      case "STORAGE_POLICY":
        // Storage policy available in 2.6.0+
        return majorVersion > 2 || (majorVersion == 2 && minorVersion >= 6);

      default:
        LOG.warn("Unknown feature: {}", featureName);
        return false;
    }
  }

  /**
   * Gets the Hadoop version string.
   *
   * @return the Hadoop version (e.g., "3.3.5")
   */
  public String getHadoopVersion() {
    return hadoopVersion;
  }

  /**
   * Gets the major version number.
   *
   * @return the major version (e.g., 3)
   */
  public int getMajorVersion() {
    return majorVersion;
  }

  /**
   * Gets the minor version number.
   *
   * @return the minor version (e.g., 3)
   */
  public int getMinorVersion() {
    return minorVersion;
  }

  /**
   * Gets the patch version number.
   *
   * @return the patch version (e.g., 5)
   */
  public int getPatchVersion() {
    return patchVersion;
  }

  /**
   * Checks if this version is compatible with another version for mixed-version clusters.
   *
   * @param otherVersion the other version to check compatibility with
   * @return true if versions are compatible, false otherwise
   */
  public boolean isCompatibleWith(VersionConfigAdapter otherVersion) {
    // Same major version is generally compatible
    if (this.majorVersion != otherVersion.majorVersion) {
      LOG.warn("Major version mismatch: {} vs {}", this.majorVersion, otherVersion.majorVersion);
      return false;
    }

    // Within Hadoop 3.x, minor versions are generally compatible
    if (this.majorVersion == 3) {
      // Allow different minor versions within 3.x
      return true;
    }

    // Within Hadoop 2.x, be more conservative
    if (this.majorVersion == 2) {
      // Allow same minor version or adjacent minor versions
      int minorDiff = Math.abs(this.minorVersion - otherVersion.minorVersion);
      return minorDiff <= 1;
    }

    return false;
  }

  /**
   * Applies version-specific default values to the configuration.
   */
  private void applyVersionDefaults(Configuration config) {
    for (Map.Entry<String, Map<VersionRange, String>> entry : VERSION_DEFAULTS.entrySet()) {
      String key = entry.getKey();

      // Only apply default if key is not already set
      if (config.get(key) == null) {
        for (Map.Entry<VersionRange, String> versionEntry : entry.getValue().entrySet()) {
          VersionRange range = versionEntry.getKey();
          if (range.contains(majorVersion, minorVersion, patchVersion)) {
            String defaultValue = versionEntry.getValue();
            config.set(key, defaultValue);
            LOG.debug("Applied version-specific default for '{}': {}", key, defaultValue);
            break;
          }
        }
      }
    }
  }

  /**
   * Removes configuration keys that are not supported in this version.
   */
  private void removeUnsupportedKeys(Configuration config) {
    // For Hadoop 2.x, remove 3.x-specific keys
    if (majorVersion == 2) {
      // Example: Remove router federation keys in 2.x
      String[] hadoop3OnlyKeys = {
        "dfs.federation.router.default.nameserviceId",
        "dfs.namenode.provided.enabled"
      };

      for (String key : hadoop3OnlyKeys) {
        if (config.get(key) != null) {
          config.unset(key);
          LOG.debug("Removed unsupported key for Hadoop 2.x: {}", key);
        }
      }
    }
  }

  /**
   * Initializes the mapping of deprecated/renamed configuration keys.
   */
  private static void initializeKeyMappings() {
    // Examples of key renames between versions
    // Note: Most Hadoop config keys are backward compatible, but this provides a mechanism
    // KEY_MAPPINGS.put("old.key.name", "new.key.name");

    // Example: In early Hadoop 2.x, some keys were renamed
    // These are hypothetical examples for demonstration
    KEY_MAPPINGS.put("dfs.namenode.rpc-address", "dfs.namenode.rpc-address"); // No change, for illustration
  }

  /**
   * Initializes the set of deprecated configuration keys.
   */
  private static void initializeDeprecatedKeys() {
    // Keys that are deprecated and should be replaced
    // These are examples based on Hadoop evolution

    DEPRECATED_KEYS.add("dfs.block.size"); // Use dfs.blocksize instead
    DEPRECATED_KEYS.add("dfs.datanode.max.xcievers"); // Use dfs.datanode.max.transfer.threads
  }

  /**
   * Initializes version-specific default values.
   */
  private static void initializeVersionDefaults() {
    // Example: Different default values for different versions
    Map<VersionRange, String> blockSizeDefaults = new HashMap<>();
    blockSizeDefaults.put(new VersionRange(2, 0, 0, 2, 999, 999), "134217728"); // 128MB for 2.x
    blockSizeDefaults.put(new VersionRange(3, 0, 0, 3, 999, 999), "134217728"); // 128MB for 3.x
    VERSION_DEFAULTS.put("dfs.blocksize", blockSizeDefaults);
  }

  /**
   * Represents a version range for applying version-specific configuration.
   */
  private static class VersionRange {
    private final int minMajor;
    private final int minMinor;
    private final int minPatch;
    private final int maxMajor;
    private final int maxMinor;
    private final int maxPatch;

    public VersionRange(int minMajor, int minMinor, int minPatch,
                       int maxMajor, int maxMinor, int maxPatch) {
      this.minMajor = minMajor;
      this.minMinor = minMinor;
      this.minPatch = minPatch;
      this.maxMajor = maxMajor;
      this.maxMinor = maxMinor;
      this.maxPatch = maxPatch;
    }

    public boolean contains(int major, int minor, int patch) {
      // Check if version is within range
      if (major < minMajor || major > maxMajor) {
        return false;
      }
      if (major == minMajor && minor < minMinor) {
        return false;
      }
      if (major == minMajor && minor == minMinor && patch < minPatch) {
        return false;
      }
      if (major == maxMajor && minor > maxMinor) {
        return false;
      }
      if (major == maxMajor && minor == maxMinor && patch > maxPatch) {
        return false;
      }
      return true;
    }
  }
}
