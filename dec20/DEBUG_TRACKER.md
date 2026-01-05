# HDFS Test Failure Debug Tracker

This document tracks failure groups from HDFS restart testing, organized by priority.

## Priority Legend
- **HIGHEST PRIORITY**: NullPointerException or IndexOutOfBoundsException from non-test production code
- **HIGH PRIORITY**: Other exceptions from production code (not timeouts, not from restarttest modules)
- **LOWER PRIORITY**: Timeouts, failures from restarttest modules, test framework assertion failures

---

## HIGHEST PRIORITY - NPE/IndexOutOfBounds from Production Code

**Total Groups: 10**

### [TEST-BUG] Group 4

**Test Executions:** 39

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java:146)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.getStoredBlock(BlockManager.java:4625)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getStoredBlock(FSNamesystem.java:3820)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkUCBlock(FSNamesystem.java:5748)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.bumpBlockGenerationStamp(FSNamesystem.java:5822)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.updateBlockForPipeline(NameNodeRpcServer.java:985)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestBlockManager_RestartInjected.testNeededReconstructionWhileAppending(TestBlockManager_RestartInjected.java:514)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(Dele
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlockManager_RestartInjected.testNeededReconstructionWhileAppending**
   - Position: after_append, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.blockmanagement.TestOverReplicatedBlocks_RestartInjected.testInvalidateOverReplicatedBlock**
   - Position: after_set_replication, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestProcessCorruptBlocks_RestartInjected.testWhenDecreasingReplication**
   - Position: after_cluster_setup, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Tests store references to NameNode components (via `cluster.getNameNodeRpc()`, etc.) before namenode restart. After restart, `BlocksMap.close()` sets `blocks = null`. The test uses stale references to the old namenode, causing NPE when accessing the closed BlocksMap.
- **Classification:** TEST-BUG - The restart-injected tests need to re-fetch namenode references after restart. The original tests weren't designed for restart scenarios.
- **Fix:** After any namenode restart, re-fetch all direct references: `namenode = cluster.getNameNodeRpc()`, `bm = cluster.getNamesystem().getBlockManager()`, etc.
- **Bug Report:** See [HDFS-BUG-GROUP-4.md](bugs/HDFS-BUG-GROUP-4.md)

---

### [TEST-BUG] Group 19

**Test Executions:** 8

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.numNodes(BlocksMap.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.numNodes(BlocksMap.java:172)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlock(BlockManager.java:1420)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlock(BlockManager.java:1382)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlockList(BlockManager.java:1353)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlocks(BlockManager.java:1503)
	at org.apache.hadoop.hdfs.server.namenode.FSDirStatAndListingOp.getBlockLocations(FSDirStatAndListingOp.java:179)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getBlockLocations(FSNamesystem.java:2124)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.getBlockLocations(NameNodeRpcServer.java:769)
	at org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcatNotCompleteBlock(TestHDFSConcat_RestartInjected
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcatNotCompleteBlock**
   - Position: before_concat, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcatNotCompleteBlock**
   - Position: after_concat, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcat**
   - Position: after_source_files_creation, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `nn = cluster.getNameNodeRpc()` in `@Before` setup. After namenode restart, `BlocksMap.close()` sets `blocks = null`. The test uses stale `nn` reference to call `getBlockLocations()`, which accesses the closed BlocksMap, causing NPE at `blocks.get(b)` in `numNodes()`.
- **Classification:** TEST-BUG - Same pattern as Group 4. The restart-injected tests need to re-fetch `nn` reference after namenode restart.
- **Fix:** After any namenode restart, re-fetch: `nn = cluster.getNameNodeRpc()`.
- **Bug Report:** See [HDFS-BUG-GROUP-19.md](bugs/HDFS-BUG-GROUP-19.md)

---

### [TEST-BUG] Group 26

**Test Executions:** 6

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.FSImageFormatPBSnapshot$Loader.loadSnapshotSection(FSImageFormatPBSnapshot.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.FSImageFormatPBSnapshot$Loader.loadSnapshotSection(FSImageFormatPBSnapshot.java:154)
	at org.apache.hadoop.hdfs.server.namenode.FSImageFormatProtobuf$Loader.loadInternal(FSImageFormatProtobuf.java:464)
	at org.apache.hadoop.hdfs.server.namenode.FSImageFormatProtobuf$Loader.load(FSImageFormatProtobuf.java:247)
	at org.apache.hadoop.hdfs.server.namenode.FSImageFormat$LoaderDelegator.load(FSImageFormat.java:228)
	at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.loadFSImageFromTempFile(TestFSImageWithSnapshot_RestartInjected.java:166)
	at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.checkImage(TestFSImageWithSnapshot_RestartInjected.java:424)
	at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage(TestFSImageWithSnapshot_RestartInjected.java:282)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Nat
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage**
   - Position: after_snapshot_s1, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage**
   - Position: after_mkdir_sub11, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage**
   - Position: after_third_rename, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `fsn = cluster.getNamesystem()` in `@Before` setup. After namenode restart, `cluster.getNamesystem()` returns the NEW FSNamesystem, but the test's `fsn` field still points to the OLD (closed) FSNamesystem. When `saveFSImageToTempFile()` and `dumpTree2File()` use the stale `fsn` reference, they operate on a closed FSNamesystem, producing inconsistent FSImage data. When this corrupted FSImage is loaded into the fresh cluster, `loadSnapshotSection()` fails to find INodes by ID, causing the NPE at `fsDir.getInode(sdirId).asDirectory()`.
- **Classification:** TEST-BUG - Same pattern as Group 4 and Group 19. The restart-injected tests need to re-fetch FSNamesystem reference after namenode restart.
- **Fix:** After any namenode restart, re-fetch: `fsn = cluster.getNamesystem()`.
- **Bug Report:** See [HDFS-BUG-GROUP-26.md](bugs/HDFS-BUG-GROUP-26.md)

---

### [TEST-BUG] Group 44

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.removeNode(BlocksMap.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.removeNode(BlocksMap.java:182)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.removeStoredBlock(BlockManager.java:4098)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.removeBlocksAssociatedTo(BlockManager.java:1688)
	at org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager.removeDatanode(DatanodeManager.java:838)
	at org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager.removeDeadDatanode(DatanodeManager.java:883)
	at org.apache.hadoop.hdfs.server.blockmanagement.HeartbeatManager.heartbeatCheck(HeartbeatManager.java:498)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerTestUtil.checkHeartbeat(BlockManagerTestUtil.java:292)
	at org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionStatusAfterDNRestart(TestDecommissioningStatus_RestartInjected.java:493)
	at sun.reflect.NativeMethodAccessorImpl.in
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionStatusAfterDNRestart**
   - Position: after_datanode_stopped, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyIsHot_RestartInjected.testDatanodeRestarts**
   - Position: after_file_creation_and_standby_catchup, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `FSNamesystem fsn = cluster.getNamesystem()` and `DatanodeManager dm = fsn.getBlockManager().getDatanodeManager()` at line 458-459 before namenode restart. After restart at lines 485-490, `BlocksMap.close()` sets `blocks = null`. The test uses stale `fsn` reference at line 493: `BlockManagerTestUtil.checkHeartbeat(fsn.getBlockManager())`, which accesses the closed BlocksMap, causing NPE at `blocks.get(b)` in `removeNode()`.
- **Classification:** TEST-BUG - Same pattern as Groups 4 and 19. The restart-injected tests need to re-fetch FSNamesystem and related references after namenode restart.
- **Fix:** After any namenode restart, re-fetch all direct references: `fsn = cluster.getNamesystem()`, `dm = fsn.getBlockManager().getDatanodeManager()`, etc.
- **Bug Report:** See [HDFS-BUG-GROUP-44.md](bugs/HDFS-BUG-GROUP-44.md)

---

### [TEST-BUG] Group 48

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.NNStorage.getStorageFile(NNStorage.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.NNStorage.getStorageFile(NNStorage.java:747)
	at org.apache.hadoop.hdfs.server.namenode.NNStorage.readTransactionIdFile(NNStorage.java:451)
	at org.apache.hadoop.hdfs.server.namenode.FSImageTestUtil.getStorageTxId(FSImageTestUtil.java:624)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestBootstrapStandby_RestartInjected.testDownloadingLaterCheckpoint(TestBootstrapStandby_RestartInjected.java:185)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosive
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestBootstrapStandby_RestartInjected.testDownloadingLaterCheckpoint**
   - Position: after_create_test_file, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestBootstrapStandby_RestartInjected.testDownloadingLaterCheckpoint**
   - Position: after_bootstrap_nns, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `nn0 = cluster.getNameNode(0)` in `@Before` setup. After namenode restart at `after_create_test_file`, `cluster.getNameNode(0)` returns a NEW NameNode instance, but `nn0` still points to the OLD (closed) NameNode. When `FSImageTestUtil.getStorageTxId(nn0, editsUri)` is called with the stale `nn0`, `getStorageDirectory(storageUri)` returns `null` for the old NameNode. `NNStorage.readTransactionIdFile(null)` then calls `getStorageFile(null, ...)`, causing NPE at `sd.getCurrentDir()`.
- **Classification:** TEST-BUG - Same pattern as Groups 4, 19, 26, and 44. The restart-injected tests need to re-fetch `nn0` reference after namenode restart.
- **Fix:** After any namenode restart, re-fetch: `nn0 = cluster.getNameNode(0)`.
- **Production Code Improvement:** Added defensive null checks in `NNStorage.java` to throw meaningful error messages instead of NPE. See [HDFS-XXXXX-improve-nnstorage-error-messages.patch](patches/HDFS-XXXXX-improve-nnstorage-error-messages.patch).
- **Bug Report:** See [HDFS-BUG-GROUP-48.md](bugs/HDFS-BUG-GROUP-48.md)

---

### [TEST-BUG] Group 53

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.chooseExcessRedundancies(BlockManager.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.chooseExcessRedundancies(BlockManager.java:3940)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.processExtraRedundancyBlock(BlockManager.java:3924)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.setReplication(BlockManager.java:3884)
	at org.apache.hadoop.hdfs.server.namenode.FSDirAttrOp.unprotectedSetReplication(FSDirAttrOp.java:408)
	at org.apache.hadoop.hdfs.server.namenode.FSDirAttrOp.setReplication(FSDirAttrOp.java:144)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.setReplication(FSNamesystem.java:2377)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.setReplication(NameNodeRpcServer.java:858)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestDNFencing_RestartInjected.testDnFencing(TestDNFencing_RestartInjected.java:152)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(Nati
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestDNFencing_RestartInjected.testDnFencing**
   - Position: after_create_file, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestDNFencing_RestartInjected.testNNClearsCommandsOnFailoverAfterStartup**
   - Position: after_create_file_failover, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `nn1 = cluster.getNameNode(0)` in `@Before` setup. After namenode restart at `after_create_file`, the test uses stale `nn1` reference at line 152: `nn1.getRpcServer().setReplication()`. This is a direct method call (not RPC) on the OLD NameNode's components. The OLD FSNamesystem's `getBlockCollection(storedBlock)` returns `null` because the FSDirectory is closed/empty, causing NPE at `bc.getStoragePolicyID()` in `chooseExcessRedundancies()`.
- **Classification:** TEST-BUG - Same pattern as Groups 4, 19, 26, 44, and 48. The restart-injected tests need to re-fetch namenode references after restart.
- **Fix:** After any namenode restart, re-fetch: `nn1 = cluster.getNameNode(0)`.
- **Production Code Improvement:** Added defensive null checks in `BlockManager.chooseExcessRedundancies()` and `chooseExcessRedundancyStriped()` to throw meaningful error messages instead of NPE. See [HDFS-XXXXX-improve-blockmanager-excess-redundancy-error-messages.patch](patches/HDFS-XXXXX-improve-blockmanager-excess-redundancy-error-messages.patch).
- **Bug Report:** See [HDFS-BUG-GROUP-53.md](bugs/HDFS-BUG-GROUP-53.md)

---

### [TEST-BUG] Group 58

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSTestUtil.addBlockToFile(DFSTestUtil.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSTestUtil.addBlockToFile(DFSTestUtil.java:2231)
	at org.apache.hadoop.hdfs.server.namenode.TestCommitBlockWithInvalidGenStamp_RestartInjected.testCommitWithInvalidGenStamp(TestCommitBlockWithInvalidGenStamp_RestartInjected.java:89)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefo
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestCommitBlockWithInvalidGenStamp_RestartInjected.testCommitWithInvalidGenStamp**
   - Position: after_file_create, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestCommitBlockWithInvalidGenStamp_RestartInjected.testCommitWithInvalidGenStamp**
   - Position: after_inode_get, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `dir = cluster.getNamesystem().getFSDirectory()` in `@Before` setup. After namenode restart at `after_file_create`, `cluster.getNamesystem().getFSDirectory()` returns the NEW FSDirectory, but the test's `dir` field still points to the OLD (closed) FSDirectory. When `fileNode = dir.getINode4Write(file.toString()).asFile()` is called with the stale `dir`, the returned `fileNode` has an empty blocks array. When `DFSTestUtil.addBlockToFile()` is called, `fileNode.getLastBlock()` returns `null`, causing NPE at `lastBlock.getBlockId()`.
- **Classification:** TEST-BUG - Same pattern as Groups 4, 19, 26, 44, 48, and 53. The restart-injected tests need to re-fetch FSDirectory and INode references after namenode restart.
- **Fix:** After any namenode restart, re-fetch: `dir = cluster.getNamesystem().getFSDirectory()`.
- **Test Utility Code Improvement:** Added defensive null check in `DFSTestUtil.addBlockToFile()` to throw meaningful error messages instead of NPE. See [HDFS-XXXXX-improve-dfstestutil-addblocktofile-error-messages.patch](patches/HDFS-XXXXX-improve-dfstestutil-addblocktofile-error-messages.patch).
- **Bug Report:** See [HDFS-BUG-GROUP-58.md](bugs/HDFS-BUG-GROUP-58.md)

---

### [TEST-BUG] Group 90

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSTestUtil$5.get(DFSTestUtil.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSTestUtil$5.get(DFSTestUtil.java:2010)
	at org.apache.hadoop.hdfs.DFSTestUtil$5.get(DFSTestUtil.java:2004)
	at org.apache.hadoop.test.GenericTestUtils.waitFor(GenericTestUtils.java:449)
	at org.apache.hadoop.test.GenericTestUtils.waitFor(GenericTestUtils.java:421)
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForDatanodeState(DFSTestUtil.java:2004)
	at org.apache.hadoop.hdfs.server.namenode.TestDeadDatanode_RestartInjected.testDeadNodeAsBlockTarget(TestDeadDatanode_RestartInjected.java:237)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(Reflectiv
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestDeadDatanode_RestartInjected.testDeadNodeAsBlockTarget**
   - Position: after_datanode_shutdown_test2, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `reg = InternalDataNodeTestUtils.getDNRegistrationForBP(...)` at line 212-213 before namenode restart. After datanode shutdown (line 229) and namenode restart at `after_datanode_shutdown_test2`, the new NameNode's DatanodeManager has an empty `datanodeMap`. The datanode was shut down before restart, so it won't re-register. When `DFSTestUtil.waitForDatanodeState(cluster, reg.getDatanodeUuid(), false, 20000)` is called at line 237, `BlockManagerTestUtil.getDatanode(namesystem, nodeID)` returns `null` because the UUID doesn't exist in the new NameNode. `dd.isAlive()` is then called on `null`, causing NPE.
- **Classification:** TEST-BUG - Same pattern as Groups 4, 19, 26, etc. The test uses stale datanode registration info (UUID) after NameNode restart. Additionally, `DFSTestUtil.waitForDatanodeState` doesn't handle the case where the datanode doesn't exist.
- **Fix:** Add null check in `DFSTestUtil.waitForDatanodeState()`: if `dd == null` and waiting for dead state (`!alive`), return `true` (non-existent = effectively dead).
- **Bug Report:** See [HDFS-BUG-GROUP-90.md](bugs/HDFS-BUG-GROUP-90.md)

---

### [TEST-BUG] Group 93

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSStripedInputStream.refreshLocatedBlock(DFSStripedInputStream.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.DFSStripedInputStream.refreshLocatedBlock(DFSStripedInputStream.java:456)
	at org.apache.hadoop.hdfs.TestDFSStripedInputStream_RestartInjected.testRefreshBlock(TestDFSStripedInputStream_RestartInjected.java:166)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.j
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.TestDFSStripedInputStream_RestartInjected.testRefreshBlock**
   - Position: after_striped_file_creation, Target: datanode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test calls `refreshLocatedBlock(blks[j])` without checking if `blks[j]` is null. After datanode restart at `after_striped_file_creation`, the restarted datanode (index 0) hasn't re-registered its blocks with the NameNode yet. When `StripedBlockUtil.parseStripedBlockGroup()` is called, it returns an array with null entries for blocks whose datanodes haven't registered. The test then iterates through all `dataBlocks` indices and calls `refreshLocatedBlock(blks[0])` on the null entry, causing NPE at `block.getBlock().getLocalBlock()` in line 456.
- **Evidence:** Debug logging showed `LocatedStripedBlock indices: [1, 2, 3, 4, 5, 6, 7, 8]` (index 0 missing) and `blks[0] = null` for all block groups.
- **Classification:** TEST-BUG - The test code doesn't account for the fact that after datanode restart, some block locations may be temporarily unavailable. The production code `refreshLocatedBlock()` correctly expects a non-null block parameter.
- **Fix:** The test should either: (1) Wait for the restarted datanode to fully re-register its blocks before getting block locations, or (2) Skip null blocks when testing `refreshLocatedBlock()`.
- **Bug Report:** See [HDFS-BUG-GROUP-93.md](bugs/HDFS-BUG-GROUP-93.md)

---

### [TEST-BUG] Group 95

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.finalizeReplica(FsDatasetImpl.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.finalizeReplica(FsDatasetImpl.java:1809)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.finalizeNewReplica(FsDatasetImpl.java:1120)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetImpl_RestartInjected.testTransferAndNativeCopyMetrics(TestFsDatasetImpl_RestartInjected.java:1498)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.int
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetImpl_RestartInjected.testTransferAndNativeCopyMetrics**
   - Position: before_finalize_replica, Target: datanode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** Test stores `dataNode = cluster.getDataNodes().get(0)` and `fsDataSetImpl = (FsDatasetImpl) dataNode.getFSDataset()` at lines 1472 and 1488 before the datanode restart. After restart at lines 1491-1496, the MiniDFSCluster creates a NEW DataNode instance with a NEW FsDatasetImpl. The test uses the stale `fsDataSetImpl` reference at line 1498: `fsDataSetImpl.finalizeNewReplica(newReplicaInfo, block)`. The OLD FsDatasetImpl's `volumeMap` no longer contains the block, so `volumeMap.get(bpid, replicaInfo.getBlockId())` returns null, causing NPE when calling `getGenerationStamp()`.
- **Classification:** TEST-BUG - Same pattern as Groups 4, 19, 44. The restart-injected tests need to re-fetch DataNode and FsDatasetImpl references after datanode restart. Additionally, `newReplicaInfo` was created using the old FsDatasetImpl's volumes and cannot be directly used with the new FsDatasetImpl.
- **Fix:** After any datanode restart, re-fetch all direct references: `dataNode = cluster.getDataNodes().get(0)`, `fsDataSetImpl = (FsDatasetImpl) dataNode.getFSDataset()`. The test may also need to be restructured since the replica was created on the old FsDatasetImpl.
- **Production Code Improvement:** Added null check in `FsDatasetImpl.finalizeReplica()` to throw descriptive IOException instead of NPE. See patch: `patches/HDFS-XXXXX-improve-fsdatasetimpl-finalizereplica-error-handling.patch`
- **Bug Report:** See [HDFS-BUG-GROUP-95.md](bugs/HDFS-BUG-GROUP-95.md)

---

## HIGH PRIORITY - Production Code Exceptions

**Total Groups: 73**

### [FP] Group 2

**Test Executions:** 91

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException): Operation category WRITE is not supported in state standby. Visit https://s.apache.org/sbnn-error
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java:108)
	at org.apache.hadoop.hdfs.server.namenode.NameNode$NameNodeHAContext.checkOperation(NameNode.java:2101)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkOperation(FSNamesystem.java:1585)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.delete(NameNodeRpcServer.java:1127)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.delete(ClientNamenodeProtocolServerSideTranslatorPB.java:727)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingMethod(ClientNamenodeProtocolProtos.java)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:621)
	at org.apache.hadoop.ipc.Pro
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestObserverNode_RestartInjected.testGetGroups**
   - Position: before_get_groups, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.TestRollingUpgrade_RestartInjected.testQueryWithMultipleNN**
   - Position: after_other_namenodes_shutdown, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.TestRollingUpgrade_RestartInjected.testQueryWithMultipleNN**
   - Position: after_other_namenodes_restart, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** The restart injection restarts namenode 0 (the active) in an HA cluster with manual failover. After restart, NN[0] comes back as **standby** (not active), leaving the cluster with no active namenode (NN[0]=standby, NN[1]=standby, NN[2]=observer). The test's `@After cleanUp()` method calls `dfs.delete()`, which is a write operation that requires an active namenode. Since there is no active namenode, the client gets `StandbyException`.
- **Evidence:** Debug logging confirmed the HA state change:
  - Before restart: NN[0]=active, NN[1]=standby, NN[2]=observer
  - After restart: NN[0]=standby, NN[1]=standby, NN[2]=observer
- **Classification:** FP (False Positive) - This is expected HA behavior, not a bug in HDFS source code. When a namenode is gracefully restarted in an HA cluster with manual failover, it comes back as standby and requires manual promotion to active. The restart adapter does not restore HA state after restart, which is an improper restart position for HA tests that depend on a specific topology.
- **Why this is NOT a bug:** The HDFS HA system is working correctly. The issue is that the restart injection fundamentally breaks the test's assumption that NN[0] will remain active throughout the test lifecycle.

---

### [FP] Group 3

**Test Executions:** 65

**Generalized Stacktrace:**
```
Caused by: java.io.EOFException
	at java.io.DataInputStream.readInt(DataInputStream.java)
```

**Raw Stacktrace Sample:**
```
java.io.EOFException: End of File Exception between local host is: "df03dece6374/172.17.0.2"; destination host is: "localhost":44585; : java.io.EOFException; For more details see:  http://wiki.apache.org/hadoop/EOFException
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
	at org.apache.hadoop.net.NetUtils.wrapWithMessage(NetUtils.java:930)
	at org.apache.hadoop.net.NetUtils.wrapException(NetUtils.java:879)
	at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java:1588)
	at org.apache.hadoop.ipc.Client.call(Client.java:1530)
	at org.apache.hadoop.ipc.Client.call(Client.java:1427)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:258)
	at org.apache.hadoop.ipc.P
```

**Sample Test Executions:**

1. **org.apache.hadoop.fs.TestGlobPaths_RestartInjected.testGlobAccessDeniedOnFC**
   - Position: before_test_glob_access_denied_on_fc, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestCacheDirectives_RestartInjected.testCacheManagerRestart**
   - Position: after_first_checkpoint, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.fs.TestGlobPaths_RestartInjected.testGlobRootOnFS**
   - Position: before_test_glob_root_on_fs, Target: namenode, Mode: GRACEFUL

**Debug Analysis:**
- **Root Cause:** The test uses statically initialized FileContext objects in `@BeforeClass`. After NameNode restart, the FileContext's DFSClient holds a stale IPC connection. When the client tries to read the RPC response, it receives EOF because the server closed the connection during restart.
- **Key Investigation Findings:**
  1. Manual retry after 1-second delay **succeeds** - proving the client CAN reconnect
  2. The client's retry policy (`dfs.client.retry.policy.enabled`) is **disabled by default**
  3. `ipc.client.connect.max.retries=10` only applies to **new connection establishment**, not to reading responses on existing connections
  4. EOFException during response read is not automatically retried when the retry policy is disabled
- **Why this fails:** When NameNode restarts:
  1. Existing IPC connections become stale (server-side sockets closed)
  2. Client sends RPC on stale connection
  3. Client gets EOF when reading response (connection severed)
  4. With retry policy disabled (default), the call fails immediately instead of retrying
- **Production Impact:** This failure mode **CAN occur in production** if:
  1. Application uses FileContext API with default configuration (retry disabled)
  2. Holds long-running client objects
  3. NameNode restarts (maintenance, failover, crash recovery)
  - Applications would experience EOFException until they manually retry or recreate connections
- **Classification:** FP (False Positive for bug hunting) - This is **expected behavior** per HDFS design:
  1. Retry policy is opt-in, disabled by default (`dfs.client.retry.policy.enabled=false`)
  2. Production deployments typically use HA configurations with automatic failover
  3. Users who need retry resilience should enable `dfs.client.retry.policy.enabled=true`
  4. The retry policy spec `"10000,6,60000,10"` would retry with exponential backoff if enabled
- **Recommendation for users:** Enable client retry policy in hdfs-site.xml:
  ```xml
  <property>
    <name>dfs.client.retry.policy.enabled</name>
    <value>true</value>
  </property>
  ```

---

### [ ] Group 8

**Test Executions:** 28

**Generalized Stacktrace:**
```
java.lang.IllegalStateException
	at org.apache.hadoop.thirdparty.com.google.common.base.Preconditions.checkState(Preconditions.java)
```

**Raw Stacktrace Sample:**
```
java.lang.IllegalStateException: Bad state: CLOSED
	at org.apache.hadoop.thirdparty.com.google.common.base.Preconditions.checkState(Preconditions.java:591)
	at org.apache.hadoop.hdfs.server.namenode.FSEditLog.getCurSegmentTxId(FSEditLog.java:578)
	at org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits(TestBackupNode_RestartInjected.java:263)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evalu
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits**
   - Position: after_edit_log_roll, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits**
   - Position: after_third_sync, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits**
   - Position: after_bn_stop, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 9

**Test Executions:** 25

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.handleBadDatanode(DataStreamer.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: All datanodes [DatanodeInfoWithStorage[127.0.0.1:33147,DS-95c110e9-defd-4ccf-802a-2b89475b8482,DISK]] are bad. Aborting...
	at org.apache.hadoop.hdfs.DataStreamer.handleBadDatanode(DataStreamer.java:1609)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipelineInternal(DataStreamer.java:1543)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipelineForAppendOrRecovery(DataStreamer.java:1529)
	at org.apache.hadoop.hdfs.DataStreamer.processDatanodeOrExternalError(DataStreamer.java:1305)
	at org.apache.hadoop.hdfs.DataStreamer.run(DataStreamer.java:668)

```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlockManager_RestartInjected.testNeededReconstructionWhileAppending**
   - Position: after_hflush, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestHSync_RestartInjected.testHSyncWithAppend**
   - Position: after_first_hsync, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestHSync_RestartInjected.testHSyncWithAppend**
   - Position: after_second_hsync, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 10

**Test Executions:** 23

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.StandbyException
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.StandbyException: Operation category WRITE is not supported in state standby. Visit https://s.apache.org/sbnn-error
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java:108)
	at org.apache.hadoop.hdfs.server.namenode.NameNode$NameNodeHAContext.checkOperation(NameNode.java:2101)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkOperation(FSNamesystem.java:1585)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.mkdirs(FSNamesystem.java:3430)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.mkdirs(NameNodeRpcServer.java:1166)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyInProgressTail_RestartInjected.testNonUniformConfig(TestStandbyInProgressTail_RestartInjected.java:535)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccess
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyInProgressTail_RestartInjected.testNonUniformConfig**
   - Position: after_nn0_restart_and_active, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestConsistentReadsObserver_RestartInjected.testMsyncFileContext**
   - Position: after_msync_filecontext, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.ha.TestEditLogsDuringFailover_RestartInjected.testFailoverFinalizesAndReadsInProgressSimple**
   - Position: after_transition_active, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 12

**Test Executions:** 20

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.hdfs.protocol.SnapshotException)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.checkNestedSnapshottable(SnapshotManager.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.protocol.SnapshotException: 
Nested snapshottable directories not allowed: path=/TestSnapshot/sub1/subsub1/subsubsub1, the ancestor /TestSnapshot/sub1 is already a snapshottable directory.
	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.checkNestedSnapshottable(SnapshotManager.java:178)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.setSnapshottable(SnapshotManager.java:193)
	at org.apache.hadoop.hdfs.server.namenode.FSDirSnapshotOp.allowSnapshot(FSDirSnapshotOp.java:63)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.allowSnapshot(FSNamesystem.java:6931)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.allowSnapshot(NameNodeRpcServer.java:1995)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.allowSnapshot(ClientNamenodeProtocolServerSideTranslatorPB.java:1306)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingM
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDiffReport_RestartInjected.testDiffReport**
   - Position: after_mkdir_subsubsub1, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDiffReport_RestartInjected.testDiffReport**
   - Position: after_first_modify_and_snapshot, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestNestedSnapshots_RestartInjected.testNestedSnapshots**
   - Position: after_allow_snapshot_foo, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 13

**Test Executions:** 20

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.DataStreamer.findNewDatanode(DataStreamer.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: Failed to replace a bad datanode on the existing pipeline due to no more good datanodes being available to try. (Nodes: current=[DatanodeInfoWithStorage[127.0.0.1:44261,DS-ac543fe2-1919-47c5-8cd5-29388cd905e0,DISK], DatanodeInfoWithStorage[127.0.0.1:34579,DS-360bb7ed-17ef-420f-949e-907ac84ab3f6,DISK]], original=[DatanodeInfoWithStorage[127.0.0.1:44261,DS-ac543fe2-1919-47c5-8cd5-29388cd905e0,DISK], DatanodeInfoWithStorage[127.0.0.1:34579,DS-360bb7ed-17ef-420f-949e-907ac84ab3f6,DISK]]). The current failed datanode replacement policy is DEFAULT, and a client may configure this via 'dfs.client.block.write.replace-datanode-on-failure.policy' in its configuration.
	at org.apache.hadoop.hdfs.DataStreamer.findNewDatanode(DataStreamer.java:1352)
	at org.apache.hadoop.hdfs.DataStreamer.addDatanode2ExistingPipeline(DataStreamer.java:1420)
	at org.apache.hadoop.hdfs.DataStreamer.handleDatanodeReplacement(DataStreamer.java:1646)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipe
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover_RestartInjected.testWriteOverCrashFailoverWithDnFail**
   - Position: after_third_hflush_dnfail, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover_RestartInjected.testWriteOverCrashFailoverWithDnFail**
   - Position: before_close_dnfail, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover_RestartInjected.testWriteOverGracefulFailoverWithDnFail**
   - Position: after_third_hflush_dnfail, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 14

**Test Executions:** 11

**Generalized Stacktrace:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getReplicaInfo(FsDatasetImpl.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException: Replica does not exist BP-255380972-172.17.0.2-1767515071776:1
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getReplicaInfo(FsDatasetImpl.java:900)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.recoverCheck(FsDatasetImpl.java:1334)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.recoverClose(FsDatasetImpl.java:1421)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testClose(TestWriteToReplica_RestartInjected.java:394)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testClose(TestWriteToReplica_RestartInjected.java:99)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.l
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testClose**
   - Position: after_replica_setup, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testAppend**
   - Position: after_replica_setup, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.TestFileAppend_RestartInjected.testConcurrentAppendRead**
   - Position: after_file_creation_concurrent_append, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 15

**Test Executions:** 10

**Generalized Stacktrace:**
```
java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkLease(FSNamesystem.java)
```

**Raw Stacktrace Sample:**
```
java.io.FileNotFoundException: File does not exist: /test (inode 16386) [Lease.  Holder: /host4, pending creates: 1]
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkLease(FSNamesystem.java:3100)
	at org.apache.hadoop.hdfs.server.namenode.FSDirWriteFileOp.analyzeFileState(FSDirWriteFileOp.java:610)
	at org.apache.hadoop.hdfs.server.namenode.FSDirWriteFileOp.validateAddBlock(FSDirWriteFileOp.java:171)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getAdditionalBlock(FSNamesystem.java:2977)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.addBlock(NameNodeRpcServer.java:911)
	at org.apache.hadoop.hdfs.server.namenode.TestBlockPlacementPolicyRackFaultTolerant_RestartInjected.testPlacementWithOnlyOneNodeInRackDecommission(TestBlockPlacementPolicyRackFaultTolerant_RestartInjected.java:295)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.Delegatin
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestBlockPlacementPolicyRackFaultTolerant_RestartInjected.testPlacementWithOnlyOneNodeInRackDecommission**
   - Position: after_file_creation, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestDefaultBlockPlacementPolicy_RestartInjected.testPlacementWithDFSNetworkTopology**
   - Position: after_start_file, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestDefaultBlockPlacementPolicy_RestartInjected.testPlacementWithLocalRackNodesDecommissioned**
   - Position: after_start_file_in_loop, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 16

**Test Executions:** 9

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.DFSClient.checkOpen(DFSClient.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: Filesystem closed
	at org.apache.hadoop.hdfs.DFSClient.checkOpen(DFSClient.java:494)
	at org.apache.hadoop.hdfs.DFSClient.getFileInfo(DFSClient.java:1737)
	at org.apache.hadoop.hdfs.DistributedFileSystem$29.doCall(DistributedFileSystem.java:1753)
	at org.apache.hadoop.hdfs.DistributedFileSystem$29.doCall(DistributedFileSystem.java:1750)
	at org.apache.hadoop.fs.FileSystemLinkResolver.resolve(FileSystemLinkResolver.java:81)
	at org.apache.hadoop.hdfs.DistributedFileSystem.getFileStatus(DistributedFileSystem.java:1765)
	at org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones_RestartInjected.verifyEncryption(TestNestedEncryptionZones_RestartInjected.java:373)
	at org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones_RestartInjected.testNestedEZWithRoot(TestNestedEncryptionZones_RestartInjected.java:232)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones_RestartInjected.testNestedEZWithRoot**
   - Position: after_init_root_ez, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones_RestartInjected.testNestedEZWithRoot**
   - Position: after_root_ez_verification, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestNestedEncryptionZones_RestartInjected.testNestedEZWithRoot**
   - Position: after_trash_verification, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 17

**Test Executions:** 9

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getDiskBalancer(DataNode.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: DiskBalancer is not initialized
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getDiskBalancer(DataNode.java:4042)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.submitDiskBalancerPlan(DataNode.java:3942)
	at org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerRPC_RestartInjected.testCancelPlan(TestDiskBalancerRPC_RestartInjected.java:193)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(Inv
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerRPC_RestartInjected.testCancelPlan**
   - Position: after_helper_setup, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerRPC_RestartInjected.testCancelPlan**
   - Position: after_plan_submit, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.diskbalancer.TestDiskBalancerRPC_RestartInjected.testSubmitPlan**
   - Position: after_helper_setup, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 18

**Test Executions:** 8

**Generalized Stacktrace:**
```
org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil$CouldNotCatchUpException
	at org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil.waitForStandbyToCatchUp(HATestUtil.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil$CouldNotCatchUpException: Standby did not catch up to txid 1 (currently at 0)
	at org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil.waitForStandbyToCatchUp(HATestUtil.java:102)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyCheckpoints_RestartInjected.testCheckpointWhenNoNewTransactionsHappened(TestStandbyCheckpoints_RestartInjected.java:457)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.state
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyCheckpoints_RestartInjected.testCheckpointWhenNoNewTransactionsHappened**
   - Position: after_namenode_restart, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestHASafeMode_RestartInjected.testBlocksAddedBeforeStandbyRestart**
   - Position: after_restart_standby, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyCheckpoints_RestartInjected.testCheckpointSucceedsWithLegacyOIVException**
   - Position: after_edits, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 20

**Test Executions:** 8

**Generalized Stacktrace:**
```
java.util.concurrent.RejectedExecutionException
	at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java)
```

**Raw Stacktrace Sample:**
```
java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask@3730ab42 rejected from java.util.concurrent.ScheduledThreadPoolExecutor@537c8c7e[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 2]
	at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2063)
	at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:830)
	at java.util.concurrent.ScheduledThreadPoolExecutor.delayedExecute(ScheduledThreadPoolExecutor.java:326)
	at java.util.concurrent.ScheduledThreadPoolExecutor.schedule(ScheduledThreadPoolExecutor.java:533)
	at java.util.concurrent.ScheduledThreadPoolExecutor.submit(ScheduledThreadPoolExecutor.java:632)
	at org.apache.hadoop.hdfs.server.blockmanagement.DatanodeAdminManager.runMonitorForTest(DatanodeAdminManager.java:416)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerTestUtil.recheckDecommissionState(BlockM
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionStatusAfterDNRestart**
   - Position: after_file_deletion, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionDeadDN**
   - Position: after_decommission, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionStatus**
   - Position: after_verify_initial_state, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 21

**Test Executions:** 8

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.StandbyException: 
Operation category WRITE is not supported in state standby. Visit https://s.apache.org/sbnn-error
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java:108)
	at org.apache.hadoop.hdfs.server.namenode.NameNode$NameNodeHAContext.checkOperation(NameNode.java:2101)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkOperation(FSNamesystem.java:1585)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.modifyCacheDirective(NameNodeRpcServer.java:2088)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.modifyCacheDirective(ClientNamenodeProtocolServerSideTranslatorPB.java:1420)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingMethod(ClientNamenodeProtocolProtos.java)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:621)
	at org.apache.hadoop.ipc.ProtobufRpcE
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestCacheDirectives_RestartInjected.testExpiryTimeConsistency**
   - Position: after_add_directive, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover_RestartInjected.testLeaseRecoveryAfterFailover**
   - Position: after_lease_recovery, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover_RestartInjected.testLeaseRecoveryAfterFailover**
   - Position: after_failback_lease, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 24

**Test Executions:** 7

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.StripeReader.checkMissingBlocks(StripeReader.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: 6 missing blocks, the stripe is: AlignedStripe(Offset=0, length=1048576, fetchedChunksNum=0, missingChunksNum=6); locatedBlocks is: LocatedBlocks{;  fileLength=25165824;  underConstruction=false;  blocks=[LocatedStripedBlock{BP-519104461-172.17.0.2-1767514243631:blk_-9223372036854775792_1001; getBlockSize()=12582912; corrupt=false; offset=0; locs=[DatanodeInfoWithStorage[127.0.0.1:34247,5189adb1-0688-4047-9042-79b938d1d60e,DISK], DatanodeInfoWithStorage[127.0.0.1:33655,02e0f6eb-c1a9-4c7c-9206-d838af64a29d,DISK], DatanodeInfoWithStorage[127.0.0.1:44479,d3c8314a-2823-4a43-a5dd-61fd23da3378,DISK], DatanodeInfoWithStorage[127.0.0.1:35527,bcf990fa-3bf6-4466-bd6a-b0dfb35a22fc,DISK], DatanodeInfoWithStorage[127.0.0.1:39595,ffde83d3-8abc-4c6e-97af-2facdc853567,DISK], DatanodeInfoWithStorage[127.0.0.1:38487,28c472fd-89f8-4a20-bfaa-a5bdc5515c0d,DISK], DatanodeInfoWithStorage[127.0.0.1:45827,947a45b3-00cc-4df8-9209-749ef8dd7772,DISK], DatanodeInfoWithStorage[127.0.0.1:33099,f
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.TestDFSStripedInputStream_RestartInjected.testStatefulRead**
   - Position: after_stateful_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.TestDFSStripedInputStream_RestartInjected.testStatefulRead**
   - Position: after_stateful_file_creation, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.TestDFSStripedInputStream_RestartInjected.testStatefulRead**
   - Position: after_stateful_file_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 27

**Test Executions:** 6

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.ObserverRetryOnActiveException)
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.ObserverRetryOnActiveException): Operation category WRITE is not supported in state observer. Visit https://s.apache.org/sbnn-error
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java:106)
	at org.apache.hadoop.hdfs.server.namenode.NameNode$NameNodeHAContext.checkOperation(NameNode.java:2101)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkOperation(FSNamesystem.java:1585)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.delete(NameNodeRpcServer.java:1127)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.delete(ClientNamenodeProtocolServerSideTranslatorPB.java:727)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingMethod(ClientNamenodeProtocolProtos.java)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:621)
	at org.apache
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestMultiObserverNode_RestartInjected.testObserverFallBehind**
   - Position: after_verification, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestMultiObserverNode_RestartInjected.testMultiObserver**
   - Position: after_manual_restart_nn2, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.ha.TestMultiObserverNode_RestartInjected.testMultiObserver**
   - Position: after_both_observers_restored, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 28

**Test Executions:** 5

**Generalized Stacktrace:**
```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException
	at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException: Cannot obtain block length for LocatedBlock{BP-866522546-172.17.0.2-1767512653092:blk_1073741825_1001; getBlockSize()=4; corrupt=false; offset=0; locs=[DatanodeInfoWithStorage[127.0.0.1:33257,DS-680f2e4b-ad2d-4870-ad77-cdf162777964,DISK]]; cachedLocs=[]} of /test
	at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java:414)
	at org.apache.hadoop.hdfs.DFSInputStream.getLastBlockLength(DFSInputStream.java:323)
	at org.apache.hadoop.hdfs.DFSInputStream.openInfo(DFSInputStream.java:243)
	at org.apache.hadoop.hdfs.DFSInputStream.<init>(DFSInputStream.java:213)
	at org.apache.hadoop.hdfs.DFSClient.openInternal(DFSClient.java:1084)
	at org.apache.hadoop.hdfs.DFSClient.open(DFSClient.java:1047)
	at org.apache.hadoop.hdfs.DistributedFileSystem$4.doCall(DistributedFileSystem.java:340)
	at org.apache.hadoop.hdfs.DistributedFileSystem$4.doCall(DistributedFileSystem.java:336)
	at org.apache.hadoop.fs.FileSystemLinkResolve
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery2_RestartInjected.testRaceBetweenReplicaRecoveryAndFinalizeBlock**
   - Position: after_file_creation_hsync, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.TestFileAppend_RestartInjected.testSimpleFlush**
   - Position: after_second_flush, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestReplicaCachingGetSpaceUsed_RestartInjected.testReplicaCachingGetSpaceUsedByRBWReplica**
   - Position: after_rbw_replica_created, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 31

**Test Executions:** 4

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl.getBlockPoolSlice(FsVolumeImpl.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: block pool BP-72657333-172.17.0.2-1767514679220 is not found
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl.getBlockPoolSlice(FsVolumeImpl.java:508)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl.createTmpFile(FsVolumeImpl.java:561)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl.createTemporary(FsVolumeImpl.java:1277)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.createTemporary(FsDatasetImpl.java:1731)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testWriteToTemporary(TestWriteToReplica_RestartInjected.java:537)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testWriteToTemporary(TestWriteToReplica_RestartInjected.java:228)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.re
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestWriteToReplica_RestartInjected.testWriteToTemporary**
   - Position: after_replica_setup, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testVolumeIteratorWithoutCaching**
   - Position: after_iterator_created, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testVolumeIteratorWithCaching**
   - Position: after_iterator_created, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 36

**Test Executions:** 3

**Generalized Stacktrace:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockLocalPathInfo(FsDatasetImpl.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException: Replica not found for BP-353107325-172.17.0.2-1767516406076:blk_1073741825_1001. The block may have been removed recently by the balancer or by intentionally reducing the replication factor. This condition is usually harmless. To be certain, please check the preceding datanode log messages for signs of a more serious issue.
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockLocalPathInfo(FsDatasetImpl.java:2995)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.getBlockForFile(TestDataNodeRollingUpgrade_RestartInjected.java:114)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testWithLayoutChangeAndFinalize(TestDataNodeRollingUpgrade_RestartInjected.java:449)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.De
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testWithLayoutChangeAndFinalize**
   - Position: after_start_rolling_upgrade, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testWithLayoutChangeAndRollback**
   - Position: after_start_rolling_upgrade, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testDatanodeRollingUpgradeWithRollback**
   - Position: after_start_rolling_upgrade, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 37

**Test Executions:** 3

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.isTrashRootPresent(TestDataNodeRollingUpgrade_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.isTrashRootPresent(TestDataNodeRollingUpgrade_RestartInjected.java:150)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testWithLayoutChangeAndFinalize(TestDataNodeRollingUpgrade_RestartInjected.java:498)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testWithLayoutChangeAndFinalize**
   - Position: after_finalize, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testWithLayoutChangeAndRollback**
   - Position: after_rollback, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeRollingUpgrade_RestartInjected.testDatanodeRollingUpgradeWithRollback**
   - Position: after_rollback, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 39

**Test Executions:** 3

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getBPOSForBlock(DataNode.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: cannot locate OfferService thread for bp=BP-908986689-172.17.0.2-1767518882149
	at org.apache.hadoop.hdfs.server.datanode.DataNode.getBPOSForBlock(DataNode.java:1635)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.transferBlock(DataNode.java:2632)
	at org.apache.hadoop.hdfs.server.datanode.TestDiskError_RestartInjected.testDataTransferWhenBytesPerChecksumIsZero(TestDiskError_RestartInjected.java:389)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.int
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDiskError_RestartInjected.testDataTransferWhenBytesPerChecksumIsZero**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetImpl_RestartInjected.testReportBadBlocks**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetImpl_RestartInjected.testReportBadBlocks**
   - Position: before_report_bad_blocks, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 41

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.saveNamespace(FSNamesystem.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: Safe mode should be turned ON in order to create namespace image.
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.saveNamespace(FSNamesystem.java:4902)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.saveNamespace(NameNodeRpcServer.java:1312)
	at org.apache.hadoop.hdfs.server.namenode.TestSaveNamespace_RestartInjected.testSaveNamespaceBeforeShutdown(TestSaveNamespace_RestartInjected.java:800)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.j
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestSaveNamespace_RestartInjected.testSaveNamespaceBeforeShutdown**
   - Position: after_first_savenamespace_check, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestParallelImageWrite_RestartInjected.testRestartDFS**
   - Position: after_first_check_images, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 42

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.verifyBlockPoolMissing(FsDatasetImplTestUtils.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: Block pool directory /workspace/apps/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test/data/dfs/data/data1/current/BP-1924108935-172.17.0.2-1767513172722 exists
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.verifyBlockPoolMissing(FsDatasetImplTestUtils.java:493)
	at org.apache.hadoop.hdfs.server.datanode.TestDeleteBlockPool_RestartInjected.testDfsAdminDeleteBlockPool(TestDeleteBlockPool_RestartInjected.java:314)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDeleteBlockPool_RestartInjected.testDfsAdminDeleteBlockPool**
   - Position: after_admin_delete_with_force, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDeleteBlockPool_RestartInjected.testDeleteBlockPool**
   - Position: after_delete_blockpool_dn1, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 43

**Test Executions:** 2

**Generalized Stacktrace:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.updateReplicaUnderRecovery(FsDatasetImpl.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException: Replica not found for BP-247852084-172.17.0.2-1767512745041:blk_1073741825_1001. The block may have been removed recently by the balancer or by intentionally reducing the replication factor. This condition is usually harmless. To be certain, please check the preceding datanode log messages for signs of a more serious issue.
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.updateReplicaUnderRecovery(FsDatasetImpl.java:2739)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.updateReplicaUnderRecovery(DataNode.java:3285)
	at org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery2_RestartInjected.testRaceBetweenReplicaRecoveryAndFinalizeBlock(TestBlockRecovery2_RestartInjected.java:315)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAc
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery2_RestartInjected.testRaceBetweenReplicaRecoveryAndFinalizeBlock**
   - Position: after_recovery_thread_start, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestInterDatanodeProtocol_RestartInjected.testUpdateReplicaUnderRecovery**
   - Position: before_replica_update, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 45

**Test Executions:** 2

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.hdfs.protocol.NSQuotaExceededException)
	at org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.verifyNamespaceQuota(DirectoryWithQuotaFeature.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.protocol.NSQuotaExceededException: 
The NameSpace quota (directories and files) of directory /TestFSDirectory_RestartInjected/sub2 is exceeded: quota=1 file count=2
	at org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.verifyNamespaceQuota(DirectoryWithQuotaFeature.java:193)
	at org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.verifyQuota(DirectoryWithQuotaFeature.java:226)
	at org.apache.hadoop.hdfs.server.namenode.FSDirectory.verifyQuota(FSDirectory.java:1221)
	at org.apache.hadoop.hdfs.server.namenode.FSDirectory.updateCount(FSDirectory.java:1052)
	at org.apache.hadoop.hdfs.server.namenode.FSDirectory.addLastINode(FSDirectory.java:1373)
	at org.apache.hadoop.hdfs.server.namenode.FSDirectory.addINode(FSDirectory.java:1184)
	at org.apache.hadoop.hdfs.server.namenode.FSDirWriteFileOp.addFile(FSDirWriteFileOp.java:579)
	at org.apache.hadoop.hdfs.server.namenode.FSDirWriteFileOp.startFile(FSDirWriteFileOp.java:398)
	at org.apache.hado
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestFSDirectory_RestartInjected.testSkipQuotaCheck**
   - Position: after_set_quota, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestFSDirectory_RestartInjected.testSkipQuotaCheck**
   - Position: after_disable_quota, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 46

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock3(TestSnapshotBlocksMap_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock3(TestSnapshotBlocksMap_RestartInjected.java:563)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock3**
   - Position: after_append_and_addblock, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock3**
   - Position: after_snapshot_creation, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 47

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.ArrayIndexOutOfBoundsException
	at java.util.concurrent.CopyOnWriteArrayList.get(CopyOnWriteArrayList.java)
```

**Raw Stacktrace Sample:**
```
java.lang.ArrayIndexOutOfBoundsException: 0
	at java.util.concurrent.CopyOnWriteArrayList.get(CopyOnWriteArrayList.java:388)
	at java.util.concurrent.CopyOnWriteArrayList.get(CopyOnWriteArrayList.java:397)
	at java.util.Collections$UnmodifiableList.get(Collections.java:1311)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeExit_RestartInjected.stopBPServiceThreads(TestDataNodeExit_RestartInjected.java:76)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeExit_RestartInjected.testBPServiceExit(TestDataNodeExit_RestartInjected.java:99)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveC
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeExit_RestartInjected.testBPServiceExit**
   - Position: after_get_datanode, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR_RestartInjected.testBlockCounters**
   - Position: after_cluster_setup, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 49

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.namenode.INodeFile.valueOf(INodeFile.java)
```

**Raw Stacktrace Sample:**
```
java.io.FileNotFoundException: File does not exist: /TestSnapshot/tempdir/tempfile
	at org.apache.hadoop.hdfs.server.namenode.INodeFile.valueOf(INodeFile.java:86)
	at org.apache.hadoop.hdfs.server.namenode.INodeFile.valueOf(INodeFile.java:76)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap.assertBlockCollection(TestSnapshotBlocksMap.java:93)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory(TestSnapshotDeletion_RestartInjected.java:351)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:1
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory**
   - Position: after_delete_deletedir, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory**
   - Position: after_snapshot_s0, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 50

**Test Executions:** 2

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.RemoteException(java.io.IOException)
	at org.apache.hadoop.hdfs.server.namenode.FSDirAppendOp.appendFile(FSDirAppendOp.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.RemoteException(java.io.IOException): append: lastBlock=blk_1073741840_1016 of src=/test.dat is not sufficiently replicated yet.
	at org.apache.hadoop.hdfs.server.namenode.FSDirAppendOp.appendFile(FSDirAppendOp.java:138)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.appendFile(FSNamesystem.java:2921)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.append(NameNodeRpcServer.java:837)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.append(ClientNamenodeProtocolServerSideTranslatorPB.java:521)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingMethod(ClientNamenodeProtocolProtos.java)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:621)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:589)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcIn
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeMetrics_RestartInjected.testVolumeMetricsWithVolumeDepartureArrival**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeMetrics_RestartInjected.testVolumeMetrics**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 51

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.IndexOutOfBoundsException
	at java.util.ArrayList.rangeCheck(ArrayList.java)
```

**Raw Stacktrace Sample:**
```
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
	at java.util.ArrayList.rangeCheck(ArrayList.java:659)
	at java.util.ArrayList.get(ArrayList.java:435)
	at org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin_RestartInjected.assertOutMsg(TestViewFileSystemOverloadSchemeWithDFSAdmin_RestartInjected.java:138)
	at org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin_RestartInjected.testSaveNamespaceWithoutSpecifyingFS(TestViewFileSystemOverloadSchemeWithDFSAdmin_RestartInjected.java:232)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.tools.TestViewFileSystemOverloadSchemeWithDFSAdmin_RestartInjected.testSaveNamespaceWithoutSpecifyingFS**
   - Position: after_safemode_enter, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestBPOfferService_RestartInjected.testCommandProcessingThreadExit**
   - Position: after_bpos_initialization, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 52

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.duplicateBlock(TestDirectoryScanner_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.duplicateBlock(TestDirectoryScanner_RestartInjected.java:199)
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testDeleteBlockOnTransientStorage(TestDirectoryScanner_RestartInjected.java:574)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runn
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testDeleteBlockOnTransientStorage**
   - Position: after_first_scan, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testRetainBlockOnPersistentStorage**
   - Position: after_first_scan, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 54

**Test Executions:** 2

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.RemoteException(java.io.IOException)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.chooseTarget4NewBlock(BlockManager.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.RemoteException(java.io.IOException): File /testBlockTokenInLastLocatedBlock could only be written to 0 of the 1 minReplication nodes. There are 1 datanode(s) running and 1 node(s) are excluded in this operation.
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.chooseTarget4NewBlock(BlockManager.java:2338)
	at org.apache.hadoop.hdfs.server.namenode.FSDirWriteFileOp.chooseTargetForNewBlock(FSDirWriteFileOp.java:294)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getAdditionalBlock(FSNamesystem.java:2989)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.addBlock(NameNodeRpcServer.java:911)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.addBlock(ClientNamenodeProtocolServerSideTranslatorPB.java:595)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingMethod(ClientNamenodeProtocolProtos.java)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.security.token.block.TestBlockToken_RestartInjected.testBlockTokenInLastLocatedBlockLegacy**
   - Position: before_close, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.security.token.block.TestBlockToken_RestartInjected.testBlockTokenInLastLocatedBlockProtobuf**
   - Position: before_close, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 55

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestReencryption_RestartInjected.getFileEncryptionInfo(TestReencryption_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestReencryption_RestartInjected.getFileEncryptionInfo(TestReencryption_RestartInjected.java:165)
	at org.apache.hadoop.hdfs.server.namenode.TestReencryption_RestartInjected.testReencryptOrdering(TestReencryption_RestartInjected.java:366)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBef
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestReencryption_RestartInjected.testReencryptOrdering**
   - Position: after_zone_created, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestReencryption_RestartInjected.testReencryptionBasic**
   - Position: after_encryption_zone_created, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 56

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockInputStream(FsDatasetImpl.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: No data exists for block BP-1446535458-172.17.0.2-1767514816735:blk_1073741825_1001
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockInputStream(FsDatasetImpl.java:834)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetCache_RestartInjected.getBlockSizes(TestFsDatasetCache_RestartInjected.java:274)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetCache_RestartInjected.testCacheAndUncacheBlock(TestFsDatasetCache_RestartInjected.java:307)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetCache_RestartInjected.testCacheAndUncacheBlockSimple(TestFsDatasetCache_RestartInjected.java:365)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at or
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetCache_RestartInjected.testCacheAndUncacheBlockSimple**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetCache_RestartInjected.testCacheAndUncacheBlockWithRetries**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 59

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.common.blockaliasmap.impl.InMemoryLevelDBAliasMapClient.getAliasMap(InMemoryLevelDBAliasMapClient.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: Unable to retrieve InMemoryAliasMap for block pool id BP-1238020335-172.17.0.2-1767512309079
	at org.apache.hadoop.hdfs.server.common.blockaliasmap.impl.InMemoryLevelDBAliasMapClient.getAliasMap(InMemoryLevelDBAliasMapClient.java:173)
	at org.apache.hadoop.hdfs.server.common.blockaliasmap.impl.InMemoryLevelDBAliasMapClient.getReader(InMemoryLevelDBAliasMapClient.java:180)
	at org.apache.hadoop.hdfs.server.aliasmap.TestSecureAliasMap_RestartInjected.testSecureConnectionToAliasMap(TestSecureAliasMap_RestartInjected.java:199)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCa
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.aliasmap.TestSecureAliasMap_RestartInjected.testSecureConnectionToAliasMap**
   - Position: after_get_block_manager, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.aliasmap.TestSecureAliasMap_RestartInjected.testSecureConnectionToAliasMap**
   - Position: after_get_alias_map, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 61

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockReplica(FsDatasetImpl.java)
```

**Raw Stacktrace Sample:**
```
java.io.FileNotFoundException: BlockId 1073741825 is not valid.
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockReplica(FsDatasetImpl.java:814)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getBlockReplica(FsDatasetImpl.java:805)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.getMetaDataInputStream(FsDatasetImpl.java:238)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestReplicaCachingGetSpaceUsed_RestartInjected.testReplicaCachingGetSpaceUsedByFINALIZEDReplica(TestReplicaCachingGetSpaceUsed_RestartInjected.java:115)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at 
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestReplicaCachingGetSpaceUsed_RestartInjected.testReplicaCachingGetSpaceUsedByFINALIZEDReplica**
   - Position: after_file_finalized, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 62

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock2(TestSnapshotBlocksMap_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock2(TestSnapshotBlocksMap_RestartInjected.java:500)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock2**
   - Position: after_append_and_addblock, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 63

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks_RestartInjected.runBalancerAndVerifyBlockPlacmentPolicy(TestBalancerLongRunningTasks_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks_RestartInjected.runBalancerAndVerifyBlockPlacmentPolicy(TestBalancerLongRunningTasks_RestartInjected.java:552)
	at org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks_RestartInjected.testUpgradeDomainPolicyAfterBalance(TestBalancerLongRunningTasks_RestartInjected.java:480)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks_RestartInjected.testUpgradeDomainPolicyAfterBalance**
   - Position: after_create_file, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 64

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.RuntimeException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.RamDiskAsyncLazyPersistService.queryVolume(RamDiskAsyncLazyPersistService.java)
```

**Raw Stacktrace Sample:**
```
java.lang.RuntimeException: AsyncLazyPersistService is already shutdown
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.RamDiskAsyncLazyPersistService.queryVolume(RamDiskAsyncLazyPersistService.java:146)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.setupAsyncLazyPersistThread(FsDatasetImpl.java:3118)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.setupAsyncLazyPersistThreads(FsDatasetImpl.java:3098)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.removeVolumes(FsDatasetImpl.java:620)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.removeVolumes(DataNode.java:1216)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.removeVolumes(DataNode.java:1185)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.refreshVolumes(DataNode.java:1153)
	at org.apache.hadoop.hdfs.server.datanode.DataNode.reconfigurePropertyImpl(DataNode.java:614)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureRep
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testHotSwapOutFailedVolumeAndReporting**
   - Position: after_first_volume_failure, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 65

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.server.datanode.DataNode.refreshVolumes(DataNode.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.conf.ReconfigurationException: Could not change property dfs.datanode.data.dir from '[DISK]file:/workspace/apps/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test/data/dfs/data/data2' to '[DISK]file:/workspace/apps/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test/data/dfs/data/data1,[DISK]file:/workspace/apps/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test/data/dfs/data/data2'
	at org.apache.hadoop.hdfs.server.datanode.DataNode.reconfigurePropertyImpl(DataNode.java:632)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testHotSwapOutFailedVolumeAndReporting(TestDataNodeVolumeFailureReporting_RestartInjected.java:787)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runne
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testHotSwapOutFailedVolumeAndReporting**
   - Position: after_hot_swap_out, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 66

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestSnapshotPathINodes_RestartInjected.testSnapshotPathINodesWithAddedFile(TestSnapshotPathINodes_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestSnapshotPathINodes_RestartInjected.testSnapshotPathINodesWithAddedFile(TestSnapshotPathINodes_RestartInjected.java:438)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.juni
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestSnapshotPathINodes_RestartInjected.testSnapshotPathINodesWithAddedFile**
   - Position: after_snapshot_create, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 67

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testScanAllBlocksImpl(TestBlockScanner_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testScanAllBlocksImpl(TestBlockScanner_RestartInjected.java:482)
	at org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testScanAllBlocksNoRescan(TestBlockScanner_RestartInjected.java:497)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.Ru
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testScanAllBlocksNoRescan**
   - Position: after_scanner_started, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 68

**Test Executions:** 1

**Generalized Stacktrace:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.getMaterializedReplica(FsDatasetImplTestUtils.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException: Replica not found for BP-252939411-172.17.0.2-1767517688455:blk_1073741825_1001. The block may have been removed recently by the balancer or by intentionally reducing the replication factor. This condition is usually harmless. To be certain, please check the preceding datanode log messages for signs of a more serious issue.
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.getMaterializedReplica(FsDatasetImplTestUtils.java:231)
	at org.apache.hadoop.hdfs.MiniDFSCluster.getMaterializedReplica(MiniDFSCluster.java:3193)
	at org.apache.hadoop.hdfs.MiniDFSCluster.corruptReplica(MiniDFSCluster.java:2373)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected.testCorruptBlockRereplicatedAcrossRacks(TestBlocksWithNotEnoughRacks_RestartInjected.java:366)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.inv
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected.testCorruptBlockRereplicatedAcrossRacks**
   - Position: after_file_create, Target: datanode, Mode: CRASH

---

### [ ] Group 69

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock(TestSnapshotBlocksMap_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock(TestSnapshotBlocksMap_RestartInjected.java:435)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)

```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap_RestartInjected.testDeletionWithZeroSizeBlock**
   - Position: after_append_and_addblock, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 70

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory(TestSnapshotDeletion_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory(TestSnapshotDeletion_RestartInjected.java:418)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory**
   - Position: after_replication_changes, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 72

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteSnapshot2(TestSnapshotDeletion_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteSnapshot2(TestSnapshotDeletion_RestartInjected.java:850)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.juni
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteSnapshot2**
   - Position: after_snapshot_s1, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 73

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.saveFSImageInAllDirs(FSImage.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: No image directories available!
	at org.apache.hadoop.hdfs.server.namenode.FSImage.saveFSImageInAllDirs(FSImage.java:1219)
	at org.apache.hadoop.hdfs.server.namenode.FSImage.saveNamespace(FSImage.java:1163)
	at org.apache.hadoop.hdfs.server.namenode.FSImage.saveNamespace(FSImage.java:1133)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.saveNamespace(FSNamesystem.java:4905)
	at org.apache.hadoop.hdfs.server.namenode.TestFSImage_RestartInjected.testHasNonEcBlockUsingStripedIDForLoadSnapshot(TestFSImage_RestartInjected.java:844)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestFSImage_RestartInjected.testHasNonEcBlockUsingStripedIDForLoadSnapshot**
   - Position: after_truncate, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 74

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.tools.TestDFSZKFailoverController_RestartInjected.testFailoverAndBackOnNNShutdown(TestDFSZKFailoverController_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.tools.TestDFSZKFailoverController_RestartInjected.testFailoverAndBackOnNNShutdown(TestDFSZKFailoverController_RestartInjected.java:251)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.apache.zookeeper.JUnit4ZKTestRunner$LoggedInvokeMethod.evaluate(JUnit4ZKTestRunner.java:80)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBe
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.tools.TestDFSZKFailoverController_RestartInjected.testFailoverAndBackOnNNShutdown**
   - Position: after_cluster_start, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 76

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionDeadDN(TestDecommissioningStatus_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionDeadDN(TestDecommissioningStatus_RestartInjected.java:582)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionDeadDN**
   - Position: after_datanode_stopped, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 77

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.IllegalMonitorStateException
	at java.util.concurrent.locks.ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java)
```

**Raw Stacktrace Sample:**
```
java.lang.IllegalMonitorStateException
	at java.util.concurrent.locks.ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java:371)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer.release(AbstractQueuedSynchronizer.java:1261)
	at java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock.unlock(ReentrantReadWriteLock.java:1131)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystemLock.writeUnlock(FSNamesystemLock.java:284)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystemLock.writeUnlock(FSNamesystemLock.java:236)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.writeUnlock(FSNamesystem.java:1765)
	at org.apache.hadoop.hdfs.server.namenode.TestBlockPlacementPolicyRackFaultTolerant_RestartInjected.testPlacementWithOnlyOneNodeInRackDecommission(TestBlockPlacementPolicyRackFaultTolerant_RestartInjected.java:346)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:6
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestBlockPlacementPolicyRackFaultTolerant_RestartInjected.testPlacementWithOnlyOneNodeInRackDecommission**
   - Position: after_second_decommission, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 78

**Test Executions:** 1

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.RetriableException)
	at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.RetriableException): Server too busy - disconnecting
	at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java:1584)
	at org.apache.hadoop.ipc.Client.call(Client.java:1530)
	at org.apache.hadoop.ipc.Client.call(Client.java:1427)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:258)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:139)
	at com.sun.proxy.$Proxy28.getFileInfo(Unknown Source)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolTranslatorPB.getFileInfo(ClientNamenodeProtocolTranslatorPB.java:966)
	at sun.reflect.GeneratedMethodAccessor5.invoke(Unknown Source)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.apache.hadoop.hdfs.server.namenode.ha.ObserverReadProxyProvider$ObserverReadInvocationHandler.invoke(ObserverReadProxyProvider.java
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestConsistentReadsObserver_RestartInjected.testRequeueCall**
   - Position: after_create_file, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 79

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcat(TestHDFSConcat_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcat(TestHDFSConcat_RestartInjected.java:143)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.runners.ParentRunner$3.evalu
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcat**
   - Position: after_target_file_creation, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 80

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.net.SocketException
	at sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java)
```

**Raw Stacktrace Sample:**
```
java.net.SocketException: Unexpected end of file from server
	at sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:873)
	at sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:899)
	at sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:680)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1610)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1515)
	at java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:480)
	at sun.net.www.protocol.https.HttpsURLConnectionImpl.getResponseCode(HttpsURLConnectionImpl.java:352)
	at org.apache.hadoop.hdfs.web.WebHdfsFileSystem.validateResponse(WebHdfsFileSystem.java:496)
	at org.apache.hadoop.hdfs.web.WebHdfsFileSystem.access$300(WebHdfsFileSystem.java:143)
	at org.apache.hadoop.hdfs.web.WebHdfsFileSystem$FsPathOutputStreamRunner$1.close(WebHdfsFileSystem.java:1041)
	at org.apache.hadoop.hdfs.web.TestHttpsFileSystem_RestartInjected.testSWebHdfsFil
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.web.TestHttpsFileSystem_RestartInjected.testSWebHdfsFileSystem**
   - Position: after_write, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 82

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.deleteBlockFile(TestDirectoryScanner_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.deleteBlockFile(TestDirectoryScanner_RestartInjected.java:160)
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testScanDirectoryStructureWarn(TestDirectoryScanner_RestartInjected.java:494)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runner
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testScanDirectoryStructureWarn**
   - Position: after_first_scan, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 83

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.net.SocketException
	at java.net.SocketInputStream.read(SocketInputStream.java)
```

**Raw Stacktrace Sample:**
```
java.net.SocketException: Connection reset
	at java.net.SocketInputStream.read(SocketInputStream.java:210)
	at java.net.SocketInputStream.read(SocketInputStream.java:141)
	at java.io.BufferedInputStream.fill(BufferedInputStream.java:246)
	at java.io.BufferedInputStream.read1(BufferedInputStream.java:286)
	at java.io.BufferedInputStream.read(BufferedInputStream.java:345)
	at sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:745)
	at sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:680)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1610)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1515)
	at java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:480)
	at org.apache.hadoop.hdfs.web.WebHdfsFileSystem.validateResponse(WebHdfsFileSystem.java:496)
	at org.apache.hadoop.hdfs.web.WebHdfsFileSystem.access$300(WebHdfsFileSystem.java:143)
	at org.apache.hadoop.hdfs.web.WebHdfsFileSystem$FsPa
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.web.TestWebHDFSForHA_RestartInjected.testFailoverAfterOpen**
   - Position: after_create, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 84

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.deleteMetaFile(TestDirectoryScanner_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.deleteMetaFile(TestDirectoryScanner_RestartInjected.java:176)
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.runTest(TestDirectoryScanner_RestartInjected.java:651)
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testDirectoryScanner(TestDirectoryScanner_RestartInjected.java:603)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testDirectoryScanner**
   - Position: after_test1_scan, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 86

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestInterDatanodeProtocol_RestartInjected.testUpdateReplicaUnderRecovery(TestInterDatanodeProtocol_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestInterDatanodeProtocol_RestartInjected.testUpdateReplicaUnderRecovery(TestInterDatanodeProtocol_RestartInjected.java:406)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestInterDatanodeProtocol_RestartInjected.testUpdateReplicaUnderRecovery**
   - Position: after_replica_recovery_init, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 87

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.io.IOException
	at org.apache.hadoop.ipc.Client.getConnection(Client.java)
```

**Raw Stacktrace Sample:**
```
java.io.IOException: Failed to get connection for localhost/127.0.0.1:42919, Call15: Client-338e63cf0a6b46c2b32b2a7ae215a3dd is already stopped
	at org.apache.hadoop.ipc.Client.getConnection(Client.java:1629)
	at org.apache.hadoop.ipc.Client.call(Client.java:1474)
	at org.apache.hadoop.ipc.Client.call(Client.java:1427)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:258)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:139)
	at com.sun.proxy.$Proxy23.blockReport(Unknown Source)
	at org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPB.blockReport(DatanodeProtocolClientSideTranslatorPB.java:218)
	at org.apache.hadoop.hdfs.server.datanode.TestLargeBlockReport_RestartInjected.testBlockReportSucceedsWithLargerLengthLimit(TestLargeBlockReport_RestartInjected.java:134)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestLargeBlockReport_RestartInjected.testBlockReportSucceedsWithLargerLengthLimit**
   - Position: after_cluster_init, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 92

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.RuntimeException
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetAsyncDiskService.execute(FsDatasetAsyncDiskService.java)
```

**Raw Stacktrace Sample:**
```
java.lang.RuntimeException: AsyncDiskService is already shutdown
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetAsyncDiskService.execute(FsDatasetAsyncDiskService.java:178)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetAsyncDiskService.deleteAsync(FsDatasetAsyncDiskService.java:248)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.invalidate(FsDatasetImpl.java:2177)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.invalidate(FsDatasetImpl.java:2099)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetImpl_RestartInjected.testDeletingBlocks(TestFsDatasetImpl_RestartInjected.java:657)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.r
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestFsDatasetImpl_RestartInjected.testDeletingBlocks**
   - Position: after_replica_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 94

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.IndexOutOfBoundsException
	at java.util.Collections$EmptyList.get(Collections.java)
```

**Raw Stacktrace Sample:**
```
java.lang.IndexOutOfBoundsException: Index: 0
	at java.util.Collections$EmptyList.get(Collections.java:4456)
	at org.apache.hadoop.hdfs.server.namenode.TestBlockUnderConstruction_RestartInjected.testEmptyExpectedLocations(TestBlockUnderConstruction_RestartInjected.java:222)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.run
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestBlockUnderConstruction_RestartInjected.testEmptyExpectedLocations**
   - Position: after_file_creation, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 97

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestDiffListBySkipList_RestartInjected.testAddFirst(TestDiffListBySkipList_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestDiffListBySkipList_RestartInjected.testAddFirst(TestDiffListBySkipList_RestartInjected.java:225)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestDiffListBySkipList_RestartInjected.testAddFirst(TestDiffListBySkipList_RestartInjected.java:182)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.r
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestDiffListBySkipList_RestartInjected.testAddFirst**
   - Position: after_creating_children_in_addFirst, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 99

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.ArrayIndexOutOfBoundsException
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testExceptionHandlingWhileDirectoryScan(TestDirectoryScanner_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.ArrayIndexOutOfBoundsException: 0
	at org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testExceptionHandlingWhileDirectoryScan(TestDirectoryScanner_RestartInjected.java:1242)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDirectoryScanner_RestartInjected.testExceptionHandlingWhileDirectoryScan**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 100

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.namenode.INodeDirectory.valueOf(INodeDirectory.java)
```

**Raw Stacktrace Sample:**
```
java.io.FileNotFoundException: Directory does not exist: /TestSnapshot/sub
	at org.apache.hadoop.hdfs.server.namenode.INodeDirectory.valueOf(INodeDirectory.java:60)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSetQuotaWithSnapshot_RestartInjected.testSetQuota(TestSetQuotaWithSnapshot_RestartInjected.java:110)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statemen
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSetQuotaWithSnapshot_RestartInjected.testSetQuota**
   - Position: after_snapshot_creation, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 101

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestINodeFileUnderConstructionWithSnapshot_RestartInjected.testSnapshotAfterAppending(TestINodeFileUnderConstructionWithSnapshot_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestINodeFileUnderConstructionWithSnapshot_RestartInjected.testSnapshotAfterAppending(TestINodeFileUnderConstructionWithSnapshot_RestartInjected.java:166)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestINodeFileUnderConstructionWithSnapshot_RestartInjected.testSnapshotAfterAppending**
   - Position: after_create_snapshot_s0, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 102

**Test Executions:** 1

**Generalized Stacktrace:**
```
org.apache.hadoop.ipc.RemoteException(java.io.IOException)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.findAndMarkBlockAsCorrupt(BlockManager.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.ipc.RemoteException(java.io.IOException): Cannot mark BP-1752846739-172.17.0.2-1767515832246:blk_1073741825_1001 as corrupt because datanode DatanodeInfoWithStorage[127.0.0.1:38239,null,DISK] (8831099a-9262-4b81-9360-4d478ab1b8c6) does not exist
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.findAndMarkBlockAsCorrupt(BlockManager.java:1785)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.reportBadBlocks(FSNamesystem.java:5791)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.reportBadBlocks(NameNodeRpcServer.java:978)
	at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolServerSideTranslatorPB.reportBadBlocks(ClientNamenodeProtocolServerSideTranslatorPB.java:655)
	at org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos$ClientNamenodeProtocol$2.callBlockingMethod(ClientNamenodeProtocolProtos.java)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:621)
	at org.apach
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestReadOnlySharedStorage_RestartInjected.testReadOnlyReplicaCorrupt**
   - Position: after_setup_complete, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 103

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testRenameAndDelete(TestSnapshotDeletion_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testRenameAndDelete(TestSnapshotDeletion_RestartInjected.java:1768)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.jun
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testRenameAndDelete**
   - Position: after_snapshot_s0, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 104

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSetQuotaWithSnapshot_RestartInjected.testClearQuota(TestSetQuotaWithSnapshot_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSetQuotaWithSnapshot_RestartInjected.testClearQuota(TestSetQuotaWithSnapshot_RestartInjected.java:212)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.j
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSetQuotaWithSnapshot_RestartInjected.testClearQuota**
   - Position: after_first_snapshot, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 106

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.nio.file.NoSuchFileException
	at sun.nio.fs.UnixException.translateToIOException(UnixException.java)
```

**Raw Stacktrace Sample:**
```
java.nio.file.NoSuchFileException: /workspace/apps/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/test/data/dfs/name1/current/edits_inprogress_0000000000000000005
	at sun.nio.fs.UnixException.translateToIOException(UnixException.java:86)
	at sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:102)
	at sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:107)
	at sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:214)
	at java.nio.file.Files.newByteChannel(Files.java:361)
	at java.nio.file.Files.newByteChannel(Files.java:407)
	at java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:384)
	at java.nio.file.Files.newInputStream(Files.java:152)
	at org.apache.hadoop.hdfs.util.MD5FileUtils.computeMd5ForFile(MD5FileUtils.java:128)
	at org.apache.hadoop.hdfs.server.namenode.FSImageTestUtil.getFileMD5(FSImageTestUtil.java:95)
	at org.apache.hadoop.hdfs.server.namenode.TestStorageRestore_RestartInjected.testStorageRestore(T
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestStorageRestore_RestartInjected.testStorageRestore**
   - Position: after_third_dir_create, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 107

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.mockito.Mockito.spy(Mockito.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.mockito.Mockito.spy(Mockito.java:1992)
	at org.apache.hadoop.hdfs.server.namenode.TestStorageRestore_RestartInjected.invalidateStorage(TestStorageRestore_RestartInjected.java:130)
	at org.apache.hadoop.hdfs.server.namenode.TestStorageRestore_RestartInjected.testMultipleSecondaryCheckpoint(TestStorageRestore_RestartInjected.java:437)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMet
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestStorageRestore_RestartInjected.testMultipleSecondaryCheckpoint**
   - Position: after_file_create, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 109

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.DistributedFileSystem$29.doCall(DistributedFileSystem.java)
```

**Raw Stacktrace Sample:**
```
java.io.FileNotFoundException: File does not exist: /dir/dst-file
	at org.apache.hadoop.hdfs.DistributedFileSystem$29.doCall(DistributedFileSystem.java:1757)
	at org.apache.hadoop.hdfs.DistributedFileSystem$29.doCall(DistributedFileSystem.java:1750)
	at org.apache.hadoop.fs.FileSystemLinkResolver.resolve(FileSystemLinkResolver.java:81)
	at org.apache.hadoop.hdfs.DistributedFileSystem.getFileStatus(DistributedFileSystem.java:1765)
	at org.apache.hadoop.hdfs.server.namenode.TestDeleteRace_RestartInjected.testOpenRenameRace(TestDeleteRace_RestartInjected.java:678)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallabl
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestDeleteRace_RestartInjected.testOpenRenameRace**
   - Position: after_file_creation, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 110

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.net.ConnectException
	at java.net.PlainSocketImpl.socketConnect(Native Method)
```

**Raw Stacktrace Sample:**
```
java.net.ConnectException: Connection refused (Connection refused)
	at java.net.PlainSocketImpl.socketConnect(Native Method)
	at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:350)
	at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:204)
	at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:188)
	at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
	at java.net.Socket.connect(Socket.java:607)
	at sun.net.NetworkClient.doConnect(NetworkClient.java:175)
	at sun.net.www.http.HttpClient.openServer(HttpClient.java:465)
	at sun.net.www.http.HttpClient.openServer(HttpClient.java:560)
	at sun.net.www.http.HttpClient.<init>(HttpClient.java:244)
	at sun.net.www.http.HttpClient.New(HttpClient.java:341)
	at sun.net.www.http.HttpClient.New(HttpClient.java:359)
	at sun.net.www.protocol.http.HttpURLConnection.getNewHttpClient(HttpURLConnection.java:1243)
	at sun.net.www.protocol.http.HttpURLConnection.plainConnect0(H
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestObserverNode_RestartInjected.testFsckWithObserver**
   - Position: after_create_file, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 111

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.testTransferRbw(TestTransferRbw_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.testTransferRbw(TestTransferRbw_RestartInjected.java:173)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runL
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.testTransferRbw**
   - Position: after_get_old_rbw, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 112

**Test Executions:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.getReplica(TestTransferRbw_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.getReplica(TestTransferRbw_RestartInjected.java:69)
	at org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.getRbw(TestTransferRbw_RestartInjected.java:63)
	at org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.testTransferRbw(TestTransferRbw_RestartInjected.java:184)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.st
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestTransferRbw_RestartInjected.testTransferRbw**
   - Position: after_transfer_rbw, Target: datanode, Mode: GRACEFUL

---

## LOWER PRIORITY - Timeouts and Test Framework Issues

**Total Groups: 29**

### [ ] Group 1

**Test Executions:** 107

**Generalized Stacktrace:**
```
java.util.concurrent.TimeoutException
Timed out waiting for condition.
```

**Raw Stacktrace Sample:**
```
java.util.concurrent.TimeoutException: 
Timed out waiting for condition. 
Thread diagnostics:
Timestamp: 2026-01-04 07:32:58,834

"MarkedDeleteBlockScrubberThread" daemon prio=5 tid=175 timed_waiting
java.lang.Thread.State: TIMED_WAITING
        at java.lang.Thread.sleep(Native Method)
        at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager$MarkedDeleteBlockScrubber.run(BlockManager.java:4998)
        at java.lang.Thread.run(Thread.java:750)
"org.apache.hadoop.hdfs.server.namenode.FSNamesystem$LazyPersistFileScrubber@72ed9aad" daemon prio=5 tid=149 timed_waiting
java.lang.Thread.State: TIMED_WAITING
        at java.lang.Thread.sleep(Native Method)
        at org.apache.hadoop.hdfs.server.namenode.FSNamesystem$LazyPersistFileScrubber.run(FSNamesystem.java:4564)
        at java.lang.Thread.run(Thread.java:750)
"IPC Server handler 7 on default port 35153" daemon prio=5 tid=140 timed_waiting
java.lang.Thread.State: TIMED_WAITING
        at sun.misc.Unsafe.park(Native Method)

```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits**
   - Position: after_backup_start, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits**
   - Position: after_bn_checkpoint, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits**
   - Position: after_backup_restart, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 5

**Test Executions:** 31

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException)
	at org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker.checkSuperuserPrivilege(FSPermissionChecker.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_test_glob_access_denied_on_fc
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.fs.TestGlobPaths_RestartInjected.testGlobAccessDeniedOnFC(TestGlobPaths_RestartInjected.java:1561)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(Invo
```

**Sample Test Executions:**

1. **org.apache.hadoop.fs.TestGlobPaths_RestartInjected.testGlobAccessDeniedOnFC**
   - Position: after_test_glob_access_denied_on_fc, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.fs.TestGlobPaths_RestartInjected.testReservedHdfsPathsOnFS**
   - Position: after_test_reserved_hdfs_paths_on_fs, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.fs.TestGlobPaths_RestartInjected.testReservedHdfsPathsOnFC**
   - Position: after_test_reserved_hdfs_paths_on_fc, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 6

**Test Executions:** 30

**Generalized Stacktrace:**
```
Caused by: java.lang.Exception
	at org.apache.hadoop.hdfs.restart.HdfsClusterAdapter.restartDataNode(HdfsClusterAdapter.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_creation
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.TestRollingUpgrade_RestartInjected.testQuery(TestRollingUpgrade_RestartInjected.java:779)
	at org.apache.hadoop.hdfs.TestRollingUpgrade_RestartInjected.testQueryWithMultipleNN(TestRollingUpgrade_RestartInjected.java:766)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMeth
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.TestRollingUpgrade_RestartInjected.testQueryWithMultipleNN**
   - Position: after_cluster_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.TestRollingUpgrade_RestartInjected.testQueryWithMultipleNN**
   - Position: after_upgrade_prepare, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.TestDFSInotifyEventInputStreamKerberized_RestartInjected.testWithKerberizedCluster**
   - Position: after_tgt_relogin_mkdir, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 7

**Test Executions:** 29

**Generalized Stacktrace:**
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.MiniDFSCluster.waitClusterUp(MiniDFSCluster.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_namenode_shutdown_test
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestBlockTokenWithDFS_RestartInjected.doTestRead(TestBlockTokenWithDFS_RestartInjected.java:708)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestBlockTokenWithDFS_RestartInjected.testRead(TestBlockTokenWithDFS_RestartInjected.java:468)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlockTokenWithDFS_RestartInjected.testRead**
   - Position: after_namenode_shutdown_test, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyCheckpoints_RestartInjected.testNonPrimarySBNUploadFSImage**
   - Position: after_transition_to_standby, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestFSImage_RestartInjected.testHasNonEcBlockUsingStripedIDForLoadFile**
   - Position: before_first_restart, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 11

**Test Executions:** 23

**Generalized Stacktrace:**
```
org.junit.ComparisonFailure
	at org.junit.Assert.assertEquals(Assert.java)
```

**Raw Stacktrace Sample:**
```
org.junit.ComparisonFailure: expected:<[3 files and directories, 2 blocks = 5] total filesystem ob...> but was:<[0 files and directories, 0 blocks = 0] total filesystem ob...>
	at org.junit.Assert.assertEquals(Assert.java:117)
	at org.junit.Assert.assertEquals(Assert.java:146)
	at org.apache.hadoop.hdfs.server.namenode.TestMetaSave_RestartInjected.testMetaSave(TestMetaSave_RestartInjected.java:127)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.Inv
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestMetaSave_RestartInjected.testMetaSave**
   - Position: after_set_replication, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.tools.TestDFSHAAdminMiniCluster_RestartInjected.testFencer**
   - Position: after_failover_nn1_to_nn2, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.TestSafeMode_RestartInjected.testSafeModeExceptionText**
   - Position: after_safemode_enter, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 22

**Test Executions:** 7

**Generalized Stacktrace:**
```
java.util.concurrent.TimeoutException
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForDatanodeStatus(DFSTestUtil.java)
```

**Raw Stacktrace Sample:**
```
java.util.concurrent.TimeoutException: Timed out waiting for capacity. Live = 0 Expected = 3 Dead = 3 Expected = 0 Total capacity = 0 Expected = 268200361984 Vol Fails = 0 Expected = 2
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForDatanodeStatus(DFSTestUtil.java:763)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testDataNodeReconfigureWithVolumeFailures(TestDataNodeVolumeFailureReporting_RestartInjected.java:515)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(Framewo
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testDataNodeReconfigureWithVolumeFailures**
   - Position: after_volume_failures, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testDataNodeReconfigureWithVolumeFailures**
   - Position: after_first_reconfigure, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testVolFailureStatsPreservedOnNNRestart**
   - Position: after_volume_failures, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 23

**Test Executions:** 7

**Generalized Stacktrace:**
```
Caused by: java.io.FileNotFoundException
	at org.apache.hadoop.hdfs.server.namenode.INodeDirectory.valueOf(INodeDirectory.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_snapshot_s2
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage(TestFSImageWithSnapshot_RestartInjected.java:291)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.e
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage**
   - Position: after_snapshot_s2, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage**
   - Position: after_snapshot_s3, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage**
   - Position: after_snapshot_s4, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 25

**Test Executions:** 6

**Generalized Stacktrace:**
```
Caused by: java.net.ConnectException
	at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_file_creation
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testHANNRestartAfterSnapshotDeletion(TestSnapshotDeletion_RestartInjected.java:1570)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testHANNRestartAfterSnapshotDeletion**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testHANNRestartAfterSnapshotDeletion**
   - Position: after_snapshot_creation, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testHANNRestartAfterSnapshotDeletion**
   - Position: after_subdir_deletion, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 29

**Test Executions:** 5

**Generalized Stacktrace:**
```
java.util.concurrent.TimeoutException
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForReplication(DFSTestUtil.java)
```

**Raw Stacktrace Sample:**
```
java.util.concurrent.TimeoutException: Timed out waiting for replication. Needed replicas = 0 Cur needed replicas = 0 Replicas = 3 Cur replicas = 3 Racks = 1 Cur racks = 2 Domains = 0 Cur domains = 0
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForReplication(DFSTestUtil.java:596)
	at org.apache.hadoop.hdfs.DFSTestUtil.waitForReplication(DFSTestUtil.java:562)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected.testReplDueToNodeFailRespectsRackPolicy(TestBlocksWithNotEnoughRacks_RestartInjected.java:497)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCa
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected.testReplDueToNodeFailRespectsRackPolicy**
   - Position: after_cluster_build, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected.testReplDueToNodeFailRespectsRackPolicy**
   - Position: after_file_create, Target: datanode, Mode: CRASH

3. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected.testReduceReplFactorDueToRejoinRespectsRackPolicy**
   - Position: after_cluster_build, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 30

**Test Executions:** 5

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.hdfs.server.common.InconsistentFSStateException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.checkUpgrade(FSImage.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_upgrade_restart_qjm
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestDFSUpgradeWithHA_RestartInjected.testUpgradeWithJournalNodes(TestDFSUpgradeWithHA_RestartInjected.java:431)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestDFSUpgradeWithHA_RestartInjected.testUpgradeWithJournalNodes**
   - Position: after_upgrade_restart_qjm, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.tools.TestDFSAdminWithHA_RestartInjected.testUpgradeCommand**
   - Position: after_nn1_upgrade_start, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.namenode.ha.TestDFSUpgradeWithHA_RestartInjected.testFinalizeWithJournalNodes**
   - Position: after_upgrade_finalize, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 32

**Test Executions:** 4

**Generalized Stacktrace:**
```
Wanted but not invoked:
datanodeProtocolClientSideTranslatorPB.blockReport(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <any string>,
    <any org.apache.hadoop.hdfs.server.protocol.StorageBlockReport[]>,
    <any>
);
-> at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java)
However, there were exactly N interactions with this mock:
datanodeProtocolClientSideTranslatorPB.blockReceivedAndDeleted(
    DatanodeRegistration(HOST:PORT, datanodeUuid=UUID, infoPort=PORT, infoSecurePort=PORT, ipcPort=PORT, storageInfo=lv=N;cid=testClusterID;nsid=N;c=TIMESTAMP),
    "BP-N-HOST-TIMESTAMP",
    [DatanodeStorage[DS-UUID,DISK,NORMAL][blk_1073741825_1001, status: RECEIVED_BLOCK, delHint: null]]
);
-> at org.apache.hadoop.hdfs.server.datanode.IncrementalBlockReportManager.sendIBRs(IncrementalBlockReportManager.java)
datanodeProtocolClientSideTranslatorPB.close();
-> at org.apache.hadoop.io.IOUtils.cleanupWithLogger(IOUtils.java)
	at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
Wanted but not invoked:
datanodeProtocolClientSideTranslatorPB.blockReport(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <any string>,
    <any org.apache.hadoop.hdfs.server.protocol.StorageBlockReport[]>,
    <any>
);
-> at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java:194)

However, there were exactly 2 interactions with this mock:
datanodeProtocolClientSideTranslatorPB.blockReceivedAndDeleted(
    DatanodeRegistration(127.0.0.1:32821, datanodeUuid=c775eb46-84b7-4be1-8241-677ffc485530, infoPort=39119, infoSecurePort=0, ipcPort=34467, storageInfo=lv=-57;cid=testClusterID;nsid=804412778;c=1767515884857),
    "BP-1144147865-172.17.0.2-1767515884857",
    [DatanodeStorage[DS-9e12d419-b7a1-43fc-aff5-289f006de962,DISK,NORMAL][blk_1073741825_1001, status: RECEIVED_BLOCK, delHint: null]]
);
-> at org.apache.hadoop.hdfs.server.datanode.IncrementalBlockReportManager.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerFullBlockReport**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerFullBlockReport**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerFullBlockReport**
   - Position: after_add_fake_deletion, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 33

**Test Executions:** 4

**Generalized Stacktrace:**
```
org.mockito.exceptions.verification.TooFewActualInvocations
datanodeProtocolClientSideTranslatorPB.blockReceivedAndDeleted(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <any string>,
    <any org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks[]>
);
Wanted 2 times:
-> at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java)
But was 1 time:
-> at org.apache.hadoop.hdfs.server.datanode.IncrementalBlockReportManager.sendIBRs(IncrementalBlockReportManager.java)
	at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
org.mockito.exceptions.verification.TooFewActualInvocations: 

datanodeProtocolClientSideTranslatorPB.blockReceivedAndDeleted(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <any string>,
    <any org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks[]>
);
Wanted 2 times:
-> at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java:183)
But was 1 time:
-> at org.apache.hadoop.hdfs.server.datanode.IncrementalBlockReportManager.sendIBRs(IncrementalBlockReportManager.java:212)

	at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerBlockReport(TestTriggerBlockReport_RestartInjected.java:183)
	at org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerIncrementalBlockReport(TestTriggerBlockReport_RestartInjected.java:218)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at s
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerIncrementalBlockReport**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerIncrementalBlockReport**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestTriggerBlockReport_RestartInjected.testTriggerIncrementalBlockReport**
   - Position: after_add_fake_deletion, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 34

**Test Executions:** 4

**Generalized Stacktrace:**
```
Caused by: org.junit.ComparisonFailure
	at org.junit.Assert.assertEquals(Assert.java)
```

**Raw Stacktrace Sample:**
```
arrays first differed at element [0]; expected:<...t/data/dfs/data/data[1]> but was:<...t/data/dfs/data/data[3]>
	at org.junit.internal.ComparisonCriteria.arrayEquals(ComparisonCriteria.java:78)
	at org.junit.internal.ComparisonCriteria.arrayEquals(ComparisonCriteria.java:28)
	at org.junit.Assert.internalArrayEquals(Assert.java:534)
	at org.junit.Assert.assertArrayEquals(Assert.java:285)
	at org.junit.Assert.assertArrayEquals(Assert.java:300)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.checkFailuresAtDataNode(TestDataNodeVolumeFailureReporting_RestartInjected.java:878)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testDataNodeReconfigureWithVolumeFailures(TestDataNodeVolumeFailureReporting_RestartInjected.java:507)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMetho
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testDataNodeReconfigureWithVolumeFailures**
   - Position: after_additional_datanodes_start, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testVolFailureStatsPreservedOnNNRestart**
   - Position: after_additional_datanodes_start, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeFailureReporting_RestartInjected.testMultipleVolFailuresOnNode**
   - Position: after_first_file_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 35

**Test Executions:** 3

**Generalized Stacktrace:**
```
org.junit.runners.model.TestTimedOutException
	at java.lang.Thread.sleep(Native Method)
```

**Raw Stacktrace Sample:**
```
org.junit.runners.model.TestTimedOutException: test timed out after 300 seconds
	at java.lang.Thread.sleep(Native Method)
	at org.apache.hadoop.test.GenericTestUtils.waitFor(GenericTestUtils.java:452)
	at org.apache.hadoop.test.GenericTestUtils.waitFor(GenericTestUtils.java:421)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestLazyPersistLockedMemory_RestartInjected.waitForLockedBytesUsed(TestLazyPersistLockedMemory_RestartInjected.java:273)
	at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestLazyPersistLockedMemory_RestartInjected.testReleaseOnFileDeletion(TestLazyPersistLockedMemory_RestartInjected.java:138)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(Framew
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestLazyPersistLockedMemory_RestartInjected.testReleaseOnFileDeletion**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestLazyPersistLockedMemory_RestartInjected.testShortBlockFinalized**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.TestLazyPersistLockedMemory_RestartInjected.testWritePipelineFailure**
   - Position: after_write_hsync, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 38

**Test Executions:** 3

**Generalized Stacktrace:**
```
Caused by: java.net.BindException
	at sun.nio.ch.Net.bind0(Native Method)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_files_created
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testVolumeIteratorImpl(TestBlockScanner_RestartInjected.java:204)
	at org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testVolumeIteratorWithoutCaching(TestBlockScanner_RestartInjected.java:298)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestBlockScanner_RestartInjected.testVolumeIteratorWithoutCaching**
   - Position: after_files_created, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.datanode.TestRefreshNamenodes_RestartInjected.testRefreshNamenodes**
   - Position: after_add_namenode_4, Target: namenode, Mode: GRACEFUL

3. **org.apache.hadoop.hdfs.server.blockmanagement.TestBlockStatsMXBean_RestartInjected.testStorageTypeStatsWhenStorageFailed**
   - Position: after_cluster_ready, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 40

**Test Executions:** 2

**Generalized Stacktrace:**
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.FSEditLog.checkForGaps(FSEditLog.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.TestGenericJournalConf_RestartInjected.testDummyJournalManager(TestGenericJournalConf_RestartInjected.java:135)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMe
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestGenericJournalConf_RestartInjected.testDummyJournalManager**
   - Position: after_cluster_start, Target: namenode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.namenode.TestGenericJournalConf_RestartInjected.testDummyJournalManager**
   - Position: after_journal_verification, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 57

**Test Executions:** 2

**Generalized Stacktrace:**
```
java.util.concurrent.TimeoutException
	at org.apache.hadoop.hdfs.DFSTestUtil.waitReplication(DFSTestUtil.java)
```

**Raw Stacktrace Sample:**
```
java.util.concurrent.TimeoutException: Timed out waiting for file [/test0] to reach [2] replicas
	at org.apache.hadoop.hdfs.DFSTestUtil.waitReplication(DFSTestUtil.java:838)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestRBWBlockInvalidation_RestartInjected.testRWRInvalidation(TestRBWBlockInvalidation_RestartInjected.java:233)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.Par
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.blockmanagement.TestRBWBlockInvalidation_RestartInjected.testRWRInvalidation**
   - Position: after_initial_write_flush, Target: datanode, Mode: GRACEFUL

2. **org.apache.hadoop.hdfs.server.blockmanagement.TestOverReplicatedBlocks_RestartInjected.testProcesOverReplicateBlock**
   - Position: after_file_creation, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 60

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: KrbException
	at sun.security.krb5.internal.KDCRep.init(KDCRep.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_first_mkdir
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.TestDFSInotifyEventInputStreamKerberized_RestartInjected$1.run(TestDFSInotifyEventInputStreamKerberized_RestartInjected.java:132)
	at org.apache.hadoop.hdfs.TestDFSInotifyEventInputStreamKerberized_RestartInjected$1.run(TestDFSInotifyEventInputStreamKerberized_RestartInjected.java:116)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.Subject.doAs(Subject.java:422)
	at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1899)
	at org.apache.hadoop.hdfs.TestDFSInotifyEventInputStreamKerberized_RestartInjected.testWithKerberizedCluster(TestDFSInotifyEventInputStreamKerberized_RestartInjected.java:116)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.TestDFSInotifyEventInputStreamKerberized_RestartInjected.testWithKerberizedCluster**
   - Position: after_first_mkdir, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 71

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.MetaRecoveryContext.editLogLoaderPrompt(MetaRecoveryContext.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_delete_segment
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestBootstrapStandby_RestartInjected.testSharedEditsMissingLogs(TestBootstrapStandby_RestartInjected.java:254)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.Invok
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestBootstrapStandby_RestartInjected.testSharedEditsMissingLogs**
   - Position: after_delete_segment, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 75

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.util.DiskChecker$DiskErrorException
	at org.apache.hadoop.hdfs.server.datanode.checker.StorageLocationChecker.check(StorageLocationChecker.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_volume_failure
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeMetrics_RestartInjected.testVolumeMetricsWithVolumeDepartureArrival(TestDataNodeVolumeMetrics_RestartInjected.java:145)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestDataNodeVolumeMetrics_RestartInjected.testVolumeMetricsWithVolumeDepartureArrival**
   - Position: after_volume_failure, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 81

**Test Executions:** 1

**Generalized Stacktrace:**
```
org.junit.ComparisonFailure
```

**Raw Stacktrace Sample:**
```
org.junit.ComparisonFailure: expected:<...s=[Snapshot.s0(id=0)[]]> but was:<...s=[Snapshot.s0(id=0)[, Snapshot.s2(id=2)]]>	at org.junit.Assert.assertEquals(Assert.java:117)	at org.junit.Assert.assertEquals(Assert.java:146)	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotTestHelper.compareDumpedTreeInFile(SnapshotTestHelper.java:248)	at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotTestHelper.compareDumpedTreeInFile(SnapshotTestHelper.java:197)	at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.restartCluster(TestFSImageWithSnapshot_RestartInjected.java:951)	at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testDoubleRename(TestFSImageWithSnapshot_RestartInjected.java:923)	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)	at
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testDoubleRename**
   - Position: after_first_rename, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 85

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.metrics2.MetricsException
	at org.apache.hadoop.metrics2.lib.DefaultMetricsSystem.newSourceName(DefaultMetricsSystem.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_fair_queue_refresh
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.TestRefreshCallQueue_RestartInjected.testRefreshCallQueueWithFairCallQueue(TestRefreshCallQueue_RestartInjected.java:177)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.e
```

**Sample Test Executions:**

1. **org.apache.hadoop.TestRefreshCallQueue_RestartInjected.testRefreshCallQueueWithFairCallQueue**
   - Position: after_fair_queue_refresh, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 88

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.hdfs.server.common.IncorrectVersionException
	at org.apache.hadoop.hdfs.server.common.StorageInfo.setLayoutVersion(StorageInfo.java)
```

**Raw Stacktrace Sample:**
```
java.lang.Exception: Unexpected exception, expected<java.lang.IllegalArgumentException> but was<org.restarttest.core.RestartException>
	at org.junit.internal.runners.statements.ExpectException.evaluate(ExpectException.java:30)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.TestRollingUpgradeDowngrade_RestartInjected.testRejectNewFsImage**
   - Position: after_storage_modification, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 89

**Test Executions:** 1

**Generalized Stacktrace:**
```
org.mockito.exceptions.verification.TooManyActualInvocations
datanodeProtocolClientSideTranslatorPB.sendHeartbeat(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <Capturing argument>,
    <any long>,
    <any long>,
    <any integer>,
    <any integer>,
    <any integer>,
    <any>,
    <any boolean>,
    <any org.apache.hadoop.hdfs.server.protocol.SlowPeerReports>,
    <any org.apache.hadoop.hdfs.server.protocol.SlowDiskReports>
);
Wanted 1 time:
-> at org.apache.hadoop.hdfs.server.datanode.TestStorageReport_RestartInjected.testStorageReportHasStorageTypeAndState(TestStorageReport_RestartInjected.java)
But was 4 times:
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java)
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java)
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java)
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java)
	at org.apache.hadoop.hdfs.server.datanode.TestStorageReport_RestartInjected.testStorageReportHasStorageTypeAndState(TestStorageReport_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
org.mockito.exceptions.verification.TooManyActualInvocations: 

datanodeProtocolClientSideTranslatorPB.sendHeartbeat(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <Capturing argument>,
    <any long>,
    <any long>,
    <any integer>,
    <any integer>,
    <any integer>,
    <any>,
    <any boolean>,
    <any org.apache.hadoop.hdfs.server.protocol.SlowPeerReports>,
    <any org.apache.hadoop.hdfs.server.protocol.SlowDiskReports>
);
Wanted 1 time:
-> at org.apache.hadoop.hdfs.server.datanode.TestStorageReport_RestartInjected.testStorageReportHasStorageTypeAndState(TestStorageReport_RestartInjected.java:124)
But was 4 times:
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java:547)
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java:547)
-> at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.sendHeartBeat(BPServiceActor.java:547)
-> at org.apache.hadoop.hdfs.server.data
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestStorageReport_RestartInjected.testStorageReportHasStorageTypeAndState**
   - Position: after_spy_setup, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 91

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.hdfs.server.common.InconsistentFSStateException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.recoverStorageDirs(FSImage.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_shutdown_and_remove
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestInitializeSharedEdits_RestartInjected.testInitializeSharedEdits(TestInitializeSharedEdits_RestartInjected.java:175)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.st
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestInitializeSharedEdits_RestartInjected.testInitializeSharedEdits**
   - Position: after_shutdown_and_remove, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 96

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.StandbyException
	at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java)
```

**Raw Stacktrace Sample:**
```
java.lang.Exception: Unexpected exception, expected<java.util.concurrent.TimeoutException> but was<org.apache.hadoop.ipc.StandbyException>
	at org.junit.internal.runners.statements.ExpectException.evaluate(ExpectException.java:30)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.ha.TestConsistentReadsObserver_RestartInjected.testAutoMsyncLongPeriod**
   - Position: after_mkdir_testpath, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 98

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: java.io.IOException
	at org.apache.hadoop.hdfs.server.namenode.FSImage.recoverStorageDirs(FSImage.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_import_checkpoint
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.namenode.TestCheckpoint_RestartInjected.testImportCheckpoint(TestCheckpoint_RestartInjected.java:1124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.namenode.TestCheckpoint_RestartInjected.testImportCheckpoint**
   - Position: after_import_checkpoint, Target: namenode, Mode: GRACEFUL

---

### [ ] Group 105

**Test Executions:** 1

**Generalized Stacktrace:**
```
Caused by: java.lang.ArrayIndexOutOfBoundsException
	at org.apache.hadoop.hdfs.MiniDFSCluster.setDataNodeStorageCapacities(MiniDFSCluster.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_add_datanode
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.hdfs.server.balancer.TestBalancer_RestartInjected.testBalancerDuringUpgrade(TestBalancer_RestartInjected.java:1595)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(Invo
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.balancer.TestBalancer_RestartInjected.testBalancerDuringUpgrade**
   - Position: after_add_datanode, Target: datanode, Mode: GRACEFUL

---

### [ ] Group 108

**Test Executions:** 1

**Generalized Stacktrace:**
```
Wanted but not invoked:
datanodeProtocolClientSideTranslatorPB.blockReceivedAndDeleted(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <any string>,
    <any org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks[]>
);
-> at org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR_RestartInjected.testBlockCounters(TestBlockCountersInPendingIBR_RestartInjected.java)
However, there was exactly N interaction with this mock:
datanodeProtocolClientSideTranslatorPB.close();
-> at org.apache.hadoop.io.IOUtils.cleanupWithLogger(IOUtils.java)
	at org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR_RestartInjected.testBlockCounters(TestBlockCountersInPendingIBR_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
Wanted but not invoked:
datanodeProtocolClientSideTranslatorPB.blockReceivedAndDeleted(
    <any org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration>,
    <any string>,
    <any org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks[]>
);
-> at org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR_RestartInjected.testBlockCounters(TestBlockCountersInPendingIBR_RestartInjected.java:133)

However, there was exactly 1 interaction with this mock:
datanodeProtocolClientSideTranslatorPB.close();
-> at org.apache.hadoop.io.IOUtils.cleanupWithLogger(IOUtils.java:285)


	at org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR_RestartInjected.testBlockCounters(TestBlockCountersInPendingIBR_RestartInjected.java:133)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:4
```

**Sample Test Executions:**

1. **org.apache.hadoop.hdfs.server.datanode.TestBlockCountersInPendingIBR_RestartInjected.testBlockCounters**
   - Position: after_blocks_added, Target: datanode, Mode: GRACEFUL

---
