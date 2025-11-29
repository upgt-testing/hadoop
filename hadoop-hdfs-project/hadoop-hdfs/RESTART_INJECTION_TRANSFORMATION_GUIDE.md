# HDFS MiniDFSCluster Test Restart Injection Transformation Guide

This document provides comprehensive guidance for transforming existing MiniDFSCluster tests into restart-injected crash tests using the `RestartInjectionFramework`.

---

## 1. Overview

### 1.1 Purpose
Transform existing HDFS MiniDFSCluster tests to systematically inject component restarts at various points, enabling discovery of bugs related to:
- State recovery and persistence
- EditLog replay
- Lease recovery
- Block recovery
- Pipeline recovery
- Data durability

### 1.2 Framework Components

```java
// RestartInjectionFramework provides:
RestartPoint   // WHERE to restart (AFTER_WRITE_BLOCK, DURING_WRITE, AFTER_FLUSH, etc.)
RestartTarget  // WHAT to restart (NAMENODE, SINGLE_DATANODE, ALL_DATANODES, etc.)
RestartMode    // HOW to restart (GRACEFUL, CRASH, DELAYED_CRASH)

// Helper methods:
executeRestart(cluster, target, mode, waitActive)
verifyClusterHealth(cluster, fs)
verifyFileIntegrity(fs, path, expectedLength, expectedData)
verifyNoDataLoss(fs, path, expectedLength)
waitForLeaseRecovery(cluster, timeoutMs)
getLeaseCount(cluster)
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

### Rule 2: Copy-Paste Original Test Method Without Modification
The original test logic MUST remain **exactly unchanged**. The ONLY modifications allowed are:
- Adding restart injection calls (`executeRestart()`)
- Adding verification calls after restart (`verifyClusterHealth()`, etc.)
- Adding necessary imports

**DO NOT:**
- Change variable names
- Modify assertion logic
- Alter test data or parameters
- Refactor or "improve" the original code
- Change the order of original operations

### Rule 3: One Original Test = Multiple Transformed Tests
Each original test method can generate multiple transformed versions based on:
- Different restart positions within the test
- Different restart targets (NameNode vs DataNode)
- Different restart modes (GRACEFUL vs CRASH)

### Rule 4: Preserve Test Semantics
The transformed test must validate the **same invariants** as the original, plus:
- Data durability across restart
- State consistency after recovery
- Operation completion after restart

---

## 3. Identifying Valid Restart Positions

### 3.1 High-Value Restart Points

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

### 3.2 Identifying Restart Points in Code

Look for these code patterns as restart injection points:

```java
// Pattern 1: After flush operations
out.hflush();
// <-- RESTART POINT: AFTER_FLUSH

out.hsync();
// <-- RESTART POINT: AFTER_SYNC

// Pattern 2: Between write operations
AppendTestUtil.write(out, 0, halfSize);
// <-- RESTART POINT: DURING_WRITE (partial data written)
AppendTestUtil.write(out, halfSize, halfSize);

// Pattern 3: Before/After close
out.hflush();
// <-- RESTART POINT: BEFORE_CLOSE
out.close();
// <-- RESTART POINT: AFTER_CLOSE

// Pattern 4: After complete block written
AppendTestUtil.write(out, 0, BLOCK_SIZE);  // Complete block
out.hflush();
// <-- RESTART POINT: AFTER_WRITE_BLOCK

// Pattern 5: During metadata operations
fs.setReplication(path, newReplication);
// <-- RESTART POINT: DURING_REPLICATION

fs.append(path);
// <-- RESTART POINT: DURING_APPEND

fs.delete(path, recursive);
// <-- RESTART POINT: DURING_DELETE

fs.rename(src, dst);
// <-- RESTART POINT: DURING_RENAME
```

### 3.3 Invalid Restart Points (AVOID)

Do NOT inject restarts:
- Inside tight loops without synchronization
- Before any data is written
- After test assertions (test already passed/failed)
- In finally/cleanup blocks

### 3.4 Tests to SKIP (Do Not Transform)

**SKIP Parameterized Tests:**
- Tests using `@Parameterized` or `@RunWith(Parameterized.class)`
- Tests using JUnit 5 `@ParameterizedTest`
- Tests with data-driven test patterns via `@Parameters`

**Reason:** Parameterized tests already generate multiple test variants. Combining with restart injection creates combinatorial explosion and complex test management.


---

## 4. Determining Restart Targets

### 4.1 Target Selection Matrix

| Operation Type | NameNode Restart | DataNode Restart | Both |
|----------------|------------------|------------------|------|
| File create/write | YES | YES | YES |
| hflush/hsync | YES | YES | NO |
| File close | YES | YES | YES |
| File read | YES | YES | NO |
| Append | YES | YES | YES |
| Delete | YES | NO | NO |
| Rename | YES | NO | NO |
| Replication change | YES | YES | YES |
| Block operations | YES | YES | YES |

### 4.2 Restart Target Rationale

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

### 4.3 Restart Mode Selection

| Mode | Use Case | Simulation |
|------|----------|------------|
| GRACEFUL | Normal restart, planned maintenance | Clean shutdown, orderly startup |
| CRASH | Power failure, process kill | Immediate shutdown, no cleanup |
| DELAYED_CRASH | Partial state propagation | Short delay before restart |

---

## 5. Transformation Process

### Step 1: Analyze Original Test

1. Read the original test method completely
2. Identify all I/O operations (create, write, read, close, etc.)
3. Identify synchronization points (hflush, hsync)
4. Note any verification/assertion logic
5. List potential restart injection points

### Step 2: Generate ALL Test Variants

**IMPORTANT: Generate ALL valid combinations.** For each identified restart point, generate test variants for ALL applicable target and mode combinations:

```
Restart Point x Restart Target x Restart Mode = Test Variant
```

**Required Targets:**
- NAMENODE (always applicable)
- SINGLE_DATANODE (for I/O operations)
- ALL_DATANODES (for I/O operations)
- RANDOM_DATANODE (for I/O operations)
- NAMENODE_AND_DATANODES (for critical operations)

**Required Modes:**
- GRACEFUL (always)
- CRASH (always)
- DELAYED_CRASH (optional, for timing-sensitive scenarios)

Example for a test with one restart point after hflush():
```
AFTER_FLUSH x NAMENODE x GRACEFUL
AFTER_FLUSH x NAMENODE x CRASH
AFTER_FLUSH x NAMENODE x DELAYED_CRASH
AFTER_FLUSH x SINGLE_DATANODE x GRACEFUL
AFTER_FLUSH x SINGLE_DATANODE x CRASH
AFTER_FLUSH x SINGLE_DATANODE x DELAYED_CRASH
AFTER_FLUSH x ALL_DATANODES x GRACEFUL
AFTER_FLUSH x ALL_DATANODES x CRASH
AFTER_FLUSH x RANDOM_DATANODE x GRACEFUL
AFTER_FLUSH x RANDOM_DATANODE x CRASH
AFTER_FLUSH x NAMENODE_AND_DATANODES x GRACEFUL
AFTER_FLUSH x NAMENODE_AND_DATANODES x CRASH
```

This generates 12+ variants per restart point. For tests with multiple restart points, generate ALL combinations for EACH point.

### Step 3: Create NEW Transformed Test File

**Do NOT modify the original test file.** Create a new file named `[OriginalTestClass]_RestartInjected.java` in the same directory.

```java
/**
 * Restart-injected version of [OriginalTestClass].
 *
 * Original test: [OriginalTestClass]#[originalMethodName]
 * This file contains all restart injection variants for the original test.
 */
public class [OriginalTestClass]_RestartInjected {
    // ... setup identical to original ...
}
```

### Step 4: Apply Transformation Template

```java
@Test(timeout = 120000)  // Increase timeout for restart overhead
public void test[OriginalName]_[RestartPoint]_[Target]_[Mode]() throws Exception {
    // === PHASE 1: Original code up to restart point (COPY EXACTLY) ===
    [original code before restart point]

    // === PHASE 2: RESTART INJECTION ===
    LOG.info("Injecting {} restart of {} at {}",
        RestartMode.[MODE], RestartTarget.[TARGET], RestartPoint.[POINT]);
    executeRestart(cluster, RestartTarget.[TARGET], RestartMode.[MODE], true);

    // === PHASE 3: Post-restart verification (BASIC) ===
    verifyClusterHealth(cluster, fs);

    // === PHASE 4: Original code after restart point (COPY EXACTLY) ===
    [original code after restart point]

    // === PHASE 5: Original assertions (COPY EXACTLY, no additions) ===
    [original assertions - unchanged]
}
```

**Note:** Verification is kept basic - only `verifyClusterHealth()` after restart, plus the original test assertions. Do NOT add additional verification calls.

### Step 5: Handle Special Cases

**Case A: Client stream must survive restart**
```java
@Before
public void setup() {
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);  // Keep client connection alive across NN restart
}
```

**Case B: File was not closed before restart**
```java
// After NN restart, client stream may need reconnection
// The framework handles this, but verification should account for lease recovery
waitForLeaseRecovery(cluster, 30000);
```

**Case C: DataNode restart during write**
```java
// Pipeline may need recovery - ensure waitActive
executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
cluster.waitActive();
// May need to trigger pipeline recovery by continuing write
```

---

## 6. Naming Conventions

### 6.1 Test File and Class Naming

**IMPORTANT: Do NOT modify the original test file.** Instead, create a NEW file with the `_RestartInjected.java` suffix.

```
[OriginalClassName]_RestartInjected.java
```

Example:
- Original: `TestHFlush.java` (DO NOT MODIFY)
- New file: `TestHFlush_RestartInjected.java`

The new file should be placed in the same directory as the original test file.

### 6.2 Test Method Naming
```
test[OriginalMethodName]_[RestartPoint]_[Target]_[Mode]
```
Examples:
```java
testHFlush_AfterFlush_NameNode_Graceful()
testHFlush_AfterFlush_NameNode_Crash()
testHFlush_AfterFlush_SingleDataNode_Graceful()
testHFlush_AfterSync_NameNode_Crash()
```

### 6.3 Abbreviated Naming (for long names)
```
[Original]_[Point]_NN_G      // NameNode Graceful
[Original]_[Point]_NN_C      // NameNode Crash
[Original]_[Point]_DN_G      // DataNode Graceful
[Original]_[Point]_DN_C      // DataNode Crash
[Original]_[Point]_ALL_G     // All DataNodes Graceful
```

---

## 7. Complete Transformation Example

### Original Test (TestHFlush.java):
```java
@Test
public void testHFlushBasic() throws IOException {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
        FileSystem fs = cluster.getFileSystem();
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();

        // Verify data is visible
        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
    } finally {
        cluster.shutdown();
    }
}
```

### Transformed Test (TestHFlush_RestartInjected.java):
```java
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

public class TestHFlush_RestartInjected {

    private static final Logger LOG =
        LoggerFactory.getLogger(TestHFlush_RestartInjected.class);

    private MiniDFSCluster cluster;
    private FileSystem fs;
    private Configuration conf;

    @Before
    public void setup() throws Exception {
        conf = new HdfsConfiguration();
        // Allow client to survive NN restart
        conf.setInt(
            CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
            0);
        cluster = new MiniDFSCluster.Builder(conf).build();
        cluster.waitActive();
        fs = cluster.getFileSystem();
    }

    @After
    public void teardown() {
        if (cluster != null) {
            cluster.shutdown();
        }
    }

    /**
     * Original: TestHFlush#testHFlushBasic
     * Restart: After hflush(), NameNode, Graceful
     */
    @Test(timeout = 120000)
    public void testHFlushBasic_AfterFlush_NN_Graceful() throws Exception {
        LOG.info("=== Starting testHFlushBasic_AfterFlush_NN_Graceful ===");

        // === ORIGINAL CODE START (UNCHANGED) ===
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();
        // === ORIGINAL CODE PAUSE ===

        // === RESTART INJECTION ===
        executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
        verifyClusterHealth(cluster, fs);

        // === ORIGINAL CODE RESUME (UNCHANGED) ===
        // Verify data is visible
        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
        // === ORIGINAL CODE END ===
    }

    /**
     * Original: TestHFlush#testHFlushBasic
     * Restart: After hflush(), NameNode, Crash
     */
    @Test(timeout = 120000)
    public void testHFlushBasic_AfterFlush_NN_Crash() throws Exception {
        LOG.info("=== Starting testHFlushBasic_AfterFlush_NN_Crash ===");

        // === ORIGINAL CODE START (UNCHANGED) ===
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();
        // === ORIGINAL CODE PAUSE ===

        // === RESTART INJECTION ===
        executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
        verifyClusterHealth(cluster, fs);

        // === ORIGINAL CODE RESUME (UNCHANGED) ===
        // Verify data is visible
        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
        // === ORIGINAL CODE END ===
    }

    /**
     * Original: TestHFlush#testHFlushBasic
     * Restart: After hflush(), SingleDataNode, Graceful
     */
    @Test(timeout = 120000)
    public void testHFlushBasic_AfterFlush_DN_Graceful() throws Exception {
        LOG.info("=== Starting testHFlushBasic_AfterFlush_DN_Graceful ===");

        // === ORIGINAL CODE START (UNCHANGED) ===
        Path path = new Path("/test");
        FSDataOutputStream out = fs.create(path, (short)3);

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        out.write(data);
        out.hflush();
        // === ORIGINAL CODE PAUSE ===

        // === RESTART INJECTION ===
        executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
        verifyClusterHealth(cluster, fs);

        // === ORIGINAL CODE RESUME (UNCHANGED) ===
        // Verify data is visible
        assertEquals(1024, fs.getFileStatus(path).getLen());

        out.close();
        // === ORIGINAL CODE END ===
    }

    // ... Continue generating ALL remaining combinations:
    // testHFlushBasic_AfterFlush_DN_Crash()
    // testHFlushBasic_AfterFlush_AllDN_Graceful()
    // testHFlushBasic_AfterFlush_AllDN_Crash()
    // testHFlushBasic_AfterFlush_RandomDN_Graceful()
    // testHFlushBasic_AfterFlush_RandomDN_Crash()
    // testHFlushBasic_AfterFlush_NNAndDN_Graceful()
    // testHFlushBasic_AfterFlush_NNAndDN_Crash()
    // ... and DELAYED_CRASH variants as applicable
}
```

---

## 8. Verification Checklist

After transformation, verify:

- [ ] **NEW file created** with `_RestartInjected.java` suffix (original file NOT modified)
- [ ] Original test logic is **EXACTLY** preserved (diff should only show additions)
- [ ] Restart injection point is at a meaningful location
- [ ] `verifyClusterHealth()` is called after restart (basic verification only)
- [ ] Test timeout is increased to account for restart overhead (typically 2x)
- [ ] Client configuration allows survival across NN restart if needed
- [ ] Original assertions remain UNCHANGED
- [ ] Test method name follows naming convention
- [ ] JavaDoc references original test and documents restart parameters
- [ ] Test is NOT a parameterized test (skip if it is)

---

## 9. Priority Test Candidates

Based on codebase analysis, prioritize transformation of:

1. **TestPersistBlocks.java** - Block persistence across restart
2. **TestHFlush.java** - Pipeline and flush semantics
3. **TestFileAppend3.java** - Append recovery
4. **TestReplication.java** - Replication consistency
5. **TestMultiThreadedHflush.java** - Concurrent flush under restart

---

## 10. Common Pitfalls

1. **Modifying original file**: NEVER edit the original test file - create a new `_RestartInjected.java` file
2. **Modifying original code**: Never change the original test logic within transformed methods
3. **Wrong restart point**: Ensure restart is at a meaningful operation boundary
4. **Missing waitActive**: Always wait for cluster to stabilize after restart
5. **Insufficient timeout**: Restart adds overhead; increase test timeout
6. **Ignoring lease recovery**: For open files, may need to wait for lease recovery
7. **Not refreshing FileSystem**: After NN restart, may need fresh FS reference
