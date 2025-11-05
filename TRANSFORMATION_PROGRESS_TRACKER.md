# Test Transformation Progress Tracker

**Last Updated:** 2025-11-04 (Completed TestFileContextSnapshot, TestDisallowModifyROSnapshot; Skipped TestReadStripedFileWithDecodingCorruptData, TestReadStripedFileWithDecodingDeletedData, TestReadStripedFileWithDNFailure)

---

## 📊 Progress Dashboard

| Metric | Count | Percentage |
|--------|------:|----------:|
| **Total Transformable Tests** | 373 | 100.0% |
| **Completed Transformations** | 33 | 8.8% |
| **Skipped (Incompatible)** | 17 | 4.6% |
| **In Progress** | 0 | 0.0% |
| **Not Started** | 323 | 86.6% |

### Progress by Priority

| Priority Level | Total | Completed | Skipped | In Progress | Not Started | % Complete |
|----------------|------:|----------:|--------:|------------:|------------:|----------:|
| **CRITICAL (Upgrade)** | 5 | 0 | 5 | 0 | 0 | 0.0% (All Skipped) |
| **HIGH (File Ops)** | 46 | 18 | 12 | 0 | 16 | 39.1% |
| **HIGH (Data Integrity)** | 14 | 1 | 0 | 0 | 13 | 7.1% |
| **MEDIUM (ViewFS)** | 40 | 3 | 0 | 0 | 37 | 7.5% |
| **MEDIUM (Balancer)** | 15 | 3 | 0 | 0 | 12 | 20.0% |
| **MEDIUM (Snapshot)** | 40 | 3 | 0 | 0 | 37 | 7.5% |
| **MEDIUM (Client/CLI)** | 80 | 2 | 0 | 0 | 78 | 2.5% |
| **LOW (Other)** | 128 | 3 | 0 | 0 | 125 | 2.3% |

---

## Legend

- ✅ **Completed** - Transformation complete and verified
- 🔄 **In Progress** - Currently being transformed
- ⏸️ **Blocked** - Waiting on dependencies or issues
- 📋 **Not Started** - Ready for transformation
- ❌ **Skipped** - Decided not to transform

---

## Phase 1: CRITICAL Priority - Upgrade Tests

**Target Completion:** Week 1
**Progress:** 0/5 (0.0% - All Skipped)

These tests were identified as CRITICAL for upgrade functionality but are **not suitable for ProcessBased transformation** due to fundamental incompatibilities.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ❌ | `org.apache.hadoop.hdfs.TestDatanodeLayoutUpgrade` | CRITICAL | **SKIPPED**: Tests cold upgrades from ancient Hadoop versions (0.22, 1.x, 2.x) using pre-packaged storage tarballs. ProcessBasedMiniDFSCluster is designed for rolling upgrades between modern versions (e.g., 3.3.5→3.3.6), not cold upgrades from decade-old versions. |
| ❌ | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade` | CRITICAL | **SKIPPED**: Requires direct DataNode internal access (FSDataset, storage directories, trash/previous directory verification, block file paths). ProcessBased runs DataNodes in separate processes and only provides client-side APIs. Tests DataNode-internal storage behavior during rolling upgrades. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.ha.TestDFSUpgradeWithHA` | CRITICAL | **SKIPPED**: Requires HA (High Availability) configuration with QJM (Quorum Journal Manager) and multiple NameNodes. ProcessBasedMiniDFSCluster currently only supports single-NameNode clusters. Also requires internal FSImage storage access and "previous" directory verification. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.TestSecondaryNameNodeUpgrade` | CRITICAL | **SKIPPED**: Tests SecondaryNameNode component which ProcessBasedMiniDFSCluster doesn't support. Requires direct access to SecondaryNameNode FSImage storage, VERSION file manipulation, and checkpoint testing. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.TestUpgradeDomainBlockPlacementPolicy` | CRITICAL | **SKIPPED**: Despite the name, this is NOT about Hadoop version upgrades - it tests block placement policy for DataNode "upgrade domains" (maintenance domains). Requires direct DataNode access via `cluster.getDataNodes()`. Not relevant to version upgrade testing. |

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

## Phase 3: HIGH Priority - Replication & Data Integrity

**Target Completion:** Week 4
**Progress:** 1/14 (7.1%)

Tests ensuring data correctness and replication during upgrades.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestCorruptionWithFailover` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestErasureCodingCorruption` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestCorruptMetadataFile` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestDNFencingWithReplication` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestPendingCorruptDnMessages` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestAddOverReplicatedStripedBlocks` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestListCorruptFileBlocks` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestProcessCorruptBlocks` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestRefreshNamenodeReplicationConfig` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.TestCrcCorruption` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.TestFileCorruption` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.TestReadStripedFileWithDecodingCorruptData` | HIGH | |
| 📋 | `org.apache.hadoop.hdfs.TestReplaceDatanodeFailureReplication` | HIGH | |
| ✅ | `org.apache.hadoop.hdfs.TestReplaceDatanodeOnFailure` | HIGH | Completed |

---

## Phase 4: MEDIUM Priority - ViewFS & Federation Tests

**Target Completion:** Week 5-6
**Progress:** 3/40 (7.5%)

Tests for ViewFS, federation, and mount points.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| 📋 | `org.apache.hadoop.fs.viewfs.TestNNStartupWhenViewFSOverloadSchemeEnabled` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemAtHdfsRoot` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemHdfs` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemLinkFallback` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemLinkMergeSlash` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemLinkRegex` | MEDIUM | |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeHdfsFileSystemContract` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeWithHdfsScheme` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemWithAcls` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemWithTruncate` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFileSystemWithXAttrs` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFsAtHdfsRoot` | MEDIUM | |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFsDefaultValue` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFsFileStatusHdfs` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFsHdfs` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFsLinkFallback` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFsWithAcls` | MEDIUM | |
| 📋 | `org.apache.hadoop.fs.viewfs.TestViewFsWithXAttrs` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithFSCommands` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFSStoragePolicyCommands` | MEDIUM | |

---

## Phase 5: MEDIUM Priority - Balancer & Mover Tests

**Target Completion:** Week 7
**Progress:** 3/15 (20.0%)

Tests for data balancing and movement operations.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeHdfsFileSystemContract` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeWithHdfsScheme` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.balancer.TestBalancer` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.hdfs.server.balancer.TestBalancerService` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.balancer.TestBalancerWithHANameNodes` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.balancer.TestBalancerWithMultipleNameNodes` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.balancer.TestBalancerWithNodeGroup` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.diskbalancer.command.TestDiskBalancerCommand` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.diskbalancer.TestConnectors` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancer` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerRPC` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerWithMockMover` | MEDIUM | |
| ✅ | `org.apache.hadoop.hdfs.server.mover.TestMover` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.sps.TestStoragePolicySatisfierWithStripedFile` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.hdfs.TestBalancerBandwidth` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithFSCommands` | MEDIUM | |

---

## Phase 6: MEDIUM Priority - Snapshot Tests

**Target Completion:** Week 8-9
**Progress:** 3/40 (7.5%)

Tests for HDFS snapshot functionality.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestAclWithSnapshot` | MEDIUM | |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestDisallowModifyROSnapshot` | MEDIUM | Completed - Tests that snapshot paths are read-only and modifications are properly disallowed. All operations use client-side APIs (FileSystem, DFSClient) to attempt modifications on snapshot paths and verify SnapshotAccessControlException is thrown. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestFileContextSnapshot` | MEDIUM | Completed - Tests FileContext snapshot operations (create, delete, rename) with parameterized upgrade checkpoints. All operations use client-side APIs (FileContext, DistributedFileSystem) |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestNestedSnapshots` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestRandomOpsWithSnapshots` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapRootDescendantDiff` | MEDIUM | |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotFileLength` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotListing` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotMetrics` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotNameWithInvalidCharacters` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotStatsMXBean` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshottableDirListing` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestUpdatePipelineWithSnapshots` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestXAttrWithSnapshot` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestAppendSnapshotTruncate` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestErasureCodingPolicyWithSnapshot` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestSnapshotCommands` | MEDIUM | |

---

## Phase 7: MEDIUM Priority - CLI & Client Tests

**Target Completion:** Week 10-11
**Progress:** 2/80 (2.5%)

Tests for command-line tools and client operations.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| 📋 | `org.apache.hadoop.cli.TestAclCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.cli.TestCacheAdminCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.cli.TestCryptoAdminCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.cli.TestDeleteCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.cli.TestErasureCodingCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.cli.TestHDFSCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.cli.TestXAttrCLI` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.client.impl.TestBlockReaderFactory` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.client.impl.TestBlockReaderLocal` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.client.impl.TestBlockReaderLocalLegacy` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.protocol.datatransfer.sasl.TestSaslDataTransfer` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.protocol.datatransfer.sasl.TestSaslDataTransferExpiredBlockToken` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDatanodeProtocolRetryPolicy` | MEDIUM | |
| ✅ | `org.apache.hadoop.hdfs.TestClientProtocolForPipelineRecovery` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.hdfs.TestClientReportBadBlock` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSClientExcludedNodes` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSClientFailover` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSClientRetries` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSClientSocketSize` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestFileCreationClient` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.TestHdfsAdmin` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestDebugAdmin` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestDelegationTokenFetcher` | MEDIUM | |
| ✅ | `org.apache.hadoop.hdfs.tools.TestDFSAdmin` | MEDIUM | Completed |
| 📋 | `org.apache.hadoop.hdfs.tools.TestDFSAdminWithHA` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestDFSZKFailoverController` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestECAdmin` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestGetGroups` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestStoragePolicyCommands` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestStoragePolicySatisfyAdminCommands` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithFSCommands` | MEDIUM | |
| 📋 | `org.apache.hadoop.hdfs.tools.TestViewFSStoragePolicyCommands` | MEDIUM | |
| 📋 | `org.apache.hadoop.tools.TestJMXGet` | MEDIUM | |

---

## Phase 8: LOW Priority - Other Transformable Tests

**Target Completion:** Week 12+
**Progress:** 3/128 (2.3%)

Remaining transformable tests not in other categories.

<details>
<summary><b>Click to expand full list (128 tests)</b></summary>

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| 📋 | `org.apache.hadoop.fs.loadGenerator.TestLoadGenerator` | LOW | |
| 📋 | `org.apache.hadoop.fs.permission.TestStickyBit` | LOW | |
| 📋 | `org.apache.hadoop.fs.shell.TestHdfsTextCommand` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestEnhancedByteBufferAccess` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestFcHdfsCreateMkdir` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestFcHdfsPermission` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestFcHdfsSetUMask` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestGlobPaths` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestHDFSFileContextMainOperations` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestResolveHdfsSymlink` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestSWebHdfsFileContextMainOperations` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestSymlinkHdfs` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestSymlinkHdfsDisable` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestUnbuffer` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestUrlStreamHandler` | LOW | |
| 📋 | `org.apache.hadoop.fs.TestWebHdfsFileContextMainOperations` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.crypto.TestHdfsCryptoStreams` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.qjournal.server.TestJournalNode` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.qjournal.TestMiniJournalCluster` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.qjournal.TestNNWithQJM` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.qjournal.TestSecureNNWithQJM` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.security.TestDelegationTokenForProxyUser` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.security.token.block.TestBlockToken` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.aliasmap.TestSecureAliasMap` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockInfoStriped` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockReportLease` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockReportRateLimiting` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockTokenWithDFS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockTokenWithDFSStriped` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockTokenWithShortCircuitRead` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestComputeInvalidateWork` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestNodeCount` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestPendingDataNodeMessages` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestRBWBlockInvalidation` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestRedundancyMonitor` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestSequentialBlockGroupId` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestSequentialBlockId` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.blockmanagement.TestSlowDiskTracker` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestCacheByPmemMappableBlockLoader` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestDatanodeRestart` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsVolumeList` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestPmemCacheRecovery` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestReplicaCachingGetSpaceUsed` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestSpaceReservation` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBatchIbr` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBlockHasMultipleReplicasOnSameDN` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery2` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBlockScanner` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestBPOfferService` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestCachingStrategy` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeECN` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeExit` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeFaultInjector` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeInitStorage` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeLifeline` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeMetrics` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeMetricsLogger` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeMXBean` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeReconfiguration` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeTcpNoDelay` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeTransferSocketSize` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDataNodeUUID` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestDeleteBlockPool` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestFsDatasetCacheRevocation` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestHSync` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestIncrementalBrVariations` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestLargeBlockReport` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestRefreshNamenodes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestStartSecureDataNode` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestStorageReport` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestTransferRbw` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.datanode.web.TestDatanodeHttpXFrame` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestBootstrapAliasmap` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestFailoverWithBlockTokensEnabled` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestGetGroupsWithHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestHAAppend` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestHAFsck` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestHAMetrics` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestHarFileSystemWithHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestInitializeSharedEdits` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestLossyRetryInvocationHandler` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestNNHealthCheck` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestQuotasWithHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestRemoteNameNodeInfo` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestSeveralNameNodes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestStateTransitionFailure` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.ha.TestXAttrsWithHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.metrics.TestNNMetricFilesInGetListingOps` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestAddBlock` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestAddBlockRetry` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestAddStripedBlockInFBR` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestAllowFormat` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestAuditLogs` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestBlockPlacementPolicyRackFaultTolerant` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestBlockUnderConstruction` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestCacheDirectives` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestCacheDirectivesWithViewDFS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestCommitBlockWithInvalidGenStamp` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestCreateEditsLog` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestDefaultBlockPlacementPolicy` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestFavoredNodesEndToEnd` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestFsckWithMultipleNameNodes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestFSImageWithAcl` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestFSImageWithXAttr` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestGetContentSummaryWithPermission` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestHostsFiles` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestINodeAttributeProvider` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestLargeDirectoryDelete` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestListOpenFiles` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestMalformedURLs` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestMetadataVersionOutput` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeReconfigure` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeResourceChecker` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeRespectsBindHostKeys` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNamenodeRetryCache` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeRetryCacheMetrics` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeRpcServer` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeRpcServerMethods` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNameNodeStatusMXBean` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNamenodeStorageDirectives` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNetworkTopologyServlet` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestNNThroughputBenchmark` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestQuotaByStorageType` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestQuotaWithStripedBlocks` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestRedudantBlocks` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestRefreshBlockPlacementPolicy` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestSecondaryWebUi` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestSecureNameNode` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestSecureNameNodeWithExternalKdc` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestStoragePolicySatisfierWithHA` | LOW | |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.TestStripedINodeFile` | LOW | Completed |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.TestValidateConfigurationSettings` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.namenode.web.resources.TestWebHdfsCreatePermissions` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.process.integration.TestProcessBasedMiniDFSCluster` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.process.unit.TestProcessBasedMiniDFSCluster_DynamicNodes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.server.process.unit.TestProcessBasedMiniDFSCluster_StorageTypes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.shortcircuit.TestShortCircuitCache` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.shortcircuit.TestShortCircuitLocalRead` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestAbandonBlock` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestAclsEndToEnd` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestApplyingStoragePolicy` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestBatchedListDirectories` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestBlockMissingException` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestBlockTokenWrappingQOP` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestByteBufferPread` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestClose` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDatanodeConfig` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDatanodeDeath` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDatanodeReport` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDatanodeStartupFixesLegacyStorageIDs` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDataStream` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDataTransferKeepalive` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDeadNodeDetection` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSAddressConfig` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSInputStream` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSInputStreamBlockLocations` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSMkdirs` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSPermission` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSRemove` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSShellGenericOptions` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSStartupVersions` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSStorageStateRecovery` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSStripedInputStream` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSStripedOutputStream` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSStripedOutputStreamUpdatePipeline` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestDFSStripedOutputStreamWithFailureBase` | LOW | |
| ✅ | `org.apache.hadoop.hdfs.TestDistributedFileSystem` | LOW | Completed |
| 📋 | `org.apache.hadoop.hdfs.TestDistributedFileSystemWithECFile` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestEncryptedTransfer` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestEncryptionZonesWithHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestErasureCodeBenchmarkThroughput` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestErasureCodingAddConfig` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestErasureCodingExerciseAPIs` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestErasureCodingMultipleRacks` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestExtendedAcls` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestExternalBlockReader` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestFetchImage` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestFSInputChecker` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestFSOutputSummer` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestFsShellPermission` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestGetFileChecksum` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestHAAuxiliaryPort` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestHDFSFileSystemContract` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestHDFSTrash` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestHFlush` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestInjectionForSimulatedStorage` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestIsMethodSupported` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestLargeBlock` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestLease` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestLeaseRecoveryStriped` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestListFilesInDFS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestListFilesInFileContext` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestLocalDFS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestLocatedBlocksRefresher` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestMaintenanceState` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestMiniDFSCluster` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestMissingBlocksAlert` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestModTime` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestMultipleNNPortQOP` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestMultiThreadedHflush` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestPersistBlocks` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestPipelines` | LOW | |
| ✅ | `org.apache.hadoop.hdfs.TestPread` | LOW | Completed |
| 📋 | `org.apache.hadoop.hdfs.TestQuotaAllowOwner` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestReconstructStripedFile` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestReconstructStripedFileWithValidator` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestRestartDFS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestSecureEncryptionZoneWithKMS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestSeekBug` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestSetrepIncreasing` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestSetTimes` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestSlowDatanodeReport` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestSmallBlock` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestStateAlignmentContextWithHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestStoragePolicyPermissionSettings` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestStripedFileAppend` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestTrashWithEncryptionZones` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestTrashWithSecureEncryptionZones` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestUnsetAndChangeDirectoryEcPolicy` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestViewDistributedFileSystem` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.TestViewDistributedFileSystemContract` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.util.TestBestEffortLongFile` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestFSMainOperationsWebHdfs` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestHttpsFileSystem` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHDFS` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHdfsFileSystemContract` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHDFSForHA` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHdfsTokens` | LOW | |
| 📋 | `org.apache.hadoop.hdfs.web.TestWebHdfsUrl` | LOW | |
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
