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
package org.apache.hadoop.hdfs;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSShellGenericOptions}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDFSShellGenericOptions Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDFSShellGenericOptions_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FS_OPTION_TEST",
      "AFTER_CONF_OPTION_TEST",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test
  public void testDFSCommand() throws Exception {
    String namenode = null;
    try {
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
      cluster.waitClusterUp();

      checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

      namenode = FileSystem.getDefaultUri(conf).toString();
      String [] args = new String[4];
      args[2] = "-mkdir";
      args[3] = "/data";

      testFsOption(args, namenode);
      checkpoint("AFTER_FS_OPTION_TEST");

      testConfOption(args, namenode);
      checkpoint("AFTER_CONF_OPTION_TEST");

      testPropertyOption(args, namenode);
      checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    } finally {
      // Cleanup handled by base class @After
    }
  }

  private void testFsOption(String [] args, String namenode) {
    // prepare arguments to create a directory /data
    args[0] = "-fs";
    args[1] = namenode;
    execute(args, namenode);
  }

  private void testConfOption(String[] args, String namenode) {
    // prepare configuration hdfs-site.xml
    File configDir = new File(new File("build", "test"), "minidfs");
    assertTrue(configDir.mkdirs());
    File siteFile = new File(configDir, "hdfs-site.xml");
    PrintWriter pw;
    try {
      pw = new PrintWriter(siteFile);
      pw.print("<?xml version=\"1.0\"?>\n"+
               "<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>\n"+
               "<configuration>\n"+
               " <property>\n"+
               "   <name>fs.defaultFS</name>\n"+
               "   <value>"+namenode+"</value>\n"+
               " </property>\n"+
               "</configuration>\n");
      pw.close();

      // prepare arguments to create a directory /data
      args[0] = "-conf";
      args[1] = siteFile.getPath();
      execute(args, namenode);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      siteFile.delete();
      configDir.delete();
    }
  }

  private void testPropertyOption(String[] args, String namenode) {
    // prepare arguments to create a directory /data
    args[0] = "-D";
    args[1] = "fs.defaultFS="+namenode;
    execute(args, namenode);
  }

  private void execute(String [] args, String namenode) {
    FsShell shell=new FsShell();
    FileSystem localFs=null;
    try {
      ToolRunner.run(shell, args);
      localFs = FileSystem.get(DFSUtilClient.getNNUri(
          DFSUtilClient.getNNAddress(namenode)), shell.getConf());
      assertTrue("Directory does not get created",
                 localFs.isDirectory(new Path("/data")));
      localFs.delete(new Path("/data"), true);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } finally {
      if (localFs!=null) {
        try {
          localFs.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

}
