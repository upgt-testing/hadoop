# FP-GROUP-3: TimeoutException in TestCleanupAfterKill

## Classification: FALSE POSITIVE

## Summary
The test `TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded` times out waiting for an application to be killed based on its lifetime expiration after an RM restart. This is a false positive because the test does not enable RM recovery, causing the application lifetime tracking to be lost after restart.

## Failure Details

**Test:** `org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded`

**Restart Parameters:**
- Position: `after_app_create`
- Target: `resourcemanager`
- Mode: `GRACEFUL`
- Index: `0`

**Error:**
```
java.util.concurrent.TimeoutException:
Timed out waiting for condition.
```

## Root Cause Analysis

### Test Behavior
1. The test creates an application with a 30-second lifetime (`exampleApp.setLifetime(30L)`)
2. After app creation, the RM is restarted at position `after_app_create`
3. The test waits for the app to reach KILLED state (timeout: 200 seconds)
4. The app never reaches KILLED state because the lifetime tracking is lost

### Why This Is a False Positive

**Missing RM Recovery Configuration:**

The test setup in `ServiceTestUtils.setupInternal()` does NOT enable RM recovery. The key configuration is:
```java
// YarnConfiguration.RECOVERY_ENABLED is NOT set (defaults to false)
```

Without RM recovery enabled:
1. When the RM restarts, all in-memory state is lost
2. The `RMAppLifetimeMonitor` is re-created with no registered applications
3. The application that was submitted before restart is not recovered
4. The application's lifetime is never enforced because it's not registered with the new `RMAppLifetimeMonitor`

### How Lifetime Monitoring Works

**Normal Flow (without restart):**
1. App is submitted with lifetime
2. `RMAppImpl.StartAppAttemptTransition` registers app with `RMAppLifetimeMonitor`
3. `RMAppLifetimeMonitor` tracks the expiration time
4. When time expires, `RMAppLifetimeMonitor.expire()` sends KILL event to the app

**Recovery Flow (with RM recovery enabled):**
1. RM restarts and recovers app state from state store
2. `RMAppRecoveredTransition` (in `RMAppImpl.java:1133-1145`) re-registers app with `RMAppLifetimeMonitor`:
```java
for (Map.Entry<ApplicationTimeoutType, Long> timeout : app.applicationTimeouts
    .entrySet()) {
  app.rmContext.getRMAppLifetimeMonitor().registerApp(app.applicationId,
      timeout.getKey(), timeout.getValue());
}
```
3. App lifetime continues to be enforced after restart

**Current Test Flow (without RM recovery):**
1. App submitted with 30s lifetime
2. RM restarted
3. RM has no knowledge of previous apps
4. `RMAppLifetimeMonitor` has no registered apps
5. App is never killed -> timeout

## Verification

**Test passes without restart injection:**
```bash
$ mvn surefire:test -Dtest=TestCleanupAfterKill_RestartInjected#testRegistryCleanedOnLifetimeExceeded
# Result: PASS (62 seconds)
```

**Test fails with restart injection:**
```bash
$ mvn surefire:test -Dtest=TestCleanupAfterKill_RestartInjected#testRegistryCleanedOnLifetimeExceeded \
    -Drestart.position=after_app_create -Drestart.target=resourcemanager -Drestart.mode=GRACEFUL
# Result: FAIL (TimeoutException after 239 seconds)
```

## Conclusion

This is a **FALSE POSITIVE** because:

1. **Test Configuration Issue:** The test does not enable RM recovery, which is required for RM to preserve state across restarts
2. **Expected Behavior:** Without recovery, RM losing state on restart is expected behavior
3. **Same Root Cause:** This is the same root cause as Group 2 (InvalidToken) and Group 5 (ApplicationNotFoundException) - all due to missing RM recovery configuration
4. **Production Behavior:** In a production deployment with proper RM recovery configuration, the application lifetime would be properly restored and enforced after RM restart

## Related Groups
- Group 2 (InvalidToken Exception) - Same root cause
- Group 5 (ApplicationNotFoundException) - Same root cause

## Affected Code Paths
- `org.apache.hadoop.yarn.server.resourcemanager.rmapp.monitor.RMAppLifetimeMonitor` - Lifetime tracking
- `org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppImpl.RMAppRecoveredTransition` - Recovery handling
- `org.apache.hadoop.yarn.service.ServiceTestUtils` - Test configuration
