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

package org.apache.hadoop.fs.aliyun.oss.yarn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.WordCount;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.aliyun.oss.AliyunOSSTestUtils;
import org.apache.hadoop.fs.aliyun.oss.contract.AliyunOSSContract;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.AbstractFSContractTestBase;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * ProcessBased version of {@link TestOSSMiniYarnCluster}.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * Tests that Aliyun OSS is usable through a YARN application with process-based cluster.
 *
 * @see TestOSSMiniYarnCluster Original test using MiniYARNCluster
 */
@RunWith(Parameterized.class)
public class TestOSSMiniYarnCluster_ProcessBased extends AbstractFSContractTestBase {

  private Configuration conf;
  private FileSystem fs;
  private ProcessBasedMiniYARNCluster yarnCluster;
  private Path rootPath;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_OSS_SETUP",
      "AFTER_JOB_SUBMIT",
      "AFTER_JOB_COMPLETE"
    );
  }

  protected void checkpoint(String name) throws Exception {
    if (name.equals(upgradeCheckpoint)) {
      LOG.info("Executing upgrade checkpoint: {}", name);
      if (yarnCluster != null) {
        yarnCluster.rollingUpgradeNodeManagers();
        LOG.info("Rolling upgrade of NodeManagers completed at checkpoint: {}", name);
      }
    }
  }

  @Override
  protected AbstractFSContract createContract(Configuration configuration) {
    this.conf = configuration;
    return new AliyunOSSContract(conf);
  }

  @Override
  public void setup() throws Exception {
    super.setup();
    fs = getFileSystem();
    rootPath = path("MiniClusterWordCount");
    Path workingDir = path("working");
    fs.setWorkingDirectory(workingDir);
    fs.mkdirs(new Path(rootPath, "input/"));

    yarnCluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(1)
        .numResourceManagers(1)
        .build();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    checkpoint("AFTER_OSS_SETUP");
  }

  @Test(timeout = 120000)
  public void testWithMiniCluster() throws Exception {
    Path input = new Path(rootPath, "input/in");
    input = input.makeQualified(fs.getUri(), fs.getWorkingDirectory());
    Path output = new Path(rootPath, "output/");
    output = output.makeQualified(fs.getUri(), fs.getWorkingDirectory());

    writeStringToFile(input, "first line\nsecond line\nthird line");

    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(WordCount.TokenizerMapper.class);
    job.setCombinerClass(WordCount.IntSumReducer.class);
    job.setReducerClass(WordCount.IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, input);
    FileOutputFormat.setOutputPath(job, output);

    checkpoint("AFTER_JOB_SUBMIT");

    int exitCode = (job.waitForCompletion(true) ? 0 : 1);
    assertEquals("Returned error code.", 0, exitCode);

    checkpoint("AFTER_JOB_COMPLETE");

    assertTrue(fs.exists(new Path(output, "_SUCCESS")));
    String outputAsStr = readStringFromFile(new Path(output, "part-r-00000"));
    Map<String, Integer> resAsMap = getResultAsMap(outputAsStr);

    assertEquals(4, resAsMap.size());
    assertEquals(1, (int) resAsMap.get("first"));
    assertEquals(1, (int) resAsMap.get("second"));
    assertEquals(1, (int) resAsMap.get("third"));
    assertEquals(3, (int) resAsMap.get("line"));
  }


  /**
   * helper method.
   */
  private Map<String, Integer> getResultAsMap(String outputAsStr) {
    Map<String, Integer> result = new HashMap<>();
    for (String line : outputAsStr.split("\n")) {
      String[] tokens = line.split("\t");
      assertTrue("Not enough tokens in in string \" "
              + line + "\" from output \"" + outputAsStr + "\"",
          tokens.length > 1);
      result.put(tokens[0], Integer.parseInt(tokens[1]));
    }
    return result;
  }

  /**
   * helper method.
   */
  private void writeStringToFile(Path path, String string) throws IOException {
    FileContext fc = AliyunOSSTestUtils.createTestFileContext(conf);
    try (FSDataOutputStream file = fc.create(path,
        EnumSet.of(CreateFlag.CREATE))) {
      file.write(string.getBytes());
    }
  }

  /**
   * helper method.
   */
  private String readStringFromFile(Path path) throws IOException {
    try (FSDataInputStream in = fs.open(path)) {
      long bytesLen = fs.getFileStatus(path).getLen();
      byte[] buffer = new byte[(int) bytesLen];
      IOUtils.readFully(in, buffer, 0, buffer.length);
      return new String(buffer);
    }
  }

  @Override
  public void teardown() throws Exception {
    if (yarnCluster != null) {
      yarnCluster.shutdown();
      yarnCluster = null;
    }
    super.teardown();
    IOUtils.closeStream(getFileSystem());
  }
}
