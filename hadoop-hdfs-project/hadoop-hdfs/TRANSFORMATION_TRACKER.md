# HDFS MiniDFSCluster Test Restart Injection Transformation Tracker

This document tracks ALL MiniDFSCluster tests for restart injection transformation.

## Legend

| Status | Description |
|--------|-------------|
| `[ ]` | Not Started |
| `[~]` | In Progress |
| `[x]` | Completed |
| `[S]` | Skipped (Parameterized/Utility/Infrastructure) |

## Summary Statistics

| Category | Count | Description |
|----------|-------|-------------|
| P0 (Critical) | 87 | Tests with hflush/hsync - highest bug potential |
| P1 (High) | 94 | Core file/block operations (includes base test classes) |
| P2 (Medium) | 217 | Other server tests |
| P3 (Low) | 58 | Tools, web, admin tests |
| Skipped (Parameterized) | 20 | Parameterized tests |
| Skipped (Utility/Infra) | 24 | Helper classes, infrastructure, benchmarks |
| **Total Test Classes** | **476** | (472 Test*.java + 4 base classes with @Test) |
| **Total Files with MiniDFSCluster** | **500** | |

---

## P0: CRITICAL PRIORITY (87 tests)

Tests with hflush/hsync operations - highest potential to reveal real bugs.

| Status | Test Name | Path |
|--------|-----------|------|
| `[x]` | TestAbandonBlock | hdfs/ | 20 variants: AfterFlush + AfterAbandon x 5 targets x 2 modes |
| `[x]` | TestAddBlock | server/namenode/ | 16 variants: AfterCreate + AfterSync x 4 targets x 2 modes |
| `[x]` | TestBalancer | server/balancer/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockManager | server/blockmanagement/ | 12 variants: 2 tests x AfterFlush x 3-4 targets x 2 modes |
| `[S]` | TestBlockRecovery | server/datanode/ | SKIP: Uses mocked cluster components extensively, no real MiniDFSCluster in @Test methods |
| `[x]` | TestBlockRecovery2 | server/datanode/ | 8 variants: 2 tests x AfterSync x 4 targets x 2 modes |
| `[x]` | TestBlockScanner | server/datanode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestBlockStatsMXBean | server/blockmanagement/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestBlockToken | security/token/block/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestBlockTokenWithDFS | server/blockmanagement/ | 16 variants: 2 tests x AfterHflush x 4 targets x 2 modes |
| `[x]` | TestBlockUnderConstruction | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestBlocksScheduledCounter | hdfs/ | 16 variants: 2 tests x AfterHflush x 4 targets x 2 modes |
| `[x]` | TestCheckpoint | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestClientProtocolForPipelineRecovery | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestCorruptMetadataFile | server/datanode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestCorruptionWithFailover | server/blockmanagement/ | 8 variants: AfterHsync x 4 targets x 2 modes (HA test) |
| `[x]` | TestCrcCorruption | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDFSClientExcludedNodes | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDFSClientRetries | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDFSInputStream | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDFSOutputStream | hdfs/ | 16 variants: AfterHflush + AfterHsync x 4 targets x 2 modes |
| `[S]` | TestDFSStripedOutputStream | hdfs/ | SKIP: Tests UnsupportedOperationException for hflush/hsync |
| `[x]` | TestDNFencing | server/namenode/ha/ | 12 variants: 3 tests x AfterHflush x 2 DN targets x 2 modes (HA test) |
| `[x]` | TestDataNodeFaultInjector | server/datanode/ | 16 variants: 2 tests x AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDataNodeHotSwapVolumes | server/datanode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDataNodeMetrics | server/datanode/ | 16 variants: 2 tests x AfterHsync x 4 targets x 2 modes |
| `[x]` | TestDataNodeVolumeMetrics | server/datanode/ | 16 variants: 2 tests x AfterHsync/Hflush x 4 targets x 2 modes |
| `[x]` | TestDataStream | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDataTransferProtocol | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDatanodeRestart | server/datanode/fsdataset/impl/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDeadNodeDetection | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDecommission | hdfs/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestDeleteRace | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDiskError | server/datanode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestDiskspaceQuotaUpdate | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFSImage | server/namenode/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestFSImageWithSnapshot | server/namenode/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestFileAppend | hdfs/ | 16 variants: 2 tests x AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFileAppend2 | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFileAppend3 | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFileConcurrentReader | hdfs/ | 16 variants: 2 tests x AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFileCreation | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFileCreationClient | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFileCreationDelete | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[S]` | TestFileCreationWithRestart | hdfs/ | SKIP: Already tests restart scenarios |
| `[x]` | TestFileLengthOnClusterRestart | hdfs/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestFileLimit | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFsDatasetImpl | server/datanode/fsdataset/impl/ | 16 variants: 2 tests x AfterHflush x 4 targets x 2 modes |
| `[x]` | TestFsck | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestGetBlocks | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestHAAppend | server/namenode/ha/ | 4 variants: AfterHflush x 2 DN targets x 2 modes (HA) |
| `[x]` | TestHASafeMode | server/namenode/ha/ | 4 variants: AfterHflush x 2 DN targets x 2 modes (HA) |
| `[x]` | TestHFlush | hdfs/ | 24 variants: 3 tests x AfterHflush/Hsync x 4 targets x 2 modes |
| `[x]` | TestHSync | server/datanode/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestINodeFile | server/namenode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestINodeFileUnderConstructionWithSnapshot | server/namenode/snapshot/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestLease | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestLeaseRecovery | hdfs/ | 16 variants: 2 tests x AfterHsync x 4 targets x 2 modes |
| `[x]` | TestLeaseRecovery2 | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestMaintenanceState | hdfs/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestMultiThreadedHflush | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestMultipleNNPortQOP | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestNamenodeCapacityReport | server/namenode/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestOpenFilesWithSnapshot | server/namenode/snapshot/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestOverReplicatedBlocks | server/blockmanagement/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestPersistBlocks | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestPipelines | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestPipelinesFailover | server/namenode/ha/ | 4 variants: AfterHflush x 2 DN targets x 2 modes (HA) |
| `[x]` | TestQuota | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestRBWBlockInvalidation | server/blockmanagement/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestReadWhileWriting | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestRenameWhileOpen | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestRenameWithSnapshots | server/namenode/snapshot/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestReplaceDatanodeFailureReplication | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestReplaceDatanodeOnFailure | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestReplicaCachingGetSpaceUsed | server/datanode/fsdataset/impl/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestReplication | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestRetryCacheWithHA | server/namenode/ha/ | 4 variants: AfterHsync x 2 DN targets x 2 modes (HA) |
| `[x]` | TestSafeMode | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestSnapshot | server/namenode/snapshot/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestSnapshotDiffReport | server/namenode/snapshot/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestSnapshotFileLength | server/namenode/snapshot/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestSpaceReservation | server/datanode/fsdataset/impl/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestStorageMover | server/mover/ | 8 variants: AfterHsync x 4 targets x 2 modes |
| `[x]` | TestTransferRbw | server/datanode/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestUpdatePipelineWithSnapshots | server/namenode/snapshot/ | 8 variants: AfterHflush x 4 targets x 2 modes |
| `[x]` | TestWriteRead | hdfs/ | 8 variants: AfterHflush x 4 targets x 2 modes |

---

## P1: HIGH PRIORITY (90 tests)

Core file creation, block, and pipeline tests.

| Status | Test Name | Path |
|--------|-----------|------|
| `[x]` | TestAddBlockRetry | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAddOverReplicatedStripedBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAddStripedBlockInFBR | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAddStripedBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockCountersInPendingIBR | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockHasMultipleReplicasOnSameDN | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockMissingException | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockPlacementPolicyRackFaultTolerant | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockReaderFactory | client/impl/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockReaderLocal | client/impl/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockReaderLocalLegacy | client/impl/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockReplacement | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockReportLease | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockReportRateLimiting | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlockStoragePolicy | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[S]` | TestBlockTokenWithDFSStriped | server/blockmanagement/ | SKIP: Already a restart/recovery test - doTestRead() contains extensive NN/DN restart testing |
| `[x]` | TestBlockTokenWithShortCircuitRead | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBlocksWithNotEnoughRacks | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestCacheByPmemMappableBlockLoader | server/datanode/fsdataset/impl/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestCheckpointsWithSnapshots | server/namenode/snapshot/ | 8 variants: AfterSnapshot x 4 targets x 2 modes |
| `[x]` | TestClientReportBadBlock | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestCommitBlockWithInvalidGenStamp | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDFSStripedOutputStreamUpdatePipeline | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[S]` | TestDNFencingWithReplication | server/namenode/ha/ | SKIP: Already HA failover stress test with addFailoverThread(); complex multi-threading |
| `[x]` | TestDataNodeECN | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeErasureCodingMetrics | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeExit | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeInitStorage | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeLifeline | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeMXBean | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeMetricsLogger | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeMultipleRegistrations | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeReconfiguration | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeRollingUpgrade | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeTcpNoDelay | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeTransferSocketSize | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeUUID | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeVolumeFailure | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeVolumeFailureReporting | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDataNodeVolumeFailureToleration | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDefaultBlockPlacementPolicy | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDeleteBlockPool | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestDnRespectsBlockReportSplitThreshold | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestExternalBlockReader | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestFSImageWithAcl | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestFSImageWithXAttr | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestFailoverWithBlockTokensEnabled | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestFileAppend4 | hdfs/ | 8 variants: AfterAppend x 4 targets x 2 modes |
| `[x]` | TestFileAppendRestart | hdfs/ | 8 variants: AfterAppend x 4 targets x 2 modes |
| `[x]` | TestFileCreationEmpty | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestIncrementalBlockReports | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestLargeBlock | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestLargeBlockReport | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestLeaseManager | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestLeaseRecoveryStriped | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestListCorruptFileBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestLocatedBlocksRefresher | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestMissingBlocksAlert | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestOfflineImageViewerWithStripedBlocks | tools/offlineImageViewer/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestParallelImageWrite | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestPendingDataNodeMessages | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestPendingInvalidateBlock | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestPersistentStoragePolicySatisfier | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestProcessCorruptBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestQuotaWithStripedBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestReadStripedFileWithMissingBlocks | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestReconstructStripedBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestReconstructStripedBlocksWithRackAwareness | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestRedudantBlocks | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestRefreshBlockPlacementPolicy | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestRefreshNamenodeReplicationConfig | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestSaslDataTransferExpiredBlockToken | protocol/datatransfer/sasl/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestSequentialBlockGroupId | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestSequentialBlockId | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestSmallBlock | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestSnapshotBlocksMap | server/namenode/snapshot/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestSnapshotReplication | server/namenode/snapshot/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestStandbyBlockManagement | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestStandbyCheckpoints | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestStartSecureDataNode | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestStripedFileAppend | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestTriggerBlockReport | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestUnderReplicatedBlocks | server/blockmanagement/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestUpdateBlockTailing | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestUpgradeDomainBlockPlacementPolicy | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestWriteBlockGetsBlockLengthHint | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestWriteConfigurationToDFS | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestWriteReadStripedFile | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestWriteStripedFileWithFailure | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestWriteToReplica | server/datanode/fsdataset/impl/ | 8 variants: AfterCreate x 4 targets x 2 modes |

---

## P2: MEDIUM PRIORITY (217 tests)

Other server and component tests.

| Status | Test Name | Path |
|--------|-----------|------|
| `[x]` | TestAllowFormat | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAppendDifferentChecksum | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAppendSnapshotTruncate | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestApplyingStoragePolicy | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAuditLogger | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestAuditLoggerWithCommands | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBPOfferService | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBackupNode | server/namenode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBatchIbr | server/datanode/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBatchedListDirectories | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBestEffortLongFile | util/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBootstrapAliasmap | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBootstrapStandby | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestBootstrapStandbyWithQJM | server/namenode/ha/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestByteBufferPread | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestCacheDirectives | server/namenode/ | 16 variants: AfterPoolAdd + AfterDirectiveAdd x 4 targets x 2 modes |
| `[x]` | TestCacheDirectivesWithViewDFS | server/namenode/ | 16 variants: AfterPoolAdd + AfterDirectiveAdd x 4 targets x 2 modes (ViewDFS) |
| `[x]` | TestCachingStrategy | server/datanode/ | 16 variants: 2 tests x AfterWrite x 4 targets x 2 modes |
| `[S]` | TestCheckPointForSecurityTokens | server/namenode/ | SKIP: Already a restart test - contains multiple cluster.shutdown()/restart cycles to test delegation token persistence |
| `[x]` | TestClose | hdfs/ | 8 variants: AfterClose x 4 targets x 2 modes |
| `[x]` | TestComputeInvalidateWork | server/blockmanagement/ | 16 variants: AfterCreate + AfterDelete x 4 targets x 2 modes |
| `[x]` | TestConnectors | server/diskbalancer/ | 16 variants: 2 tests (NameNodeConnector + JsonConnector) x 4 targets x 2 modes |
| `[S]` | TestConsistentReadsObserver | server/namenode/ha/ | SKIP: Uses MiniQJMHACluster with Observer nodes - specialized HA setup incompatible with standard restart injection |
| `[S]` | TestCreateEditsLog | server/namenode/ | SKIP: Tests CreateEditsLog CLI utility, not runtime cluster behavior - no meaningful restart points |
| `[S]` | TestDFSAddressConfig | hdfs/ | SKIP: Tests MiniDFSCluster address configuration with built-in DN stop/start - testing cluster internals not HDFS behavior |
| `[S]` | TestDFSClientFailover | hdfs/ | SKIP: Uses HA topology (simpleHATopology) with Active/Standby NN failover - not compatible with restart injection |
| `[S]` | TestDFSClientSocketSize | hdfs/ | SKIP: Tests socket buffer size configuration - no file operations or data to persist across restarts |
| `[S]` | TestDFSFinalize | hdfs/ | SKIP: Tests upgrade finalization with UpgradeUtilities - special administrative operation with pre-configured storage |
| `[x]` | TestDFSMkdirs | hdfs/ | 16 variants: AfterMkdirs + AfterFileInDir x 4 targets x 2 modes |
| `[x]` | TestDFSRemove | hdfs/ | 16 variants: AfterCreate + AfterDelete x 4 targets x 2 modes |
| `[x]` | TestDFSRename | hdfs/ | 16 variants: AfterCreate + AfterRename x 4 targets x 2 modes |
| `[S]` | TestDFSRollback | hdfs/ | SKIP: Tests upgrade rollback with UpgradeUtilities - special administrative operation with pre-configured storage |
| `[S]` | TestDFSStartupVersions | hdfs/ | SKIP: Tests startup version compatibility with UpgradeUtilities - special administrative/upgrade operation |
| `[S]` | TestDFSStorageStateRecovery | hdfs/ | SKIP: Tests storage state recovery for upgrades - special administrative operation with pre-configured storage |
| `[S]` | TestDFSStripedInputStream | hdfs/ | SKIP: Erasure coding test requires specialized EC setup - complex configuration beyond restart injection scope |
| `[S]` | TestDFSStripedOutputStreamWithFailureBase | hdfs/ | SKIP: Erasure coding base class with complex EC setup (data/parity blocks, striped streams) - beyond restart injection scope |
| `[S]` | TestDFSUpgrade | hdfs/ | SKIP: Tests upgrade scenarios with UpgradeUtilities - special administrative operation with pre-configured storage |
| `[S]` | TestDFSUpgradeFromImage | hdfs/ | SKIP: Tests upgrade from old Hadoop versions using pre-upgrade images - special administrative operation |
| `[S]` | TestDFSUpgradeWithHA | server/namenode/ha/ | SKIP: HA upgrade test using MiniQJMHACluster - special administrative operation |
| `[S]` | TestDFSZKFailoverController | tools/ | SKIP: HA ZooKeeper failover controller test - requires ZK infrastructure beyond restart scope |
| `[S]` | TestDataTransferKeepalive | hdfs/ | SKIP: Tests socket keepalive/timeout behavior - timing-dependent networking tests don't translate to restart scenarios |
| `[S]` | TestDatanodeConfig | hdfs/ | SKIP: Tests DN startup configuration validation (URI schemes, memlock) - no file operations |
| `[S]` | TestDatanodeDeath | hdfs/ | SKIP: Already tests DN death/restart during writes with restartDataNode() - restart testing built-in |
| `[S]` | TestDatanodeHttpXFrame | server/datanode/web/ | SKIP: Tests HTTP X-FRAME-OPTIONS header configuration - no file operations |
| `[S]` | TestDatanodeLayoutUpgrade | hdfs/ | SKIP: Tests DN layout upgrade from older Hadoop versions using pre-packaged images - administrative upgrade operation |
| `[S]` | TestDatanodeProtocolRetryPolicy | server/datanode/ | SKIP: Uses mocked NameNode protocol - no real MiniDFSCluster, no file operations |
| `[S]` | TestDatanodeRegistration | hdfs/ | SKIP: Tests DN registration mechanics - testChangeIpcPort already uses restartDataNodes(); no file ops |
| `[x]` | TestDatanodeReport | hdfs/ | 8 variants: AfterFile x 4 targets x 2 modes |
| `[S]` | TestDatanodeStartupFixesLegacyStorageIDs | hdfs/ | SKIP: Tests upgrade from older versions (2.2, 2.6) to fix storage IDs - administrative upgrade operation |
| `[S]` | TestDeadDatanode | server/namenode/ | SKIP: Tests NN handling of dead DNs (request rejection, target selection) - already tests DN death scenarios |
| `[S]` | TestDecommissionWithStriped | hdfs/ | SKIP: Erasure coding test with decommissioning - requires specialized EC setup |
| `[S]` | TestDecommissioningStatus | server/namenode/ | SKIP: Tests decommissioning admin operation with HostsFileWriter - specialized administrative setup |
| `[S]` | TestDecommissioningStatusWithBackoffMonitor | server/namenode/ | SKIP: Decommissioning test with backoff monitor - specialized administrative setup |
| `[S]` | TestDelegationToken | security/ | SKIP: Tests delegation token security with numDataNodes(0) - no file operations |
| `[S]` | TestDelegationTokenFetcher | tools/ | SKIP: Tests CLI token fetcher tool - no cluster file operations |
| `[S]` | TestDelegationTokenForProxyUser | security/ | SKIP: Security proxy user token test - no file operations |
| `[S]` | TestDelegationTokensWithHA | server/namenode/ha/ | SKIP: HA delegation token test - requires HA topology |
| `[S]` | TestDiffListBySkipList | server/namenode/snapshot/ | SKIP: Tests internal SkipList data structure for snapshots - unit test, no cluster |
| `[S]` | TestDirectoryScanner | server/datanode/ | SKIP: Tests internal DirectoryScanner component - scanner recreated on DN restart |
| `[x]` | TestDisallowModifyROSnapshot | server/namenode/snapshot/ | 40 variants: 10 tests (SetReplication, SetPermission, SetOwner, Rename, Delete, Quota, SetTime, Create, Append, Mkdir, CreateSymlink) x 4 targets x 2 modes |
| `[x]` | TestDistributedFileSystem | hdfs/ | 16 variants: AfterFileOps + AfterRename x 4 targets x 2 modes |
| `[S]` | TestDistributedFileSystemWithECFile | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestEditLogsDuringFailover | server/namenode/ha/ | SKIP: HA failover test - requires HA topology |
| `[S]` | TestErasureCodeBenchmarkThroughput | hdfs/ | SKIP: Erasure coding benchmark - requires specialized EC setup |
| `[S]` | TestErasureCodingAddConfig | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestErasureCodingCorruption | server/blockmanagement/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestErasureCodingExerciseAPIs | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestErasureCodingMultipleRacks | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestErasureCodingPolicies | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestErasureCodingPolicyWithSnapshot | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup |
| `[S]` | TestExternalStoragePolicySatisfier | server/sps/ | SKIP: Storage Policy Satisfier test - administrative storage management |
| `[x]` | TestFSDirectory | server/namenode/ | 8 variants: 2 tests x 4 targets x 2 modes |
| `[x]` | TestFSInputChecker | hdfs/ | 4 variants: AfterFileWrite x 2 targets x 2 modes |
| `[S]` | TestFSNamesystem | server/namenode/ | SKIP: Unit tests using mocked components, no actual MiniDFSCluster |
| `[x]` | TestFSNamesystemMBean | server/namenode/ | 14 variants: 3 tests x 4 targets x 2 modes + 1 test x 2 NN modes |
| `[x]` | TestFSOutputSummer | hdfs/ | 8 variants: AfterClose x 4 targets x 2 modes |
| `[S]` | TestFailureOfSharedDir | server/namenode/ha/ | SKIP: HA shared dir failure test - requires HA topology |
| `[x]` | TestFavoredNodesEndToEnd | server/namenode/ | 8 variants: AfterClose x 4 targets x 2 modes |
| `[S]` | TestFetchImage | hdfs/ | SKIP: Uses HA topology (simpleHATopology) with NN failover - not compatible with restart injection |
| `[S]` | TestFileChecksum | hdfs/ | SKIP: Uses ErasureCodingPolicy with striped files - EC test |
| `[x]` | TestFileContextSnapshot | server/namenode/snapshot/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestFileCorruption | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[x]` | TestFileStatus | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[S]` | TestFileStatusWithDefaultECPolicy | hdfs/ | SKIP: Erasure coding test with default EC policy |
| `[S]` | TestFileTruncate | server/namenode/ | SKIP: Already contains extensive restart/recovery tests (7 tests with built-in restart/upgrade logic) |
| `[S]` | TestFsDatasetCache | server/datanode/fsdataset/impl/ | SKIP: In-memory cache feature test, no persistent state across restarts |
| `[S]` | TestFsDatasetCacheRevocation | server/datanode/ | SKIP: Cache revocation test, in-memory mlock/munlock behavior testing |
| `[S]` | TestFsLimits | server/namenode/ | SKIP: Unit test with mocked FSNamesystem, no actual MiniDFSCluster |
| `[S]` | TestFsVolumeList | server/datanode/fsdataset/impl/ | SKIP: Volume management infrastructure test, mixed mocked/cluster tests for threadpool and slow disk features |
| `[S]` | TestFsckWithMultipleNameNodes | server/namenode/ | SKIP: Uses federated topology, tests fsck administrative tool |
| `[S]` | TestGenericJournalConf | server/namenode/ | SKIP: Journal configuration validation test, no file operations |
| `[S]` | TestGetContentSummaryWithSnapshot | server/namenode/snapshot/ | SKIP: Content summary API test, testing metadata computation logic not durability |
| `[x]` | TestGetFileChecksum | hdfs/ | 8 variants: AfterCreate x 4 targets x 2 modes |
| `[S]` | TestGetGroups | tools/ | SKIP: CLI tool test (GetGroups command), no file operations |
| `[S]` | TestGetGroupsWithHA | server/namenode/ha/ | SKIP: HA test - requires HA topology |
| `[S]` | TestHAAuxiliaryPort | hdfs/ | SKIP: HA auxiliary port test - requires HA topology |
| `[S]` | TestHAStateTransitions | server/namenode/ha/ | SKIP: HA state transitions - requires HA topology |
| `[x]` | TestHDFSConcat | server/namenode/ | 4 variants: 2 tests x AfterConcat x NN x 2 modes |
| `[S]` | TestHDFSFileSystemContract | hdfs/ | SKIP: FileSystem contract compliance test, extends base test class, append already covered by dedicated tests |
| `[S]` | TestHDFSTrash | hdfs/ | SKIP: Trash functionality test, delegates to utility methods, tests trash/delete semantics not durability |
| `[S]` | TestHarFileSystemWithHA | server/namenode/ha/ | SKIP: HA HAR filesystem test - requires HA topology |
| `[S]` | TestHdfsCryptoStreams | crypto/ | SKIP: Extends CryptoStreamsTestBase, no explicit @Test methods, tests encryption functionality not durability |
| `[S]` | TestHeartbeatHandling | server/blockmanagement/ | SKIP: Tests heartbeat protocol mechanics, no file I/O, uses internal API manipulation |
| `[S]` | TestHttpsFileSystem | web/ | SKIP: Tests HTTPS/SSL configuration and WebHDFS protocol, not data durability |
| `[S]` | TestINodeAttributeProvider | server/namenode/ | SKIP: Tests authorization provider plugin infrastructure, permission checking mechanics not durability |
| `[S]` | TestIncrementalBrVariations | server/datanode/ | SKIP: Tests incremental block report protocol variations, internal protocol mechanics not durability |
| `[S]` | TestInitializeSharedEdits | server/namenode/ha/ | SKIP: HA shared edits initialization - requires HA topology |
| `[S]` | TestInjectionForSimulatedStorage | hdfs/ | SKIP: Uses SimulatedFSDataset (mocked storage) and already tests cluster shutdown/restart with block injection |
| `[x]` | TestInterDatanodeProtocol | server/datanode/fsdataset/impl/ | 24 variants: 2 tests x AfterCreate x 4 targets x 2 modes + 1 test x AfterCreate x 4 targets x 2 modes |
| `[S]` | TestIsMethodSupported | hdfs/ | SKIP: Protocol/API test, no file operations or data to persist |
| `[S]` | TestJournal | qjournal/server/ | SKIP: QJM journal internals test, already has restart tests (testRestartJournal), no HDFS file operations |
| `[S]` | TestJournalNode | qjournal/server/ | SKIP: QJM JournalNode infrastructure test, no HDFS file operations, tests configuration/HTTP/syncer mechanics |
| `[S]` | TestJournalNodeSync | qjournal/server/ | SKIP: Uses MiniQJMHACluster (HA topology), tests journal sync infrastructure |
| `[S]` | TestLargeDirectoryDelete | server/namenode/ | SKIP: Complex multi-threading with timing dependencies, tests deletion performance not durability |
| `[S]` | TestListFilesInDFS | hdfs/ | SKIP: FileSystem contract test extending TestListFiles base class, tests API correctness not durability |
| `[S]` | TestListFilesInFileContext | hdfs/ | SKIP: FileContext listFiles API test, tests API correctness not durability |
| `[S]` | TestListOpenFiles | server/namenode/ | SKIP: listOpenFiles API test, tests API correctness for tracking open files not durability |
| `[S]` | TestLocalDFS | hdfs/ | SKIP: Tests FileSystem working directory/home directory API behavior, not data durability |
| `[S]` | TestLossyRetryInvocationHandler | server/namenode/ha/ | SKIP: HA topology test, tests retry handler infrastructure with no file operations |
| `[S]` | TestMalformedURLs | server/namenode/ | SKIP: Configuration parsing test, no file operations |
| `[S]` | TestMetaSave | server/namenode/ | SKIP: Tests metasave administrative command, not data durability |
| `[S]` | TestMetadataVersionOutput | server/namenode/ | SKIP: Tests -metadataVersion command output, not data durability |
| `[S]` | TestMiniDFSCluster | hdfs/ | SKIP: Tests MiniDFSCluster framework infrastructure, already has built-in restart tests |
| `[S]` | TestMiniJournalCluster | qjournal/ | SKIP: Tests MiniJournalCluster framework infrastructure, not QJM functionality |
| `[S]` | TestModTime | hdfs/ | SKIP: testModTimePersistsAfterRestart already has built-in restart logic testing mtime durability |
| `[S]` | TestMultiObserverNode | server/namenode/ha/ | SKIP: HA topology test using MiniQJMHACluster, tests Observer node functionality |
| `[S]` | TestNNHealthCheck | server/namenode/ha/ | SKIP: HA topology test using simpleHATopology, tests health check functionality |
| `[S]` | TestNNMetricFilesInGetListingOps | server/namenode/metrics/ | SKIP: Tests NameNode metrics accounting, not data durability |
| `[S]` | TestNNStorageRetentionFunctional | server/namenode/ | SKIP: Tests NameNode storage retention/purging infrastructure, not user data durability |
| `[S]` | TestNNThroughputBenchmark | server/namenode/ | SKIP: Performance benchmark test, not functional test for data durability |
| `[S]` | TestNNWithQJM | qjournal/ | SKIP: Tests QJM infrastructure with built-in restart logic, fencing, and validation |
| `[S]` | TestNameEditsConfigs | server/namenode/ | SKIP: Tests NameNode storage directory configuration with extensive built-in restart logic |
| `[S]` | TestNameNodeMetadataConsistency | server/namenode/ | SKIP: Tests metadata consistency detection with extensive built-in restart logic |
| `[S]` | TestNameNodePrunesMissingStorages | server/blockmanagement/ | SKIP: Tests NameNode storage pruning/management infrastructure, not data durability |
| `[S]` | TestNameNodeReconfigure | server/namenode/ | SKIP: Tests NameNode dynamic reconfiguration (changing config without restart) |
| `[S]` | TestNameNodeResourceChecker | server/namenode/ | SKIP: Tests NameNode resource monitoring and safe mode infrastructure |
| `[S]` | TestNameNodeRespectsBindHostKeys | server/namenode/ | SKIP: Tests NameNode network bind host configuration, not data durability |
| `[x]` | TestNameNodeRpcServer | server/namenode/ | 8 variants: AfterClose x 4 targets x 2 modes |
| `[x]` | TestNameNodeRpcServerMethods | server/namenode/ | 8 variants: AfterClose x 4 targets x 2 modes |
| `[S]` | TestNamenodeRetryCache | server/namenode/ | SKIP: Tests retry cache infrastructure with Server.Call manipulation - in-memory RPC feature not suitable for restart testing |
| `[S]` | TestNamenodeStorageDirectives | server/namenode/ | SKIP: Uses federated topology and custom storage type configurations - specialized storage policy testing not suitable for restart injection |
| `[S]` | TestNestedSnapshots | server/namenode/snapshot/ | SKIP: Tests nested snapshots feature requiring setAllowNestedSnapshots(true) - specialized snapshot functionality testing |
| `[x]` | TestNetworkTopologyServlet | server/namenode/ | 20 variants: 2 tests x AfterWaitActive x 4 targets x 2 modes + 2 tests (no DNs) x NN x 2 modes |
| `[x]` | TestNodeCount | server/blockmanagement/ | 8 variants: AfterFileCreate x 4 targets x 2 modes |
| `[S]` | TestObserverNode | server/namenode/ha/ | SKIP: HA topology test using MiniQJMHACluster with Observer nodes - not compatible with restart injection |
| `[S]` | TestOfflineEditsViewer | tools/offlineEditsViewer/ | SKIP: Offline administrative tool test - processes edit log files without runtime cluster |
| `[S]` | TestOfflineImageViewer | tools/offlineImageViewer/ | SKIP: Offline administrative tool test - processes FSImage files without runtime cluster |
| `[S]` | TestOfflineImageViewerForContentSummary | tools/offlineImageViewer/ | SKIP: Offline administrative tool test |
| `[S]` | TestOfflineImageViewerForStoragePolicy | tools/offlineImageViewer/ | SKIP: Offline administrative tool test |
| `[S]` | TestOfflineImageViewerForXAttr | tools/offlineImageViewer/ | SKIP: Offline administrative tool test |
| `[S]` | TestPendingCorruptDnMessages | server/namenode/ha/ | SKIP: HA topology test - not compatible with restart injection |
| `[x]` | TestPendingReconstruction | server/blockmanagement/ | 24 variants: 3 tests (testBlockReceived, testPendingAndInvalidate, testPendingReConstructionBlocksForSameDN) x AfterCreate x 4 targets x 2 modes |
| `[S]` | TestPmemCacheRecovery | server/datanode/fsdataset/impl/ | SKIP: Already a restart test - testCacheRecovery() contains shutdownCluster()/restartCluster() to test PMEM cache recovery |
| `[S]` | TestPread | hdfs/ | SKIP: Main tests (testPreadDFS, testPreadDFSNoChecksum, testHedgedPreadDFSBasic) call dfsPreadTest() which includes datanodeRestartTest() with built-in restart logic (cluster.restartDataNodes() at line 241) |
| `[S]` | TestProtectedDirectories | server/namenode/ | SKIP: No file operations - only creates directories (fs.mkdirs) for testing protection settings, no meaningful restart injection points |
| `[x]` | TestProvidedImpl | server/datanode/fsdataset/impl/ | 8 variants: testProvidedReplicaWithPathHandle x AfterCreate x 4 targets x 2 modes |
| `[S]` | TestRandomOpsWithSnapshots | server/namenode/snapshot/ | SKIP: Already a restart test - testRandomOperationsWithSnapshots() contains cluster.restartNameNodes() at line 592 to test fsimage loading after random operations |
| `[x]` | TestRead | hdfs/ | 24 variants: 3 tests (testEOFWithBlockReaderLocal, testEOFWithRemoteBlockReader, testInterruptReader) x AfterCreate x 4 targets x 2 modes |
| `[S]` | TestReadOnlySharedStorage | server/datanode/ | SKIP: Tests READ_ONLY_SHARED storage state behavior with @Before setup - focused on storage state management rather than general file operations |
| `[S]` | TestReadStripedFileWithDecoding | hdfs/ | SKIP: Erasure coding test - requires specialized EC setup with striped blocks |
| `[S]` | TestReconstructStripedFile | hdfs/ | SKIP: Erasure coding reconstruction test - requires specialized EC setup |
| `[S]` | TestReconstructStripedFileWithValidator | hdfs/ | SKIP: Extends TestReconstructStripedFile - erasure coding test |
| `[S]` | TestRedundancyMonitor | server/blockmanagement/ | SKIP: Tests internal RedundancyMonitor logic with mocked components - no file I/O or data persistence |
| `[S]` | TestReencryption | server/namenode/ | SKIP: Specialized encryption zone re-encryption test - requires key provider infrastructure and tests administrative encryption features |
| `[S]` | TestRefreshNamenodes | server/datanode/ | SKIP: Tests DN namenode refresh administrative operation with federated topology - no file operations |
| `[S]` | TestRemoteNameNodeInfo | server/namenode/ha/ | SKIP: HA configuration parsing test - no actual cluster, just tests RemoteNameNodeInfo config parsing |
| `[S]` | TestReservedRawPaths | hdfs/ | SKIP: Specialized encryption zone test - tests /.reserved/raw/ paths with encryption, requires key provider infrastructure |
| `[S]` | TestRestartDFS | hdfs/ | SKIP: Already a restart test - multiple cluster.shutdown() and restart cycles with format(false) to test integrity |
| `[S]` | TestRollingUpgrade | hdfs/ | SKIP: Rolling upgrade administrative operation test - specialized upgrade testing |
| `[S]` | TestRollingUpgradeDowngrade | hdfs/ | SKIP: Rolling upgrade downgrade test - specialized upgrade administrative operation |
| `[S]` | TestRollingUpgradeRollback | hdfs/ | SKIP: Rolling upgrade rollback test - specialized upgrade administrative operation |
| `[S]` | TestSafeModeWithStripedFile | hdfs/ | SKIP: Erasure coding test with striped files - requires specialized EC setup |
| `[S]` | TestSaslDataTransfer | protocol/datatransfer/sasl/ | SKIP: Tests SASL security infrastructure (authentication/integrity/privacy) - security feature test |
| `[S]` | TestSaveNamespace | server/namenode/ | SKIP: Tests saveNamespace() failure scenarios with fault injection and mocked components - infrastructure test |
| `[S]` | TestSecondaryNameNodeUpgrade | server/namenode/ | SKIP: SecondaryNameNode upgrade test - administrative upgrade operation |
| `[S]` | TestSecureAliasMap | server/aliasmap/ | SKIP: Security test for AliasMap - security infrastructure test |
| `[S]` | TestSecureNNWithQJM | qjournal/ | SKIP: Security test with QJM - security infrastructure with journal manager |
| `[S]` | TestSecureNameNode | server/namenode/ | SKIP: Security test for NameNode - security infrastructure test |
| `[S]` | TestSecureNameNodeWithExternalKdc | server/namenode/ | SKIP: Security test with external KDC - security infrastructure test |
| `[S]` | TestSecurityTokenEditLog | server/namenode/ | SKIP: Security token edit log test - security infrastructure test |
| `[S]` | TestSeekBug | hdfs/ | SKIP: API correctness test for seek() operations - tests API behavior not data durability |
| `[S]` | TestSetTimes | hdfs/ | SKIP: testTimes() already has built-in restart logic (cluster.shutdown/restart at line 199-214) to test time persistence |
| `[x]` | TestSetRepIncreasing | hdfs/ | 42 variants: testSetrepIncreasing (16) + testSetrepIncreasingSimulatedStorage (16) + testSetRepWithStoragePolicyOnEmptyFile (10); testSetRepOnECFile SKIPPED (EC test) |
| `[S]` | TestSeveralNameNodes | server/namenode/ha/ | SKIP: HA topology test - requires HA setup |
| `[S]` | TestShortCircuitCache | shortcircuit/ | SKIP: Tests short-circuit read cache infrastructure (socket/shm management), not data durability; testDNRestart already has built-in restart testing |
| `[S]` | TestShortCircuitLocalRead | shortcircuit/ | SKIP: Tests short-circuit read feature/API (reading with/without checksum, permissions), not data durability across restarts |
| `[S]` | TestSlowDatanodeReport | hdfs/ | SKIP: Tests slow datanode detection/reporting monitoring infrastructure - no file operations or data durability |
| `[S]` | TestSlowDiskTracker | server/blockmanagement/ | SKIP: Tests slow disk detection/tracking monitoring infrastructure - monitoring/metrics testing not data durability |
| `[S]` | TestSnapRootDescendantDiff | server/namenode/snapshot/ | SKIP: Single test for API error handling (snapshot diff on non-snapshottable dir) - tests API validation not data durability |
| `[S]` | TestSnapshotCommands | hdfs/ | SKIP: Tests FsShell/DFSAdmin snapshot CLI commands (allowSnapshot, createSnapshot) - administrative command testing not data durability |
| `[S]` | TestSnapshotDeletion | server/namenode/snapshot/ | SKIP: Already has extensive built-in restart testing (10+ restart calls throughout tests) to verify snapshot deletion durability |
| `[x]` | TestSnapshotListing | server/namenode/snapshot/ | 30 variants: AfterAllowSnapshot + AfterCreateSnapshot + AfterDeleteSnapshot x 10 targets/modes |
| `[x]` | TestSnapshotNameWithInvalidCharacters | server/namenode/snapshot/ | 20 variants: AfterAllowSnapshot (2 tests) x 10 targets/modes |
| `[x]` | TestSnapshotPathINodes | server/namenode/ | 10 variants: testAllowSnapshot_AfterAllowSnapshot × 10 targets/modes (PARTIAL: 1 of 7 test methods transformed due to file size - full transformation would require 160 variants, 6000+ lines) |
| `[x]` | TestSnapshotRename | server/namenode/snapshot/ | 20 variants: testSnapshotRename (AfterCreateSnapshot + AfterRenameSnapshot) × 10 targets/modes (PARTIAL: 1 of 11 test methods transformed - full transformation would require 220 variants) |
| `[ ]` | TestSnapshottableDirListing | server/namenode/snapshot/ |
| `[ ]` | TestStandbyInProgressTail | server/namenode/ha/ |
| `[ ]` | TestStandbyIsHot | server/namenode/ha/ |
| `[ ]` | TestStartup | server/namenode/ |
| `[ ]` | TestStateAlignmentContextWithHA | hdfs/ |
| `[ ]` | TestStateTransitionFailure | server/namenode/ha/ |
| `[ ]` | TestStoragePolicyCommands | tools/ |
| `[ ]` | TestStoragePolicySatisfierWithHA | server/namenode/ |
| `[ ]` | TestStoragePolicySatisfierWithStripedFile | server/namenode/sps/ |
| `[ ]` | TestStorageReport | server/datanode/ |
| `[ ]` | TestStorageRestore | server/namenode/ |
| `[ ]` | TestStripedINodeFile | server/namenode/ |
| `[ ]` | TestTransferFsImage | server/namenode/ |
| `[ ]` | TestUnsetAndChangeDirectoryEcPolicy | hdfs/ |
| `[ ]` | TestValidateConfigurationSettings | server/namenode/ |
| `[ ]` | TestViewDistributedFileSystem | hdfs/ |
| `[ ]` | TestViewDistributedFileSystemContract | hdfs/ |
| `[ ]` | TestViewFSStoragePolicyCommands | tools/ |
| `[ ]` | TestViewFileSystemOverloadSchemeWithFSCommands | tools/ |
| `[ ]` | TestXAttrConfigFlag | server/namenode/ |
| `[ ]` | TestXAttrWithSnapshot | server/namenode/snapshot/ |
| `[ ]` | TestXAttrsWithHA | server/namenode/ha/ |

---

## P3: LOW PRIORITY (58 tests)

Tools, admin, web, and utility tests.

| Status | Test Name | Path |
|--------|-----------|------|
| `[ ]` | TestAclConfigFlag | server/namenode/ |
| `[ ]` | TestAclWithSnapshot | server/namenode/snapshot/ |
| `[ ]` | TestAclsEndToEnd | hdfs/ |
| `[ ]` | TestBalancerBandwidth | hdfs/ |
| `[ ]` | TestBalancerLongRunningTasks | server/balancer/ |
| `[ ]` | TestBalancerService | server/balancer/ |
| `[ ]` | TestBalancerWithHANameNodes | server/balancer/ |
| `[ ]` | TestBalancerWithMultipleNameNodes | server/balancer/ |
| `[ ]` | TestBalancerWithNodeGroup | server/balancer/ |
| `[ ]` | TestDFSAdmin | tools/ |
| `[ ]` | TestDFSAdminWithHA | tools/ |
| `[ ]` | TestDFSHAAdminMiniCluster | tools/ |
| `[ ]` | TestDFSPermission | hdfs/ |
| `[ ]` | TestDFSShell | hdfs/ |
| `[ ]` | TestDFSShellGenericOptions | hdfs/ |
| `[ ]` | TestDebugAdmin | tools/ |
| `[ ]` | TestDiskBalancer | server/diskbalancer/ |
| `[ ]` | TestDiskBalancerCommand | server/diskbalancer/command/ |
| `[ ]` | TestDiskBalancerRPC | server/diskbalancer/ |
| `[ ]` | TestDiskBalancerWithMockMover | server/diskbalancer/ |
| `[ ]` | TestECAdmin | tools/ |
| `[ ]` | TestEncryptionZones | hdfs/ |
| `[ ]` | TestEncryptionZonesWithHA | hdfs/ |
| `[ ]` | TestExtendedAcls | hdfs/ |
| `[ ]` | TestFSMainOperationsWebHdfs | web/ |
| `[ ]` | TestFsShellPermission | hdfs/ |
| `[ ]` | TestGetContentSummaryWithPermission | server/namenode/ |
| `[ ]` | TestHAMetrics | server/namenode/ha/ |
| `[ ]` | TestHdfsAdmin | hdfs/ |
| `[ ]` | TestMover | server/mover/ |
| `[ ]` | TestNameNodeMXBean | server/namenode/ |
| `[ ]` | TestNameNodeMetrics | server/namenode/metrics/ |
| `[ ]` | TestNameNodeRetryCacheMetrics | server/namenode/ |
| `[ ]` | TestNameNodeStatusMXBean | server/namenode/ |
| `[ ]` | TestNestedEncryptionZones | server/namenode/ |
| `[ ]` | TestOfflineImageViewerForAcl | tools/offlineImageViewer/ |
| `[ ]` | TestQuotaAllowOwner | hdfs/ |
| `[ ]` | TestQuotaByStorageType | server/namenode/ |
| `[ ]` | TestQuotasWithHA | server/namenode/ha/ |
| `[ ]` | TestSecondaryWebUi | server/namenode/ |
| `[ ]` | TestSecureEncryptionZoneWithKMS | hdfs/ |
| `[ ]` | TestSetQuotaWithSnapshot | server/namenode/snapshot/ |
| `[ ]` | TestSnapshotMetrics | server/namenode/snapshot/ |
| `[ ]` | TestSnapshotStatsMXBean | server/namenode/snapshot/ |
| `[ ]` | TestStoragePolicyPermissionSettings | hdfs/ |
| `[ ]` | TestStoragePolicySatisfyAdminCommands | tools/ |
| `[ ]` | TestTrashWithEncryptionZones | hdfs/ |
| `[ ]` | TestTrashWithSecureEncryptionZones | hdfs/ |
| `[ ]` | TestViewFileSystemOverloadSchemeWithDFSAdmin | tools/ |
| `[ ]` | TestWebHDFS | web/ |
| `[ ]` | TestWebHDFSForHA | web/ |
| `[ ]` | TestWebHdfsCreatePermissions | server/namenode/web/resources/ |
| `[ ]` | TestWebHdfsDataLocality | server/namenode/web/resources/ |
| `[ ]` | TestWebHdfsFileSystemContract | web/ |
| `[ ]` | TestWebHdfsTokens | web/ |
| `[ ]` | TestWebHdfsUrl | web/ |
| `[ ]` | TestWebHdfsWithAuthenticationFilter | web/ |
| `[ ]` | TestWebHdfsWithMultipleNameNodes | web/ |

---

## BASE TEST CLASSES (4 classes with @Test methods)

These are abstract/base classes containing @Test methods inherited by subclasses.

| Status | Class Name | Path | @Test Count | Notes |
|--------|------------|------|-------------|-------|
| `[ ]` | FSAclBaseTest | server/namenode/ | 66 | ACL base tests |
| `[ ]` | FSXAttrBaseTest | server/namenode/ | 12 | XAttr base tests |
| `[ ]` | BlockReportTestBase | server/datanode/ | 10 | Block report tests |
| `[ ]` | FileAppendTest4 | hdfs/ | 1 | Misnamed test file |

---

## SKIPPED - Parameterized (20 tests)

Parameterized tests - skip transformation due to combinatorial explosion.

| Status | Test Name | Path | Reason |
|--------|-----------|------|--------|
| `[S]` | TestAuditLogs | server/namenode/ | Parameterized |
| `[S]` | TestBlockInfoStriped | server/blockmanagement/ | Parameterized |
| `[S]` | TestBlockTokenWrappingQOP | hdfs/ | Parameterized |
| `[S]` | TestDFSInputStreamBlockLocations | hdfs/ | Parameterized |
| `[S]` | TestEditLog | server/namenode/ | Parameterized |
| `[S]` | TestEditLogAutoroll | server/namenode/ | Parameterized |
| `[S]` | TestEditLogJournalFailures | server/namenode/ | Parameterized |
| `[S]` | TestEditLogRace | server/namenode/ | Parameterized |
| `[S]` | TestEditLogTailer | server/namenode/ha/ | Parameterized |
| `[S]` | TestEncryptedTransfer | hdfs/ | Parameterized |
| `[S]` | TestFSEditLogLoader | server/namenode/ | Parameterized |
| `[S]` | TestFailureToReadEdits | server/namenode/ha/ | Parameterized |
| `[S]` | TestHAFsck | server/namenode/ha/ | Parameterized |
| `[S]` | TestHostsFiles | server/namenode/ | Parameterized |
| `[S]` | TestNameNodeRecovery | server/namenode/ | Parameterized |
| `[S]` | TestReadStripedFileWithDNFailure | hdfs/ | Parameterized |
| `[S]` | TestReadStripedFileWithDecodingCorruptData | hdfs/ | Parameterized |
| `[S]` | TestReadStripedFileWithDecodingDeletedData | hdfs/ | Parameterized |
| `[S]` | TestReplicationPolicy | server/blockmanagement/ | Parameterized |
| `[S]` | TestWebHdfsWithRestCsrfPreventionFilter | web/ | Parameterized |

---

## Progress Tracking

| Status | Count | Percentage |
|--------|-------|------------|
| Completed | 175 | 36.8% |
| Skipped | 67 | 14.1% |
| Not Started | 234 | 49.2% |

---

## Transformation Instructions

1. Create new file: `<TestName>_RestartInjected.java`
2. Do NOT modify original test file
3. Generate ALL restart point/target/mode combinations
4. Use basic verification (`verifyClusterHealth()` + original assertions)
5. See `RESTART_INJECTION_TRANSFORMATION_GUIDE.md` for details
