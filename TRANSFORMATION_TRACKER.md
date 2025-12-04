# YARN Cluster Tests Transformation Tracker

**Goal**: Transform all MiniYARNCluster tests to support restart functionality

**Started**: 2025-12-02

---

## Progress Overview

- **Total Test Files**: 33
- **Completed**: 33
- **In Progress**: 0
- **Not Started**: 0

---

## Transformation Status
- [ ] Not Started
- [x] Finished

### HADOOP-YARN-CLIENT MODULE (20 files)

| Status | Test | Note |
|--------|------|-----|
| [x]    | TestYarnCLI.java | Transformed 3 queue preemption tests with restart injection |
| [x]    | TestYarnClient.java | Transformed 2 application submission tests with restart injection |
| [x]    | TestYarnClientWithReservation.java | Transformed 7 reservation tests with restart injection |
| [x]    | TestOpportunisticContainerAllocationE2E.java | Transformed 4 opportunistic container allocation tests with restart injection |
| [x]    | TestAMRMProxy.java | Transformed 3 AMRM proxy tests with restart injection |
| [x]    | TestNMClient.java | Transformed 2 NM client tests with restart injection |
| [x]    | TestAMRMClient.java | Transformed 4 AMRM client tests with restart injection |
| [x]    | TestAMRMClientPlacementConstraints.java | Transformed 2 placement constraint tests with restart injection |
| [ ]    | BaseAMRMClientTest.java | Base class |
| [ ]    | BaseAMRMProxyE2ETest.java | Base class |
| [x]    | TestRMFailover.java | Transformed 3 RM failover tests with restart injection |
| [x]    | TestHedgingRequestRMFailoverProxyProvider.java | Transformed 1 hedging request proxy test with restart injection |
| [x]    | TestNoHaRMFailoverProxyProvider.java | Transformed 1 no-HA proxy test with restart injection |
| [x]    | TestFederationRMFailoverProxyProvider.java | Transformed 2 federation proxy tests with restart injection |
| [ ]    | ProtocolHATestBase.java | Base class |
| [x]    | TestApplicationClientProtocolOnHA.java | Transformed 17 protocol tests with restart injection |
| [x]    | TestResourceTrackerOnHA.java | Transformed 1 resource tracker test with restart injection |
| [ ]    | ApplicationMasterServiceProtoTestBase.java | Intermediate base class |
| [x]    | TestApplicationMasterServiceProtocolOnHA.java | Transformed 3 ApplicationMaster protocol tests with restart injection |
| [x]    | TestApplicationMasterServiceProtocolForTimelineV2.java | Transformed 1 timeline v2 protocol test with restart injection |

### HADOOP-YARN-SERVER-TESTS MODULE (5 files)

| Status | Test | Note |
|--------|------|------|
| [x] | TestMiniYarnCluster.java | Transformed 1 mini cluster test with restart injection |
| [x] | TestMiniYARNClusterForHA.java | Transformed 1 HA cluster test with restart injection |
| [x] | TestMiniYarnClusterNodeUtilization.java | Transformed 1 node utilization test with restart injection |
| [x] | TestDiskFailures.java | Transformed 2 disk failure tests with restart injection |
| [x] | TestContainerManagerSecurity.java | Transformed 1 container security test with restart injection |

### HADOOP-YARN-APPLICATIONS-DISTRIBUTEDSHELL MODULE (4 files)

| Status | Test | Note |
|--------|------|------|
| [x] | DistributedShellBaseTest.java | Transformed base class with restart injection in setupInternal |
| [x] | TestDSTimelineV10.java | Transformed 21+ timeline v1.0 tests with restart injection |
| [x] | TestDSTimelineV15.java | Transformed timeline v1.5 tests with restart injection |
| [x] | TestDSTimelineV20.java | Transformed timeline v2.0 tests with restart injection |

### HADOOP-YARN-APPLICATIONS-UNMANAGED-AM-LAUNCHER MODULE (1 file)

| Status | Test | Note |
|--------|------|------|
| [x] | TestUnmanagedAMLauncher.java | Transformed 1 unmanaged AM launcher test with restart injection |

### HADOOP-YARN-SERVICES MODULE (7 files)

| Status | Test | Note |
|--------|------|------|
| [x] | ServiceTestUtils.java | Base helper class (no test methods, extended by transformed tests) |
| [x] | TestCleanupAfterKill.java | Transformed 1 registry cleanup test with restart injection |
| [x] | TestServiceAM.java | Transformed (unit test with mocks, no cluster restart points) |
| [x] | TestYarnNativeServices.java | Transformed 16 service lifecycle tests with restart injection |
| [x] | TestServiceApiUtil.java | Transformed (unit test, no cluster restart points) |
| [x] | TestComponentDecommissionInstances.java | Transformed 1 component decommission test with restart injection |
| [x] | TestServiceMonitor.java | Transformed (unit test with MockServiceAM, no cluster restart points) |

---

## Transformation Guidelines

1. **Replace MiniYARNCluster with restart-enabled adapter**
2. **Update setup/teardown methods** to use the new adapter
3. **Add restart test cases** where applicable
4. **Verify all existing tests pass** with the new adapter
5. **Update base classes first** to minimize duplicate work for subclasses

---

## Notes

- Base classes should be prioritized to avoid redundant work on subclasses
- Some tests (HA-related tests) may already have restart logic that needs to be adapted
- The restart-yarn-adapter should be used as the reference implementation

---

## Completed Transformations

_(None yet)_
