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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for HDFS rolling upgrade operations.
 *
 * This class defines which nodes to upgrade and in what order during a rolling upgrade.
 * Configuration can be loaded from:
 * 1. System property preset: -Dhadoop.upgrade.plan=all-nodes
 * 2. Configuration file: -Dhadoop.upgrade.plan.file=/path/to/plan.json
 * 3. Default: upgrade all nodes (NameNodes first, then DataNodes)
 *
 * <p>Supported presets:</p>
 * <ul>
 *   <li><b>all-nodes</b>: Upgrade all NameNodes then all DataNodes (default, recommended)</li>
 *   <li><b>datanodes-only</b>: Upgrade only DataNodes</li>
 *   <li><b>namenodes-only</b>: Upgrade only NameNodes</li>
 * </ul>
 */
public class UpgradeConfig {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeConfig.class);

  private List<UpgradeStep> steps;

  /**
   * Constructor.
   */
  public UpgradeConfig() {
    this.steps = new ArrayList<>();
  }

  /**
   * Get the list of upgrade steps.
   */
  public List<UpgradeStep> getSteps() {
    return steps;
  }

  /**
   * Add an upgrade step.
   */
  public void addStep(UpgradeStep step) {
    this.steps.add(step);
  }

  /**
   * Add an upgrade step with convenience parameters.
   */
  public void addStep(NodeType nodeType, int[] indices, String description) {
    this.steps.add(new UpgradeStep(nodeType, indices, description));
  }

  /**
   * Create upgrade configuration from a preset name.
   *
   * @param preset the preset name (all-nodes, datanodes-only, namenodes-only)
   * @param cluster the cluster to upgrade
   * @return upgrade configuration
   */
  public static UpgradeConfig fromPreset(String preset, ProcessBasedMiniDFSCluster cluster) {
    LOG.info("Creating upgrade configuration from preset: {}", preset);

    UpgradeConfig config = new UpgradeConfig();

    switch (preset) {
      case "all-nodes":
        // Follow official procedure: NameNodes first, then DataNodes
        config.addStep(NodeType.NAMENODE, null, "Upgrade all NameNodes");
        config.addStep(NodeType.DATANODE, null, "Upgrade all DataNodes");
        break;

      case "datanodes-only":
        config.addStep(NodeType.DATANODE, null, "Upgrade all DataNodes");
        break;

      case "namenodes-only":
        config.addStep(NodeType.NAMENODE, null, "Upgrade all NameNodes");
        break;

      case "none":
        // Empty plan - no upgrade
        LOG.info("Upgrade preset is 'none', no nodes will be upgraded");
        break;

      default:
        throw new IllegalArgumentException(
            "Unknown upgrade preset: " + preset +
            ". Supported presets: all-nodes, datanodes-only, namenodes-only, none");
    }

    LOG.info("Created upgrade configuration with {} steps", config.getSteps().size());
    return config;
  }

  /**
   * Create upgrade configuration from a file.
   *
   * <p>File format (simple properties):</p>
   * <pre>
   * # Upgrade plan
   * namenodes=0
   * datanodes=0,1,2
   * </pre>
   *
   * @param path path to configuration file
   * @return upgrade configuration
   * @throws IOException if file cannot be read
   */
  public static UpgradeConfig fromFile(String path) throws IOException {
    LOG.info("Loading upgrade configuration from file: {}", path);

    File file = new File(path);
    if (!file.exists()) {
      throw new IOException("Upgrade plan file not found: " + path);
    }

    UpgradeConfig config = new UpgradeConfig();

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        // Skip comments and empty lines
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        // Parse key=value
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
          LOG.warn("Skipping invalid line in upgrade plan: {}", line);
          continue;
        }

        String key = parts[0].trim();
        String value = parts[1].trim();

        if (key.equals("namenodes")) {
          int[] indices = parseIndices(value);
          config.addStep(NodeType.NAMENODE, indices,
              "Upgrade NameNodes: " + value);
        } else if (key.equals("datanodes")) {
          int[] indices = parseIndices(value);
          config.addStep(NodeType.DATANODE, indices,
              "Upgrade DataNodes: " + value);
        } else {
          LOG.warn("Unknown configuration key: {}", key);
        }
      }
    }

    LOG.info("Loaded upgrade configuration with {} steps from file", config.getSteps().size());
    return config;
  }

  /**
   * Parse comma-separated indices, or "all" for all nodes.
   *
   * @param value string like "0,1,2" or "all"
   * @return array of indices, or null for "all"
   */
  private static int[] parseIndices(String value) {
    if (value.equalsIgnoreCase("all")) {
      return null; // null means all nodes
    }

    String[] parts = value.split(",");
    int[] indices = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      indices[i] = Integer.parseInt(parts[i].trim());
    }
    return indices;
  }

  /**
   * Represents a single step in the upgrade plan.
   */
  public static class UpgradeStep {
    private final NodeType nodeType;
    private final int[] indices;
    private final String description;

    /**
     * Constructor.
     *
     * @param nodeType type of nodes to upgrade (NAMENODE or DATANODE)
     * @param indices specific node indices to upgrade, or null for all nodes
     * @param description human-readable description of this step
     */
    public UpgradeStep(NodeType nodeType, int[] indices, String description) {
      this.nodeType = nodeType;
      this.indices = indices;
      this.description = description;
    }

    public NodeType getNodeType() {
      return nodeType;
    }

    public int[] getIndices() {
      return indices;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Type of node in the cluster.
   */
  public enum NodeType {
    NAMENODE,
    DATANODE
  }
}
