# Step 3: YARN Test Transformation Guide

## Table of Contents
1. [Introduction & Philosophy](#introduction--philosophy)
2. [Prerequisites & Setup](#prerequisites--setup)
3. [Test Organization and Naming Convention](#test-organization-and-naming-convention)
4. [Core Transformation Rules](#core-transformation-rules)
5. [API Mapping Tables](#api-mapping-tables)
6. [Step-by-Step Transformation Process](#step-by-step-transformation-process)
7. [Common Transformation Patterns](#common-transformation-patterns)
8. [Inserting Cluster Upgrade Method Calls](#inserting-cluster-upgrade-method-calls)
9. [Parameterized Upgrade Checkpoints](#parameterized-upgrade-checkpoints)
10. [When to Comment Out Logic](#when-to-comment-out-logic)
11. [Testing Checklist](#testing-checklist)
12. [Best Practices](#best-practices)
13. [Quick Reference Decision Tree](#quick-reference-decision-tree)

---

## Introduction & Philosophy

### Purpose
Transform existing `MiniYARNCluster` tests to `ProcessBasedMiniYARNCluster` to enable:
- **Process-based testing** - Each YARN node runs in separate JVM for realistic testing
- **Multi-version testing** - Test upgrades between different Hadoop versions
- **Rolling upgrade scenarios** - Simulate production upgrade procedures
- **Version compatibility** - Verify YARN RPC compatibility across Hadoop versions

### Key Principle
**Most server-side operations have client-side RPC equivalents.** The goal is to maximize test logic preservation by finding client-side APIs that provide equivalent functionality.

### Transformation Hierarchy
When encountering server-side operations, try these approaches in order:

1. **YarnClient API** - High-level client operations (application management, node reports, queue info)
2. **ClientRMService API** - Mid-level operations (cluster metrics, direct RM protocol)
3. **ApplicationClientProtocol** - Low-level RPC interface
4. **RMAdminCLI / Admin APIs** - Administrative operations (refresh queues, refresh nodes)
5. **Web UI / REST API** - For metrics and runtime statistics via HTTP
6. **Comment Out** - Only if truly no client-side equivalent exists

### Why ProcessBasedMiniYARNCluster?

**MiniYARNCluster limitations:**
- All nodes run in same JVM - cannot test different Hadoop versions
- Direct object access - not realistic for production scenarios
- In-process - cannot simulate true process failures and restarts
- Cannot test rolling upgrades realistically

**ProcessBasedMiniYARNCluster benefits:**
- True process isolation - realistic testing
- Multi-version support - essential for Hadoop upgrade testing
- Client-only access - forces use of public APIs (more realistic)
- Better represents production YARN environments
- Enables realistic rolling upgrade testing

---

## Prerequisites & Setup

### System Properties (Automatic!)
ProcessBasedMiniYARNCluster automatically reads distributions from system properties:

```bash
# No manual setup needed! Just pass system properties to Maven:
mvn test -Dtest=YourTransformedTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

**Backward compatibility:** Environment variables (`HADOOP_HOME`, `HADOOP_UPGRADE_HOME`) still work as fallback.

### Test Configuration
```java
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.client.api.YarnClient;

// No @Before setup needed! System properties are read automatically!

@Test
public void testSomething() {
    // Just build - automatic!
    ProcessBasedMiniYARNCluster cluster =
        new ProcessBasedMiniYARNCluster.Builder(conf)
            .numNodeManagers(3)
            .build();  // Automatically reads system properties!

    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Use yarnClient for all operations...
}
```

### Running Transformed Tests
```bash
# Run with system properties (recommended)
mvn test -Dtest=YourTransformedTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run all ProcessBased tests
mvn test -Dtest="*_ProcessBased" \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

---

## Test Organization and Naming Convention

### File Location Strategy
**Place transformed tests in the SAME directory as the original tests**, using a naming suffix to distinguish them.

This approach provides:
- ✅ Side-by-side comparison of original and transformed tests
- ✅ Tests alphabetically adjacent in file listings
- ✅ Clear visual distinction via suffix
- ✅ Original package structure preserved
- ✅ Both versions can coexist long-term

### Naming Convention: `_ProcessBased` Suffix

```
Original Test:     TestYarnFeature.java
Transformed Test:  TestYarnFeature_ProcessBased.java

Location:          Same directory, same package
```

### Directory Structure Examples

```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/
    ├── TestApplicationSubmit.java                   [ORIGINAL - MiniYARNCluster]
    ├── TestApplicationSubmit_ProcessBased.java      [TRANSFORMED - ProcessBased]
    ├── TestResourceManager.java                     [ORIGINAL - MiniYARNCluster]
    ├── TestResourceManager_ProcessBased.java        [TRANSFORMED - ProcessBased]
    └── resourcemanager/
        ├── TestRMRestart.java                       [ORIGINAL - MiniYARNCluster]
        └── TestRMRestart_ProcessBased.java          [TRANSFORMED - ProcessBased]
```

### Package and Class Declaration

The transformed test uses the **same package** as the original:

```java
// Original: TestYarnFeature.java
package org.apache.hadoop.yarn.server;

public class TestYarnFeature {
  // ... MiniYARNCluster tests
}
```

```java
// Transformed: TestYarnFeature_ProcessBased.java
package org.apache.hadoop.yarn.server;  // Same package!

/**
 * ProcessBased version of {@link TestYarnFeature}.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * @see TestYarnFeature Original test using MiniYARNCluster
 */
public class TestYarnFeature_ProcessBased {
  // ... ProcessBasedMiniYARNCluster tests
}
```

### Test Execution Patterns

```bash
# Run original test only
mvn test -Dtest=TestYarnFeature

# Run transformed test only
mvn test -Dtest=TestYarnFeature_ProcessBased \
  -Dhadoop.start.home=/opt/hadoop-3.3.6

# Run ALL ProcessBased tests across the codebase
mvn test -Dtest="*_ProcessBased" \
  -Dhadoop.start.home=/opt/hadoop-3.3.6

# Run both versions for comparison
mvn test -Dtest=TestYarnFeature,TestYarnFeature_ProcessBased
```

---

## Core Transformation Rules

### Rule 1: Maximize Test Logic Preservation
**Preserve as much of the original test logic as possible** by finding client-side equivalents for server-side operations.

✅ **DO**: Find YarnClient API that provides same functionality
❌ **DON'T**: Remove test logic unless absolutely necessary

### Rule 2: Use the API Hierarchy
Always try to find client-side equivalents in this order:
1. YarnClient API (highest level) - application management, node reports, queue info
2. ClientRMService API (mid-level) - cluster metrics, direct RM protocol
3. ApplicationClientProtocol (RPC level) - low-level YARN RPC
4. RMAdminCLI / Admin APIs (admin operations) - queue refresh, node refresh
5. Web UI / REST API (monitoring) - HTTP-based metrics and monitoring

### Rule 3: Comment Out Only When Necessary
Only comment out operations when:
- No client-side API exists (e.g., internal RM state, NM internal storage)
- Operation accesses internal YARN state (e.g., RM scheduler internals)
- Operation manipulates JVM-internal state

### Rule 4: Document Minimally
Add comments only for:
- Non-obvious transformations
- Commented-out logic (explain why and what was removed)
- Workarounds or limitations

---

## API Mapping Tables

**These tables provide YARN-specific mappings from server-side operations to client-side APIs.**

### Table 1: MiniYARNCluster → ProcessBasedMiniYARNCluster

| MiniYARNCluster Method | ProcessBasedMiniYARNCluster | Status | Notes |
|------------------------|------------------------------|--------|-------|
| **Cluster Creation** |
| `new Builder(conf).build()` | `new Builder(conf).build()` | ✓ | Automatically reads hadoop.start.home/hadoop.upgrade.home |
| `new Builder(conf).numNodeManagers(n)` | `new Builder(conf).numNodeManagers(n)` | ✓ | Same API |
| `new Builder(conf).numResourceManagers(n)` | `new Builder(conf).numResourceManagers(n)` | ✓ | Same API for RM HA |
| **Client Access** |
| N/A (test creates YarnClient directly) | `createYarnClient()` | ✓ | New helper method |
| `getConfig()` | `getConfiguration()` | ✓ | Same API |
| **Configuration** |
| `getConfig()` | `getConfiguration()` | ⚠️ | Returns base config, not process-specific |
| **Node Management** |
| `restartResourceManager(int i)` | `restartResourceManager(int i)` | ✓ | Same API |
| `restartNodeManager(int i)` | `restartNodeManager(int i)` | ✓ | Same API |
| `shutdownResourceManager(int i)` | `shutdownResourceManager(int i)` | ✓ | Same API |
| `shutdownNodeManager(int i)` | `shutdownNodeManager(int i)` | ✓ | Same API |
| `waitForNodeManagersToConnect(timeout)` | `waitForNodeManagersToConnect(timeout)` | ✓ | Same API |
| `waitActive()` or `waitClusterUp()` | `waitClusterUp()` | ✓ | Same or similar |
| **Direct Object Access** (❌ Use YarnClient instead) |
| `getResourceManager()` | ❌ | ✗ | Use YarnClient APIs (see Table 2) |
| `getResourceManager(int i)` | ❌ | ✗ | Use YarnClient APIs (see Table 2) |
| `getNodeManager(int i)` | ❌ | ✗ | Use YarnClient.getNodeReports() |
| `getApplicationHistoryServer()` | ❌ | ✗ | Use Timeline service client APIs |
| **Cluster Control** |
| `triggerNodeHeartbeat()` | ⚠️ | ⚠️ | Use `Thread.sleep(3000)` + wait for state change |
| `shutdown()` | `shutdown()` | ✓ | Same API |
| **Cluster State** |
| `getNumNodeManagers()` | `getNumNodeManagers()` | ✓ | Same API |
| `getNumResourceManagers()` | `getNumResourceManagers()` | ✓ | Same API for RM HA |
| `isClusterUp()` | `isClusterUp()` | ✓ | Same API |
| `getActiveRMIndex()` | `getActiveRMIndex()` | ✓ | Same API for RM HA |

### Table 2: ResourceManager (Server-Side) → Client-Side APIs

**Context**: `ResourceManager` is a private, server-side class managing resource allocation, application lifecycle, and cluster coordination. All its operations should have client-side equivalents through YARN RPC.

| ResourceManager Method | Client-Side API | API Layer | Code Example |
|------------------------|-----------------|-----------|--------------|
| **Application Operations** |
| `submitApplication(request)` | `yarnClient.submitApplication(context)` | YarnClient | `ApplicationId appId = yarnClient.submitApplication(appContext);` |
| `getApplicationReport(appId)` | `yarnClient.getApplicationReport(appId)` | YarnClient | `ApplicationReport report = yarnClient.getApplicationReport(appId);` |
| `getAllApplications()` | `yarnClient.getApplications()` | YarnClient | `List<ApplicationReport> apps = yarnClient.getApplications();` |
| `getApplications(appTypes)` | `yarnClient.getApplications(appTypes)` | YarnClient | `List<ApplicationReport> apps = yarnClient.getApplications(types);` |
| `killApplication(appId)` | `yarnClient.killApplication(appId)` | YarnClient | `yarnClient.killApplication(appId);` |
| `forceKillApplication(appId)` | `yarnClient.killApplication(appId)` | YarnClient | Same as killApplication |
| **Queue Operations** |
| `getQueueInfo(queueName)` | `yarnClient.getQueueInfo(queueName)` | YarnClient | `QueueInfo info = yarnClient.getQueueInfo("default");` |
| `getAllQueues()` | `yarnClient.getAllQueues()` | YarnClient | `List<QueueInfo> queues = yarnClient.getAllQueues();` |
| `getChildQueueInfos(queueName)` | `yarnClient.getChildQueueInfos(queueName)` | YarnClient | `List<QueueInfo> children = yarnClient.getChildQueueInfos("root");` |
| **Cluster Metrics** |
| `getClusterMetrics()` | `yarnClient.getYarnClusterMetrics()` | YarnClient | `YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();` |
| **Node Operations** |
| `getNodeReports()` | `yarnClient.getNodeReports()` | YarnClient | `List<NodeReport> nodes = yarnClient.getNodeReports();` |
| `getNodeReports(state)` | `yarnClient.getNodeReports(state)` | YarnClient | `List<NodeReport> nodes = yarnClient.getNodeReports(NodeState.RUNNING);` |
| **Container Operations** |
| `getContainers(attemptId)` | `yarnClient.getContainers(attemptId)` | YarnClient | `List<ContainerReport> containers = yarnClient.getContainers(attemptId);` |
| `getContainerReport(containerId)` | `yarnClient.getContainerReport(containerId)` | YarnClient | `ContainerReport report = yarnClient.getContainerReport(containerId);` |
| **Application Attempt Operations** |
| `getApplicationAttemptReport(attemptId)` | `yarnClient.getApplicationAttemptReport(attemptId)` | YarnClient | `ApplicationAttemptReport report = yarnClient.getApplicationAttemptReport(attemptId);` |
| `getApplicationAttempts(appId)` | `yarnClient.getApplicationAttempts(appId)` | YarnClient | `List<ApplicationAttemptReport> attempts = yarnClient.getApplicationAttempts(appId);` |
| **Reservation Operations** |
| `submitReservation(request)` | `yarnClient.submitReservation(request)` | YarnClient | Only if reservation system enabled |
| `updateReservation(request)` | `yarnClient.updateReservation(request)` | YarnClient | Only if reservation system enabled |
| `deleteReservation(reservationId)` | `yarnClient.deleteReservation(reservationId)` | YarnClient | Only if reservation system enabled |
| **Admin Operations** |
| `refreshQueues()` | `RMAdminCLI.refreshQueues()` or admin protocol | RMAdminCLI | Via admin protocol |
| `refreshNodes()` | `RMAdminCLI.refreshNodes()` or admin protocol | RMAdminCLI | Via admin protocol |
| `refreshUserToGroupsMappings()` | `RMAdminCLI.refreshUserToGroupsMappings()` | RMAdminCLI | Via admin protocol |
| `transitionToActive()` | `RMAdminCLI.transitionToActive()` | RMAdminCLI | For RM HA |
| `transitionToStandby()` | `RMAdminCLI.transitionToStandby()` | RMAdminCLI | For RM HA |
| **Internal Operations** (❌ No client API) |
| `getRMContext()` | ❌ | - | Internal RM state - comment out |
| `getRMDispatcher()` | ❌ | - | Internal event dispatcher - comment out |
| `getApplicationMasterService()` | ❌ | - | Internal service - comment out |
| `getResourceScheduler()` | ❌ | - | Internal scheduler - comment out |
| **Delegation Token Operations** |
| `getDelegationToken(renewer)` | `yarnClient.getRMDelegationToken(renewer)` | YarnClient | `Token token = yarnClient.getRMDelegationToken(new Text("user"));` |
| `renewDelegationToken(token)` | `yarnClient.renewRMDelegationToken(token)` | YarnClient | `long expiry = yarnClient.renewRMDelegationToken(token);` |
| `cancelDelegationToken(token)` | `yarnClient.cancelRMDelegationToken(token)` | YarnClient | `yarnClient.cancelRMDelegationToken(token);` |

### Table 3: NodeManager (Server-Side) → Client-Side APIs

**Context**: `NodeManager` is a server-side class managing containers on worker nodes. Most NM state is accessible via ResourceManager's node reports.

| NodeManager Method | Client-Side API | API Layer | Status | Notes |
|--------------------|-----------------|-----------|--------|-------|
| **Node Status** |
| `getNodeHealthStatus()` | `yarnClient.getNodeReports()` then `report.getNodeHealthStatus()` | YarnClient | ✓ | Via NodeReport |
| `getNodeState()` | `yarnClient.getNodeReports()` then `report.getNodeState()` | YarnClient | ✓ | Via NodeReport |
| `getUsedResource()` | `yarnClient.getNodeReports()` then `report.getUsed()` | YarnClient | ✓ | Via NodeReport |
| `getCapability()` | `yarnClient.getNodeReports()` then `report.getCapability()` | YarnClient | ✓ | Via NodeReport |
| **Container Operations** |
| `getNMContext().getContainers()` | `yarnClient.getContainers(attemptId)` | YarnClient | ✓ | Via ContainerReport |
| `startContainer(request)` | ❌ (handled by ApplicationMaster) | - | ✗ | AM starts containers, not client |
| `stopContainer(containerId)` | ❌ (handled by ApplicationMaster) | - | ✗ | AM stops containers |
| **Internal Operations** (❌ No client API) |
| `getNodeStatusUpdater()` | ❌ | - | ✗ | Internal NM component - comment out |
| `getContainerManager()` | ❌ | - | ✗ | Internal NM component - comment out |
| `getLocalResourcesTracker()` | ❌ | - | ✗ | Internal storage - comment out |
| `getNMContext()` | ❌ | - | ✗ | Internal context - comment out |

### Table 4: Admin Operations → RMAdminCLI / Admin APIs

| Operation | RMAdminCLI Command/API | Code Example | Alternative API |
|-----------|------------------------|--------------|-----------------|
| **Upgrade Operations** |
| Start upgrade | N/A for YARN (use rolling upgrade) | Via process restart | `cluster.rollingUpgradeNodeManagers()` |
| Query upgrade status | Check node versions via NodeReports | `yarnClient.getNodeReports()` | Via JMX or REST API |
| **Queue Operations** |
| Refresh queues | `RMAdminCLI.refreshQueues()` | Via admin protocol | YarnClient admin methods |
| Refresh queue ACLs | Part of `refreshQueues()` | Via admin protocol | Included in refresh queues |
| **Node Operations** |
| Refresh nodes | `RMAdminCLI.refreshNodes()` | Via admin protocol | Update include/exclude files |
| Decommission nodes | Update exclude list + refreshNodes | Via admin protocol | Via configuration files |
| **Security Operations** |
| Refresh user mappings | `RMAdminCLI.refreshUserToGroupsMappings()` | Via admin protocol | Via admin client |
| Refresh super-user groups | `RMAdminCLI.refreshSuperUserGroupsConfiguration()` | Via admin protocol | Via admin client |
| Refresh ACLs | `RMAdminCLI.refreshAdminAcls()` | Via admin protocol | Via admin client |
| **HA Operations** |
| Transition to active | `RMAdminCLI.transitionToActive(rmId)` | `rmAdminCLI.transitionToActive("rm0")` | Via HAServiceProtocol |
| Transition to standby | `RMAdminCLI.transitionToStandby(rmId)` | `rmAdminCLI.transitionToStandby("rm0")` | Via HAServiceProtocol |
| Get HA state | `RMAdminCLI.getServiceState(rmId)` | `rmAdminCLI.getServiceState("rm0")` | `cluster.getActiveRMIndex()` |

### Table 5: Monitoring/Metrics → Web UI / JMX / ClusterMetrics

| Server-Side Check | Client-Side Alternative | Access Method | Example |
|-------------------|-------------------------|---------------|---------|
| **Cluster Metrics** |
| RM metrics | `yarnClient.getYarnClusterMetrics()` | YarnClient | `metrics.getNumNodeManagers()` |
| Active nodes count | `clusterMetrics.getNumActiveNodeManagers()` | YarnClient | Via ClusterMetrics |
| Decommissioned nodes | `clusterMetrics.getNumDecommissionedNodeManagers()` | YarnClient | Via ClusterMetrics |
| Lost nodes | `clusterMetrics.getNumLostNodeManagers()` | YarnClient | Via ClusterMetrics |
| Unhealthy nodes | `clusterMetrics.getNumUnhealthyNodeManagers()` | YarnClient | Via ClusterMetrics |
| Available memory | Sum from NodeReports | YarnClient | `nodeReport.getCapability().getMemorySize()` |
| Available vcores | Sum from NodeReports | YarnClient | `nodeReport.getCapability().getVirtualCores()` |
| **Application Metrics** |
| Running apps count | Filter `yarnClient.getApplications()` | YarnClient | `apps.stream().filter(r -> r.getYarnApplicationState() == RUNNING).count()` |
| Finished apps count | Filter `yarnClient.getApplications()` | YarnClient | Via application state filter |
| Failed apps count | Filter `yarnClient.getApplications()` | YarnClient | Via application state filter |
| **Queue Metrics** |
| Queue capacity | `yarnClient.getQueueInfo(queue)` | YarnClient | `queueInfo.getCapacity()` |
| Queue current capacity | `queueInfo.getCurrentCapacity()` | YarnClient | Via QueueInfo |
| Queue max capacity | `queueInfo.getMaximumCapacity()` | YarnClient | Via QueueInfo |
| **JMX Metrics** |
| RM JMX endpoint | HTTP GET to `/jmx` | REST API | `http://rm-host:8088/jmx` |
| NM JMX endpoint | HTTP GET to `/jmx` | REST API | `http://nm-host:8042/jmx` |

### Table 6: Common Test Utilities

| MiniYARNCluster Utility | ProcessBasedMiniYARNCluster Alternative | Notes |
|-------------------------|------------------------------------------|-------|
| `cluster.triggerNodeHeartbeat()` | `Thread.sleep(3000)` + verify state | Wait for natural NodeManager heartbeat cycle (default 3s) |
| `cluster.getResourceManager().getNodeStatus()` | `yarnClient.getNodeReports()` | Via client API |
| `cluster.getResourceManager().getQueueInfo()` | `yarnClient.getQueueInfo(queueName)` | Via client API |
| `cluster.waitForNodeManagersToConnect(timeout)` | `cluster.waitForNodeManagersToConnect(timeout)` | Same API |
| Direct RM/NM object access | Use YarnClient APIs | Always use client APIs |

---

## Step-by-Step Transformation Process

### Step 0: Create Transformed Test File

**Goal**: Set up the new test file with proper naming and location.

1. **Locate the original test:**
   ```bash
   # Example: Original test
   hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
   └── src/test/java/org/apache/hadoop/yarn/server/TestYarnFeature.java
   ```

2. **Create new file with `_ProcessBased` suffix in the SAME directory:**
   ```bash
   # New transformed test (same directory!)
   hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
   └── src/test/java/org/apache/hadoop/yarn/server/TestYarnFeature_ProcessBased.java
   ```

3. **Copy original test content to new file:**
   ```bash
   cp TestYarnFeature.java TestYarnFeature_ProcessBased.java
   ```

4. **Update class name and add Javadoc:**
   ```java
   package org.apache.hadoop.yarn.server;  // Same package as original!

   /**
    * ProcessBased version of {@link TestYarnFeature}.
    *
    * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
    * process-based testing and multi-version Hadoop upgrade scenarios.
    *
    * @see TestYarnFeature Original test using MiniYARNCluster
    */
   public class TestYarnFeature_ProcessBased {  // Note: _ProcessBased suffix
     // ... test methods
   }
   ```

5. **Checklist before proceeding:**
   - [ ] New file created in same directory as original
   - [ ] Class name has `_ProcessBased` suffix
   - [ ] Package declaration is identical to original
   - [ ] Javadoc references original test with `@see` tag
   - [ ] File compiles (even if tests fail)

### Step 1: Analyze Test Dependencies

**Goal**: Understand what server-side operations the test uses.

1. **Scan for direct object access patterns:**
   ```bash
   # Search for common patterns in YARN tests
   grep -E "cluster\.(getResourceManager|getNodeManager)" TestYarnFeature.java
   grep -E "\.getRMContext\(\)" TestYarnFeature.java
   grep -E "\.getResourceScheduler\(\)" TestYarnFeature.java
   grep -E "\.getNMContext\(\)" TestYarnFeature.java
   ```

2. **Categorize operations:**
   - ✅ **Already client-side**: YarnClient operations, application submission
   - ⚠️ **Has client equivalent**: RM methods, NM status checks
   - ❌ **No client equivalent**: Internal RM state, scheduler internals, NM context

3. **Plan transformation:**
   - List all operations that need transformation
   - Find client equivalents in mapping tables above
   - Identify operations that must be commented out

### Step 2: Transform Import Statements

```java
// BEFORE
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;

// AFTER
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
// Remove: import ...ResourceManager;
// Remove: import ...NodeManager;
// Keep only if needed for type declarations that can't be removed
```

### Step 3: Transform Cluster Setup

#### Basic Cluster Creation

```java
// BEFORE (MiniYARNCluster)
Configuration conf = new YarnConfiguration();
MiniYARNCluster cluster = new MiniYARNCluster.Builder(conf)
    .numNodeManagers(3)
    .build();
cluster.waitForNodeManagersToConnect(5000);

// Create YarnClient
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfig());
yarnClient.start();

// AFTER (ProcessBasedMiniYARNCluster) - AUTOMATIC!
Configuration conf = new YarnConfiguration();
// No environment variable checks needed! Automatic!

ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(3)
        .format(true)
        .build();  // Automatically reads system properties!
cluster.waitForNodeManagersToConnect(5000);

// Create YarnClient (same as before)
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();
```

**Note:** System properties are passed via Maven:
```bash
mvn test -Dtest=MyTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
```

#### Multi-Version Cluster (for upgrade tests)

```java
// All ProcessBased tests support upgrades automatically!
// Just build the cluster - it reads system properties automatically

ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(3)
        .format(true)
        .build();  // Starts with hadoop.start.home

// Later, perform rolling upgrade to hadoop.upgrade.home
String upgradeHome = cluster.getUpgradeDistributionPath();
for (int i = 0; i < 3; i++) {
    cluster.shutdownNodeManager(i);
    cluster.changeNodeManagerVersion(i, upgradeHome);
    cluster.startNodeManager(i);
    cluster.waitForNodeManagersToConnect(5000);
}
```

### Step 4: Transform Operations Using Mapping Tables

**Use the mapping tables above to guide these transformations.**

#### Example 1: YarnClient Operations (No Change)
```java
// These work identically - no transformation needed
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

// Submit application
ApplicationId appId = yarnClient.submitApplication(appContext);

// Get application report
ApplicationReport report = yarnClient.getApplicationReport(appId);
```

#### Example 2: ResourceManager Access → YarnClient API
```java
// BEFORE: Direct server-side access
ResourceManager rm = cluster.getResourceManager();
YarnClusterMetrics metrics = rm.getClusterMetrics();
int numNodes = metrics.getNumNodeManagers();

// AFTER: Client-side equivalent (use Table 2!)
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
int numNodes = metrics.getNumNodeManagers();
```

#### Example 3: NodeManager Status → NodeReport via YarnClient
```java
// BEFORE: Direct NM access
NodeManager nm = cluster.getNodeManager(0);
NodeHealthStatus health = nm.getNodeHealthStatus();
Resource used = nm.getNMContext().getNodeResourceMonitor().getUsedResource();

// AFTER: Via YarnClient NodeReports (use Table 3!)
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

List<NodeReport> nodes = yarnClient.getNodeReports();
NodeReport nodeReport = nodes.get(0);
NodeHealthStatus health = nodeReport.getNodeHealthStatus();
Resource used = nodeReport.getUsed();
```

#### Example 4: Internal RM State → Comment Out
```java
// BEFORE: Internal RM state verification
ResourceManager rm = cluster.getResourceManager();
ResourceScheduler scheduler = rm.getResourceScheduler();
// ... verification of internal scheduler state

// AFTER: Comment out with documentation
// TRANSFORMATION NOTE: Internal ResourceScheduler state verification removed.
// getResourceScheduler() provides access to internal RM scheduler state,
// which is not available via any client API (YarnClient, RMAdminCLI, etc.).
// Original test verified internal scheduler queue structure and resource allocation.
// No client-side alternative available - scheduler internals are not exposed.
//
// Original code:
// ResourceManager rm = cluster.getResourceManager();
// ResourceScheduler scheduler = rm.getResourceScheduler();
// ... (commented out verification code)
```

### Step 5: Transform Cleanup Code

```java
// BEFORE
@After
public void tearDown() {
    if (yarnClient != null) {
        yarnClient.stop();
    }
    if (cluster != null) {
        cluster.shutdown();
    }
}

// AFTER (same, or use try-with-resources if supported)
@After
public void tearDown() {
    if (yarnClient != null) {
        yarnClient.stop();
    }
    if (cluster != null) {
        cluster.shutdown();
    }
}

// OR for parameterized tests, extend YarnUpgradeTestBase:
// (No @After needed - base class handles cleanup automatically!)
```

### Step 6: Validate Transformation

Run through this checklist:

- [ ] All imports updated
- [ ] All direct RM/NM object access either transformed or commented out
- [ ] Cluster creation includes distribution path (via system properties)
- [ ] YarnClient properly initialized
- [ ] Test logic preserved as much as possible
- [ ] Only truly internal operations commented out
- [ ] Comments added for non-obvious transformations
- [ ] Test compiles without errors
- [ ] System properties documented in test javadoc

---

## Common Transformation Patterns

### Pattern 1: Application Submission and Monitoring
**Status**: ✅ No transformation needed

```java
// Works identically in both frameworks
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

// Submit application
ApplicationSubmissionContext appContext = ...;
ApplicationId appId = yarnClient.submitApplication(appContext);

// Monitor application
ApplicationReport report = yarnClient.getApplicationReport(appId);
YarnApplicationState state = report.getYarnApplicationState();
FinalApplicationStatus finalStatus = report.getFinalApplicationStatus();
```

### Pattern 2: Cluster Metrics
```java
// BEFORE: Via ResourceManager object
ResourceManager rm = cluster.getResourceManager();
YarnClusterMetrics metrics = rm.getClusterMetrics();

// AFTER: Via YarnClient (Table 5)
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
int numNMs = metrics.getNumNodeManagers();
int activeNMs = metrics.getNumActiveNodeManagers();
```

### Pattern 3: Node Information
```java
// BEFORE: Direct NodeManager access
NodeManager nm = cluster.getNodeManager(0);
NodeHealthStatus health = nm.getNodeHealthStatus();
Resource capability = nm.getNMContext().getNodeResourceMonitor().getCapability();

// AFTER: Via YarnClient NodeReports (Table 3)
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

List<NodeReport> nodes = yarnClient.getNodeReports();
NodeReport nodeReport = nodes.get(0);
NodeHealthStatus health = nodeReport.getNodeHealthStatus();
Resource capability = nodeReport.getCapability();
```

### Pattern 4: Waiting for State Changes
```java
// BEFORE: Trigger immediate state propagation
cluster.triggerNodeHeartbeat();

// AFTER: Wait for natural propagation (Table 6)
Thread.sleep(3000);  // Wait for NodeManager heartbeat interval (default 3s)

// Or use GenericTestUtils.waitFor() for condition-based waiting
GenericTestUtils.waitFor(() -> {
    try {
        List<NodeReport> nodes = yarnClient.getNodeReports();
        return nodes.size() == expectedNumNodes;
    } catch (Exception e) {
        return false;
    }
}, 500, 30000);
```

### Pattern 5: Queue Operations
```java
// BEFORE: Via ResourceManager
ResourceManager rm = cluster.getResourceManager();
QueueInfo queueInfo = rm.getQueueInfo("default");

// AFTER: Via YarnClient (Table 2)
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

QueueInfo queueInfo = yarnClient.getQueueInfo("default");
float capacity = queueInfo.getCapacity();
float currentCapacity = queueInfo.getCurrentCapacity();
```

---

## Inserting Cluster Upgrade Method Calls

### Overview

When transforming tests to support rolling upgrades, you need to insert `cluster.rollingUpgradeNodeManagers()` method calls at appropriate points in the test. This section explains how to identify upgrade points and handle the critical pattern of **closing resources before upgrade and reopening them afterward**.

### Why Resource Management is Critical

During a rolling upgrade, YARN NodeManager processes are restarted with new Hadoop versions. This restart **breaks active connections** between the client and the nodes.

**Key principle**: Any active connection/stream/resource that spans an upgrade point must be:
1. **Closed** before calling `cluster.rollingUpgradeNodeManagers()`
2. **Reopened** after the upgrade completes

### Identifying Upgrade Points

An upgrade point is a logical location in your test where you want to simulate a rolling upgrade. Common upgrade points include:

1. **Mid-operation** - Testing that applications created before upgrade continue after upgrade
2. **Between distinct test phases** - After setup operations but before verification
3. **After application submission** - Testing upgrade while applications are pending/running
4. **During application execution** - Testing upgrade resilience during active workload

### Step-by-Step: Inserting Upgrade Calls

#### Step 1: Identify the Upgrade Point

Look for a logical point in the test where upgrade makes sense:

```java
// BEFORE: Original test without upgrade
yarnClient.submitApplication(appContext1);
ApplicationReport report1 = waitForAppCompletion(yarnClient, appId1);
// <-- Potential upgrade point
yarnClient.submitApplication(appContext2);
ApplicationReport report2 = waitForAppCompletion(yarnClient, appId2);
```

#### Step 2: Close Resources Before Upgrade

If a resource is open at the upgrade point, close it first:

```java
// AFTER: With upgrade point inserted
yarnClient.submitApplication(appContext1);
ApplicationReport report1 = waitForAppCompletion(yarnClient, appId1);

// === ROLLING UPGRADE POINT ===
// CRITICAL: Stop YarnClient before upgrade since NMs will be restarted
yarnClient.stop();
System.out.println("Stopped YarnClient before rolling upgrade");
```

#### Step 3: Call cluster.rollingUpgradeNodeManagers()

```java
// Perform rolling upgrade
cluster.rollingUpgradeNodeManagers();  // Upgrades all NMs one by one
System.out.println("Rolling upgrade of NodeManagers completed successfully");
```

**Note**: The `cluster.rollingUpgradeNodeManagers()` method:
- Automatically upgrades each NodeManager one at a time
- Waits for each NM to reconnect before upgrading the next
- Ensures cluster remains operational throughout upgrade
- Uses hadoop.upgrade.home from system properties

#### Step 4: Reopen Resources After Upgrade

If you need to continue operations, reopen the resource:

```java
// Recreate and restart YarnClient after upgrade
yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();
System.out.println("Restarted YarnClient after upgrade");

// Continue operations
yarnClient.submitApplication(appContext2);
ApplicationReport report2 = waitForAppCompletion(yarnClient, appId2);
```

### Complete Example Pattern

```java
@Test
public void testApplicationAcrossUpgrade() throws Exception {
    Configuration conf = new YarnConfiguration();

    ProcessBasedMiniYARNCluster cluster =
        new ProcessBasedMiniYARNCluster.Builder(conf)
            .numNodeManagers(3)
            .build();
    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    try {
        cluster.waitForNodeManagersToConnect(5000);

        // Submit first application
        ApplicationId appId1 = submitDistributedShellApp(yarnClient, "sleep 10");
        waitForAppCompletion(yarnClient, appId1);
        assertEquals(FinalApplicationStatus.SUCCEEDED,
            yarnClient.getApplicationReport(appId1).getFinalApplicationStatus());

        // === ROLLING UPGRADE POINT ===
        // STEP 1: Stop YarnClient before upgrade
        yarnClient.stop();
        System.out.println("Stopped YarnClient before rolling upgrade");

        // STEP 2: Perform rolling upgrade
        cluster.rollingUpgradeNodeManagers();
        System.out.println("Rolling upgrade completed successfully");

        // STEP 3: Recreate YarnClient
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(cluster.getConfiguration());
        yarnClient.start();
        System.out.println("Restarted YarnClient after upgrade");

        // Continue operations after upgrade
        ApplicationId appId2 = submitDistributedShellApp(yarnClient, "sleep 10");
        waitForAppCompletion(yarnClient, appId2);
        assertEquals(FinalApplicationStatus.SUCCEEDED,
            yarnClient.getApplicationReport(appId2).getFinalApplicationStatus());

    } finally {
        yarnClient.stop();
        cluster.shutdown();
    }
}
```

### Common Mistakes to Avoid

#### ❌ Mistake 1: Not stopping YarnClient before upgrade

```java
// WRONG - YarnClient remains connected during upgrade
yarnClient.submitApplication(appContext1);
cluster.rollingUpgradeNodeManagers();  // Connections will break!
yarnClient.submitApplication(appContext2);  // This will fail!
```

#### ✅ Correct Pattern

```java
// CORRECT - Stop, upgrade, restart
yarnClient.submitApplication(appContext1);
yarnClient.stop();

cluster.rollingUpgradeNodeManagers();

yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();
yarnClient.submitApplication(appContext2);
```

---

## Parameterized Upgrade Checkpoints

### Overview

**Recommended Approach**: Instead of hardcoding a single upgrade point in each test, use JUnit parameterization to run each test multiple times with upgrades at different checkpoints. This provides comprehensive upgrade coverage with reproducible results.

**Key Benefits**:
- Single test → multiple upgrade scenarios automatically
- 100% reproducible (deterministic checkpoint execution)
- Comprehensive coverage (10+ checkpoints per test)
- Guaranteed cleanup between executions
- Easy to identify which checkpoint caused failure

### Base Class: YarnUpgradeTestBase

All ProcessBased tests should extend `YarnUpgradeTestBase`, which provides:

1. **@Before cleanup**: Kills orphaned YARN processes, cleans old directories
2. **@After cleanup**: Stops YarnClient, shuts down cluster, verifies cleanup
3. **checkpoint(name)**: Performs upgrade if name matches parameter
4. **shouldUpgrade(name)**: Checks if upgrade should happen

**Location**: `org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase`

### Transformation Steps

#### Step 1: Add Parameterization Framework

```java
// Add imports
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import java.util.Arrays;
import java.util.Collection;

// Add annotation and extend base class
@RunWith(Parameterized.class)
public class TestYarnFeature_ProcessBased extends YarnUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,           // Always include baseline
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_APP_SUBMIT",
      "AFTER_APP_RUNNING",
      "AFTER_APP_FINISHED",
      "AFTER_CONTAINER_ALLOCATION",
      // ... 10+ checkpoints per test
    );
  }
}
```

#### Step 2: Remove try-finally Blocks

**BEFORE** (manual cleanup):
```java
@Test
public void testSomething() throws Exception {
  Configuration conf = new YarnConfiguration();
  ProcessBasedMiniYARNCluster cluster = new Builder(conf).build();
  YarnClient yarnClient = YarnClient.createYarnClient();
  yarnClient.init(cluster.getConfiguration());
  yarnClient.start();

  try {
    // test logic
  } finally {
    yarnClient.stop();
    cluster.shutdown();
  }
}
```

**AFTER** (automatic cleanup via base class):
```java
@Test
public void testSomething() throws Exception {
  // Use conf, cluster, yarnClient from base class
  cluster = new ProcessBasedMiniYARNCluster.Builder(conf).build();
  yarnClient = YarnClient.createYarnClient();
  yarnClient.init(cluster.getConfiguration());
  yarnClient.start();

  // test logic with checkpoints
  ApplicationId appId = submitApp(yarnClient);
  checkpoint(YarnUpgradeCheckpoints.AFTER_APP_SUBMIT);

  waitForAppCompletion(yarnClient, appId);
  checkpoint("AFTER_APP_FINISHED");

  // No try-finally needed - @After handles cleanup!
}
```

#### Step 3: Replace Hardcoded cluster.rollingUpgradeNodeManagers() with checkpoint()

**BEFORE** (hardcoded upgrade point):
```java
ApplicationId appId1 = submitApp(yarnClient);

// === ROLLING UPGRADE POINT ===
yarnClient.stop();
cluster.rollingUpgradeNodeManagers();
yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

ApplicationId appId2 = submitApp(yarnClient);
```

**AFTER** (parameterized checkpoints):
```java
ApplicationId appId1 = submitApp(yarnClient);
checkpoint(YarnUpgradeCheckpoints.AFTER_APP_SUBMIT);

// Stop before potential upgrade
yarnClient.stop();
checkpoint("AFTER_CLIENT_STOP");

// Restart (always needed, regardless of upgrade)
yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();
checkpoint("AFTER_CLIENT_RESTART");

ApplicationId appId2 = submitApp(yarnClient);
checkpoint("AFTER_APP_SUBMIT_2");
```

### Checkpoint Naming Guidelines

1. **Always include NO_UPGRADE first**: Ensures test passes without upgrade
2. **Use YarnUpgradeCheckpoints constants**: For common checkpoint names
3. **Use custom strings**: For test-specific checkpoints
4. **Be descriptive**: "AFTER_APP_RUNNING" not "CHECKPOINT_3"
5. **Fine-grained coverage**: 10-15 checkpoints per test method

**Common checkpoint categories**:
- Cluster lifecycle: `AFTER_CLUSTER_START`
- Application operations: `AFTER_APP_SUBMIT`, `AFTER_APP_RUNNING`, `AFTER_APP_FINISHED`
- Client lifecycle: `AFTER_CLIENT_STOP`, `AFTER_CLIENT_RESTART`
- Resource operations: `AFTER_CONTAINER_ALLOCATION`, `AFTER_CONTAINER_RELEASE`
- Queue operations: `AFTER_QUEUE_REFRESH`

### Running Parameterized Tests

**Run all checkpoints**:
```bash
mvn test -Dtest=TestYarnFeature_ProcessBased \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
```

**Run specific checkpoint**:
```bash
mvn test -Dtest='TestYarnFeature_ProcessBased#testMethod[upgrade-at=AFTER_APP_RUNNING]' \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
```

---

## When to Comment Out Logic

### Only comment out operations that are:

1. **Internal RM State Operations**
   - RMContext access
   - ResourceScheduler internals
   - Internal RM dispatcher
   - Application state machine internals

2. **Internal NM Operations**
   - NMContext access
   - Container manager internals
   - Local resource tracker
   - NM state store internals

3. **In-Process Manipulation**
   - Direct object field modification
   - Mock object injection
   - Reflection on private fields

4. **JVM-Level Operations**
   - Memory manipulation
   - Thread state inspection (beyond public APIs)
   - ClassLoader manipulation

### Comment Template

Use this template when commenting out unsupported logic:

```java
// TRANSFORMATION NOTE: [Brief explanation of what was removed]
// [Why it was removed - what makes it inaccessible via client APIs]
// [What the original code verified/tested]
// [Suggestion for alternative verification if applicable, or "No client-side alternative available"]
//
// Original code:
// [indented commented-out code]
```

### Example: Internal RM Scheduler Check

```java
// TRANSFORMATION NOTE: Internal ResourceScheduler state verification removed.
// The ResourceScheduler class is internal to the ResourceManager process and not
// accessible via any client API (YarnClient, RMAdminCLI, Web UI, etc.). The original
// test verified internal scheduler queue structure and resource preemption logic.
// No client-side alternative available - scheduler internals are not exposed.
// Scheduler behavior can only be tested via application submission and observing
// allocation patterns via ApplicationReports and ContainerReports.
//
// Original code:
// ResourceManager rm = cluster.getResourceManager();
// ResourceScheduler scheduler = rm.getResourceScheduler();
// CapacityScheduler cs = (CapacityScheduler) scheduler;
// CSQueue queue = cs.getQueue("default");
// ... (verification of internal queue state)
```

### When NOT to Comment Out

Do NOT comment out if there's a client-side equivalent:

❌ **WRONG**:
```java
// TRANSFORMATION NOTE: Cannot access ResourceManager directly
// Original code:
// ResourceManager rm = cluster.getResourceManager();
// int numApps = rm.getAllApplications().size();
```

✅ **CORRECT**:
```java
// Use YarnClient instead of direct RM access
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();
int numApps = yarnClient.getApplications().size();
```

---

## Testing Checklist

### Before Running Test

- [ ] **File organization correct**
  - Transformed test in same directory as original
  - File name has `_ProcessBased` suffix
  - Package declaration identical to original
  - Class name matches file name with `_ProcessBased` suffix
  - Javadoc includes `@see` reference to original test

- [ ] **System properties ready**
  ```bash
  # System properties will be passed when running tests:
  mvn test -Dtest=MyTest \
    -Dhadoop.start.home=/opt/hadoop-3.3.6 \
    -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
  ```

- [ ] **Test compiles without errors**
  ```bash
  mvn test-compile -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
  ```

- [ ] **Imports are correct**
  - ProcessBasedMiniYARNCluster imported
  - YarnClient imported
  - Unnecessary RM/NM imports removed
  - Client API imports added (ApplicationReport, NodeReport, etc.)

### During Test Execution

- [ ] **Cluster starts successfully**
  - Check logs for "Cluster started successfully"
  - Verify all ResourceManagers and NodeManagers are up

- [ ] **YarnClient accessible**
  - Can create and start YarnClient instance
  - Can perform basic operations (getYarnClusterMetrics)

- [ ] **Core assertions pass**
  - Main test logic validates correctly
  - Application submission and completion work
  - Data integrity checks pass

### After Test Execution

- [ ] **Test passes (or fails as expected)**
  - If original test passed, transformed test should pass
  - If failure, verify it's not due to transformation

- [ ] **Cluster cleans up properly**
  - No orphaned ResourceManager or NodeManager processes (check with `jps`)
  - Test directories cleaned up

- [ ] **Review transformation quality**
  - Maximum logic preserved?
  - Only necessary operations commented out?
  - Appropriate documentation added?

---

## Best Practices

### DO ✅

1. **Consult mapping tables first** - Before assuming something is unsupported, check all mapping tables (Tables 1-6)

2. **Use the API hierarchy** - Try YarnClient → ClientRMService → ApplicationClientProtocol → RMAdminCLI → Web UI in order

3. **Preserve test intent** - Even if implementation changes, maintain what the test is verifying

4. **System properties are automatic** - No manual environment checks needed!

5. **Keep transformations minimal** - Change only what's necessary

6. **Document significant changes** - But only non-obvious ones

7. **Test both single-version and multi-version scenarios** when applicable

8. **Always initialize YarnClient properly**:
   ```java
   YarnClient yarnClient = YarnClient.createYarnClient();
   yarnClient.init(cluster.getConfiguration());
   yarnClient.start();
   ```

### DON'T ❌

1. **Don't give up on transformation too early** - Most operations have YarnClient equivalents

2. **Don't remove test logic without checking mapping tables** - Tables 2-5 have comprehensive mappings

3. **Don't use MiniYARNCluster-specific test utilities** - Many have client API equivalents

4. **Don't over-document** - Only comment what's not obvious

5. **Don't mix MiniYARNCluster and ProcessBasedMiniYARNCluster** in same test

6. **Don't manually check environment variables** - System properties are handled automatically!

7. **Don't access RM or NM objects directly** - Always use YarnClient APIs

### Performance Considerations

1. **Process startup is slower** - ProcessBasedMiniYARNCluster takes longer to start than MiniYARNCluster
   - Be patient with cluster startup
   - Consider increasing timeouts for slow systems

2. **NodeManager heartbeats are real-time** - Can't artificially trigger them
   - Use `Thread.sleep(3000)` or `GenericTestUtils.waitFor()`
   - Account for natural heartbeat intervals (default 3s)

3. **RPC overhead** - All operations go through YARN RPC protocols
   - Slightly slower than in-process calls
   - Not significant for most tests

---

## Quick Reference Decision Tree

```
Found server-side operation?
    │
    ├─> Is it already client-side? (yarnClient.someMethod())
    │   └─> ✅ Use as-is, no transformation needed
    │
    ├─> Check ResourceManager table (Table 2)
    │   ├─> Found YarnClient equivalent?
    │   │   └─> ✅ Use YarnClient API
    │   └─> Not found?
    │       └─> Continue...
    │
    ├─> Check NodeManager table (Table 3)
    │   ├─> Found NodeReport equivalent?
    │   │   └─> ✅ Use yarnClient.getNodeReports()
    │   └─> Not found?
    │       └─> Continue...
    │
    ├─> Check Admin operations table (Table 4)
    │   ├─> Found RMAdminCLI equivalent?
    │   │   └─> ✅ Use RMAdminCLI or admin protocol
    │   └─> Not found?
    │       └─> Continue...
    │
    ├─> Check Monitoring table (Table 5)
    │   ├─> Found ClusterMetrics/Web UI equivalent?
    │   │   └─> ✅ Use YarnClient metrics or REST API
    │   └─> Not found?
    │       └─> Continue...
    │
    └─> No client-side equivalent exists
        └─> ❌ Comment out with documentation template
```

---

## Summary

### Transformation Success Criteria

A successful transformation:
1. ✅ Compiles without errors
2. ✅ Runs with ProcessBasedMiniYARNCluster
3. ✅ Preserves maximum test logic
4. ✅ Uses YarnClient APIs for all accessible operations
5. ✅ Comments out only truly inaccessible operations (internal RM/NM state)
6. ✅ Includes minimal, clear documentation
7. ✅ Passes when original test passed

### Key Takeaways

- **Most operations have YarnClient equivalents** - Consult mapping tables thoroughly (Tables 1-6)
- **Use the API hierarchy** - YarnClient → ClientRMService → ApplicationClientProtocol → RMAdminCLI → Web UI
- **Only comment out internal RM/NM operations** - Everything else has a client API
- **Document sparingly** - Only non-obvious transformations
- **Test thoroughly** - Verify core test logic preserved
- **System properties are automatic** - No manual setup needed

---

**End of YARN Test Transformation Guide**

This comprehensive guide provides everything needed to transform MiniYARNCluster tests to ProcessBasedMiniYARNCluster. Use the API mapping tables, follow the step-by-step process, and consult the decision tree when in doubt. The goal is to preserve as much test logic as possible while adapting to the process-based, client-only architecture.
