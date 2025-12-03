package org.restarttest.adapter.yarn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for YARN adapter with High Availability (HA) configurations.
 *
 * Tests restart behavior when multiple ResourceManagers are configured,
 * including active/standby RM restarts and failover scenarios.
 */
public class YarnAdapterHATest {

    private MiniYARNCluster cluster;
    private Configuration conf;

    @Before
    public void setUp() throws Exception {
        conf = new YarnConfiguration();

        // Disable automatic failover (which requires ZooKeeper)
        conf.setBoolean(YarnConfiguration.AUTO_FAILOVER_ENABLED, false);
        conf.setBoolean(YarnConfiguration.RM_HA_ENABLED, true);

        // Create HA cluster with 2 RMs and 2 NMs
        cluster = new MiniYARNCluster("test-ha", 2, 2, 1, 1);
        cluster.init(conf);
        cluster.start();

        // Wait for NodeManagers to connect
        assertTrue("NodeManagers should connect",
            cluster.waitForNodeManagersToConnect(10000));
    }

    @After
    public void tearDown() throws Exception {
        if (cluster != null) {
            cluster.stop();
            cluster.close();
        }
    }

    @Test
    public void testActiveResourceManagerRestart() throws Exception {
        // Find the active RM
        int activeRMIndex = cluster.getActiveRMIndex();
        assertTrue("Should have active RM", activeRMIndex != -1);

        // Restart the active RM
        RestartFramework.at("test-active-rm-restart")
            .on(cluster)
            .restart("resourcemanager")
            .withIndex(activeRMIndex)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Verify we still have an active RM (may have failed over)
        int newActiveRMIndex = cluster.getActiveRMIndex();
        assertNotEquals("Should have active RM after restart", -1, newActiveRMIndex);

        // Verify NodeManagers reconnected
        assertTrue("NodeManagers should reconnect",
            cluster.waitForNodeManagersToConnect(10000));
    }

    @Test
    public void testStandbyResourceManagerRestart() throws Exception {
        // Find the active and standby RMs
        int activeRMIndex = cluster.getActiveRMIndex();
        assertTrue("Should have active RM", activeRMIndex != -1);

        int standbyRMIndex = (activeRMIndex + 1) % 2;

        // Restart the standby RM
        RestartFramework.at("test-standby-rm-restart")
            .on(cluster)
            .restart("resourcemanager")
            .withIndex(standbyRMIndex)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Active RM should remain the same
        assertEquals("Active RM should not change",
            activeRMIndex, cluster.getActiveRMIndex());

        // Verify NodeManagers still connected
        assertTrue("NodeManagers should remain connected",
            cluster.waitForNodeManagersToConnect(10000));
    }

    @Test
    public void testActiveResourceManagerCrashFailover() throws Exception {
        int activeRMIndex = cluster.getActiveRMIndex();
        assertTrue("Should have active RM", activeRMIndex != -1);

        // Crash the active RM (should trigger failover)
        RestartFramework.at("test-active-rm-crash")
            .on(cluster)
            .restart("resourcemanager")
            .withIndex(activeRMIndex)
            .withMode(RestartMode.CRASH)
            .execute();

        // Should have a new active RM after failover
        int newActiveRMIndex = cluster.getActiveRMIndex();
        assertNotEquals("Should have active RM after failover", -1, newActiveRMIndex);

        // Verify NodeManagers reconnected
        assertTrue("NodeManagers should reconnect after failover",
            cluster.waitForNodeManagersToConnect(10000));
    }

    @Test
    public void testRestartAllResourceManagers() throws Exception {
        // Restart all RMs sequentially
        RestartFramework.at("test-restart-all-rms")
            .on(cluster)
            .restart("resourcemanager")
            .withIndex("all")
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Verify we have an active RM
        assertNotEquals("Should have active RM after restarting all",
            -1, cluster.getActiveRMIndex());

        // Verify NodeManagers reconnected
        assertTrue("NodeManagers should reconnect",
            cluster.waitForNodeManagersToConnect(10000));
    }

    @Test
    public void testMultipleNodeManagerRestarts() throws Exception {
        // Restart all NodeManagers
        RestartFramework.at("test-restart-all-nms")
            .on(cluster)
            .restart("nodemanager")
            .withIndex("all")
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Verify all NodeManagers reconnected
        assertTrue("All NodeManagers should reconnect",
            cluster.waitForNodeManagersToConnect(10000));
    }

    @Test
    public void testDelayedCrashRestart() throws Exception {
        int activeRMIndex = cluster.getActiveRMIndex();

        // Test DELAYED_CRASH mode
        RestartFramework.at("test-delayed-crash")
            .on(cluster)
            .restart("resourcemanager")
            .withIndex(activeRMIndex)
            .withMode(RestartMode.DELAYED_CRASH)
            .execute();

        // Verify recovery
        assertNotEquals("Should have active RM after delayed crash",
            -1, cluster.getActiveRMIndex());
        assertTrue("NodeManagers should reconnect",
            cluster.waitForNodeManagersToConnect(10000));
    }
}
