# YARN Restart Test Failure Inspection List

Failures prioritized by likelihood of being real bugs (highest priority first).

## Executive Summary

**Total Failure Groups:** 83
**High Priority (Real Bugs):** 9 groups

### Top Issues (by occurrence count):

1. **java.lang.AssertionError** (9 occurrences)
   - Number of nm-log-dirs is wrong. expected:<4> but was:<0>
   - Test class: `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected`

2. **java.lang.AssertionError** (9 occurrences)
   - Number of nm-local-dirs is wrong. expected:<4> but was:<0>
   - Test class: `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected`

3. **java.lang.IndexOutOfBoundsException** (9 occurrences)
   - Index: 0, Size: 0
   - Test class: `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected`

4. **java.lang.IndexOutOfBoundsException** (8 occurrences)
   - Index: 0, Size: 0
   - Test class: `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected`

5. **org.junit.runners.model.TestTimedOutException** (5 occurrences)
   - test timed out after 120000 milliseconds
   - Test class: `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected`

---

**Legend:**
- 🔴 HIGH PRIORITY: Likely real bugs (exceptions, assertion failures, etc.)
- 🟡 MEDIUM PRIORITY: Unclear failures, may need investigation
- ⚪ LOW PRIORITY: Testing framework issues

---


## 🔴 HIGH PRIORITY - Likely Real Bugs

### Group 1: java.lang.AssertionError

**Occurrences:** 9

**Failure Message:**
```
Number of nm-log-dirs is wrong. expected:<4> but was:<0>
```

**Stack Trace (first 5 lines):**
```
java.lang.AssertionError: Number of nm-log-dirs is wrong. expected:<4> but was:<0>
	at org.junit.Assert.fail(Assert.java)
	at org.junit.Assert.failNotEquals(Assert.java)
	at org.junit.Assert.assertEquals(Assert.java)
	at org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testDirsFailures(TestDiskFailures_RestartInjected.java)
```

**Affected Tests (9):**

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_initial_health_check`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `027-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_initial_health_check__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_first_disk_failure`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `029-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_first_disk_failure__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `024-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_node_unhealthy`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `033-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_node_unhealthy__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_first_disk_failure`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `030-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_first_disk_failure__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_first_disk_failure`
  - **Target:** `nodemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `031-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_first_disk_failure__target_nodemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_node_unhealthy`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `032-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_node_unhealthy__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `026-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_cluster_start__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_initial_health_check`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `028-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_initial_health_check__target_nodemanager__mode_CRASH_`

---

### Group 2: java.lang.AssertionError

**Occurrences:** 9

**Failure Message:**
```
Number of nm-local-dirs is wrong. expected:<4> but was:<0>
```

**Stack Trace (first 5 lines):**
```
java.lang.AssertionError: Number of nm-local-dirs is wrong. expected:<4> but was:<0>
	at org.junit.Assert.fail(Assert.java)
	at org.junit.Assert.failNotEquals(Assert.java)
	at org.junit.Assert.assertEquals(Assert.java)
	at org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testDirsFailures(TestDiskFailures_RestartInjected.java)
```

**Affected Tests (9):**

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_initial_health_check`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `017-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_initial_health_check__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_node_unhealthy`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `023-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_node_unhealthy__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_node_unhealthy`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `022-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_node_unhealthy__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_first_disk_failure`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `020-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_first_disk_failure__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_initial_health_check`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `018-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_initial_health_check__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_first_disk_failure`
  - **Target:** `nodemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `021-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_first_disk_failure__target_nodemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `014-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_first_disk_failure`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `019-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_first_disk_failure__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `016-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_cluster_start__target_resourcemanager__mode_CRASH_`

---

### Group 3: java.lang.IndexOutOfBoundsException

**Occurrences:** 9

**Failure Message:**
```
Index: 0, Size: 0
```

**Stack Trace (first 5 lines):**
```
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
	at java.util.ArrayList.rangeCheck(ArrayList.java)
	at java.util.ArrayList.get(ArrayList.java)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.allocateContainers(TestNMClient_RestartInjected.java)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop(TestNMClient_RestartInjected.java)
```

**Affected Tests (9):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `before_am_finish`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `112-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_before_am_finish__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `103-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_container_allocate`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `108-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_container_allocate__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_container_allocate`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `110-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_container_allocate__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_container_allocate`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `109-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_container_allocate__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `before_am_finish`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `111-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_before_am_finish__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `106-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_am_register__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_container_allocate`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `107-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_container_allocate__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `105-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_am_register__target_resourcemanager__mode_CRASH_`

---

### Group 4: java.lang.IndexOutOfBoundsException

**Occurrences:** 8

**Failure Message:**
```
Index: 0, Size: 0
```

**Stack Trace (first 5 lines):**
```
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
	at java.util.ArrayList.rangeCheck(ArrayList.java)
	at java.util.ArrayList.get(ArrayList.java)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.allocateContainers(TestNMClient_RestartInjected.java)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient(TestNMClient_RestartInjected.java)
```

**Affected Tests (8):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `after_container_management`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `117-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_after_container_management__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `before_am_finish`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `121-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_before_am_finish__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `115-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_after_am_register__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `before_am_finish`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `120-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_before_am_finish__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `after_container_management`
  - **Target:** `nodemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `119-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_after_container_management__target_nodemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `113-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `116-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_after_am_register__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `after_container_management`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `118-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_after_container_management__target_nodemanager__mode_CRASH_`

---

### Group 5: org.junit.runners.model.TestTimedOutException

**Occurrences:** 5

**Failure Message:**
```
test timed out after 120000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 120000 milliseconds
	at org.apache.hadoop.yarn.client.api.impl.BaseAMRMProxyE2ETest.createApp(BaseAMRMProxyE2ETest.java)
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal(TestAMRMProxy_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (5):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `096-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyTokenRenewal__position_after_app_submit__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `092-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyTokenRenewal__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `094-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyTokenRenewal__position_after_cluster_start__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `095-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyTokenRenewal__position_after_app_submit__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `097-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyTokenRenewal__position_after_app_submit__target_resourcemanager__mode_DELAYED_CRASH_`

---

### Group 6: org.junit.runners.model.TestTimedOutException

**Occurrences:** 5

**Failure Message:**
```
test timed out after 120000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 120000 milliseconds
	at org.apache.hadoop.yarn.client.api.impl.BaseAMRMProxyE2ETest.createApp(BaseAMRMProxyE2ETest.java)
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E(TestAMRMProxy_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (5):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `086-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyE2E__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `089-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyE2E__position_after_app_submit__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `088-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyE2E__position_after_cluster_start__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `091-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyE2E__position_after_app_submit__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `090-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyE2E__position_after_app_submit__target_resourcemanager__mode_CRASH_`

---

### Group 7: org.junit.runners.model.TestTimedOutException

**Occurrences:** 4

**Failure Message:**
```
test timed out after 120000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 120000 milliseconds
	at org.apache.hadoop.yarn.client.api.impl.BaseAMRMProxyE2ETest.createApp(BaseAMRMProxyE2ETest.java)
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap(TestAMRMProxy_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (4):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `101-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testE2ETokenSwap__position_after_app_submit__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `102-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testE2ETokenSwap__position_after_app_submit__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `100-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testE2ETokenSwap__position_after_cluster_start__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `098-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testE2ETokenSwap__position_null__target_null__mode_null_`

---

### Group 8: org.junit.runners.model.TestTimedOutException

**Occurrences:** 1

**Failure Message:**
```
test timed out after 60000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 60000 milliseconds
	at java.lang.Thread.sleep(Native Method)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.processWaitTimeAndRetryInfo(RetryInvocationHandler.java)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.invokeOnce(RetryInvocationHandler.java)
	at org.apache.hadoop.io.retry.RetryInvocationHandler.invoke(RetryInvocationHandler.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testFederationRMFailoverProxyProviderWithoutFlushFacadeCache`
  - **Position:** `after_rm_active`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `207-org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected_testFederationRMFailoverProxyProviderWithoutFlushFacadeCache__position_after_rm_active__target_resourcemanager__mode_GRACEFUL_`

---

### Group 9: org.junit.runners.model.TestTimedOutException

**Occurrences:** 1

**Failure Message:**
```
test timed out after 60000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 60000 milliseconds
	at java.lang.Thread.sleep(Native Method)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.processWaitTimeAndRetryInfo(RetryInvocationHandler.java)
	at org.apache.hadoop.io.retry.RetryInvocationHandler$Call.invokeOnce(RetryInvocationHandler.java)
	at org.apache.hadoop.io.retry.RetryInvocationHandler.invoke(RetryInvocationHandler.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testFederationRMFailoverProxyProvider`
  - **Position:** `after_rm_active`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `201-org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected_testFederationRMFailoverProxyProvider__position_after_rm_active__target_resourcemanager__mode_GRACEFUL_`

---


## ⚪ LOW PRIORITY - Testing Framework Issues

### Group 10: java.lang.IllegalArgumentException

**Occurrences:** 15

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA(TestResourceTrackerOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (15):**

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_connect`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `271-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_connect__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_heartbeat`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `274-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_heartbeat__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `264-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_heartbeat`
  - **Target:** `nodemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `275-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_heartbeat__target_nodemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_register`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `269-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_register__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_heartbeat`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `277-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_heartbeat__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_heartbeat`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `278-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_heartbeat__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_heartbeat`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `273-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_heartbeat__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_register`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `266-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_register__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_register`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `268-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_register__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_heartbeat`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `276-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_heartbeat__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_register`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `265-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_register__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_register`
  - **Target:** `nodemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `267-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_register__target_nodemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_register`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `270-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_register__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected.testResourceTrackerOnHA`
  - **Position:** `after_nm_connect`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `272-org.apache.hadoop.yarn.client.TestResourceTrackerOnHA_RestartInjected_testResourceTrackerOnHA__position_after_nm_connect__target_resourcemanager__mode_CRASH_`

---

### Group 11: java.lang.IllegalArgumentException

**Occurrences:** 5

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testAllocateOnHA(TestApplicationMasterServiceProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (5):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testAllocateOnHA`
  - **Position:** `after_allocate`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `289-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testAllocateOnHA__position_after_allocate__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testAllocateOnHA`
  - **Position:** `after_allocate`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `290-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testAllocateOnHA__position_after_allocate__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testAllocateOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `286-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testAllocateOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testAllocateOnHA`
  - **Position:** `after_allocate`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `287-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testAllocateOnHA__position_after_allocate__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testAllocateOnHA`
  - **Position:** `after_allocate`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `288-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testAllocateOnHA__position_after_allocate__target_resourcemanager__mode_CRASH_`

---

### Group 12: java.lang.IllegalArgumentException

**Occurrences:** 5

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.testAllocateForTimelineV2OnHA(TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (5):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.testAllocateForTimelineV2OnHA`
  - **Position:** `after_allocate_timeline`
  - **Target:** `nodemanager`
  - **Mode:** `CRASH`
  - **Directory:** `295-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected_testAllocateForTimelineV2OnHA__position_after_allocate_timeline__target_nodemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.testAllocateForTimelineV2OnHA`
  - **Position:** `after_allocate_timeline`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `293-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected_testAllocateForTimelineV2OnHA__position_after_allocate_timeline__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.testAllocateForTimelineV2OnHA`
  - **Position:** `after_allocate_timeline`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `294-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected_testAllocateForTimelineV2OnHA__position_after_allocate_timeline__target_nodemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.testAllocateForTimelineV2OnHA`
  - **Position:** `after_allocate_timeline`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `292-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected_testAllocateForTimelineV2OnHA__position_after_allocate_timeline__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected.testAllocateForTimelineV2OnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `291-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolForTimelineV2_RestartInjected_testAllocateForTimelineV2OnHA__position_null__target_null__mode_null_`

---

### Group 13: java.lang.IllegalArgumentException

**Occurrences:** 4

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testRenewDelegationTokenOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (4):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testRenewDelegationTokenOnHA`
  - **Position:** `after_token_renew`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `260-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testRenewDelegationTokenOnHA__position_after_token_renew__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testRenewDelegationTokenOnHA`
  - **Position:** `after_token_renew`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `259-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testRenewDelegationTokenOnHA__position_after_token_renew__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testRenewDelegationTokenOnHA`
  - **Position:** `after_token_renew`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `258-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testRenewDelegationTokenOnHA__position_after_token_renew__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testRenewDelegationTokenOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `257-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testRenewDelegationTokenOnHA__position_null__target_null__mode_null_`

---

### Group 14: java.lang.IllegalArgumentException

**Occurrences:** 4

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testSubmitApplicationOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (4):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testSubmitApplicationOnHA`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `244-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testSubmitApplicationOnHA__position_after_app_submit__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testSubmitApplicationOnHA`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `246-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testSubmitApplicationOnHA__position_after_app_submit__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testSubmitApplicationOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `243-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testSubmitApplicationOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testSubmitApplicationOnHA`
  - **Position:** `after_app_submit`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `245-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testSubmitApplicationOnHA__position_after_app_submit__target_resourcemanager__mode_CRASH_`

---

### Group 15: java.lang.IllegalArgumentException

**Occurrences:** 4

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testRegisterApplicationMasterOnHA(TestApplicationMasterServiceProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (4):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testRegisterApplicationMasterOnHA`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `281-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testRegisterApplicationMasterOnHA__position_after_am_register__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testRegisterApplicationMasterOnHA`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `280-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testRegisterApplicationMasterOnHA__position_after_am_register__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testRegisterApplicationMasterOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `279-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testRegisterApplicationMasterOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testRegisterApplicationMasterOnHA`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `282-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testRegisterApplicationMasterOnHA__position_after_am_register__target_resourcemanager__mode_DELAYED_CRASH_`

---

### Group 16: java.lang.IllegalArgumentException

**Occurrences:** 4

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetDelegationTokenOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (4):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetDelegationTokenOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `253-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetDelegationTokenOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetDelegationTokenOnHA`
  - **Position:** `after_token_generation`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `254-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetDelegationTokenOnHA__position_after_token_generation__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetDelegationTokenOnHA`
  - **Position:** `after_token_generation`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `256-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetDelegationTokenOnHA__position_after_token_generation__target_resourcemanager__mode_DELAYED_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetDelegationTokenOnHA`
  - **Position:** `after_token_generation`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `255-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetDelegationTokenOnHA__position_after_token_generation__target_resourcemanager__mode_CRASH_`

---

### Group 17: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testCancelDelegationTokenOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testCancelDelegationTokenOnHA`
  - **Position:** `after_token_cancel`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `262-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testCancelDelegationTokenOnHA__position_after_token_cancel__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testCancelDelegationTokenOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `261-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testCancelDelegationTokenOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testCancelDelegationTokenOnHA`
  - **Position:** `after_token_cancel`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `263-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testCancelDelegationTokenOnHA__position_after_token_cancel__target_resourcemanager__mode_CRASH_`

---

### Group 18: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueUserAclsOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueUserAclsOnHA`
  - **Position:** `after_get_queue_acls`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `230-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetQueueUserAclsOnHA__position_after_get_queue_acls__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueUserAclsOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `228-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetQueueUserAclsOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueUserAclsOnHA`
  - **Position:** `after_get_queue_acls`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `229-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetQueueUserAclsOnHA__position_after_get_queue_acls__target_resourcemanager__mode_GRACEFUL_`

---

### Group 19: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueInfoOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueInfoOnHA`
  - **Position:** `after_get_queue_info`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `227-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetQueueInfoOnHA__position_after_get_queue_info__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueInfoOnHA`
  - **Position:** `after_get_queue_info`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `226-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetQueueInfoOnHA__position_after_get_queue_info__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetQueueInfoOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `225-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetQueueInfoOnHA__position_null__target_null__mode_null_`

---

### Group 20: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testFinishApplicationMasterOnHA(TestApplicationMasterServiceProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testFinishApplicationMasterOnHA`
  - **Position:** `after_am_finish`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `285-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testFinishApplicationMasterOnHA__position_after_am_finish__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testFinishApplicationMasterOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `283-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testFinishApplicationMasterOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected.testFinishApplicationMasterOnHA`
  - **Position:** `after_am_finish`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `284-org.apache.hadoop.yarn.client.TestApplicationMasterServiceProtocolOnHA_RestartInjected_testFinishApplicationMasterOnHA__position_after_am_finish__target_resourcemanager__mode_GRACEFUL_`

---

### Group 21: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterMetricsOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterMetricsOnHA`
  - **Position:** `after_get_cluster_metrics`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `218-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetClusterMetricsOnHA__position_after_get_cluster_metrics__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterMetricsOnHA`
  - **Position:** `after_get_cluster_metrics`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `217-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetClusterMetricsOnHA__position_after_get_cluster_metrics__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterMetricsOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `216-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetClusterMetricsOnHA__position_null__target_null__mode_null_`

---

### Group 22: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptReportOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptReportOnHA`
  - **Position:** `after_get_attempt_report`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `232-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationAttemptReportOnHA__position_after_get_attempt_report__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptReportOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `231-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationAttemptReportOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptReportOnHA`
  - **Position:** `after_get_attempt_report`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `233-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationAttemptReportOnHA__position_after_get_attempt_report__target_resourcemanager__mode_CRASH_`

---

### Group 23: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationsOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationsOnHA`
  - **Position:** `after_get_applications`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `221-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationsOnHA__position_after_get_applications__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationsOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `219-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationsOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationsOnHA`
  - **Position:** `after_get_applications`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `220-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationsOnHA__position_after_get_applications__target_resourcemanager__mode_GRACEFUL_`

---

### Group 24: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterNodesOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterNodesOnHA`
  - **Position:** `after_get_node_reports`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `224-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetClusterNodesOnHA__position_after_get_node_reports__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterNodesOnHA`
  - **Position:** `after_get_node_reports`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `223-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetClusterNodesOnHA__position_after_get_node_reports__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetClusterNodesOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `222-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetClusterNodesOnHA__position_null__target_null__mode_null_`

---

### Group 25: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testForceKillApplicationOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testForceKillApplicationOnHA`
  - **Position:** `after_app_kill`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `252-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testForceKillApplicationOnHA__position_after_app_kill__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testForceKillApplicationOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `250-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testForceKillApplicationOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testForceKillApplicationOnHA`
  - **Position:** `after_app_kill`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `251-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testForceKillApplicationOnHA__position_after_app_kill__target_resourcemanager__mode_GRACEFUL_`

---

### Group 26: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptsOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptsOnHA`
  - **Position:** `after_get_attempts`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `236-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationAttemptsOnHA__position_after_get_attempts__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptsOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `234-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationAttemptsOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationAttemptsOnHA`
  - **Position:** `after_get_attempts`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `235-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationAttemptsOnHA__position_after_get_attempts__target_resourcemanager__mode_GRACEFUL_`

---

### Group 27: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainersOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainersOnHA`
  - **Position:** `after_get_containers`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `242-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetContainersOnHA__position_after_get_containers__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainersOnHA`
  - **Position:** `after_get_containers`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `241-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetContainersOnHA__position_after_get_containers__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainersOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `240-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetContainersOnHA__position_null__target_null__mode_null_`

---

### Group 28: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testMoveApplicationAcrossQueuesOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testMoveApplicationAcrossQueuesOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `247-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testMoveApplicationAcrossQueuesOnHA__position_null__target_null__mode_null_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testMoveApplicationAcrossQueuesOnHA`
  - **Position:** `after_move_app_queue`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `249-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testMoveApplicationAcrossQueuesOnHA__position_after_move_app_queue__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testMoveApplicationAcrossQueuesOnHA`
  - **Position:** `after_move_app_queue`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `248-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testMoveApplicationAcrossQueuesOnHA__position_after_move_app_queue__target_resourcemanager__mode_GRACEFUL_`

---

### Group 29: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetNewApplicationOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetNewApplicationOnHA`
  - **Position:** `after_create_app`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `214-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetNewApplicationOnHA__position_after_create_app__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetNewApplicationOnHA`
  - **Position:** `after_create_app`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `215-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetNewApplicationOnHA__position_after_create_app__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetNewApplicationOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `213-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetNewApplicationOnHA__position_null__target_null__mode_null_`

---

### Group 30: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationReportOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationReportOnHA`
  - **Position:** `after_get_app_report`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `211-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationReportOnHA__position_after_get_app_report__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationReportOnHA`
  - **Position:** `after_get_app_report`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `212-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationReportOnHA__position_after_get_app_report__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetApplicationReportOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `210-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetApplicationReportOnHA__position_null__target_null__mode_null_`

---

### Group 31: java.lang.IllegalArgumentException

**Occurrences:** 3

**Failure Message:**
```
No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
```

**Stack Trace (first 5 lines):**
```
java.lang.IllegalArgumentException: No adapter registered or discovered for cluster type: org.apache.hadoop.yarn.client.ProtocolHATestBase$MiniYARNClusterForHATesting. Available adapters: [class org.apache.hadoop.yarn.server.MiniYARNCluster]. Make sure the adapter module is on the classpath and has META-INF/services/org.restarttest.core.ClusterAdapter configured.
	at org.restarttest.core.AdapterRegistry.getAdapterOrDiscover(AdapterRegistry.java)
	at org.restarttest.api.RestartPointBuilder.on(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainerReportOnHA(TestApplicationClientProtocolOnHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (3):**

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainerReportOnHA`
  - **Position:** `after_get_container_report`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `238-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetContainerReportOnHA__position_after_get_container_report__target_resourcemanager__mode_GRACEFUL_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainerReportOnHA`
  - **Position:** `after_get_container_report`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `239-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetContainerReportOnHA__position_after_get_container_report__target_resourcemanager__mode_CRASH_`

- **Test:** `org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected.testGetContainerReportOnHA`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `237-org.apache.hadoop.yarn.client.TestApplicationClientProtocolOnHA_RestartInjected_testGetContainerReportOnHA__position_null__target_null__mode_null_`

---

### Group 32: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testProxyProvider(TestFederationRMFailoverProxyProvider_RestartInjected.java)
	at org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testFederationRMFailoverProxyProviderWithoutFlushFacadeCache(TestFederationRMFailoverProxyProvider_RestartInjected.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testFederationRMFailoverProxyProviderWithoutFlushFacadeCache`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `205-org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected_testFederationRMFailoverProxyProviderWithoutFlushFacadeCache__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 33: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoPreemptionEnabled(TestYarnCLI_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoPreemptionEnabled`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `007-org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected_testGetQueueInfoPreemptionEnabled__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 34: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal(TestAMRMProxy_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyTokenRenewal`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `093-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyTokenRenewal__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 35: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoOverrideIntraQueuePreemption(TestYarnCLI_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoOverrideIntraQueuePreemption`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `002-org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected_testGetQueueInfoOverrideIntraQueuePreemption__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 36: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoPreemptionDisabled(TestYarnCLI_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoPreemptionDisabled`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `010-org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected_testGetQueueInfoPreemptionDisabled__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 37: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testProxyProvider(TestFederationRMFailoverProxyProvider_RestartInjected.java)
	at org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testFederationRMFailoverProxyProvider(TestFederationRMFailoverProxyProvider_RestartInjected.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected.testFederationRMFailoverProxyProvider`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `199-org.apache.hadoop.yarn.client.TestFederationRMFailoverProxyProvider_RestartInjected_testFederationRMFailoverProxyProvider__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 38: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestNoHaRMFailoverProxyProvider_RestartInjected.testRestartedRM(TestNoHaRMFailoverProxyProvider_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestNoHaRMFailoverProxyProvider_RestartInjected.testRestartedRM`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `196-org.apache.hadoop.yarn.client.TestNoHaRMFailoverProxyProvider_RestartInjected_testRestartedRM__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 39: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover(TestRMFailover_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `177-org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected_testAutomaticFailover__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 40: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E(TestAMRMProxy_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testAMRMProxyE2E`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `087-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testAMRMProxyE2E__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 41: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestHedgingRequestRMFailoverProxyProvider_RestartInjected.testHedgingRequestProxyProvider(TestHedgingRequestRMFailoverProxyProvider_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestHedgingRequestRMFailoverProxyProvider_RestartInjected.testHedgingRequestProxyProvider`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `188-org.apache.hadoop.yarn.client.TestHedgingRequestRMFailoverProxyProvider_RestartInjected_testHedgingRequestProxyProvider__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 42: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap(TestAMRMProxy_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected.testE2ETokenSwap`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `099-org.apache.hadoop.yarn.client.api.impl.TestAMRMProxy_RestartInjected_testE2ETokenSwap__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 43: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testDirsFailures(TestDiskFailures_RestartInjected.java)
	at org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures(TestDiskFailures_RestartInjected.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLocalDirsFailures`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `015-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLocalDirsFailures__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 44: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.server.TestMiniYarnCluster_RestartInjected.testTimelineServiceStartInMiniCluster(TestMiniYarnCluster_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.server.TestMiniYarnCluster_RestartInjected.testTimelineServiceStartInMiniCluster`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `002-org.apache.hadoop.yarn.server.TestMiniYarnCluster_RestartInjected_testTimelineServiceStartInMiniCluster__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 45: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testDirsFailures(TestDiskFailures_RestartInjected.java)
	at org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures(TestDiskFailures_RestartInjected.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected.testLogDirsFailures`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `025-org.apache.hadoop.yarn.server.TestDiskFailures_RestartInjected_testLogDirsFailures__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 46: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_start
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_start
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.setup(TestUnmanagedAMLauncher_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected`
  - **Position:** `after_cluster_start`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `002-org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected_testUMALauncher__position_after_cluster_start__target_resourcemanager__mode_GRACEFUL_`

---

### Group 47: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_am_register
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_am_register
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient(TestNMClient_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClient`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `114-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClient__position_after_am_register__target_resourcemanager__mode_GRACEFUL_`

---

### Group 48: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_am_register
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_am_register
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop(TestNMClient_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected.testNMClientNoCleanupOnStop`
  - **Position:** `after_am_register`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `104-org.apache.hadoop.yarn.client.api.impl.TestNMClient_RestartInjected_testNMClientNoCleanupOnStop__position_after_am_register__target_resourcemanager__mode_GRACEFUL_`

---

### Group 49: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_queue_status_check
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_queue_status_check
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoOverrideIntraQueuePreemption(TestYarnCLI_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected.testGetQueueInfoOverrideIntraQueuePreemption`
  - **Position:** `after_queue_status_check`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `004-org.apache.hadoop.yarn.client.cli.TestYarnCLI_RestartInjected_testGetQueueInfoOverrideIntraQueuePreemption__position_after_queue_status_check__target_resourcemanager__mode_GRACEFUL_`

---

### Group 50: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_failover
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_failover
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testWebAppProxyInStandAloneMode(TestRMFailover_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testWebAppProxyInStandAloneMode`
  - **Position:** `after_failover`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `185-org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected_testWebAppProxyInStandAloneMode__position_after_failover__target_resourcemanager__mode_GRACEFUL_`

---

### Group 51: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_first_failover
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_first_failover
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testExplicitFailover(TestRMFailover_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testExplicitFailover`
  - **Position:** `after_first_failover`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `173-org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected_testExplicitFailover__position_after_first_failover__target_resourcemanager__mode_GRACEFUL_`

---

### Group 52: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_first_failover
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_first_failover
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover(TestRMFailover_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected.testAutomaticFailover`
  - **Position:** `after_first_failover`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `179-org.apache.hadoop.yarn.client.TestRMFailover_RestartInjected_testAutomaticFailover__position_after_first_failover__target_resourcemanager__mode_GRACEFUL_`

---

### Group 53: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position before_nm_connect
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position before_nm_connect
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.server.TestMiniYARNClusterForHA_RestartInjected.testClusterWorks(TestMiniYARNClusterForHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.server.TestMiniYARNClusterForHA_RestartInjected.testClusterWorks`
  - **Position:** `before_nm_connect`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `005-org.apache.hadoop.yarn.server.TestMiniYARNClusterForHA_RestartInjected_testClusterWorks__position_before_nm_connect__target_resourcemanager__mode_GRACEFUL_`

---

### Group 54: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_nm_connect
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_nm_connect
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.server.TestMiniYARNClusterForHA_RestartInjected.testClusterWorks(TestMiniYARNClusterForHA_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.server.TestMiniYARNClusterForHA_RestartInjected.testClusterWorks`
  - **Position:** `after_nm_connect`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `007-org.apache.hadoop.yarn.server.TestMiniYARNClusterForHA_RestartInjected_testClusterWorks__position_after_nm_connect__target_nodemanager__mode_GRACEFUL_`

---

### Group 55: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_nm_connect
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_nm_connect
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.server.TestMiniYarnClusterNodeUtilization_RestartInjected.testUpdateNodeUtilization(TestMiniYarnClusterNodeUtilization_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.server.TestMiniYarnClusterNodeUtilization_RestartInjected.testUpdateNodeUtilization`
  - **Position:** `after_nm_connect`
  - **Target:** `nodemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `011-org.apache.hadoop.yarn.server.TestMiniYarnClusterNodeUtilization_RestartInjected_testUpdateNodeUtilization__position_after_nm_connect__target_nodemanager__mode_GRACEFUL_`

---

### Group 56: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_app_create
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_app_create
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded(TestCleanupAfterKill_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded`
  - **Position:** `after_app_create`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `004-org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected_testRegistryCleanedOnLifetimeExceeded__position_after_app_create__target_resourcemanager__mode_GRACEFUL_`

---

### Group 57: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded(TestCleanupAfterKill_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected.testRegistryCleanedOnLifetimeExceeded`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `002-org.apache.hadoop.yarn.service.TestCleanupAfterKill_RestartInjected_testRegistryCleanedOnLifetimeExceeded__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 58: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.runAMSignalTest(TestYarnNativeServices_RestartInjected.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testAMSigkillDoesNotKillApplication(TestYarnNativeServices_RestartInjected.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testAMSigkillDoesNotKillApplication`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `046-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testAMSigkillDoesNotKillApplication__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 59: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testComponentStartOrder(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testComponentStartOrder`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `010-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testComponentStartOrder__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 60: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateServiceWithPlacementPolicy(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateServiceWithPlacementPolicy`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `038-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testCreateServiceWithPlacementPolicy__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 61: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testRestartServiceForNonExistingInRM(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testRestartServiceForNonExistingInRM`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `054-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testRestartServiceForNonExistingInRM__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 62: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.runAMSignalTest(TestYarnNativeServices_RestartInjected.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testAMSigtermDoesNotKillApplication(TestYarnNativeServices_RestartInjected.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testAMSigtermDoesNotKillApplication`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `042-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testAMSigtermDoesNotKillApplication__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 63: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testExpressUpgrade(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testExpressUpgrade`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `030-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testExpressUpgrade__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 64: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateServiceSameNameSameUser(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateServiceSameNameSameUser`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `018-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testCreateServiceSameNameSameUser__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 65: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testUpgrade(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testUpgrade`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `026-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testUpgrade__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 66: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testAMFailureValidity(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testAMFailureValidity`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `058-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testAMFailureValidity__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 67: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testServiceSameNameWithFailure(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testServiceSameNameWithFailure`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `062-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testServiceSameNameWithFailure__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 68: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCancelUpgrade(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCancelUpgrade`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `034-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testCancelUpgrade__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 69: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateFlexStopDestroyService(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateFlexStopDestroyService`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `002-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testCreateFlexStopDestroyService__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 70: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.component.TestComponentDecommissionInstances_RestartInjected.testDecommissionInstances(TestComponentDecommissionInstances_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.component.TestComponentDecommissionInstances_RestartInjected.testDecommissionInstances`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `066-org.apache.hadoop.yarn.service.component.TestComponentDecommissionInstances_RestartInjected_testDecommissionInstances__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 71: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateServiceSameNameDifferentUser(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testCreateServiceSameNameDifferentUser`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `014-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testCreateServiceSameNameDifferentUser__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 72: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testComponentHealthThresholdMonitor(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testComponentHealthThresholdMonitor`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `050-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testComponentHealthThresholdMonitor__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 73: org.restarttest.core.RestartException

**Occurrences:** 1

**Failure Message:**
```
Restart failed at position after_cluster_setup
```

**Stack Trace (first 5 lines):**
```
org.restarttest.core.RestartException: Restart failed at position after_cluster_setup
	at org.restarttest.core.RestartExecutor.execute(RestartExecutor.java)
	at org.restarttest.api.RestartPointBuilder.execute(RestartPointBuilder.java)
	at org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testStopDestroySavedService(TestYarnNativeServices_RestartInjected.java)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected.testStopDestroySavedService`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `006-org.apache.hadoop.yarn.service.TestYarnNativeServices_RestartInjected_testStopDestroySavedService__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 74: org.junit.runners.model.TestTimedOutException

**Occurrences:** 1

**Failure Message:**
```
test timed out after 30000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 30000 milliseconds
	at java.lang.Thread.sleep(Native Method)
	at org.apache.hadoop.yarn.server.MiniYARNCluster.waitForNodeManagersToConnect(MiniYARNCluster.java)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.testUMALauncher`
  - **Position:** `after_launcher_run`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `006-org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected_testUMALauncher__position_after_launcher_run__target_resourcemanager__mode_GRACEFUL_`

---

### Group 75: org.junit.runners.model.TestTimedOutException

**Occurrences:** 1

**Failure Message:**
```
test timed out after 30000 milliseconds
```

**Stack Trace (first 5 lines):**
```
org.junit.runners.model.TestTimedOutException: test timed out after 30000 milliseconds
	at java.lang.Thread.sleep(Native Method)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java)
	at org.restarttest.adapter.yarn.YarnClusterAdapter.waitActive(YarnClusterAdapter.java)
	at org.restarttest.core.RestartExecutor.executeRestart(RestartExecutor.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected.testUMALauncher`
  - **Position:** `after_launcher_init`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `004-org.apache.hadoop.yarn.applications.unmanagedamlauncher.TestUnmanagedAMLauncher_RestartInjected_testUMALauncher__position_after_launcher_init__target_resourcemanager__mode_GRACEFUL_`

---

### Group 76: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2348051515400935223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithoutDomain`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `089-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithoutDomain__position_null__target_null__mode_null_`

---

### Group 77: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4486900253383092152.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithoutDomain`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `092-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithoutDomain__position_after_cluster_setup__target_resourcemanager__mode_DELAYED_CRASH_`

---

### Group 78: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-7022427883638967234.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithDomain`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `086-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithDomain__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 79: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-5300998488049681604.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithoutDomain`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `091-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithoutDomain__position_after_cluster_setup__target_resourcemanager__mode_CRASH_`

---

### Group 80: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-8187205234736239863.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithDomain`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `CRASH`
  - **Directory:** `087-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithDomain__position_after_cluster_setup__target_resourcemanager__mode_CRASH_`

---

### Group 81: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-2335432484588377223.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithDomain`
  - **Position:** `null`
  - **Target:** `null`
  - **Mode:** `null`
  - **Directory:** `085-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithDomain__position_null__target_null__mode_null_`

---

### Group 82: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-1465061319269077705.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithoutDomain`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `GRACEFUL`
  - **Directory:** `090-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithoutDomain__position_after_cluster_setup__target_resourcemanager__mode_GRACEFUL_`

---

### Group 83: java.lang.UnsatisfiedLinkError

**Occurrences:** 1

**Failure Message:**
```
Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
```

**Stack Trace (first 5 lines):**
```
java.lang.UnsatisfiedLinkError: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8: dlopen(/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8, 0x0001): tried: '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64')), '/System/Volumes/Preboot/Cryptexes/OS/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8' (no such file), '/private/var/folders/0l/qfnj5yxj18x1hnk332p4wntw0000gn/T/libleveldbjni-64-1-4226174453931570623.8' (fat file, but missing compatible architecture (have 'x86_64,i386', need 'arm64e' or 'arm64e.v1' or 'arm64' or 'arm64'))]
	at org.fusesource.hawtjni.runtime.Library.doLoad(Library.java)
	at org.fusesource.hawtjni.runtime.Library.load(Library.java)
	at org.fusesource.leveldbjni.JniDBFactory.<clinit>(JniDBFactory.java)
	at org.apache.hadoop.yarn.server.timeline.LeveldbTimelineStore.serviceInit(LeveldbTimelineStore.java)
```

**Affected Tests (1):**

- **Test:** `org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected.testDSShellWithDomain`
  - **Position:** `after_cluster_setup`
  - **Target:** `resourcemanager`
  - **Mode:** `DELAYED_CRASH`
  - **Directory:** `088-org.apache.hadoop.yarn.applications.distributedshell.TestDSTimelineV15_RestartInjected_testDSShellWithDomain__position_after_cluster_setup__target_resourcemanager__mode_DELAYED_CRASH_`

---

