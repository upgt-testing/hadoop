# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

This is a **fork/branch of Apache Hadoop** focused on transforming YARN's MiniYARNCluster into a process-based version for multi-version upgrade testing. The primary goal is to enable realistic testing of Hadoop YARN rolling upgrades across different versions.

**Current Branch**: `yarn-upgrade-test-in-process-3.3.5`

**Project Goal**: Create `ProcessBasedMiniYARNCluster` that runs each YARN node (ResourceManager, NodeManager) in separate JVM processes, enabling:
- Multi-version testing (e.g., Hadoop 3.3.x → 3.4.x)
- Rolling upgrade simulation
- Version compatibility verification
- Process-isolated testing (vs. in-JVM testing)

## Key Directories

```
yarn-transform/
├── hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/
│   └── hadoop-yarn-server-tests/
│       └── src/test/java/org/apache/hadoop/yarn/server/
│           └── MiniYARNCluster.java          # Original in-process cluster
├── prompts/                                   # AI transformation guides
│   ├── step1-process-based-yarn-cluster-implementation.md  (~12k lines)
│   ├── step2-yarn-upgrade-base-test-generation.md          (~500 lines)
│   └── step3-yarn-test-transformation-guide.md             (~21k lines)
└── template/                                  # Generic templates (reference only)
```

## Architecture Overview

### Current: MiniYARNCluster
- All YARN nodes (RM, NM) run in single JVM process
- Direct object access (getResourceManager(), getNodeManager())
- Cannot test different Hadoop versions simultaneously
- Fast but unrealistic for upgrade testing

### Target: ProcessBasedMiniYARNCluster
- Each YARN node runs in separate JVM process
- Only client-side APIs accessible (YarnClient, RMAdminCLI)
- Support different Hadoop versions per node
- Realistic process isolation for upgrade testing
- Automatic distribution path management via system properties

**Key Design Principle**: Most server-side operations have client-side equivalents via YarnClient APIs. Direct RM/NM object access should be replaced with RPC-based client operations.

## Build & Test Commands

### Build Entire Project
```bash
# Full build (from project root)
mvn clean install -DskipTests

# Build YARN server tests module only
mvn clean install -DskipTests \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

### Run Tests

```bash
# Run specific test
mvn test -Dtest=TestYarnFeature \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run ProcessBased test with system properties (for multi-version testing)
mvn test -Dtest=TestYarnFeature_ProcessBased \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run all ProcessBased tests
mvn test -Dtest="*_ProcessBased" \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run specific parameterized checkpoint
mvn test -Dtest='TestYarnFeature_ProcessBased#testMethod[upgrade-at=AFTER_APP_RUNNING]' \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0 \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

### Code Quality

```bash
# Run checkstyle
mvn checkstyle:checkstyle \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

# Run spotbugs
mvn compile spotbugs:spotbugs \
  -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
```

## Implementation Phases

The transformation is organized into 3 phases, each with detailed prompts in `prompts/`:

### Phase 1: ProcessBasedMiniYARNCluster Implementation (Weeks 1-6)
**Guide**: `prompts/step1-process-based-yarn-cluster-implementation.md`

**Tasks**:
1. **Core Infrastructure** (Weeks 1-2)
   - Process management (ProcessNodeManager, ResourceManagerProcessManager, NodeManagerProcessManager)
   - Classpath & version management (HadoopDistribution, HadoopVersionRegistry)
   - Configuration generation (ProcessConfigurationGenerator, PortAllocator)

2. **Main Cluster Class** (Weeks 3-4)
   - ProcessBasedMiniYARNCluster with Builder pattern
   - Health monitoring and retry logic
   - Cluster lifecycle management

3. **Multi-Version Support** (Week 5)
   - Version-specific configuration adapter
   - Mixed-version cluster testing
   - Rolling upgrade helpers

4. **Testing & Documentation** (Week 6)
   - Comprehensive test suite
   - Integration tests
   - Documentation

**Target Location**: `hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/src/test/java/org/apache/hadoop/yarn/server/process/`

### Phase 2: Generate YarnUpgradeTestBase (Week 5)
**Guide**: `prompts/step2-yarn-upgrade-base-test-generation.md`

**Purpose**: Create JUnit base class for parameterized upgrade testing with:
- Automatic lifecycle management (@Before/@After cleanup)
- Checkpoint-based upgrades via `checkpoint(name)` method
- Orphaned process cleanup
- System properties handling (hadoop.start.home, hadoop.upgrade.home)

**Target Location**: `hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/src/test/java/org/apache/hadoop/yarn/server/process/upgrade/YarnUpgradeTestBase.java`

### Phase 3: Transform Existing Tests (Week 6+)
**Guide**: `prompts/step3-yarn-test-transformation-guide.md`

**Naming Convention**: `TestYarnFeature.java` → `TestYarnFeature_ProcessBased.java` (same directory)

**Key Transformations**:
- `MiniYARNCluster` → `ProcessBasedMiniYARNCluster`
- `cluster.getResourceManager()` → `yarnClient.getYarnClusterMetrics()`
- `cluster.getNodeManager(i)` → `yarnClient.getNodeReports().get(i)`
- Direct object access → Client-side YarnClient APIs

**Comprehensive API Mapping Tables** (in step3 guide):
- Table 1: MiniYARNCluster → ProcessBasedMiniYARNCluster (25+ mappings)
- Table 2: ResourceManager → YarnClient APIs (40+ mappings)
- Table 3: NodeManager → NodeReport APIs (10+ mappings)
- Table 4: Admin Operations → RMAdminCLI (15+ mappings)
- Table 5: Monitoring → ClusterMetrics/Web UI (20+ mappings)

## System Properties for Multi-Version Testing

ProcessBasedMiniYARNCluster automatically reads these system properties:

```bash
-Dhadoop.start.home=/opt/hadoop-3.3.6      # Starting Hadoop distribution
-Dhadoop.upgrade.home=/opt/hadoop-3.4.0    # Target upgrade distribution
```

**No manual environment variable checks needed** - the cluster handles this automatically in the Builder.

## YARN Testing Architecture

### Original Test Pattern (MiniYARNCluster)
```java
MiniYARNCluster cluster = new MiniYARNCluster.Builder(conf)
    .numNodeManagers(3)
    .build();

YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfig());
yarnClient.start();

// Direct access possible:
ResourceManager rm = cluster.getResourceManager();
YarnClusterMetrics metrics = rm.getClusterMetrics();
```

### Transformed Pattern (ProcessBasedMiniYARNCluster)
```java
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(3)
        .build();  // Auto-reads hadoop.start.home

YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

// Client-side only:
YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
List<NodeReport> nodes = yarnClient.getNodeReports();
```

### Parameterized Upgrade Testing Pattern
```java
@RunWith(Parameterized.class)
public class TestYarnFeature_ProcessBased extends YarnUpgradeTestBase {

    @Parameter
    public String upgradeCheckpoint;

    @Parameters(name = "upgrade-at={0}")
    public static Collection<String> checkpoints() {
        return Arrays.asList(
            YarnUpgradeCheckpoints.NO_UPGRADE,
            YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
            "AFTER_APP_SUBMIT",
            "AFTER_APP_RUNNING"
        );
    }

    @Test
    public void testFeature() throws Exception {
        cluster = new ProcessBasedMiniYARNCluster.Builder(conf).build();
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(cluster.getConfiguration());
        yarnClient.start();

        checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

        ApplicationId appId = submitApp(yarnClient);
        checkpoint("AFTER_APP_SUBMIT");

        waitForAppCompletion(yarnClient, appId);
        checkpoint("AFTER_APP_RUNNING");

        // No try-finally needed - base class handles cleanup!
    }
}
```

## Key Hadoop Utilities to Use

When implementing ProcessBasedMiniYARNCluster, leverage existing Hadoop utilities:

- **GenericTestUtils**: `waitFor()` for polling conditions, `getTestDir()` for test directories
- **ServerSocketUtil**: `getPort()` for finding available ports
- **Shell**: Process execution and platform detection
- **HAUtil**: High Availability utilities for ResourceManager HA
- **Time**: `monotonicNow()` for timestamps
- **FileContext**: File operations with proper error handling

## API Transformation Hierarchy

When transforming tests, try client APIs in this order:

1. **YarnClient API** (highest level) - Application management, node reports, queue info
2. **ClientRMService API** (mid-level) - Cluster metrics, direct RM protocol
3. **ApplicationClientProtocol** (low-level) - YARN RPC interface
4. **RMAdminCLI / Admin APIs** - Administrative operations (refresh queues, nodes)
5. **Web UI / REST API** - HTTP-based metrics and monitoring
6. **Comment out** - Only if truly no client-side equivalent (internal RM/NM state)

## Common Pitfalls to Avoid

1. **Don't access RM/NM objects directly** in ProcessBased tests
   - ❌ `cluster.getResourceManager().getClusterMetrics()`
   - ✅ `yarnClient.getYarnClusterMetrics()`

2. **Don't trigger immediate state propagation** (no longer possible in process-based)
   - ❌ `cluster.triggerNodeHeartbeat()`
   - ✅ `Thread.sleep(3000)` or `GenericTestUtils.waitFor()`

3. **Close resources before checkpoint()** calls
   - YarnClient connections break during NodeManager restarts
   - Stop YarnClient → upgrade → restart YarnClient

4. **Use proper file naming** for transformed tests
   - Format: `TestYarnFeature_ProcessBased.java` (same directory as original)
   - Include `@see` link to original test in Javadoc

5. **System properties are automatic** - Don't manually check environment variables

## Code Style

Follow Apache Hadoop coding standards:
- Apache License 2.0 header on all new files
- Comprehensive Javadoc on public classes/methods
- Use SLF4J for logging (LOG.info, LOG.debug, LOG.error)
- Follow existing naming conventions (e.g., `ProcessBasedMini*`, `*ProcessManager`)
- Write unit tests for all new components
- Use proper exception handling with meaningful messages

## Important Reference Files

- **MiniYARNCluster**: `hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/src/test/java/org/apache/hadoop/yarn/server/MiniYARNCluster.java`
  - Original in-process cluster implementation
  - Study this for API compatibility and expected behavior

- **Transformation Guides**: `prompts/step*.md`
  - Complete, YARN-specific implementation guides
  - Use as reference or provide to AI agents for code generation
  - All placeholders already filled with YARN-specific values

- **BUILDING.txt**: Root directory
  - Maven build commands and options
  - Dependency requirements
  - Platform-specific build instructions

## Working with AI Agents

When using AI agents to implement ProcessBasedMiniYARNCluster:

1. **Use phased approach** - Don't send all 3 prompts at once (~33k lines)
2. **Start with Phase 1, Task 1.1** - Process management foundation
3. **Provide specific sections** from the guides, not entire files
4. **Request unit tests** with every component
5. **Validate incrementally** - Compile and test after each task
6. **Reference existing Hadoop utilities** in prompts

See earlier conversation for detailed AI agent prompt templates.
