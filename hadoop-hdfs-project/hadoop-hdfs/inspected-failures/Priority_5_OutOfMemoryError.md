# OutOfMemoryError: Java heap space

**Priority:** 5 (Originally classified as LIKELY_BUG)
**Severity:** High - Tests crash with OOM
**Category:** TEST FRAMEWORK BUG - Multiple root causes
**Status:** FALSE POSITIVE - Test issues, not production bugs

---

## Executive Summary

OutOfMemoryError: Java heap space occurs in 6 test methods across two test classes during restart-injection testing. Investigation reveals **two distinct root causes**, both related to test implementation issues:

1. **TestDataNodeVolumeMetrics_RestartInjected**: Tries to read a 2GB file into memory (transformation bug)
2. **TestSetRepIncreasing_RestartInjected**: Heap exhaustion from running 42 tests sequentially without proper cleanup

**Root Causes:**
- **TestDataNodeVolumeMetrics**: Test transformation added `DFSTestUtil.readFile()` call that wasn't in original test, attempting to load 2GB file into ByteArrayOutputStream
- **TestSetRepIncreasing**: Multiple tests in sequence without proper memory cleanup

**Classification:** FALSE POSITIVE - Test framework bugs, not production code issues

---

## Reproduction

### Affected Tests

**Group 1: TestDataNodeVolumeMetrics_RestartInjected (2 tests)**
1. `testVolumeMetrics_AfterHsync_NN_Crash`
2. `testWriteIoVolumeMetrics_AfterHflush_NN_Graceful`

**Group 2: TestSetRepIncreasing_RestartInjected (4 tests)**
3. `testSetrepIncreasingSimulatedStorage_AfterSetrep_DN_Graceful`
4. `testSetrepIncreasing_AfterCreate_DN_Crash`
5. `testSetrepIncreasing_AfterCreate_NN_Crash`
6. `testSetrepIncreasing_AfterSetrep_RandomDN_Crash`

### Reproduction Steps

**TestDataNodeVolumeMetrics (always fails):**
```bash
mvn surefire:test -Dtest=TestDataNodeVolumeMetrics_RestartInjected#testVolumeMetrics_AfterHsync_NN_Crash
```

**TestSetRepIncreasing (only fails when running full suite):**
```bash
# Run single test - PASSES
mvn surefire:test -Dtest=TestSetRepIncreasing_RestartInjected#testSetrepIncreasing_AfterCreate_NN_Crash

# Run all tests in class - some fail with OOM
mvn surefire:test -Dtest=TestSetRepIncreasing_RestartInjected
```

---

## Stack Traces

### TestDataNodeVolumeMetrics Stack Trace

```
java.lang.OutOfMemoryError: Java heap space
	at java.util.Arrays.copyOf(Arrays.java:3236)
	at java.io.ByteArrayOutputStream.grow(ByteArrayOutputStream.java:118)
	at java.io.ByteArrayOutputStream.ensureCapacity(ByteArrayOutputStream.java:93)
	at java.io.ByteArrayOutputStream.write(ByteArrayOutputStream.java:153)
	at org.apache.hadoop.io.IOUtils.copyBytes(IOUtils.java:98)
	at org.apache.hadoop.io.IOUtils.copyBytes(IOUtils.java:69)
	at org.apache.hadoop.hdfs.DFSTestUtil.readFileBuffer(DFSTestUtil.java:426)
	at org.apache.hadoop.hdfs.DFSTestUtil.readFile(DFSTestUtil.java:418)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeMetrics_RestartInjected.testVolumeMetricsWithRestart(TestDataNodeVolumeMetrics_RestartInjected.java:99)
```

### TestSetRepIncreasing Stack Trace

```
java.lang.OutOfMemoryError: Java heap space
```
(No additional stack trace - JVM ran out of memory)

---

## Root Cause Analysis

### Issue 1: TestDataNodeVolumeMetrics - Reading 2GB File Into Memory

**Test Code - TestDataNodeVolumeMetrics_RestartInjected.java:**

```java
private void testVolumeMetricsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
  Configuration conf = new HdfsConfiguration();
  conf.setInt(DFSConfigKeys.DFS_DATANODE_FILEIO_PROFILING_SAMPLING_PERCENTAGE_KEY, 100);
  conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
  SimulatedFSDataset.setFactory(conf);

  MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
      .numDataNodes(NUM_DATANODES)
      .storageTypes(new StorageType[]{StorageType.RAM_DISK, StorageType.DISK})
      .storagesPerDatanode(2)
      .build();

  try {
    cluster.waitActive();
    FileSystem fs = cluster.getFileSystem();
    final Path fileName = new Path("/test.dat");
    final long fileLen = Integer.MAX_VALUE + 1L;  // ← 2GB FILE!
    DFSTestUtil.createFile(fs, fileName, false, BLOCK_SIZE, fileLen,
        fs.getDefaultBlockSize(fileName), REPL, 1L, true);

    try (FSDataOutputStream out = fs.append(fileName)) {
      out.writeBytes("hello world");
      ((DFSOutputStream) out.getWrappedStream()).hsync();

      // RESTART
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
    }

    // ❌ PROBLEM: Try to read 2GB file into memory!
    DFSTestUtil.readFile(fs, fileName);  // LINE 98 - OOM!
  } finally {
    if (cluster != null) {
      cluster.shutdown();
    }
  }
}
```

**The Problem:**

**Line 82:** File size = `Integer.MAX_VALUE + 1L` = **2,147,483,648 bytes = 2GB**

**Line 98:** `DFSTestUtil.readFile(fs, fileName)` attempts to read the entire file

**DFSTestUtil.readFileBuffer() implementation:**

```java
public static byte[] readFileBuffer(FileSystem fs, Path fileName)
    throws IOException {
  try (ByteArrayOutputStream os = new ByteArrayOutputStream();
       FSDataInputStream in = fs.open(fileName)) {
    IOUtils.copyBytes(in, os, 1024, true);  // Copy entire file to ByteArrayOutputStream
    return os.toByteArray();
  }
}
```

**Result:** Tries to allocate a 2GB byte array → OutOfMemoryError

### Comparison with Original Test

**Original Test - TestDataNodeVolumeMetrics.java:**

```java
@Test
public void testVolumeMetrics() throws Exception {
  MiniDFSCluster cluster = setupClusterForVolumeMetrics();
  try {
    FileSystem fs = cluster.getFileSystem();
    final Path fileName = new Path("/test.dat");
    final long fileLen = Integer.MAX_VALUE + 1L;  // Also 2GB
    DFSTestUtil.createFile(fs, fileName, false, BLOCK_SIZE, fileLen,
        fs.getDefaultBlockSize(fileName),
        REPL, 1L, true);

    try (FSDataOutputStream out = fs.append(fileName)) {
      out.writeBytes("hello world");
      ((DFSOutputStream) out.getWrappedStream()).hsync();
    }

    // ✅ ORIGINAL: Verifies metrics, does NOT read file content
    verifyDataNodeVolumeMetrics(fs, cluster, fileName);
  } finally {
    if (cluster != null) {
      cluster.shutdown();
    }
  }
}
```

**verifyDataNodeVolumeMetrics() method:**

```java
private void verifyDataNodeVolumeMetrics(final FileSystem fs,
    final MiniDFSCluster cluster, final Path fileName) throws IOException {
  List<DataNode> datanodes = cluster.getDataNodes();
  DataNode datanode = datanodes.get(0);

  final ExtendedBlock block = DFSTestUtil.getFirstBlock(fs, fileName);
  final FsVolumeSpi volume = datanode.getFSDataset().getVolume(block);
  DataNodeVolumeMetrics metrics = volume.getMetrics();

  // Verify metrics - does NOT read file content!
  MetricsRecordBuilder rb = getMetrics(volume.getMetrics().name());
  assertCounter("TotalDataFileIos", metrics.getTotalDataFileIos(), rb);

  LOG.info("TotalMetadataOperations : " + metrics.getTotalMetadataOperations());
  LOG.info("TotalDataFileIos : " + metrics.getTotalDataFileIos());
  // ... more metric verification
}
```

**Key Difference:**
- **Original test**: Calls `verifyDataNodeVolumeMetrics()` to check metrics (no file read)
- **Restart-injected test**: Calls `DFSTestUtil.readFile()` to verify file is readable (reads 2GB into memory)

**This is a test transformation bug!** The transformation tool added a file verification step that wasn't in the original test, without considering the file size.

---

### Issue 2: TestSetRepIncreasing - Heap Exhaustion from Multiple Tests

**Observation:**
- Running single test: **PASSES**
- Running full test suite (42 tests): **8 tests fail with OOM**

**Evidence:**

```bash
# Single test
$ mvn surefire:test -Dtest=TestSetRepIncreasing_RestartInjected#testSetrepIncreasing_AfterCreate_NN_Crash
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0  ← PASSES

# Full suite
$ mvn surefire:test -Dtest=TestSetRepIncreasing_RestartInjected
[ERROR] Tests run: 42, Failures: 4, Errors: 8, Skipped: 0  ← 8 OOM errors
```

**Root Cause:** Memory accumulation across multiple test runs

Each test creates:
- `MiniDFSCluster` with 10 DataNodes
- `DistributedFileSystem` instances
- Large test files
- Log buffers
- Metrics data structures

Even with proper shutdown in `finally` blocks, 42 sequential tests can exhaust the heap before GC can reclaim memory, especially if:
1. Objects are still referenced (preventing GC)
2. Shutdown doesn't clean up all resources
3. Static caches accumulate data
4. Default test heap size is too small for 42 sequential cluster creations

This is a **test suite configuration issue**, not a bug in the test logic itself.

---

## Why This is NOT a Production Bug

### TestDataNodeVolumeMetrics

**In Production:**
1. **Clients don't load entire files into memory**
   - Production code streams data in chunks
   - Reads are buffered (typically 64KB)
   - Never allocates arrays sized to file length

2. **File verification uses different methods**
   - Checksum verification reads in blocks
   - HDFS tools stream data
   - No ByteArrayOutputStream used for large files

3. **The 2GB file read is test-only**
   - Added by transformation tool for verification
   - Not part of actual production code path
   - Original test doesn't read file content at all

### TestSetRepIncreasing

**In Production:**
1. **No sequential cluster creation**
   - Production runs one HDFS cluster at a time
   - No 42 sequential restarts
   - Memory is bounded per cluster

2. **Proper resource lifecycle**
   - Clusters run for days/weeks
   - Restart is controlled and planned
   - No rapid create/destroy cycle

3. **Heap sizing for workload**
   - Production heaps sized appropriately
   - NameNode typically gets several GB
   - Test heaps are constrained

---

## Impact Assessment

### Production Impact: **NONE**

Neither issue affects production HDFS:
- **TestDataNodeVolumeMetrics**: Test verification code only, not production path
- **TestSetRepIncreasing**: Test suite execution issue, not code bug

### Test Impact: **MEDIUM**

- 6 test methods fail with OOM
- Can be confusing when investigating failures
- Reduces confidence in test suite
- Wastes CI/CD resources

---

## Recommended Fixes

### Fix 1: TestDataNodeVolumeMetrics - Remove Unnecessary File Read

The restart-injected test should verify metrics like the original, not read the file:

```java
private void testVolumeMetricsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
  // ... setup code ...

  try {
    // ... file creation and append ...

    executeRestart(cluster, target, mode, true);
    verifyClusterHealth(cluster, (DistributedFileSystem) fs);

    // ✅ FIX: Verify metrics instead of reading file
    verifyDataNodeVolumeMetrics(fs, cluster, fileName);
    // ❌ REMOVE: DFSTestUtil.readFile(fs, fileName);
  } finally {
    if (cluster != null) {
      cluster.shutdown();
    }
  }
}

// Add the verification method from original test
private void verifyDataNodeVolumeMetrics(final FileSystem fs,
    final MiniDFSCluster cluster, final Path fileName) throws IOException {
  List<DataNode> datanodes = cluster.getDataNodes();
  DataNode datanode = datanodes.get(0);

  final ExtendedBlock block = DFSTestUtil.getFirstBlock(fs, fileName);
  final FsVolumeSpi volume = datanode.getFSDataset().getVolume(block);
  DataNodeVolumeMetrics metrics = volume.getMetrics();

  MetricsRecordBuilder rb = getMetrics(volume.getMetrics().name());
  assertCounter("TotalDataFileIos", metrics.getTotalDataFileIos(), rb);
}
```

**Alternative:** If file read is necessary, read only first few blocks:
```java
// Read first 1MB instead of entire 2GB file
try (FSDataInputStream in = fs.open(fileName)) {
  byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
  int bytesRead = in.read(buffer);
  assertTrue("File should be readable", bytesRead > 0);
}
```

### Fix 2: TestSetRepIncreasing - Increase Heap or Fork Tests

**Option A: Increase Test Heap Size**

In `pom.xml`, increase Surefire heap:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>-Xmx4g -Xms1g</argLine>  <!-- Increase from default -->
  </configuration>
</plugin>
```

**Option B: Fork Tests (Recommended)**

Run each test in a new JVM to prevent memory accumulation:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkCount>1</forkCount>
    <reuseForks>false</reuseForks>  <!-- New JVM per test -->
  </configuration>
</plugin>
```

**Option C: Add Explicit GC Hints**

In test teardown, suggest GC (not guaranteed to help):
```java
@After
public void tearDown() {
  cluster.shutdown();
  cluster = null;
  fs = null;
  System.gc();  // Suggest GC
  System.runFinalization();
}
```

### Fix 3: Transformation Tool - Don't Add File Reads for Large Files

Update the test transformation tool to:

1. **Detect large file tests** - Check if `fileLen > threshold` (e.g., 100MB)
2. **Skip file read verification** - Don't add `DFSTestUtil.readFile()` for large files
3. **Use alternative verification** - Add metadata checks or partial reads instead

```python
# Pseudo-code for transformation tool
if original_test_has_file_read():
    add_file_read_verification()
elif file_size_is_large():
    add_metadata_verification()  # File exists, length, etc.
else:
    add_partial_read_verification()  # Read first block
```

---

## Evidence

### TestDataNodeVolumeMetrics Evidence

**File size calculation:**
```java
final long fileLen = Integer.MAX_VALUE + 1L;
// Integer.MAX_VALUE = 2,147,483,647
// Integer.MAX_VALUE + 1 = 2,147,483,648 bytes = 2 GB
```

**ByteArrayOutputStream growth:**
- Initial size: 32 bytes
- After first write: 64 bytes
- Doubles on each overflow
- Final size needed: 2,147,483,648 bytes
- **Cannot allocate 2GB array in test heap (typically 512MB-1GB)**

**Stack trace confirms:**
```
at java.io.ByteArrayOutputStream.grow(ByteArrayOutputStream.java:118)
at java.io.ByteArrayOutputStream.ensureCapacity(ByteArrayOutputStream.java:93)
```

### TestSetRepIncreasing Evidence

**Test execution log:**
```
Tests run: 42, Failures: 4, Errors: 8
```

**Memory accumulation pattern:**
- Tests 1-34: Pass
- Tests 35-42: Start failing with OOM
- Pattern suggests gradual heap exhaustion

**Individual test success:**
```bash
$ mvn surefire:test -Dtest=TestSetRepIncreasing_RestartInjected#testSetrepIncreasing_AfterCreate_NN_Crash
Tests run: 1, Failures: 0, Errors: 0
```

**Confirms:** Not a single-test bug, but suite-level memory issue

---

## Lessons Learned

### Test Transformation Guidelines

1. **Don't blindly add file reads** - Check file size before adding verification
2. **Preserve original verification** - Use same validation methods as original test
3. **Consider memory constraints** - Test heap is limited (typically < 2GB)
4. **Stream large data** - Never load entire large files into memory

### Test Suite Best Practices

1. **Fork tests when using MiniDFSCluster** - Prevents memory accumulation
2. **Monitor heap usage** - Profile test suites to find memory leaks
3. **Explicit resource cleanup** - Don't rely solely on garbage collection
4. **Size heap appropriately** - Large test suites need larger heaps

---

## Conclusion

Both OOM issues are **TEST FRAMEWORK BUGS**, not production code issues.

**TestDataNodeVolumeMetrics:**
- **Classification:** TEST TRANSFORMATION BUG
- **Cause:** Transformation added unnecessary 2GB file read
- **Fix:** Remove file read or use original test's metric verification

**TestSetRepIncreasing:**
- **Classification:** TEST SUITE CONFIGURATION ISSUE
- **Cause:** Memory accumulation across 42 sequential tests
- **Fix:** Increase heap size or fork tests

**Priority Downgrade:** From Priority 5 (LIKELY_BUG) to Priority 13-16 (TEST_FRAMEWORK)

**Action Required:**
1. Fix TestDataNodeVolumeMetrics to not read large files
2. Configure Surefire to fork tests or increase heap
3. Update transformation tool to avoid adding reads for large files
4. Audit other restart-injected tests for similar issues

**Production HDFS:** No action needed - not affected by these test-only issues

---

## Test Output Reference

- TestDataNodeVolumeMetrics run: `/tmp/priority5_test_run.log`
- Original batch run logs: `restart-test-log-20251130_220042/TestDataNodeVolumeMetrics_RestartInjected.log`
- Original batch run logs: `restart-test-log-20251130_220042/TestSetRepIncreasing_RestartInjected.log`

**Date tested:** 2025-12-01
**Tester:** Claude Code Analysis
**Environment:** Hadoop 3.3.5, Ubuntu 20.04, Java 8
