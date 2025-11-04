# MiniDFSCluster to ProcessBasedMiniDFSCluster Transformation Guide

## Table of Contents
1. [Introduction & Philosophy](#introduction--philosophy)
2. [Prerequisites & Setup](#prerequisites--setup)
3. [Test Organization and Naming Convention](#test-organization-and-naming-convention)
4. [Core Transformation Rules](#core-transformation-rules)
5. [Comprehensive API Mapping Tables](#comprehensive-api-mapping-tables)
6. [Step-by-Step Transformation Process](#step-by-step-transformation-process)
7. [Common Transformation Patterns](#common-transformation-patterns)
8. [Inserting Cluster Upgrade Method Calls](#inserting-cluster-upgrade-method-calls)
9. [When to Comment Out Logic](#when-to-comment-out-logic)
10. [Testing Checklist](#testing-checklist)
11. [Best Practices](#best-practices)
12. [Quick Reference Decision Tree](#quick-reference-decision-tree)

---

## Introduction & Philosophy

### Purpose
Transform existing MiniDFSCluster tests to ProcessBasedMiniDFSCluster to enable:
- **Process-based testing** - Each node runs in separate JVM for realistic testing
- **Multi-version testing** - Test upgrades between different Hadoop versions
- **Rolling upgrade scenarios** - Simulate production upgrade procedures
- **Version compatibility** - Verify protocol compatibility across versions

### Key Principle
**Most server-side operations have client-side RPC equivalents.** The goal is to maximize test logic preservation by finding client-side APIs that provide equivalent functionality.

### Transformation Hierarchy
When encountering server-side operations, try these approaches in order:

1. **FileSystem API** - High-level file operations (`fs.mkdirs()`, `fs.delete()`, etc.)
2. **DFSClient API** - Mid-level HDFS operations (block locations, namespace info)
3. **ClientProtocol** - Low-level RPC interface (nearly 1:1 with FSNamesystem)
4. **Admin Tools** - DFSAdmin for administrative operations
5. **JMX/Monitoring** - For metrics and runtime statistics
6. **Comment Out** - Only if truly no client-side equivalent exists

### Why ProcessBasedMiniDFSCluster?

**MiniDFSCluster limitations:**
- All nodes run in same JVM - cannot test different Hadoop versions
- Direct object access - not realistic for production scenarios
- In-process - cannot simulate true process failures and restarts

**ProcessBasedMiniDFSCluster benefits:**
- True process isolation - realistic testing
- Multi-version support - essential for upgrade testing
- Client-only access - forces use of public APIs (more realistic)
- Better represents production environments

---

## Prerequisites & Setup

### System Properties (Automatic!)
ProcessBasedMiniDFSCluster now **automatically** reads Hadoop distributions from system properties:

```bash
# No manual setup needed! Just pass system properties to Maven:
mvn test -Dtest=YourTransformedTest \
  -Dhadoop.start.home=/path/to/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/path/to/hadoop-3.3.6 \
  -pl hadoop-hdfs-project/hadoop-hdfs
```

**Backward compatibility:** Environment variables (`HADOOP_HOME`, `HADOOP_UPGRADE_HOME`) still work as fallback.

### Test Configuration
```java
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;

// No @Before setup needed! System properties are read automatically!

@Test
public void testSomething() {
    // Just build - automatic!
    ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(3)
            .build();  // Automatically reads hadoop.start.home and hadoop.upgrade.home!
}
```

### Running Transformed Tests
```bash
# Run with system properties (recommended)
mvn test -Dtest=YourTransformedTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6 \
  -pl hadoop-hdfs-project/hadoop-hdfs

# Or use the automated script
./run-upgrade-test.sh --test-class YourTransformedTest
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
Original Test:     TestRollingUpgrade.java
Transformed Test:  TestRollingUpgrade_ProcessBased.java

Location:          Same directory, same package
```

### Directory Structure Examples

```
hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/
├── TestRollingUpgrade.java                      [ORIGINAL - MiniDFSCluster]
├── TestRollingUpgrade_ProcessBased.java         [TRANSFORMED - ProcessBased]
├── TestRollingUpgradeRollback.java              [ORIGINAL - MiniDFSCluster]
├── TestRollingUpgradeRollback_ProcessBased.java [TRANSFORMED - ProcessBased]
└── server/
    └── namenode/
        └── ha/
            ├── TestHAAppend.java                [ORIGINAL - MiniDFSCluster]
            └── TestHAAppend_ProcessBased.java   [TRANSFORMED - ProcessBased]
```

### Package and Class Declaration

The transformed test uses the **same package** as the original:

```java
// Original: TestRollingUpgrade.java
package org.apache.hadoop.hdfs;

public class TestRollingUpgrade {
  // ... MiniDFSCluster tests
}
```

```java
// Transformed: TestRollingUpgrade_ProcessBased.java
package org.apache.hadoop.hdfs;  // Same package!

/**
 * ProcessBasedMiniDFSCluster version of {@link TestRollingUpgrade}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestRollingUpgrade Original test using MiniDFSCluster
 */
public class TestRollingUpgrade_ProcessBased {
  // ... ProcessBasedMiniDFSCluster tests
}
```

### Test Execution Patterns

```bash
# Run original test only
mvn test -Dtest=TestRollingUpgrade

# Run transformed test only
mvn test -Dtest=TestRollingUpgrade_ProcessBased

# Run ALL ProcessBased tests across the codebase
mvn test -Dtest="*_ProcessBased"

# Run both versions for comparison
mvn test -Dtest=TestRollingUpgrade,TestRollingUpgrade_ProcessBased
```

### Special Case: Example Tests

Tests that serve as **examples or demonstrations** (not real transformations) may be placed in dedicated directories:

```
org.apache.hadoop.hdfs.server.process.upgrade.TestTransformationExample
```

This is acceptable for tutorial/documentation purposes, but **production test transformations** should follow the `_ProcessBased` suffix convention in the same directory.

---

## Core Transformation Rules

### Rule 1: Maximize Test Logic Preservation
**Preserve as much of the original test logic as possible** by finding client-side equivalents for server-side operations.

✅ **DO**: Find client API that provides same functionality
❌ **DON'T**: Remove test logic unless absolutely necessary

### Rule 2: Use the API Hierarchy
Always try to find client-side equivalents in this order:
1. FileSystem/DistributedFileSystem (highest level)
2. DFSClient (mid-level)
3. ClientProtocol (RPC level)
4. DFSAdmin (admin operations)
5. JMX/HTTP (monitoring)

### Rule 3: Comment Out Only When Necessary
Only comment out operations when:
- No client-side API exists
- Operation accesses internal storage (FSImage, edit logs)
- Operation manipulates JVM-internal state

### Rule 4: Document Minimally
Add comments only for:
- Non-obvious transformations
- Commented-out logic (explain why and what was removed)
- Workarounds or limitations

---

## Comprehensive API Mapping Tables

### 4.1 MiniDFSCluster → ProcessBasedMiniDFSCluster

| MiniDFSCluster Method | ProcessBasedMiniDFSCluster | Status | Notes |
|----------------------|---------------------------|--------|-------|
| **Cluster Creation** |
| `new Builder(conf).build()` | `new Builder(conf).build()` | ✓ | Automatically reads hadoop.start.home and hadoop.upgrade.home from system properties |
| **FileSystem Access** |
| `getFileSystem()` | `getFileSystem()` | ✓ | Same API |
| `getFileSystem(int nn)` | `getFileSystem()` | ✓ | For multi-NN, use NN-specific config |
| `getURI()` | `getURI()` | ✓ | Same API |
| `getURI(int nn)` | `getURI(int nn)` | ✓ | Same API |
| **Configuration** |
| `getConfiguration(int nn)` | `getConfiguration(int nn)` | ⚠️ | Returns base config, not process-specific |
| **Node Management** |
| `restartNameNode(int i)` | `restartNameNode(int i)` | ✓ | Same API |
| `restartDataNode(int i)` | `restartDataNode(int i)` | ✓ | Same API |
| `shutdownNameNode(int i)` | `shutdownNameNode(int i)` | ✓ | Same API |
| `shutdownDataNode(int i)` | `shutdownDataNode(int i)` | ✓ | Same API |
| `startDataNode(config, ...)` | `startDataNode(int i)` | ⚠️ | Different signature - restarts existing DN |
| `waitActive()` | `waitClusterUp()` | ✓ | Different name, same purpose |
| `waitClusterUp()` | `waitClusterUp()` | ✓ | Same API |
| **Direct Object Access** |
| `getNameNode()` | ❌ | ✗ | Use client APIs (see below) |
| `getNameNode(int i)` | ❌ | ✗ | Use client APIs (see below) |
| `getDataNode(int i)` | ❌ | ✗ | Use client APIs (see below) |
| `getDataNodes()` | ❌ | ✗ | Use client APIs (see below) |
| `getNamesystem()` | ❌ | ✗ | Use client APIs (see FSNamesystem table) |
| `getNamesystem(int i)` | ❌ | ✗ | Use client APIs (see FSNamesystem table) |
| **Cluster Control** |
| `triggerHeartbeats()` | ⚠️ | ⚠️ | Use `Thread.sleep()` + wait for state change |
| `triggerBlockReports()` | ⚠️ | ⚠️ | Use `Thread.sleep()` + verify via client API |
| `shutdown()` | `shutdown()` | ✓ | Same API |
| **Cluster State** |
| `getNumDataNodes()` | `getNumDataNodes()` | ✓ | Same API |
| `isClusterUp()` | `isClusterUp()` | ✓ | Same API |
| `isDataNodeUp(int i)` | ⚠️ | ⚠️ | Check via `fs.getDataNodeStats()` |

### 4.2 FSNamesystem (Server-Side) → Client-Side APIs

**Context**: `FSNamesystem` is a private, server-side class managing the NameNode's in-memory namespace and block map. All its operations have client-side equivalents through RPC.

| FSNamesystem Method | Client-Side API | API Layer | Code Example |
|--------------------|----------------|-----------|--------------|
| **File Operations** |
| `getFileInfo(src)` | `fs.getFileStatus(path)` | FileSystem | `FileStatus status = fs.getFileStatus(new Path("/foo"));` |
| `mkdirs(src, perm, createParent)` | `fs.mkdirs(path, perm)` | FileSystem | `fs.mkdirs(new Path("/foo"), FsPermission.getDefault());` |
| `delete(src, recursive)` | `fs.delete(path, recursive)` | FileSystem | `fs.delete(new Path("/foo"), true);` |
| `renameTo(src, dst)` | `fs.rename(src, dst)` | FileSystem | `fs.rename(new Path("/foo"), new Path("/bar"));` |
| `create(src, ...)` | `fs.create(path)` | FileSystem | `FSDataOutputStream out = fs.create(new Path("/foo/file.txt"));` |
| `append(src, ...)` | `fs.append(path)` | FileSystem | `FSDataOutputStream out = fs.append(new Path("/foo/file.txt"));` |
| **Block Operations** |
| `getBlockLocations(src, offset, len)` | `fs.getFileBlockLocations(status, offset, len)` | FileSystem | `BlockLocation[] locs = fs.getFileBlockLocations(new Path("/foo"), 0, len);` |
| `getBlockLocations(src, offset, len)` | `client.getBlockLocations(src, offset, len)` | DFSClient | `LocatedBlocks blocks = client.getBlockLocations("/foo", 0, Long.MAX_VALUE);` |
| **Metadata Operations** |
| `getContentSummary(src)` | `fs.getContentSummary(path)` | FileSystem | `ContentSummary cs = fs.getContentSummary(new Path("/"));` |
| `setPermission(src, perm)` | `fs.setPermission(path, perm)` | FileSystem | `fs.setPermission(new Path("/foo"), new FsPermission("755"));` |
| `setOwner(src, username, group)` | `fs.setOwner(path, user, group)` | FileSystem | `fs.setOwner(new Path("/foo"), "hdfs", "supergroup");` |
| `setTimes(src, mtime, atime)` | `fs.setTimes(path, mtime, atime)` | FileSystem | `fs.setTimes(new Path("/foo"), mtime, atime);` |
| `setReplication(src, repl)` | `fs.setReplication(path, repl)` | FileSystem | `fs.setReplication(new Path("/foo"), (short) 3);` |
| **Directory Listing** |
| `getListing(src, startAfter)` | `fs.listStatus(path)` | FileSystem | `FileStatus[] files = fs.listStatus(new Path("/foo"));` |
| `getListing(src, startAfter)` | `client.listPaths(src, startAfter)` | DFSClient | `DirectoryListing listing = client.listPaths("/foo", new byte[0]);` |
| **Namespace Info** |
| `getBlockPoolId()` | `client.getNamespaceInfo().getBlockPoolID()` | DFSClient | `String bpid = dfs.getClient().getNamespaceInfo().getBlockPoolID();` |
| `getNamespaceInfo()` | `client.getNamespaceInfo()` | DFSClient | `NamespaceInfo info = dfs.getClient().getNamespaceInfo();` |
| **Safe Mode** |
| `setSafeMode(action)` | `dfs.setSafeMode(action)` | DistributedFileSystem | `dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);` |
| `isInSafeMode()` | `dfs.setSafeMode(SafeModeAction.SAFEMODE_GET)` | DistributedFileSystem | `boolean safe = dfs.setSafeMode(SafeModeAction.SAFEMODE_GET);` |
| **Statistics** |
| `getStats()` | `dfs.getStatus()` | DistributedFileSystem | `FsStatus status = dfs.getStatus();` |
| `getStats()` | DFSAdmin `-report` | DFSAdmin | `new DFSAdmin(conf).run(new String[]{"-report"});` |
| **Internal Operations** (❌ No client API) |
| `getFSImage()` | ❌ | - | Internal storage - comment out |
| `getBlockManager()` | ❌ | - | Internal state - comment out |
| `getEditLog()` | ❌ | - | Internal logging - comment out |
| `getFSDirectory()` | ❌ | - | Internal namespace - use FileSystem operations instead |
| `getLeaseManager()` | ❌ | - | Internal lease tracking - comment out |

### 4.3 NameNode (Server-Side) → Client-Side APIs

| NameNode Method | Client-Side API | Status | Notes |
|----------------|----------------|--------|-------|
| **RPC Server** |
| `getRpcServer()` | ✓ Returns `ClientProtocol` | ✓ | This IS the client interface! |
| `getRpcServer().getBlockLocations(...)` | `fs.getFileBlockLocations(...)` | ✓ | Use FileSystem wrapper |
| **NameNode State** |
| `getNamesystem()` | See FSNamesystem table | ⚠️ | Map each method individually |
| `isInSafeMode()` | `dfs.setSafeMode(SafeModeAction.SAFEMODE_GET)` | ✓ | Client-side check |
| **Address Info** |
| `getHttpAddress()` | `cluster.getNameNodeRpcAddress()` | ✓ | Available in ProcessBasedMiniDFSCluster |
| `getNameNodeAddress()` | `cluster.getNameNodeRpcAddress()` | ✓ | Available in ProcessBasedMiniDFSCluster |
| **Internal Components** |
| `getFSImage()` | ❌ | ✗ | Internal storage - comment out |
| `getHttpServer()` | ⚠️ | ⚠️ | Use HTTP client to query `http://nn:9870` |
| **Lifecycle** |
| `stop()` | `cluster.shutdownNameNode(i)` | ✓ | Use cluster management |
| `join()` | Implicit in shutdown | ✓ | ProcessBasedMiniDFSCluster handles this |

### 4.4 DataNode (Server-Side) → Client-Side APIs

| DataNode Method | Client-Side API | API Layer | Status | Notes |
|----------------|----------------|-----------|--------|-------|
| **Block Information** |
| Get block locations for file | `fs.getFileBlockLocations(path, 0, len)` | FileSystem | ✓ | Returns block locations including DN info |
| Get block info | `client.getBlockLocations(src, 0, len)` | DFSClient | ✓ | More detailed block information |
| **DataNode Statistics** |
| DataNode stats | `dfs.getDataNodeStats()` | DistributedFileSystem | ✓ | Returns array of DatanodeInfo |
| Cluster status | DFSAdmin `-report` | DFSAdmin | ✓ | Full cluster report including DN status |
| **Internal Operations** (❌ No client API) |
| `getFSDataset()` | ❌ | - | ✗ | Internal dataset - comment out |
| `getStorage()` | ❌ | - | ✗ | Internal storage - comment out |
| `getBlockLocalPathInfo()` | ❌ | - | ✗ | Local file paths not accessible remotely |
| `getBPServiceActors()` | ❌ | - | ✗ | Internal actor management - comment out |
| `getXferAddress()` | From `DatanodeInfo` | ✓ | Get from `dfs.getDataNodeStats()` |

### 4.5 Admin Operations → DFSAdmin

| Operation | DFSAdmin Command | Code Example | Alternative API |
|-----------|------------------|--------------|-----------------|
| **Rolling Upgrade** |
| Start rolling upgrade | `-rollingUpgrade prepare` | `new DFSAdmin(conf).run(new String[]{"-rollingUpgrade", "prepare"});` | `dfs.rollingUpgrade(RollingUpgradeAction.PREPARE)` |
| Finalize upgrade | `-rollingUpgrade finalize` | `new DFSAdmin(conf).run(new String[]{"-rollingUpgrade", "finalize"});` | `dfs.rollingUpgrade(RollingUpgradeAction.FINALIZE)` |
| Query upgrade status | `-rollingUpgrade query` | `new DFSAdmin(conf).run(new String[]{"-rollingUpgrade", "query"});` | `RollingUpgradeInfo info = dfs.rollingUpgrade(RollingUpgradeAction.QUERY)` |
| **Safe Mode** |
| Enter safe mode | `-safemode enter` | `new DFSAdmin(conf).run(new String[]{"-safemode", "enter"});` | `dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER)` |
| Leave safe mode | `-safemode leave` | `new DFSAdmin(conf).run(new String[]{"-safemode", "leave"});` | `dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE)` |
| Get safe mode status | `-safemode get` | `new DFSAdmin(conf).run(new String[]{"-safemode", "get"});` | `boolean inSafe = dfs.setSafeMode(SafeModeAction.SAFEMODE_GET)` |
| **Cluster Status** |
| Cluster report | `-report` | `new DFSAdmin(conf).run(new String[]{"-report"});` | - |
| Refresh nodes | `-refreshNodes` | `new DFSAdmin(conf).run(new String[]{"-refreshNodes"});` | - |
| **Finalize** |
| Finalize upgrade | `-finalizeUpgrade` | `new DFSAdmin(conf).run(new String[]{"-finalizeUpgrade"});` | - |

### 4.6 Monitoring/Metrics → JMX/Client APIs

| Server-Side Check | Client-Side Alternative | Access Method | Example |
|------------------|------------------------|---------------|---------|
| **Block Manager Stats** |
| Block count | `dfs.getStatus()` | DistributedFileSystem | `FsStatus status = dfs.getStatus(); long blocks = status.getUsed() / blockSize;` |
| Block stats | JMX query | HTTP/JMX | `curl http://nn:9870/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem` |
| **Namespace Stats** |
| File/directory count | `fs.getContentSummary("/")` | FileSystem | `ContentSummary cs = fs.getContentSummary(new Path("/")); long files = cs.getFileCount();` |
| Disk usage | `fs.getStatus()` | FileSystem | `FsStatus status = fs.getStatus(); long used = status.getUsed();` |
| **Safe Mode Status** |
| In safe mode | `dfs.setSafeMode(SafeModeAction.SAFEMODE_GET)` | DistributedFileSystem | `boolean safe = dfs.setSafeMode(SafeModeAction.SAFEMODE_GET);` |
| **Rolling Upgrade Status** |
| Upgrade status | `dfs.rollingUpgrade(RollingUpgradeAction.QUERY)` | DistributedFileSystem | `RollingUpgradeInfo info = dfs.rollingUpgrade(RollingUpgradeAction.QUERY);` |
| **DataNode Status** |
| DataNode info | `dfs.getDataNodeStats()` | DistributedFileSystem | `DatanodeInfo[] dns = dfs.getDataNodeStats(); for (DatanodeInfo dn : dns) { ... }` |

### 4.7 Common Test Utilities

| MiniDFSCluster Utility | ProcessBasedMiniDFSCluster Alternative | Notes |
|----------------------|---------------------------------------|-------|
| `cluster.triggerHeartbeats()` | `Thread.sleep(5000)` + verify state | Wait for natural heartbeat cycle |
| `cluster.triggerBlockReports()` | `Thread.sleep(10000)` + verify blocks | Wait for natural block report |
| `DataNodeTestUtils.triggerHeartbeat(dn)` | `Thread.sleep(3000)` | Cannot trigger individual DN heartbeat |
| `NameNodeAdapter.enterSafeMode(nn, false)` | `dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER)` | Use client API |
| `NameNodeAdapter.leaveSafeMode(nn)` | `dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE)` | Use client API |

---

## Step-by-Step Transformation Process

### Step 0: Create Transformed Test File

**Goal**: Set up the new test file with proper naming and location.

1. **Locate the original test:**
   ```bash
   # Example: Original test
   hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestRollingUpgrade.java
   ```

2. **Create new file with `_ProcessBased` suffix in the SAME directory:**
   ```bash
   # New transformed test (same directory!)
   hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestRollingUpgrade_ProcessBased.java
   ```

3. **Copy original test content to new file:**
   ```bash
   cp TestRollingUpgrade.java TestRollingUpgrade_ProcessBased.java
   ```

4. **Update class name and add Javadoc:**
   ```java
   package org.apache.hadoop.hdfs;  // Same package as original!

   /**
    * ProcessBasedMiniDFSCluster version of {@link TestRollingUpgrade}.
    *
    * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
    * process-based testing and multi-version upgrade scenarios.
    *
    * @see TestRollingUpgrade Original test using MiniDFSCluster
    */
   public class TestRollingUpgrade_ProcessBased {  // Note: _ProcessBased suffix
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
   # Search for common patterns
   grep -E "cluster\.(getNameNode|getDataNode|getNamesystem)" YourTest.java
   grep -E "\.getFSImage\(\)|\.getBlockManager\(\)|\.getFSDataset\(\)" YourTest.java
   ```

2. **Categorize operations:**
   - ✅ **Already client-side**: FileSystem operations, DFSAdmin calls
   - ⚠️ **Has client equivalent**: FSNamesystem methods, some NameNode methods
   - ❌ **No client equivalent**: FSImage, internal storage, JVM state

3. **Plan transformation:**
   - List all operations that need transformation
   - Find client equivalents in mapping tables
   - Identify operations that must be commented out

### Step 2: Transform Import Statements

```java
// BEFORE
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.datanode.DataNode;

// AFTER
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
// Remove: import org.apache.hadoop.hdfs.MiniDFSCluster;
// Keep NameNode/DataNode imports only if used in type declarations
// that can't be removed (rare)
```

### Step 3: Transform Cluster Setup

#### Basic Cluster Creation

```java
// BEFORE (MiniDFSCluster)
Configuration conf = new HdfsConfiguration();
MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
    .numDataNodes(3)
    .build();
cluster.waitActive();

// AFTER (ProcessBasedMiniDFSCluster) - AUTOMATIC!
Configuration conf = new HdfsConfiguration();
// No environment variable checks needed! Automatic!

ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();  // Automatically reads hadoop.start.home and hadoop.upgrade.home!
cluster.waitClusterUp();
```

**Note:** System properties are passed via Maven:
```bash
mvn test -Dtest=MyTest \
  -Dhadoop.start.home=/path/to/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/path/to/hadoop-3.3.6
```

#### Multi-Version Cluster (for upgrade tests)

```java
// All ProcessBased tests support upgrades automatically!
// Just build the cluster - it reads system properties automatically

ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();  // Starts with hadoop.start.home

// Later, perform rolling upgrade to hadoop.upgrade.home
String upgradeHome = cluster.getUpgradeDistributionPath();
for (int i = 0; i < 3; i++) {
    cluster.shutdownDataNode(i);
    cluster.changeDataNodeVersion(i, upgradeHome);
    cluster.startDataNode(i);
}
```

### Step 4: Transform Operations Using Mapping Tables

#### Example 1: File Operations (No Change)
```java
// These work identically - no transformation needed
DistributedFileSystem fs = cluster.getFileSystem();
fs.mkdirs(new Path("/test"));
Path file = new Path("/test/file.txt");
FSDataOutputStream out = fs.create(file);
out.write("Hello".getBytes());
out.close();
assertTrue(fs.exists(file));
```

#### Example 2: FSNamesystem Access → Client API
```java
// BEFORE: Direct FSNamesystem access
FSNamesystem fsn = cluster.getNamesystem();
String blockPoolId = fsn.getBlockPoolId();
ContentSummary summary = fsn.getContentSummary("/");

// AFTER: Client-side equivalents
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
String blockPoolId = dfs.getClient().getNamespaceInfo().getBlockPoolID();
ContentSummary summary = dfs.getContentSummary(new Path("/"));
```

#### Example 3: NameNode State → Client Query
```java
// BEFORE: Direct NameNode access
NameNode nn = cluster.getNameNode();
boolean inSafeMode = nn.isInSafeMode();

// AFTER: Client-side check
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
boolean inSafeMode = dfs.setSafeMode(SafeModeAction.SAFEMODE_GET);
```

#### Example 4: Rolling Upgrade Operations
```java
// BEFORE: Using DFSAdmin (already client-side, no change needed)
DFSAdmin admin = new DFSAdmin(conf);
assertEquals(0, admin.run(new String[]{"-rollingUpgrade", "prepare"}));

// AFTER: Same, or use DistributedFileSystem API
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
RollingUpgradeInfo info = dfs.rollingUpgrade(RollingUpgradeAction.PREPARE);
assertNotNull(info);
assertTrue(info.isStarted());
```

#### Example 5: DataNode Restart
```java
// BEFORE: Complex restart with properties
MiniDFSCluster.DataNodeProperties dnProp = cluster.stopDataNode(0);
dnProp.setDnArgs("-rollback");
cluster.restartDataNode(dnProp, true);

// AFTER: Simpler, but may need to set args differently
cluster.shutdownDataNode(0);
// Note: -rollback args handling may differ - document if behavior changes
cluster.startDataNode(0);
```

#### Example 6: Internal Storage Check → Comment Out
```java
// BEFORE: Internal storage verification
NNStorage storage = cluster.getNamesystem().getFSImage().getStorage();
checkNNStorage(storage, 3, -1);

// AFTER: Comment out with documentation
// TRANSFORMATION NOTE: Internal NameNode storage verification removed.
// FSImage.getStorage() is not accessible via client APIs as it's internal
// to the NameNode process. The original test verified edit log segments
// and FSImage files, which requires direct filesystem access to the NN's
// storage directories.
//
// Original code:
// NNStorage storage = cluster.getNamesystem().getFSImage().getStorage();
// checkNNStorage(storage, 3, -1);
```

### Step 5: Transform Cleanup Code

```java
// BEFORE
@After
public void tearDown() {
    if (cluster != null) {
        cluster.shutdown();
    }
}

// AFTER (same, but can use try-with-resources)
@After
public void tearDown() {
    if (cluster != null) {
        cluster.shutdown();
    }
}

// OR use try-with-resources in the test method:
@Test
public void testSomething() throws Exception {
    try (ProcessBasedMiniDFSCluster cluster =
            new ProcessBasedMiniDFSCluster.Builder(conf)
                .numDataNodes(3)
                .build()) {  // Automatic - reads system properties!
        // Test logic here
    }
}
```

### Step 6: Validate Transformation

Run through this checklist:

- [ ] All imports updated
- [ ] All direct object access either transformed or commented out
- [ ] Cluster creation includes Hadoop distribution path
- [ ] Test logic preserved as much as possible
- [ ] Only truly internal operations commented out
- [ ] Comments added for non-obvious transformations
- [ ] Test compiles without errors
- [ ] Environment variables documented in test javadoc

---

## Common Transformation Patterns

### Pattern 1: FileSystem Operations
**Status**: ✅ No transformation needed

```java
// Works identically in both frameworks
DistributedFileSystem fs = cluster.getFileSystem();
fs.mkdirs(new Path("/dir"));
fs.create(new Path("/dir/file"));
fs.delete(new Path("/dir"), true);
fs.listStatus(new Path("/"));
```

### Pattern 2: Safe Mode Operations
```java
// BEFORE: Multiple ways to enter/exit safe mode
cluster.getNameNode().getNamesystem().setSafeMode(SafeModeAction.SAFEMODE_ENTER);
// OR
NameNodeAdapter.enterSafeMode(cluster.getNameNode(), false);

// AFTER: Use client API (works in both frameworks)
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
boolean inSafe = dfs.setSafeMode(SafeModeAction.SAFEMODE_GET);
```

### Pattern 3: Rolling Upgrade Workflow
```java
// Standard rolling upgrade pattern - works with both frameworks

// 1. Enter safe mode
dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);

// 2. Prepare rolling upgrade
RollingUpgradeInfo info = dfs.rollingUpgrade(RollingUpgradeAction.PREPARE);
assertTrue(info.isStarted());

// 3. Leave safe mode
dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);

// 4. Upgrade DataNodes one by one
for (int i = 0; i < 3; i++) {
    cluster.restartDataNode(i);
    // Verify cluster still functional
    assertTrue(fs.exists(testFile));
}

// 5. Finalize upgrade
info = dfs.rollingUpgrade(RollingUpgradeAction.FINALIZE);
```

### Pattern 4: Waiting for State Changes
```java
// BEFORE: Trigger heartbeats to speed up state propagation
cluster.triggerHeartbeats();
cluster.triggerBlockReports();

// AFTER: Wait for natural propagation
Thread.sleep(5000);  // Wait for heartbeat interval
// Or use GenericTestUtils.waitFor()
GenericTestUtils.waitFor(() -> {
    try {
        // Check desired state via client API
        return dfs.getDataNodeStats().length == expectedCount;
    } catch (Exception e) {
        return false;
    }
}, 500, 30000);
```

### Pattern 5: Block Location Queries
```java
// BEFORE: Via NameNode RPC server
NameNode nn = cluster.getNameNode();
LocatedBlocks blocks = nn.getRpcServer().getBlockLocations(
    "/test/file", 0, Long.MAX_VALUE);

// AFTER: Via FileSystem or DFSClient
// Option 1: FileSystem (recommended)
FileStatus status = fs.getFileStatus(new Path("/test/file"));
BlockLocation[] locations = fs.getFileBlockLocations(status, 0, status.getLen());

// Option 2: DFSClient (more detailed)
DFSClient client = ((DistributedFileSystem) fs).getClient();
LocatedBlocks blocks = client.getBlockLocations("/test/file", 0, Long.MAX_VALUE);
```

### Pattern 6: DataNode Information
```java
// BEFORE: Direct DataNode access
List<DataNode> dataNodes = cluster.getDataNodes();
DataNode dn0 = dataNodes.get(0);
String dnInfo = dn0.getDisplayName();

// AFTER: Via cluster statistics
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
DatanodeInfo[] datanodes = dfs.getDataNodeStats();
assertEquals(3, datanodes.length);
// Can access DatanodeInfo properties: getHostName(), getXferAddr(), etc.
```

### Pattern 7: Namespace Info
```java
// BEFORE: Via FSNamesystem
FSNamesystem fsn = cluster.getNamesystem();
String bpid = fsn.getBlockPoolId();

// AFTER: Via DFSClient
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
NamespaceInfo nsInfo = dfs.getClient().getNamespaceInfo();
String bpid = nsInfo.getBlockPoolID();
String clusterId = nsInfo.getClusterID();
```

### Pattern 8: Content Summary / Statistics
```java
// BEFORE: Via FSNamesystem
FSNamesystem fsn = cluster.getNamesystem();
ContentSummary summary = fsn.getContentSummary("/");

// AFTER: Via FileSystem (identical API)
ContentSummary summary = fs.getContentSummary(new Path("/"));
long fileCount = summary.getFileCount();
long dirCount = summary.getDirectoryCount();
long totalBytes = summary.getLength();
```

---

## Inserting Cluster Upgrade Method Calls

### Overview

When transforming tests to support rolling upgrades, you need to insert `cluster.upgrade()` method calls at appropriate points in the test. This section explains how to identify upgrade points and handle the critical pattern of **closing streams before upgrade and reopening them afterward**.

### Why Stream Management is Critical

During a rolling upgrade, HDFS nodes (DataNodes and NameNodes) are restarted with new software versions. This restart **breaks the write pipeline** - the active connection between the client and the DataNode that's handling write operations.

**Key principle**: Any `FSDataOutputStream` (or similar stream) that spans an upgrade point must be:
1. **Closed** before calling `cluster.upgrade()`
2. **Reopened** (typically in append mode) after `cluster.upgrade()` completes

### Identifying Upgrade Points

An upgrade point is a logical location in your test where you want to simulate a rolling upgrade. Common upgrade points include:

1. **Mid-write operations** - Testing that data written before upgrade is accessible after upgrade
2. **Between distinct test phases** - After setup operations but before verification
3. **After creating test data** - Testing upgrade with existing data
4. **During long-running operations** - Testing upgrade resilience

### Step-by-Step: Inserting Upgrade Calls

#### Step 1: Identify the Upgrade Point

Look for a logical point in the test where upgrade makes sense:

```java
// BEFORE: Original test without upgrade
Path file = new Path("/test/file.txt");
FSDataOutputStream out = fs.create(file);
out.write(data, 0, 100);      // Write first part
out.hflush();
out.write(data, 100, 100);    // Write second part  <-- Potential upgrade point
out.hflush();
out.close();
```

#### Step 2: Close Streams Before Upgrade

If a stream is open at the upgrade point, close it first:

```java
// AFTER: With upgrade point inserted
Path file = new Path("/test/file.txt");
FSDataOutputStream out = fs.create(file);
out.write(data, 0, 100);      // Write first part
out.hflush();

// === ROLLING UPGRADE POINT ===
// CRITICAL: Close the stream before upgrade since the DataNode will be restarted
// and the write pipeline will be broken
out.close();
System.out.println("Closed stream before rolling upgrade");
```

#### Step 3: Call cluster.upgrade()

```java
// Perform rolling upgrade
cluster.upgrade();  // Executes full rolling upgrade procedure
System.out.println("Rolling upgrade completed successfully");
```

**Note**: The `cluster.upgrade()` method:
- Automatically follows the official HDFS rolling upgrade procedure
- Enters safe mode, prepares upgrade, upgrades nodes, stabilizes cluster, and finalizes
- **Automatically waits 5 seconds** after cluster stabilization for blocks to be reported
- This automatic waiting ensures existing files can be reopened for append

#### Step 4: Reopen Streams After Upgrade

If you need to continue writing to the file, reopen it in append mode:

```java
// Reopen the file in append mode after upgrade
out = fs.append(file);
System.out.println("Reopened file in append mode after upgrade");

// Continue writing
out.write(data, 100, 100);    // Write second part
out.hflush();
out.close();
```

### Complete Example: TestFileAppend_ProcessBased

Here's the complete pattern from `TestFileAppend_ProcessBased.java`:

```java
@Test
public void testSimpleFlush() throws Exception {
    Configuration conf = new HdfsConfiguration();
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    DistributedFileSystem fs = cluster.getFileSystem();

    try {
        cluster.waitClusterUp();

        // Create file and write first part
        Path file1 = new Path("/simpleFlush.dat");
        FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
        System.out.println("Created file simpleFlush.dat");

        int mid = AppendTestUtil.FILE_SIZE / 2;
        stm.write(fileContents, 0, mid);
        stm.hflush();
        System.out.println("Wrote and Flushed first part of file.");

        // === ROLLING UPGRADE POINT ===
        // STEP 1: Close the stream before upgrade
        stm.close();
        System.out.println("Closed stream before rolling upgrade");

        // STEP 2: Perform rolling upgrade
        cluster.upgrade();  // Automatic wait for blocks included!
        System.out.println("Rolling upgrade completed successfully");

        // STEP 3: Reopen in append mode
        stm = fs.append(file1);
        System.out.println("Reopened file in append mode after upgrade");

        // Continue writing after upgrade
        stm.write(fileContents, mid, AppendTestUtil.FILE_SIZE - mid);
        System.out.println("Written second part of file");
        stm.hflush();
        stm.hflush();
        System.out.println("Wrote and Flushed second part of file.");

        // Verify that full blocks are sane
        checkFile(fs, file1, 1);

        stm.close();
        System.out.println("Closed file.");

        // Verify that entire file is good
        AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
            fileContents, "Read 2");

    } finally {
        fs.close();
        cluster.shutdown();
    }
}
```

### Automatic Block Replication Waiting

The `cluster.upgrade()` method includes an **automatic 5-second grace period** after cluster stabilization. This ensures:

- DataNodes have time to report their blocks to the NameNode
- Existing files can be immediately reopened for append operations
- Tests don't need manual waiting logic for block replication

**You do NOT need to add manual waiting code** like this:

```java
// ❌ NOT NEEDED - cluster.upgrade() handles this automatically!
Thread.sleep(5000);
GenericTestUtils.waitFor(() -> checkBlocksReplicated(), 1000, 30000);
```

The automatic waiting is built into `cluster.upgrade()` at `/Users/allenwang/xlab/hadoop-transform/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/process/ProcessBasedMiniDFSCluster.java:1630-1641`.

### Pattern Variations

#### Variation 1: Read-only operations (no stream management needed)

If your test only reads data, no stream management is required:

```java
// Create test files
fs.create(new Path("/file1")).close();
fs.create(new Path("/file2")).close();

// Upgrade - no stream management needed
cluster.upgrade();

// Verify files still readable
assertTrue(fs.exists(new Path("/file1")));
assertTrue(fs.exists(new Path("/file2")));
```

#### Variation 2: Multiple streams

If multiple streams are open, close all of them:

```java
FSDataOutputStream out1 = fs.create(new Path("/file1"));
FSDataOutputStream out2 = fs.create(new Path("/file2"));

// Write to both
out1.write(data1);
out2.write(data2);
out1.hflush();
out2.hflush();

// Close all streams before upgrade
out1.close();
out2.close();
System.out.println("Closed all streams before upgrade");

// Perform upgrade
cluster.upgrade();

// Reopen as needed
out1 = fs.append(new Path("/file1"));
out2 = fs.append(new Path("/file2"));
```

#### Variation 3: Input streams

Input streams (readers) should also be closed and reopened:

```java
FSDataInputStream in = fs.open(new Path("/file"));
byte[] buffer = new byte[1000];
in.read(buffer);

// Close input stream before upgrade
in.close();
System.out.println("Closed input stream before upgrade");

// Perform upgrade
cluster.upgrade();

// Reopen at same position
in = fs.open(new Path("/file"));
in.seek(1000);  // Resume reading from where we left off
```

### Common Mistakes to Avoid

#### ❌ Mistake 1: Not closing streams before upgrade

```java
// WRONG - Stream remains open during upgrade
FSDataOutputStream out = fs.create(file);
out.write(data, 0, 100);
cluster.upgrade();  // Pipeline will break!
out.write(data, 100, 100);  // This will fail!
```

#### ❌ Mistake 2: Forgetting to reopen for continued writes

```java
// WRONG - Stream closed but not reopened
FSDataOutputStream out = fs.create(file);
out.write(data, 0, 100);
out.close();
cluster.upgrade();
// Missing: out = fs.append(file);
out.write(data, 100, 100);  // NullPointerException or wrong stream!
```

#### ❌ Mistake 3: Using create() instead of append() after upgrade

```java
// WRONG - create() will overwrite existing data
out.close();
cluster.upgrade();
out = fs.create(file);  // WRONG - overwrites the file!
out.write(data, 100, 100);
```

#### ✅ Correct Pattern

```java
// CORRECT - Close, upgrade, reopen in append mode
FSDataOutputStream out = fs.create(file);
out.write(data, 0, 100);
out.hflush();
out.close();

cluster.upgrade();

out = fs.append(file);  // CORRECT - append mode preserves existing data
out.write(data, 100, 100);
out.close();
```

### Testing Your Upgrade Point

After inserting the upgrade call, verify:

1. **Data before upgrade is preserved** - Read and verify data written before upgrade
2. **Data after upgrade is correct** - Read and verify data written after upgrade
3. **Combined data is valid** - Verify the complete file has all data in correct order
4. **No exceptions during upgrade** - Upgrade completes without errors
5. **Cluster is healthy after upgrade** - All nodes are operational

### Summary Checklist

When inserting `cluster.upgrade()` calls:

- [ ] Identify logical upgrade point in test
- [ ] Close all open `FSDataOutputStream` instances before `cluster.upgrade()`
- [ ] Close all open `FSDataInputStream` instances before `cluster.upgrade()`
- [ ] Call `cluster.upgrade()` (automatic block replication wait included)
- [ ] Reopen streams in **append mode** (not create mode) if continuing to write
- [ ] Add informative `System.out.println()` messages for debugging
- [ ] Verify data integrity before and after upgrade
- [ ] Test passes with upgrade point inserted

---

## Parameterized Upgrade Checkpoints

### Overview

**New Approach (Recommended)**: Instead of hardcoding a single upgrade point in each test, use JUnit parameterization to run each test multiple times with upgrades at different checkpoints. This provides comprehensive upgrade coverage with reproducible results.

**Key Benefits**:
- Single test → multiple upgrade scenarios automatically
- 100% reproducible (deterministic checkpoint execution)
- Comprehensive coverage (10+ checkpoints per test)
- Guaranteed cleanup between executions
- Easy to identify which checkpoint caused failure

### Architecture

```
Test Method (testSimpleFlush)
├── Execution 1: checkpoint=NO_UPGRADE
│   ├── @Before: cleanup, init
│   ├── Test runs (no upgrade)
│   └── @After: cleanup verified
│
├── Execution 2: checkpoint=AFTER_CREATE
│   ├── @Before: cleanup, init
│   ├── Test runs, upgrades at AFTER_CREATE
│   └── @After: cleanup verified
│
├── Execution 3: checkpoint=AFTER_WRITE
│   ├── @Before: cleanup, init
│   ├── Test runs, upgrades at AFTER_WRITE
│   └── @After: cleanup verified
│
... (10+ more checkpoint executions)
```

### Base Class: ProcessBasedUpgradeTestBase

All ProcessBased tests should extend `ProcessBasedUpgradeTestBase`, which provides:

1. **@Before cleanup**: Kills orphaned processes, cleans old directories
2. **@After cleanup**: Closes fs, shuts down cluster, verifies cleanup
3. **checkpoint(name)**: Performs upgrade if name matches parameter
4. **shouldUpgrade(name)**: Checks if upgrade should happen

**Location**: `org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase`

### Transformation Steps

#### Step 1: Add Parameterization Framework

```java
// Add imports
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import java.util.Arrays;
import java.util.Collection;

// Add annotation and extend base class
@RunWith(Parameterized.class)
public class TestFileAppend_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,           // Always include baseline
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_FIRST_WRITE,
      UpgradeCheckpoints.AFTER_FIRST_FLUSH,
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
  Configuration conf = new HdfsConfiguration();
  ProcessBasedMiniDFSCluster cluster = new Builder(conf).build();
  DistributedFileSystem fs = cluster.getFileSystem();

  try {
    // test logic
  } finally {
    fs.close();
    cluster.shutdown();
  }
}
```

**AFTER** (automatic cleanup via base class):
```java
@Test
public void testSomething() throws Exception {
  // Use conf, cluster, fs from base class
  cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
  fs = cluster.getFileSystem();

  // test logic with checkpoints
  fs.create(file).close();
  checkpoint(UpgradeCheckpoints.AFTER_CREATE);

  // No try-finally needed - @After handles cleanup!
}
```

#### Step 3: Replace Hardcoded cluster.upgrade() with checkpoint()

**BEFORE** (hardcoded upgrade point):
```java
stm.write(data, 0, mid);
stm.hflush();

// === ROLLING UPGRADE POINT ===
stm.close();
cluster.upgrade();
stm = fs.append(file);

stm.write(data, mid, size - mid);
```

**AFTER** (parameterized checkpoints):
```java
stm.write(data, 0, mid);
checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

stm.hflush();
checkpoint(UpgradeCheckpoints.AFTER_FIRST_FLUSH);

// Close before potential upgrade
stm.close();
checkpoint("AFTER_FIRST_CLOSE");

// Reopen (always needed, regardless of upgrade)
stm = fs.append(file);
checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

stm.write(data, mid, size - mid);
checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);
```

### Checkpoint Naming Guidelines

1. **Always include NO_UPGRADE first**: Ensures test passes without upgrade
2. **Use UpgradeCheckpoints constants**: For common checkpoint names
3. **Use custom strings**: For test-specific checkpoints
4. **Be descriptive**: "AFTER_BALANCER_RUN" not "CHECKPOINT_7"
5. **Fine-grained coverage**: 10-15 checkpoints per test method

**Common checkpoint categories**:
- Cluster lifecycle: `AFTER_CLUSTER_START`
- File operations: `AFTER_FILE_CREATE`, `AFTER_FILE_DELETE`
- Write operations: `AFTER_FIRST_WRITE`, `AFTER_SECOND_WRITE`
- Flush operations: `AFTER_FIRST_FLUSH`, `AFTER_SECOND_FLUSH`
- Stream lifecycle: `AFTER_FIRST_CLOSE`, `AFTER_APPEND_REOPEN`
- Verification: `BEFORE_VERIFICATION`, `AFTER_VERIFICATION`

### Stream Management with Checkpoints

**Critical Rule**: Always close streams before checkpoints, reopen after if needed.

**Pattern 1: Write → Upgrade → Continue Writing**
```java
FSDataOutputStream out = fs.create(file);
out.write(data, 0, 100);
checkpoint("AFTER_FIRST_WRITE");

// Close before potential upgrade
out.close();
checkpoint("AFTER_FIRST_CLOSE");

// Reopen if continuing to write
out = fs.append(file);
out.write(data, 100, 100);
checkpoint("AFTER_SECOND_WRITE");

out.close();
```

**Pattern 2: Read → Upgrade → Continue Reading**
```java
FSDataInputStream in = fs.open(file);
byte[] buf1 = new byte[100];
in.read(buf1);
checkpoint("AFTER_FIRST_READ");

// Close before potential upgrade
long position = in.getPos();
in.close();
checkpoint("AFTER_READ_CLOSE");

// Reopen and seek to continue
in = fs.open(file);
in.seek(position);
byte[] buf2 = new byte[100];
in.read(buf2);
checkpoint("AFTER_SECOND_READ");

in.close();
```

### Test Isolation and Cleanup

**JUnit Parameterized Lifecycle** (per checkpoint execution):

```
1. Create new test instance
2. @Before (ProcessBasedUpgradeTestBase.setupTest())
   - Kill orphaned processes
   - Clean old directories
   - Initialize configuration
3. @Test method runs
   - Creates cluster
   - Runs test logic
   - Calls checkpoint() throughout
4. @After (ProcessBasedUpgradeTestBase.tearDownTest())
   - Close FileSystem
   - Shutdown cluster (delete directories)
   - Wait for processes to die
   - Verify no orphaned processes
   - Force kill if verification fails
5. Destroy test instance

(Repeat for next checkpoint)
```

**Cleanup Guarantee**: Each checkpoint execution is completely isolated with:
- ✅ Pre-cleanup: Defensive process killing before test
- ✅ Post-cleanup: Guaranteed shutdown in @After
- ✅ Verification: Asserts no orphaned processes
- ✅ Force cleanup: Kills orphans if verification fails
- ✅ Directory cleanup: Deletes cluster directories

### Complete Transformation Example

**Original test**:
```java
public class TestFileAppend_ProcessBased {
  @Test
  public void testSimpleFlush() throws Exception {
    Configuration conf = new HdfsConfiguration();
    ProcessBasedMiniDFSCluster cluster = new Builder(conf).build();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      fs.create(file).write(data);
      cluster.upgrade();  // Single hardcoded upgrade point
      fs.append(file).write(moreData);
    } finally {
      fs.close();
      cluster.shutdown();
    }
  }
}
```

**Transformed with parameterization**:
```java
@RunWith(Parameterized.class)
public class TestFileAppend_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_FIRST_WRITE,
      "AFTER_FIRST_CLOSE",
      UpgradeCheckpoints.AFTER_APPEND_REOPEN,
      UpgradeCheckpoints.AFTER_SECOND_WRITE
    );
  }

  @Test
  public void testSimpleFlush() throws Exception {
    // Use conf from base class
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FSDataOutputStream out = fs.create(file);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    out.write(data);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    out = fs.append(file);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    out.write(moreData);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();

    // No try-finally - cleanup automatic!
  }
}
```

**Result**:
- Before: 1 test execution, 1 upgrade point
- After: 7 test executions, comprehensive upgrade coverage

### Handling Test Failures

When a test fails at a specific checkpoint:

**Failure message shows checkpoint**:
```
testSimpleFlush[upgrade-at=AFTER_FIRST_WRITE]  FAILED
```

**Debugging strategy**:
1. Identify which checkpoint caused failure
2. Run just that checkpoint: `-Dtest=TestClass#testMethod[upgrade-at=AFTER_FIRST_WRITE]`
3. Check if upgrade at that point is unsafe
4. Decide: Fix code or mark checkpoint as unsafe

**Options for unsafe checkpoints**:
1. Remove from @Parameters list
2. Add conditional skip in checkpoint() method
3. Document as known limitation

### Common Pitfalls

❌ **Pitfall 1**: Forgetting to close stream before checkpoint
```java
// WRONG - stream open during upgrade
out.write(data);
checkpoint("AFTER_WRITE");  // Pipeline breaks if upgrade happens!
out.write(moreData);        // This will fail
```

✅ **Correct**:
```java
out.write(data);
out.close();
checkpoint("AFTER_WRITE");
out = fs.append(file);
out.write(moreData);
```

❌ **Pitfall 2**: Not including NO_UPGRADE baseline
```java
// WRONG - no baseline test
@Parameters
public static Collection<String> checkpoints() {
  return Arrays.asList(
    "AFTER_CREATE",  // Missing NO_UPGRADE!
    "AFTER_WRITE"
  );
}
```

✅ **Correct**:
```java
@Parameters
public static Collection<String> checkpoints() {
  return Arrays.asList(
    UpgradeCheckpoints.NO_UPGRADE,  // Always first!
    "AFTER_CREATE",
    "AFTER_WRITE"
  );
}
```

❌ **Pitfall 3**: Reusing stream variable without closing
```java
// WRONG - overwrites stream reference without closing
FSDataOutputStream out = fs.create(file);
out.write(data);
checkpoint("AFTER_WRITE");
out = fs.append(file);  // Leaked previous stream!
```

✅ **Correct**:
```java
FSDataOutputStream out = fs.create(file);
out.write(data);
out.close();  // Explicit close
checkpoint("AFTER_WRITE");
out = fs.append(file);
```

### Testing Parameterized Tests

**Run all checkpoints**:
```bash
mvn test -Dtest=TestFileAppend_ProcessBased \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6
```

**Run specific checkpoint**:
```bash
mvn test -Dtest='TestFileAppend_ProcessBased#testSimpleFlush[upgrade-at=AFTER_CREATE]' \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6
```

**Expected output** (for test with 13 checkpoints):
```
TestFileAppend_ProcessBased.testSimpleFlush[upgrade-at=NO_UPGRADE]  ✓
TestFileAppend_ProcessBased.testSimpleFlush[upgrade-at=AFTER_CLUSTER_START]  ✓
TestFileAppend_ProcessBased.testSimpleFlush[upgrade-at=AFTER_FILE_CREATE]  ✓
... (10 more)
```

### Transformation Checklist

When converting a test to use parameterized checkpoints:

- [ ] Extend ProcessBasedUpgradeTestBase
- [ ] Add @RunWith(Parameterized.class) annotation
- [ ] Add @Parameter field for upgradeCheckpoint
- [ ] Add @Parameters method with checkpoints list
- [ ] Include NO_UPGRADE as first checkpoint
- [ ] Remove all try-finally blocks around cluster/fs
- [ ] Replace hardcoded cluster.upgrade() with checkpoint() calls
- [ ] Insert 10+ checkpoint() calls throughout test
- [ ] Close streams before each checkpoint
- [ ] Reopen streams after checkpoints if needed
- [ ] Verify test compiles
- [ ] Run test and verify all checkpoints execute
- [ ] Check cleanup verification passes

---

## When to Comment Out Logic

### Only comment out operations that are:

1. **Internal Storage Operations**
   - FSImage access
   - Edit log inspection
   - Storage directory verification
   - Checkpoint file checks

2. **In-Process Manipulation**
   - Direct object field modification
   - Mock object injection
   - Reflection on private fields

3. **JVM-Level Operations**
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

### Example: Internal Storage Check

```java
// TRANSFORMATION NOTE: Internal NameNode storage structure verification removed.
// The FSImage and NNStorage classes are internal to the NameNode process and not
// accessible via any client API. The original test verified the presence of specific
// edit log segments and fsimage files in the NameNode's storage directories.
// No client-side alternative available - this verification requires filesystem access
// to the NameNode's data directories.
//
// Original code:
// NNStorage storage = cluster.getNamesystem().getFSImage().getStorage();
// List<File> finalizedEdits = storage.getFiles(
//     NNStorage.NameNodeDirType.EDITS,
//     NNStorage.getFinalizedEditsFileName(1, imageTxId));
// Assert.assertTrue(fileExists(finalizedEdits));
```

### Example: DataNode Internal Check

```java
// TRANSFORMATION NOTE: DataNode FSDataset access removed.
// getFSDataset() provides access to the DataNode's internal block storage,
// which is not available via any client RPC protocol. The original test
// verified block trash functionality during rolling upgrades.
// Consider alternative: Verify block availability via fs.exists() on files
// instead of checking internal block storage state.
//
// Original code:
// DataNode dn = cluster.getDataNodes().get(0);
// assertTrue(dn.getFSDataset().trashEnabled(blockPoolId));
```

### When NOT to Comment Out

Do NOT comment out if there's a client-side equivalent:

❌ **WRONG**:
```java
// TRANSFORMATION NOTE: Cannot access NameNode directly
// Original code:
// boolean inSafe = cluster.getNameNode().isInSafeMode();
```

✅ **CORRECT**:
```java
// Use client API instead of direct NameNode access
DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
boolean inSafe = dfs.setSafeMode(SafeModeAction.SAFEMODE_GET);
```

---

## Testing Checklist

### Before Running Test

- [ ] **File organization correct**
  - Transformed test in same directory as original
  - File name has `_ProcessBased` suffix (e.g., `TestRollingUpgrade_ProcessBased.java`)
  - Package declaration identical to original
  - Class name matches file name with `_ProcessBased` suffix
  - Javadoc includes `@see` reference to original test

- [ ] **System properties ready** (No manual setup needed!)
  ```bash
  # System properties will be passed when running tests:
  mvn test -Dtest=MyTest \
    -Dhadoop.start.home=/path/to/hadoop-3.3.5 \
    -Dhadoop.upgrade.home=/path/to/hadoop-3.3.6
  ```

  Note: Environment variables (`HADOOP_HOME`, `HADOOP_UPGRADE_HOME`) still work as fallback.

- [ ] **Test compiles without errors**
  ```bash
  mvn test-compile -pl hadoop-hdfs-project/hadoop-hdfs
  ```

- [ ] **Imports are correct**
  - ProcessBasedMiniDFSCluster imported
  - Unnecessary NameNode/DataNode imports removed
  - Client API imports added (DFSClient, DistributedFileSystem, etc.)

### During Test Execution

- [ ] **Cluster starts successfully**
  - Check logs for "Cluster started successfully"
  - Verify all nodes are up

- [ ] **FileSystem accessible**
  - Can get FileSystem instance
  - Can perform basic operations

- [ ] **Core assertions pass**
  - Main test logic validates correctly
  - Data integrity checks pass

### After Test Execution

- [ ] **Test passes (or fails as expected)**
  - If original test passed, transformed test should pass
  - If failure, verify it's not due to transformation

- [ ] **Cluster cleans up properly**
  - No orphaned processes
  - Test directories cleaned up

- [ ] **Review transformation quality**
  - Maximum logic preserved?
  - Only necessary operations commented out?
  - Appropriate documentation added?

### Validation Commands

```bash
# Run single test with system properties
mvn test -Dtest=YourTransformedTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6 \
  -pl hadoop-hdfs-project/hadoop-hdfs

# Run with debug output
mvn test -Dtest=YourTransformedTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6 \
  -pl hadoop-hdfs-project/hadoop-hdfs -X

# Check for orphaned processes after test
ps aux | grep -E "(NameNode|DataNode|ProcessLauncher)"

# Clean up if needed
pkill -f "ProcessLauncher"
```

---

## Best Practices

### DO ✅

1. **Consult mapping tables first** - Before assuming something is unsupported, check all mapping tables

2. **Use the API hierarchy** - Try FileSystem → DFSClient → Protocol → Admin → JMX in order

3. **Preserve test intent** - Even if implementation changes, maintain what the test is verifying

4. **System properties are automatic** - No manual environment checks needed!
   ```java
   // Simply build the cluster - system properties handled automatically!
   ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
       .numDataNodes(3)
       .build();  // Reads hadoop.start.home and hadoop.upgrade.home automatically
   ```

5. **Keep transformations minimal** - Change only what's necessary

6. **Document significant changes** - But only non-obvious ones

7. **Test both single-version and multi-version scenarios** when applicable

### DON'T ❌

1. **Don't give up on transformation too early** - Most operations have client equivalents

2. **Don't remove test logic without checking mapping tables**

3. **Don't use MiniDFSCluster-specific test utilities** - Many have client API equivalents:
   - ❌ `DataNodeTestUtils.triggerHeartbeat(dn)`
   - ✅ `Thread.sleep() + GenericTestUtils.waitFor()`

4. **Don't over-document** - Only comment what's not obvious

5. **Don't mix MiniDFSCluster and ProcessBasedMiniDFSCluster** in same test

6. **Don't manually check environment variables** - System properties are handled automatically!

### Performance Considerations

1. **Process startup is slower** - ProcessBasedMiniDFSCluster takes longer to start than MiniDFSCluster
   - Be patient with cluster startup
   - Consider increasing timeouts for slow systems

2. **Heartbeats are real-time** - Can't artificially trigger them
   - Use `Thread.sleep()` or `GenericTestUtils.waitFor()`
   - Account for natural heartbeat intervals (default 3s)

3. **RPC overhead** - All operations go through RPC
   - Slightly slower than in-process calls
   - Not significant for most tests

---

## Quick Reference Decision Tree

```
Found server-side operation?
    │
    ├─> Is it already client-side? (fs.method(), dfs.method())
    │   └─> ✅ Use as-is, no transformation needed
    │
    ├─> Check FSNamesystem table (Section 4.2)
    │   ├─> Found equivalent?
    │   │   └─> ✅ Use client API (FileSystem, DFSClient, etc.)
    │   └─> Not found?
    │       └─> Continue...
    │
    ├─> Check NameNode/DataNode tables (Sections 4.3, 4.4)
    │   ├─> Found equivalent?
    │   │   └─> ✅ Use client API
    │   └─> Not found?
    │       └─> Continue...
    │
    ├─> Check Admin operations table (Section 4.5)
    │   ├─> Found equivalent?
    │   │   └─> ✅ Use DFSAdmin or admin API
    │   └─> Not found?
    │       └─> Continue...
    │
    ├─> Check Monitoring table (Section 4.6)
    │   ├─> Found equivalent?
    │   │   └─> ✅ Use JMX or client statistics API
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
2. ✅ Runs with ProcessBasedMiniDFSCluster
3. ✅ Preserves maximum test logic
4. ✅ Uses client APIs for all accessible operations
5. ✅ Comments out only truly inaccessible operations
6. ✅ Includes minimal, clear documentation
7. ✅ Passes when original test passed

### Key Takeaways

- **Most operations have client equivalents** - Consult mapping tables thoroughly
- **Use the API hierarchy** - FileSystem → DFSClient → Protocol → Admin → JMX
- **Only comment out internal storage/JVM operations** - Everything else has an API
- **Document sparingly** - Only non-obvious transformations
- **Test thoroughly** - Verify core test logic preserved

### Getting Help

If stuck on a transformation:
1. Check all mapping tables in Section 4
2. Review common patterns in Section 6
3. Look at existing transformed tests in `hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/process/upgrade/`
4. Consult ProcessBasedMiniDFSCluster documentation:
   - `ProcessBasedMiniDFSCluster-UserGuide.md`
   - `VersionUpgradeTestingGuide.md`

---

**End of Transformation Guide**

For questions or contributions to this guide, please update this document with additional patterns and mappings as you discover them.
