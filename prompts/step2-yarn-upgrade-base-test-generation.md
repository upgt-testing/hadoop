# Step 2: Generate YARN Upgrade Base Test Class

## Purpose

This document provides a complete, YARN-specific prompt for generating `YarnUpgradeTestBase`, a JUnit base class for parameterized upgrade testing of ProcessBasedMiniYARNCluster. The generated class provides automatic lifecycle management, checkpoint-based upgrade testing, and complete test isolation.

## How to Use This Document

1. Copy the **AI Agent Prompt** section below
2. Provide it to an AI programming agent (ChatGPT, Claude, Copilot, etc.) or use as your implementation guide
3. Review and adjust the generated code as needed
4. Place the generated class in your test source directory

---

## AI Agent Prompt

**Copy the section below and provide to an AI agent:**

```
Generate a JUnit base test class for parameterized upgrade testing with the following requirements:

### SYSTEM INFORMATION

**Cluster Type**: Apache Hadoop YARN - Resource management and job scheduling framework
Description: Distributed cluster for managing compute resources and scheduling applications (MapReduce, Spark, etc.)

**Cluster Class**: org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster
Full package name: org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster

**Client/Connection Class**: org.apache.hadoop.yarn.client.api.YarnClient
Full package name: org.apache.hadoop.yarn.client.api.YarnClient

**Configuration Class**: org.apache.hadoop.conf.Configuration
Full package name: org.apache.hadoop.conf.Configuration
(Note: YarnConfiguration extends Configuration and adds YARN-specific properties)

**Package Name**: org.apache.hadoop.yarn.server.process.upgrade
Package for upgrade tests

**File Location**: hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/src/test/java/org/apache/hadoop/yarn/server/process/upgrade/
Relative path from project root

### CLUSTER LIFECYCLE

**Cluster Initialization Pattern**:
```java
Configuration conf = new YarnConfiguration();
// System properties are read automatically - no manual setup needed!
cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
    .numNodeManagers(3)
    .format(true)
    .build();  // Automatically reads hadoop.start.home and hadoop.upgrade.home
cluster.waitForNodeManagersToConnect(5000);
```

**Client Initialization Pattern**:
```java
yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();
```

**Cluster Shutdown Pattern**:
```java
cluster.shutdown();  // Shuts down all ResourceManager and NodeManager processes
```

**Client Shutdown Pattern**:
```java
if (yarnClient != null) {
    yarnClient.stop();
}
```

### UPGRADE MECHANISM

**Upgrade Method**: Rolling upgrade
Description: NodeManagers are upgraded one at a time while keeping the cluster operational. For HA clusters, standby ResourceManager is upgraded first, followed by failover, then the old active is upgraded.

**Upgrade Invocation**:
```java
// Rolling upgrade of all NodeManagers
cluster.rollingUpgradeNodeManagers();

// Or manual control for custom upgrade patterns:
for (int i = 0; i < cluster.getNumNodeManagers(); i++) {
    cluster.shutdownNodeManager(i);
    cluster.changeNodeManagerVersion(i, cluster.getUpgradeDistributionPath());
    cluster.startNodeManager(i);
    cluster.waitForNodeManagersToConnect(5000);
}

// For RM HA upgrade (if applicable):
int standbyIndex = (cluster.getActiveRMIndex() + 1) % 2;
cluster.shutdownResourceManager(standbyIndex);
cluster.changeResourceManagerVersion(standbyIndex, cluster.getUpgradeDistributionPath());
cluster.startResourceManager(standbyIndex);
// Perform failover...
```

**Pre-upgrade Health Check**:
```java
if (!cluster.isClusterUp()) {
    throw new IllegalStateException("Cluster is not healthy before upgrade");
}

// Verify all NMs are connected
if (cluster.getNumNodeManagers() > 0) {
    boolean allConnected = cluster.waitForNodeManagersToConnect(10000);
    if (!allConnected) {
        throw new IllegalStateException("Not all NodeManagers connected before upgrade");
    }
}
```

**Post-upgrade Health Check**:
```java
cluster.waitForNodeManagersToConnect(10000);  // Wait for NMs to reconnect after restart

// Verify cluster metrics
YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
if (metrics.getNumNodeManagers() < expectedNumNMs) {
    throw new IllegalStateException("Not all NodeManagers reconnected after upgrade");
}
```

### PROCESS/RESOURCE CLEANUP

**Process Pattern to Kill**: ResourceManager|NodeManager
Regular expression to match YARN process names

**Process Cleanup Command**:
```bash
jps | grep -E 'ResourceManager|NodeManager' | awk '{print $1}' | xargs -r kill -9
```
Shell command to kill orphaned YARN processes

**Temporary Directory Pattern**: process-miniyarn-*
Pattern to match cluster temporary directories

**Directory Cleanup Logic**:
```java
File tmpDir = new File(System.getProperty("java.io.tmpdir"));
File[] oldDirs = tmpDir.listFiles((dir, name) ->
    name.startsWith("process-miniyarn-") &&
    name.matches(".*\\d{13}$"));  // Match timestamp suffix
if (oldDirs != null) {
    long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
    for (File dir : oldDirs) {
        // Only delete directories older than 1 hour (avoid deleting active tests)
        if (dir.lastModified() < oneHourAgo) {
            deleteDirectory(dir);
        }
    }
}
```
Java code to find and delete old cluster directories

### CHECKPOINT CONFIGURATION

**Checkpoint Constants Class**: YarnUpgradeCheckpoints
Class name for checkpoint constants

**Common Checkpoint Names**: NO_UPGRADE, AFTER_CLUSTER_START, AFTER_APP_SUBMIT, AFTER_APP_RUNNING, AFTER_APP_FINISHED, AFTER_QUEUE_REFRESH, AFTER_NODE_REPORT, AFTER_CONTAINER_ALLOCATION
List of common checkpoint names (comma-separated)

**Checkpoint No-Upgrade Constant**: NO_UPGRADE
Constant name for baseline test (no upgrade) - this should always be included in test parameters to verify tests pass without upgrades

### ADDITIONAL REQUIREMENTS

**Additional Managed Resources**:
- None required beyond YarnClient and cluster
- Tests may create ApplicationId, ApplicationAttemptId references, but these don't need explicit cleanup
- Container handles are managed by YARN

**Additional Cleanup Steps**:
1. Stop YarnClient (if started)
2. Shutdown cluster (stops all RM and NM processes)
3. Verify no orphaned ResourceManager or NodeManager processes
4. Clean up old cluster directories (optional, only if older than 1 hour)

**Special Considerations**:
- YARN applications may still be running during upgrade - this is intentional for upgrade testing
- Close any InputStreams or OutputStreams from application logs before calling checkpoint() to avoid broken pipes
- ApplicationId and ApplicationAttemptId are just identifiers, not resources that need cleanup
- NMs will be restarted during rolling upgrades - expect temporary disconnections
- For RM HA clusters, failover may be required between standby and active RM upgrades

### PLATFORM COMPATIBILITY

**Operating Systems**: Linux, macOS
Target operating systems - Windows support is lower priority

**Process Management Approach**:
Use jps (Java process status tool) and kill -9 on Unix systems.
For cross-platform compatibility, also support process.destroy() and process.destroyForcibly().

### GENERATED CLASS STRUCTURE

Please generate a base test class with the following structure:

1. **Class Header**:
   - Apache License 2.0 header
   - Package declaration: org.apache.hadoop.yarn.server.process.upgrade
   - Comprehensive JavaDoc explaining:
     - Purpose of the base class
     - Usage pattern with @RunWith(Parameterized.class)
     - Example test implementation
     - Test isolation guarantees
     - Cleanup guarantees

2. **Protected Fields**:
   - upgradeCheckpoint (String) - Parameter from subclass
   - cluster (ProcessBasedMiniYARNCluster) - Cluster instance
   - yarnClient (YarnClient) - Client instance
   - conf (Configuration) - Configuration instance

3. **@Before setupTest() Method**:
   - Sync @Parameter field from subclass to base class (use reflection)
   - Log setup start with checkpoint name
   - Clean up orphaned YARN processes from previous failed runs
   - Clean up old cluster directories (older than 1 hour)
   - Initialize fresh YarnConfiguration
   - Set cluster = null, yarnClient = null (defensive)
   - Log setup completion

4. **@After tearDownTest() Method**:
   - Log teardown start with checkpoint name
   - Stop yarnClient (with try-catch, null check, finally block)
   - Shutdown cluster with cleanup (with try-catch, null check, finally block)
   - Wait for processes to terminate (Thread.sleep(2000) to allow graceful shutdown)
   - Verify cleanup success (no orphaned ResourceManager or NodeManager processes)
   - Force cleanup if verification fails
   - Log teardown completion
   - **CRITICAL**: Use independent try-catch blocks for each cleanup step to ensure all cleanup runs even if one step fails

5. **checkpoint(String name) Method**:
   - Check if upgrade should happen at this checkpoint (call shouldUpgrade(name))
   - If no upgrade needed, return immediately
   - Log checkpoint name
   - Verify cluster health before upgrade (pre-upgrade health check)
   - Perform rolling upgrade using cluster.rollingUpgradeNodeManagers()
   - Verify cluster health after upgrade (post-upgrade health check)
   - Log upgrade completion
   - **JavaDoc should warn**: "IMPORTANT: Close all streams, application logs, and resources before calling checkpoint() to avoid broken pipelines during NodeManager restarts"

6. **shouldUpgrade(String name) Method**:
   - Return false if upgradeCheckpoint is null
   - Return false if upgradeCheckpoint equals "NO_UPGRADE"
   - Return true if upgradeCheckpoint.equals(name)
   - Return false otherwise

7. **Private Helper Methods**:
   - syncUpgradeCheckpointFromSubclass(): Use Java reflection to find @Parameter field named "upgradeCheckpoint" in subclass, copy value to base class field
   - cleanupOrphanedProcesses(): Execute "jps | grep -E 'ResourceManager|NodeManager' | awk '{print $1}' | xargs -r kill -9"
   - cleanupOldClusterDirectories(): Find directories matching "process-miniyarn-*" pattern, delete only if older than 1 hour
   - deleteDirectory(File): Recursive directory deletion utility
   - verifyCleanup(): Execute jps and verify no processes match "ResourceManager|NodeManager"

8. **Best Practices**:
   - Use SLF4J Logger for all logging
   - Each cleanup step in @After must be in independent try-catch block
   - Set fields to null in finally blocks after cleanup
   - Log all major steps (setup, cleanup, upgrade, verification)
   - Defensive cleanup in @Before (kill orphaned processes from failed previous runs)
   - Comprehensive JavaDoc with usage examples
   - Platform-aware process cleanup (handle Linux/macOS)

### OUTPUT FORMAT

Generate:
1. Complete Java source file with Apache License 2.0 header
2. Necessary import statements (minimize unused imports)
3. All methods with comprehensive JavaDoc comments
4. Inline comments for complex logic
5. Proper exception handling and logging
6. Example usage in class-level JavaDoc showing:
   - How to extend this base class
   - How to define @Parameters method
   - How to use checkpoint() in tests
   - Sample test method structure

The generated class should be production-ready and follow Java best practices for Hadoop projects.

### EXAMPLE USAGE (to include in class-level JavaDoc)

```java
/**
 * Base test class for parameterized upgrade testing of ProcessBasedMiniYARNCluster.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @RunWith(Parameterized.class)
 * public class TestApplicationUpgrade extends YarnUpgradeTestBase {
 *
 *   @Parameter
 *   public String upgradeCheckpoint;
 *
 *   @Parameters(name = "upgrade-at={0}")
 *   public static Collection<String> checkpoints() {
 *     return Arrays.asList(
 *       YarnUpgradeCheckpoints.NO_UPGRADE,           // Baseline
 *       YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
 *       "AFTER_APP_SUBMIT",
 *       "AFTER_APP_RUNNING",
 *       "AFTER_CONTAINER_ALLOCATED",
 *       "AFTER_APP_FINISHED"
 *     );
 *   }
 *
 *   @Test
 *   public void testDistributedShellUpgrade() throws Exception {
 *     // Use cluster and yarnClient from base class
 *     cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
 *         .numNodeManagers(3)
 *         .build();
 *     yarnClient = YarnClient.createYarnClient();
 *     yarnClient.init(cluster.getConfiguration());
 *     yarnClient.start();
 *
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);
 *
 *     // Submit application
 *     ApplicationId appId = submitDistributedShellApp(yarnClient);
 *     checkpoint("AFTER_APP_SUBMIT");
 *
 *     // Wait for running
 *     waitForAppState(yarnClient, appId, YarnApplicationState.RUNNING);
 *     checkpoint("AFTER_APP_RUNNING");
 *
 *     // Wait for completion
 *     waitForAppState(yarnClient, appId, YarnApplicationState.FINISHED);
 *     checkpoint("AFTER_APP_FINISHED");
 *
 *     // Verify success
 *     ApplicationReport report = yarnClient.getApplicationReport(appId);
 *     assertEquals(FinalApplicationStatus.SUCCEEDED,
 *         report.getFinalApplicationStatus());
 *
 *     // No try-finally needed - @After handles cleanup automatically!
 *   }
 * }
 * }</pre>
 */
```

### ADDITIONAL NOTES

1. **System Properties Handling**: The cluster automatically reads system properties for Hadoop distributions:
   - hadoop.start.home - Starting Hadoop distribution
   - hadoop.upgrade.home - Upgrade target Hadoop distribution
   No manual environment variable checks are needed in the base class.

2. **Cluster Ready Checks**: Use cluster.waitForNodeManagersToConnect(timeoutMs) to wait for cluster readiness.

3. **Upgrade Flexibility**: The checkpoint() method should support both:
   - Automatic rolling upgrade via cluster.rollingUpgradeNodeManagers()
   - Manual upgrade control for advanced scenarios

4. **Logging**: Use LOG.info() for major steps, LOG.debug() for detailed progress, LOG.error() for failures.

5. **Thread Safety**: Assume tests run sequentially (not in parallel), so no thread safety needed for fields.

6. **Resource Ordering**: Always stop yarnClient before shutting down cluster.

7. **Timeouts**: Use reasonable timeouts:
   - waitForNodeManagersToConnect: 10000ms
   - Process termination wait: 2000ms
   - Upgrade completion wait: 30000ms

8. **Error Messages**: Provide clear, actionable error messages when cleanup fails or upgrade verification fails.
```

---

## Expected Output Structure

The AI agent will generate a class with this structure:

```java
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

package org.apache.hadoop.yarn.server.process.upgrade;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Base test class for parameterized upgrade testing of ProcessBasedMiniYARNCluster.
 *
 * <p>This class provides automatic lifecycle management, checkpoint-based upgrade testing,
 * and complete test isolation. Each test execution is fully isolated with guaranteed
 * cleanup between runs.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @RunWith(Parameterized.class)
 * public class TestMyFeature extends YarnUpgradeTestBase {
 *
 *   @Parameter
 *   public String upgradeCheckpoint;
 *
 *   @Parameters(name = "upgrade-at={0}")
 *   public static Collection<String> checkpoints() {
 *     return Arrays.asList(
 *       YarnUpgradeCheckpoints.NO_UPGRADE,
 *       YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
 *       "AFTER_APP_SUBMIT",
 *       "AFTER_APP_RUNNING"
 *     );
 *   }
 *
 *   @Test
 *   public void testFeature() throws Exception {
 *     cluster = new ProcessBasedMiniYARNCluster.Builder(conf).build();
 *     yarnClient = YarnClient.createYarnClient();
 *     yarnClient.init(cluster.getConfiguration());
 *     yarnClient.start();
 *
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);
 *
 *     // Test logic with checkpoints...
 *     // No try-finally needed - @After handles cleanup!
 *   }
 * }
 * }</pre>
 *
 * <h3>Guarantees:</h3>
 * <ul>
 *   <li>Complete isolation between checkpoint executions</li>
 *   <li>Automatic cleanup of YARN processes and directories</li>
 *   <li>Verification of cleanup success</li>
 *   <li>Force cleanup if verification fails</li>
 * </ul>
 */
public abstract class YarnUpgradeTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(YarnUpgradeTestBase.class);

    // Fields synced from subclass via reflection
    protected String upgradeCheckpoint;

    // Protected fields for test use
    protected Configuration conf;
    protected ProcessBasedMiniYARNCluster cluster;
    protected YarnClient yarnClient;

    @Before
    public void setupTest() throws Exception {
        // Implementation...
    }

    @After
    public void tearDownTest() throws Exception {
        // Implementation...
    }

    /**
     * Checkpoint for potential upgrade.
     * IMPORTANT: Close all streams and resources before calling checkpoint() to avoid
     * broken pipelines during NodeManager restarts.
     */
    protected void checkpoint(String name) throws Exception {
        // Implementation...
    }

    protected boolean shouldUpgrade(String name) {
        // Implementation...
    }

    // Private helper methods...
}
```

---

## Validation Checklist

After generation, verify the base class has:

- [ ] Complete JavaDoc with usage example
- [ ] Apache License 2.0 header
- [ ] @Before method that syncs @Parameter fields using reflection
- [ ] @After method with independent try-catch blocks for each cleanup step
- [ ] checkpoint(String) method with health checks before and after upgrade
- [ ] shouldUpgrade(String) method with proper logic
- [ ] Process cleanup logic using jps | grep 'ResourceManager|NodeManager'
- [ ] Directory cleanup with age-based filtering (older than 1 hour)
- [ ] Cleanup verification method that checks for orphaned processes
- [ ] Proper null checks before all cleanup operations
- [ ] Fields set to null in finally blocks after cleanup
- [ ] Logger with appropriate log levels (INFO for major steps, DEBUG for details)
- [ ] No resource leaks in helper methods
- [ ] Platform-appropriate process management (Unix focus)
- [ ] Comprehensive exception handling (independent try-catch per cleanup step)

---

## Checkpoint Constants Class

You should also create a separate constants class:

```java
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

package org.apache.hadoop.yarn.server.process.upgrade;

/**
 * Common checkpoint names for YARN upgrade testing.
 *
 * <p>These constants define common points in YARN application lifecycle where
 * rolling upgrades can be tested. Tests should always include NO_UPGRADE as
 * the first checkpoint to establish a baseline.
 */
public final class YarnUpgradeCheckpoints {

    /** Baseline - no upgrade performed, verifies test passes without upgrade */
    public static final String NO_UPGRADE = "NO_UPGRADE";

    /** After cluster has started and all NodeManagers are connected */
    public static final String AFTER_CLUSTER_START = "AFTER_CLUSTER_START";

    /** After application has been submitted to ResourceManager */
    public static final String AFTER_APP_SUBMIT = "AFTER_APP_SUBMIT";

    /** After application is in RUNNING state with containers allocated */
    public static final String AFTER_APP_RUNNING = "AFTER_APP_RUNNING";

    /** After application has finished (SUCCEEDED, FAILED, or KILLED) */
    public static final String AFTER_APP_FINISHED = "AFTER_APP_FINISHED";

    /** After queue configuration has been refreshed */
    public static final String AFTER_QUEUE_REFRESH = "AFTER_QUEUE_REFRESH";

    /** After retrieving node reports from ResourceManager */
    public static final String AFTER_NODE_REPORT = "AFTER_NODE_REPORT";

    /** After containers have been allocated to application */
    public static final String AFTER_CONTAINER_ALLOCATION = "AFTER_CONTAINER_ALLOCATION";

    private YarnUpgradeCheckpoints() {
        // Utility class, no instantiation
    }
}
```

---

## Running Tests with the Generated Base Class

### Maven Command

```bash
# Run specific test with upgrade
mvn test \
  -Dtest=TestYourFeature \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run all upgrade tests
mvn test \
  -Dtest="*Upgrade*" \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run specific checkpoint
mvn test \
  -Dtest='TestYourFeature#testMethod[upgrade-at=AFTER_APP_RUNNING]' \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

### Environment Variables (Backward Compatibility)

```bash
# Also supported (for backward compatibility)
export HADOOP_HOME=/opt/hadoop-3.3.6
export HADOOP_UPGRADE_HOME=/opt/hadoop-3.4.0

mvn test -Dtest=TestYourFeature \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

---

## Tips for Best Results

1. **Be Specific**: The AI agent works best with concrete, specific requirements
2. **Include Imports**: Key imports are mentioned in the prompt
3. **Error Handling**: Independent try-catch blocks specified for cleanup
4. **Logging Level**: INFO for major steps, DEBUG for details, ERROR for failures
5. **Timeouts**: Specific timeout values provided (10s for NM connection, 2s for process termination)
6. **Thread Safety**: Explicitly stated that tests run sequentially
7. **Resource Ordering**: YarnClient stops before cluster shutdown
8. **Platform Differences**: Unix focus (Linux/macOS)

---

## Notes

- This prompt is designed for JUnit 4 with Parameterized runner
- For JUnit 5, adapt @Before/@After to @BeforeEach/@AfterEach and use @ParameterizedTest
- The reflection-based @Parameter syncing handles subclass parameter fields
- Comprehensive cleanup ensures no test pollution between executions
- System properties are handled automatically by ProcessBasedMiniYARNCluster

---

**End of YARN Upgrade Base Test Generation Guide**

Use this complete prompt to generate the `YarnUpgradeTestBase` class for your YARN upgrade testing framework. The generated class will provide robust, production-ready infrastructure for parameterized upgrade testing.
