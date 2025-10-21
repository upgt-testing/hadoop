# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Apache Hadoop 3.3.5 - A distributed computing framework consisting of four main components:
- **Hadoop Common**: Common utilities and libraries shared across modules
- **HDFS**: Hadoop Distributed File System for distributed storage
- **YARN**: Resource management and job scheduling platform
- **MapReduce**: Distributed data processing framework

This is a fork of the official Apache Hadoop project for custom modifications.

## Build System

Hadoop uses **Maven** as its primary build system. All builds should be run from the repository root unless working on a specific submodule.

### Common Build Commands

**Basic compilation:**
```bash
mvn compile                          # Compile all modules
mvn compile -Pnative                 # Compile with native code
```

**Testing:**
```bash
mvn test                             # Run all tests
mvn test -Dtest=TestClassName        # Run specific test class
mvn test -Dtest=TestClass#methodName # Run specific test method
mvn test -Pnative                    # Run tests with native code
mvn test -Pshelltest                 # Run shell script tests
```

**Building distributions:**
```bash
# Binary distribution without native code
mvn package -Pdist -DskipTests -Dtar -Dmaven.javadoc.skip=true

# Binary distribution with native code
mvn package -Pdist,native -DskipTests -Dtar

# Source distribution
mvn package -Psrc -DskipTests
```

**Code quality:**
```bash
mvn compile checkstyle:checkstyle    # Run checkstyle
mvn compile spotbugs:spotbugs        # Run spotbugs (static analysis)
mvn apache-rat:check                 # Check license headers
```

**Installation:**
```bash
# Install artifacts to local Maven cache (useful when working on submodules)
mvn install -DskipTests

# After this, you can work from submodules without rebuilding dependencies
# Use -nsu to prevent Maven from updating SNAPSHOTs
mvn install -DskipTests -nsu
```

### Building Submodules

When working on a specific submodule, you can build from that directory. First run `mvn install -DskipTests` from the root to cache all dependencies, then work from the submodule directory.

### Docker Build Environment

For a consistent build environment with all dependencies:
```bash
./start-build-env.sh
```

This provides a Docker container with all required build tools pre-configured.

## Project Structure

```
hadoop/
├── hadoop-common-project/      # Core utilities, authentication, configuration
│   ├── hadoop-common/          # Main common module
│   ├── hadoop-auth/            # Authentication libraries
│   ├── hadoop-kms/             # Key Management Service
│   └── hadoop-nfs/             # NFS gateway
├── hadoop-hdfs-project/        # Distributed filesystem
│   ├── hadoop-hdfs/            # Main HDFS module
│   ├── hadoop-hdfs-client/     # HDFS client libraries
│   ├── hadoop-hdfs-rbf/        # Router-based federation
│   └── hadoop-hdfs-native-client/ # Native HDFS client (C++)
├── hadoop-yarn-project/        # Resource management
│   └── hadoop-yarn/            # YARN implementation
├── hadoop-mapreduce-project/   # MapReduce framework
│   └── hadoop-mapreduce-client/
├── hadoop-tools/               # Additional tools (DistCp, streaming, etc.)
├── hadoop-client-modules/      # Client-facing modules
├── hadoop-dist/                # Distribution assembly
└── dev-support/                # Development and release tools
```

## Architecture Notes

### Maven Module Hierarchy

Hadoop uses a parent POM structure:
- `hadoop-project/` - Parent POM defining all plugin versions and dependencies
- `hadoop-project-dist/` - Parent for distribution modules
- Each subproject (common, hdfs, yarn, mapreduce) has its own module hierarchy

### Native Code

Hadoop includes native C/C++ code for performance-critical operations. Building with `-Pnative` enables compilation of:
- Native compression codecs (zlib, snappy, zstd)
- Native encryption (OpenSSL integration)
- JNI interfaces for filesystem operations
- Erasure coding (Intel ISA-L)

Protocol Buffers 3.7.1 is required for native compilation.

### Test Organization

- Test code lives in `src/test/java` within each module
- Native tests can be run with: `mvn test -Pnative -Dtest=allNative`
- Shell tests require `-Pshelltest` profile
- Test exclusions: `-Dtest.exclude=TestClassName` or `-Dtest.exclude.pattern=**/<pattern>.java`

### Configuration System

Hadoop uses XML configuration files:
- `core-site.xml` - Common configuration
- `hdfs-site.xml` - HDFS configuration
- `yarn-site.xml` - YARN configuration
- `mapred-site.xml` - MapReduce configuration

Configuration classes typically extend `org.apache.hadoop.conf.Configuration`.

## Development Tools

### Test Patch

The `dev-support/bin/test-patch` script (using Apache Yetus) validates patches:
- Runs checkstyle, spotbugs, unit tests
- Validates patch format and licensing
- Used in CI/CD pipelines

### Release Notes

Build release documentation:
```bash
mvn package -Preleasedocs -DskipTests
```

### Memory Configuration

If builds fail with OutOfMemoryError:
```bash
export MAVEN_OPTS="-Xms256m -Xmx1536m"
```

## Important Patterns

### Hadoop Versioning

The project version (3.3.5) is defined in the root `pom.xml` and referenced as `${hadoop.version}` in child modules.

To change versions across the project:
```bash
mvn versions:set -DnewVersion=NEW_VERSION
```

### Shaded Client Jars

Hadoop provides shaded client jars to avoid dependency conflicts. During development, you can skip shading for faster builds:
```bash
mvn package -DskipShade
```

Note: Never use `-DskipShade` for release artifacts.

### Protocol Buffers

Many Hadoop RPC interfaces use Protocol Buffers. `.proto` files are in `src/main/proto/` directories. Changes to `.proto` files require regenerating Java sources (happens automatically during `mvn compile`).

## Requirements

- JDK 8
- Maven 3.3 or later
- Protocol Buffers 3.7.1 (for native code)
- CMake 3.1+ (for native code)
- Platform-specific build tools (see BUILDING.txt for details)

## Testing Frameworks

### ProcessBasedMiniDFSCluster

A process-based testing framework for HDFS that runs NameNodes and DataNodes in separate JVM processes, enabling:
- **Mixed-version clusters**: Test different Hadoop versions on different nodes
- **Rolling upgrades**: Simulate production upgrade scenarios
- **Version compatibility**: Verify cross-version protocol compatibility
- **Process isolation**: True multi-process behavior for realistic testing

#### Location

```
hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/process/
```

#### Quick Start

```java
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;

Configuration conf = new HdfsConfiguration();
ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.5")
        .format(true)
        .build();

cluster.waitClusterUp();
FileSystem fs = cluster.getFileSystem();
// ... use cluster ...
cluster.shutdown();
```

#### Running Tests

```bash
# Run unit tests
mvn test -Dtest="org.apache.hadoop.hdfs.server.process.unit.*" -pl hadoop-hdfs-project/hadoop-hdfs

# Run integration tests (requires HADOOP_HOME)
export HADOOP_HOME=/opt/hadoop-3.3.5
mvn test -Dtest="TestProcessBasedMiniDFSCluster" -pl hadoop-hdfs-project/hadoop-hdfs

# Run upgrade tests (requires multiple Hadoop versions)
export HADOOP_3_3_1_HOME=/opt/hadoop-3.3.1
export HADOOP_3_3_5_HOME=/opt/hadoop-3.3.5
mvn test -Dtest="TestRollingUpgrade" -pl hadoop-hdfs-project/hadoop-hdfs
```

#### Documentation

- **User Guide**: `hadoop-hdfs-project/hadoop-hdfs/docs/ProcessBasedMiniDFSCluster-UserGuide.md`
- **Developer Guide**: `hadoop-hdfs-project/hadoop-hdfs/docs/ProcessBasedMiniDFSCluster-DeveloperGuide.md`
- **Testing Guide**: `hadoop-hdfs-project/hadoop-hdfs/docs/VersionUpgradeTestingGuide.md`

#### Key Components

- **ProcessBasedMiniDFSCluster**: Main cluster coordinator
- **ProcessNodeManager**: Base class for managing node processes
- **VersionConfigAdapter**: Handles version-specific configuration
- **UpgradeTestHelper**: Utilities for testing upgrades

#### Test Structure

```
hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/process/
├── ProcessBasedMiniDFSCluster.java          # Main cluster class
├── ProcessNodeManager.java                   # Base process manager
├── NameNodeProcessManager.java              # NameNode process manager
├── DataNodeProcessManager.java              # DataNode process manager
├── VersionConfigAdapter.java                # Version compatibility
├── unit/                                     # Unit tests (159 tests)
│   ├── TestHadoopDistribution.java
│   ├── TestVersionConfigAdapter.java
│   └── ...
├── integration/                              # Integration tests
│   └── TestProcessBasedMiniDFSCluster.java
└── upgrade/                                  # Upgrade tests (19 tests)
    ├── UpgradeTestHelper.java
    ├── TestMixedVersionCluster.java
    └── TestRollingUpgrade.java
```
