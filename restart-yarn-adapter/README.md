# YARN Adapter for Restart Testing Framework

This module provides an adapter for testing Apache Hadoop YARN `MiniYARNCluster` with the Restart Testing Framework. It enables systematic restart testing of YARN components including ResourceManagers and NodeManagers.

## Features

- **ResourceManager Restart**: Supports restarting ResourceManagers in both single-RM and HA configurations
- **NodeManager Restart**: Custom implementation for restarting NodeManagers
- **Multiple Restart Modes**: GRACEFUL, CRASH, and DELAYED_CRASH modes
- **HA Support**: Full support for High Availability configurations with multiple ResourceManagers
- **State Capture**: Captures and verifies YARN cluster state across restarts including:
  - ResourceManager state (count, active RM, HA states)
  - NodeManager registration and health
  - Application and container state
  - Queue configuration and states
- **Health Checks**: Comprehensive health checks for cluster components
- **Role Aliases**: Supports generic role names ("master"/"worker") and YARN-specific names ("resourcemanager"/"nodemanager")

## Installation

### Add Dependency

Add the YARN adapter to your test dependencies:

```xml
<dependency>
    <groupId>org.restarttest</groupId>
    <artifactId>restart-yarn-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### Build from Source

```bash
cd restart-yarn-adapter
mvn clean install
```

## Usage

### Basic Example

```java
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

@Test
public void testYarnWithRestart() throws Exception {
    // Create and start YARN cluster
    Configuration conf = new YarnConfiguration();
    MiniYARNCluster cluster = new MiniYARNCluster("test", 1, 2, 1, 1);
    cluster.init(conf);
    cluster.start();
    cluster.waitForNodeManagersToConnect(5000);

    // Run your YARN application/test...

    // Add a restart point - NO-OP unless system properties match
    RestartFramework.at("after_job_submit")
        .on(cluster)
        .restart("resourcemanager")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    // Continue with your test...

    cluster.stop();
}
```

### Running with Restart Injection

```bash
# Run normally (no restart)
mvn test -Dtest=MyYarnTest

# Run with ResourceManager restart
mvn test -Dtest=MyYarnTest \
  -Drestart.position=after_job_submit \
  -Drestart.target=resourcemanager \
  -Drestart.mode=CRASH

# Run with NodeManager restart
mvn test -Dtest=MyYarnTest \
  -Drestart.position=after_job_submit \
  -Drestart.target=nodemanager \
  -Drestart.index=0 \
  -Drestart.mode=GRACEFUL
```

### High Availability Testing

```java
@Test
public void testHAFailover() throws Exception {
    // Create HA cluster with 2 ResourceManagers
    Configuration conf = new YarnConfiguration();
    MiniYARNCluster cluster = new MiniYARNCluster("test-ha", 2, 2, 1, 1);
    cluster.init(conf);
    cluster.start();

    int activeRMIndex = cluster.getActiveRMIndex();

    // Restart active RM (triggers failover)
    RestartFramework.at("trigger_failover")
        .on(cluster)
        .restart("resourcemanager")
        .withIndex(activeRMIndex)
        .withMode(RestartMode.CRASH)
        .execute();

    // Verify failover occurred
    int newActiveRMIndex = cluster.getActiveRMIndex();
    assertNotEquals(activeRMIndex, newActiveRMIndex);
}
```

## Supported Node Roles

| Role Name | Alias | Component |
|-----------|-------|-----------|
| `resourcemanager` | `master` | YARN ResourceManager |
| `nodemanager` | `worker` | YARN NodeManager |
| `all` | - | All components |

## Restart Modes

- **GRACEFUL**: Clean shutdown followed by restart (uses built-in restart methods)
- **CRASH**: Abrupt shutdown without cleanup, then restart (simulates crash)
- **DELAYED_CRASH**: Crash with 500ms delay before restart (tests state propagation windows)

## State Capture

The adapter captures the following cluster state:

### ResourceManager State
- RM count
- Active RM index
- HA state for each RM (ACTIVE/STANDBY/INITIALIZING)

### NodeManager State
- Registered NodeManager count
- Active NodeManager count
- Lost/unhealthy/decommissioned NodeManager counts

### Application State
- Total application count
- Application states (RUNNING, FAILED, FINISHED, KILLED)

### Container State
- Allocated containers
- Pending containers
- Reserved containers

### Queue State (Capacity Scheduler)
- Queue hierarchy
- Queue states (RUNNING/STOPPED)

## Health Checks

Four health checks are performed after each restart:

1. **YarnResourceManagerActiveCheck**: Verifies active RM exists and is operational
2. **YarnNodeManagersRegisteredCheck**: Verifies all NodeManagers are registered and active
3. **YarnApplicationsHealthCheck**: Reports application state (informational)
4. **YarnQueuesHealthCheck**: Verifies queue hierarchy and states (Capacity Scheduler only)

## Configuration

### Maven Plugin Integration

Use the Maven plugin for automated test matrix execution:

```json
{
  "tests": [
    {
      "testClass": "com.example.YarnTest",
      "testMethod": "testJobExecution",
      "restartPoints": [
        {
          "position": "after_job_submit",
          "targets": ["resourcemanager", "nodemanager"],
          "modes": ["GRACEFUL", "CRASH"]
        }
      ]
    }
  ]
}
```

Run with:
```bash
mvn restart-test:run -Drestart.config=restart-tests.json
```

## Implementation Notes

### NodeManager Restart

Since `MiniYARNCluster` doesn't provide built-in NodeManager restart methods, the adapter implements custom restart logic:

1. Stops the NodeManager using `nm.stop()` or `nm.serviceStop()`
2. Re-initializes with `nm.init(cluster.getConfig())`
3. Restarts with `nm.start()`
4. Waits for NodeManager to reconnect to ResourceManager

### HA Behavior

When restarting the active ResourceManager in an HA setup:
- GRACEFUL/CRASH modes may trigger failover to standby RM
- After restart, the original RM may become standby
- The adapter verifies that at least one RM is active after restart

## Testing

### Run Integration Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=YarnAdapterIntegrationTest

# Run HA tests only
mvn test -Dtest=YarnAdapterHATest
```

### Test Coverage

- Basic ResourceManager restart (GRACEFUL, CRASH, DELAYED_CRASH)
- Basic NodeManager restart (GRACEFUL, CRASH)
- HA active RM restart with failover
- HA standby RM restart
- System property activation
- Role name aliases
- Restart all nodes

## Troubleshooting

### NodeManagers Don't Reconnect

If NodeManagers fail to reconnect after restart:
- Increase timeout in `waitForNodeManagersToConnect()`
- Check NodeManager logs for connection errors
- Verify ResourceManager is active and accepting connections

### Health Checks Fail

If health checks consistently fail:
- Check cluster logs for errors
- Verify cluster configuration is correct
- Increase wait time in `waitActive()`
- Disable health checks temporarily: `.healthChecks(false)`

### HA Failover Issues

If failover doesn't work as expected:
- Verify HA is properly configured (2+ ResourceManagers)
- Check ZooKeeper connectivity (if using ZK for HA)
- Review RM logs for failover events

## Requirements

- Java 8+
- Apache Hadoop 3.3.5
- Restart Testing Framework Core 1.0.0-SNAPSHOT
- JUnit 4.13+

## License

Same as Apache Hadoop (Apache License 2.0)

## Contributing

Contributions are welcome! Please ensure:
- All tests pass
- Code follows existing style
- New features include tests
- Documentation is updated

## References

- [Restart Testing Framework Documentation](../RestartTestingFramework/docs/)
- [HDFS Adapter](../RestartTestingFramework/restart-hdfs-adapter/) (reference implementation)
- [MiniYARNCluster Javadoc](https://hadoop.apache.org/docs/current/api/org/apache/hadoop/yarn/server/MiniYARNCluster.html)
