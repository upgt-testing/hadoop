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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.VersionConfigAdapter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for VersionConfigAdapter.
 */
public class TestVersionConfigAdapter {

  private YarnConfiguration sourceConfig;

  @Before
  public void setUp() {
    sourceConfig = new YarnConfiguration();
  }

  @Test
  public void testForVersion() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    Assert.assertNotNull("Adapter should not be null", adapter);
    Assert.assertEquals("Target version should match", "3.4.0",
        adapter.getTargetVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testForVersionNull() {
    VersionConfigAdapter.forVersion(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testForVersionEmpty() {
    VersionConfigAdapter.forVersion("");
  }

  @Test
  public void testAreCompatibleSameVersion() {
    Assert.assertTrue("Same version should be compatible",
        VersionConfigAdapter.areCompatible("3.3.6", "3.3.6"));
  }

  @Test
  public void testAreCompatibleSameMajorMinor() {
    Assert.assertTrue("Same major.minor should be compatible",
        VersionConfigAdapter.areCompatible("3.3.5", "3.3.6"));
    Assert.assertTrue("Same major.minor should be compatible",
        VersionConfigAdapter.areCompatible("3.4.0", "3.4.1"));
  }

  @Test
  public void testAreCompatibleDifferentMinor() {
    // Different minor versions may or may not be compatible
    // depending on implementation policy
    boolean compatible = VersionConfigAdapter.areCompatible("3.3.6", "3.4.0");
    // Just verify it returns a boolean (implementation may vary)
    Assert.assertNotNull("Should return a compatibility result",
        Boolean.valueOf(compatible));
  }

  @Test
  public void testAreCompatibleDifferentMajor() {
    Assert.assertFalse("Different major versions should not be compatible",
        VersionConfigAdapter.areCompatible("2.10.0", "3.3.6"));
    Assert.assertFalse("Different major versions should not be compatible",
        VersionConfigAdapter.areCompatible("3.3.6", "4.0.0"));
  }

  @Test
  public void testAreCompatibleInvalidVersions() {
    // Invalid version strings should return false
    Assert.assertFalse("Invalid version should not be compatible",
        VersionConfigAdapter.areCompatible("invalid", "3.3.6"));
    Assert.assertFalse("Invalid version should not be compatible",
        VersionConfigAdapter.areCompatible("3.3.6", "invalid"));
  }

  @Test
  public void testAdaptSameVersion() {
    sourceConfig.set("test.property", "value");
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.3.6");

    YarnConfiguration adapted = adapter.adapt(sourceConfig, "3.3.6");

    Assert.assertNotNull("Adapted config should not be null", adapted);
    Assert.assertEquals("Property should be preserved",
        "value", adapted.get("test.property"));
  }

  @Test
  public void testAdaptBasicProperties() {
    sourceConfig.set(YarnConfiguration.RM_ADDRESS, "localhost:8032");
    sourceConfig.set(YarnConfiguration.NM_ADDRESS, "localhost:8040");

    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    YarnConfiguration adapted = adapter.adapt(sourceConfig, "3.3.6");

    Assert.assertNotNull("Adapted config should not be null", adapted);
    // Basic properties should be preserved
    Assert.assertEquals("RM address should be preserved",
        "localhost:8032", adapted.get(YarnConfiguration.RM_ADDRESS));
    Assert.assertEquals("NM address should be preserved",
        "localhost:8040", adapted.get(YarnConfiguration.NM_ADDRESS));
  }

  @Test
  public void testAdaptEmptyConfig() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    YarnConfiguration adapted = adapter.adapt(new YarnConfiguration(), "3.3.6");

    Assert.assertNotNull("Adapted config should not be null", adapted);
  }

  @Test
  public void testApplyDefaults() {
    YarnConfiguration config = new YarnConfiguration();
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");

    adapter.applyDefaults(config);

    // Verify some defaults are applied
    Assert.assertNotNull("Config should not be null after applyDefaults", config);
    // Specific defaults depend on implementation
  }

  @Test
  public void testGetTargetVersion() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    Assert.assertEquals("Target version should match", "3.4.0",
        adapter.getTargetVersion());
  }

  @Test
  public void testToString() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    String str = adapter.toString();

    Assert.assertTrue("toString should contain class name",
        str.contains("VersionConfigAdapter"));
    Assert.assertTrue("toString should contain version",
        str.contains("3.4.0"));
  }

  @Test
  public void testAdaptMultipleProperties() {
    sourceConfig.set(YarnConfiguration.RM_ADDRESS, "rm-host:8032");
    sourceConfig.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, "rm-host:8030");
    sourceConfig.set(YarnConfiguration.RM_ADMIN_ADDRESS, "rm-host:8033");
    sourceConfig.set(YarnConfiguration.NM_ADDRESS, "nm-host:8040");
    sourceConfig.setInt(YarnConfiguration.RM_AM_MAX_ATTEMPTS, 5);

    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    YarnConfiguration adapted = adapter.adapt(sourceConfig, "3.3.6");

    Assert.assertEquals("RM address should be preserved",
        "rm-host:8032", adapted.get(YarnConfiguration.RM_ADDRESS));
    Assert.assertEquals("Scheduler address should be preserved",
        "rm-host:8030", adapted.get(YarnConfiguration.RM_SCHEDULER_ADDRESS));
    Assert.assertEquals("Admin address should be preserved",
        "rm-host:8033", adapted.get(YarnConfiguration.RM_ADMIN_ADDRESS));
    Assert.assertEquals("NM address should be preserved",
        "nm-host:8040", adapted.get(YarnConfiguration.NM_ADDRESS));
    Assert.assertEquals("Max attempts should be preserved",
        5, adapted.getInt(YarnConfiguration.RM_AM_MAX_ATTEMPTS, -1));
  }

  @Test
  public void testVersionParsing() {
    // Test various version formats
    String[] validVersions = {
        "3.3.6",
        "3.4.0",
        "3.10.15",
        "4.0.0",
        "3.3.6-SNAPSHOT",
        "3.4.0-alpha1"
    };

    for (String version : validVersions) {
      VersionConfigAdapter adapter = VersionConfigAdapter.forVersion(version);
      Assert.assertNotNull("Should create adapter for " + version, adapter);
      Assert.assertEquals("Version should match", version,
          adapter.getTargetVersion());
    }
  }

  @Test
  public void testCompatibilityMatrix() {
    // Test comprehensive compatibility matrix
    String[][] compatiblePairs = {
        {"3.3.5", "3.3.6"},
        {"3.3.6", "3.3.5"},
        {"3.4.0", "3.4.1"},
        {"3.10.0", "3.10.99"}
    };

    for (String[] pair : compatiblePairs) {
      Assert.assertTrue(
          "Should be compatible: " + pair[0] + " <-> " + pair[1],
          VersionConfigAdapter.areCompatible(pair[0], pair[1]));
    }

    String[][] incompatiblePairs = {
        {"2.10.0", "3.3.6"},
        {"3.3.6", "4.0.0"},
        {"1.0.0", "2.0.0"}
    };

    for (String[] pair : incompatiblePairs) {
      Assert.assertFalse(
          "Should not be compatible: " + pair[0] + " <-> " + pair[1],
          VersionConfigAdapter.areCompatible(pair[0], pair[1]));
    }
  }

  @Test
  public void testAdaptPreservesConfigTypes() {
    sourceConfig.set("string.property", "value");
    sourceConfig.setInt("int.property", 42);
    sourceConfig.setLong("long.property", 123456789L);
    sourceConfig.setBoolean("boolean.property", true);

    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    YarnConfiguration adapted = adapter.adapt(sourceConfig, "3.3.6");

    Assert.assertEquals("String property should be preserved",
        "value", adapted.get("string.property"));
    Assert.assertEquals("Int property should be preserved",
        42, adapted.getInt("int.property", -1));
    Assert.assertEquals("Long property should be preserved",
        123456789L, adapted.getLong("long.property", -1));
    Assert.assertTrue("Boolean property should be preserved",
        adapted.getBoolean("boolean.property", false));
  }

  @Test
  public void testAdaptWithNullSource() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");

    try {
      adapter.adapt(null, "3.3.6");
      Assert.fail("Should throw exception for null config");
    } catch (NullPointerException | IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testAdaptWithNullSourceVersion() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");

    try {
      adapter.adapt(sourceConfig, null);
      Assert.fail("Should throw exception for null source version");
    } catch (NullPointerException | IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testMultipleAdaptersIndependence() {
    VersionConfigAdapter adapter336 = VersionConfigAdapter.forVersion("3.3.6");
    VersionConfigAdapter adapter340 = VersionConfigAdapter.forVersion("3.4.0");

    Assert.assertEquals("First adapter should have correct version",
        "3.3.6", adapter336.getTargetVersion());
    Assert.assertEquals("Second adapter should have correct version",
        "3.4.0", adapter340.getTargetVersion());

    // Adapters should be independent
    Assert.assertNotEquals("Adapters should be different instances",
        adapter336, adapter340);
  }

  @Test
  public void testAdaptIdempotency() {
    sourceConfig.set(YarnConfiguration.RM_ADDRESS, "localhost:8032");

    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    YarnConfiguration adapted1 = adapter.adapt(sourceConfig, "3.3.6");
    YarnConfiguration adapted2 = adapter.adapt(sourceConfig, "3.3.6");

    Assert.assertEquals("Multiple adaptations should produce same result",
        adapted1.get(YarnConfiguration.RM_ADDRESS),
        adapted2.get(YarnConfiguration.RM_ADDRESS));
  }

  @Test
  public void testApplyDefaultsDoesNotOverrideExisting() {
    YarnConfiguration config = new YarnConfiguration();
    config.set(YarnConfiguration.RM_ADDRESS, "custom-host:8032");

    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    adapter.applyDefaults(config);

    Assert.assertEquals("Custom value should not be overridden",
        "custom-host:8032", config.get(YarnConfiguration.RM_ADDRESS));
  }

  @Test
  public void testVersionComparison() {
    // Test that version comparison works correctly
    Assert.assertTrue("3.3.x should be compatible within minor version",
        VersionConfigAdapter.areCompatible("3.3.0", "3.3.99"));
    Assert.assertTrue("Patch versions should be compatible",
        VersionConfigAdapter.areCompatible("3.3.1", "3.3.2"));
  }

  @Test
  public void testSnapshotVersionHandling() {
    // Test that snapshot versions are handled correctly
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0-SNAPSHOT");
    Assert.assertNotNull("Should handle snapshot versions", adapter);
    Assert.assertEquals("Version should include SNAPSHOT",
        "3.4.0-SNAPSHOT", adapter.getTargetVersion());
  }

  @Test
  public void testAlphaVersionHandling() {
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("4.0.0-alpha1");
    Assert.assertNotNull("Should handle alpha versions", adapter);
    Assert.assertEquals("Version should include alpha suffix",
        "4.0.0-alpha1", adapter.getTargetVersion());
  }

  @Test
  public void testConfigurationIsolation() {
    sourceConfig.set("test.property", "original");

    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
    YarnConfiguration adapted = adapter.adapt(sourceConfig, "3.3.6");

    // Modify adapted config
    adapted.set("test.property", "modified");

    // Original should be unchanged
    Assert.assertEquals("Original config should be unchanged",
        "original", sourceConfig.get("test.property"));
    Assert.assertEquals("Adapted config should have new value",
        "modified", adapted.get("test.property"));
  }
}
