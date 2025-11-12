/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.applications.unmanagedamlauncher;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.ApplicationMasterProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterRequest;
import org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterResponse;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterRequest;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationAttemptState;
import org.apache.hadoop.yarn.client.ClientRMProxy;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBased version of {@link TestUnmanagedAMLauncher}.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * @see TestUnmanagedAMLauncher Original test using MiniYARNCluster
 */
@RunWith(Parameterized.class)
public class TestUnmanagedAMLauncher_ProcessBased extends YarnUpgradeTestBase {
  private static final Logger LOG = LoggerFactory
      .getLogger(TestUnmanagedAMLauncher_ProcessBased.class);

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_CONFIG_WRITE",
      "AFTER_LAUNCHER_INIT",
      "AFTER_LAUNCHER_RUN"
    );
  }

  /**
   * Writes the cluster configuration to yarn-site.xml file in classpath.
   * This is required for the UnmanagedAM process to connect to the cluster.
   */
  private void setupClusterConfig() throws IOException {
    Configuration yarnClusterConfig = cluster.getConfiguration();
    LOG.info("ProcessBased ResourceManager published address: " +
             yarnClusterConfig.get(YarnConfiguration.RM_ADDRESS));
    LOG.info("ProcessBased ResourceManager published web address: " +
             yarnClusterConfig.get(YarnConfiguration.RM_WEBAPP_ADDRESS));
    String webapp = yarnClusterConfig.get(YarnConfiguration.RM_WEBAPP_ADDRESS);
    assertTrue("Web app address still unbound to a host at " + webapp,
      !webapp.startsWith("0.0.0.0"));
    LOG.info("Yarn webapp is at "+ webapp);

    URL url = Thread.currentThread().getContextClassLoader()
        .getResource("yarn-site.xml");
    if (url == null) {
      throw new RuntimeException(
          "Could not find 'yarn-site.xml' dummy file in classpath");
    }
    // Write the document to a buffer (not directly to the file, as that
    // can cause the file being written to get read - which will then fail)
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    yarnClusterConfig.writeXml(bytesOut);
    bytesOut.close();
    // Write the bytes to the file in the classpath
    OutputStream os = new FileOutputStream(new File(url.getPath()));
    os.write(bytesOut.toByteArray());
    os.close();
  }

  private static String getTestRuntimeClasspath() {
    LOG.info("Trying to generate classpath for app master from current thread's classpath");
    String envClassPath = "";
    String cp = System.getProperty("java.class.path");
    if (cp != null) {
      envClassPath += cp.trim() + File.pathSeparator;
    }
    // yarn-site.xml at this location contains proper config for mini cluster
    ClassLoader thisClassLoader = Thread.currentThread()
      .getContextClassLoader();
    URL url = thisClassLoader.getResource("yarn-site.xml");
    envClassPath += new File(url.getFile()).getParent();
    return envClassPath;
  }

  @Test(timeout=120000)
  public void testUMALauncher() throws Exception {
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(1)
        .numResourceManagers(1)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    // Wait for cluster to be ready
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      LOG.info("setup thread sleep interrupted. message=" + e.getMessage());
    }

    setupClusterConfig();
    checkpoint("AFTER_CONFIG_WRITE");

    String classpath = getTestRuntimeClasspath();
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      LOG.error("JAVA_HOME not defined. Test not running.");
      return;
    }
    String[] args = {
        "--classpath",
        classpath,
        "--queue",
        "default",
        "--cmd",
        javaHome
            + "/bin/java -Xmx512m "
            + TestUnmanagedAMLauncher_ProcessBased.class.getCanonicalName()
            + " success" };

    LOG.info("Initializing Launcher");
    UnmanagedAMLauncher launcher =
        new UnmanagedAMLauncher(new Configuration(cluster.getConfiguration())) {
          public void launchAM(ApplicationAttemptId attemptId)
              throws IOException, YarnException {
            YarnApplicationAttemptState attemptState =
                rmClient.getApplicationAttemptReport(attemptId)
                  .getYarnApplicationAttemptState();
            Assert.assertTrue(attemptState
              .equals(YarnApplicationAttemptState.LAUNCHED));
            super.launchAM(attemptId);
          }
        };
    boolean initSuccess = launcher.init(args);
    Assert.assertTrue(initSuccess);
    checkpoint("AFTER_LAUNCHER_INIT");

    LOG.info("Running Launcher");
    boolean result = launcher.run();
    checkpoint("AFTER_LAUNCHER_RUN");

    LOG.info("Launcher run completed. Result=" + result);
    Assert.assertTrue(result);
  }

  @Test(timeout=120000)
  public void testUMALauncherError() throws Exception {
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(1)
        .numResourceManagers(1)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    // Wait for cluster to be ready
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      LOG.info("setup thread sleep interrupted. message=" + e.getMessage());
    }

    setupClusterConfig();
    checkpoint("AFTER_CONFIG_WRITE");

    String classpath = getTestRuntimeClasspath();
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      LOG.error("JAVA_HOME not defined. Test not running.");
      return;
    }
    String[] args = {
        "--classpath",
        classpath,
        "--queue",
        "default",
        "--cmd",
        javaHome
            + "/bin/java -Xmx512m "
            + TestUnmanagedAMLauncher_ProcessBased.class.getCanonicalName()
            + " failure" };

    LOG.info("Initializing Launcher");
    UnmanagedAMLauncher launcher = new UnmanagedAMLauncher(new Configuration(
        cluster.getConfiguration()));
    boolean initSuccess = launcher.init(args);
    Assert.assertTrue(initSuccess);
    checkpoint("AFTER_LAUNCHER_INIT");

    LOG.info("Running Launcher");

    try {
      launcher.run();
      checkpoint("AFTER_LAUNCHER_RUN");
      fail("Expected an exception to occur as launch should have failed");
    } catch (RuntimeException e) {
      // Expected
      checkpoint("AFTER_LAUNCHER_RUN");
    }
  }

  // Provide main method so this class can act as AM
  public static void main(String[] args) throws Exception {
    Configuration conf = new YarnConfiguration();
    if (args[0].equals("success")) {
      ApplicationMasterProtocol client = ClientRMProxy.createRMProxy(conf,
          ApplicationMasterProtocol.class);
      client.registerApplicationMaster(RegisterApplicationMasterRequest
          .newInstance(NetUtils.getHostname(), -1, ""));
      Thread.sleep(1000);
      FinishApplicationMasterResponse resp =
          client.finishApplicationMaster(FinishApplicationMasterRequest
            .newInstance(FinalApplicationStatus.SUCCEEDED, "success", null));
      assertTrue(resp.getIsUnregistered());
      System.exit(0);
    } else {
      System.exit(1);
    }
  }
}
