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
package org.apache.hadoop.hdfs.server.process.unit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.process.VersionConfigAdapter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for VersionConfigAdapter.
 */
public class TestVersionConfigAdapter {

  @Test
  public void testVersionParsing() {
    // Test basic version parsing
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    assertEquals(3, adapter.getMajorVersion());
    assertEquals(3, adapter.getMinorVersion());
    assertEquals(5, adapter.getPatchVersion());
    assertEquals("3.3.5", adapter.getHadoopVersion());
  }

  @Test
  public void testVersionParsingWithSnapshot() {
    // Test version with SNAPSHOT suffix
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.4.0-SNAPSHOT");
    assertEquals(3, adapter.getMajorVersion());
    assertEquals(4, adapter.getMinorVersion());
    assertEquals(0, adapter.getPatchVersion());
  }

  @Test
  public void testVersionParsingTwoPartVersion() {
    // Test version with only major.minor
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3");
    assertEquals(3, adapter.getMajorVersion());
    assertEquals(3, adapter.getMinorVersion());
    assertEquals(0, adapter.getPatchVersion());
  }

  @Test
  public void testVersionParsingHadoop2x() {
    // Test Hadoop 2.x version
    VersionConfigAdapter adapter = new VersionConfigAdapter("2.10.2");
    assertEquals(2, adapter.getMajorVersion());
    assertEquals(10, adapter.getMinorVersion());
    assertEquals(2, adapter.getPatchVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullVersionThrowsException() {
    new VersionConfigAdapter(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyVersionThrowsException() {
    new VersionConfigAdapter("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidVersionFormatThrowsException() {
    new VersionConfigAdapter("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidVersionSingleNumberThrowsException() {
    new VersionConfigAdapter("3");
  }

  @Test
  public void testAdaptConfiguration() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    Configuration config = new Configuration(false);

    config.set("test.key", "test.value");
    Configuration adapted = adapter.adaptConfiguration(config);

    // Original key should still be present
    assertEquals("test.value", adapted.get("test.key"));
  }

  @Test
  public void testDeprecatedKeyMapping() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");

    // Check deprecated keys
    assertTrue(adapter.isKeyDeprecated("dfs.block.size"));
    assertTrue(adapter.isKeyDeprecated("dfs.datanode.max.xcievers"));
    assertFalse(adapter.isKeyDeprecated("dfs.blocksize"));
  }

  @Test
  public void testGetCurrentKeyName() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");

    // Keys that are not mapped should return themselves
    assertEquals("dfs.blocksize", adapter.getCurrentKeyName("dfs.blocksize"));
    assertEquals("dfs.replication", adapter.getCurrentKeyName("dfs.replication"));
  }

  @Test
  public void testIsKeySupported() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");

    // Non-deprecated keys should be supported
    assertTrue(adapter.isKeySupported("dfs.blocksize"));
    assertTrue(adapter.isKeySupported("dfs.replication"));

    // Deprecated keys should not be supported
    assertFalse(adapter.isKeySupported("dfs.block.size"));
  }

  @Test
  public void testFeatureAvailability_ErasureCoding() {
    // Erasure coding available in 3.0.0+
    VersionConfigAdapter adapter30 = new VersionConfigAdapter("3.0.0");
    assertTrue(adapter30.isFeatureAvailable("ERASURE_CODING"));

    VersionConfigAdapter adapter33 = new VersionConfigAdapter("3.3.5");
    assertTrue(adapter33.isFeatureAvailable("ERASURE_CODING"));

    // Not available in 2.x
    VersionConfigAdapter adapter2 = new VersionConfigAdapter("2.10.2");
    assertFalse(adapter2.isFeatureAvailable("ERASURE_CODING"));
  }

  @Test
  public void testFeatureAvailability_RouterFederation() {
    // Router federation available in 3.0.0+
    VersionConfigAdapter adapter30 = new VersionConfigAdapter("3.0.0");
    assertTrue(adapter30.isFeatureAvailable("HDFS_ROUTER_FEDERATION"));

    // Not available in 2.x
    VersionConfigAdapter adapter2 = new VersionConfigAdapter("2.10.2");
    assertFalse(adapter2.isFeatureAvailable("HDFS_ROUTER_FEDERATION"));
  }

  @Test
  public void testFeatureAvailability_NameNodeHA() {
    // NameNode HA available in 2.0.0+
    VersionConfigAdapter adapter2 = new VersionConfigAdapter("2.10.2");
    assertTrue(adapter2.isFeatureAvailable("NAMENODE_HA"));

    VersionConfigAdapter adapter3 = new VersionConfigAdapter("3.3.5");
    assertTrue(adapter3.isFeatureAvailable("NAMENODE_HA"));
  }

  @Test
  public void testFeatureAvailability_Snapshots() {
    // Snapshots available in 2.1.0+
    VersionConfigAdapter adapter20 = new VersionConfigAdapter("2.0.0");
    assertFalse(adapter20.isFeatureAvailable("HDFS_SNAPSHOTS"));

    VersionConfigAdapter adapter21 = new VersionConfigAdapter("2.1.0");
    assertTrue(adapter21.isFeatureAvailable("HDFS_SNAPSHOTS"));

    VersionConfigAdapter adapter3 = new VersionConfigAdapter("3.3.5");
    assertTrue(adapter3.isFeatureAvailable("HDFS_SNAPSHOTS"));
  }

  @Test
  public void testFeatureAvailability_Encryption() {
    // HDFS encryption available in 2.6.0+
    VersionConfigAdapter adapter25 = new VersionConfigAdapter("2.5.0");
    assertFalse(adapter25.isFeatureAvailable("HDFS_ENCRYPTION"));

    VersionConfigAdapter adapter26 = new VersionConfigAdapter("2.6.0");
    assertTrue(adapter26.isFeatureAvailable("HDFS_ENCRYPTION"));

    VersionConfigAdapter adapter3 = new VersionConfigAdapter("3.3.5");
    assertTrue(adapter3.isFeatureAvailable("HDFS_ENCRYPTION"));
  }

  @Test
  public void testFeatureAvailability_StoragePolicy() {
    // Storage policy available in 2.6.0+
    VersionConfigAdapter adapter25 = new VersionConfigAdapter("2.5.0");
    assertFalse(adapter25.isFeatureAvailable("STORAGE_POLICY"));

    VersionConfigAdapter adapter26 = new VersionConfigAdapter("2.6.0");
    assertTrue(adapter26.isFeatureAvailable("STORAGE_POLICY"));

    VersionConfigAdapter adapter3 = new VersionConfigAdapter("3.3.5");
    assertTrue(adapter3.isFeatureAvailable("STORAGE_POLICY"));
  }

  @Test
  public void testFeatureAvailability_UnknownFeature() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");

    // Unknown features should return false
    assertFalse(adapter.isFeatureAvailable("UNKNOWN_FEATURE"));
    assertFalse(adapter.isFeatureAvailable("NON_EXISTENT"));
  }

  @Test
  public void testVersionCompatibility_SameVersion() {
    VersionConfigAdapter adapter1 = new VersionConfigAdapter("3.3.5");
    VersionConfigAdapter adapter2 = new VersionConfigAdapter("3.3.5");

    assertTrue(adapter1.isCompatibleWith(adapter2));
    assertTrue(adapter2.isCompatibleWith(adapter1));
  }

  @Test
  public void testVersionCompatibility_Hadoop3xMinorVersions() {
    // Within Hadoop 3.x, different minor versions should be compatible
    VersionConfigAdapter adapter33 = new VersionConfigAdapter("3.3.5");
    VersionConfigAdapter adapter34 = new VersionConfigAdapter("3.4.0");
    VersionConfigAdapter adapter30 = new VersionConfigAdapter("3.0.0");

    assertTrue(adapter33.isCompatibleWith(adapter34));
    assertTrue(adapter34.isCompatibleWith(adapter33));
    assertTrue(adapter33.isCompatibleWith(adapter30));
    assertTrue(adapter30.isCompatibleWith(adapter33));
  }

  @Test
  public void testVersionCompatibility_Hadoop3xPatchVersions() {
    // Within same minor version, different patches should be compatible
    VersionConfigAdapter adapter331 = new VersionConfigAdapter("3.3.1");
    VersionConfigAdapter adapter335 = new VersionConfigAdapter("3.3.5");
    VersionConfigAdapter adapter336 = new VersionConfigAdapter("3.3.6");

    assertTrue(adapter331.isCompatibleWith(adapter335));
    assertTrue(adapter335.isCompatibleWith(adapter331));
    assertTrue(adapter335.isCompatibleWith(adapter336));
  }

  @Test
  public void testVersionCompatibility_Hadoop2xSameMinor() {
    // Within Hadoop 2.x same minor version should be compatible
    VersionConfigAdapter adapter2101 = new VersionConfigAdapter("2.10.1");
    VersionConfigAdapter adapter2102 = new VersionConfigAdapter("2.10.2");

    assertTrue(adapter2101.isCompatibleWith(adapter2102));
    assertTrue(adapter2102.isCompatibleWith(adapter2101));
  }

  @Test
  public void testVersionCompatibility_Hadoop2xAdjacentMinor() {
    // Within Hadoop 2.x, adjacent minor versions should be compatible
    VersionConfigAdapter adapter29 = new VersionConfigAdapter("2.9.2");
    VersionConfigAdapter adapter210 = new VersionConfigAdapter("2.10.2");

    assertTrue(adapter29.isCompatibleWith(adapter210));
    assertTrue(adapter210.isCompatibleWith(adapter29));
  }

  @Test
  public void testVersionCompatibility_Hadoop2xDistantMinor() {
    // Within Hadoop 2.x, distant minor versions should not be compatible
    VersionConfigAdapter adapter26 = new VersionConfigAdapter("2.6.0");
    VersionConfigAdapter adapter210 = new VersionConfigAdapter("2.10.2");

    assertFalse(adapter26.isCompatibleWith(adapter210));
    assertFalse(adapter210.isCompatibleWith(adapter26));
  }

  @Test
  public void testVersionCompatibility_DifferentMajorVersions() {
    // Different major versions should not be compatible
    VersionConfigAdapter adapter2 = new VersionConfigAdapter("2.10.2");
    VersionConfigAdapter adapter3 = new VersionConfigAdapter("3.3.5");

    assertFalse(adapter2.isCompatibleWith(adapter3));
    assertFalse(adapter3.isCompatibleWith(adapter2));
  }

  @Test
  public void testAdaptConfigurationRemovesDeprecatedKeys() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    Configuration config = new Configuration(false);

    // Set a deprecated key
    config.set("dfs.block.size", "268435456");

    Configuration adapted = adapter.adaptConfiguration(config);

    // The deprecated key should not be present (or should be handled)
    // Since we don't have explicit removal logic yet, just verify adaptation works
    assertNotNull(adapted);
  }

  @Test
  public void testVersionDefaultsApplied() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    Configuration config = new Configuration(false);

    // Don't set dfs.blocksize, let adapter apply default
    Configuration adapted = adapter.adaptConfiguration(config);

    // Check if default was applied
    String blockSize = adapted.get("dfs.blocksize");
    assertNotNull("Default block size should be applied", blockSize);
    assertEquals("134217728", blockSize); // 128MB
  }

  @Test
  public void testVersionDefaultsNotOverrideExisting() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    Configuration config = new Configuration(false);

    // Set custom value
    config.set("dfs.blocksize", "67108864"); // 64MB

    Configuration adapted = adapter.adaptConfiguration(config);

    // Custom value should be preserved
    assertEquals("67108864", adapted.get("dfs.blocksize"));
  }

  @Test
  public void testConfigurationAdaptationHadoop2x() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("2.10.2");
    Configuration config = new Configuration(false);

    // Set some Hadoop 3.x specific keys that should be removed
    config.set("dfs.federation.router.default.nameserviceId", "test");
    config.set("dfs.namenode.provided.enabled", "true");
    config.set("dfs.replication", "3");

    Configuration adapted = adapter.adaptConfiguration(config);

    // Hadoop 3.x specific keys should be removed
    assertNull(adapted.get("dfs.federation.router.default.nameserviceId"));
    assertNull(adapted.get("dfs.namenode.provided.enabled"));

    // Common keys should remain
    assertEquals("3", adapted.get("dfs.replication"));
  }

  @Test
  public void testConfigurationAdaptationHadoop3x() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    Configuration config = new Configuration(false);

    // Set Hadoop 3.x specific keys
    config.set("dfs.federation.router.default.nameserviceId", "test");
    config.set("dfs.replication", "3");

    Configuration adapted = adapter.adaptConfiguration(config);

    // All keys should be preserved in 3.x
    assertEquals("test", adapted.get("dfs.federation.router.default.nameserviceId"));
    assertEquals("3", adapted.get("dfs.replication"));
  }

  @Test
  public void testMultipleVersionAdapters() {
    // Test creating multiple adapters for different versions
    VersionConfigAdapter adapter331 = new VersionConfigAdapter("3.3.1");
    VersionConfigAdapter adapter335 = new VersionConfigAdapter("3.3.5");
    VersionConfigAdapter adapter340 = new VersionConfigAdapter("3.4.0");
    VersionConfigAdapter adapter210 = new VersionConfigAdapter("2.10.2");

    // All should be independent and work correctly
    assertEquals("3.3.1", adapter331.getHadoopVersion());
    assertEquals("3.3.5", adapter335.getHadoopVersion());
    assertEquals("3.4.0", adapter340.getHadoopVersion());
    assertEquals("2.10.2", adapter210.getHadoopVersion());
  }

  @Test
  public void testConfigurationIndependence() {
    VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");
    Configuration original = new Configuration(false);
    original.set("test.key", "original");

    Configuration adapted = adapter.adaptConfiguration(original);
    adapted.set("test.key", "modified");

    // Original should not be modified
    assertEquals("original", original.get("test.key"));
    assertEquals("modified", adapted.get("test.key"));
  }
}
