# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Apache Hadoop YARN Server Tests** module (`hadoop-yarn-server-tests`), part of the larger Hadoop project. It contains integration and unit tests for YARN (Yet Another Resource Negotiator) server components, including the ResourceManager, NodeManager, and related services.

## Build System

This project uses **Maven** as its build system with the following key configurations:
- Maven coordinates: `org.apache.hadoop:hadoop-yarn-server-tests:3.3.5`
- Parent: `hadoop-yarn-server`
- Java-based project with Protocol Buffers integration

## Common Commands

### Building and Testing
```bash
# Build the project
mvn compile

# Run all tests
mvn test

# Build classpath for dependencies
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt

# Run a specific test class
mvn test -Dtest=TestMiniYarnCluster

# Clean and rebuild
mvn clean compile test-compile
```

### Custom Test Execution
The project includes custom scripts for specialized test execution:

```bash
# Run a single test method with upgrade testing capabilities
./run-single-test-method.sh <TestClass> <TestMethod> [START_VERSION] [END_VERSION] [MODE] [LOG_FILE] [DEBUG_PORT]

# Example:
./run-single-test-method.sh org.apache.hadoop.yarn.server.TestMiniYarnCluster testTimelineServiceStartInMiniCluster

# Prepare upgrade testing environment
./upgt_prepare.sh <current_version>
```

### Debug Mode
The `run-single-test-method.sh` script supports JVM debugging:
- Pass a debug port as the 8th parameter to enable remote debugging
- The script will automatically find an available port if the specified one is in use

## Project Structure

```
src/test/java/org/apache/hadoop/yarn/server/
├── MiniYARNCluster.java           # Original mini cluster for testing YARN components  
├── MiniYARNClusterInJVM.java      # UPGT-enhanced mini cluster with version isolation
├── TestMiniYarnCluster.java       # Tests for mini cluster functionality
├── TestMiniYARNClusterForHA.java  # High availability testing
├── TestContainerManagerSecurity.java
├── TestDiskFailures.java
├── TestRMNMSecretKeys.java
├── timeline/                      # Timeline service tests
└── timelineservice/               # Timeline service v2 tests

target/
├── classes/                       # Compiled classes
├── test-classes/                  # Compiled test classes
└── surefire-reports/             # Test execution reports

upgrade/                          # Upgrade testing artifacts (created by scripts)
├── target/                       # Copied compiled classes for upgrade tests
└── jars/                        # JAR files and classpath info for different versions
```

## Key Components and Architecture

### MiniYARNCluster
The core testing infrastructure that provides:
- **ResourceManager**: Central resource allocation and job scheduling
- **NodeManager**: Worker node management and container execution
- **ApplicationHistoryService**: Application lifecycle tracking
- **Timeline Service**: Event and metrics storage
- **Security Components**: Token management, authentication, and authorization

### Test Categories
1. **Integration Tests**: Full cluster testing with multiple components
2. **Unit Tests**: Individual component testing
3. **Security Tests**: Authentication, authorization, and token management
4. **High Availability Tests**: Failover and recovery scenarios
5. **Timeline Service Tests**: Event storage and retrieval
6. **Upgrade Tests**: Version compatibility and upgrade scenarios

### Custom Testing Features
- **Upgrade Testing**: Support for testing version upgrades using `upgt_prepare.sh` and specialized test runners
- **Debug Support**: Built-in JVM debugging capabilities with automatic port management
- **Timeout Handling**: 10-minute test timeouts with proper cleanup
- **Memory Management**: Automatic JVM heap sizing based on system resources

## Development Workflow

1. **Making Changes**: Modify test files in `src/test/java/`
2. **Building**: Use `mvn compile test-compile` to build
3. **Testing**: Run tests with `mvn test` or use custom scripts for specific scenarios
4. **Debugging**: Use the debug port feature in `run-single-test-method.sh` for troubleshooting

## Dependencies

Key dependencies include:
- **Hadoop Common**: Core Hadoop utilities and frameworks
- **YARN Components**: API, common libraries, server components
- **Testing Frameworks**: JUnit, Mockito
- **Security**: Kerberos, token management
- **Protocol Buffers**: Message serialization

## Upgrade Testing Framework (UPGT)

This project is specifically configured for **upgrade testing** using the UPGT (UpGrade Testing) framework located at `/Users/allenwang/xlab/upgt/upgt`. The key transformation involves:

### MiniYARNCluster Transformation
- **Original**: `MiniYARNCluster.java` - Standard YARN mini cluster for testing
- **Transformed**: `MiniYARNClusterInJVM.java` - Enhanced version with upgrade testing capabilities

### UPGT Architecture
The upgrade testing system uses customized class loaders and instances to load different versions of YARN components:

#### Key Components:
1. **ResourceManagerInstance.java** - Manages different versions of ResourceManager
2. **NodeManagerInstance.java** - Manages different versions of NodeManager  
3. **Custom ClassLoader System** - Enables loading multiple versions of the same classes

#### Version Isolation:
- `MiniYARNClusterInJVM.java` uses interface-based communication (`ResourceManagerJVMInterface`, `NodeManagerJVMInterface`)
- Bridge methods (e.g., `nodeHeartbeat_bridge`, `getClusterMetrics_bridge`) handle cross-version communication
- ClassLoader context switching ensures proper version isolation during operations

### Usage Patterns:
```bash
# Prepare upgrade environment
./upgt_prepare.sh <current_version>

# Run upgrade tests with version parameters
./run-single-test-method.sh <TestClass> <TestMethod> <START_VERSION> <END_VERSION> <MODE> [LOG_FILE] [DEBUG_PORT]
```

### Development Guidelines:
1. **Interface Compliance**: All YARN components must implement JVM interfaces for cross-version compatibility
2. **Bridge Methods**: Use bridge methods for method calls between different YARN versions
3. **ClassLoader Awareness**: Always use `resourceManagerInstance.getVersionClassLoader()` when working with versioned components
4. **State Management**: Handle state transitions carefully when switching between YARN versions

## Special Considerations

- Tests may require specific system configurations for security testing
- Timeline service tests may need additional setup for different storage backends  
- Upgrade tests require careful version management and classpath isolation
- Some tests are resource-intensive and may require adequate system memory
- The project includes specialized tooling for testing distributed system scenarios
- **UPGT Integration**: When working with upgrade tests, always consider version compatibility and interface contracts
- **ClassLoader Management**: Proper context switching is critical for version isolation