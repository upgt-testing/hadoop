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
2. **Identify the operation being tested**
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

---

## 5. Transformation Process

### Step 1: Analyze Original Test

1. Read the complete test method
2. Identify all valuable operations
3. Note any verification/assertion logic
4. List potential restart injection points
5. Identify the cluster variable name used in the test

### Step 2: Identify ALL Restart Configurations

For each restart point identified in Step 1:

1. Determine applicable targets based on Section 4
2. Determine applicable modes (typically GRACEFUL and CRASH for all; DELAYED_CRASH optional)
3. Generate complete list of configurations: Point × Target × Mode

### Step 3: Generate Conditional Restart Calls

For each configuration identified in Step 2:

1. Insert `restart()` call immediately after the operation
2. Format: `restart(cluster, "position_name", RestartTarget.TARGET, RestartMode.MODE);`
3. Use snake_case for position names
4. Group all restart() calls for the same position together
5. Do NOT add comments around restart() calls

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

## 6. Verification Checklist

After transformation, verify:

- ✓ **NEW file created** with `_RestartInjected.java` suffix (original file NOT modified)
- ✓ New file is in the same directory as the original test file
- ✓ Original test logic is EXACTLY preserved (only `restart()` calls added)
- ✓ Restart injection points are at meaningful operation boundaries
- ✓ `restart()` calls use correct format: `restart(cluster, "position", RestartTarget.X, RestartMode.Y)`
- ✓ Position names are snake_case and descriptive
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