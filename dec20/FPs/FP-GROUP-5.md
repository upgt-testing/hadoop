# FP-GROUP-5: ApplicationNotFoundException After RM Restart

**Classification:** FALSE POSITIVE
**Confidence:** HIGH

## Summary

The `ApplicationNotFoundException` after ResourceManager restart is **not a bug** in YARN source code. The failure is caused by missing RM recovery configuration in the test. Without recovery enabled, the RM loses all in-memory state on restart, including the registered applications, which causes `getApplicationReport()` to fail.

## Root Cause Analysis

### Error Observed
```
org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException:
Application with id 'application_1767767604127_0001' doesn't exist in RM. Please check that the job submission was successful.
```

### Failure Flow

1. **Test submits application:**
   - `createApp(rmClient, cluster, conf)` submits an application and waits for it to reach LAUNCHED state
   - Application is registered in RM's in-memory `RMApps` collection

2. **RM is restarted at "after_app_submit":**
   - RestartFramework triggers RM restart
   - Without recovery enabled, RM starts fresh with empty state

3. **Test tries to get application report:**
   - `createAMRMProtocol()` calls `rmClient.getApplicationReport(appId)` at line 76
   - RM's `ClientRMService.getApplicationReport()` checks `rmContext.getRMApps().get(appId)`
   - The app is not found (collection is empty after restart)
   - Throws `ApplicationNotFoundException`

### Why It Fails

Looking at `ClientRMService.getApplicationReport()` (ClientRMService.java:421):
```java
RMApp application = rmContext.getRMApps().get(applicationId);
if (application == null) {
  throw new ApplicationNotFoundException("Application with id '"
      + applicationId + "' doesn't exist in RM. Please check "
      + "that the job submission was successful.");
}
```

After RM restart WITHOUT recovery:
- `rmContext.getRMApps()` is an empty `ConcurrentHashMap`
- The application submitted before restart is lost
- `getApplicationReport()` throws `ApplicationNotFoundException`

## Missing Configuration

The test `TestAMRMProxy_RestartInjected.testE2ETokenSwap` is missing these required recovery configurations:

```java
// MISSING in TestAMRMProxy_RestartInjected.testE2ETokenSwap:
conf.set(YarnConfiguration.RECOVERY_ENABLED, "true");
conf.set(YarnConfiguration.RM_STORE, MemoryRMStateStore.class.getName());
conf.setBoolean(YarnConfiguration.RM_WORK_PRESERVING_RECOVERY_ENABLED, true);
```

### Current Test Configuration (Incomplete)
```java
Configuration conf = new YarnConfiguration();
conf.setBoolean(YarnConfiguration.AMRM_PROXY_ENABLED, true);
// Enable RPC mode with fixed ports for restart testing
conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, true);
conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_USE_RPC, true);
conf.set(YarnConfiguration.RM_ADDRESS, "localhost:18332");
conf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, "localhost:18330");
conf.set(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS, "localhost:18331");
conf.set(YarnConfiguration.RM_ADMIN_ADDRESS, "localhost:18333");
// NO RECOVERY CONFIG!
```

### Evidence from Official RM Restart Test

The official `TestAMRMClientOnRMRestart` properly configures recovery:
```java
conf.set(YarnConfiguration.RECOVERY_ENABLED, "true");
conf.set(YarnConfiguration.RM_STORE, MemoryRMStateStore.class.getName());
conf.setInt(YarnConfiguration.RM_AM_MAX_ATTEMPTS,
    YarnConfiguration.DEFAULT_RM_AM_MAX_ATTEMPTS);
conf.setBoolean(YarnConfiguration.RM_WORK_PRESERVING_RECOVERY_ENABLED, true);
conf.setLong(YarnConfiguration.RM_WORK_PRESERVING_RECOVERY_SCHEDULING_WAIT_MS, 0);
```

## Why This Is NOT A Bug

1. **Expected Behavior:** YARN is designed to support stateless RM restarts. When recovery is disabled, the RM starts fresh with no applications. This is documented behavior.

2. **Recovery Mechanism Exists:** The production code has proper recovery support:
   - `ResourceManager.recover()` recovers state from state store
   - `RMAppManager.recover()` restores applications
   - Applications are re-registered with proper state

3. **Test Configuration Issue:** The restart-injected test was derived from `TestAMRMProxyE2E` which never needed RM restart support. The restart injection framework exposed this missing configuration.

4. **Same Root Cause as Group 2:** This failure shares the identical root cause with Group 2 (InvalidToken exception) - both are due to missing RM recovery configuration.

## Affected Test Executions

Both executions in Group 5 are affected:
- `TestAMRMProxy_RestartInjected.testE2ETokenSwap` at position `after_app_submit` (2 executions)

Both fail when trying to get the application report after RM restart without recovery.

## Recommendation

This is a **false positive** caused by improper test configuration for the restart scenario. The failure does not indicate a bug in YARN's production code.

If the restart testing framework wants to test RM restart scenarios on `TestAMRMProxy`, the test should be modified to:
1. Enable RM recovery configuration
2. Use a state store (MemoryRMStateStore for testing)
3. Enable work-preserving recovery

However, since the original test was not designed to test RM restart resilience, these failures should be classified as FP.

## Related Groups

- **Group 2 (InvalidToken Exception):** Same root cause - missing RM recovery configuration causes state loss after restart.
