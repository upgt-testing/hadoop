# DEBUG_TRACKER - HDFS

Generated: 2026-01-08
Total Failure Groups: 95

---

## Priority Summary

- **HIGHEST PRIORITY**: 7 groups - Most likely actual bugs (NPE/IndexOutOfBounds from production code)
- **MEDIUM PRIORITY**: 25 groups - Potential bugs requiring investigation
- **LOWER PRIORITY**: 63 groups - Likely false positives (timeouts, test code issues, restart framework issues)

---

## HIGHEST PRIORITY - Most Likely Actual Bugs

### Group 12: NullPointerException in BlocksMap.getStoredBlock

[x] TEST-BUG - See bugs/TEST-BUG-GROUP-12.md

**Priority Reason**: NullPointerException from production HDFS code (org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap). This indicates a potential race condition or state issue in block management during restart.

**Resolution**: TEST-BUG - The test code does not refresh `fsn`, `fsdir`, and `blockmanager` references after NameNode restart. After restart, the old BlockManager's `BlocksMap.blocks` field is null (set by `close()` during shutdown), causing NPE when the test uses stale references.

**Test Executions**: 13 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java)
```

**Raw Stack Trace Sample**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java)
```

**Test Executions (Examples)**:

1. Test: `org.apache.hadoop.hdfs.server.blockmanagement.TestBlockManager_RestartInjected.testBlockReportQueueing`
   - "position": "after_first_block_report"
   - "target": "datanode"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "036-da5a8d8d"

---

### Group 27: NullPointerException in BlocksMap.numNodes

[ ] Not started

**Priority Reason**: NullPointerException from production HDFS code (BlocksMap.numNodes). Block management state corruption during restart.

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.numNodes(BlocksMap.java)
```

**Test Executions (Examples)**:

1. Test: Block management related tests
   - "position": "during block operations"
   - "target": "datanode/namenode"
   - "mode": "GRACEFUL"

---

### Group 48: NullPointerException in BlockManager.chooseExcessRedundancies

[ ] Not started

**Priority Reason**: NullPointerException from production HDFS code (BlockManager.chooseExcessRedundancies). Indicates state issue in block replication management.

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.chooseExcessRedundancies(BlockManager.java)
```

---

### Group 25: NullPointerException in FSImageFormatPBSnapshot.Loader

[ ] Not started

**Priority Reason**: NullPointerException from production HDFS code during snapshot loading. Critical for data integrity.

**Test Executions**: 6 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.FSImageFormatPBSnapshot$Loader.loadSnapshotSection(FSImageFormatPBSnapshot.java)
```

---

### Group 46: IndexOutOfBoundsException in ArrayList

[ ] Not started

**Priority Reason**: IndexOutOfBoundsException indicating potential concurrent access or state corruption during restart.

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
java.lang.IndexOutOfBoundsException
	at java.util.ArrayList.rangeCheck(ArrayList.java)
```

---

### Group 18: IllegalMonitorStateException in ReentrantReadWriteLock

[ ] Not started

**Priority Reason**: IllegalMonitorStateException indicates lock state corruption - potential concurrency bug during restart.

**Test Executions**: 7 failures

**Generalized Stack Trace**:
```
java.lang.IllegalMonitorStateException
	at java.util.concurrent.locks.ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java)
```

---

### Group 9: IllegalStateException from Preconditions.checkState

[ ] Not started

**Priority Reason**: IllegalStateException from production code - unexpected state during restart operations.

**Test Executions**: 21 failures

**Generalized Stack Trace**:
```
java.lang.IllegalStateException
	at org.apache.hadoop.thirdparty.com.google.common.base.Preconditions.checkState(Preconditions.java)
```

---

## MEDIUM PRIORITY - Potential Bugs

### Group 5: IOException in DataStreamer.handleBadDatanode

[ ] Not started

**Priority Reason**: IOException from production streaming code - may indicate restart handling issues in data pipeline.

**Test Executions**: 26 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.handleBadDatanode(DataStreamer.java)
```

---

### Group 11: IOException in DataStreamer.findNewDatanode

[ ] Not started

**Priority Reason**: IOException during datanode recovery - potential restart handling gap.

**Test Executions**: 19 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.findNewDatanode(DataStreamer.java)
```

---

### Group 2: StandbyException from StandbyState.checkOperation

[ ] Not started

**Priority Reason**: HA state management issue - requests hitting standby during failover/restart.

**Test Executions**: 86 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

---

### Group 6: StandbyException (direct)

[ ] Not started

**Priority Reason**: HA state issue during restart - similar to Group 2.

**Test Executions**: 26 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.StandbyException
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

---

### Group 3: AccessControlException in FSPermissionChecker

[ ] Not started

**Priority Reason**: Security exception from production code - may indicate state issue after restart.

**Test Executions**: 31 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException)
	at org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker.checkSuperuserPrivilege(FSPermissionChecker.java)
```

---

### Group 10: SnapshotException from SnapshotManager

[ ] Not started

**Priority Reason**: Snapshot state management issue during restart.

**Test Executions**: 20 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.hdfs.protocol.SnapshotException)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.checkNestedSnapshottable(SnapshotManager.java)
```

---

### Group 8: ComparisonFailure in Assert.assertEquals

[ ] Not started

**Priority Reason**: Test assertion failures - may indicate state inconsistency after restart.

**Test Executions**: 23 failures

**Generalized Stack Trace**:
```
org.junit.ComparisonFailure
	at org.junit.Assert.assertEquals(Assert.java)
```

---

### Group 13: EOFException in DataInputStream

[ ] Not started

**Priority Reason**: Connection terminated unexpectedly during restart - may indicate timing issue.

**Test Executions**: 12 failures

**Generalized Stack Trace**:
```
Caused by: java.io.EOFException
	at java.io.DataInputStream.readInt(DataInputStream.java)
```

**Test Executions (Examples)**:

1. Test: `org.apache.hadoop.hdfs.server.namenode.TestCacheDirectives_RestartInjected.testCacheManagerRestart`
   - "position": "after_first_checkpoint"
   - "target": "namenode"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "020-7a1510cd"

---

### Group 14: IOException - Filesystem closed

[ ] Not started

**Priority Reason**: Client attempting operations on closed filesystem after restart.

**Test Executions**: 9 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DFSClient.checkOpen(DFSClient.java)
```

**Test Executions (Examples)**:

1. Test: `org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones_RestartInjected.testNestedEZWithRoot`
   - "position": "after_init_root_ez"
   - "target": "namenode"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "029-09755848"

---

### Group 15: IOException in DataNode.getDiskBalancer

[ ] Not started

**Priority Reason**: Disk balancer state issue during restart.

**Test Executions**: 9 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getDiskBalancer(DataNode.java)
```

---

### Group 16: HATestUtil.CouldNotCatchUpException

[ ] Not started

**Priority Reason**: HA synchronization issue - standby couldn't catch up after restart.

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil$CouldNotCatchUpException
	at org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil.waitForStandbyToCatchUp(HATestUtil.java)
```

---

### Group 20: FileNotFoundException in INodeDirectory

[ ] Not started

**Priority Reason**: Directory state inconsistency after restart.

**Test Executions**: 7 failures

**Generalized Stack Trace**:
```
Caused by: java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.namenode.INodeDirectory.valueOf(INodeDirectory.java)
```

---

### Group 21: FileNotFoundException in FSNamesystem.checkLease

[ ] Not started

**Priority Reason**: Lease state inconsistency after restart.

**Test Executions**: 7 failures

**Generalized Stack Trace**:
```
java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkLease(FSNamesystem.java)
```

---

### Group 22: IOException in StripeReader.checkMissingBlocks

[ ] Not started

**Priority Reason**: Erasure coding stripe state issue after restart.

**Test Executions**: 7 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.StripeReader.checkMissingBlocks(StripeReader.java)
```

---

### Group 23: ReplicaNotFoundException in FsDatasetImpl

[ ] Not started

**Priority Reason**: Block replica state inconsistency after restart.

**Test Executions**: 6 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getReplicaInfo(FsDatasetImpl.java)
```

---

### Group 28: CannotObtainBlockLengthException

[ ] Not started

**Priority Reason**: Block metadata inconsistency after restart.

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException
	at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java)
```

---

### Group 30: InconsistentFSStateException in FSImage

[ ] Not started

**Priority Reason**: Filesystem state corruption during restart - HIGH value for inspection despite being "Caused by".

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.hdfs.server.common.InconsistentFSStateException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.checkUpgrade(FSImage.java)
```

---

### Group 31: IOException in FsVolumeImpl.getBlockPoolSlice

[ ] Not started

**Priority Reason**: Volume state issue after restart.

**Test Executions**: 4 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl.getBlockPoolSlice(FsVolumeImpl.java)
```

---

### Group 38: IOException in FSEditLog.checkForGaps

[ ] Not started

**Priority Reason**: Edit log corruption detection - critical for data integrity.

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.FSEditLog.checkForGaps(FSEditLog.java)
```

---

## LOWER PRIORITY - Likely False Positives

### Group 1: TimeoutException waiting for condition

[ ] Not started

**Priority Reason**: TimeoutException - lower priority per guidelines as likely test flakiness.

**Test Executions**: 101 failures

**Generalized Stack Trace**:
```
java.util.concurrent.TimeoutException
Timed out waiting for condition.
```

---

### Group 35: TestTimedOutException

[ ] Not started

**Priority Reason**: Test timeout - lower priority per guidelines.

**Test Executions**: 3 failures

**Generalized Stack Trace**:
```
org.junit.runners.model.TestTimedOutException
	at java.lang.Thread.sleep(Native Method)
```

---

### Group 4: Exception from HdfsClusterAdapter.restartDataNode

[ ] Not started

**Priority Reason**: Exception DIRECTLY from restarttest module - test infrastructure issue.

**Test Executions**: 30 failures

**Generalized Stack Trace**:
```
Caused by: java.lang.Exception
	at org.apache.hadoop.hdfs.restart.HdfsClusterAdapter.restartDataNode(HdfsClusterAdapter.java)
```

---

### Group 7: IOException in MiniDFSCluster.waitClusterUp

[ ] Not started

**Priority Reason**: Test infrastructure issue with cluster startup.

**Test Executions**: 23 failures

**Generalized Stack Trace**:
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.MiniDFSCluster.waitClusterUp(MiniDFSCluster.java)
```

---

### Group 17: StandbyException (remote, wrapped)

[ ] Not started

**Priority Reason**: Similar to Group 2, 6 - HA timing issue, likely test expectation mismatch.

**Test Executions**: 8 failures

---

### Group 19: TimeoutException in DFSTestUtil.waitForDatanodeStatus

[ ] Not started

**Priority Reason**: Test utility timeout.

**Test Executions**: 7 failures

---

### Group 24: RejectedExecutionException

[ ] Not started

**Priority Reason**: Thread pool shutdown during test - timing issue.

**Test Executions**: 6 failures

---

### Group 26: ObserverRetryOnActiveException

[ ] Not started

**Priority Reason**: HA observer state issue - expected during transitions.

**Test Executions**: 6 failures

---

### Group 29: TimeoutException in DFSTestUtil.waitForReplication

[ ] Not started

**Priority Reason**: Test utility timeout.

**Test Executions**: 5 failures

---

### Group 34: NoSuchFileException

[ ] Not started

**Priority Reason**: File system state after restart - timing issue.

**Test Executions**: 4 failures

---

### Group 36: ReplicaNotFoundException in getBlockLocalPathInfo

[ ] Not started

**Priority Reason**: Replica state after restart - expected during recovery.

**Test Executions**: 3 failures

---

### Group 37: ComparisonFailure (Caused by)

[ ] Not started

**Priority Reason**: Test assertion failure - wrapped exception.

**Test Executions**: 3 failures

---

### Group 39: AccessControlException in FSDirAttrOp.setOwner

[ ] Not started

**Priority Reason**: Permission issue - likely test expectation.

**Test Executions**: 2 failures

---

### Group 40-95: Various low-count failures

[ ] Not started

**Priority Reason**: Low occurrence count (1-2 failures) - likely transient issues or test flakiness.

**Test Executions**: 1-2 failures each

Groups include:
- NPEs from test code (Groups 43, 47, 50, 53, 56, 57, 59, 60, 62, 63, 64, 66, 68, 70, 72, 75, 78, 83)
- IOExceptions from various locations
- Configuration and metric exceptions
- Network exceptions (BindException, SocketException)

---

## Summary

Total groups analyzed: 95
- Highest priority (likely bugs): 7
- Medium priority (require investigation): 25
- Lower priority (likely false positives): 63
