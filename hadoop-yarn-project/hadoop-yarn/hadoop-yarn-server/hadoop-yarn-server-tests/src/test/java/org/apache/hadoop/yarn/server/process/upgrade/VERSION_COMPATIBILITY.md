# YARN Multi-Version Compatibility Matrix

This document describes the compatibility matrix for running mixed-version YARN clusters using ProcessBasedMiniYARNCluster.

## Compatibility Levels

### Level 1: Fully Compatible (Same Major.Minor)
Versions with the same major.minor version are fully compatible and can be mixed freely in the same cluster.

- **3.3.x** series: 3.3.0, 3.3.1, 3.3.2, 3.3.3, 3.3.4, 3.3.5, 3.3.6
- **3.4.x** series: 3.4.0, 3.4.1 (future releases)

**Supported Scenarios:**
- ✅ Mix different patch versions (e.g., RM on 3.3.5, NMs on 3.3.6)
- ✅ Rolling upgrade within same major.minor (e.g., 3.3.4 → 3.3.6)
- ✅ All cluster configurations (standalone, HA)

### Level 2: Protocol Compatible (Different Major.Minor, RPC Compatible)
Versions with different major.minor but compatible RPC protocols can run in mixed-version mode with careful testing.

**Generally Compatible:**
- **3.3.x ↔ 3.4.x**: Should work for basic operations
  - ✅ RM on 3.3.x, NMs on 3.4.x
  - ✅ RM on 3.4.x, NMs on 3.3.x
  - ⚠️  Some advanced features may not work
  - ⚠️  Extensive testing recommended

**Testing Required:**
- Application submission and completion
- Container lifecycle
- Resource allocation
- Cluster metrics
- Web UI functionality

### Level 3: Incompatible (Major Version Change)
Major version changes typically break compatibility and should not be mixed.

**Not Recommended:**
- ❌ 2.x ↔ 3.x: Protocol incompatibilities
- ❌ 3.x ↔ 4.x: Future major versions

## Supported Upgrade Paths

### Rolling Upgrade: NodeManagers

Recommended approach for production-like testing:

```
Initial:  RM(3.3.6) + NM0(3.3.6) + NM1(3.3.6) + NM2(3.3.6)
Step 1:   RM(3.3.6) + NM0(3.4.0) + NM1(3.3.6) + NM2(3.3.6)  <- Upgrade NM0
Step 2:   RM(3.3.6) + NM0(3.4.0) + NM1(3.4.0) + NM2(3.3.6)  <- Upgrade NM1
Step 3:   RM(3.3.6) + NM0(3.4.0) + NM1(3.4.0) + NM2(3.4.0)  <- Upgrade NM2
Final:    RM(3.4.0) + NM0(3.4.0) + NM1(3.4.0) + NM2(3.4.0)  <- Upgrade RM
```

**Benefits:**
- Maintains cluster capacity during upgrade
- Can rollback individual nodes
- Minimal service disruption

**Code Example:**
```java
UpgradeTestHelper helper = new UpgradeTestHelper(cluster);
helper.rollingUpgradeAllNodeManagers("/opt/hadoop-3.4.0", 5000);
// Then upgrade RM
helper.upgradeNodeManager(0, "/opt/hadoop-3.4.0");
```

### RM HA Upgrade with Failover

For HA clusters with multiple ResourceManagers:

```
Initial:  RM0(3.3.6,active) + RM1(3.3.6,standby) + NMs(3.3.6)
Step 1:   RM0(3.3.6,active) + RM1(3.4.0,standby) + NMs(3.3.6)  <- Upgrade standby
Step 2:   RM0(3.4.0,standby) + RM1(3.4.0,active) + NMs(3.3.6)  <- Failover & upgrade
Final:    RM0(3.4.0) + RM1(3.4.0) + NMs(3.4.0)                 <- Upgrade NMs
```

**Benefits:**
- No RM service interruption
- Automatic failover during active RM upgrade
- Can test failover mechanism

**Code Example:**
```java
UpgradeTestHelper helper = new UpgradeTestHelper(cluster);
helper.upgradeResourceManagerWithFailover(0, "/opt/hadoop-3.4.0");
```

### Batch Upgrade

For faster upgrades in test/dev environments:

```
Initial:  RM(3.3.6) + NM0(3.3.6) + NM1(3.3.6) + NM2(3.3.6)
Step 1:   RM(3.3.6) + NM0(3.4.0) + NM1(3.4.0) + NM2(3.4.0)  <- Upgrade all NMs
Final:    RM(3.4.0) + NM0(3.4.0) + NM1(3.4.0) + NM2(3.4.0)  <- Upgrade RM
```

**Code Example:**
```java
UpgradeTestHelper helper = new UpgradeTestHelper(cluster);
helper.batchUpgradeNodeManagers(new int[]{0, 1, 2}, "/opt/hadoop-3.4.0");
```

## Known Issues and Limitations

### Configuration Differences

Some configuration keys have changed between versions:

| Feature | 3.3.x | 3.4.x | Notes |
|---------|-------|-------|-------|
| *(Placeholder)* | - | - | Add actual config differences |

Use `VersionConfigAdapter` to handle these automatically:

```java
VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
YarnConfiguration adapted = adapter.adapt(config33x, "3.3.6");
```

### Feature Compatibility

| Feature | 3.3.x → 3.4.x | 3.4.x → 3.3.x | Notes |
|---------|---------------|---------------|-------|
| Application Submission | ✅ | ✅ | Fully compatible |
| Container Execution | ✅ | ✅ | Fully compatible |
| Web UI | ✅ | ⚠️  | Some new features unavailable |
| Timeline Service | ⚠️  | ⚠️  | May have schema differences |
| Federation | ⚠️  | ⚠️  | Limited testing |

## Testing Recommendations

### Before Upgrade Testing

1. **Verify Hadoop distributions are built and available:**
   ```bash
   ls -l /opt/hadoop-3.3.6
   ls -l /opt/hadoop-3.4.0
   ```

2. **Set system properties:**
   ```bash
   -Dhadoop.start.home=/opt/hadoop-3.3.6
   -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
   ```

3. **Validate versions:**
   ```bash
   /opt/hadoop-3.3.6/bin/hadoop version
   /opt/hadoop-3.4.0/bin/hadoop version
   ```

### During Upgrade Testing

1. **Monitor cluster health:**
   ```java
   boolean healthy = helper.verifyClusterHealthy();
   ```

2. **Check version compatibility:**
   ```java
   boolean compatible = UpgradeTestHelper.areVersionsCompatible("3.3.6", "3.4.0");
   String path = UpgradeTestHelper.getUpgradePath("3.3.6", "3.4.0");
   ```

3. **Validate applications:**
   - Submit test applications before upgrade
   - Keep applications running during upgrade
   - Submit new applications after upgrade
   - Verify all applications complete successfully

### After Upgrade Testing

1. **Verify cluster metrics:**
   - All nodes registered
   - Correct resource counts
   - No errors in logs

2. **Test application lifecycle:**
   - Submit applications
   - Monitor container allocation
   - Verify completion

3. **Test failover (HA clusters):**
   - Trigger manual failover
   - Verify active RM switches
   - Confirm no application interruption

## Version-Specific Notes

### Hadoop 3.3.x

- **Stable**: Mature release with extensive production usage
- **Recommended for**: Starting version in upgrade tests
- **Key Features**: Standard YARN features, Timeline Service v1.5

### Hadoop 3.4.x

- **Status**: Newer release
- **Recommended for**: Target version in upgrade tests
- **Key Features**: Performance improvements, new scheduler features

## Troubleshooting

### Issue: Nodes fail to register after upgrade

**Cause**: RPC protocol incompatibility or port conflicts

**Solution:**
1. Check logs: `cluster.getClusterRoot()/nm0/logs/`
2. Verify ports are available
3. Check VersionConfigAdapter applied correctly

### Issue: Applications fail during upgrade

**Cause**: Container state lost during NM restart

**Solution:**
1. Use work-preserving restart features (if available)
2. Wait for running containers to complete before upgrade
3. Use smaller batch sizes in rolling upgrade

### Issue: RM failover fails in HA cluster

**Cause**: ZooKeeper connection issues or state store problems

**Solution:**
1. Verify ZooKeeper is running
2. Check RM state store directory permissions
3. Review RM logs for detailed errors

## References

- [YARN Rolling Upgrade Documentation](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/YarnRollingUpgrade.html)
- ProcessBasedMiniYARNCluster JavaDoc
- VersionConfigAdapter JavaDoc
- UpgradeTestHelper JavaDoc
