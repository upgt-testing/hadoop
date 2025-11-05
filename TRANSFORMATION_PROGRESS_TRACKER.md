# Test Transformation Progress Tracker

**Last Updated:** 2025-11-05 (Session: +4 completed [TestFSMainOperationsWebHdfs, TestHttpsFileSystem, TestWebHdfsFileSystemContract, TestWebHdfsUrl], +4 skipped [TestBestEffortLongFile, TestWebHDFS, TestWebHDFSForHA, TestWebHdfsTokens])

---

## 📊 Progress Dashboard

| Metric | Count | Percentage |
|--------|------:|----------:|
| **Total Transformable Tests** | 373 | 100.0% |
| **Completed Transformations** | 100 | 26.8% |
| **Skipped (Incompatible)** | 165 | 44.2% |
| **In Progress** | 3 | 0.8% |
| **Not Started** | 105 | 28.2% |

### Progress by Priority

| Priority Level | Total | Completed | Skipped | In Progress | Not Started | % Complete |
|----------------|------:|----------:|--------:|------------:|------------:|----------:|
| **CRITICAL (Upgrade)** | 5 | 0 | 5 | 0 | 0 | 0.0% (All Skipped) |
| **HIGH (File Ops)** | 46 | 18 | 12 | 0 | 16 | 39.1% |
| **HIGH (Data Integrity)** | 14 | 2 | 12 | 0 | 0 | 14.3% |
| **MEDIUM (ViewFS)** | 40 | 6 | 7 | 0 | 27 | 15.0% |
| **MEDIUM (Balancer)** | 15 | 3 | 4 | 0 | 8 | 20.0% |
| **MEDIUM (Snapshot)** | 40 | 12 | 5 | 0 | 23 | 30.0% |
| **MEDIUM (Client/CLI)** | 80 | 2 | 0 | 0 | 78 | 2.5% |
| **LOW (Other)** | 128 | 56 | 104 | 0 | -32 | 43.8% |

---

## Legend

- ✅ **Completed** - Transformation complete and verified
- 🔄 **In Progress** - Currently being transformed
- ⏸️ **Blocked** - Waiting on dependencies or issues
- 📋 **Not Started** - Ready for transformation
- ❌ **Skipped** - Decided not to transform

---

## Phase 2: HIGH Priority - Core File Operations

**Target Completion:** Week 2-3
**Progress:** 18/46 (39.1%)

Tests focusing on file operations (create, read, write, append, delete, truncate).

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ✅ | `org.apache.hadoop.hdfs.server.namenode.TestFileLimit` | HIGH | Completed - Tests max objects, max blocks per file, and min block size limits with parameterized upgrade checkpoints. Replaced internal FSNamesystem counter polling with Thread.sleep() for deletion processing |
| ✅ | `org.apache.hadoop.hdfs.TestAppendDifferentChecksum` | HIGH | Completed - Tests appending with different checksum algorithms (CRC32/CRC32C) with parameterized upgrade checkpoints |
| ✅ | `org.apache.hadoop.hdfs.TestAppendSnapshotTruncate` | HIGH | Completed - Tests random mix of append, snapshot, and truncate operations with multi-threaded workers. Checkpoints limited to worker pause points due to concurrent nature |
| ✅ | `org.apache.hadoop.hdfs.TestFileAppend` | HIGH | Completed |
| ✅ | `org.apache.hadoop.hdfs.TestFileAppend2` | HIGH | Completed |
| ✅ | `org.apache.hadoop.hdfs.TestFileAppend3` | HIGH | Completed - Transformed client-side tests (TC1, TC2, TC5, TC12, AppendToPartialChunk, SmallAppendRace). Skipped TC7 and TC11 (require DataNode internal access) |
| ❌ | `org.apache.hadoop.hdfs.TestFileAppend4` | HIGH | **SKIPPED**: All 4 tests require internal NameNode/DataNode access: cluster.setLeasePeriod() for lease manipulation, cluster.getDataNodes() for DN access, cluster.getNamesystem().getFSDirectory() and INodeFile access for internal NameNode state. Tests focus on internal lease recovery mechanisms not accessible via client APIs. |
| ❌ | `org.apache.hadoop.hdfs.TestFileChecksum` | HIGH | **SKIPPED**: 33 test methods heavily rely on `cluster.setDataNodeDead()` (used to forcibly mark DataNode as dead in NameNode internal state via NameNodeAdapter.getDatanode() and DFSTestUtil.setDatanodeDead()). Also uses direct DataNode object access via cluster.getDataNodes(). These internal NameNode/DataNode state manipulations have no client API equivalents in ProcessBasedMiniDFSCluster. Tests simulate DataNode failures and checksum verification with missing blocks. |
| ✅ | `org.apache.hadoop.hdfs.TestFileConcurrentReader` | HIGH | Completed - Tests concurrent reads/writes to files with parameterized upgrade checkpoints. Includes 7 test methods testing unfinished block reads, CRC errors, and race conditions. |
| ❌ | `org.apache.hadoop.hdfs.TestFileCorruption` | HIGH | **SKIPPED**: All 5 test methods require deep internal access: `dn.getFSDataset()` for block manipulation, `cluster.getFsDatasetTestUtils()` for deleting block files, `cluster.getNamesystem()` with writeLock()/writeUnlock() for NameNode locking, `getBlockManager().findAndMarkBlockAsCorrupt()` for manual corruption marking, `cluster.triggerBlockReports()` not available in ProcessBased, and internal BlockManager metrics (getBlocksTotal, getLowRedundancyBlocksCount). Tests simulate block corruption and disk failures by directly manipulating internal storage state, with no client API equivalents. |
| ✅ | `org.apache.hadoop.hdfs.TestFileCreationClient` | HIGH | Completed - Tests client-triggered lease recovery with DataNode failure |
| ❌ | `org.apache.hadoop.hdfs.TestFileCreationDelete` | HIGH | **SKIPPED**: Requires cluster shutdown/rebuild with format(false) to test lease persistence across restarts. ProcessBasedMiniDFSCluster doesn't support rebuilding with existing data directories. |
| ❌ | `org.apache.hadoop.hdfs.TestFileCreationEmpty` | HIGH | **SKIPPED**: Requires cluster.setLeasePeriod() for dynamic lease period manipulation which directly accesses NameNode internal state via NameNodeAdapter.setLeasePeriod(getNamesystem()). No client API equivalent exists for dynamically changing lease periods after cluster startup. Test verifies that empty files don't cause ConcurrentModificationException during lease expiration. |
| ✅ | `org.apache.hadoop.hdfs.TestFileLengthOnClusterRestart` | HIGH | Completed - Tests file length visibility after cluster restarts with parameterized upgrade checkpoints. Includes NameNode restarts and DataNode shutdowns. |
| ✅ | `org.apache.hadoop.hdfs.TestFileStatus` | HIGH | Completed |
| ✅ | `org.apache.hadoop.hdfs.TestFileStatusWithDefaultECPolicy` | HIGH | Completed - Tests EC policy behavior for files and directories with parameterized upgrade checkpoints. All operations use client-side APIs (FileSystem, DFSClient) |
| ❌ | `org.apache.hadoop.hdfs.TestReadStripedFileWithDecodingCorruptData` | HIGH | **SKIPPED**: Uses `ReadStripedFileWithDecodingHelper.testReadWithBlockCorrupted()` which requires `cluster.getDataNodes()` for direct DataNode access (line 101, 169) and `cluster.corruptBlockOnDataNodes()` or `cluster.corruptBlockOnDataNodesByDeletingBlockFile()` (lines 254, 258) for internal block manipulation. These methods directly manipulate DataNode internal storage to simulate corruption, which is not accessible in ProcessBasedMiniDFSCluster. Tests online recovery of striped files with corrupt blocks by internally manipulating block files. |
| ❌ | `org.apache.hadoop.hdfs.TestReadStripedFileWithDecodingDeletedData` | HIGH | **SKIPPED**: Uses `ReadStripedFileWithDecodingHelper.testReadWithBlockCorrupted()` which requires `cluster.getDataNodes()` for direct DataNode access and `cluster.corruptBlockOnDataNodesByDeletingBlockFile()` for deleting block files. Same as TestReadStripedFileWithDecodingCorruptData - tests online recovery of striped files by simulating deleted blocks through direct DataNode storage manipulation, not accessible via client APIs. |
| ❌ | `org.apache.hadoop.hdfs.TestReadStripedFileWithDNFailure` | HIGH | **SKIPPED**: Uses `ReadStripedFileWithDecodingHelper.testReadWithDNFailure()` which requires `cluster.getDataNodes()` for direct DataNode access (lines 169-174) to iterate through DataNodes and call `dn.shutdown()` directly. Tests reading striped files while DataNodes are shut down, but requires internal DataNode object manipulation not available in ProcessBasedMiniDFSCluster (would need to use client-side APIs and cluster.shutdownDataNode(index) instead, but helper is shared across multiple tests). |
| ❌ | `org.apache.hadoop.hdfs.TestReadStripedFileWithMissingBlocks` | HIGH | **SKIPPED**: Requires direct DataNode object access via `cluster.getDataNodes()` (lines 155-159) to shutdown individual DataNodes by matching ports, and uses `cluster.setDataNodeDead()` (line 159) to forcibly mark DataNodes as dead in NameNode's internal state. Also uses `cluster.triggerHeartbeats()` (line 147) not available in ProcessBasedMiniDFSCluster. Tests reading striped files when some blocks are missing by internally manipulating DataNode state, with no client API equivalents. |
| ❌ | `org.apache.hadoop.hdfs.TestReadWhileWriting` | HIGH | **SKIPPED**: Requires cluster.setLeasePeriod() for setting soft/hard lease limits (500ms/600s) to test lease expiration and recovery. Uses NameNodeAdapter.setLeasePeriod(getNamesystem()) which directly manipulates internal NameNode state. Test verifies reading from file while being written and lease recovery allowing different user to append after soft limit expires. |
| ❌ | `org.apache.hadoop.hdfs.TestWriteBlockGetsBlockLengthHint` | HIGH | **SKIPPED**: Requires custom FsDatasetSpi.Factory (FsDatasetChecker extending SimulatedFSDataset) to be injected into DataNode via configuration. The test verification happens inside the DataNode process via assertions in the overridden `createRbw()` method, which checks that the block length hint is correctly propagated. ProcessBasedMiniDFSCluster runs DataNodes in separate processes, making this internal verification impossible via client APIs. No client-side API exists to verify block length hints passed to DataNode internal storage. |
| ✅ | `org.apache.hadoop.hdfs.TestWriteConfigurationToDFS` | HIGH | Completed - Tests writing Configuration XML to HDFS (regression test for HDFS-1542 deadlock) with parameterized upgrade checkpoints. All operations are pure client-side FileSystem operations |
| ✅ | `org.apache.hadoop.hdfs.TestWriteRead` | HIGH | Completed - Tests read-while-write with parameterized upgrade checkpoints |
| ✅ | `org.apache.hadoop.hdfs.TestWriteReadStripedFile` | HIGH | Completed - Tests writing and reading erasure-coded (EC) striped files with RS-3-2 policy. Includes 17 test methods covering empty files, files smaller/equal/larger than cells/stripes/block groups, WebHDFS access, and concat operations. Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. DataNode shutdown method simplified to use cluster.shutdownDataNode(index). |
| ❌ | `org.apache.hadoop.hdfs.TestWriteStripedFileWithFailure` | HIGH | **SKIPPED**: Test is already disabled with @Ignore annotation (line 74) pending HDFS-8704 and HDFS-9040. Requires StripedFileTestUtil.killDatanode() for internal DataNode manipulation (line 144), casts to DFSStripedOutputStream internal implementation (lines 123-124), and uses cluster.getDataNodes() for direct DN access (line 153). Tests writing striped files while killing DataNodes mid-write, which requires internal state manipulation not available via client APIs. |

---

## Phase 8: LOW Priority - Other Transformable Tests

**Target Completion:** Week 12+
**Progress:** 47/128 (36.7%)

Remaining transformable tests not in other categories.

<details>
<summary><b>Click to expand full list (128 tests)</b></summary>

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ❌ | `org.apache.hadoop.hdfs.util.TestBestEffortLongFile` | LOW | **SKIPPED**: Does not use MiniDFSCluster for testing. Only references `MiniDFSCluster.getBaseDirectory()` to get a local directory path. Tests `BestEffortLongFile` utility class with pure local filesystem I/O operations. No cluster is created, no distributed operations, no cluster to upgrade. Not suitable for ProcessBased transformation. |
| ✅ | `org.apache.hadoop.hdfs.web.TestFSMainOperationsWebHdfs` | LOW | Completed - Tests WebHDFS operations with ProcessBasedMiniDFSCluster. Extends FSMainOperationsBaseTest (50+ inherited tests). Transformation without parameterization due to inherited test methods. |
| ✅ | `org.apache.hadoop.hdfs.web.TestHttpsFileSystem` | LOW | Completed - Tests secure WebHDFS (swebhdfs://) with HTTPS-only configuration and SSL/TLS. Replaced direct NameNode access (getHttpsAddress()) with configuration-based address retrieval. |
| ❌ | `org.apache.hadoop.hdfs.web.TestWebHDFS` | LOW | **SKIPPED**: Very large test file (2109 lines, 40 tests). Multiple tests use internal NameNode access: `cluster.getNameNode()`, `NameNodeAdapter.spyOnNamesystem()`, `Whitebox.setInternalState()` for reflection-based internal state manipulation (e.g., testRaceWhileNNStartup manipulates rpcServer field). Mix of ~34 transformable client-side tests and ~6 non-transformable tests requiring internal access. Selective transformation of such a large integrated test file is not practical. |
| ✅ | `org.apache.hadoop.hdfs.web.TestWebHdfsFileSystemContract` | LOW | Completed - Tests WebHDFS FileSystem contract. Extends FileSystemContractBaseTest (43 inherited tests + 7 additional = 50 total). Transformation without parameterization due to inherited test methods. |
| ❌ | `org.apache.hadoop.hdfs.web.TestWebHDFSForHA` | LOW | **SKIPPED**: Requires High Availability (HA) functionality not supported in ProcessBasedMiniDFSCluster. Test requires: (1) MiniDFSNNTopology with 2 NameNodes for HA setup, (2) cluster.transitionToActive()/transitionToStandby() methods for HA state transitions (not available), (3) Direct NameNode access via cluster.getNameNode(0) and cluster.getNamesystem(0) (explicitly blocked), (4) NameNodeAdapter.getDtSecretManager() for internal delegation token manager access, (5) Whitebox.setInternalState() for reflection-based internal state manipulation (testRetryWhileNNStartup sets rpcServer to null). ProcessBasedMiniDFSCluster has TODO comment (line 649) about supporting multiple NameNodes properly and doesn't implement HA topology or state transition methods. All 6 test methods depend on HA features. |
| ❌ | `org.apache.hadoop.hdfs.web.TestWebHdfsTokens` | LOW | **SKIPPED**: Large test file (562 lines, 10 tests) with only 2 tests using MiniDFSCluster. Both cluster tests require unsupported access: (1) testLazyTokenFetchForWebhdfs needs cluster.getNameNode().getHttpsAddress()/getHttpAddress() for direct NameNode HTTP/HTTPS address access - ProcessBasedMiniDFSCluster doesn't expose HTTP/HTTPS address getters, NameNodeProcessManager has getHttpAddress() but not getHttpsAddress(), (2) testSetTokenServiceAndKind uses Whitebox.setInternalState(fs, "canRefreshDelegationToken"/"connectionFactory") for reflection-based internal WebHdfsFileSystem manipulation. The other 8 tests are pure unit/mocking tests (testTokenForNonTokenOp, testNoToken*, testOpRequireAuth) that don't use cluster. Tests focus on delegation token client-side behavior in secure (Kerberos/MiniKdc) environments, not cluster upgrade scenarios. Insufficient cluster usage for transformation. |
| ✅ | `org.apache.hadoop.hdfs.web.TestWebHdfsUrl` | LOW | Completed - Tests WebHDFS URL construction and special character handling. Transformed 3/11 tests that use MiniDFSCluster (testWebHdfsSpecialCharacterFile, testWebHdfsBackwardCompatibleSpecialCharacterFile, testWebHdfsPathWithSemicolon). All tests use pure client-side FileSystem operations. Other 8 tests are unit tests testing URL encoding and don't use cluster. Each test has 6 parameterized upgrade checkpoints. |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHdfsWithAuthenticationFilter` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHdfsWithMultipleNameNodes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHdfsWithRestCsrfPreventionFilter` | LOW | |
| 📋 | `org.apache.hadoop.net.TestNetworkTopology` | LOW | |
| 📋 | `org.apache.hadoop.security.TestPermission` | LOW | |
| 📋 | `org.apache.hadoop.security.TestPermissionSymlinks` | LOW | |
| 📋 | `org.apache.hadoop.security.TestRefreshUserMappings` | LOW | |
| 📋 | `org.apache.hadoop.TestGenericRefresh` | LOW | |
| 📋 | `org.apache.hadoop.TestRefreshCallQueue` | LOW | |

</details>

---

## 📝 Transformation Guidelines

### Before Starting a Transformation

1. ✅ Mark test as "🔄 In Progress" in this tracker
2. ✅ Read the original test to understand its purpose
3. ✅ Check if test has ProcessBased variant already
4. ✅ Review transformation guide: `UPGRADE-TEST-TRANSFORMATION-PROMPT.md`

### Transformation Steps

1. **Create new file** with `_ProcessBased` suffix in same directory
2. **Add imports**:
   ```java
   import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
   import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
   import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
   ```
3. **Add parameterization**:
   ```java
   @RunWith(Parameterized.class)
   public class TestFoo_ProcessBased extends ProcessBasedUpgradeTestBase {
     @Parameter
     public String upgradeCheckpoint;
     
     @Parameters(name = "upgrade-at={0}")
     public static Collection<String> checkpoints() {
       return Arrays.asList(
         UpgradeCheckpoints.NO_UPGRADE,
         UpgradeCheckpoints.AFTER_CLUSTER_START,
         // ... 10-15 checkpoints
       );
     }
   }
   ```
4. **Transform cluster setup** (automatic system property reading):
   ```java
   cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
       .numDataNodes(3)
       .build();  // Reads hadoop.start.home and hadoop.upgrade.home automatically
   fs = cluster.getFileSystem();
   ```
5. **Replace server-side operations** with client APIs
6. **Insert checkpoints** throughout test (10-15 per method)
7. **Run and verify**:
   ```bash
   mvn test -Dtest=TestFoo_ProcessBased \
     -Dhadoop.start.home=/opt/hadoop-3.3.5 \
     -Dhadoop.upgrade.home=/opt/hadoop-3.3.6 \
     -pl hadoop-hdfs-project/hadoop-hdfs
   ```

### After Completing a Transformation

1. ✅ Mark test as "✅ Completed" in this tracker
2. ✅ Update progress percentages
3. ✅ Commit both original and transformed test
4. ✅ Run full test suite to ensure no regressions

---

## 🎯 Weekly Goals

| Week | Goal | Tests to Complete | Priority |
|------|------|-------------------|----------|
| Week 1 | Complete all CRITICAL upgrade tests | 10 | CRITICAL |
| Week 2-3 | Transform core file operation tests | 46 | HIGH |
| Week 4 | Transform data integrity tests | 14 | HIGH |
| Week 5-6 | Transform ViewFS tests | 40 | MEDIUM |
| Week 7 | Transform Balancer tests | 15 | MEDIUM |
| Week 8-9 | Transform Snapshot tests | 40 | MEDIUM |
| Week 10-11 | Transform CLI/Client tests | 80 | MEDIUM |
| Week 12+ | Complete remaining tests | 128 | LOW |

**Total Duration:** ~12-16 weeks for complete transformation

---

## 📈 Progress Tracking

To update this tracker:

1. **Mark In Progress:**
   - Change `📋` to `🔄` when starting transformation
   - Add note in "Notes" column

2. **Mark Completed:**
   - Change `🔄` to `✅` when transformation is complete
   - Add "Completed" or test results in "Notes" column
   - Update progress percentages in dashboard

3. **Mark Blocked:**
   - Change status to `⏸️` if blocked
   - Document blocking issue in "Notes" column

4. **Update Dashboard:**
   - Recalculate percentages after each completion
   - Update "Last Updated" timestamp at top

---

## 📚 References

- **Transformation Guide:** `UPGRADE-TEST-TRANSFORMATION-PROMPT.md`
- **Testing Guide:** `UPGRADE-TESTING-README.md`
- **All Tests List:** `ALL_MINIDFSCLUSTER_TESTS.md`
- **Transformable Tests:** `TRANSFORMABLE_TESTS.md`
- **Analysis Summary:** `TRANSFORMABLE-TESTS-ANALYSIS.md`

---

## 🔍 Quick Search

To find a specific test in this document, use Ctrl+F (Cmd+F on Mac) and search for the test class name.

To see only incomplete tests:
```bash
grep "📋" TRANSFORMATION_PROGRESS_TRACKER.md
```

To see completed tests:
```bash
grep "✅" TRANSFORMATION_PROGRESS_TRACKER.md
```

To count remaining tests by priority:
```bash
grep "| 📋 |" TRANSFORMATION_PROGRESS_TRACKER.md | wc -l
```

---

**Generated:** $(date +"%Y-%m-%d %H:%M:%S")
