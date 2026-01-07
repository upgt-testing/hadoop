# FP-GROUP-2: InvalidToken Exception After RM Restart

**Classification:** FALSE POSITIVE
**Confidence:** HIGH

## Summary

The `InvalidToken` exception after ResourceManager restart is **not a bug** in YARN source code. The failure is caused by missing RM recovery configuration in the test. Without recovery enabled, the RM loses all in-memory state on restart, including the AMRM token registry, which causes token validation to fail.

## Root Cause Analysis

### Error Observed
```
org.apache.hadoop.security.token.SecretManager$InvalidToken:
  appattempt_1767767122685_0001_000001 not found in AMRMTokenSecretManager.
```

### Token Validation Flow

The `AMRMTokenSecretManager.retrievePassword()` method validates tokens by checking two conditions:

1. **Is the applicationAttemptId registered?** (Line 247-250 in AMRMTokenSecretManager.java)
   ```java
   if (!appAttemptSet.contains(applicationAttemptId)) {
       throw new InvalidToken(applicationAttemptId
           + " not found in AMRMTokenSecretManager.");
   }
   ```

2. **Does the token's keyId match current or next master key?** (Line 251-261)

### Why It Fails

1. **During normal operation:**
   - When AM registers, `createAndGetAMRMToken()` adds the `appAttemptId` to `appAttemptSet`
   - Token validation succeeds

2. **After RM restart WITHOUT recovery:**
   - The `appAttemptSet` is an in-memory `HashSet<ApplicationAttemptId>` (line 75-76)
   - On restart, this set is empty
   - Token validation fails because `appAttemptId` is not in the empty set

3. **What proper recovery would do:**
   - `RMAppAttemptImpl.recoverAppAttemptCredentials()` calls `createAndGetAMRMToken()` (line 1036-1037)
   - This would re-populate `appAttemptSet` with recovered app attempts
   - Token validation would then succeed

## Missing Configuration

The test `TestNMClient_RestartInjected` is missing these required recovery configurations:

```java
// MISSING in TestNMClient_RestartInjected:
conf.set(YarnConfiguration.RECOVERY_ENABLED, "true");
conf.set(YarnConfiguration.RM_STORE, MemoryRMStateStore.class.getName());
conf.setBoolean(YarnConfiguration.RM_WORK_PRESERVING_RECOVERY_ENABLED, true);
```

### Evidence from Official RM Restart Test

The official `TestAMRMClientOnRMRestart` (lines 87-97) properly configures recovery:
```java
conf.set(YarnConfiguration.RECOVERY_ENABLED, "true");
conf.set(YarnConfiguration.RM_STORE, MemoryRMStateStore.class.getName());
conf.setInt(YarnConfiguration.RM_AM_MAX_ATTEMPTS,
    YarnConfiguration.DEFAULT_RM_AM_MAX_ATTEMPTS);
conf.setBoolean(YarnConfiguration.RM_WORK_PRESERVING_RECOVERY_ENABLED, true);
conf.setLong(YarnConfiguration.RM_WORK_PRESERVING_RECOVERY_SCHEDULING_WAIT_MS, 0);
```

## Why This Is NOT A Bug

1. **Expected Behavior:** YARN is designed to support stateless RM restarts. When recovery is disabled, the RM starts fresh. This is documented behavior.

2. **Recovery Mechanism Exists:** The production code has proper recovery support:
   - `AMRMTokenSecretManager.recover()` recovers master keys from state store
   - `RMAppAttemptImpl.recoverAppAttemptCredentials()` re-creates tokens for recovered apps
   - `addPersistedPassword()` can restore token registrations

3. **Test Configuration Issue:** The restart-injected test was derived from `TestNMClient` which never needed RM restart support. The restart injection framework exposed this missing configuration.

## Affected Test Executions

All 6 executions in Group 2 are affected:
- `TestNMClient_RestartInjected.testNMClientNoCleanupOnStop` (3 executions)
- `TestNMClient_RestartInjected.testNMClient` (3 executions)

All fail at positions after AM registration when RM is restarted without recovery.

## Recommendation

This is a **false positive** caused by improper test configuration for the restart scenario. The failure does not indicate a bug in YARN's production code.

If the restart testing framework wants to test RM restart scenarios on `TestNMClient`, the test should be modified to:
1. Enable RM recovery configuration
2. Use a state store (MemoryRMStateStore for testing)
3. Enable work-preserving recovery

However, since the original test was not designed to test RM restart resilience, these failures should be classified as FP.
