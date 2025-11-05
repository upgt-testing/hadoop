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

package org.apache.hadoop.security;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.tools.DFSAdmin;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.apache.hadoop.security.authorize.DefaultImpersonationProvider;
import org.apache.hadoop.security.authorize.ProxyUsers;
import org.apache.hadoop.test.GenericTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


/**
 * ProcessBasedMiniDFSCluster version of {@link TestRefreshUserMappings}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests user-to-groups mappings refresh and superuser groups configuration refresh.
 *
 * @see TestRefreshUserMappings Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestRefreshUserMappings_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(
      TestRefreshUserMappings_ProcessBased.class);
  private static final long groupRefreshTimeoutSec = 1;
  private String tempResource = null;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FIRST_GROUP_CHECK",
      "AFTER_REFRESH_COMMAND",
      "BEFORE_VERIFICATION"
    );
  }

  public static class MockUnixGroupsMapping implements GroupMappingServiceProvider {
    private int i=0;

    @Override
    public List<String> getGroups(String user) throws IOException {
      System.out.println("Getting groups in MockUnixGroupsMapping");
      String g1 = user + (10 * i + 1);
      String g2 = user + (10 * i + 2);
      List<String> l = new ArrayList<String>(2);
      l.add(g1);
      l.add(g2);
      i++;
      return l;
    }

    @Override
    public void cacheGroupsRefresh() throws IOException {
      System.out.println("Refreshing groups in MockUnixGroupsMapping");
    }

    @Override
    public void cacheGroupsAdd(List<String> groups) throws IOException {
    }
  }

  @After
  public void cleanupTempResource() {
    if(tempResource!=null) {
      File f = new File(tempResource);
      f.delete();
      tempResource = null;
    }
  }

  @Test
  public void testGroupMappingRefresh() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    testConf.setClass("hadoop.security.group.mapping",
        TestRefreshUserMappings_ProcessBased.MockUnixGroupsMapping.class,
        GroupMappingServiceProvider.class);
    testConf.setLong("hadoop.security.groups.cache.secs", groupRefreshTimeoutSec);
    Groups.getUserToGroupsMappingService(testConf);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf).numDataNodes(3).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    GenericTestUtils.setLogLevel(Groups.LOG, Level.DEBUG);

    DFSAdmin admin = new DFSAdmin(testConf);
    String [] args =  new String[]{"-refreshUserToGroupsMappings"};
    Groups groups = Groups.getUserToGroupsMappingService(testConf);
    String user = UserGroupInformation.getCurrentUser().getUserName();

    LOG.debug("First attempt:");
    List<String> g1 = groups.getGroups(user);
    LOG.debug(g1.toString());

    LOG.debug("Second attempt, should be the same:");
    List<String> g2 = groups.getGroups(user);
    LOG.debug(g2.toString());
    for(int i=0; i<g2.size(); i++) {
      assertEquals("Should be same group ", g1.get(i), g2.get(i));
    }
    checkpoint("AFTER_FIRST_GROUP_CHECK");

    // Test refresh command
    admin.run(args);
    checkpoint("AFTER_REFRESH_COMMAND");

    LOG.debug("Third attempt(after refresh command), should be different:");
    List<String> g3 = groups.getGroups(user);
    LOG.debug(g3.toString());
    for(int i=0; i<g3.size(); i++) {
      assertFalse("Should be different group: "
              + g1.get(i) + " and " + g3.get(i), g1.get(i).equals(g3.get(i)));
    }

    // Test timeout
    LOG.debug("Fourth attempt(after timeout), should be different:");
    GenericTestUtils.waitFor(() -> {
      List<String> g4;
      try {
        g4 = groups.getGroups(user);
      } catch (IOException e) {
        return false;
      }
      LOG.debug(g4.toString());
      // if g4 is the same as g3, wait and retry
      return !g3.equals(g4);
    }, 50, Math.toIntExact(groupRefreshTimeoutSec * 1000 * 30));
    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testRefreshSuperUserGroupsConfiguration() throws Exception {
    final String SUPER_USER = "super_user";
    final List<String> groupNames1 = new ArrayList<>();
    groupNames1.add("gr1");
    groupNames1.add("gr2");
    final List<String> groupNames2 = new ArrayList<>();
    groupNames2.add("gr3");
    groupNames2.add("gr4");

    Configuration testConf = new HdfsConfiguration(conf);

    //keys in conf
    String userKeyGroups = DefaultImpersonationProvider.getTestProvider().
        getProxySuperuserGroupConfKey(SUPER_USER);
    String userKeyHosts = DefaultImpersonationProvider.getTestProvider().
        getProxySuperuserIpConfKey (SUPER_USER);

    testConf.set(userKeyGroups, "gr3,gr4,gr5"); // superuser can proxy for this group
    testConf.set(userKeyHosts,"127.0.0.1");
    ProxyUsers.refreshSuperUserGroupsConfiguration(testConf);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf).numDataNodes(3).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    UserGroupInformation ugi1 = mock(UserGroupInformation.class);
    UserGroupInformation ugi2 = mock(UserGroupInformation.class);
    UserGroupInformation suUgi = mock(UserGroupInformation.class);
    when(ugi1.getRealUser()).thenReturn(suUgi);
    when(ugi2.getRealUser()).thenReturn(suUgi);

    when(suUgi.getShortUserName()).thenReturn(SUPER_USER); // super user
    when(suUgi.getUserName()).thenReturn(SUPER_USER+"L"); // super user

    when(ugi1.getShortUserName()).thenReturn("user1");
    when(ugi2.getShortUserName()).thenReturn("user2");

    when(ugi1.getUserName()).thenReturn("userL1");
    when(ugi2.getUserName()).thenReturn("userL2");

    // set groups for users
    when(ugi1.getGroups()).thenReturn(groupNames1);
    when(ugi2.getGroups()).thenReturn(groupNames2);
    checkpoint("AFTER_INITIAL_CONFIG");

    // check before
    try {
      ProxyUsers.authorize(ugi1, "127.0.0.1");
      fail("first auth for " + ugi1.getShortUserName() + " should've failed ");
    } catch (AuthorizationException e) {
      // expected
      System.err.println("auth for " + ugi1.getUserName() + " failed");
    }
    try {
      ProxyUsers.authorize(ugi2, "127.0.0.1");
      System.err.println("auth for " + ugi2.getUserName() + " succeeded");
      // expected
    } catch (AuthorizationException e) {
      fail("first auth for " + ugi2.getShortUserName() + " should've succeeded: " + e.getLocalizedMessage());
    }
    checkpoint("AFTER_FIRST_AUTH_CHECK");

    // refresh will look at configuration on the server side
    // add additional resource with the new value
    // so the server side will pick it up
    String rsrc = "testGroupMappingRefresh_rsrc.xml";
    tempResource = addNewConfigResource(rsrc, userKeyGroups, "gr2",
        userKeyHosts, "127.0.0.1");
    checkpoint("AFTER_CONFIG_RESOURCE_ADD");

    DFSAdmin admin = new DFSAdmin(testConf);
    String [] args = new String[]{"-refreshSuperUserGroupsConfiguration"};
    admin.run(args);
    checkpoint("AFTER_REFRESH_COMMAND");

    try {
      ProxyUsers.authorize(ugi2, "127.0.0.1");
      fail("second auth for " + ugi2.getShortUserName() + " should've failed ");
    } catch (AuthorizationException e) {
      // expected
      System.err.println("auth for " + ugi2.getUserName() + " failed");
    }
    try {
      ProxyUsers.authorize(ugi1, "127.0.0.1");
      System.err.println("auth for " + ugi1.getUserName() + " succeeded");
      // expected
    } catch (AuthorizationException e) {
      fail("second auth for " + ugi1.getShortUserName() + " should've succeeded: " + e.getLocalizedMessage());
    }
    checkpoint("BEFORE_VERIFICATION");
  }

  public static String addNewConfigResource(String rsrcName, String keyGroup,
      String groups, String keyHosts, String hosts)
          throws FileNotFoundException, UnsupportedEncodingException {
    // location for temp resource should be in CLASSPATH
    Configuration conf = new Configuration();
    URL url = conf.getResource("hdfs-site.xml");

    String urlPath = URLDecoder.decode(url.getPath().toString(), "UTF-8");
    Path p = new Path(urlPath);
    Path dir = p.getParent();
    String tmp = dir.toString() + "/" + rsrcName;

    String newResource =
    "<configuration>"+
    "<property><name>" + keyGroup + "</name><value>"+groups+"</value></property>" +
    "<property><name>" + keyHosts + "</name><value>"+hosts+"</value></property>" +
    "</configuration>";
    PrintWriter writer = new PrintWriter(new FileOutputStream(tmp));
    writer.println(newResource);
    writer.close();

    Configuration.addDefaultResource(rsrcName);
    return tmp;
  }
}
