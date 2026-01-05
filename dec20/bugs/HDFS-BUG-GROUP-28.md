# HDFS-BUG-GROUP-28: RWR Replicas Break hflush/hsync Visibility Contract After Datanode Restart

## Summary

After a datanode restart, data that was successfully `hflush()`'d or `hsync()`'d becomes unreadable because `ReplicaWaitingToBeRecovered.getVisibleLength()` unconditionally returns `-1`. This violates the HDFS `hflush()` contract defined in `Syncable.java`: "After the return of this call, new readers will see the data."

## Bug Classification

**Type:** Production Code Bug / Design Limitation
**Severity:** High
**Component:** HDFS DataNode - Replica State Management

## Affected Versions

- Hadoop 3.3.5 (tested)
- Likely affects all versions with the same RWR implementation

## The hflush/hsync Contract

From `Syncable.java` (lines 35-39):
```java
/** Flush out the data in client's user buffer. After the return of
 * this call, new readers will see the data.
 * @throws IOException if any error occurs
 */
void hflush() throws IOException;
```

The contract is clear: **"new readers will see the data"** after hflush returns.

## Root Cause Analysis

### What Happens

1. **Before datanode restart:**
   - Block is in RBW (Replica Being Written) state
   - `ReplicaBeingWritten.getVisibleLength()` returns `bytesAcked`
   - New readers CAN read the hflush'd/hsync'd data

2. **After datanode restart:**
   - RBW replicas are loaded as RWR (Replica Waiting to be Recovered)
   - `ReplicaWaitingToBeRecovered.getVisibleLength()` returns `-1`
   - New readers CANNOT read the data - throws `CannotObtainBlockLengthException`

This is a **visibility regression** - data that was successfully synced becomes invisible after restart.

### The Intentional Design Choice

The comment in `ReplicaWaitingToBeRecovered.java` (lines 27-35) explains:
```
A replica waiting to be recovered does not provision read nor
participates in any pipeline recovery. It will become outdated if its
client continues to write or be recovered as a result of lease recovery.
```

This design is **intentionally conservative** for safety reasons:

```
Timeline:
Writer: write(data1) → hflush() → [acked] → write(data2) → [DN CRASHES]
                          ↑                       ↑
                     data1 is safe         data2 might be corrupted
```

After restart, `getBytesOnDisk()` returns `len(data1 + data2)`, but only `data1` was hflush'd. The datanode doesn't know which bytes were actually acknowledged, so it conservatively returns `-1` to avoid exposing potentially corrupted data.

### Why This Is Still a Bug

The safety rationale is valid, but the implementation **violates the hflush contract**:

| Scenario | Behavior | Contract Violation? |
|----------|----------|---------------------|
| Replication=3, one DN restarts | Reads from other DNs | No |
| Replication=3, all DNs restart | **Cannot read data** | **YES** |
| Replication=1, DN restarts | **Cannot read data** | **YES** |

The contract says "new readers will see the data" - it doesn't say "unless datanodes restart."

## Buggy Code Location

**File:** `hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/ReplicaWaitingToBeRecovered.java`

**Lines 74-82:**
```java
@Override //ReplicaInfo
public long getVisibleLength() {
  return -1;  //no bytes are visible  <-- Returns -1 unconditionally
}

@Override
public long getBytesOnDisk() {
  return getNumBytes();  // Data IS on disk!
}
```

The data IS on disk (`getBytesOnDisk()` returns the actual value), but it's declared as "not visible."

## Evidence

Debug logging confirmed the state transition:
```
replica state=RWR, replica class=ReplicaWaitingToBeRecovered
getReplicaVisibleLength returned -1
```

Meanwhile, `getBytesOnDisk()` returns the actual data size, proving the data is intact.

## Reproduction Steps

1. Create a file and write data
2. Call `hsync()` to ensure data is durable and visible
3. Verify new readers can see the data (they can)
4. Restart the datanode
5. Try to open and read the file
6. **Result:** `CannotObtainBlockLengthException` - data that was visible is now invisible

**Test command:**
```bash
cd /home/shuai/xlab/restart_testing/hdfs/hadoop-hdfs-project/hadoop-hdfs
mvn surefire:test -Dtest=org.apache.hadoop.hdfs.server.datanode.TestBlockRecovery2_RestartInjected#testRaceBetweenReplicaRecoveryAndFinalizeBlock \
  -Drestart.position=after_file_creation_hsync \
  -Drestart.target=datanode \
  -Drestart.mode=GRACEFUL
```

## Potential Fixes

### Option 1: Return bytes on disk (Simple but potentially unsafe)

```java
@Override
public long getVisibleLength() {
  return getBytesOnDisk();
}
```

**Risk:** May expose bytes that were written after the last hflush but before crash (potentially corrupted).

### Option 2: Persist visible length during hflush (Recommended)

The proper fix requires HDFS to persist the "last acknowledged visible length" to disk during hflush/hsync:

```java
// During hflush, persist bytesAcked to a metadata file
// After restart, load this value

@Override
public long getVisibleLength() {
  return lastPersistedVisibleLength;  // Safe: only returns acked bytes
}
```

This would require:
1. During `hflush()`: Write `bytesAcked` to a persistent metadata file (e.g., `.visible` file alongside block file)
2. During datanode restart: Load `lastPersistedVisibleLength` from the metadata file
3. `ReplicaWaitingToBeRecovered.getVisibleLength()` returns this persisted value

### Option 3: Checksum-based validation

```java
@Override
public long getVisibleLength() {
  // Return only bytes that have valid checksums
  return getChecksumValidatedLength();
}
```

This leverages the existing checksum mechanism to determine how much data is safely readable.

### Option 4: Throw RetriableException (Client-side workaround)

```java
// In DataNode.getReplicaVisibleLength()
if (visibleLength < 0) {
  throw new RetriableException(
      "Replica is waiting to be recovered. Try again later.");
}
```

This doesn't fix the underlying issue but allows clients to retry until lease recovery completes.

## Impact

- **Data availability:** Files under construction become temporarily unreadable after datanode restart
- **Use cases affected:**
  - Log tailing applications
  - Streaming reads
  - Any application reading files being written
  - HBase WAL reads
  - Kafka HDFS connector
- **Severity increased with:** Lower replication factors

## Related Classes

| Class | Behavior |
|-------|----------|
| `ReplicaBeingWritten.java` | Returns `bytesAcked` for visible length (correct) |
| `FinalizedReplica.java` | Returns `numBytes` for visible length (correct) |
| `ReplicaWaitingToBeRecovered.java` | Returns `-1` for visible length (**problematic**) |
| `DFSInputStream.java:readBlockLength()` | Throws `CannotObtainBlockLengthException` when visible length unavailable |

## Test Cases Affected

All tests in Group 28 that involve reading files after datanode restart:

1. `TestBlockRecovery2_RestartInjected.testRaceBetweenReplicaRecoveryAndFinalizeBlock`
   - Position: after_file_creation_hsync, Target: datanode, Mode: GRACEFUL

2. `TestFileAppend_RestartInjected.testSimpleFlush`
   - Position: after_second_flush, Target: datanode, Mode: GRACEFUL

3. `TestReplicaCachingGetSpaceUsed_RestartInjected.testReplicaCachingGetSpaceUsedByRBWReplica`
   - Position: after_rbw_replica_created, Target: datanode, Mode: GRACEFUL

## Discussion

This bug represents a fundamental tension between **safety** and **availability** in HDFS:

- **Current design (Safety first):** Don't serve any data from RWR replicas because we can't be sure which bytes are valid
- **Alternative design (Availability first):** Serve data from RWR replicas based on checksums or persisted metadata

The current design relies on **replication** to maintain availability during single-datanode failures. However, this breaks down when:
1. Replication factor is 1
2. All replicas' datanodes restart simultaneously
3. Network partitions isolate all replicas

A robust implementation should persist the visible length during hflush/hsync, allowing RWR replicas to serve at least the data that was acknowledged before the crash.
