# FP-GROUP-19: CannotObtainBlockLengthException

## Summary

**Verdict**: FALSE POSITIVE

The `CannotObtainBlockLengthException` is expected HDFS behavior when reading a file immediately after a DataNode restart while the file is still being written. The restart framework injects a DataNode restart during an active write operation, and HDFS correctly puts the block in RWR (Replica Waiting to be Recovered) state, which intentionally returns no visible bytes until lease recovery completes.

## Test Information

- **Test Class**: `org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery2_RestartInjected`
- **Test Method**: `testRaceBetweenReplicaRecoveryAndFinalizeBlock`
- **Restart Position**: `after_file_creation_hsync`
- **Restart Target**: `datanode`
- **Restart Mode**: `GRACEFUL`

## Stack Trace

```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException: Cannot obtain block length for LocatedBlock{...}
    at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java:414)
    at org.apache.hadoop.hdfs.DFSInputStream.getLastBlockLength(DFSInputStream.java:323)
    at org.apache.hadoop.hdfs.DFSInputStream.openInfo(DFSInputStream.java:243)
    at org.apache.hadoop.hdfs.DFSInputStream.<init>(DFSInputStream.java:213)
    at org.apache.hadoop.hdfs.DFSClient.openInternal(DFSClient.java:1084)
    at org.apache.hadoop.hdfs.DFSClient.open(DFSClient.java:1047)
```

## Root Cause Analysis

### Test Flow

1. Test creates a file `/test` and writes "data" to it
2. Test calls `hsync()` which flushes data to the DataNode but doesn't close/finalize the file
3. Block is in **RBW (Replica Being Written)** state
4. **Restart framework injects DataNode restart at `after_file_creation_hsync` position**
5. After DataNode restart, blocks in the `rbw` directory are loaded as **ReplicaWaitingToBeRecovered (RWR)** state
6. Test immediately calls `DFSTestUtil.getAllBlocks(fs.open(path))` to read the file
7. `DFSInputStream.readBlockLength()` calls `getReplicaVisibleLength()` on the DataNode
8. RWR replica returns `-1` (no bytes visible), causing `CannotObtainBlockLengthException`

### HDFS Design: ReplicaWaitingToBeRecovered State

From `ReplicaWaitingToBeRecovered.java`:

```java
/**
 * This class represents a replica that is waiting to be recovered.
 * After a datanode restart, any replica in "rbw" directory is loaded
 * as a replica waiting to be recovered.
 * A replica waiting to be recovered does not provision read nor
 * participates in any pipeline recovery.
 */
public class ReplicaWaitingToBeRecovered extends LocalReplica {

    @Override //ReplicaInfo
    public long getVisibleLength() {
        return -1;  //no bytes are visible
    }
}
```

This is **intentional HDFS behavior**:
- RWR replicas do not provision read operations
- They must go through lease recovery first
- Only after lease recovery finalizes the block can it be read

### DFSInputStream.readBlockLength() Behavior

```java
// DFSInputStream.java
private long readBlockLength(LocatedBlock locatedblock) throws IOException {
    // ...
    while (nodeList.size() > 0) {
        DatanodeInfo datanode = nodeList.pop();
        // ...
        final long n = cdp.getReplicaVisibleLength(locatedblock.getBlock());
        if (n >= 0) {
            return n;  // Valid length found
        }
        // If n < 0, continue trying other datanodes
    }

    // All datanodes returned -1 or failed
    throw new CannotObtainBlockLengthException(locatedblock, src);
}
```

When all datanodes return `-1` for `getReplicaVisibleLength()`, the exception is thrown.

## Why This Is a False Positive

1. **Expected State Transition**: After a DataNode restart during an active write, HDFS correctly transitions RBW blocks to RWR state.

2. **Intentional API Behavior**: The `-1` return from `getVisibleLength()` is documented and intentional - RWR replicas should not provision reads.

3. **Lease Recovery Required**: In production, the proper flow is:
   - File writer either continues writing or crashes
   - Lease expires after timeout
   - NameNode initiates lease recovery
   - Block is finalized and becomes readable
   - Client retries and succeeds

4. **Test Timing Issue**: The test artificially tries to read the file immediately after DataNode restart, skipping the lease recovery process.

## Production Behavior

In a production scenario with proper client behavior:
- If the writer crashes, lease expires after soft/hard lease timeout (60s/1h by default)
- NameNode initiates lease recovery which finalizes the block
- Any subsequent reads will succeed after lease recovery completes
- If the writer is still alive, it can continue writing and eventually close the file

## Evidence from Test Logs

```
# Block report shows block exists after restart (1 block)
614 [Block report processor] INFO BlockStateChange - BLOCK* processReport ... blocks: 1

# DataNode restart completed successfully
641 [Listener at localhost/45411] INFO restart.HdfsClusterAdapter - DataNode 0 restarted successfully
646 [Listener at localhost/45411] INFO core.RestartExecutor - === RESTART POINT COMPLETED: after_file_creation_hsync ===

# File open attempted (and failed due to RWR state)
651 [IPC Server handler] INFO FSNamesystem.audit - allowed=true ... cmd=open src=/test
```

## Conclusion

This failure is a **FALSE POSITIVE** caused by improper restart timing. The restart is injected during an active write operation, and the test immediately tries to read the file without waiting for lease recovery. HDFS correctly rejects reads from RWR replicas, and this is expected behavior, not a bug.

The proper test approach would be to either:
1. Wait for lease recovery to complete before attempting to read
2. Accept that reads will fail during the RWR state
3. Use a different restart position (after file is closed)
