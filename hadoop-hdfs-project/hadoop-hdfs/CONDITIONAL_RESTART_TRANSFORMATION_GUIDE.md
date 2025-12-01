# HDFS Conditional Restart Transformation Guide

This document provides comprehensive guidance for transforming existing MiniDFSCluster tests to use conditional restart injection with the `RestartInjectionFramework`.

---

## 1. Overview

### 1.1 Purpose

Transform existing HDFS MiniDFSCluster tests to systematically inject component restarts at various points, enabling discovery of bugs related to the durability and consistency of target operations across restarts.

### 1.2 Framework Components

The `RestartInjectionFramework` provides:

```java
// Conditional restart method - pass cluster directly
restart(MiniDFSCluster cluster, String position, RestartTarget target, RestartMode mode)

// Restart configuration enums
RestartTarget  // WHAT to restart: NAMENODE, SINGLE_DATANODE, ALL_DATANODES, etc.
RestartMode    // HOW to restart: GRACEFUL, CRASH, DELAYED_CRASH

// Helper methods (called internally by restart())
executeRestart(cluster, target, mode, waitActive)
verifyClusterHealth(cluster, fs)
verifyFileIntegrity(fs, path, expectedLength, expectedData)
```

---

## 2. HARD RULES FOR TRANSFORMATION

### Rule 1: Create a NEW File - Do NOT Modify Original

**NEVER modify the original test file.** Instead, create a new file with the `_RestartInjected.java` suffix.

```
Original: TestHFlush.java           (DO NOT TOUCH)
New file: TestHFlush_RestartInjected.java  (CREATE THIS)
```

The new file should be in the same directory as the original.

### Rule 2: Preserve Original Test Logic Exactly

The original test logic MUST remain **exactly unchanged**. The ONLY modifications allowed are:
- Adding `restart()` calls at identified restart points
- Adding import statement if not already present: `import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;`

**DO NOT:**
- Change variable names
- Modify assertion logic
- Alter test data or parameters
- Refactor or "improve" the original code
- Change the order of original operations
- Add ANY comments (even explanatory ones)

### Rule 3: One Test Method, Multiple Restart Calls

Each `restart()` call is conditional:
- Only executes if system properties match: `restart.position`, `restart.target`, `restart.mode`
- If properties don't match, the call is a no-op

### Rule 4: Preserve Test Semantics

The transformed test must validate the **same invariants** as the original, plus:
- Data durability across restart (verified by `restart()` method internally)
- State consistency after recovery (verified by `verifyClusterHealth()`)
- Operation completion after restart (original assertions)

### Rule 5: Output Format

Output MUST consist of TWO parts:

**Part 1: Complete Java File**
- Create new file with `_RestartInjected.java` suffix
- Copy original test method with `restart()` calls added
- Include import statement: `import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;`
- Preserve all original code exactly (only add `restart()` calls)

**Part 2: Metadata JSON**
- Maps test methods to restart configurations
- Saved to `./test-restart-config` directory
- Includes file paths, restart positions, line numbers, targets, and modes
- Machine-readable format for test orchestration

### Rule 6: No Setup Modification

**DO NOT modify @Before or @After methods.**

The `restart()` method takes the cluster as a parameter directly, so no setup/teardown modifications are needed.

### Rule 7: No Comments

**DO NOT add any comments** to the generated code.

Only output:
- Import statement (if not present)
- `restart()` method calls

No explanatory comments like:
- ❌ `// Restart NameNode gracefully after flush`
- ❌ `// === RESTART INJECTION START ===`
- ❌ `// Only executes if restart.position=after_flush`

---

## 3. Identifying Valid Restart Positions

### 3.1 Understand the Test First

**Before identifying restart points, you must understand what the test is actually testing.**

1. **Read the entire test method** to understand its purpose and flow
2. **Identify the core operation being tested** (e.g., hflush durability, append semantics, delete atomicity)
3. **Determine what state changes are critical** and where a restart would meaningfully test durability/recovery
4. **Consider the test's assertions** - restart points should be placed where the restart could potentially cause the assertion to fail if there's a bug

**Key principle:** Restart points should be placed at locations where:
- Important state has been created or modified
- The system has made durability guarantees (explicit or implicit)
- A restart could expose bugs in recovery logic

**Do NOT mechanically inject restarts at every operation.** Instead, think about:
- What is this test trying to verify?
- Where would a restart be most likely to expose a bug?
- What state needs to survive the restart for the test to pass?

### 3.2 High-Value Restart Points (Reference)

The following table provides **reference patterns** for common restart points. Use this as guidance, but always prioritize understanding the specific test's intent.

| Operation Pattern | Restart Position | Value | Rationale |
|-------------------|------------------|-------|-----------|
| `write() -> hflush()` | After `hflush()` | HIGH | Tests pipeline data persistence |
| `write() -> hsync()` | After `hsync()` | HIGH | Tests NameNode metadata persistence |
| `create() -> write()` | During write (partial) | HIGH | Tests incomplete block handling |
| `write() -> close()` | Before `close()` | HIGH | Tests file finalization recovery |
| `close() -> read()` | After `close()` | MEDIUM | Tests completed file persistence |
| `setReplication()` | During replication | MEDIUM | Tests replication state recovery |
| `append()` | During append | HIGH | Tests append recovery semantics |
| `delete()` | During delete | MEDIUM | Tests delete atomicity |
| `rename()` | During rename | MEDIUM | Tests rename atomicity |

### 3.3 Code Patterns (Reference)

Look for these code patterns as restart injection points:

```java
// Pattern 1: After flush operations
out.hflush();
// <-- RESTART POINT: after_flush

out.hsync();
// <-- RESTART POINT: after_sync

// Pattern 2: Between write operations
AppendTestUtil.write(out, 0, halfSize);
// <-- RESTART POINT: during_write (partial data written)
AppendTestUtil.write(out, halfSize, halfSize);

// Pattern 3: Before/After close
out.hflush();
// <-- RESTART POINT: before_close
out.close();
// <-- RESTART POINT: after_close

// Pattern 4: After complete block written
AppendTestUtil.write(out, 0, BLOCK_SIZE);  // Complete block
out.hflush();
// <-- RESTART POINT: after_write_block

// Pattern 5: During metadata operations
fs.setReplication(path, newReplication);
// <-- RESTART POINT: during_replication

fs.append(path);
// <-- RESTART POINT: after_append

fs.delete(path, recursive);
// <-- RESTART POINT: after_delete

fs.rename(src, dst);
// <-- RESTART POINT: after_rename
```

### 3.4 Invalid Restart Points (AVOID)

Do NOT inject restarts:
- Inside tight loops without synchronization
- Before any data is written
- After test assertions (test already passed/failed)
- In finally/cleanup blocks

### 3.5 Tests to SKIP (Do Not Transform)

**SKIP Parameterized Tests:**
- Tests using `@Parameterized` or `@RunWith(Parameterized.class)`
- Tests using JUnit 5 `@ParameterizedTest`
- Tests with data-driven test patterns via `@Parameters`

**Reason:** Parameterized tests already generate multiple test variants. Combining with restart injection creates combinatorial explosion and complex test management.

---

## 4. Determining Restart Targets and Modes

### 4.1 Choose Targets Based on Test Intent

**Before selecting restart targets, understand what the test is verifying and which components are involved.**

1. **Identify which components the test exercises:**
   - Does the test involve NameNode operations (metadata, namespace, leases)?
   - Does the test involve DataNode operations (block storage, pipelines, replication)?
   - Does the test involve client-side behavior?

2. **Match restart targets to the test's purpose:**
   - If testing metadata durability → NAMENODE restart is high value
   - If testing write pipeline resilience → SINGLE_DATANODE or RANDOM_DATANODE restart is high value
   - If testing data availability guarantees → ALL_DATANODES restart tests the worst case

3. **Consider the replication factor:**
   - Tests with replication=1: SINGLE_DATANODE restart will likely cause data loss (may be intentional)
   - Tests with replication=3: SINGLE_DATANODE restart should preserve data availability

**Do NOT blindly apply all targets to every restart point.** Select targets that meaningfully test the operation's recovery semantics.

### 4.2 Target Selection Matrix (Reference)

The following matrix provides **reference guidance** for which targets are typically applicable. Always prioritize understanding the specific test.

| Operation Type | NAMENODE | SINGLE_DATANODE | ALL_DATANODES | RANDOM_DATANODE | NAMENODE_AND_DATANODES |
|----------------|----------|-----------------|---------------|-----------------|------------------------|
| File create/write | YES | YES | YES | YES | YES |
| hflush/hsync | YES | YES | NO | YES | NO |
| File close | YES | YES | YES | YES | YES |
| File read | YES | YES | NO | YES | NO |
| Append | YES | YES | YES | YES | YES |
| Delete | YES | NO | NO | NO | NO |
| Rename | YES | NO | NO | NO | NO |
| Replication change | YES | YES | YES | YES | YES |
| Block operations | YES | YES | YES | YES | YES |

### 4.3 Restart Target Rationale (Reference)

**NAMENODE Restart Tests:**
- EditLog replay correctness
- FSImage/checkpoint recovery
- Lease recovery
- Namespace state persistence
- Block mapping recovery

**SINGLE_DATANODE Restart Tests:**
- Pipeline recovery
- Block report handling
- Replica recovery
- Write continuation after DN failure

**ALL_DATANODES Restart Tests:**
- Full cluster recovery
- Block availability after full outage
- Data durability across complete storage failure

**RANDOM_DATANODE Restart Tests:**
- Non-deterministic failure scenarios
- Pipeline resilience
- Replica selection after failure

**NAMENODE_AND_DATANODES Restart Tests:**
- Full cluster restart
- Complete state recovery
- Startup coordination

### 4.4 Restart Mode Selection

| Mode | Use Case | Simulation |
|------|----------|------------|
| GRACEFUL | Normal restart, planned maintenance | Clean shutdown, orderly startup |
| CRASH | Power failure, process kill | Immediate shutdown, no cleanup |
| DELAYED_CRASH | Partial state propagation | Short delay before restart |

---

## 5. Transformation Process

### Step 1: Analyze Original Test

1. Read the complete test method
2. Identify all I/O operations (create, write, read, close, etc.)
3. Identify synchronization points (hflush, hsync)
4. Note any verification/assertion logic
5. List potential restart injection points
6. Identify the cluster variable name used in the test

### Step 2: Identify ALL Restart Configurations

For each restart point identified in Step 1:

1. Determine applicable targets based on Section 4
2. Determine applicable modes (typically GRACEFUL and CRASH for all; DELAYED_CRASH optional)
3. Generate complete list of configurations: Point × Target × Mode

**Example:** For `after_flush` restart point with file I/O operation:
```
after_flush × NAMENODE × GRACEFUL
after_flush × NAMENODE × CRASH
after_flush × NAMENODE × DELAYED_CRASH
after_flush × SINGLE_DATANODE × GRACEFUL
after_flush × SINGLE_DATANODE × CRASH
after_flush × ALL_DATANODES × GRACEFUL
after_flush × ALL_DATANODES × CRASH
after_flush × RANDOM_DATANODE × GRACEFUL
after_flush × RANDOM_DATANODE × CRASH
after_flush × NAMENODE_AND_DATANODES × GRACEFUL
after_flush × NAMENODE_AND_DATANODES × CRASH
```

This generates 11 configurations per restart point.

**IMPORTANT:** Generate ALL valid combinations, not just a few examples.

### Step 3: Generate Conditional Restart Calls

For each configuration identified in Step 2:

1. Insert `restart()` call immediately after the operation
2. Format: `restart(cluster, "position_name", RestartTarget.TARGET, RestartMode.MODE);`
3. Use snake_case for position names (e.g., `after_flush`, not `afterFlush`)
4. Group all restart() calls for the same position together
5. Do NOT add comments around restart() calls

**Example:**
```java
out.hflush();
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
restart(cluster, "after_flush", RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
restart(cluster, "after_flush", RestartTarget.ALL_DATANODES, RestartMode.CRASH);
```

### Step 4: Create New Java File with Restart Calls

Create a new Java file with `_RestartInjected.java` suffix:

1. **Copy the original file structure:**
   - Package declaration
   - Import statements
   - Class declaration
   - Instance variables
   - @Before and @After methods (if present)

2. **Add the restart framework import:**
   ```java
   import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
   ```

3. **Copy the test method and inject restart() calls:**
   - Copy the original test method exactly
   - Insert `restart()` calls at identified restart points
   - Preserve all original code, assertions, and logic

**Example:**
```java
package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

public class TestHFlush_RestartInjected {
    private MiniDFSCluster cluster;
    private FileSystem fs;

    @Before
    public void setup() throws Exception {
        Configuration conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();
        fs = cluster.getFileSystem();
    }

    @Test
    public void testHFlush() throws Exception {
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();
        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);

        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.CRASH);
    }
}
```

### Step 5: Generate Metadata JSON

Create a JSON file mapping test methods to restart configurations. This file should be saved to the `./test-restart-config` directory.

**Format:**
```json
{
  "fully.qualified.TestClassName.testMethodName": {
    "cluster_variable": "cluster",
    "restart_points": [
      {
        "position": "after_flush",
        "line_number": 48,
        "targets": ["NAMENODE", "SINGLE_DATANODE", "ALL_DATANODES"],
        "modes": ["GRACEFUL", "CRASH"]
      },
      {
        "position": "after_close",
        "line_number": 52,
        "targets": ["NAMENODE"],
        "modes": ["GRACEFUL", "CRASH"]
      }
    ]
  }
}
```

**Field descriptions:**
- `fully.qualified.TestClassName.testMethodName`: Full test identifier (package.Class.method)
- `cluster_variable`: Name of the MiniDFSCluster variable in the test (typically "cluster")
- `restart_points`: Array of restart point configurations
  - `position`: Snake_case position name
  - `line_number`: Line number where restart() calls are inserted
  - `targets`: Array of applicable RestartTarget enum values
  - `modes`: Array of applicable RestartMode enum values

---

## 6. Output Format Specification

### 6.1 Complete Java File (Part 1 of Output)

**File name:**
```
TestClassName_RestartInjected.java
```

**File location:**
Same directory as the original test file.

**File content:**
Complete Java source file with the following structure:

```java
package org.apache.hadoop.hdfs;

// All original imports
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

// Add restart framework import
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

// Class name with _RestartInjected suffix
public class TestHFlush_RestartInjected {
    // Copy all instance variables
    private MiniDFSCluster cluster;
    private FileSystem fs;

    // Copy @Before method
    @Before
    public void setup() throws Exception {
        Configuration conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();
        fs = cluster.getFileSystem();
    }

    // Copy test method and add restart() calls
    @Test
    public void testHFlush() throws Exception {
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();
        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
        restart(cluster, "after_flush", RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);

        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.CRASH);
    }
}
```

### 6.2 Metadata JSON Format (Part 2 of Output)

**File name:**
```
TestClassName.json
```

**File location:**
```
./test-restart-config/TestClassName.json
```

**JSON content:**
```json
{
  "org.apache.hadoop.hdfs.TestHFlush.testHFlush": {
    "file_path": "/Users/user/hadoop/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java",
    "cluster_variable": "cluster",
    "restart_points": [
      {
        "position": "after_flush",
        "line_number": 88,
        "targets": ["NAMENODE", "SINGLE_DATANODE", "ALL_DATANODES"],
        "modes": ["GRACEFUL", "CRASH"]
      }
    ]
  }
}
```

### 6.3 Output Structure

For each test file transformed, create TWO files:

1. **Transformed Java file:**
   ```
   src/test/java/org/apache/hadoop/hdfs/TestClassName_RestartInjected.java
   ```

2. **Metadata JSON file:**
   ```
   ./test-restart-config/TestClassName.json
   ```

---

## 7. Restart Position Naming Convention

Position names must follow these rules:
- **Format:** snake_case (lowercase with underscores)
- **Structure:** `<timing>_<operation>` (e.g., `after_flush`, `before_close`)
- **Consistency:** Use same name for same operation across all tests

### 7.1 Standard Position Names

| Position Name | When to Use | Example |
|--------------|-------------|---------|
| `after_create` | After file creation | `fs.create(path)` |
| `after_write` | After write operation | `out.write(data)` |
| `during_write` | After partial write | First half of data written |
| `after_flush` | After hflush() call | `out.hflush()` |
| `after_sync` | After hsync() call | `out.hsync()` |
| `before_close` | Before close() call | Just before `out.close()` |
| `after_close` | After close() completes | Just after `out.close()` |
| `after_append` | After append operation | `fs.append(path)` |
| `after_delete` | After delete operation | `fs.delete(path)` |
| `after_rename` | After rename operation | `fs.rename(src, dst)` |
| `during_replication` | During setReplication | `fs.setReplication(path, n)` |
| `after_write_block` | After complete block written | Full BLOCK_SIZE written |

### 7.2 Position Name Examples

**Good:**
- `after_flush` ✓
- `before_close` ✓
- `during_write` ✓

**Bad:**
- `afterFlush` ✗ (camelCase, not snake_case)
- `AFTER_FLUSH` ✗ (UPPER_CASE, not snake_case)
- `flush_restart` ✗ (doesn't follow <timing>_<operation> structure)
- `restart_point_1` ✗ (not descriptive)

---

## 8. Complete Transformation Example

### 8.1 Original Test

```java
package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TestHFlush {
    private MiniDFSCluster cluster;
    private FileSystem fs;

    @Before
    public void setup() throws Exception {
        Configuration conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();
        fs = cluster.getFileSystem();
    }

    @Test
    public void testHFlush() throws Exception {
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();

        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
    }
}
```

### 8.2 Transformed Output

**Part 1: Complete Java File (TestHFlush_RestartInjected.java)**

```java
package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

public class TestHFlush_RestartInjected {
    private MiniDFSCluster cluster;
    private FileSystem fs;

    @Before
    public void setup() throws Exception {
        Configuration conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();
        fs = cluster.getFileSystem();
    }

    @Test
    public void testHFlush() throws Exception {
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();
        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
        restart(cluster, "after_flush", RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);

        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.CRASH);
    }
}
```

**Part 2: Metadata JSON (./test-restart-config/TestHFlush.json)**

```json
{
  "org.apache.hadoop.hdfs.TestHFlush.testHFlush": {
    "file_path": "/Users/user/hadoop/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java",
    "cluster_variable": "cluster",
    "restart_points": [
      {
        "position": "after_flush",
        "line_number": 36,
        "targets": ["NAMENODE", "SINGLE_DATANODE", "ALL_DATANODES"],
        "modes": ["GRACEFUL", "CRASH"]
      },
      {
        "position": "after_close",
        "line_number": 44,
        "targets": ["NAMENODE"],
        "modes": ["GRACEFUL", "CRASH"]
      }
    ]
  }
}
```

---

## 9. Verification Checklist

After transformation, verify:

- ✓ **NEW file created** with `_RestartInjected.java` suffix (original file NOT modified)
- ✓ New file is in the same directory as the original test file
- ✓ Original test logic is EXACTLY preserved (only `restart()` calls added)
- ✓ Restart injection points are at meaningful operation boundaries
- ✓ `restart()` calls use correct format: `restart(cluster, "position", RestartTarget.X, RestartMode.Y)`
- ✓ Position names are snake_case and descriptive (e.g., `after_flush`, not `afterFlush`)
- ✓ Import statement added: `import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;`
- ✓ NO comments added anywhere in the code
- ✓ @Before and @After methods copied exactly (if present in original)
- ✓ All instance variables copied exactly
- ✓ Class name has `_RestartInjected` suffix
- ✓ Metadata JSON is valid JSON and complete
- ✓ Metadata JSON saved to `./test-restart-config` directory
- ✓ Line numbers in metadata match the transformed file
- ✓ `cluster_variable` field correctly identifies the cluster variable name
- ✓ All applicable targets and modes included for each restart point
- ✓ Test is NOT a parameterized test (those should be skipped)

---

## 10. Common Pitfalls

### 10.1 Pitfall: Adding Comments

**Wrong:**
```java
out.hflush();
// Restart NameNode gracefully after flush
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
```

**Correct:**
```java
out.hflush();
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
```

### 10.2 Pitfall: Modifying @Before/@After

**Wrong:**
Modifying setup/teardown methods is unnecessary. The cluster is passed directly to `restart()`.

**Correct:**
Do NOT modify @Before or @After. Just pass cluster to `restart()` calls.

### 10.3 Pitfall: Wrong Position Names

**Wrong:**
```java
restart(cluster, "afterFlush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // camelCase
restart(cluster, "AFTER_FLUSH", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // UPPER_CASE
```

**Correct:**
```java
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // snake_case
```

### 10.4 Pitfall: Missing Import

**Wrong:**
Only add restart() calls without checking if import is present.

**Correct:**
Always add import statement in the new file:
```java
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
```

### 10.5 Pitfall: Incomplete Metadata

**Wrong:**
```json
{
  "restart_points": [
    {
      "position": "after_flush",
      "targets": ["NAMENODE"]
    }
  ]
}
```

**Correct:**
Include ALL applicable targets and modes:
```json
{
  "restart_points": [
    {
      "position": "after_flush",
      "line_number": 48,
      "targets": ["NAMENODE", "SINGLE_DATANODE", "ALL_DATANODES"],
      "modes": ["GRACEFUL", "CRASH"]
    }
  ]
}
```

### 10.6 Pitfall: Wrong Restart Point

**Wrong:**
```java
out.write(data);
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // Wrong! No flush yet
out.hflush();
```

**Correct:**
```java
out.write(data);
out.hflush();
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // Correct position
```

### 10.7 Pitfall: Modifying Original Logic

**Wrong:**
```java
// In the new _RestartInjected.java file:
assertEquals(1024, fs.getFileStatus(path).getLen());  // Changed from original
```

**Correct:**
Never modify original assertions. Copy them exactly and only add restart() calls.

---

## 11. Output Instructions Summary

When transforming a test, you MUST create TWO files:

### File 1: Transformed Java File

**File name:** `TestClassName_RestartInjected.java`

**Location:** Same directory as the original test file

**Content:**
1. Package declaration (same as original)
2. All original imports
3. Import statement: `import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;`
4. Class declaration with `_RestartInjected` suffix
5. All instance variables (copied from original)
6. @Before and @After methods (copied from original, if present)
7. Test methods with `restart()` calls injected at identified restart points
8. All original assertions and logic preserved exactly

### File 2: Metadata JSON File

**File name:** `TestClassName.json`

**Location:** `./test-restart-config/TestClassName.json`

**Content:**
- Fully qualified test method name
- File path to original test
- cluster_variable name
- Complete restart point configurations (position, line_number, targets, modes)

**IMPORTANT:**
- Create actual files, do NOT output explanations or commentary
- The transformed Java file must be a complete, compilable Java source file
- The JSON file must be valid JSON