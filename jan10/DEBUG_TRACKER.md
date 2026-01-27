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

[x] FP - See [FP-GROUP-15.md](FPs/FP-GROUP-15.md)

**Test Executions**: 7 failures

**Generalized Stack Trace**:
```
java.lang.IllegalMonitorStateException
	at java.util.concurrent.locks.ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java)
```

**Analysis**: FALSE POSITIVE - Restart injected inside a critical section (between writeLock() and writeUnlock()). After restart, the test code reassigns FSNamesystem reference and tries to unlock the new instance which was never locked by the current thread.

---

### Group 33: IndexOutOfBoundsException in Collections.EmptyList

[x] FP - See [FP-GROUP-33.md](FPs/FP-GROUP-33.md)

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
java.lang.IndexOutOfBoundsException
	at java.util.Collections$EmptyList.get(Collections.java)
```

**Analysis**: FALSE POSITIVE - Restart testing framework's reference refresh incorrectly replaces FSDataOutputStream by re-executing `cluster.getFileSystem().create()`. This creates a new stream, resetting the file and causing the empty block list.

---

### Group 37: IndexOutOfBoundsException in ArrayList

[x] FP - See [FP-GROUP-37.md](FPs/FP-GROUP-37.md)

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
java.lang.IndexOutOfBoundsException
	at java.util.ArrayList.rangeCheck(ArrayList.java)
```

**Analysis**: FALSE POSITIVE - Restart injected at `after_safemode_enter` position. Safemode is a transient in-memory state that doesn't persist across NameNode restarts. After restart, the NameNode automatically leaves safemode (0 blocks means threshold immediately met), causing `saveNamespace` to fail with IOException. The test's `assertOutMsg` then fails when trying to access an empty list.

---

### Group 74: ArrayIndexOutOfBoundsException in MiniDFSCluster

[x] BUG - See [BUG-GROUP-74.md](bugs/BUG-GROUP-74.md)

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
Caused by: java.lang.ArrayIndexOutOfBoundsException
	at org.apache.hadoop.hdfs.MiniDFSCluster.setDataNodeStorageCapacities(MiniDFSCluster.java)
```

**Analysis**: BUG - `MiniDFSCluster.setDataNodeStorageCapacities()` does not check if the datanode index is within bounds of the `storageCap` array. When a DataNode is added without specifying storageCapacities, no entry is added to `storageCap`, but `restartDataNode()` still tries to access it using the datanode's index, causing AIOOB.

---

### Group 54: NullPointerException in BlockTokenSecretManager

[x] FP - See [FP-GROUP-54.md](FPs/FP-GROUP-54.md)

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManager.isTokenExpired(BlockTokenSecretManager.java)
```

**Analysis**: FALSE POSITIVE - The NPE occurs in test utility code (`BlockTokenSecretManager.isTokenExpired` is marked "for unit test only"). The restart at `after_hflush` creates an inconsistent state where the DataStreamer's accessToken is null. In production, if there were token issues, the write would fail before reaching this point. The test doesn't guard against null tokens from `getBlockToken()`.

---

### Group 70: NullPointerException in DFSStripedInputStream

[x] FP - See [FP-GROUP-70.md](FPs/FP-GROUP-70.md)

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSStripedInputStream.refreshLocatedBlock(DFSStripedInputStream.java)
```

**Analysis**: FALSE POSITIVE - The restart at `after_striped_file_creation` causes the restarted DataNode to not finish block reporting yet. The test directly calls `refreshLocatedBlock()` with a null block (missing from block locations). The production code handles null/missing blocks properly via `StripeReader.readChunk()` which marks missing blocks and uses erasure coding for reconstruction.

---

### Group 73: NullPointerException in DatanodeID

[x] FP - See [FP-GROUP-73.md](FPs/FP-GROUP-73.md)

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.protocol.DatanodeID.<init>(DatanodeID.java)
```

**Analysis**: FALSE POSITIVE - The restart framework's reference refresh mechanism incorrectly nullifies the `readOnlyDataNode` field after a datanode restart. The expression chain captures the old DatanodeId at assignment time, and when replayed after restart, `datanodeManager.getDatanode(oldId)` returns null because the restarted datanode has a new UUID.

---

### Group 18: NullPointerException in Test (TestDecommissionWithStriped)

[x] FP - See [FP-GROUP-18.md](FPs/FP-GROUP-18.md)

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.TestDecommissionWithStriped_RestartInjected.assertBlockIndexAndTokenPosition(TestDecommissionWithStriped_RestartInjected.java)
```

**Analysis**: FALSE POSITIVE - Restart injected at `before_decommission` position. After NameNode restart, block location information is rebuilt from datanode block reports, causing the datanodes in the block locations to change. The test's cached datanode-to-index/token mappings (built before restart) become stale. When the test tries to look up new datanodes in the old mappings, HashMap.get() returns null, and unboxing null to byte causes NPE.

---

### Group 29: NullPointerException in Test (DataNodeTestUtils)

[x] FP - See [FP-GROUP-29.md](FPs/FP-GROUP-29.md)

**Test Executions**: 3 failures

**Generalized Stack Trace**:
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.DataNodeTestUtils.triggerBlockReport(DataNodeTestUtils.java)
```

**Analysis**: FALSE POSITIVE - The restart framework's reference refresh mechanism incorrectly replaces the `DataNodeProperties` objects stored in local variables `h10` and `h11`. When the test later calls `cluster.restartDataNode(h10)` and `cluster.restartDataNode(h11)`, it uses the replaced (incorrect) `DataNodeProperties` objects, which do not correspond to the original stopped datanodes (host10 and host11). This results in those datanodes not being restarted, and `getDataNode("host11")` returns null.

---

## HIGH-MEDIUM PRIORITY - State/HA Issues

### Group 2: StandbyException (RemoteException)

[x] FP - See [FP-GROUP-2.md](FPs/FP-GROUP-2.md)

**Test Executions**: 84 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Analysis**: FALSE POSITIVE - The restart framework injects restarts on the Active NameNode in HA clusters without automatic failover. When the Active NameNode is restarted, it comes back as Standby (HDFS's default behavior), leaving the cluster without any Active NameNode. WRITE operations then fail with StandbyException because there's no Active NameNode to handle them. This is not a bug in HDFS - it's an improper restart position for manual HA clusters.

---

### Group 3: StandbyException (Direct)

[x] FP - See [FP-GROUP-3.md](FPs/FP-GROUP-3.md)

**Test Executions**: 33 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.StandbyException
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Analysis**: FALSE POSITIVE - Same root cause as Group 2. The restart framework was injecting restarts on the Active NameNode without restoring HA state after restart. The NameNode comes back as Standby by default, leaving no Active NameNode. This has been fixed in `HdfsClusterAdapter.restoreHAState()`. Tests now pass.

---

### Group 14: StandbyException (Caused by RemoteException)

[x] FP - See [FP-GROUP-14.md](FPs/FP-GROUP-14.md)

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Analysis**: FALSE POSITIVE - Same root cause as Groups 2 and 3. The restart framework was injecting restarts on the Active NameNode without restoring HA state after restart. The NameNode comes back as Standby by default, leaving no Active NameNode. This has been fixed in `HdfsClusterAdapter.restoreHAState()`. Tests now pass.

---

### Group 21: ObserverRetryOnActiveException

[x] FP - See [FP-GROUP-21.md](FPs/FP-GROUP-21.md)

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.ObserverRetryOnActiveException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Analysis**: FALSE POSITIVE - Same root cause as Groups 2, 3, and 14. The restart framework was injecting restarts on the Active NameNode without restoring HA state after restart. The NameNode comes back as Standby by default, leaving no Active NameNode. WRITE operations then get routed to Observer nodes which correctly reject them. This has been fixed in `HdfsClusterAdapter.restoreHAState()`. Tests now pass.

---

### Group 17: CouldNotCatchUpException in HATestUtil

[x] FP - See [FP-GROUP-17.md](FPs/FP-GROUP-17.md)

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil$CouldNotCatchUpException
	at org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil.waitForStandbyToCatchUp(HATestUtil.java)
```

**Analysis**: FALSE POSITIVE - The test maintains cached NameNode references in the `nns[]` field. When the restart framework injects a restart after the test's own restart, the `nns[1]` reference becomes stale, pointing to the old (shutdown) NameNode instance. The `waitForStandbyToCatchUp` method then queries the shutdown NameNode's FSImage which returns txid 0, causing the timeout.

---

### Group 72: IOException in FSImage.recoverStorageDirs

[x] FP - See [FP-GROUP-72.md](FPs/FP-GROUP-72.md)

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.recoverStorageDirs(FSImage.java)
```

**Analysis**: FALSE POSITIVE - The restart framework preserves the original `StartupOption.IMPORT` when restarting the NameNode. However, IMPORT is a one-time operation that converts empty name directories into formatted directories. After a successful import, subsequent NameNode restarts should use `StartupOption.REGULAR`, not IMPORT. HDFS correctly rejects the re-import attempt with "Cannot import image from a checkpoint. NameNode already contains an image."

---

## MEDIUM PRIORITY - Data/IO Issues

### Group 4: AccessControlException

[x] FP - See [FP-GROUP-4.md](FPs/FP-GROUP-4.md)

**Test Executions**: 31 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException)
	at org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker.checkSuperuserPrivilege(FSPermissionChecker.java)
```

**Analysis**: FALSE POSITIVE - The restart framework inherits the test's unprivileged user context when performing system-level operations (`datanodeReport`) that require superuser privilege. The test deliberately sets `UserGroupInformation.setLoginUser(unprivilegedUser)` to test access control features. When the restart framework calls `MiniDFSCluster.waitActive()` after restarting the DataNode, it creates a `DFSClient` with the current (unprivileged) user, which then fails the superuser check for `datanodeReport()`. This is not an HDFS bug - the framework should save/restore the privileged user context for system operations.

---

### Group 7: SnapshotException (checkNestedSnapshottable)

[x] FP - See [FP-GROUP-7.md](FPs/FP-GROUP-7.md)

**Test Executions**: 19 failures

**Generalized Stack Trace**:
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.hdfs.protocol.SnapshotException)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.checkNestedSnapshottable(SnapshotManager.java)
```

**Analysis**: FALSE POSITIVE - The test enables nested snapshots via `setAllowNestedSnapshots(true)`, an in-memory only setting that doesn't persist across NameNode restarts. After restart, the NameNode reverts to the default behavior (`allowNestedSnapshots=false`), causing the SnapshotException when the test tries to create nested snapshottable directories. This is not an HDFS bug - the test relies on special in-memory configuration that cannot survive restarts.

---

### Group 8: IOException in DataStreamer.handleBadDatanode

[x] FP - See [FP-GROUP-8.md](FPs/FP-GROUP-8.md)

**Test Executions**: 19 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.handleBadDatanode(DataStreamer.java)
```

**Analysis**: FALSE POSITIVE - The restart framework does not use HDFS's built-in DataNode restart notification mechanism (OOB messages). HDFS sends OOB (Out-of-Band) messages to clients only when `shutdownForUpgrade=true`, which tells clients to wait for the DataNode to restart. The framework uses `MiniDFSCluster.stopDataNode()` which calls `shutdown()` directly without setting this flag. Without OOB, clients treat the DataNode as permanently failed. Combined with single-DataNode test configurations, recovery is impossible.

---

### Group 10: IOException in DataStreamer.findNewDatanode

[x] FP - See [FP-GROUP-10.md](FPs/FP-GROUP-10.md)

**Test Executions**: 11 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.findNewDatanode(DataStreamer.java)
```

**Analysis**: FALSE POSITIVE - The restart injection creates an excessive DataNode failure scenario. The tests (e.g., `testWriteOverCrashFailoverWithDnFail`) explicitly stop 2 DataNodes as part of their test scenario. When the restart framework injects an additional restart of a 3rd DataNode, HDFS correctly fails with "no more good datanodes being available to try" because there aren't enough available nodes (only 2 of 5) to maintain the write pipeline with replication factor 3.

---

### Group 11: IOException in StripeReader.checkMissingBlocks

[x] FP - See [FP-GROUP-11.md](FPs/FP-GROUP-11.md)

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.StripeReader.checkMissingBlocks(StripeReader.java)
```

**Analysis**: FALSE POSITIVE - The restart framework injects DataNode restart during erasure coding tests that use `SimulatedFSDataset`. SimulatedFSDataset is an in-memory test utility that doesn't persist data across DataNode restarts. When the DataNode is restarted, all simulated blocks are lost, causing stripe reads to fail with "missing blocks" errors. In production with real disk storage, data persists across DataNode restarts.

---

### Group 19: CannotObtainBlockLengthException

[x] FP - See [FP-GROUP-19.md](FPs/FP-GROUP-19.md)

**Test Executions**: 5 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException
	at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java)
```

**Analysis**: FALSE POSITIVE - The restart is injected during an active write operation at `after_file_creation_hsync` position. After DataNode restart, RBW (Replica Being Written) blocks are loaded as RWR (Replica Waiting to be Recovered) state. By design, RWR replicas return `-1` for `getVisibleLength()` and do not provision reads until lease recovery completes. The test immediately tries to read the file without waiting for lease recovery, which is not a valid production scenario.

---

### Group 69: BlockMissingException

[x] FP - See [FP-GROUP-69.md](FPs/FP-GROUP-69.md)

**Test Executions**: 1 failure

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.BlockMissingException
	at org.apache.hadoop.hdfs.DFSInputStream.refetchLocations(DFSInputStream.java)
```

**Analysis**: FALSE POSITIVE - The restart framework injects an additional NameNode restart at `after_namenode_restart` position, which disrupts the test's carefully orchestrated restart sequence for HDFS-4799. After the additional restart, only DataNodes with stale generation stamp (1001) replicas are available, while the block's recorded generation stamp is 1014. HDFS correctly refuses to serve data from stale replicas, causing the BlockMissingException. This is expected data integrity protection, not a bug.

---

### Group 25: NoSuchFileException (Edit Log File Not Found)

[x] FP - See [FP-GROUP-25.md](FPs/FP-GROUP-25.md)

**Test Executions**: 4 failures

**Generalized Stack Trace**:
```
Caused by: java.nio.file.NoSuchFileException
	at sun.nio.fs.UnixException.translateToIOException(UnixException.java)
```

**Analysis**: FALSE POSITIVE - The test's verification logic hardcodes the assumption that edit log files start from transaction ID 1 (`NNStorage.getFinalizedEditsFile(sd, 1, 1 + expectedTransactions - 1)`). After a NameNode restart, edit logs are segmented - the pre-restart transactions are in one segment (e.g., txn 1-4) and post-restart transactions are in a new segment (e.g., txn 5-30008). The expected file `edits_0000000000000000001-0000000000000030006` never exists because transactions are split across multiple segments. This is correct HDFS behavior - the test simply doesn't account for edit log segmentation during restarts.

---

### Group 13: IOException in DataNode.getDiskBalancer

[x] FP - See [FP-GROUP-13.md](FPs/FP-GROUP-13.md)

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getDiskBalancer(DataNode.java)
```

**Analysis**: FALSE POSITIVE - The restart framework's reference refresh mechanism doesn't update DataNode references stored in inner class fields (like `RpcTestHelper.dataNode`). After a DataNode restart, the test uses a stale reference to the old (shut-down) DataNode instance, which has `diskBalancer = null`. In production, clients use RPC to communicate with DataNodes, not direct Java object references.

---

### Group 12: EOFException in DataInputStream

[x] FP - See [FP-GROUP-12.md](FPs/FP-GROUP-12.md)

**Test Executions**: 8 failures

**Generalized Stack Trace**:
```
Caused by: java.io.EOFException
	at java.io.DataInputStream.readInt(DataInputStream.java)
```

**Analysis**: FALSE POSITIVE - The restart framework injects a restart while an RPC call is actively paused by test mock infrastructure (`DelayAnswer`). The test's spy/mock setup captures a stale reference to the original NameNode proxy. When the NameNode restarts, the old proxy's connection is dead, and the mock bypasses HDFS's normal retry/reconnection logic. In production, `RetryInvocationHandler` would establish a new connection and the checkpoint would succeed.

---

### Group 34: InvalidToken (renewToken)

[x] FP - See [FP-GROUP-34.md](FPs/FP-GROUP-34.md)

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.security.token.SecretManager$InvalidToken
	at org.apache.hadoop.security.token.delegation.AbstractDelegationTokenSecretManager.renewToken(AbstractDelegationTokenSecretManager.java)
```

**Analysis**: FALSE POSITIVE - The test uses a test-only utility method `generateDelegationToken()` that creates tokens directly on the secret manager, bypassing the normal RPC flow through `FSNamesystem.getDelegationToken()`. In production, clients obtain delegation tokens via RPC which properly logs them to the edit log (`getEditLog().logGetDelegationToken()`). After restart, tokens are restored from the edit log. The test's tokens are never logged, so they cannot survive the restart. This is not a bug in HDFS - it's expected behavior when using test-only APIs that skip persistence.

---

### Group 35: ReplicaNotFoundException

[x] TEST-BUG - See [TEST-BUG-GROUP-35.md](bugs/TEST-BUG-GROUP-35.md)

**Test Executions**: 2 failures

**Generalized Stack Trace**:
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.getMaterializedReplica(FsDatasetImplTestUtils.java)
```

**Analysis**: TEST-BUG - Bug in `MiniDFSCluster.java` test utility. After a datanode restart, `restartDataNode()` appends the restarted datanode to the END of the list instead of maintaining its original position. This causes `readBlockOnDataNode()` (which uses index-based storage paths) and `getMaterializedReplica()` (which uses list-based DataNode lookup) to return inconsistent results. When `firstDnWithBlock()` finds a block using storage paths and then `corruptReplica()` queries the DataNode at that index, it may query the wrong DataNode (one that doesn't have the block), causing ReplicaNotFoundException.

---

## LOWER PRIORITY - Timeouts and Connection Issues

### Group 1: TimeoutException (Generic)

[x] FP - See [FP-GROUP-1.md](FPs/FP-GROUP-1.md)

**Test Executions**: 97 failures

**Generalized Stack Trace**:
```
java.util.concurrent.TimeoutException
Timed out waiting for condition.
```

**Analysis**: FALSE POSITIVE - The restart framework injects NameNode restarts that break in-memory BackupNode-NameNode edit streaming connections. The BackupNode only registers once during initialization, and when the NameNode restarts, the registration is lost (stored in in-memory `journalSet`). Without re-registration, new edits are not sent to the BackupNode, causing tests to timeout waiting for namespace synchronization. This is expected behavior - BackupNode architecture assumes stable NameNode connection.

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
