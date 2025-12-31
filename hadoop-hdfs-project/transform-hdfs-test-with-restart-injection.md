# Prompt: Transform HDFS Mini Cluster Test with Restart Position Injection

## Objective

Transform an existing HDFS mini cluster test to inject restart positions for distributed system restart testing. The transformation will generate:
1. A new test file with `_RestartInjected` suffix
2. A restart configuration file for the Maven plugin

## Input

- **Test File Path**: Path to the original test file (e.g., `/path/to/TestHdfsOperations.java`)
- **Test Class**: Fully-qualified class name (e.g., `org.apache.hadoop.hdfs.TestHdfsOperations`)

## Output

1. **Generated Test File**: `{OriginalFileName}_RestartInjected.java` at the same directory as the input file
2. **Restart Configuration**: `restart-config.json` in the `restarts-config/` directory under the same module directory as the test file, if not exist create it.

## HDFS Cluster Configuration

### MiniDFSCluster Overview

The `MiniDFSCluster` is HDFS's test cluster implementation that creates a single-process DFS cluster for testing. It supports:
- Multiple NameNodes (for HA configurations)
- Multiple DataNodes (configurable number)
- Federation configurations
- Various startup options (FORMAT, REGULAR, UPGRADE, ROLLBACK)

### Node Roles

HDFS has two primary node types managed by the restart adapter:

| Role | Aliases | Description |
|------|---------|-------------|
| `namenode` | `master` | Manages filesystem metadata, block mapping, namespace operations |
| `datanode` | `worker` | Stores actual data blocks, handles data read/write operations |
| `all` | - | All nodes (NameNodes first, then DataNodes) |

### HDFS Restart Adapter

The adapter is located at: `hadoop-hdfs-project/restart-hdfs-adapter`

**Adapter Class**: `org.apache.hadoop.hdfs.restart.HdfsClusterAdapter`

**Key Features**:
- Supports GRACEFUL, CRASH, and DELAYED_CRASH restart modes
- Preserves StartupOption across restarts
- Handles NameNode and DataNode restarts with proper cleanup
- Calls `waitActive()` after DataNode restarts to ensure registration

## Transformation Instructions

### Step 1: Analyze the Original Test

Read the input test file and identify:

1. **Cluster Instance**: Find the MiniDFSCluster instance variable
   - Direct field: `private MiniDFSCluster cluster;`
   - Protected field from parent: `protected MiniDFSCluster cluster;`
   - Via getter: `getCluster()` from parent class

2. **Test Methods**: Identify all `@Test` annotated methods

3. **Critical Operations**: Look for operations that involve state transitions:
   - **File I/O operations**: `create()`, `write()`, `writeUTF()`, `hflush()`, `hsync()`, `close()`, `append()`
   - **FileSystem operations**: `mkdirs()`, `delete()`, `rename()`, `setReplication()`, `setPermission()`
   - **Block operations**: Block allocation, replication, recovery
   - **Stream operations**: Flush operations, sync operations
   - **Lease operations**: Lease renewal, lease recovery
   - **Snapshot operations**: `createSnapshot()`, `deleteSnapshot()`, `renameSnapshot()`
   - **Encryption operations**: Creating encryption zones
   - **ACL/XAttr operations**: Setting ACLs, extended attributes

### Step 2: Identify Cluster Source (CRITICAL)

**IMPORTANT**: In many HDFS tests, the MiniDFSCluster object comes from a parent base test class, NOT directly initialized in the test class or method body.

#### Common HDFS Base Test Classes

| Base Class | Cluster Variable | Getter Method |
|------------|------------------|---------------|
| `AdminStatesBaseTest` | `private MiniDFSCluster cluster` | `getCluster()` |
| `BlockReportTestBase` | `protected MiniDFSCluster cluster` | Direct access |
| `FSXAttrBaseTest` | `protected static MiniDFSCluster dfsCluster` | Direct access |
| `TestDFSStripedOutputStreamWithFailureBase` | `protected MiniDFSCluster cluster` | Direct access |

#### Finding the Cluster Object

1. **Check test class fields** for `MiniDFSCluster cluster` or similar
2. **Check parent class** if extends a base test class:
   ```java
   public class TestDecommission extends AdminStatesBaseTest {
       // cluster available via getCluster()
   }
   ```
3. **Check @Before/@BeforeEach methods** for cluster initialization
4. **Check static @BeforeClass methods** for shared cluster setup
5. **Check helper utility classes** like `HBaseTestingUtility` patterns

#### Transformation Strategy for Inherited Clusters

When the cluster comes from a parent class:

```java
// If parent has protected cluster field:
RestartFramework.at("after_write")
    .on(cluster)  // Direct access to inherited field
    .restart("datanode")
    .execute();

// If parent has getCluster() method:
RestartFramework.at("after_write")
    .on(getCluster())  // Via getter
    .restart("datanode")
    .execute();
```

### Step 3: Identify Restart Points

For each test method, identify potential restart points based on these criteria:

**Good Restart Points** (inject here):
- After `hflush()` or `hsync()` operations
- After `create()` but before writing data
- During long write operations (in the middle of data writing)
- Before `close()` operations
- After append operations
- After block allocation (pipeline established)
- During block replication
- After lease renewal
- After snapshot creation
- After setting ACLs or XAttrs
- During directory operations (after mkdir, before populating)

**Poor Restart Points** (avoid):
- Before cluster setup (no cluster exists yet)
- After cluster teardown (cluster already destroyed)
- During trivial operations (simple reads with no state changes)
- Operations that are too fast to test meaningful state

**Naming Convention for Restart Positions**:
- Use descriptive, lowercase names with underscores
- Pattern: `{operation}_{context}`
- Examples:
  - `after_hflush`
  - `after_create`
  - `before_close`
  - `during_write`
  - `after_append`
  - `after_block_allocation`
  - `after_replication`
  - `after_lease_renew`
  - `after_snapshot`
  - `after_set_acl`
  - `after_mkdir`

### Step 4: Generate the Restart-Injected Test File

Create a new test file with the following transformations:

#### 4.1 Package and Imports

```java
// Keep original package declaration
package org.apache.hadoop.hdfs;

// Add these imports at the top (if not already present)
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

// Keep all original imports
```

#### 4.2 Class Declaration

```java
// Original class name: TestHdfsOperations
// New class name: TestHdfsOperations_RestartInjected

// IMPORTANT: Preserve the same inheritance structure!
// If original: public class TestHdfsOperations extends AdminStatesBaseTest
// Then use:    public class TestHdfsOperations_RestartInjected extends AdminStatesBaseTest
public class TestHdfsOperations_RestartInjected extends AdminStatesBaseTest {
    // Keep all original fields and variables
}
```

**CRITICAL**: Always preserve the base class inheritance. The cluster object is often inherited from the parent class.

#### 4.3 Cluster Setup and Teardown

Keep the `@Before`/`@BeforeEach` and `@After`/`@AfterEach` methods unchanged:

```java
@Before
public void setUp() throws Exception {
    // Keep original setup code unchanged
}

@After
public void tearDown() throws Exception {
    // Keep original teardown code unchanged
}
```

**Note**: Setup methods may be inherited from parent class. In that case, don't add setup methods to the generated test unless they contain test-specific logic.

#### 4.4 Transform Test Methods

For each `@Test` method, apply the following transformations:

**Original Test Method**:
```java
@Test
public void testHFlush() throws Exception {
    Path file = new Path("/test.txt");
    FSDataOutputStream out = fs.create(file);
    out.writeUTF("Data before flush");
    out.hflush();
    out.writeUTF("Data after flush");
    out.close();

    assertTrue(fs.exists(file));
}
```

**Transformed Test Method**:
```java
@Test
public void testHFlush() throws Exception {
    Path file = new Path("/test.txt");
    FSDataOutputStream out = fs.create(file);

    // RESTART POINT 1: after_create
    RestartFramework.at("after_create")
        .on(cluster)  // or getCluster() if from parent
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    out.writeUTF("Data before flush");
    out.hflush();

    // RESTART POINT 2: after_hflush
    RestartFramework.at("after_hflush")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    out.writeUTF("Data after flush");

    // RESTART POINT 3: before_close
    RestartFramework.at("before_close")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    out.close();

    assertTrue(fs.exists(file));
}
```

**Injection Pattern**:

1. **After I/O Operations**:
   ```java
   out.hflush();

   // Inject restart point
   RestartFramework.at("after_hflush")
       .on(cluster)
       .restart("datanode")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();
   ```

2. **Before Critical Operations**:
   ```java
   // Inject restart point before close
   RestartFramework.at("before_close")
       .on(cluster)
       .restart("datanode")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();

   out.close();
   ```

3. **During Long Operations**:
   ```java
   for (int i = 0; i < 1000; i++) {
       out.write(data);

       // Inject restart point in the middle
       if (i == 500) {
           RestartFramework.at("during_write")
               .on(cluster)
               .restart("datanode")
               .withIndex(0)
               .withMode(RestartMode.GRACEFUL)
               .execute();
       }
   }
   ```

#### 4.5 Node Role Selection Guidelines

| Operation Type | Primary Node Role | Secondary Node Role | Reason |
|----------------|-------------------|---------------------|--------|
| File creation (namespace) | `namenode` | `datanode` | NameNode creates namespace entry |
| Data write/read | `datanode` | - | DataNode handles data blocks |
| Data flush (hflush/hsync) | `datanode` | - | DataNode durability |
| File close | `datanode` | `namenode` | Both involved in finalization |
| Block replication | `datanode` | `namenode` | DataNode copies, NameNode coordinates |
| File delete/rename | `namenode` | - | Namespace operations |
| Snapshot operations | `namenode` | - | Metadata operations |
| ACL/XAttr operations | `namenode` | - | Metadata operations |
| Lease operations | `namenode` | `datanode` | NameNode tracks, DataNode holds |
| Block recovery | `datanode` | `namenode` | Pipeline recovery |
| Append operations | `datanode` | `namenode` | Both involved |

#### 4.6 Default Restart Configuration

Use these defaults for all injected restart points:
- **Node Role**: `"datanode"` (most common; use `"namenode"` for metadata operations)
- **Node Index**: `0` (first node)
- **Restart Mode**: `RestartMode.GRACEFUL` (default, safest)

### Step 5: Generate Restart Configuration File

Create `restarts-config/restart-config.json` with the following structure:

```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.hdfs.TestHdfsOperations_RestartInjected",
      "testMethod": "testHFlush",
      "restartPoints": [
        {
          "position": "after_create",
          "targets": ["datanode", "namenode"],
          "modes": ["GRACEFUL", "CRASH"]
        },
        {
          "position": "after_hflush",
          "targets": ["datanode"],
          "modes": ["GRACEFUL", "CRASH", "DELAYED_CRASH"]
        },
        {
          "position": "before_close",
          "targets": ["datanode"],
          "modes": ["GRACEFUL", "CRASH"]
        }
      ]
    }
  ]
}
```

#### Configuration Generation Rules

For each test method in the transformed test:

1. **Create a test specification** with:
   - `testClass`: The fully-qualified name of the generated test class
   - `testMethod`: The test method name (same as original)
   - `restartPoints`: Array of restart point configurations

2. **For each restart point** injected in the test method:
   - `position`: The position identifier used in `.at("...")`
   - `targets`: Array of node roles to test
     - Default: `["datanode"]` for data operations
     - Use `["datanode", "namenode"]` for operations involving both
     - Use `["namenode"]` for metadata-only operations
   - `modes`: Array of restart modes to test
     - Minimum: `["GRACEFUL", "CRASH"]`
     - Full: `["GRACEFUL", "CRASH", "DELAYED_CRASH"]` for timing-sensitive operations

#### Target Selection Guidelines

- **`["datanode"]`**: Data write/read, block operations, flush operations
- **`["namenode"]`**: Metadata operations (delete, rename, snapshot, ACL)
- **`["datanode", "namenode"]`**: Operations involving both (create + write, append, lease recovery)

#### Mode Selection Guidelines

- **`["GRACEFUL"]`**: Basic test, verify restart works
- **`["GRACEFUL", "CRASH"]`**: Standard test, verify crash recovery
- **`["GRACEFUL", "CRASH", "DELAYED_CRASH"]`**: Advanced test for timing-sensitive operations (flush, sync, replication)

### Step 6: File Placement

1. **Generated Test File**:
   - Location: Same directory as original test file
   - Name: `{OriginalClassName}_RestartInjected.java`
   - Example: `TestWrite.java` -> `TestWrite_RestartInjected.java`

2. **Restart Configuration**:
   - Location: `restarts-config/` directory under the same module directory as the test file
   - Name: `restart-config.json`
   - If file exists, append to the `tests` array (avoid duplicates)
   - If file doesn't exist, create new file

## HDFS-Specific Patterns

### Pattern 1: File Write with Flush

```java
@Test
public void testWriteWithFlush() throws Exception {
    Path file = new Path("/test.txt");
    FSDataOutputStream out = fs.create(file);

    RestartFramework.at("after_create")
        .on(cluster)
        .restart("datanode")
        .execute();

    out.writeUTF("data");
    out.hflush();

    RestartFramework.at("after_hflush")
        .on(cluster)
        .restart("datanode")
        .execute();

    out.hsync();

    RestartFramework.at("after_hsync")
        .on(cluster)
        .restart("datanode")
        .execute();

    out.close();
    assertTrue(fs.exists(file));
}
```

### Pattern 2: Block Replication Testing

```java
@Test
public void testBlockReplication() throws Exception {
    Path file = new Path("/replicated.txt");
    DFSTestUtil.createFile(fs, file, 1024, (short)3, 0L);

    RestartFramework.at("after_file_creation")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .execute();

    // Wait for replication
    DFSTestUtil.waitReplication(fs, file, (short)3);

    RestartFramework.at("after_replication_complete")
        .on(cluster)
        .restart("datanode")
        .withIndex(1)
        .execute();

    // Verify file still accessible
    assertTrue(fs.exists(file));
}
```

### Pattern 3: Append Operations

```java
@Test
public void testAppend() throws Exception {
    Path file = new Path("/append.txt");
    DFSTestUtil.createFile(fs, file, 1024, (short)1, 0L);

    FSDataOutputStream out = fs.append(file);

    RestartFramework.at("after_append_open")
        .on(cluster)
        .restart("datanode")
        .execute();

    out.writeUTF("appended data");

    RestartFramework.at("before_append_close")
        .on(cluster)
        .restart("datanode")
        .execute();

    out.close();
}
```

### Pattern 4: Snapshot Testing

```java
@Test
public void testSnapshot() throws Exception {
    Path dir = new Path("/snapshotDir");
    fs.mkdirs(dir);
    fs.allowSnapshot(dir);

    // Create file
    DFSTestUtil.createFile(fs, new Path(dir, "file.txt"), 1024, (short)1, 0L);

    RestartFramework.at("before_snapshot")
        .on(cluster)
        .restart("namenode")
        .execute();

    // Create snapshot
    fs.createSnapshot(dir, "snap1");

    RestartFramework.at("after_snapshot")
        .on(cluster)
        .restart("namenode")
        .execute();

    // Verify snapshot exists
    assertTrue(fs.exists(new Path(dir, ".snapshot/snap1")));
}
```

### Pattern 5: HA NameNode Testing

```java
@Test
public void testHAFailover() throws Exception {
    // Assuming cluster is configured with HA (2 NameNodes)
    Path file = new Path("/ha_test.txt");
    DFSTestUtil.createFile(fs, file, 1024, (short)1, 0L);

    RestartFramework.at("before_failover")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)  // Active NameNode
        .execute();

    // Failover should occur, file should still be accessible
    assertTrue(fs.exists(file));

    RestartFramework.at("after_failover")
        .on(cluster)
        .restart("namenode")
        .withIndex(1)  // New Active (former Standby)
        .execute();
}
```

### Pattern 6: Inherited Cluster from Base Class

```java
// When cluster comes from parent class like AdminStatesBaseTest
public class TestDecommission_RestartInjected extends AdminStatesBaseTest {

    @Test
    public void testDecommission() throws Exception {
        // Access cluster via getter from parent
        MiniDFSCluster cluster = getCluster();

        FileSystem fs = cluster.getFileSystem();
        Path file = new Path("/decommission_test.txt");
        DFSTestUtil.createFile(fs, file, 1024, (short)3, 0L);

        RestartFramework.at("after_file_creation")
            .on(getCluster())  // Use getter
            .restart("datanode")
            .withIndex(0)
            .execute();

        // Perform decommission operations
        // ...
    }
}
```

### Pattern 7: Lease Recovery Testing

```java
@Test
public void testLeaseRecovery() throws Exception {
    Path file = new Path("/lease_test.txt");
    FSDataOutputStream out = fs.create(file);
    out.writeUTF("data");
    out.hflush();

    // Don't close - simulate client crash
    RestartFramework.at("before_client_crash")
        .on(cluster)
        .restart("datanode")
        .withMode(RestartMode.CRASH)
        .execute();

    // Wait for lease to expire
    Thread.sleep(60000);

    RestartFramework.at("after_lease_expiry")
        .on(cluster)
        .restart("namenode")
        .execute();

    // Trigger lease recovery by attempting to append
    // fs.append(file) or wait for automatic recovery
}
```

## Common HDFS Operations and Suggested Restart Points

| HDFS Operation | Suggested Restart Point Name | Node Role | Timing |
|----------------|------------------------------|-----------|--------|
| `fs.create()` | `after_create` | `namenode`, `datanode` | After |
| `out.write()` | `during_write`, `after_write` | `datanode` | During/After |
| `out.hflush()` | `after_hflush` | `datanode` | After |
| `out.hsync()` | `after_hsync` | `datanode` | After |
| `out.close()` | `before_close`, `after_close` | `datanode` | Before/After |
| `fs.append()` | `after_append_open`, `during_append` | `datanode` | After/During |
| `fs.delete()` | `before_delete`, `after_delete` | `namenode` | Before/After |
| `fs.rename()` | `before_rename`, `after_rename` | `namenode` | Before/After |
| `fs.mkdirs()` | `after_mkdir` | `namenode` | After |
| `fs.setReplication()` | `after_set_replication` | `namenode` | After |
| `fs.createSnapshot()` | `after_snapshot` | `namenode` | After |
| `fs.setAcl()` | `after_set_acl` | `namenode` | After |
| `fs.setXAttr()` | `after_set_xattr` | `namenode` | After |
| Block replication | `during_replication`, `after_replication` | `datanode` | During/After |
| Pipeline setup | `after_pipeline_setup` | `datanode` | After |
| Lease renewal | `after_lease_renew` | `namenode` | After |

## Validation Checklist

After transformation, verify:

- [ ] Generated test file compiles without errors
- [ ] All original test logic is preserved
- [ ] Class inheritance is maintained (extends same base class if applicable)
- [ ] Correct cluster variable is used (direct, inherited, or via getter)
- [ ] Restart points are placed at meaningful HDFS operations
- [ ] Restart position names are descriptive and HDFS-specific
- [ ] Node roles (namenode/datanode) are correctly chosen
- [ ] Configuration file has correct fully-qualified class names
- [ ] Configuration file includes all restart points from the test
- [ ] Target arrays match the operation type
- [ ] Mode arrays are appropriate for timing sensitivity
- [ ] Files are placed in correct locations
- [ ] Original test file is not modified (only new files created)

## Advanced Scenarios

### Multiple Test Methods

If the original test has multiple `@Test` methods:

1. Transform each method independently
2. Inject restart points in each method
3. Create a separate test specification for each method in the configuration

Example configuration:
```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.hdfs.TestHdfs_RestartInjected",
      "testMethod": "testWrite",
      "restartPoints": [...]
    },
    {
      "testClass": "org.apache.hadoop.hdfs.TestHdfs_RestartInjected",
      "testMethod": "testRead",
      "restartPoints": [...]
    }
  ]
}
```

### Helper Methods

If the test has helper methods:

1. **Do not inject restart points in helper methods**
2. Only inject in `@Test` annotated methods
3. Keep helper methods unchanged

### Tests Without Obvious Restart Points

If a test has no clear state transitions:

1. Inject restart points within the range of (a) after cluster setup and (b) before cluster teardown
2. Evenly distribute restart points to cover the test execution
3. You MUST use percentage-based positions (e.g., `at_25_percent`, `at_50_percent`) to at least cover 4 points during the test execution
4. Find cluster operations to place restart points around

**IMPORTANT**: You are NOT allowed to skip any test transformation due to lack of restart points. Always inject at least one restart point per test method.

### HA (High Availability) Configurations

For tests with multiple NameNodes:

```java
// Restart active NameNode (index 0)
RestartFramework.at("restart_active_nn")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .execute();

// Restart standby NameNode (index 1)
RestartFramework.at("restart_standby_nn")
    .on(cluster)
    .restart("namenode")
    .withIndex(1)
    .execute();

// Restart all NameNodes
RestartFramework.at("restart_all_namenodes")
    .on(cluster)
    .restart("namenode")
    .withIndex("all")
    .execute();
```

Configuration:
```json
{
  "position": "restart_active_nn",
  "targets": ["namenode"],
  "modes": ["GRACEFUL", "CRASH"]
}
```

### Multiple DataNodes

For tests with multiple DataNodes:

```java
// Restart specific DataNode
RestartFramework.at("restart_dn_2")
    .on(cluster)
    .restart("datanode")
    .withIndex(2)  // Third DataNode
    .execute();

// Restart a random DataNode
RestartFramework.at("restart_random_dn")
    .on(cluster)
    .restart("datanode")
    .withIndex("random")
    .execute();

// Restart all DataNodes
RestartFramework.at("restart_all_datanodes")
    .on(cluster)
    .restart("datanode")
    .withIndex("all")
    .execute();
```

## Notes

- **Non-invasive**: Original test file is never modified
- **Incremental**: Can transform tests one at a time
- **Compatible**: Generated tests can run both with and without restart injection
- **Configurable**: Configuration file allows easy adjustment of test matrix
- **HDFS-aware**: Node role selection is specific to HDFS architecture (NameNode vs DataNode)
- **Inheritance-aware**: Handles tests that inherit cluster setup from parent classes

## Dependencies

Ensure the following dependencies are included in the test's module to use the Restart Testing Framework:

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Restart Testing Framework - Core -->
    <dependency>
        <groupId>org.restarttest</groupId>
        <artifactId>restart-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>

    <!-- Restart Testing Framework - HDFS Adapter -->
    <dependency>
        <groupId>org.restarttest</groupId>
        <artifactId>restart-hdfs-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Note**: Since the HDFS adapter is in `hadoop-hdfs-project/restart-hdfs-adapter`, you may need to adjust the groupId/artifactId based on your project structure, or use the adapter already available in the HDFS project.

## Quick Reference: Finding the Cluster Object

| Pattern | How to Access Cluster | Example |
|---------|----------------------|---------|
| Direct field in test class | `cluster` | `private MiniDFSCluster cluster;` |
| Protected field from parent | `cluster` | `protected MiniDFSCluster cluster;` |
| Getter method from parent | `getCluster()` | `AdminStatesBaseTest.getCluster()` |
| Static field from parent | `dfsCluster` | `protected static MiniDFSCluster dfsCluster;` |
| Builder in @Before method | `cluster` (after assignment) | `cluster = new MiniDFSCluster.Builder(conf).build();` |
| HBaseTestingUtility-like pattern | `testUtil.getDFSCluster()` | Via utility class getter |

## Example Transformations

### Example 1: Simple Test with Direct Cluster

**Input**: `TestWrite.java`
```java
package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestWrite {
    private MiniDFSCluster cluster;
    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        Configuration conf = new Configuration();
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
        cluster.waitActive();
        fs = cluster.getFileSystem();
    }

    @After
    public void tearDown() throws Exception {
        if (fs != null) fs.close();
        if (cluster != null) cluster.shutdown();
    }

    @Test
    public void testHFlush() throws Exception {
        Path file = new Path("/test.txt");
        FSDataOutputStream out = fs.create(file);
        out.writeUTF("Data before flush");
        out.hflush();
        out.writeUTF("Data after flush");
        out.close();

        assertTrue(fs.exists(file));
    }
}
```

**Output**: `TestWrite_RestartInjected.java`
```java
package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import static org.junit.Assert.*;

public class TestWrite_RestartInjected {
    private MiniDFSCluster cluster;
    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        Configuration conf = new Configuration();
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
        cluster.waitActive();
        fs = cluster.getFileSystem();
    }

    @After
    public void tearDown() throws Exception {
        if (fs != null) fs.close();
        if (cluster != null) cluster.shutdown();
    }

    @Test
    public void testHFlush() throws Exception {
        Path file = new Path("/test.txt");
        FSDataOutputStream out = fs.create(file);

        // RESTART POINT 1: after_create
        RestartFramework.at("after_create")
            .on(cluster)
            .restart("datanode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        out.writeUTF("Data before flush");
        out.hflush();

        // RESTART POINT 2: after_hflush
        RestartFramework.at("after_hflush")
            .on(cluster)
            .restart("datanode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        out.writeUTF("Data after flush");

        // RESTART POINT 3: before_close
        RestartFramework.at("before_close")
            .on(cluster)
            .restart("datanode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        out.close();

        assertTrue(fs.exists(file));
    }
}
```

### Example 2: Test with Inherited Cluster

**Input**: `TestDecommission.java`
```java
package org.apache.hadoop.hdfs;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestDecommission extends AdminStatesBaseTest {

    @Test
    public void testDecommissionFile() throws Exception {
        FileSystem fs = getCluster().getFileSystem();
        Path file = new Path("/decom_test.txt");
        DFSTestUtil.createFile(fs, file, 1024, (short)3, 0L);

        // Decommission operations...
        decommissionNode(0);

        assertTrue(fs.exists(file));
    }
}
```

**Output**: `TestDecommission_RestartInjected.java`
```java
package org.apache.hadoop.hdfs;

import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import static org.junit.Assert.*;

public class TestDecommission_RestartInjected extends AdminStatesBaseTest {

    @Test
    public void testDecommissionFile() throws Exception {
        FileSystem fs = getCluster().getFileSystem();
        Path file = new Path("/decom_test.txt");
        DFSTestUtil.createFile(fs, file, 1024, (short)3, 0L);

        // RESTART POINT 1: after_file_creation
        RestartFramework.at("after_file_creation")
            .on(getCluster())  // Via getter from parent class
            .restart("datanode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Decommission operations...
        decommissionNode(0);

        // RESTART POINT 2: after_decommission
        RestartFramework.at("after_decommission")
            .on(getCluster())
            .restart("namenode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        assertTrue(fs.exists(file));
    }
}
```

**Configuration**: `restarts-config/restart-config.json`
```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.hdfs.TestDecommission_RestartInjected",
      "testMethod": "testDecommissionFile",
      "restartPoints": [
        {
          "position": "after_file_creation",
          "targets": ["datanode"],
          "modes": ["GRACEFUL", "CRASH"]
        },
        {
          "position": "after_decommission",
          "targets": ["namenode", "datanode"],
          "modes": ["GRACEFUL", "CRASH"]
        }
      ]
    }
  ]
}
```
