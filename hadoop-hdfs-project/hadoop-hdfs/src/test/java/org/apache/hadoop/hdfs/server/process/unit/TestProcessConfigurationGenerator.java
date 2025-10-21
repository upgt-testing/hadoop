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
package org.apache.hadoop.hdfs.server.process.unit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.server.process.DirectoryManager;
import org.apache.hadoop.hdfs.server.process.PortAllocator;
import org.apache.hadoop.hdfs.server.process.ProcessConfigurationGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for ProcessConfigurationGenerator.
 */
public class TestProcessConfigurationGenerator {

  private ProcessConfigurationGenerator configGen;
  private PortAllocator portAllocator;
  private DirectoryManager dirManager;

  @Before
  public void setUp() throws IOException {
    Configuration baseConfig = new Configuration(false);
    portAllocator = new PortAllocator();
    dirManager = new DirectoryManager(false);
    configGen = new ProcessConfigurationGenerator(baseConfig, portAllocator);
  }

  @After
  public void tearDown() throws IOException {
    if (portAllocator != null) {
      portAllocator.releaseAll();
    }
    if (dirManager != null) {
      try {
        dirManager.cleanup();
      } catch (Exception e) {
        // Ignore cleanup errors in tests
      }
    }
  }

  @Test
  public void testGenerateNameNodeConfig() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    assertNotNull("Node configuration should not be null", nodeConfig);
    assertNotNull("Configuration should not be null", nodeConfig.getConfig());
    assertNotNull("Allocated ports should not be null", nodeConfig.getAllocatedPorts());
  }

  @Test
  public void testNameNodeConfigHasRequiredPorts() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    Map<String, Integer> ports = nodeConfig.getAllocatedPorts();
    assertTrue("Should have RPC port", ports.containsKey("rpc"));
    assertTrue("Should have HTTP port", ports.containsKey("http"));
    assertTrue("Should have service RPC port", ports.containsKey("serviceRpc"));

    // All ports should be in valid range
    for (Integer port : ports.values()) {
      assertTrue("Port should be in valid range", port >= 50000 && port <= 59999);
    }
  }

  @Test
  public void testNameNodeConfigHasRequiredProperties() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    Configuration config = nodeConfig.getConfig();

    // Check RPC address
    String rpcAddress = config.get(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY);
    assertNotNull("RPC address should be set", rpcAddress);
    assertTrue("RPC address should contain localhost", rpcAddress.contains("localhost"));

    // Check HTTP address
    String httpAddress = config.get(DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY);
    assertNotNull("HTTP address should be set", httpAddress);
    assertTrue("HTTP address should contain localhost", httpAddress.contains("localhost"));

    // Check service RPC address
    String serviceRpcAddress = config.get(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY);
    assertNotNull("Service RPC address should be set", serviceRpcAddress);

    // Check data directory
    String nameDir = config.get(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY);
    assertNotNull("Name directory should be set", nameDir);
    assertTrue("Name directory should contain node data dir path",
        nameDir.contains(nodeDir.getDataDir().getName()));
  }

  @Test
  public void testNameNodeConfigWritesFiles() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    configGen.generateNameNodeConfig(0, nodeDir);

    // Check that configuration files were created
    File coreSite = new File(nodeDir.getConfDir(), "core-site.xml");
    File hdfsSite = new File(nodeDir.getConfDir(), "hdfs-site.xml");

    assertTrue("core-site.xml should exist", coreSite.exists());
    assertTrue("hdfs-site.xml should exist", hdfsSite.exists());
    assertTrue("core-site.xml should be a file", coreSite.isFile());
    assertTrue("hdfs-site.xml should be a file", hdfsSite.isFile());
  }

  @Test
  public void testGenerateDataNodeConfig() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    List<InetSocketAddress> nnAddresses = new ArrayList<>();
    nnAddresses.add(new InetSocketAddress("localhost", 9000));

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateDataNodeConfig(0, nodeDir, nnAddresses);

    assertNotNull("Node configuration should not be null", nodeConfig);
    assertNotNull("Configuration should not be null", nodeConfig.getConfig());
    assertNotNull("Allocated ports should not be null", nodeConfig.getAllocatedPorts());
  }

  @Test
  public void testDataNodeConfigHasRequiredPorts() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    List<InetSocketAddress> nnAddresses = new ArrayList<>();
    nnAddresses.add(new InetSocketAddress("localhost", 9000));

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateDataNodeConfig(0, nodeDir, nnAddresses);

    Map<String, Integer> ports = nodeConfig.getAllocatedPorts();
    assertTrue("Should have data port", ports.containsKey("data"));
    assertTrue("Should have IPC port", ports.containsKey("ipc"));
    assertTrue("Should have HTTP port", ports.containsKey("http"));

    // All ports should be in valid range
    for (Integer port : ports.values()) {
      assertTrue("Port should be in valid range", port >= 50000 && port <= 59999);
    }
  }

  @Test
  public void testDataNodeConfigHasRequiredProperties() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    List<InetSocketAddress> nnAddresses = new ArrayList<>();
    nnAddresses.add(new InetSocketAddress("localhost", 9000));

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateDataNodeConfig(0, nodeDir, nnAddresses);

    Configuration config = nodeConfig.getConfig();

    // Check data address
    String dataAddress = config.get(DFSConfigKeys.DFS_DATANODE_ADDRESS_KEY);
    assertNotNull("Data address should be set", dataAddress);
    assertTrue("Data address should contain localhost", dataAddress.contains("localhost"));

    // Check IPC address
    String ipcAddress = config.get(DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_KEY);
    assertNotNull("IPC address should be set", ipcAddress);

    // Check HTTP address
    String httpAddress = config.get(DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_KEY);
    assertNotNull("HTTP address should be set", httpAddress);

    // Check data directory
    String dataDir = config.get(DFSConfigKeys.DFS_DATANODE_DATA_DIR_KEY);
    assertNotNull("Data directory should be set", dataDir);

    // Check default filesystem
    String defaultFs = config.get("fs.defaultFS");
    assertNotNull("Default filesystem should be set", defaultFs);
    assertTrue("Default filesystem should be hdfs", defaultFs.startsWith("hdfs://"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDataNodeConfigWithNullNameNodeAddress() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);
    configGen.generateDataNodeConfig(0, nodeDir, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDataNodeConfigWithEmptyNameNodeAddress() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);
    List<InetSocketAddress> emptyList = new ArrayList<>();
    configGen.generateDataNodeConfig(0, nodeDir, emptyList);
  }

  @Test
  public void testMultipleNameNodeConfigs() throws IOException {
    DirectoryManager.NodeDirectory nn0 = dirManager.createNameNodeDirectory(0);
    DirectoryManager.NodeDirectory nn1 = dirManager.createNameNodeDirectory(1);

    ProcessConfigurationGenerator.NodeConfiguration config0 =
        configGen.generateNameNodeConfig(0, nn0);
    ProcessConfigurationGenerator.NodeConfiguration config1 =
        configGen.generateNameNodeConfig(1, nn1);

    // Each should have different ports
    int rpc0 = config0.getPort("rpc");
    int rpc1 = config1.getPort("rpc");
    assertNotEquals("RPC ports should be different", rpc0, rpc1);

    int http0 = config0.getPort("http");
    int http1 = config1.getPort("http");
    assertNotEquals("HTTP ports should be different", http0, http1);
  }

  @Test
  public void testMultipleDataNodeConfigs() throws IOException {
    List<InetSocketAddress> nnAddresses = new ArrayList<>();
    nnAddresses.add(new InetSocketAddress("localhost", 9000));

    DirectoryManager.NodeDirectory dn0 = dirManager.createDataNodeDirectory(0);
    DirectoryManager.NodeDirectory dn1 = dirManager.createDataNodeDirectory(1);

    ProcessConfigurationGenerator.NodeConfiguration config0 =
        configGen.generateDataNodeConfig(0, dn0, nnAddresses);
    ProcessConfigurationGenerator.NodeConfiguration config1 =
        configGen.generateDataNodeConfig(1, dn1, nnAddresses);

    // Each should have different ports
    int data0 = config0.getPort("data");
    int data1 = config1.getPort("data");
    assertNotEquals("Data ports should be different", data0, data1);

    int ipc0 = config0.getPort("ipc");
    int ipc1 = config1.getPort("ipc");
    assertNotEquals("IPC ports should be different", ipc0, ipc1);
  }

  @Test
  public void testCommonHdfsPropertiesSet() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    Configuration config = nodeConfig.getConfig();

    // Check replication factor
    int replication = config.getInt(DFSConfigKeys.DFS_REPLICATION_KEY, -1);
    assertTrue("Replication should be set", replication > 0);

    // Check block size
    long blockSize = config.getLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, -1);
    assertTrue("Block size should be set", blockSize > 0);

    // Check permissions disabled
    boolean permissions = config.getBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, true);
    assertFalse("Permissions should be disabled for testing", permissions);

    // Check heartbeat interval
    long heartbeat = config.getLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, -1);
    assertTrue("Heartbeat interval should be set", heartbeat > 0);
  }

  @Test
  public void testNodeConfigurationToString() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    String str = nodeConfig.toString();
    assertNotNull("toString should not be null", str);
    assertTrue("toString should contain class name",
        str.contains("NodeConfiguration"));
    assertTrue("toString should contain ports",
        str.contains("allocatedPorts"));
  }

  @Test
  public void testGetRpcPortFromNameNode() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    int rpcPort = nodeConfig.getRpcPort();
    assertTrue("RPC port should be valid", rpcPort > 0);
    assertEquals("RPC port should match allocated port",
        nodeConfig.getPort("rpc").intValue(), rpcPort);
  }

  @Test
  public void testGetRpcPortFromDataNode() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    List<InetSocketAddress> nnAddresses = new ArrayList<>();
    nnAddresses.add(new InetSocketAddress("localhost", 9000));

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateDataNodeConfig(0, nodeDir, nnAddresses);

    int rpcPort = nodeConfig.getRpcPort();
    assertTrue("RPC port should be valid", rpcPort > 0);
    assertEquals("RPC port should match IPC port for DataNode",
        nodeConfig.getPort("ipc").intValue(), rpcPort);
  }

  @Test
  public void testGetHttpPort() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    int httpPort = nodeConfig.getHttpPort();
    assertTrue("HTTP port should be valid", httpPort > 0);
    assertEquals("HTTP port should match allocated port",
        nodeConfig.getPort("http").intValue(), httpPort);
  }

  @Test
  public void testConfigInheritsFromBase() throws IOException {
    Configuration baseConfig = new Configuration(false);
    baseConfig.set("test.property", "test-value");

    ProcessConfigurationGenerator customGen =
        new ProcessConfigurationGenerator(baseConfig, portAllocator);

    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        customGen.generateNameNodeConfig(0, nodeDir);

    // Custom property should be inherited
    String value = nodeConfig.getConfig().get("test.property");
    assertEquals("Should inherit base config property", "test-value", value);
  }

  @Test
  public void testWithNullBaseConfig() throws IOException {
    ProcessConfigurationGenerator nullBaseGen =
        new ProcessConfigurationGenerator(null, portAllocator);

    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    // Should not throw exception
    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        nullBaseGen.generateNameNodeConfig(0, nodeDir);

    assertNotNull("Should generate config with null base", nodeConfig);
  }

  @Test
  public void testDataNodeWithMultipleNameNodes() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    List<InetSocketAddress> nnAddresses = new ArrayList<>();
    nnAddresses.add(new InetSocketAddress("localhost", 9000));
    nnAddresses.add(new InetSocketAddress("localhost", 9001));

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateDataNodeConfig(0, nodeDir, nnAddresses);

    // Should use the first NameNode for now (full HA support comes later)
    Configuration config = nodeConfig.getConfig();
    String defaultFs = config.get("fs.defaultFS");
    assertTrue("Should use first NameNode", defaultFs.contains("9000"));
  }

  @Test
  public void testPortsAreTrackedByAllocator() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    int beforeCount = portAllocator.getAllocatedPortCount();

    configGen.generateNameNodeConfig(0, nodeDir);

    int afterCount = portAllocator.getAllocatedPortCount();

    assertEquals("Should allocate 3 ports for NameNode", 3, afterCount - beforeCount);
  }

  @Test
  public void testGetPortReturnsNullForUnknownPort() throws IOException {
    DirectoryManager.NodeDirectory nodeDir = dirManager.createNameNodeDirectory(0);

    ProcessConfigurationGenerator.NodeConfiguration nodeConfig =
        configGen.generateNameNodeConfig(0, nodeDir);

    Integer unknownPort = nodeConfig.getPort("unknown-port-name");
    assertNull("Should return null for unknown port", unknownPort);
  }
}
