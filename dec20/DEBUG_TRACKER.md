# DEBUG TRACKER - YARN Dec20 Grouped Failures

**Total Groups:** 6
**Ordered by:** Priority (Highest = Most Likely Actual Bug, Lowest = Most Likely False Positive)

---

## HIGHEST PRIORITY - Likely Actual Bugs

### [ ] Group 6 - NullPointerException in Test Code
**Priority:** HIGHEST - NPE thrown directly from test code (not from restarttest adapter)
**Execution Count:** 1

**Generalized Stacktrace:**
```
java.lang.NullPointerException
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal(TestAMRMProxy_RestartInjected.java)
```

**Raw Stacktrace Sample:**
```
java.lang.NullPointerException
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal(TestAMRMProxy_RestartInjected.java:255)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
	at org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:365)
	at org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:273)
	at org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:238)
	at org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:159)
	at org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:384)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:345)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:126)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:418)
```

**Test Executions (Examples):**
1. Test: `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
   - "position": "after_cluster_start"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "002-1c9a6681"

---

### [ ] Group 2 - InvalidToken Exception in Production Code
**Priority:** HIGH - InvalidToken exception thrown from production code (org.apache.hadoop.ipc.Client)
**Execution Count:** 6

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.token.SecretManager$InvalidToken)
	at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.security.token.SecretManager$InvalidToken: appattempt_1766338138761_0001_000001 not found in AMRMTokenSecretManager.
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
	at org.apache.hadoop.yarn.ipc.RPCUtil.instantiateException(RPCUtil.java:53)
	at org.apache.hadoop.yarn.ipc.RPCUtil.instantiateIOException(RPCUtil.java:80)
	at org.apache.hadoop.yarn.ipc.RPCUtil.unwrapAndThrowException(RPCUtil.java:119)
	at org.apache.hadoop.yarn.api.impl.pb.client.ApplicationMasterProtocolPBClientImpl.allocate(ApplicationMasterProtocolPBClientImpl.java:80)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.apache.hadoop.io.retry.RetryInvocationHandler.invokeMethod(RetryInvocationHandler.java:433)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.invokeMethod(RetryInvocationHandler.java:166)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.invoke(RetryInvocationHandler.java:158)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.invokeOnce(RetryInvocationHandler.java:96)
	at org.apache.hadoop.io.retry.RetryInvocationHandler.invoke(RetryInvocationHandler.java:362)
	at com.sun.proxy.$Proxy93.allocate(Unknown Source)
	at org.apache.hadoop.yarn.client.api.impl.AMRMClientImpl.allocate(AMRMClientImpl.java:325)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.allocateContainers(TestNMClient_RestartInjected.java:397)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop(TestNMClient_RestartInjected.java:309)
...
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.token.SecretManager$InvalidToken): appattempt_1766338138761_0001_000001 not found in AMRMTokenSecretManager.
	at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java:1584)
	at org.apache.hadoop.ipc.Client.call(Client.java:1530)
	at org.apache.hadoop.ipc.Client.call(Client.java:1427)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:258)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:139)
	at com.sun.proxy.$Proxy92.allocate(Unknown Source)
	at org.apache.hadoop.yarn.api.impl.pb.client.ApplicationMasterProtocolPBClientImpl.allocate(ApplicationMasterProtocolPBClientImpl.java:78)
	... 43 more
```

**Test Executions (Examples):**
1. Test: `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
   - "position": "after_am_register"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "002-69c87beb"
2. Test: `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
   - "position": "after_container_allocate"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "003-00f4984d"
3. Test: `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
   - "position": "after_am_register"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "002-3d37282c"

---

### [ ] Group 5 - ApplicationNotFoundException in Production Code
**Priority:** MEDIUM-HIGH - Exception thrown from production code (ClientRMService)
**Execution Count:** 2

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException)
	at org.apache.hadoop.yarn.server.resourcemanager.ClientRMService.getApplicationReport(ClientRMService.java)
```

**Raw Stacktrace Sample:**
```
org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException:
Application with id 'application_1766338173972_0001' doesn't exist in RM. Please check that the job submission was successful.
	at org.apache.hadoop.yarn.server.resourcemanager.ClientRMService.getApplicationReport(ClientRMService.java:421)
	at org.apache.hadoop.yarn.api.impl.pb.service.ApplicationClientProtocolPBServiceImpl.getApplicationReport(ApplicationClientProtocolPBServiceImpl.java:247)
	at org.apache.hadoop.yarn.proto.ApplicationClientProtocol$ApplicationClientProtocolService$2.callBlockingMethod(ApplicationClientProtocol.java:615)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:621)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:589)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:573)
	at org.apache.hadoop.ipc.RPC$Server.call(RPC.java:1213)
	at org.apache.hadoop.ipc.Server$RpcCall.run(Server.java:1089)
	at org.apache.hadoop.ipc.Server$RpcCall.run(Server.java:1012)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.Subject.doAs(Subject.java:422)
	at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1899)
	at org.apache.hadoop.ipc.Server$Handler.run(Server.java:3026)
...
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException): Application with id 'application_1766338173972_0001' doesn't exist in RM. Please check that the job submission was successful.
	at org.apache.hadoop.yarn.server.resourcemanager.ClientRMService.getApplicationReport(ClientRMService.java:421)
	... (remote call stack)
	at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java:1584)
	at org.apache.hadoop.ipc.Client.call(Client.java:1530)
	at org.apache.hadoop.ipc.Client.call(Client.java:1427)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:258)
	at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:139)
	at com.sun.proxy.$Proxy93.getApplicationReport(Unknown Source)
	at org.apache.hadoop.yarn.api.impl.pb.client.ApplicationClientProtocolPBClientImpl.getApplicationReport(ApplicationClientProtocolPBClientImpl.java:256)
	... 41 more
```

**Test Executions (Examples):**
1. Test: `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
   - "position": "after_app_submit"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "003-2cbb8a40"
2. Test: `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
   - "position": "after_app_submit"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "003-2cbb8a40"

---

## LOWER PRIORITY - Likely False Positives

### [ ] Group 3 - TimeoutException
**Priority:** LOW - Timeout exception (per guidelines)
**Execution Count:** 2

**Generalized Stacktrace:**
```
java.util.concurrent.TimeoutException
Timed out waiting for condition.
```

**Raw Stacktrace Sample:**
```
java.util.concurrent.TimeoutException:
Timed out waiting for condition.
Thread diagnostics:
Timestamp: 2025-12-21 05:34:47,797

"IPC Server handler 14 on default port 28032" daemon prio=5 tid=695 timed_waiting
java.lang.Thread.State: TIMED_WAITING
        at sun.misc.Unsafe.park(Native Method)
        at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
        at java.util.concurrent.LinkedBlockingQueue.poll(LinkedBlockingQueue.java:467)
        at org.apache.hadoop.ipc.CallQueueManager.take(CallQueueManager.java:317)
        at org.apache.hadoop.ipc.Server$Handler.run(Server.java:2992)
...
[Thread dump contains extensive thread state information]
```

**Test Executions (Examples):**
1. Test: `org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded`
   - "position": "after_app_create"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "003-3ef3f233"
2. Test: `org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded`
   - "position": "after_app_create"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "003-3ef3f233"

---

### [ ] Group 1 - RestartTest Adapter Exception (NodeManagers Failed to Connect)
**Priority:** VERY LOW - Exception directly thrown from restarttest adapter module
**Execution Count:** 22

**Generalized Stacktrace:**
```
Caused by: java.lang.Exception
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_launcher_init
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.testUMALauncher(TestUnmanagedAMLauncher_RestartInjected.java:173)
...
Caused by: java.lang.Exception: NodeManagers failed to connect after restart
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java:158)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java:41)
	at org.restarttest.core.RestartExecutor.executeRestart(RestartExecutor.java:116)
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:83)
	... 32 more
```

**Test Executions (Examples):**
1. Test: `org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.testUMALauncher`
   - "position": "after_launcher_init"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "002-91c9f515"
2. Test: `org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.testUMALauncher`
   - "position": "after_launcher_run"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "003-113ed99d"
3. Test: `org.apache.hadoop.yarn.client.api.impl.TestOpportunisticContainerAllocationE2E_RestartInjected.testMixedAllocationAndRelease`
   - "position": "after_container_allocate"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "006-69167a8b"

---

### [ ] Group 4 - RestartTest Adapter Exception (HA State Restore Failed)
**Priority:** VERY LOW - Exception directly thrown from restarttest adapter module
**Execution Count:** 2

**Generalized Stacktrace:**
```
Caused by: org.apache.hadoop.security.AccessControlException
	at org.apache.hadoop.yarn.server.resourcemanager.AdminService.checkHaStateChange(AdminService.java)
```

**Raw Stacktrace Sample:**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:85)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java:172)
	at org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover(TestRMFailover_RestartInjected.java:216)
...
Caused by: java.lang.Exception: Failed to restore RM HA state after restart
	at org.restarttest.adapter.yarn.YarnClusterAdapter.restartResourceManager(YarnClusterAdapter.java:288)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.restartNode(YarnClusterAdapter.java:78)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.restartNode(YarnClusterAdapter.java:41)
	at org.restarttest.core.RestartExecutor.restartSingleNode(RestartExecutor.java:186)
	at org.restarttest.core.RestartExecutor.performRestart(RestartExecutor.java:170)
	at org.restarttest.core.RestartExecutor.executeRestart(RestartExecutor.java:112)
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java:83)
	... 34 more
Caused by: org.apache.hadoop.security.AccessControlException: Manual failover for this ResourceManager is disallowed, because automatic failover is enabled.
	at org.apache.hadoop.yarn.server.resourcemanager.AdminService.checkHaStateChange(AdminService.java:253)
	at org.apache.hadoop.yarn.server.resourcemanager.AdminService.transitionToActive(AdminService.java:311)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.restartResourceManager(YarnClusterAdapter.java:268)
	... 40 more
```

**Test Executions (Examples):**
1. Test: `org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover`
   - "position": "after_cluster_start"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "002-e18afe6c"
2. Test: `org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover`
   - "position": "after_cluster_start"
   - "target": "resourcemanager"
   - "mode": "GRACEFUL"
   - "index": "0"
   - "executionDir": "002-e18afe6c"

---

## Summary

**Total Groups Analyzed:** 6

**Priority Distribution:**
- **HIGHEST Priority (NPE in non-restarttest code):** 1 group
- **HIGH Priority (Exceptions in production code):** 2 groups
- **LOW Priority (Timeouts):** 1 group
- **VERY LOW Priority (RestartTest adapter failures):** 2 groups

**Recommendation:** Start debugging with Group 6, then Group 2, then Group 5. These are the most likely to represent actual bugs in the YARN codebase that are exposed by restart testing.
