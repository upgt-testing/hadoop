# Integration Guide: Using the YARN Adapter in Your Tests

## Overview

This guide shows how to integrate the YARN Restart Adapter into existing YARN tests in the `hadoop-yarn-project` or any other module.

## Quick Start

### Step 1: Install the Adapter

First, build and install the adapter to your local Maven repository:

```bash
cd /Users/allenwang/xlab/yarn-transform/restart-yarn-adapter
mvn clean install
```

This installs the JAR to: `~/.m2/repository/org/restarttest/restart-yarn-adapter/1.0.0-SNAPSHOT/`

### Step 2: Add Dependency to Your Test Module

In your test module's `pom.xml` (e.g., `hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-resourcemanager/pom.xml`):

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

### Step 3: Write Your Test

Create or modify a test in your module:

```java
package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import static org.junit.Assert.*;

public class TestRMWithRestarts {

    private MiniYARNCluster cluster;
    private Configuration conf;

    @Before
    public void setUp() throws Exception {
        conf = new YarnConfiguration();
        cluster = new MiniYARNCluster("test", 1, 2, 1, 1);
        cluster.init(conf);
        cluster.start();
        cluster.waitForNodeManagersToConnect(10000);
    }

    @After
    public void tearDown() throws Exception {
        if (cluster != null) {
            cluster.stop();
            cluster.close();
        }
    }

    @Test
    public void testApplicationSubmissionWithRMRestart() throws Exception {
        // Submit your YARN application here
        // ...

        // Add a restart point - NO-OP unless system properties are set
        RestartFramework.at("after_app_submit")
            .on(cluster)
            .restart("resourcemanager")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Continue your test - verify the application recovered
        // ...
    }
}
```

### Step 4: Run Tests

**Normal Run (no restart):**
```bash
mvn test -Dtest=TestRMWithRestarts
```
The restart point is a NO-OP, test runs normally.

**Run with Restart Injection:**
```bash
mvn test -Dtest=TestRMWithRestarts \
  -Drestart.position=after_app_submit \
  -Drestart.target=resourcemanager \
  -Drestart.mode=CRASH
```

The test will inject a crash restart at the specified position.

## How ServiceLoader Discovery Works

### 1. JAR Structure

When you run `mvn install`, the adapter JAR is created with this structure:

```
restart-yarn-adapter-1.0.0-SNAPSHOT.jar
├── META-INF/
│   ├── services/
│   │   └── org.restarttest.core.ClusterAdapter
│   └── MANIFEST.MF
└── org/
    └── restarttest/
        └── adapter/
            └── yarn/
                ├── YarnClusterAdapter.class
                ├── YarnStateCapture.class
                └── health/
                    └── ...
```

The `META-INF/services/org.restarttest.core.ClusterAdapter` file contains:
```
org.restarttest.adapter.yarn.YarnClusterAdapter
```

### 2. Runtime Discovery Process

When your test runs:

1. **JVM Classpath Scanning**: The JUnit test starts with the restart-yarn-adapter JAR on the classpath

2. **ServiceLoader Activation**: The Restart Testing Framework core uses:
   ```java
   ServiceLoader<ClusterAdapter> loader = ServiceLoader.load(ClusterAdapter.class);
   ```

3. **Automatic Discovery**: ServiceLoader:
   - Scans all JARs on the classpath
   - Finds `META-INF/services/org.restarttest.core.ClusterAdapter` files
   - Reads `org.restarttest.adapter.yarn.YarnClusterAdapter` from the file
   - Uses reflection to instantiate: `new YarnClusterAdapter()`
   - Registers it with the AdapterRegistry

4. **Type Matching**: When you call `RestartFramework.at(...).on(cluster)`:
   - Framework checks: `cluster instanceof MiniYARNCluster`
   - Finds the adapter where `adapter.getClusterType() == MiniYARNCluster.class`
   - Uses that adapter for restart operations

### 3. Verification

You can verify the adapter is on your classpath:

```bash
# Check if the JAR is in your local Maven repo
ls -la ~/.m2/repository/org/restarttest/restart-yarn-adapter/1.0.0-SNAPSHOT/

# Verify the ServiceLoader file is in the JAR
jar -tf ~/.m2/repository/org/restarttest/restart-yarn-adapter/1.0.0-SNAPSHOT/restart-yarn-adapter-1.0.0-SNAPSHOT.jar | grep META-INF/services

# View the contents of the ServiceLoader file
jar -xf ~/.m2/repository/org/restarttest/restart-yarn-adapter/1.0.0-SNAPSHOT/restart-yarn-adapter-1.0.0-SNAPSHOT.jar META-INF/services/org.restarttest.core.ClusterAdapter
cat META-INF/services/org.restarttest.core.ClusterAdapter
```

### 4. Debugging Discovery Issues

If the adapter isn't being discovered, check:

**A. Verify dependency is in effective POM:**
```bash
mvn dependency:tree | grep restart-yarn-adapter
```

Should show:
```
[INFO] +- org.restarttest:restart-yarn-adapter:jar:1.0.0-SNAPSHOT:test
```

**B. Check classpath during test:**

Add to your test:
```java
@Before
public void debugClasspath() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    System.out.println("Classpath:");
    if (cl instanceof URLClassLoader) {
        URLClassLoader ucl = (URLClassLoader) cl;
        for (URL url : ucl.getURLs()) {
            System.out.println("  " + url);
        }
    }
}
```

You should see the restart-yarn-adapter JAR listed.

**C. Manual verification:**

Add this to verify ServiceLoader is working:
```java
import java.util.ServiceLoader;
import org.restarttest.core.ClusterAdapter;

@Test
public void testServiceLoaderDiscovery() {
    ServiceLoader<ClusterAdapter> loader = ServiceLoader.load(ClusterAdapter.class);
    int count = 0;
    for (ClusterAdapter adapter : loader) {
        System.out.println("Found adapter: " + adapter.getClass().getName());
        System.out.println("  Supports: " + adapter.getClusterType().getName());
        count++;
    }
    assertTrue("Should find at least 1 adapter", count > 0);
}
```

## Advanced: Maven Plugin for Test Matrix

For systematic testing, use the Maven plugin:

**1. Create `restart-tests.json` in your test module:**

```json
{
  "tests": [
    {
      "testClass": "org.apache.hadoop.yarn.server.resourcemanager.TestRMFailover",
      "testMethod": "testRMWebAppRedirect",
      "restartPoints": [
        {
          "position": "after_state_store_init",
          "targets": ["resourcemanager"],
          "modes": ["GRACEFUL", "CRASH"]
        }
      ]
    },
    {
      "testClass": "org.apache.hadoop.yarn.server.TestMiniYARNCluster",
      "testMethod": "testNodeManagersConnect",
      "restartPoints": [
        {
          "position": "after_nm_connect",
          "targets": ["resourcemanager", "nodemanager"],
          "modes": ["GRACEFUL"]
        }
      ]
    }
  ]
}
```

**2. Add plugin to `pom.xml`:**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.restarttest</groupId>
            <artifactId>restart-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <configuration>
                <configFile>restart-tests.json</configFile>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**3. Run test matrix:**

```bash
mvn restart-test:run
```

This will automatically run all combinations of tests, restart points, targets, and modes.

## Troubleshooting

### Problem: "No adapter found for cluster type"

**Cause**: The adapter isn't on the classpath or ServiceLoader discovery failed.

**Solutions**:
1. Verify dependency in `pom.xml`
2. Run `mvn clean install` in the restart-yarn-adapter directory
3. Check `mvn dependency:tree` output
4. Look for "Found adapter: YarnClusterAdapter" in test output

### Problem: "NodeManagers don't reconnect after restart"

**Cause**: This is expected behavior when disk space is >90% full.

**Solutions**:
1. Free up disk space
2. Increase timeout: `cluster.waitForNodeManagersToConnect(60000)`
3. Configure YARN to be less strict about disk usage

### Problem: ServiceLoader file not in JAR

**Cause**: Resources weren't copied during build.

**Solution**:
1. Verify `src/main/resources/META-INF/services/org.restarttest.core.ClusterAdapter` exists
2. Run `mvn clean package` (not just `mvn compile`)
3. Check JAR contents: `jar -tf target/*.jar | grep META-INF/services`

## Summary

The YARN Adapter uses Java's ServiceLoader mechanism for automatic discovery:

1. ✅ **Build**: `mvn install` packages the adapter with `META-INF/services/` file
2. ✅ **Install**: JAR goes to `~/.m2/repository/`
3. ✅ **Dependency**: Other modules add it as a `<dependency>`
4. ✅ **Discovery**: ServiceLoader automatically finds and loads it at runtime
5. ✅ **Usage**: Tests use `RestartFramework.at(...).on(cluster)` - adapter is used automatically

No manual registration or initialization needed - it's completely automatic!
