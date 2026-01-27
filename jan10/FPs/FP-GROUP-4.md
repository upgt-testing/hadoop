# FP-GROUP-4: AccessControlException in FSPermissionChecker.checkSuperuserPrivilege

## Summary
FALSE POSITIVE - The restart framework inherits the test's unprivileged user context when performing system-level operations (`datanodeReport`) that require superuser privilege.

## Failure Details
- **Test Class**: `org.apache.hadoop.fs.TestGlobPaths_RestartInjected`
- **Test Methods**: Multiple methods (31 failures total)
- **Restart Position**: `after_test_*` positions (e.g., `after_test_glob_access_denied_on_fc`)
- **Restart Target**: DataNode (index 0)
- **Restart Mode**: GRACEFUL
- **Exception**: `org.apache.hadoop.security.AccessControlException: Access denied for user myuser. Superuser privilege is required`

## Stack Trace
```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException): Access denied for user myuser. Superuser privilege is required
    at org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker.checkSuperuserPrivilege(FSPermissionChecker.java:154)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkSuperuserPrivilege(FSNamesystem.java:5211)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.datanodeReport(FSNamesystem.java:4851)
    at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.getDatanodeReport(NameNodeRpcServer.java:1269)
    ...
    at org.apache.hadoop.hdfs.DFSClient.datanodeReport(DFSClient.java:2102)
    at org.apache.hadoop.hdfs.MiniDFSCluster.waitActive(MiniDFSCluster.java:2812)
    at org.apache.hadoop.hdfs.restart.HdfsClusterAdapter.restartDataNode(HdfsClusterAdapter.java:265)
```

## Root Cause Analysis

### Test Setup Context
The test `TestGlobPaths_RestartInjected` explicitly creates an unprivileged user for testing access control features:

```java
// TestGlobPaths_RestartInjected.java (lines 43-45)
private static final UserGroupInformation unprivilegedUser =
    UserGroupInformation.createUserForTesting("myuser", new String[] { "mygroup" });
```

In the `@BeforeClass` setup (lines 70-84), the test:
1. Creates a MiniDFSCluster with default (privileged) user
2. Sets permission on "/" to 777 to allow unprivileged access
3. **Changes the login user to `unprivilegedUser`** via `UserGroupInformation.setLoginUser(unprivilegedUser)`
4. Gets new FileSystem/FileContext with the unprivileged user

```java
// TestGlobPaths_RestartInjected.java (lines 78-82)
privilegedFs.setPermission(new Path("/"), FsPermission.createImmutable((short)0777));
UserGroupInformation.setLoginUser(unprivilegedUser);  // <-- Key line
fs = FileSystem.get(conf);
fc = FileContext.getFileContext(conf);
```

### Restart Framework Behavior
When the restart framework restarts a DataNode:

1. `HdfsClusterAdapter.restartDataNode()` (line 309-352) performs the restart
2. After restart, it calls `cluster.waitActive()` (line 350)
3. `MiniDFSCluster.waitActive()` creates a `DFSClient` with the **current user context** (line 2806)
4. The `DFSClient` calls `datanodeReport()` (line 2812), which requires superuser privilege
5. Since the current login user is still "myuser" (set by the test), the call fails

```java
// MiniDFSCluster.java (lines 2806-2812)
DFSClient client = new DFSClient(addr, conf);  // Uses current login user ("myuser")
while (shouldWait(client.datanodeReport(DatanodeReportType.LIVE), addr)) {  // Requires superuser
    LOG.info("Waiting for cluster to become active");
    Thread.sleep(100);
}
```

### Why This Is Not A Bug

1. **Test-specific user context**: The test intentionally sets up an unprivileged user to test HDFS access control features (e.g., `TestGlobAccessDenied`). This is a valid and correct test design.

2. **Restart framework limitation**: The restart framework performs system-level operations (`datanodeReport`) under whatever user context the test has established. In production, restart operations would be performed by a privileged administrator or daemon.

3. **Not an HDFS bug**: HDFS correctly enforces that `datanodeReport` requires superuser privilege. The permission check is working as designed.

4. **Improper restart position**: Injecting a restart after the test completes but before the user context is restored creates an incompatible state where system operations cannot be performed.

## Why This Is A False Positive

The failure occurs because:
1. The restart framework does not save/restore the original privileged user context
2. The restart framework's `waitActive()` call is a system-level operation that requires privileges the current test user doesn't have
3. The test legitimately uses an unprivileged user to test access control, which is correct behavior

In a real production environment:
- Restart operations are performed by privileged system processes (e.g., YARN NodeManager, systemd services)
- Users don't perform administrative operations like datanode restarts directly
- The separation of user operations from system operations is intentional and correct

## Affected Tests
All 31 failures in Group 4 share this pattern - they are tests that change the login user to an unprivileged user and then have restart injection at positions where the framework's `waitActive()` fails due to the unprivileged context.

## Potential Framework Fix
The restart framework could be enhanced to:
1. Save the original `UserGroupInformation.getLoginUser()` before restart
2. Temporarily restore a privileged user context for system operations
3. Restore the test's user context after the restart completes

However, this is a restart framework enhancement, not an HDFS bug fix.
