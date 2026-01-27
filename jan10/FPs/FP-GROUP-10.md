# FP-GROUP-10: IOException in DataStreamer.findNewDatanode

## Summary
FALSE POSITIVE - The restart injection creates an excessive DataNode failure scenario that exceeds the test's designed fault tolerance. The test explicitly stops 2 DataNodes as part of its test scenario, and when the restart framework injects an additional restart of a 3rd DataNode, HDFS correctly fails because there aren't enough available nodes to maintain the write pipeline.

## Error Details
```
java.io.IOException: Failed to replace a bad datanode on the existing pipeline due to no more good datanodes being available to try. (Nodes: current=[DatanodeInfoWithStorage[127.0.0.1:43159,...], DatanodeInfoWithStorage[127.0.0.1:41937,...]], original=[DatanodeInfoWithStorage[127.0.0.1:43159,...], DatanodeInfoWithStorage[127.0.0.1:41937,...]]).
	at org.apache.hadoop.hdfs.DataStreamer.findNewDatanode(DataStreamer.java:1352)
	at org.apache.hadoop.hdfs.DataStreamer.addDatanode2ExistingPipeline(DataStreamer.java:1420)
	at org.apache.hadoop.hdfs.DataStreamer.handleDatanodeReplacement(DataStreamer.java:1646)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipelineInternal(DataStreamer.java:1547)
	at org.apache.hadoop.hdfs.DataStreamer.setupPipelineForAppendOrRecovery(DataStreamer.java:1529)
	at org.apache.hadoop.hdfs.DataStreamer.processDatanodeOrExternalError(DataStreamer.java:1305)
	at org.apache.hadoop.hdfs.DataStreamer.run(DataStreamer.java:668)
```

## Test Configuration
- **Test Class**: `TestPipelinesFailover_RestartInjected`
- **Test Method**: `testWriteOverCrashFailoverWithDnFail` (and similar `WithDnFail` tests)
- **Restart Position**: `after_third_hflush_dnfail` (and similar positions)
- **Restart Target**: datanode (index 2)
- **Restart Mode**: GRACEFUL
- **Cluster Configuration**: 5 DataNodes, 3 NameNodes, default replication factor (3)

## Test Flow Analysis

The test `doTestWriteOverFailoverWithDnFail` is designed to test pipeline recovery during HA failover with DataNode failures:

1. **Initial Setup**: 5 DataNodes available (DN0, DN1, DN2, DN3, DN4)
2. **Line 331**: Test explicitly stops DN0 → 4 DataNodes remain
3. **Line 362**: Test explicitly stops DN1 → 3 DataNodes remain (DN2, DN3, DN4)
4. **Line 372**: Third hflush with only DN2, DN3, DN4 available
5. **Restart Injection**: Framework restarts DN2 → Only DN3 and DN4 available

## Why This Is a False Positive

### 1. Test Design Assumptions
The test is specifically designed to test pipeline recovery with controlled DataNode failures:
- The test stops DN0 and DN1 deliberately to simulate specific failure scenarios
- The test expects to continue writing with the remaining 3 DataNodes (DN2, DN3, DN4)
- Default replication factor is 3, which requires at least 3 available DataNodes

### 2. Restart Creates Unsupportable Scenario
When the restart framework injects a restart of DN2:
- DN0: Stopped by test (line 331)
- DN1: Stopped by test (line 362)
- DN2: Being restarted by framework
- DN3: Available
- DN4: Available

This means only 2 DataNodes are available during the restart window, but the pipeline needs 3 for replication.

### 3. Expected HDFS Behavior
The error in `DataStreamer.findNewDatanode()` (line 1347-1357) is correct:
```java
if (nodes.length != original.length + 1) {
    throw new IOException(
        "Failed to replace a bad datanode on the existing pipeline "
            + "due to no more good datanodes being available to try. "
            ...
```
The NameNode cannot add a replacement node because there aren't enough available DataNodes.

### 4. Not a Bug in HDFS
- HDFS correctly detects that DN2 became unavailable
- HDFS correctly attempts to find a replacement
- HDFS correctly fails when no replacement can be found
- The error message accurately describes the situation

## Root Cause
The restart framework creates a fault scenario (3 out of 5 DataNodes unavailable) that exceeds the test's fault tolerance threshold. With default replication factor 3 and only 2 available DataNodes, pipeline maintenance is mathematically impossible.

## Comparison with Group 8
This failure is closely related to Group 8 (`IOException in DataStreamer.handleBadDatanode`):
- **Group 8**: General failure to replace a node during pipeline recovery
- **Group 10**: Specifically no available replacement nodes

Both share the same root cause: restart framework injecting DataNode restarts during active write pipelines in tests that already have reduced DataNode availability.

## Affected Test Executions
All 11 test executions in this group follow the same pattern:
- Tests that explicitly stop DataNodes as part of their test scenario
- Restart injection on another DataNode creates excessive unavailability
- Pipeline recovery fails due to insufficient available nodes

## Recommendation
This restart position is inappropriate for tests that explicitly stop DataNodes. The framework should:
1. Avoid injecting restarts when doing so would exceed the cluster's fault tolerance
2. Consider the number of already-stopped/failed nodes before injecting additional failures
3. Skip restart injection at positions where the available node count is at the minimum required for replication
