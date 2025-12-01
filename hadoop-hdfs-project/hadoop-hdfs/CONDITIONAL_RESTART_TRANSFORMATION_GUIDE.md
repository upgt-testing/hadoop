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

## 2. CRITICAL DIFF GENERATION RULES (HIGHEST PRIORITY)

### ⚠️ MOST IMPORTANT: NO HALLUCINATED DIFF HUNKS

**Git patches MUST be byte-perfect or they will fail to apply. Follow these rules EXACTLY:**

#### Rule 2.0: ONLY ADD LINES - NEVER DELETE OR MODIFY

1. **You may ONLY add new lines** (lines with "+" prefix in diff hunks).
   - You MUST NOT delete lines (no "-" prefix allowed).
   - You MUST NOT modify existing lines.
   - All existing lines MUST remain byte-for-byte identical.

2. **All context lines in the diff MUST EXACTLY match the original file.**
   - No extra spaces.
   - No removed spaces.
   - No normalized indentation.
   - No tab-to-space conversions or vice versa.
   - Context lines MUST be exact character-for-character copies from the original file.

3. **The diff MUST use git unified diff format:**
   - First line:    `--- a/path/to/file`
   - Second line:   `+++ b/path/to/file`
   - Hunk header:   `@@ -old_start,old_len +new_start,new_len @@`
   - Context lines: Prefixed with single space ` `
   - Added lines:   Prefixed with `+`

4. **Hunk headers MUST be mathematically correct:**
   - `old_len` = number of context lines in the hunk (before insertion point + after insertion point)
   - `new_len` = old_len + number of inserted lines
   - Example: If you have 3 context lines before, insert 5 lines, and 3 context lines after:
     - old_len = 3 + 3 = 6
     - new_len = 6 + 5 = 11
     - Header: `@@ -X,6 +Y,11 @@`

5. **ABSOLUTELY NO hallucinated lines:**
   - Every context line (space-prefixed) MUST exist exactly as-is in the original file.
   - No lines that "look similar" to the original - they must be EXACT matches.
   - If you cannot find the exact line in the original, DO NOT generate a diff.

#### MANDATORY SELF-VERIFICATION BEFORE OUTPUT

**Before outputting any diff, you MUST internally verify:**

1. **Parse original file into lines:**
   - Read the original file content provided in the user prompt.
   - Split it into an array: L[0], L[1], L[2], ... L[N]

2. **Simulate applying each insertion:**
   - For each hunk, identify the exact line number where insertions occur.
   - Insert the new lines at the specified locations.
   - Produce a simulated UPDATED_FILE.

3. **Verify invariants:**
   - UPDATED_FILE contains exactly the old lines + only the allowed insertions.
   - No line was deleted from the original.
   - No line was modified from the original.
   - All context lines in your diff match the original lines byte-for-byte.

4. **Test the diff:**
   - Your diff, when applied with `git apply`, MUST produce EXACTLY the simulated UPDATED_FILE.
   - No extra blank lines.
   - No missing lines.
   - No changed indentation.

5. **If ANY uncertainty exists:**
   - DO NOT output a diff.
   - Instead output exactly:
     ```
     ERROR: Cannot generate valid diff. Context lines do not match original file exactly.
     ```

---

## 3. HARD RULES FOR TRANSFORMATION

### Rule 1: Modify Original File (Output Git Diff)

**DO:**
- Output a unified git diff showing changes to the original test file
- Format must be compatible with `git apply`
- Preserve original file path in diff headers

**DO NOT:**
- Output a complete Java file

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

**Part 1: Git Unified Diff**
- Compatible with `git apply` command
- Shows line-by-line changes to original file
- Includes context lines (3 before and after each change)
- Adds import statement if not already present
- Adds `restart()` calls at identified restart points

**Part 2: Metadata JSON**
- Maps test methods to restart configurations
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

## 4. Identifying Valid Restart Positions

### 4.1 Understand the Test First

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

### 4.2 High-Value Restart Points (Reference)

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

### 4.3 Code Patterns (Reference)

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

### 4.4 Invalid Restart Points (AVOID)

Do NOT inject restarts:
- Inside tight loops without synchronization
- Before any data is written
- After test assertions (test already passed/failed)
- In finally/cleanup blocks

### 4.5 Tests to SKIP (Do Not Transform)

**SKIP Parameterized Tests:**
- Tests using `@Parameterized` or `@RunWith(Parameterized.class)`
- Tests using JUnit 5 `@ParameterizedTest`
- Tests with data-driven test patterns via `@Parameters`

**Reason:** Parameterized tests already generate multiple test variants. Combining with restart injection creates combinatorial explosion and complex test management.

---

## 5. Determining Restart Targets and Modes

### 5.1 Choose Targets Based on Test Intent

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

### 5.2 Target Selection Matrix (Reference)

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

### 5.3 Restart Target Rationale (Reference)

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

### 5.4 Restart Mode Selection

| Mode | Use Case | Simulation |
|------|----------|------------|
| GRACEFUL | Normal restart, planned maintenance | Clean shutdown, orderly startup |
| CRASH | Power failure, process kill | Immediate shutdown, no cleanup |
| DELAYED_CRASH | Partial state propagation | Short delay before restart |

---

## 6. Transformation Process

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

### Step 4: Generate Git Unified Diff

Create a unified diff showing:

1. **File headers:**
   ```diff
   --- a/path/to/TestClassName.java
   +++ b/path/to/TestClassName.java
   ```

2. **Import statement** (if not already present):
   ```diff
   @@ -12,6 +12,7 @@ import org.apache.hadoop.fs.Path;
    import org.junit.Test;
   +import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
   ```

3. **Restart() calls** at each restart point:
   ```diff
   @@ -45,6 +46,9 @@ public void testHFlush() throws Exception {
        out.write(data);
        out.hflush();
   +    restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
   +    restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
   +    restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
   ```

**Format requirements:**
- Use unified diff format (compatible with `git apply`)
- Include 3 context lines before and after each change
- Preserve correct line numbers
- Use `+` prefix for added lines
- Use space prefix for context lines

### Step 5: Generate Metadata JSON

Create a JSON file mapping test methods to restart configurations.

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

## 7. Output Format Specification

### 7.1 Git Diff Format (Part 1 of Output)

**Header:**
```
TRANSFORMATION DIFF FOR: TestClassName.java
```

**Diff content:**
```diff
--- a/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java
+++ b/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java
@@ -15,6 +15,7 @@ import org.apache.hadoop.fs.Path;
 import org.junit.Before;
 import org.junit.Test;
+import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

 public class TestHFlush {
@@ -85,6 +86,11 @@ public void testHFlush() throws Exception {
     out.write(data);
     out.hflush();
+    restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
+    restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
+    restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
+    restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
+    restart(cluster, "after_flush", RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);

     assertEquals(1024, fs.getFileStatus(path).getLen());
```

### 7.2 Metadata JSON Format (Part 2 of Output)

**Header:**
```
METADATA JSON FOR: TestClassName.java
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

### 7.3 Output Structure

For each test file transformed, output BOTH parts in this order:

```
TRANSFORMATION DIFF FOR: TestClassName.java
[Git unified diff]

METADATA JSON FOR: TestClassName.java
[JSON metadata]
```

---

## 8. Restart Position Naming Convention

Position names must follow these rules:
- **Format:** snake_case (lowercase with underscores)
- **Structure:** `<timing>_<operation>` (e.g., `after_flush`, `before_close`)
- **Consistency:** Use same name for same operation across all tests

### 8.1 Standard Position Names

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

### 8.2 Position Name Examples

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

## 9. Complete Transformation Example

### 9.1 Original Test

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

### 9.2 Transformed Output

**Part 1: Git Unified Diff**

```
TRANSFORMATION DIFF FOR: TestHFlush.java

--- a/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java
+++ b/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java
@@ -11,6 +11,8 @@ import org.junit.Before;
 import org.junit.Test;

+import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
+
 import java.util.Random;

 import static org.junit.Assert.assertEquals;
@@ -43,10 +45,17 @@ public void testHFlush() throws Exception {
         byte[] data = new byte[1024];
         new Random().nextBytes(data);
         out.write(data);
         out.hflush();
+        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
+        restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.CRASH);
+        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
+        restart(cluster, "after_flush", RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
+        restart(cluster, "after_flush", RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);

         assertEquals(1024, fs.getFileStatus(path).getLen());

         out.close();
+        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
+        restart(cluster, "after_close", RestartTarget.NAMENODE, RestartMode.CRASH);
     }
 }
```

**Part 2: Metadata JSON**

```
METADATA JSON FOR: TestHFlush.java

{
  "org.apache.hadoop.hdfs.TestHFlush.testHFlush": {
    "file_path": "/Users/user/hadoop/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHFlush.java",
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
        "line_number": 54,
        "targets": ["NAMENODE"],
        "modes": ["GRACEFUL", "CRASH"]
      }
    ]
  }
}
```

---

## 10. Verification Checklist

After transformation, verify:

- ✓ **CRITICAL: All context lines in diff EXACTLY match original file (byte-for-byte, no hallucinations)**
- ✓ **CRITICAL: Diff only contains additions (+ lines), no deletions (-) or modifications**
- ✓ **CRITICAL: Hunk headers are mathematically correct (old_len, new_len)**
- ✓ Git diff is valid unified diff format (can be applied with `git apply`)
- ✓ Original test logic is EXACTLY preserved (diff shows only additions)
- ✓ Restart injection points are at meaningful operation boundaries
- ✓ `restart()` calls use correct format: `restart(cluster, "position", RestartTarget.X, RestartMode.Y)`
- ✓ Position names are snake_case and descriptive (e.g., `after_flush`, not `afterFlush`)
- ✓ Import statement added if not already present: `import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;`
- ✓ NO comments added anywhere in the code
- ✓ NO modifications to @Before or @After methods
- ✓ Metadata JSON is valid JSON and complete
- ✓ Line numbers in metadata match the diff
- ✓ `cluster_variable` field correctly identifies the cluster variable name
- ✓ All applicable targets and modes included for each restart point
- ✓ Test is NOT a parameterized test (those should be skipped)

---

## 11. Common Pitfalls

### 11.1 Pitfall: Adding Comments

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

### 11.2 Pitfall: Modifying @Before/@After

**Wrong:**
Modifying setup/teardown methods is unnecessary. The cluster is passed directly to `restart()`.

**Correct:**
Do NOT modify @Before or @After. Just pass cluster to `restart()` calls.

### 11.3 Pitfall: Wrong Position Names

**Wrong:**
```java
restart(cluster, "afterFlush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // camelCase
restart(cluster, "AFTER_FLUSH", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // UPPER_CASE
```

**Correct:**
```java
restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);  // snake_case
```

### 11.4 Pitfall: Invalid Diff Format

**Wrong:**
```
File: TestHFlush.java
Changed lines: 48-50
Add: restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
```

**Correct:**
Use standard unified diff format with `---`, `+++`, `@@` markers.

### 11.5 Pitfall: Missing Import

**Wrong:**
Only add restart() calls without checking if import is present.

**Correct:**
Add import statement in diff if not already in file:
```diff
+import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
```

### 11.6 Pitfall: Incomplete Metadata

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

### 11.7 Pitfall: Wrong Restart Point

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

### 11.8 Pitfall: Modifying Original Logic

**Wrong:**
```diff
-    assertEquals(1024, fs.getFileStatus(path).getLen());
+    assertTrue(fs.getFileStatus(path).getLen() >= 1024);  // Changed assertion
```

**Correct:**
Never modify original assertions. Only add restart() calls.

### 11.9 Pitfall: Hallucinated Diff Context Lines

**Wrong:**
```diff
@@ -45,6 +46,9 @@ public void testHFlush() throws Exception {
     byte[] data = new byte[1024];
     Random rnd = new Random();  // WRONG: This line doesn't exist in original!
     rnd.nextBytes(data);
+    restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
```

**Correct:**
Every context line (space-prefixed) must be an EXACT copy from the original file:
```diff
@@ -45,6 +46,9 @@ public void testHFlush() throws Exception {
     byte[] data = new byte[1024];
     new Random().nextBytes(data);  // Matches original exactly
     out.hflush();
+    restart(cluster, "after_flush", RestartTarget.NAMENODE, RestartMode.GRACEFUL);
```

**Key point:** If you cannot find the exact context lines in the original file, DO NOT generate a diff. Output an error instead.

---

## 12. Output Instructions Summary

When transforming a test, you MUST output:

1. **Header:** `TRANSFORMATION DIFF FOR: TestClassName.java`
2. **Git unified diff** showing:
   - Import statement (if needed)
   - restart() calls at each restart point
   - Proper line numbers and context
3. **Header:** `METADATA JSON FOR: TestClassName.java`
4. **JSON metadata** with:
   - Fully qualified test method name
   - File path
   - cluster_variable name
   - Complete restart point configurations

**Output ONLY these two parts. Do NOT include:**
- Explanations of what you did
- Suggestions for running the tests
- Additional commentary
- Code snippets outside the diff format