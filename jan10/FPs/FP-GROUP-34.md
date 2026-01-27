# FP-GROUP-34: InvalidToken in renewToken

## Summary
FALSE POSITIVE - The test uses a test-only utility method `generateDelegationToken()` that bypasses the normal delegation token persistence mechanism. In production, delegation tokens are created via RPC which properly logs them to the edit log for persistence across restarts.

## Failure Details
- **Test Class**: `org.apache.hadoop.hdfs.security.TestDelegationToken_RestartInjected`
- **Test Methods**: `testDelegationTokenMetrics`, `testDelegationTokenSecretManager`
- **Restart Position**: `after_token_generation`
- **Restart Target**: `namenode`
- **Restart Mode**: `GRACEFUL`
- **Failures**: 2

## Stack Trace
```
org.apache.hadoop.security.token.SecretManager$InvalidToken: Renewal request for unknown token (token for SomeUser: HDFS_DELEGATION_TOKEN owner=SomeUser, renewer=JobTracker, realUser=, issueDate=..., maxDate=..., sequenceNumber=1, masterKeyId=2)
    at org.apache.hadoop.security.token.delegation.AbstractDelegationTokenSecretManager.renewToken(AbstractDelegationTokenSecretManager.java:608)
    at org.apache.hadoop.hdfs.security.TestDelegationToken_RestartInjected.testDelegationTokenMetrics(TestDelegationToken_RestartInjected.java:220)
```

## Root Cause Analysis

### Test's Token Creation Method
The test uses a test-only utility method to create delegation tokens:
```java
private Token<DelegationTokenIdentifier> generateDelegationToken(
    String owner, String renewer) {
  DelegationTokenIdentifier dtId = new DelegationTokenIdentifier(new Text(
      owner), new Text(renewer), null);
  return new Token<DelegationTokenIdentifier>(dtId, dtSecretManager);
}
```

This method:
1. Creates a `DelegationTokenIdentifier` directly
2. Creates a Token using the `dtSecretManager` constructor
3. **Does NOT log the token to the edit log**

### Normal Production Flow
In production, clients obtain delegation tokens via RPC which goes through `FSNamesystem.getDelegationToken()`:
```java
// From FSNamesystem.java line 6103-6142
Token<DelegationTokenIdentifier> getDelegationToken(Text renewer) throws IOException {
    // ... validation ...
    DelegationTokenIdentifier dtId = new DelegationTokenIdentifier(owner, renewer, realUser);
    token = new Token<DelegationTokenIdentifier>(dtId, dtSecretManager);
    long expiryTime = dtSecretManager.getTokenExpiryTime(dtId);
    getEditLog().logGetDelegationToken(dtId, expiryTime);  // <-- LOGS TO EDIT LOG
    // ...
}
```

The key difference is line 6134: `getEditLog().logGetDelegationToken(dtId, expiryTime)` which persists the token to the edit log.

### Why Token Is Lost After Restart

1. **Token Creation**: Test calls `generateDelegationToken()` which stores token only in memory (in `dtSecretManager.currentTokens` map)
2. **No Edit Log Entry**: The test method bypasses `FSNamesystem.getDelegationToken()` so no edit log entry is created
3. **NameNode Restart**: The restart framework injects a graceful NameNode restart
4. **Edit Log Replay**: During restart, the edit log is replayed but there's no `logGetDelegationToken` entry for this token
5. **Token Lost**: The token is not restored to the new DelegationTokenSecretManager
6. **Renewal Fails**: When the test tries to renew the token, it gets "unknown token" error

This is confirmed by the log output after restart:
```
Token renewal for identifier: ... total currentTokens 0
```

The `currentTokens 0` indicates no tokens were loaded after restart.

### Why This Is a False Positive

1. **Test-Only Method**: `generateDelegationToken()` is a test utility designed to unit test the secret manager's in-memory behavior, not the full persistence system

2. **Production Behavior Is Correct**: Real clients always get tokens via RPC which properly persists them:
   - `DFSClient.getDelegationToken()` → RPC → `FSNamesystem.getDelegationToken()` → Edit log

3. **Inappropriate Restart Position**: The restart position `after_token_generation` is inappropriate for this test because:
   - The test creates tokens using a non-production path that doesn't persist to edit log
   - Injecting a restart here tests a scenario that can't occur in production

4. **No Bug in HDFS**: The persistence mechanism works correctly. The test just doesn't use it.

## Verification

Confirmed by checking log output:
- Before restart: Token is created successfully (`Creating password for identifier: ...`)
- After restart: No tokens in memory (`total currentTokens 0`)
- No edit log entry for the token creation

## Conclusion
This is a FALSE POSITIVE caused by the restart framework injecting a restart at an inappropriate position for this test. The test's token creation method bypasses the normal persistence path, so tokens are not persisted and cannot survive restarts. This is not a bug in HDFS - it's expected behavior when using test-only APIs that skip persistence.
