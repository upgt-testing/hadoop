# False Positive Analysis: Group 1 - NodeManagers Failed to Connect After Restart

## Summary
**Status:** FALSE POSITIVE
**Priority:** VERY LOW (Exception from RestartTest adapter)
**Execution Count:** 22

## Root Cause
The restart-injected tests in this failure group lack the `YARN_MINICLUSTER_FIXED_PORTS = true` configuration, which is required for restart testing.

Without fixed ports, when the ResourceManager restarts:
1. The RM gets new ephemeral ports on restart
2. NodeManagers are still configured to connect to the old port addresses
3. NMs cannot find the RM at the old address
4. The `waitForNodeManagersToConnect()` method in YarnClusterAdapter fails
5. The adapter throws "NodeManagers failed to connect after restart"

## Evidence

### Affected Tests Configuration
The following tests in Group 1 do NOT set `YARN_MINICLUSTER_FIXED_PORTS = true`:

**TestUnmanagedAMLauncher_RestartInjected.java:**
```java
@BeforeClass
public static void setup() throws InterruptedException, IOException {
    LOG.info("Starting up YARN cluster");
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    // NOTE: YARN_MINICLUSTER_FIXED_PORTS is NOT set
    if (yarnCluster == null) {
      yarnCluster = new MiniYARNCluster(
          TestUnmanagedAMLauncher_RestartInjected.class.getSimpleName(), 1, 1, 1);
      yarnCluster.init(conf);
      yarnCluster.start();
```

**TestOpportunisticContainerAllocationE2E_RestartInjected.java:**
```java
@BeforeClass
public static void setup() throws Exception {
    conf = new YarnConfiguration();
    conf.setLong(
        YarnConfiguration.RM_AMRM_TOKEN_MASTER_KEY_ROLLING_INTERVAL_SECS,
        ROLLING_INTERVAL_SEC);
    // ... more config but NO YARN_MINICLUSTER_FIXED_PORTS ...
    yarnCluster =
        new MiniYARNCluster(TestAMRMClient.class.getName(), nodeCount, 1, 1);
    yarnCluster.init(conf);
    yarnCluster.start();
```

### Comparison with Properly Configured Tests
Other restart-injected tests that work correctly DO set fixed ports:

**TestNMClient_RestartInjected.java (line 173):**
```java
conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, true);
```

**TestAMRMProxy_RestartInjected.java (line 78):**
```java
conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, true);
```

**TestMiniYARNClusterForHA_RestartInjected.java (line 56):**
```java
conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, true);
```

### YarnClusterAdapter Code
From `YarnClusterAdapter.java` (lines 128-161):
```java
@Override
public void waitActive(MiniYARNCluster cluster) throws Exception {
    LOG.info("Waiting for YARN cluster to become active");

    // ... wait for RMs to be started ...

    // Wait for all NodeManagers to connect with extended timeout
    int maxAttempts = 3;
    boolean connected = false;
    for (int attempt = 1; attempt <= maxAttempts && !connected; attempt++) {
        LOG.info("Waiting for NodeManagers to connect (attempt {}/{})", attempt, maxAttempts);
        connected = cluster.waitForNodeManagersToConnect(20000);
        if (!connected && attempt < maxAttempts) {
            Thread.sleep(2000);
        }
    }

    if (!connected) {
        LOG.warn("NodeManagers did not all connect after {} attempts", maxAttempts);
        throw new Exception("NodeManagers failed to connect after restart");
    }
}
```

The adapter correctly expects NMs to reconnect, but they cannot do so with ephemeral ports.

## Why This Is a False Positive

1. **Not a Production Code Issue:** The failure is not in YARN production code. It's caused by missing test configuration for the restart testing framework.

2. **Test Configuration Deficiency:** The restart-injected tests were generated without the `YARN_MINICLUSTER_FIXED_PORTS = true` setting that is essential for restart testing.

3. **Other Tests Work Correctly:** Restart-injected tests that DO include the fixed ports configuration work correctly and don't exhibit this failure.

4. **Exception From Adapter:** The exception `NodeManagers failed to connect after restart` is thrown from `YarnClusterAdapter.waitActive()` in the restart testing framework, not from YARN production code.

## Recommendation

The restart-injected tests in Group 1 need to be regenerated with `YARN_MINICLUSTER_FIXED_PORTS = true` configuration added to their setup methods. Without this, the NodeManagers cannot find the ResourceManager after it restarts with new ports.

## Affected Test Classes

Based on the Group 1 test executions:
- `org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected`
- `org.apache.hadoop.yarn.client.api.impl.TestOpportunisticContainerAllocationE2E_RestartInjected`

## Stacktrace Sample

```
org.restarttest.core.RestartException: Restart failed at position after_launcher_init
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	...
Caused by: java.lang.Exception: NodeManagers failed to connect after restart
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java:138)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java:27)
	at org.restarttest.core.RestartExecutor.executeRestart(RestartExecutor.java:116)
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:83)
	... 32 more
```
