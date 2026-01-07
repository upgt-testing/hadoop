# FALSE POSITIVE Report - Group 6

## Summary
**Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
**Classification:** FALSE POSITIVE
**Root Cause:** Non-production test behavior causes timing-dependent failure

## Failure Details

### Exception
```
java.lang.NullPointerException
    at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal(TestAMRMProxy_RestartInjected.java:255)
```

### Restart Configuration
- **Position:** `after_cluster_start`
- **Target:** `resourcemanager`
- **Mode:** `GRACEFUL`

## Root Cause Analysis

### The Test Creates a Non-Production Token

The test's `BaseAMRMProxyE2ETest.createAMRMProtocol()` method (lines 87-89) explicitly creates a new token:

```java
org.apache.hadoop.security.token.Token<AMRMTokenIdentifier> token =
    amrmTokenSecretManager
        .createAndGetAMRMToken(report.getCurrentApplicationAttemptId());
```

**This is NOT what happens in production.** In production:
- The AM uses the token created during `initializePipeline()` when the AM container starts
- The AM does NOT create a new token before calling allocate

### Timing Analysis from Debug Logs

```
23:59:39.605: initializePipeline creates token (keyId=-1861838438, BEFORE roll)
23:59:40.061: AMRMProxy rolls key (nextMasterKey.keyId=-1861838437)
23:59:40.793: createAMRMProtocol() creates NEW token (keyId=-1861838437, uses nextMasterKey!)
23:59:40.854: Iteration 0 - AMRMToken=null

Token renewal check in AMRMProxyService.updateAMRMTokens():
  nextMasterKey.keyId (-1861838437) == AM token keyId (-1861838437)
  → Condition (nextMasterKey.keyId != amrmTokenIdentifier.keyId) is FALSE
  → No token sent!
```

### Why This Is a False Positive

1. **Non-production behavior:** The test's `createAMRMProtocol()` creates a new token that uses `getMasterKey()`, which returns `nextMasterKey` when it's set during a key rolling window.

2. **KeyId match:** The newly created token has the same keyId as nextMasterKey, causing the renewal condition to fail.

3. **Production flow is different:** In production, AMs use tokens from `initializePipeline()` which happens at container launch time, not test-created tokens.

4. **Mathematically impossible in production:** The test spans 18 seconds (5 iterations × 4.5s spacing). With 6.75-second renewal windows every 20 seconds, the gap between windows is 13.25 seconds. Since 18 > 13.25, at least one allocation MUST fall within a renewal window IF the token was created at a fixed time (not during the window itself).

### Why the Original Test Works

In the original test (without restart injection):
- Setup is faster (~15 seconds vs ~65 seconds with restart)
- Token creation timing is different relative to the key rolling schedule
- The test happens to hit a renewal window

With restart:
- The RM restart adds ~45 seconds of delay
- This shifts `createAMRMProtocol()` to happen exactly during a key rolling window
- The token gets the window's nextMasterKey keyId, causing the timing issue

## Mathematical Proof: Production Cannot Fail This Way

Key rolling parameters:
- Rolling interval: 20 seconds
- Window duration: 6.75 seconds
- Gap between windows: 13.25 seconds

Test parameters:
- 5 allocate calls spanning 18 seconds

Since the test span (18s) > gap between windows (13.25s), at least one allocation MUST occur during a renewal window. The failure only happens because the test artificially creates a token DURING the window, which is not production behavior.

## Conclusion

This is a **FALSE POSITIVE** because:
1. The production AMRMProxy token renewal mechanism works correctly
2. The failure is caused by test-specific token creation at an unfortunate time
3. In production, this scenario cannot occur because AMs use tokens from `initializePipeline()`, not manually created tokens

No bug exists in the production code. The test design (creating a new token in `createAMRMProtocol()`) combined with restart timing creates an artificial failure scenario.
