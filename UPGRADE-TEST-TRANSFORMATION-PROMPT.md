# MiniDFSCluster to ProcessBasedMiniDFSCluster Transformation Guide

## Table of Contents
1. [Introduction & Philosophy](#introduction--philosophy)
2. [Prerequisites & Setup](#prerequisites--setup)
3. [Test Organization and Naming Convention](#test-organization-and-naming-convention)
4. [Core Transformation Rules](#core-transformation-rules)
5. [Comprehensive API Mapping Tables](#comprehensive-api-mapping-tables)
6. [Step-by-Step Transformation Process](#step-by-step-transformation-process)
7. [Common Transformation Patterns](#common-transformation-patterns)
8. [When to Comment Out Logic](#when-to-comment-out-logic)
9. [Testing Checklist](#testing-checklist)
10. [Best Practices](#best-practices)
11. [Quick Reference Decision Tree](#quick-reference-decision-tree)

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

### Environment Variables
ProcessBasedMiniDFSCluster requires Hadoop distribution paths:

```bash
# For single-version testing
export HADOOP_HOME=/path/to/hadoop-3.3.5

# For multi-version upgrade testing
export HADOOP_3_3_5_HOME=/path/to/hadoop-3.3.5
export HADOOP_3_3_6_HOME=/path/to/hadoop-3.3.6

# Verify environment
echo $HADOOP_HOME
echo $HADOOP_3_3_5_HOME
```

### Test Configuration
```java
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;

@Before
public void setUp() {
    // Check environment variables in test setup
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set", hadoopHome);
}
```

### Running Transformed Tests
```bash
# Run with environment variable
export HADOOP_HOME=/opt/hadoop-3.3.5
mvn test -Dtest=YourTransformedTest -pl hadoop-hdfs-project/hadoop-hdfs

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
| `new Builder(conf).build()` | `new Builder(conf).allNodesHadoopDistribution(hadoopHome).build()` | ✓ | Must specify Hadoop distribution |
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

// AFTER (ProcessBasedMiniDFSCluster)
Configuration conf = new HdfsConfiguration();
String hadoopHome = System.getenv("HADOOP_HOME");
assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .build();
cluster.waitClusterUp();
```

#### Multi-Version Cluster (for upgrade tests)

```java
// For testing 3.3.5 → 3.3.6 upgrade
String hadoop335 = System.getenv("HADOOP_3_3_5_HOME");
String hadoop336 = System.getenv("HADOOP_3_3_6_HOME");
assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335);
assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336);

ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution(hadoop335)      // NN on 3.3.5
        .dataNodeHadoopDistribution(0, hadoop335)   // DN0 on 3.3.5
        .dataNodeHadoopDistribution(1, hadoop335)   // DN1 on 3.3.5
        .dataNodeHadoopDistribution(2, hadoop335)   // DN2 on 3.3.5
        .format(true)
        .build();
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
                .allNodesHadoopDistribution(hadoopHome)
                .build()) {
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

- [ ] **Environment variables set**
  ```bash
  export HADOOP_HOME=/path/to/hadoop-3.3.5
  # OR for upgrade tests:
  export HADOOP_3_3_5_HOME=/path/to/hadoop-3.3.5
  export HADOOP_3_3_6_HOME=/path/to/hadoop-3.3.6
  ```

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
# Run single test
export HADOOP_HOME=/opt/hadoop-3.3.5
mvn test -Dtest=YourTransformedTest -pl hadoop-hdfs-project/hadoop-hdfs

# Run with debug output
mvn test -Dtest=YourTransformedTest -pl hadoop-hdfs-project/hadoop-hdfs -X

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

4. **Use assumeNotNull() for environment checks**
   ```java
   String hadoopHome = System.getenv("HADOOP_HOME");
   assumeNotNull("HADOOP_HOME must be set", hadoopHome);
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

6. **Don't assume environment variables are set** - Always use `assumeNotNull()`

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
