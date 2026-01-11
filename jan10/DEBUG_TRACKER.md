# HDFS Debug Tracker - Jan10

## Priority Order Rationale

Groups are ordered by likelihood of being actual bugs:
1. **Highest Priority**: NPE/IOOB/IllegalMonitorStateException from non-test, non-restarttest code
2. **High Priority**: Exceptions indicating state inconsistency in HDFS core code
3. **Medium Priority**: IO errors and transient issues
4. **Lowest Priority**: Timeouts and exceptions directly from restarttest framework or test utilities

---

## HIGH PRIORITY - Likely Bugs

### Group 15: IllegalMonitorStateException in ReentrantReadWriteLock

[ ] Not started

**Test Executions**: 7 failures

**Generalized Stack Trace**:
```
java.lang.IllegalMonitorStateException
	at java.util.concurrent.locks.ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java)
```

**Analysis**: IllegalMonitorStateException during lock release indicates a thread is trying to release a lock it doesn't hold. This is a potential concurrency bug after restart.

---

### Group 33: IndexOutOfBoundsException in Collections.EmptyList

[ ] Not started

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
java.lang.IndexOutOfBoundsException
	at java.util.Collections$EmptyList.get(Collections.java)
```

**Analysis**: IOOB on empty list indicates missing data after restart.

---

### Group 37: IndexOutOfBoundsException in ArrayList

[ ] Not started

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
java.lang.IndexOutOfBoundsException
	at java.util.ArrayList.rangeCheck(ArrayList.java)
```

**Analysis**: Another IOOB indicating list access issue after restart.

---

### Group 74: ArrayIndexOutOfBoundsException in MiniDFSCluster

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
Caused by: java.lang.ArrayIndexOutOfBoundsException
	at org.apache.hadoop.hdfs.MiniDFSCluster.setDataNodeStorageCapacities(MiniDFSCluster.java)
```

**Analysis**: AIOOB in MiniDFSCluster - likely test/restart framework issue.

---

### Group 54: NullPointerException in BlockTokenSecretManager

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManager.isTokenExpired(BlockTokenSecretManager.java)
```

**Analysis**: NPE in security token manager - potential uninitialized state after restart.

---

### Group 70: NullPointerException in DFSStripedInputStream

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSStripedInputStream.refreshLocatedBlock(DFSStripedInputStream.java)
```

**Analysis**: NPE in erasure coding input stream - potential block location issue after restart.

---

### Group 73: NullPointerException in DatanodeID

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.protocol.DatanodeID.<init>(DatanodeID.java)
```

**Analysis**: NPE during DatanodeID construction - potential datanode registration issue.

---

### Group 18: NullPointerException in Test (TestDecommissionWithStriped)

[ ] Not started

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.TestDecommissionWithStriped_RestartInjected.assertBlockIndexAndTokenPosition(TestDecommissionWithStriped_RestartInjected.java)
```

---

### Group 29: NullPointerException in Test (DataNodeTestUtils)

[ ] Not started

**Test Executions**: 3 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.DataNodeTestUtils.triggerBlockReport(DataNodeTestUtils.java)
```

---

## HIGH-MEDIUM PRIORITY - State/HA Issues

### Group 2: StandbyException (RemoteException)

[ ] Not started

**Test Executions**: 84 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Analysis**: Operations attempted on standby namenode. This is expected behavior during HA transitions but high count suggests potential state sync issues.

---

### Group 3: StandbyException (Direct)

[ ] Not started

**Test Executions**: 33 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.StandbyException
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

---

### Group 14: StandbyException (Caused by RemoteException)

[ ] Not started

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

---

### Group 21: ObserverRetryOnActiveException

[ ] Not started

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.ObserverRetryOnActiveException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

---

### Group 17: CouldNotCatchUpException in HATestUtil

[ ] Not started

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil$CouldNotCatchUpException
	at org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil.waitForStandbyToCatchUp(HATestUtil.java)
```

**Analysis**: Standby namenode failed to catch up with active - potential sync issue.

---

### Group 72: StandbyException (Another Caused by)

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.StandbyException
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

---

## MEDIUM PRIORITY - Data/IO Issues

### Group 4: AccessControlException

[ ] Not started

**Test Executions**: 31 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException)
	at org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker.checkSuperuserPrivilege(FSPermissionChecker.java)
```

**Analysis**: Superuser privilege check failed - potential permission state issue after restart.

---

### Group 7: SnapshotException (checkNestedSnapshottable)

[ ] Not started

**Test Executions**: 19 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.hdfs.protocol.SnapshotException)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.checkNestedSnapshottable(SnapshotManager.java)
```

---

### Group 8: IOException in DataStreamer.handleBadDatanode

[ ] Not started

**Test Executions**: 19 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.handleBadDatanode(DataStreamer.java)
```

**Analysis**: DataStreamer cannot write to datanode - expected during restart, but high count.

---

### Group 10: IOException in DataStreamer.findNewDatanode

[ ] Not started

**Test Executions**: 11 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.findNewDatanode(DataStreamer.java)
```

---

### Group 11: IOException in StripeReader.checkMissingBlocks

[ ] Not started

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.StripeReader.checkMissingBlocks(StripeReader.java)
```

**Analysis**: Erasure coding missing blocks check failing after restart.

---

### Group 19: CannotObtainBlockLengthException

[ ] Not started

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException
	at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java)
```

**Analysis**: Block length cannot be obtained - potential metadata inconsistency.

---

### Group 69: BlockMissingException

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.BlockMissingException
	at org.apache.hadoop.hdfs.DFSInputStream.refetchLocations(DFSInputStream.java)
```

---

### Group 25: InconsistentFSStateException

[ ] Not started

**Test Executions**: 4 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.hdfs.server.common.InconsistentFSStateException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.checkUpgrade(FSImage.java)
```

**Analysis**: File system state inconsistency detected during upgrade check.

---

### Group 13: IOException in DataNode.getDiskBalancer

[ ] Not started

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getDiskBalancer(DataNode.java)
```

---

### Group 12: EOFException in DataInputStream

[ ] Not started

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
Caused by: java.io.EOFException
	at java.io.DataInputStream.readInt(DataInputStream.java)
```

**Analysis**: Unexpected end of data stream - potential data corruption or incomplete write.

---

### Group 34: InvalidToken (renewToken)

[ ] Not started

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.security.token.SecretManager$InvalidToken
	at org.apache.hadoop.security.token.delegation.AbstractDelegationTokenSecretManager.renewToken(AbstractDelegationTokenSecretManager.java)
```

---

### Group 35: ReplicaNotFoundException

[ ] Not started

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.getMaterializedReplica(FsDatasetImplTestUtils.java)
```

---

## LOWER PRIORITY - Timeouts and Connection Issues

### Group 1: TimeoutException (Generic)

[ ] Not started

**Test Executions**: 97 failures

**Generalized Stack Trace**:
```
java.util.concurrent.TimeoutException
Timed out waiting for condition.
```

**Analysis**: Generic timeout - likely test framework or slow operations.

---

### Group 20: TimeoutException (waitForReplication)

[ ] Not started

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
java.util.concurrent.TimeoutException
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForReplication(DFSTestUtil.java)
```

---

### Group 28: TimeoutException (waitReplication)

[ ] Not started

**Test Executions**: 3 failures

**Generalized Stack Trace**:
```
java.util.concurrent.TimeoutException
	at org.apache.hadoop.hdfs.DFSTestUtil.waitReplication(DFSTestUtil.java)
```

---

### Group 38: TestTimedOutException (socketRead0)

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
org.junit.runners.model.TestTimedOutException
	at java.net.SocketInputStream.socketRead0(Native Method)
```

---

### Group 42: TestTimedOutException (Thread.sleep)

[ ] Not started

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
org.junit.runners.model.TestTimedOutException
	at java.lang.Thread.sleep(Native Method)
```

---

## FALSE POSITIVE - Restart Framework Issues

### Group 5: Exception from HdfsClusterAdapter.restartDataNode

[ ] Not started

**Test Executions**: 29 failures

**Generalized Stack Trace**:
```
Caused by: java.lang.Exception
	at org.apache.hadoop.hdfs.restart.HdfsClusterAdapter.restartDataNode(HdfsClusterAdapter.java)
```

**Analysis**: Restart framework exception - FALSE POSITIVE.

---

### Group 6: IOException at MiniDFSCluster.waitClusterUp

[ ] Not started

**Test Executions**: 19 failures

**Generalized Stack Trace**:
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.MiniDFSCluster.waitClusterUp(MiniDFSCluster.java)
```

**Analysis**: Cluster not coming up after restart - likely framework issue.

---

## Test Assertion Failures

### Group 9: JUnit ComparisonFailure

[ ] Not started

**Test Executions**: 14 failures

**Generalized Stack Trace**:
```
org.junit.ComparisonFailure
	at org.junit.Assert.assertEquals(Assert.java)
```

**Analysis**: Assertion failures in tests - need inspection to determine if bug or expected.

---

## Single-Failure Groups (Remaining - 43 groups)

The following groups have 1-2 failures each:

### NPEs in Various Locations:
- Group 46: TestSaveNamespace_RestartInjected
- Group 50: TestFSImageWithSnapshot_RestartInjected
- Group 51: TestBlockScanner_RestartInjected
- Group 56: TestDFSZKFailoverController_RestartInjected
- Group 66: DFSTestUtil
- Group 67: TestDataNodeReconfiguration_RestartInjected
- Group 78: TestTransferRbw_RestartInjected

### IOException Variants:
- Group 27: FsVolumeImpl.getBlockPoolSlice
- Group 30: FSEditLog.checkForGaps
- Group 32: FsDatasetImplTestUtils.verifyBlockPoolMissing
- Group 39: FSNamesystem.saveNamespace
- Group 40: DFSInputStream.getBlockRange
- Group 43: NativeIO.renameTo
- Group 55: MetaRecoveryContext.editLogLoaderPrompt
- Group 56: FSImage.saveFSImageInAllDirs
- Group 71: FSNamesystem.commitBlockSynchronization
- Group 73: FSImage.recoverStorageDirs
- Group 79: AbstractDelegationTokenSecretManager.addKey

### Network/Connection Issues:
- Group 59: SocketException (HttpClient)
- Group 60: SocketException (SocketInputStream)
- Group 61: BindException
- Group 63: IOException (Client.getConnection)
- Group 77: ConnectException

### Other:
- Group 16: FileNotFoundException (INodeDirectory.valueOf)
- Group 26: NoSuchFileException
- Group 44: FileNotFoundException (HardLink.getLinkCount)
- Group 57: DiskChecker.DiskErrorException
- Group 58: RetriableException
- Group 62: MetricsException
- Group 64: IncorrectVersionException
- Group 75: NoSuchFileException
- Group 80: ClassCastException (TestWriteToReplica)

---

## Summary Statistics

| Priority | Group IDs | Exception Category | Total Failures | Verdict |
|----------|-----------|-------------------|----------------|---------|
| HIGH | 15, 33, 37, 54, 70, 73, 74 | NPE/IOOB/IllegalMonitorState | ~17 | Likely Bugs |
| HIGH | 18, 29, 46, 50, 51, 56, 66, 67, 78 | NPEs in Test Code | ~15 | Need Inspection |
| MEDIUM-HIGH | 2, 3, 14, 17, 21, 72 | HA/Standby Issues | ~137 | State Issues |
| MEDIUM | 4, 7, 8, 10, 11, 12, 13, 19, 25, 34, 35, 69 | Data/IO Issues | ~110 | Need Inspection |
| LOW | 1, 20, 28, 38, 42 | Timeouts | ~107 | Expected |
| FALSE POSITIVE | 5, 6 | Restart Framework | ~48 | Framework Issue |
| ASSERTION | 9 | Test Assertions | 14 | Need Inspection |

**Total Groups**: 80
**Total Failures**: ~550+ (non-assertion)
