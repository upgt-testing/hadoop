# Prompt: Transform YARN Mini Cluster Test with Restart Position Injection

## Objective

Transform an existing YARN mini cluster test to inject restart positions for distributed system restart testing. The transformation will generate:
1. A new test file with `_RestartInjected` suffix
2. A restart configuration file for the Maven plugin

## Input

- **Test File Path**: Path to the original test file (e.g., `/path/to/TestYarnApp.java`)
- **Test Class**: Fully-qualified class name (e.g., `org.apache.hadoop.yarn.server.TestYarnApp`)

## Output

1. **Generated Test File**: `{OriginalFileName}_RestartInjected.java` at the same directory as the input file
2. **Restart Configuration**: `restart-config.json` in the `restarts-config/` directory under the same module directory as the test file, if not exist create it.

## Transformation Instructions

### Step 1: Analyze the Original Test

Read the input test file and identify:

1. **Cluster Setup**: Find the YARN cluster instance variable (e.g., `MiniYARNCluster yarnCluster`)
2. **Test Methods**: Identify all `@Test` annotated methods
3. **Critical Operations**: Look for operations that involve state transitions, such as:
    - Application lifecycle: `submitApplication()`, `startApp()`, `killApplication()`, `finishApplication()`
    - Container operations: `allocateContainer()`, `launchContainer()`, `stopContainer()`
    - Resource allocation: `addResourceRequest()`, `allocate()`, `updateResourceRequest()`
    - State transitions: Application state changes, container state changes
    - Heartbeat operations: `nodeHeartbeat()`, `registerNodeManager()`, `unregisterNodeManager()`
    - Token operations: Token generation, renewal, validation
    - Queue operations: Submit to queue, queue updates, capacity changes

### Step 2: Identify Restart Points

For each test method, identify potential restart points based on these criteria:

**Good Restart Points** (inject here):
- After application submission but before launch
- After container allocation but before launch
- During application running (mid-execution)
- After heartbeat operations
- Before application finish/kill
- After node registration
- During resource allocation/negotiation
- After state persistence operations (e.g., state store updates)
- During token operations
- After queue operations

**Poor Restart Points** (avoid):
- Before cluster setup (no cluster exists yet)
- After cluster teardown (cluster already destroyed)
- During trivial operations (simple getters with no state changes)
- Operations that are too fast to test meaningful state

**Naming Convention for Restart Positions**:
- Use descriptive, lowercase names with underscores
- Pattern: `{operation}_{context}`
- Examples:
    - `after_app_submit`
    - `after_container_allocate`
    - `during_app_running`
    - `before_app_finish`
    - `after_node_register`
    - `after_heartbeat`
    - `after_resource_request`
    - `after_token_generation`
    - `before_container_launch`

### Step 3: Generate the Restart-Injected Test File

Create a new test file with the following transformations:

#### 3.1 Package and Imports

```java
// Keep original package declaration
package org.apache.hadoop.yarn.server;

// Add these imports at the top (if not already present)
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

// Keep all original imports
```

#### 3.2 Class Declaration

```java
// Original class name: TestYarnApp
// New class name: TestYarnApp_RestartInjected
public class TestYarnApp_RestartInjected {
    // Keep all original fields and variables
}
```

#### 3.3 Cluster Setup and Teardown

Keep the `@Before` and `@After` methods unchanged:

```java
@Before
public void setUp() throws Exception {
    // Keep original setup code unchanged
}

@After
public void tearDown() throws Exception {
    // Keep original teardown code unchanged
}
```

#### 3.4 Transform Test Methods

For each `@Test` method, apply the following transformations:

**Original Test Method**:
```java
@Test
public void testApplicationSubmission() throws Exception {
    YarnClient client = YarnClient.createYarnClient();
    client.init(conf);
    client.start();

    ApplicationSubmissionContext appContext = client.createApplication().getApplicationSubmissionContext();
    ApplicationId appId = appContext.getApplicationId();

    client.submitApplication(appContext);

    ApplicationReport report = client.getApplicationReport(appId);
    assertEquals(YarnApplicationState.ACCEPTED, report.getYarnApplicationState());

    client.killApplication(appId);
    client.stop();
}
```

**Transformed Test Method**:
```java
@Test
public void testApplicationSubmission() throws Exception {
    YarnClient client = YarnClient.createYarnClient();
    client.init(conf);
    client.start();

    ApplicationSubmissionContext appContext = client.createApplication().getApplicationSubmissionContext();
    ApplicationId appId = appContext.getApplicationId();

    client.submitApplication(appContext);

    // RESTART POINT 1: after_app_submit
    RestartFramework.at("after_app_submit")
        .on(yarnCluster)  // Use the YARN cluster instance from setUp()
        .restart("resourcemanager")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    ApplicationReport report = client.getApplicationReport(appId);
    assertEquals(YarnApplicationState.ACCEPTED, report.getYarnApplicationState());

    // RESTART POINT 2: before_app_kill
    RestartFramework.at("before_app_kill")
        .on(yarnCluster)
        .restart("resourcemanager")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    client.killApplication(appId);
    client.stop();
}
```

**Injection Pattern**:

1. **After Application Operations**:
   ```java
   client.submitApplication(appContext);

   // Inject restart point
   RestartFramework.at("after_app_submit")
       .on(yarnCluster)
       .restart("resourcemanager")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();
   ```

2. **After Container Operations**:
   ```java
   allocateResponse = rmClient.allocate(allocateRequest);

   // Inject restart point
   RestartFramework.at("after_container_allocate")
       .on(yarnCluster)
       .restart("nodemanager")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();
   ```

3. **Before Critical Operations**:
   ```java
   // Inject restart point before kill
   RestartFramework.at("before_app_kill")
       .on(yarnCluster)
       .restart("resourcemanager")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();

   client.killApplication(appId);
   ```

4. **During Long Operations**:
   ```java
   // Start application
   launchAM(appId);

   // Wait for running state
   waitForState(appId, YarnApplicationState.RUNNING);

   // Inject restart during execution
   RestartFramework.at("during_app_running")
       .on(yarnCluster)
       .restart("nodemanager")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();

   // Continue execution
   waitForCompletion(appId);
   ```

#### 3.5 YARN-Specific Node Roles

YARN has two primary node types:

- **`resourcemanager`** (or `"rm"`): The master node that manages resource allocation and application lifecycle
- **`nodemanager`** (or `"nm"`): Worker nodes that run containers and report to ResourceManager

**Default Restart Configuration**:
Use these defaults for all injected restart points:
- **For application/resource operations**: `"resourcemanager"` (manages app state, scheduling)
- **For container/execution operations**: `"nodemanager"` (executes containers)
- **For cluster-wide operations**: Both `"resourcemanager"` and `"nodemanager"`
- **Node Index**: `0` (first node)
- **Restart Mode**: `RestartMode.GRACEFUL` (default, safest)

#### 3.6 Node Role Selection Guidelines

| Operation Type | Primary Node Role | Secondary Node Role | Reason |
|----------------|-------------------|---------------------|--------|
| Application submission | `resourcemanager` | - | RM manages app lifecycle |
| Application state changes | `resourcemanager` | - | RM tracks app state |
| Container allocation | `resourcemanager` | `nodemanager` | RM allocates, NM executes |
| Container execution | `nodemanager` | - | NM runs containers |
| Heartbeat operations | `nodemanager` | `resourcemanager` | NM sends, RM receives |
| Resource requests | `resourcemanager` | - | RM handles scheduling |
| Token operations | `resourcemanager` | - | RM generates tokens |
| Node registration | `nodemanager` | `resourcemanager` | NM registers with RM |
| Queue operations | `resourcemanager` | - | RM manages queues |

### Step 4: Generate Restart Configuration File

Create `restarts-config/restart-config.json` with the following structure:

```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.yarn.server.TestYarnApp_RestartInjected",
      "testMethod": "testApplicationSubmission",
      "restartPoints": [
        {
          "position": "after_app_submit",
          "targets": ["resourcemanager"],
          "modes": ["GRACEFUL", "CRASH", "DELAYED_CRASH"]
        },
        {
          "position": "before_app_kill",
          "targets": ["resourcemanager"],
          "modes": ["GRACEFUL", "CRASH"]
        }
      ]
    }
  ]
}
```

#### Configuration Generation Rules

For each test method in the transformed test:

1. **Create a test specification** with:
    - `testClass`: The fully-qualified name of the generated test class
    - `testMethod`: The test method name (same as original)
    - `restartPoints`: Array of restart point configurations

2. **For each restart point** injected in the test method:
    - `position`: The position identifier used in `.at("...")`
    - `targets`: Array of node roles to test
    - `modes`: Array of restart modes to test

#### Target Selection for YARN Operations

**ResourceManager-only operations** (`["resourcemanager"]`):
- Application submission, kill, finish
- Resource allocation decisions
- Scheduling operations
- Queue management
- Token generation/renewal
- Application state tracking

**NodeManager-only operations** (`["nodemanager"]`):
- Container execution
- Local resource management
- Container logs
- Node health status

**Both ResourceManager and NodeManager** (`["resourcemanager", "nodemanager"]`):
- Container allocation (RM allocates, NM executes)
- Application execution (RM coordinates, NM runs)
- Heartbeat operations (NM sends, RM receives)
- Node registration/unregistration
- Distributed application workflows

#### Mode Selection Guidelines

- **`["GRACEFUL"]`**: Basic test, verify restart works
    - Use for: Initial testing, simple state transitions

- **`["GRACEFUL", "CRASH"]`**: Standard test, verify crash recovery
    - Use for: Application lifecycle, container operations, resource allocation

- **`["GRACEFUL", "CRASH", "DELAYED_CRASH"]`**: Advanced test, verify timing-sensitive operations
    - Use for: Heartbeat operations, state persistence, token operations, distributed coordination

### Step 5: File Placement

1. **Generated Test File**:
    - Location: Same directory as original test file
    - Name: `{OriginalClassName}_RestartInjected.java`
    - Example: `TestYarnApp.java` → `TestYarnApp_RestartInjected.java`

2. **Restart Configuration**:
    - Location: `restarts-config/` directory under the same module directory as the test file
    - Name: `restart-config.json`
    - If file exists, append to the `tests` array (avoid duplicates)
    - If file doesn't exist, create new file

## Example Transformation

### Input: `TestContainerAllocation.java`

```java
package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMApp;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestContainerAllocation {
    private MiniYARNCluster yarnCluster;
    private YarnConfiguration conf;

    @Before
    public void setUp() throws Exception {
        conf = new YarnConfiguration();
        yarnCluster = new MiniYARNCluster("test", 1, 1, 1);
        yarnCluster.init(conf);
        yarnCluster.start();
    }

    @After
    public void tearDown() throws Exception {
        if (yarnCluster != null) {
            yarnCluster.stop();
            yarnCluster.close();
        }
    }

    @Test
    public void testContainerAllocateAndLaunch() throws Exception {
        // Submit application
        RMApp app = submitApplication();
        ApplicationAttemptId attemptId = app.getCurrentAppAttempt().getAppAttemptId();

        // Request container
        AllocateRequest allocRequest = createAllocateRequest(attemptId);
        AllocateResponse allocResponse = rmClient.allocate(allocRequest);

        // Get allocated container
        List<Container> containers = allocResponse.getAllocatedContainers();
        assertEquals(1, containers.size());
        Container container = containers.get(0);

        // Launch container
        nmClient.startContainer(container, containerLaunchContext);

        // Verify container is running
        ContainerStatus status = nmClient.getContainerStatus(container.getId());
        assertEquals(ContainerState.RUNNING, status.getState());
    }
}
```

### Output 1: `TestContainerAllocation_RestartInjected.java`

```java
package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMApp;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import static org.junit.Assert.*;

public class TestContainerAllocation_RestartInjected {
    private MiniYARNCluster yarnCluster;
    private YarnConfiguration conf;

    @Before
    public void setUp() throws Exception {
        conf = new YarnConfiguration();
        yarnCluster = new MiniYARNCluster("test", 1, 1, 1);
        yarnCluster.init(conf);
        yarnCluster.start();
    }

    @After
    public void tearDown() throws Exception {
        if (yarnCluster != null) {
            yarnCluster.stop();
            yarnCluster.close();
        }
    }

    @Test
    public void testContainerAllocateAndLaunch() throws Exception {
        // Submit application
        RMApp app = submitApplication();
        ApplicationAttemptId attemptId = app.getCurrentAppAttempt().getAppAttemptId();

        // RESTART POINT 1: after_app_submit
        RestartFramework.at("after_app_submit")
            .on(yarnCluster)
            .restart("resourcemanager")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Request container
        AllocateRequest allocRequest = createAllocateRequest(attemptId);
        AllocateResponse allocResponse = rmClient.allocate(allocRequest);

        // RESTART POINT 2: after_container_allocate
        RestartFramework.at("after_container_allocate")
            .on(yarnCluster)
            .restart("resourcemanager")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Get allocated container
        List<Container> containers = allocResponse.getAllocatedContainers();
        assertEquals(1, containers.size());
        Container container = containers.get(0);

        // RESTART POINT 3: before_container_launch
        RestartFramework.at("before_container_launch")
            .on(yarnCluster)
            .restart("nodemanager")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Launch container
        nmClient.startContainer(container, containerLaunchContext);

        // RESTART POINT 4: after_container_launch
        RestartFramework.at("after_container_launch")
            .on(yarnCluster)
            .restart("nodemanager")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Verify container is running
        ContainerStatus status = nmClient.getContainerStatus(container.getId());
        assertEquals(ContainerState.RUNNING, status.getState());
    }
}
```

### Output 2: `restarts-config/restart-config.json`

```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.yarn.server.resourcemanager.TestContainerAllocation_RestartInjected",
      "testMethod": "testContainerAllocateAndLaunch",
      "restartPoints": [
        {
          "position": "after_app_submit",
          "targets": ["resourcemanager"],
          "modes": ["GRACEFUL", "CRASH", "DELAYED_CRASH"]
        },
        {
          "position": "after_container_allocate",
          "targets": ["resourcemanager", "nodemanager"],
          "modes": ["GRACEFUL", "CRASH"]
        },
        {
          "position": "before_container_launch",
          "targets": ["nodemanager"],
          "modes": ["GRACEFUL", "CRASH"]
        },
        {
          "position": "after_container_launch",
          "targets": ["nodemanager"],
          "modes": ["GRACEFUL", "CRASH", "DELAYED_CRASH"]
        }
      ]
    }
  ]
}
```

## YARN-Specific Patterns

### Pattern 1: Application Lifecycle Testing

```java
@Test
public void testAppLifecycle() throws Exception {
    // Submit
    ApplicationId appId = submitApp();

    RestartFramework.at("after_submit")
        .on(yarnCluster)
        .restart("resourcemanager")
        .execute();

    // Running
    waitForState(appId, RUNNING);

    RestartFramework.at("during_running")
        .on(yarnCluster)
        .restart("resourcemanager")
        .execute();

    // Finish
    RestartFramework.at("before_finish")
        .on(yarnCluster)
        .restart("resourcemanager")
        .execute();

    finishApp(appId);
}
```

### Pattern 2: Container Lifecycle Testing

```java
@Test
public void testContainerLifecycle() throws Exception {
    // Allocate
    Container container = allocateContainer();

    RestartFramework.at("after_allocate")
        .on(yarnCluster)
        .restart("resourcemanager")
        .execute();

    // Launch
    launchContainer(container);

    RestartFramework.at("after_launch")
        .on(yarnCluster)
        .restart("nodemanager")
        .execute();

    // Complete
    waitForCompletion(container.getId());
}
```

### Pattern 3: Heartbeat and Registration Testing

```java
@Test
public void testNodeManagerHeartbeat() throws Exception {
    // Register NodeManager
    registerNodeManager();

    RestartFramework.at("after_nm_register")
        .on(yarnCluster)
        .restart("nodemanager")
        .execute();

    // Send heartbeat
    sendHeartbeat();

    RestartFramework.at("after_heartbeat")
        .on(yarnCluster)
        .restart("resourcemanager")
        .execute();

    // Verify registration persists
    verifyNodeRegistered();
}
```

### Pattern 4: Multi-Node Testing

```java
@Test
public void testMultiNodeAllocation() throws Exception {
    // Allocate containers on different nodes
    List<Container> containers = allocateContainersOnDifferentNodes(3);

    // Restart random NodeManager
    RestartFramework.at("after_allocate")
        .on(yarnCluster)
        .restart("nodemanager")
        .withIndex("random")
        .execute();

    // Verify all containers still accessible
    verifyContainers(containers);
}
```

## Validation Checklist

After transformation, verify:

- [ ] Generated test file compiles without errors
- [ ] All original test logic is preserved
- [ ] Restart points are placed at meaningful YARN operations
- [ ] Restart position names are descriptive and YARN-specific
- [ ] Node roles (resourcemanager/nodemanager) are correctly chosen
- [ ] Configuration file has correct fully-qualified class names
- [ ] Configuration file includes all restart points from the test
- [ ] Target arrays match the operation type (RM vs NM vs both)
- [ ] Mode arrays are appropriate for timing sensitivity
- [ ] Files are placed in correct locations
- [ ] Original test file is not modified (only new files created)

## Advanced Scenarios

### Multiple Test Methods

If the original test has multiple `@Test` methods:

1. Transform each method independently
2. Inject restart points in each method
3. Create a separate test specification for each method in the configuration

Example configuration:
```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.yarn.server.TestYarn_RestartInjected",
      "testMethod": "testAppSubmit",
      "restartPoints": [...]
    },
    {
      "testClass": "org.apache.hadoop.yarn.server.TestYarn_RestartInjected",
      "testMethod": "testContainerAllocation",
      "restartPoints": [...]
    }
  ]
}
```

### Helper Methods

If the test has helper methods:

1. **Do not inject restart points in helper methods**
2. Only inject in `@Test` annotated methods
3. Keep helper methods unchanged

### High Availability (HA) Configurations

For tests with HA ResourceManager:

```java
// Restart active RM
RestartFramework.at("after_failover")
    .on(yarnCluster)
    .restart("resourcemanager")
    .withIndex(0)  // Active RM
    .execute();

// Or restart all RMs
RestartFramework.at("restart_all_rm")
    .on(yarnCluster)
    .restart("resourcemanager")
    .withIndex("all")
    .execute();
```

Configuration:
```json
{
  "position": "after_failover",
  "targets": ["resourcemanager"],
  "modes": ["GRACEFUL", "CRASH"]
}
```

### Tests Without Obvious Restart Points

If a test has no clear state transitions:

1. Inject restart points within the range of (a) after cluster setup and (b) before cluster teardown
2. Evenly distribute restart points to cover the test execution
3. You MUST Use percentage-based positions (e.g., `at_25_percent`, `at_50_percent`) to at least cover 4 points during the test execution
4. Find cluster operations to place restart points around

**IMPORTANT**: You are NOT allowed to skip any test transformation due to lack of restart points. Always inject at least one restart point per test method.


## Common YARN Operations and Suggested Restart Points

| YARN Operation | Suggested Restart Point Name | Node Role | Timing |
|----------------|------------------------------|-----------|--------|
| `submitApplication()` | `after_app_submit` | `resourcemanager` | After |
| `killApplication()` | `before_app_kill` | `resourcemanager` | Before |
| `allocate()` (containers) | `after_container_allocate` | `resourcemanager` | After |
| `startContainer()` | `before_container_launch`, `after_container_launch` | `nodemanager` | Before/After |
| `stopContainer()` | `before_container_stop` | `nodemanager` | Before |
| `nodeHeartbeat()` | `after_heartbeat` | `nodemanager` | After |
| `registerApplicationMaster()` | `after_am_register` | `resourcemanager` | After |
| `finishApplicationMaster()` | `before_am_finish` | `resourcemanager` | Before |
| Application state = RUNNING | `during_app_running` | `resourcemanager` or `nodemanager` | During |
| Token generation | `after_token_generation` | `resourcemanager` | After |
| Queue operations | `after_queue_update` | `resourcemanager` | After |

## Notes

- **Non-invasive**: Original test file is never modified
- **Incremental**: Can transform tests one at a time
- **Compatible**: Generated tests can run both with and without restart injection
- **Configurable**: Configuration file allows easy adjustment of test matrix
- **YARN-aware**: Node role selection is specific to YARN architecture (RM vs NM)

## Dependencies

Ensure the following dependencies are included in the test's module to use the Restart Testing Framework:

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Restart Testing Framework - Core -->
    <dependency>
        <groupId>org.restarttest</groupId>
        <artifactId>restart-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>

    <!-- Restart Testing Framework - YARN Adapter -->
    <dependency>
        <groupId>org.restarttest</groupId>
        <artifactId>restart-yarn-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```