# FP-GROUP-69: BlockMissingException - RWR Replicas with Stale Generation Stamps

## Summary

**Verdict**: FALSE POSITIVE

The `BlockMissingException` occurs because the restart framework injects an additional NameNode restart at `after_namenode_restart` position, which disrupts the test's carefully orchestrated restart sequence designed to test HDFS-4799 (RWR replica invalidation). After the additional restart, the DataNode with old generation stamp replicas is started first, but HDFS correctly refuses to serve data from these stale replicas, causing the BlockMissingException.

## Test Information

- **Test Class**: `org.apache.hadoop.hdfs.server.blockmanagement.TestRBWBlockInvalidation_RestartInjected`
- **Test Method**: `testRWRInvalidation`
- **Restart Position**: `after_namenode_restart`
- **Restart Target**: `namenode`
- **Restart Mode**: `GRACEFUL`

## Stack Trace

```
org.apache.hadoop.hdfs.BlockMissingException: Could not obtain block: BP-xxx:blk_1073741825_1014
file=/test0 No live nodes contain current block
Block locations: DatanodeInfoWithStorage[127.0.0.1:42305,...]
Dead nodes: DatanodeInfoWithStorage[127.0.0.1:42305,...]
    at org.apache.hadoop.hdfs.DFSInputStream.refetchLocations(DFSInputStream.java:968)
    at org.apache.hadoop.hdfs.DFSInputStream.chooseDataNode(DFSInputStream.java:951)
    at org.apache.hadoop.hdfs.DFSInputStream.blockSeekTo(DFSInputStream.java:637)
    at org.apache.hadoop.hdfs.DFSInputStream.readWithStrategy(DFSInputStream.java:845)
```

## Root Cause Analysis

### Test Purpose (HDFS-4799)

This test validates that when HDFS has both old and new generation stamp replicas after a restart, the system correctly invalidates the stale (old gen stamp) replicas instead of the valid ones.

### Original Test Flow

1. Create 10 files with replication factor 2 (both DataNodes have replicas)
2. Stop DN0 - this DN now has replicas with "old" generation stamp (1001)
3. Write more data and flush - this bumps the generation stamp to 1014
4. Set replication to 1 and close files - only DN1 has the new gen stamp data
5. Stop DN1 (the only remaining DataNode)
6. Restart NameNode (test's own restart)
7. Restart DN0 (old gen stamp 1001) first
8. Restart DN1 (new gen stamp 1014)
9. Trigger invalidation to remove old gen stamp replicas
10. Read files and verify data integrity

### What Happens with Restart Injection

The restart framework injects an additional NameNode restart at `after_namenode_restart` position (line 288-293 in the test), which occurs:

1. AFTER the test's own NameNode restart (line 286)
2. AFTER both DataNodes have been stopped (lines 285, 191)
3. BEFORE either DataNode is restarted (lines 296, 300)

The injection creates this problematic sequence:

```
Test stops DN0 (old gen stamp 1001)
Test writes more data, bumping gen stamp to 1014
Test stops DN1 (new gen stamp 1014)
Test restarts NameNode
>>> Framework injects another NameNode restart <<<
Test restarts DN0 (old gen stamp 1001) first
Test tries to read...
```

### Generation Stamp Mismatch

When DN0 is restarted first after the additional NameNode restart:

1. DN0 sends a block report with RWR replicas having generation stamp 1001
2. NameNode's metadata (persisted in editlogs/fsimage) shows blocks with generation stamp 1014
3. The client tries to read file `/test0`
4. DataNode refuses to serve the block:

```
opReadBlock blk_1073741825_1014 received exception java.io.IOException:
Replica gen stamp < block genstamp, block=blk_1073741825_1014,
replica=ReplicaWaitingToBeRecovered, blk_1073741825_1001, RWR
```

5. HDFS correctly refuses to serve data from stale replicas
6. After exhausting retries, client throws `BlockMissingException`

## Why This Is a False Positive

1. **Correct HDFS Behavior**: HDFS correctly refuses to serve data from replicas with outdated generation stamps. This is a critical data integrity protection mechanism.

2. **Test Sequence Disruption**: The test carefully orchestrates when each DataNode is restarted to validate HDFS-4799 behavior. The framework's additional restart disrupts this sequence, putting the system in an untested state.

3. **Incomplete State**: The additional restart occurs when:
   - Both DataNodes are stopped
   - The only available replicas (on DN0) have stale generation stamps
   - The valid replicas (on DN1) haven't been reported yet

4. **Production Scenario Validity**: In production:
   - A NameNode restart wouldn't randomly occur multiple times in quick succession
   - If only stale replicas are available, HDFS would wait for other DataNodes to report
   - Once all DataNodes report, HDFS would have both old and new gen stamp replicas and correctly invalidate the stale ones

## Evidence from Test Logs

```
# Test's own NameNode restart
059 [main] INFO  TestRBWBlockInvalidation - =========================== restarting cluster
199 [Listener] INFO  MiniDFSCluster - Restarted the namenode

# Framework's additional NameNode restart
312 [Listener] INFO  HdfsClusterAdapter - Restarting NameNode 0 with mode GRACEFUL
454 [Listener] INFO  MiniDFSCluster - Restarted the namenode
665 [Listener] INFO  HdfsClusterAdapter - NameNode 0 restarted successfully

# DataNode refuses stale replica
709 [DataXceiver] INFO  DataNode - opReadBlock blk_1073741825_1014 received exception
java.io.IOException: Replica gen stamp < block genstamp,
block=blk_1073741825_1014, replica=blk_1073741825_1001, RWR

# Client retries and eventually fails
710 [Listener] WARN  DFSClient - No live nodes contain block blk_1073741825_1014
930 [Listener] WARN  DFSClient - Throwing a BlockMissingException
```

## Conclusion

This failure is a **FALSE POSITIVE** because:

1. The restart framework injects a restart at an inappropriate position that disrupts a carefully designed test sequence
2. HDFS correctly rejects reads from replicas with stale generation stamps - this is expected data integrity protection
3. The test is specifically designed to test HDFS-4799 behavior with controlled restart ordering
4. The additional restart creates an artificial scenario where only stale replicas are available
5. In production, all DataNodes would eventually report and HDFS would correctly handle the old gen stamp replicas

The proper approach would be to skip restart injection at `after_namenode_restart` position for tests that have their own restart logic, or to wait for all DataNodes to come online before asserting read correctness.
