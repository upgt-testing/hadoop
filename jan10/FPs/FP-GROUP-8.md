# FP-GROUP-8: IOException in DataStreamer.handleBadDatanode

## Summary
FALSE POSITIVE - The restart framework does not use HDFS's built-in DataNode restart notification mechanism (OOB messages). Without the OOB message, the client cannot distinguish between a restarting DataNode and a permanently failed one. Combined with a single-DataNode test configuration, recovery is impossible.

## Error Details
```
java.io.IOException: All datanodes [DatanodeInfoWithStorage[127.0.0.1:46337,DS-400acae3-f4c7-4699-9d69-c88a92baee63,DISK]] are bad. Aborting...
	at org.apache.hadoop.hdfs.DataStreamer.handleBadDatanode(DataStreamer.java:1609)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipelineInternal(DataStreamer.java:1543)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipelineForAppendOrRecovery(DataStreamer.java:1529)
	at org.apache.hadoop.hdfs.DataStreamer.processDatanodeOrExternalError(DataStreamer.java:1305)
	at org.apache.hadoop.hdfs.DataStreamer.run(DataStreamer.java:668)
```

## Test Reproduction
- **Test**: `TestHSync_RestartInjected#testHSyncWithAppend`
- **Position**: `after_first_hsync`
- **Target**: `datanode`
- **Mode**: `GRACEFUL`
- **Index**: `0`

## Root Cause Analysis

### HDFS's Built-in DataNode Restart Mechanism

HDFS has a proper mechanism for handling DataNode restarts during active writes:

1. **OOB (Out-of-Band) Message**: When a DataNode shuts down for restart/upgrade, it sends an OOB message to all connected clients indicating it's restarting.

2. **Client-side handling**: When the client (DataStreamer) receives an OOB restart message, it:
   - Marks the node as "restarting" (not "bad")
   - Waits up to `dfs.client.datanode.restart.timeout` (default 30 seconds) for the node to come back
   - Retries the pipeline setup

3. **Code path** (`DataStreamer.java:1087-1110`):
   ```java
   boolean shouldWaitForRestart(int index) {
     // Only one node in the pipeline.
     if (nodes.length == 1) {
       return true;  // Will wait for single-node restart
     }
     // For multi-node pipelines, only wait for local nodes
     ...
   }
   ```

### Why the Restart Framework Doesn't Trigger This Mechanism

The OOB message is **only sent** when `shutdownForUpgrade=true`:

**DataXceiverServer.java:346-363**:
```java
public void sendOOBToPeers() {
  lock.lock();
  try {
    if (!datanode.shutdownForUpgrade) {
      return;  // <-- Early return if not shutdown for upgrade
    }
    for (Peer p : peers.keySet()) {
      try {
        peersXceiver.get(p).sendOOB();  // Send OOB message
      } catch ...
    }
  } finally {
    lock.unlock();
  }
}
```

The restart framework uses `MiniDFSCluster.stopDataNode()` which calls:
```java
public synchronized DataNodeProperties stopDataNode(int i) {
  ...
  dn.shutdown();  // Direct shutdown, not shutdownDatanode(true)
  ...
}
```

This does NOT set `shutdownForUpgrade=true`, so no OOB message is sent.

### The Correct Shutdown Path for Restarts

The proper way to shutdown a DataNode for restart is:
```java
datanode.shutdownDatanode(true);  // forUpgrade=true triggers OOB
```

**DataNode.java:3571-3581**:
```java
public synchronized void shutdownDatanode(boolean forUpgrade) throws IOException {
  ...
  shutdownForUpgrade = forUpgrade;  // This enables OOB messages
  ...
}
```

### Why This Leads to "All datanodes are bad"

Without the OOB message:
1. The client's ResponseProcessor thread encounters a connection error
2. The DataStreamer marks the node as "bad" (not "restarting")
3. `handleBadDatanode()` is called with `nodes.length == 1`
4. Since there's only one node and it's bad, the write aborts

**DataStreamer.java:1604-1611**:
```java
boolean handleBadDatanode() {
  final int badNodeIndex = errorState.getBadNodeIndex();
  if (badNodeIndex >= 0) {
    if (nodes.length <= 1) {
      lastException.set(new IOException("All datanodes "
          + Arrays.toString(nodes) + " are bad. Aborting..."));
      streamerClosed = true;
      return false;
    }
    ...
  }
}
```

## Why This Is a False Positive

1. **HDFS has proper restart handling**: The OOB mechanism is designed exactly for this scenario - notifying clients that a DataNode is restarting so they can wait for it.

2. **Restart framework doesn't use the proper API**: The framework should call `shutdownDatanode(true)` instead of `shutdown()` directly to trigger the OOB message.

3. **Test configuration limitation**: The test uses only 1 DataNode, making recovery impossible without the OOB mechanism. With the OOB message, the client would wait 30 seconds for the DataNode to return.

4. **Not a bug in HDFS**: HDFS correctly:
   - Sends OOB messages when `shutdownForUpgrade=true`
   - Waits for restarting nodes when OOB is received
   - Aborts writes when all nodes are permanently bad

## Recommendation

The restart framework should be modified to:
1. Call `datanode.shutdownDatanode(true)` instead of `shutdown()` for graceful restarts
2. Or directly set `shutdownForUpgrade=true` before calling shutdown
3. This will enable HDFS's built-in restart recovery mechanism

## Affected Tests (19 failures)

All 19 failures in this group follow the same pattern:
- DataNode restart during an active write operation
- No OOB message sent to client
- Client treats DataNode as permanently failed
- Single-DataNode configuration prevents recovery
