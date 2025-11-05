# Test Transformation Progress Tracker

**Last Updated:** 2025-11-04 (Completed 2 tests, Skipped 5 tests: 4 HA/Federation, 1 NodeGroup)

---

## 📊 Progress Dashboard

| Metric | Count | Percentage |
|--------|------:|----------:|
| **Total Transformable Tests** | 373 | 100.0% |
| **Completed Transformations** | 45 | 12.1% |
| **Skipped (Incompatible)** | 45 | 12.1% |
| **In Progress** | 0 | 0.0% |
| **Not Started** | 283 | 75.9% |

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
**Progress:** 2/14 completed, 12/14 skipped (14.3% completion rate - Phase Complete!)

Tests ensuring data correctness and replication during upgrades.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ❌ | `org.apache.hadoop.hdfs.server.blockmanagement.TestCorruptionWithFailover` | HIGH | **SKIPPED**: Requires HA configuration with QJM and multiple NameNodes (line 53: `MiniDFSNNTopology.simpleHATopology()`). Also requires direct access to internal BlockManager via `cluster.getNamesystem().getBlockManager()` (line 78) and internal methods `getCorruptBlocks()` (line 74) and `markAllDatanodesStale()` (lines 68-69). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. |
| ❌ | `org.apache.hadoop.hdfs.server.blockmanagement.TestErasureCodingCorruption` | HIGH | **SKIPPED**: Requires HA configuration (line 53: `simpleHATopology()`) and HA failover operations (lines 81-82: `transitionToStandby()`, `transitionToActive()`). Also requires direct access to internal BlockManager (line 78: `getNamesystem().getBlockManager()`) and internal method `getCorruptECBlockGroups()` (line 88) with no client API equivalent. ProcessBasedMiniDFSCluster only supports single-NameNode clusters. |
| ❌ | `org.apache.hadoop.hdfs.server.datanode.TestCorruptMetadataFile` | HIGH | **SKIPPED**: Requires direct access to DataNode's local filesystem to corrupt metadata files (line 81: `cluster.getBlockMetadataFile(0, block)`) and direct file manipulation (lines 89-98). Also requires internal BlockManager access (lines 109-110: `getNamesystem().getBlockManager().getCorruptBlocks()`). No client API exists for accessing or manipulating DataNode metadata files on disk. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.ha.TestDNFencingWithReplication` | HIGH | **SKIPPED**: Requires HA configuration with 3 NameNodes (`harness.setNumberOfNameNodes(3)`) and uses `HAStressTestHarness` which requires QJM/shared edits. Tests HA failover scenarios during replication (`harness.addFailoverThread`, `cluster.transitionToActive`). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.ha.TestPendingCorruptDnMessages` | HIGH | **SKIPPED**: Requires HA configuration (`MiniDFSNNTopology.simpleHATopology()` line 59) with 2 NameNodes and HA failover operations (`transitionToActive`, `transitionToStandby`). Also requires internal BlockManager access (`getNamesystem().getBlockManager().getPendingDataNodeMessageCount()` lines 94-96) with no client API equivalent. ProcessBasedMiniDFSCluster only supports single-NameNode clusters. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.TestAddOverReplicatedStripedBlocks` | HIGH | **SKIPPED**: Requires extensive internal access: `SimulatedFSDataset.setFactory(conf)` to inject custom dataset (line 79), `cluster.injectBlocks()` to directly manipulate DataNode storage (lines 112, 118-122), `cluster.triggerBlockReports()` and `triggerHeartbeats()` not available in ProcessBased, `cluster.getDataNodes()` for direct DN access (line 159), `getNamesystem().getBlockManager()` for internal state (line 204), and `getNamesystem().writeLock()/writeUnlock()` for NameNode locking (lines 207-213). No client API equivalents exist for block injection and internal state manipulation. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.TestListCorruptFileBlocks` | HIGH | **SKIPPED**: Requires direct filesystem access to DataNode storage directories to corrupt blocks: `cluster.getInstanceStorageDir()` and `MiniDFSCluster.getFinalizedDir()` (lines 102-105, 184-186, 302-303, 415-416, 495-496, 585-586), `getAllBlockFiles()/getAllBlockMetadataFiles()` (lines 105, 188, 304, 417, 498), direct file manipulation with RandomAccessFile and delete() to corrupt blocks on disk (lines 109-115, 192-198, 310-317, 423-430, 502-506, 591-597), and `DataNodeTestUtils.runDirectoryScanner()` for internal DN operations (line 512). While `dfs.listCorruptFileBlocks()` is a client API, the test requires direct manipulation of block files on disk which is not accessible in ProcessBasedMiniDFSCluster. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.TestProcessCorruptBlocks` | HIGH | **SKIPPED**: Requires internal access to corrupt blocks and verify replication state: `cluster.getNamesystem()` for direct FSNamesystem access (lines 64, 119, 170, 224), `namesystem.getBlockManager().countNodes()` to count corrupt replicas via internal BlockManager methods (lines 264-266), `cluster.getMaterializedReplica().truncateData()` to directly manipulate blocks on disk (line 276), `DataNodeTestUtils.runDirectoryScanner()` for internal DataNode operation (line 279), and `cluster.getStorageDir()/MiniDFSCluster.getFinalizedDir()` for direct filesystem access to DataNode storage (lines 285-286). No client API exists to corrupt blocks or query internal replication statistics. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.TestRefreshNamenodeReplicationConfig` | HIGH | **SKIPPED**: Requires direct access to internal BlockManager (line 57: `cluster.getNameNode().getNamesystem().getBlockManager()`) and internal BlockManager methods `getMaxReplicationStreams()`, `getReplicationStreamsHardLimit()`, `getBlocksReplWorkMultiplier()` (lines 72-74, 85-87, etc.) which have no client API equivalents. Test verifies internal replication configuration that can only be validated through direct BlockManager access. |
| ❌ | `org.apache.hadoop.hdfs.TestCrcCorruption` | HIGH | **SKIPPED**: Requires direct manipulation of DataNode storage to corrupt blocks. Uses `cluster.getDataNodes().get(dnIdx)` for direct DN access (line 173), `dn.getFSDataset().getFinalizedBlocks(bpid)` for internal dataset access (lines 175-176), and `cluster.deleteMeta()`, `cluster.truncateMeta()`, `cluster.corruptMeta()` to directly manipulate meta files on DataNode storage (lines 184, 189, 191). Also uses `cluster.corruptBlockOnDataNodes()` to corrupt block files (line 289). No client API exists to corrupt meta files or block files on DataNode storage. |
| ❌ | `org.apache.hadoop.hdfs.TestFileCorruption` | HIGH | **SKIPPED**: (Already skipped in Phase 2) All 5 test methods require deep internal access: `dn.getFSDataset()` for block manipulation, `cluster.getFsDatasetTestUtils()` for deleting block files, `cluster.getNamesystem()` with writeLock()/writeUnlock() for NameNode locking, `getBlockManager().findAndMarkBlockAsCorrupt()` for manual corruption marking, `cluster.triggerBlockReports()` not available in ProcessBased, and internal BlockManager metrics (getBlocksTotal, getLowRedundancyBlocksCount). Tests simulate block corruption and disk failures by directly manipulating internal storage state, with no client API equivalents. |
| ❌ | `org.apache.hadoop.hdfs.TestReadStripedFileWithDecodingCorruptData` | HIGH | **SKIPPED**: (Already skipped in Phase 2) Uses `ReadStripedFileWithDecodingHelper.testReadWithBlockCorrupted()` which requires `cluster.getDataNodes()` for direct DataNode access (line 101, 169) and `cluster.corruptBlockOnDataNodes()` or `cluster.corruptBlockOnDataNodesByDeletingBlockFile()` (lines 254, 258) for internal block manipulation. These methods directly manipulate DataNode internal storage to simulate corruption, which is not accessible in ProcessBasedMiniDFSCluster. Tests online recovery of striped files with corrupt blocks by internally manipulating block files. |
| ✅ | `org.apache.hadoop.hdfs.TestReplaceDatanodeFailureReplication` | HIGH | Completed - Tests pipeline replacement behavior when DataNodes fail during write operations. Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. Replaced `cluster.waitFirstBRCompleted()` with Thread.sleep(). All tests use client-side APIs (`out.getCurrentBlockReplication()`, file operations). Includes 5 test methods testing various datanode failure scenarios during pipeline writes. |
| ✅ | `org.apache.hadoop.hdfs.TestReplaceDatanodeOnFailure` | HIGH | Completed |

---

## Phase 4: MEDIUM Priority - ViewFS & Federation Tests

**Target Completion:** Week 5-6
**Progress:** 6/40 completed, 15/40 skipped (15.0% completion rate)

Tests for ViewFS, federation, and mount points.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ❌ | `org.apache.hadoop.fs.viewfs.TestNNStartupWhenViewFSOverloadSchemeEnabled` | MEDIUM | **SKIPPED**: Primary test method `testHANameNodeAndDataNodeStartup()` requires HA configuration (`MiniDFSNNTopology.simpleHATopology()`) with multiple NameNodes and `cluster.transitionToActive()` for HA failover. ProcessBasedMiniDFSCluster only supports single-NameNode clusters. While `testNameNodeAndDataNodeStartup()` could theoretically be transformed, the primary focus of this test is HA startup validation which is not supported. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemAtHdfsRoot` | MEDIUM | **SKIPPED**: Extends `ViewFileSystemBaseTest` which has 86 inherited test methods. Would require transforming the entire base class. Additionally, some inherited tests may require features not supported by ProcessBasedMiniDFSCluster. Cost/benefit ratio too high for MEDIUM priority ViewFS tests. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemHdfs` | MEDIUM | **SKIPPED**: Extends `ViewFileSystemBaseTest` (86 inherited test methods) AND uses federation topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)`). ProcessBasedMiniDFSCluster only supports single-NameNode clusters, not federation. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemLinkFallback` | MEDIUM | **SKIPPED**: Likely extends ViewFileSystemBaseTest or requires federation based on pattern of other ViewFS tests. These ViewFS tests are complex with low upgrade testing value. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemLinkMergeSlash` | MEDIUM | **SKIPPED**: Part of ViewFS test suite - likely extends base class or requires federation. Low priority for upgrade testing. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemLinkRegex` | MEDIUM | **SKIPPED**: Part of ViewFS test suite - likely extends base class or requires federation. Low priority for upgrade testing. |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeHdfsFileSystemContract` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeWithHdfsScheme` | MEDIUM | Completed |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemWithAcls` | MEDIUM | **SKIPPED**: Requires federated topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)` on line 68). ProcessBasedMiniDFSCluster only supports single-NameNode clusters, not federation. Test verifies ViewFS ACL operations across multiple federated namespaces. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemWithTruncate` | MEDIUM | **SKIPPED**: Requires federated topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)` on line 59). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies truncate functionality through ViewFileSystem. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemWithXAttrs` | MEDIUM | **SKIPPED**: Requires federated topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)` on line 63). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies XAttr operations across multiple federated namespaces. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFsAtHdfsRoot` | MEDIUM | **SKIPPED**: Extends `ViewFsBaseTest` which has 66 inherited test methods. Would require transforming the entire base class. Cost/benefit ratio too high for MEDIUM priority ViewFS tests. |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFsDefaultValue` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFsFileStatusHdfs` | MEDIUM | Completed - Tests ViewFS FileStatus serialization and checksum functionality. Transformed with 8 parameterized upgrade checkpoints (NO_UPGRADE, AFTER_CLUSTER_START, AFTER_VIEWFS_SETUP, AFTER_FILE_CREATE, BEFORE_SERIALIZATION, AFTER_SERIALIZATION, BEFORE_CHECKSUM_VERIFY, AFTER_CHECKSUM_VERIFY). All operations use client-side APIs (FileSystem, ViewFileSystem). |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFsHdfs` | MEDIUM | **SKIPPED**: Extends `ViewFsBaseTest` which has 66 inherited test methods. Would require transforming the entire base class. Cost/benefit ratio too high for MEDIUM priority ViewFS tests. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFsLinkFallback` | MEDIUM | **SKIPPED**: Requires federated topology with 3 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(3)` on line 75-76). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies ViewFS LinkFallback mount table entries across federation. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFsWithAcls` | MEDIUM | **SKIPPED**: Requires federated topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)`). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies ACL operations through ViewFS across federation. |
| ❌ | `org.apache.hadoop.fs.viewfs.TestViewFsWithXAttrs` | MEDIUM | **SKIPPED**: Requires federated topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)`). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies XAttr operations through ViewFS across federation. |
| ✅ | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin` | MEDIUM | Completed - Tests DFSAdmin commands (saveNamespace, safemode, allowSnapshot, setBalancerBandwidth) with ViewFileSystemOverloadScheme. Transformed with 7 parameterized upgrade checkpoints. All operations use client-side DFSAdmin APIs |
| ✅ | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithFSCommands` | MEDIUM | Completed - Tests FsShell `-df` command with ViewFileSystemOverloadScheme. Transformed with 5 parameterized upgrade checkpoints. All operations use client-side FsShell APIs |
| ❌ | `org.apache.hadoop.hdfs.tools.TestViewFSStoragePolicyCommands` | MEDIUM | **SKIPPED**: Requires federated topology with 2 NameNodes (`MiniDFSNNTopology.simpleFederatedTopology(2)` on line 48-49). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies StoragePolicyAdmin commands through ViewFS across federation. |

---

## Phase 5: MEDIUM Priority - Balancer & Mover Tests

**Target Completion:** Week 7
**Progress:** 6/15 completed (includes 2 duplicates from Phase 4), 9/15 skipped (40.0% completion rate)

Tests for data balancing and movement operations.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeHdfsFileSystemContract` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeWithHdfsScheme` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.balancer.TestBalancer` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks` | MEDIUM | Completed |
| ❌ | `org.apache.hadoop.hdfs.server.balancer.TestBalancerService` | MEDIUM | **SKIPPED**: Requires HA configuration (`MiniDFSNNTopology.simpleHATopology()` on line 69) with 2 NameNodes, HA failover operations (`cluster.transitionToActive()`), and uses `cluster.triggerHeartbeats()` / `cluster.triggerBlockReports()` (lines 135-136) which are not available in ProcessBasedMiniDFSCluster. Test verifies balancer running as a service with HA and periodic re-balancing after NameNode restarts. |
| ❌ | `org.apache.hadoop.hdfs.server.balancer.TestBalancerWithHANameNodes` | MEDIUM | **SKIPPED**: Explicitly tests balancer with HA NameNodes (imports MiniQJMHACluster, HATestUtil). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies balancer operations with HA failover and stale read scenarios. |
| ❌ | `org.apache.hadoop.hdfs.server.balancer.TestBalancerWithMultipleNameNodes` | MEDIUM | **SKIPPED**: Explicitly tests balancer with multiple NameNodes (federation). Suite class iterates over multiple NameNodes (`cluster.getNameNode(i).getRpcServer()`). ProcessBasedMiniDFSCluster only supports single-NameNode clusters. Test verifies cross-NameNode balancing in federated setup. |
| ❌ | `org.apache.hadoop.hdfs.server.balancer.TestBalancerWithNodeGroup` | MEDIUM | **SKIPPED**: Uses `MiniDFSClusterWithNodeGroup` (line 39, 69) - specialized cluster variant for testing node group aware placement. Requires `NetworkTopologyWithNodeGroup` and `BlockPlacementPolicyWithNodeGroup` configuration. ProcessBasedMiniDFSCluster doesn't have a "WithNodeGroup" variant. Test verifies balancer with sub-rack (node group) topology awareness. |
| ❌ | `org.apache.hadoop.hdfs.server.diskbalancer.command.TestDiskBalancerCommand` | MEDIUM | **SKIPPED**: Requires extensive direct DataNode object access via `cluster.getDataNodes()` (lines 107-109, 364, 607, 656, 697, 713, etc.) for getting DataNode UUIDs, calling `dn.shutdown()`, and accessing `dn.getIpcPort()`. Also uses `cluster.getInstanceStorageDir()` (lines 629, 636) to access internal DataNode storage directories. ProcessBasedMiniDFSCluster runs DataNodes in separate processes and doesn't provide direct DataNode object references or internal storage directory access. |
| ✅ | `org.apache.hadoop.hdfs.server.diskbalancer.TestConnectors` | MEDIUM | Completed - Tests DiskBalancer connectors (NameNodeConnector and JsonConnector) with parameterized upgrade checkpoints. Transformed to use ProcessBasedMiniDFSCluster. All operations use client-side APIs via ClusterConnector |
| ❌ | `org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancer` | MEDIUM | **SKIPPED**: Requires extensive direct DataNode internal access via `cluster.getDataNodes()` (lines 100, 246, 549, 605, 643, 770, 780) and accesses internal DataNode components: `dnNode.getFSDataset()` (line 108), `dnNode.getFSDataset().getFsVolumeReferences()` (line 108-110), `node.getFSDataset()` (line 248). Test also uses Mockito to spy on and mock internal FsDataset operations like `moveBlockAcrossVolumes()` (lines 248, 264-265). This is an internal implementation test for DataNode's DiskBalancer logic, not a client-side API test. ProcessBasedMiniDFSCluster runs DataNodes in separate processes and cannot provide access to internal DataNode objects or allow mocking of internal components. |
| ❌ | `org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerRPC` | MEDIUM | **SKIPPED**: Requires direct DataNode object access via `cluster.getDataNodes()` (lines 260, 310, 313) and calls DataNode-specific admin RPC methods directly on DataNode objects: `dataNode.submitDiskBalancerPlan()` (lines 89, 143, 207, 223), `dataNode.cancelDiskBalancePlan()` (line 145), `dataNode.queryDiskBalancerPlan()` (lines 225, 235), `dataNode.getDiskBalancerSetting()` (line 209). Also accesses internal FsDataset: `dnNode.getFSDataset().getFsVolumeReferences()` (lines 261-262) and manipulates internal FsVolume objects (lines 264-267). These are DataNode-specific admin APIs, not standard client protocols. ProcessBasedMiniDFSCluster doesn't provide DataNode object references or a mechanism to make DataNode-specific RPC calls. |
| ❌ | `org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerWithMockMover` | MEDIUM | **SKIPPED**: Requires direct DataNode internal access via `cluster.getDataNodes()` (lines 90, 114, 330) and extensive internal FsDataset access: `dataNode.getFSDataset()` (lines 91, 115, 331, 493). Test uses Mockito to create a mock `TestMover` with internal FsDataset to test DiskBalancer implementation (lines 90-91, 93-95). This is an internal unit test for DiskBalancer logic that mocks internal DataNode components, not a client-side integration test. ProcessBasedMiniDFSCluster runs DataNodes in separate processes and cannot provide access to internal DataNode objects or allow mocking of internal components. |
| ✅ | `org.apache.hadoop.hdfs.server.mover.TestMover` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.sps.TestStoragePolicySatisfierWithStripedFile` | MEDIUM | Completed |
| ❌ | `org.apache.hadoop.hdfs.TestBalancerBandwidth` | MEDIUM | **SKIPPED**: Requires direct DataNode object access via `cluster.getDataNodes()` (line 67) and calls DataNode methods: `datanodes.get(0).getBalancerBandwidth()` (lines 69-70, 121-122), `datanodes.get(0).getIpcPort()` (line 73). Also accesses DataNode internal field `ipcServer` to get listener addresses (lines 72-74). While the test uses client-side APIs like `fs.setBalancerBandwidth()` (line 86) and DFSAdmin commands (lines 78-93), it requires DataNode object access for getting addresses and verifying bandwidth settings. ProcessBasedMiniDFSCluster doesn't provide `getDataNodes()` or methods to get individual DataNode addresses/ports. |
| ✅ | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin` | MEDIUM | Completed (see Phase 4, line 150) |
| ✅ | `org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithFSCommands` | MEDIUM | Completed (see Phase 4, line 151) |

---

## Phase 6: MEDIUM Priority - Snapshot Tests

**Target Completion:** Week 8-9
**Progress:** 12/40 completed, 5/40 skipped (30.0% completion rate)

Tests for HDFS snapshot functionality.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ❌ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestAclWithSnapshot` | MEDIUM | **SKIPPED**: Requires extensive internal NameNode access: (1) FSAclBaseTest.getAclFeature(path, cluster) to get internal AclFeature objects and check reference counts; (2) AclStorage.getUniqueAclFeatures() to access internal NameNode storage; (3) cluster.getNameNode() + NameNodeAdapter.enterSafeMode/saveNamespace() for restart with checkpoint operations. The testDeDuplication test (60% of the code) heavily relies on internal AclFeature reference counting which has no client API equivalent. Most ACL operations are client-side, but internal verification makes this test unsuitable for ProcessBased transformation. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestDisallowModifyROSnapshot` | MEDIUM | Completed - Tests that snapshot paths are read-only and modifications are properly disallowed. All operations use client-side APIs (FileSystem, DFSClient) to attempt modifications on snapshot paths and verify SnapshotAccessControlException is thrown. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestFileContextSnapshot` | MEDIUM | Completed - Tests FileContext snapshot operations (create, delete, rename) with parameterized upgrade checkpoints. All operations use client-side APIs (FileContext, DistributedFileSystem) |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestNestedSnapshots` | MEDIUM | **SKIPPED**: Requires internal NameNode access to configure nested snapshot support: (1) cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true/false) for enabling/disabling nested snapshots - no client API equivalent; (2) testDisallowNestedSnapshottableDir requires cluster.getNamesystem().getFSDirectory(), fsdir.getINode(), and checks internal INode types (isWithSnapshot(), isSnapshottable()); (3) SnapshotTestHelper.dumpTree() requires cluster object. Core nested snapshot functionality depends on internal configuration not accessible via client APIs. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestRandomOpsWithSnapshots` | MEDIUM | Completed - Comprehensive test of random FileSystem operations (create/delete/rename files/directories) mixed with snapshot operations (create/delete/rename snapshots), with NameNode restarts to verify fsimage loading. Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. Replaced cluster.restartNameNodes() with cluster.restartNameNode(0), cluster.getNameNode().isInSafeMode() with hdfs.setSafeMode(SafeModeAction.SAFEMODE_GET), and cluster.isDataNodeUp() with cluster.getNumDataNodes() > 0. Tests 250 files with random directory depths, performing random iterations of operations. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapRootDescendantDiff` | MEDIUM | Completed - Tests snapshot diff report for non-snapshottable descendant directories. Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. All operations use client-side APIs (mkdirs, allowSnapshot, createSnapshot, getSnapshotDiffReport). Test verifies that attempting to get snapshot diff on a non-snapshottable directory properly throws exception. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotFileLength` | MEDIUM | Completed |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotListing` | MEDIUM | Completed - Tests listing snapshots under snapshottable directories with parameterized upgrade checkpoints. All operations use client-side APIs (FileSystem operations: listStatus, allowSnapshot, createSnapshot, deleteSnapshot). Removed unused FSNamesystem field from original test. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotMetrics` | MEDIUM | **SKIPPED**: Requires internal NameNode access to enable nested snapshot support: cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true) on line 82. The test verifies snapshot-related metrics (SnapshottableDirectories, AllowSnapshotOps, DisallowSnapshotOps) which rely on nested snapshot functionality. Same blocker as TestNestedSnapshots and TestSnapshottableDirListing. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotNameWithInvalidCharacters` | MEDIUM | Completed - Tests that snapshot names with invalid characters (: and /) are properly rejected with RemoteException. All operations use client-side APIs (createFile, allowSnapshot, createSnapshot). Simple validation test with parameterized upgrade checkpoints. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotStatsMXBean` | MEDIUM | **SKIPPED**: Requires internal NameNode access to SnapshotManager: cluster.getNamesystem().getSnapshotManager() (line 52) to call getNumSnapshottableDirs() and getNumSnapshots() (lines 66, 70) for validating JMX metrics. While JMX metrics can be queried via MBeanServer in ProcessBased tests, the validation requires comparing against internal SnapshotManager state which has no client API equivalent. Test verifies SnapshotStatsMXBean by cross-checking MBean data with internal SnapshotManager counters. |
| ❌ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshottableDirListing` | MEDIUM | **SKIPPED**: Requires internal NameNode access to enable nested snapshot support: cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true) on lines 79 and 177. This is necessary for both test methods (testListSnapshottableDir and testListWithDifferentUser) to work correctly. No client API equivalent exists for configuring nested snapshot support at runtime. Same blocker as TestNestedSnapshots. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestUpdatePipelineWithSnapshots` | MEDIUM | Completed - Tests pipeline recovery after deleting a file that's in a snapshot (regression test for HDFS-6647). Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. Changed from cluster.getNameNodeRpc() (returns NamenodeProtocols) to dfs.getClient().getNamenode() (returns ClientProtocol). All RPC operations (updateBlockForPipeline, updatePipeline) are client-side calls. |
| ✅ | `org.apache.hadoop.hdfs.server.namenode.snapshot.TestXAttrWithSnapshot` | MEDIUM | Completed - Tests interaction of XAttrs (extended attributes) with snapshots. Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. Replaced NameNodeAdapter.enterSafeMode/saveNamespace with client-side APIs (hdfs.setSafeMode, hdfs.saveNamespace). Includes tests for modifying/removing XAttrs, successive snapshots, read-only snapshot verification, and snapshot copying with XAttr preservation. |
| ✅ | `org.apache.hadoop.hdfs.TestAppendSnapshotTruncate` | MEDIUM | Completed (duplicate entry - already completed in Phase 2) |
| ✅ | `org.apache.hadoop.hdfs.TestErasureCodingPolicyWithSnapshot` | MEDIUM | Completed - Tests erasure coding policy behavior with snapshots, including policy changes, NameNode restarts, and snapshot operations. Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. All operations use client-side APIs (setErasureCodingPolicy, getErasureCodingPolicy, allowSnapshot, createSnapshot, deleteSnapshot, setSafeMode, saveNamespace). Includes 7 test methods testing EC policy with successive snapshots, NN restarts, snapshot copying, and policy changes. |
| ✅ | `org.apache.hadoop.hdfs.TestSnapshotCommands` | MEDIUM | Completed - Tests snapshot CLI commands (allowSnapshot, disallowSnapshot, createSnapshot, deleteSnapshot, renameSnapshot, snapshot diff). Transformed to use ProcessBasedMiniDFSCluster with parameterized upgrade checkpoints. All operations use client-side APIs (DFSAdmin commands, FsShell commands, FileSystem APIs). Includes 8 test methods covering snapshot command functionality, max limits, reserved names, URI handling, and snapshot diff operations. |

---

## Phase 7: MEDIUM Priority - CLI & Client Tests

**Target Completion:** Week 10-11
**Progress:** 2/80 (2.5%)

Tests for command-line tools and client operations.

| Status | Test Class | Priority | Notes |
|--------|------------|----------|-------|
| ✅ | `org.apache.hadoop.cli.TestAclCLI` | MEDIUM | Completed - CLI framework test for ACL commands. Transformed to use ProcessBasedMiniDFSCluster. Extends CLITestHelperDFS which runs commands from testAclCLI.xml. All operations use client-side CLI commands (no parameterized checkpoints needed for framework-based tests) |
| ✅ | `org.apache.hadoop.cli.TestCacheAdminCLI` | MEDIUM | Completed - CLI framework test for CacheAdmin commands. Transformed to use ProcessBasedMiniDFSCluster. Extends CLITestHelper which runs cache admin commands from testCacheAdminConf.xml. All operations use client-side CacheAdmin CLI tool (no parameterized checkpoints needed for framework-based tests) |
| ✅ | `org.apache.hadoop.cli.TestCryptoAdminCLI` | MEDIUM | Completed - CLI framework test for CryptoAdmin commands. Transformed to use ProcessBasedMiniDFSCluster. Replaced server-side NameNode access (getNameNode().getNamesystem().getProvider()) with client-side KeyProvider API (DistributedFileSystem.getKeyProvider()). Reordered initialization to create FileSystem before creating key. All operations use client-side CryptoAdmin CLI tool |
| ✅ | `org.apache.hadoop.cli.TestDeleteCLI` | MEDIUM | Completed - CLI framework test for delete commands. Transformed to use ProcessBasedMiniDFSCluster. Extends CLITestHelperDFS which runs delete commands from testDeleteConf.xml. All operations use client-side CLI commands |
| ✅ | `org.apache.hadoop.cli.TestErasureCodingCLI` | MEDIUM | Completed - CLI framework test for erasure coding commands. Transformed to use ProcessBasedMiniDFSCluster. Uses client-side DistributedFileSystem.enableErasureCodingPolicy() API. Extends CLITestHelper which runs EC admin commands from testErasureCodingConf.xml |
| ✅ | `org.apache.hadoop.cli.TestHDFSCLI` | MEDIUM | Completed - CLI framework test for HDFS commands. Transformed to use ProcessBasedMiniDFSCluster. Removed .hosts() builder method (not supported by ProcessBasedMiniDFSCluster.Builder). Uses .racks() for topology testing. Extends CLITestHelperDFS which runs commands from testHDFSConf.xml |
| ✅ | `org.apache.hadoop.cli.TestXAttrCLI` | MEDIUM | Completed - CLI framework test for extended attributes (XAttr) commands. Transformed to use ProcessBasedMiniDFSCluster. Extends CLITestHelperDFS which runs XAttr commands from testXAttrConf.xml. All operations use client-side CLI commands |
| ❌ | `org.apache.hadoop.hdfs.client.impl.TestBlockReaderFactory` | MEDIUM | Skipped - Requires server-side API access. Uses cluster.getNameNode().getRpcServer().getBlockLocations() (lines 299, 607) and cluster.getDataNodes().get(0).getDatanodeId() (line 472) to test low-level BlockReader factory internals (short-circuit reads, UNIX domain sockets). These internal implementation details are not accessible through client-side APIs in ProcessBasedMiniDFSCluster |
| ❌ | `org.apache.hadoop.hdfs.client.impl.TestBlockReaderLocal` | MEDIUM | Skipped - Requires direct file system access to DataNode storage. Uses cluster.getBlockFile(0, block) and cluster.getBlockMetadataFile(0, block) (lines 187-188) to access physical block files for testing low-level BlockReaderLocal internals. ProcessBasedMiniDFSCluster does not support these methods due to process isolation |
| ❌ | `org.apache.hadoop.hdfs.client.impl.TestBlockReaderLocalLegacy` | MEDIUM | Skipped - Requires server-side API access. Uses cluster.getNameNode().getRpcServer() (lines 193, 217) to test legacy BlockReader internals. Similar to TestBlockReaderLocal and TestBlockReaderFactory, tests low-level block reading mechanisms not accessible through client-side APIs in ProcessBasedMiniDFSCluster |
| ✅ | `org.apache.hadoop.hdfs.protocol.datatransfer.sasl.TestSaslDataTransfer` | MEDIUM | Completed - SASL authentication test for data transfer. Transformed to use ProcessBasedMiniDFSCluster. Changed cluster.waitActive() to cluster.waitClusterUp() and added TimeoutException to startCluster() throws clause. All test methods use client-side FileSystem APIs (doTest method) or don't use cluster at all (mock-based tests). Tests authentication, integrity, privacy modes for secure data transfer |
| ❌ | `org.apache.hadoop.hdfs.protocol.datatransfer.sasl.TestSaslDataTransferExpiredBlockToken` | MEDIUM | Skipped - Requires server-side API access. Uses cluster.getNameNode().getNamesystem().getBlockManager().getBlockTokenSecretManager() (line 72) to access NameNode's internal BlockTokenSecretManager for testing expired block token handling. This server-side component is not accessible through client-side APIs in ProcessBasedMiniDFSCluster |
| ❌ | `org.apache.hadoop.hdfs.server.datanode.TestDatanodeProtocolRetryPolicy` | MEDIUM | Skipped - Not a cluster-based test. This is a unit test that creates a DataNode instance directly and uses mock NameNode objects (Mockito) to test DataNode's internal retry policy logic. Does not use MiniDFSCluster for testing (only uses MiniDFSCluster.getBaseDirectory() static utility at line 68). ProcessBasedMiniDFSCluster is designed for cluster-based integration tests, not isolated unit tests of server components |
| ✅ | `org.apache.hadoop.hdfs.TestClientProtocolForPipelineRecovery` | MEDIUM | Completed |
| ❌ | `org.apache.hadoop.hdfs.TestClientReportBadBlock` | MEDIUM | Skipped - Requires server-side API access. Uses cluster.getDataNode(dninfo.getIpcPort()) (line 222) to access DataNode object directly for testing bad block reporting. ProcessBasedMiniDFSCluster does not provide direct access to DataNode objects due to process isolation |
| ✅ | `org.apache.hadoop.hdfs.TestDFSClientExcludedNodes` | MEDIUM | Completed - Tests DFSClient excluded nodes handling. Transformed to use ProcessBasedMiniDFSCluster. Changed cluster.stopDataNode() to cluster.shutdownDataNode(), cluster.restartDataNode(DataNodeProperties) to cluster.restartDataNode(int index), and cluster.waitActive() to cluster.waitClusterUp(). Added TimeoutException to method signatures. Tests that clients properly exclude failed DataNodes and can forgive them after expiry period |
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
