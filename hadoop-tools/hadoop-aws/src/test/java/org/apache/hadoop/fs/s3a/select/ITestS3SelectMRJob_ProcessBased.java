/*
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

package org.apache.hadoop.fs.s3a.select;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.hadoop.fs.s3a.impl.ChangeDetectionPolicy;
import org.apache.hadoop.fs.s3a.impl.ChangeDetectionPolicy.Source;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.WordCount;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.hadoop.fs.s3a.S3ATestUtils;
import org.apache.hadoop.fs.s3a.S3AUtils;
import org.apache.hadoop.fs.s3a.commit.files.SuccessData;
import org.apache.hadoop.fs.s3a.commit.staging.StagingCommitter;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.DurationInfo;
import org.apache.hadoop.util.functional.FutureIO;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;

import static org.apache.hadoop.fs.s3a.commit.CommitConstants.FS_S3A_COMMITTER_NAME;
import static org.apache.hadoop.fs.s3a.commit.CommitConstants.FS_S3A_COMMITTER_STAGING_UNIQUE_FILENAMES;
import static org.apache.hadoop.fs.s3a.select.SelectConstants.*;

/**
 * ProcessBased version of {@link ITestS3SelectMRJob}.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * Run an MR job with an S3 Select query to verify S3 Select integration with YARN.
 *
 * @see ITestS3SelectMRJob Original test using MiniYARNCluster
 */
@RunWith(Parameterized.class)
public class ITestS3SelectMRJob_ProcessBased extends AbstractS3SelectTest {

  private final Configuration conf = new YarnConfiguration();

  private S3AFileSystem fs;

  private ProcessBasedMiniYARNCluster yarnCluster;

  private Path rootPath;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_S3_SETUP",
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
  public void setup() throws Exception {
    super.setup();
    fs = S3ATestUtils.createTestFileSystem(conf);

    ChangeDetectionPolicy changeDetectionPolicy =
        getLandsatFS().getChangeDetectionPolicy();
    Assume.assumeFalse("the standard landsat bucket doesn't have versioning",
        changeDetectionPolicy.getSource() == Source.VersionId
            && changeDetectionPolicy.isRequireVersion());

    rootPath = path("ITestS3SelectMRJob");
    Path workingDir = path("working");
    fs.setWorkingDirectory(workingDir);
    fs.mkdirs(new Path(rootPath, "input/"));

    yarnCluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(1)
        .numResourceManagers(1)
        .build();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    checkpoint("AFTER_S3_SETUP");
  }

  @Override
  public void teardown() throws Exception {
    if (yarnCluster != null) {
      yarnCluster.shutdown();
      yarnCluster = null;
    }
    super.teardown();
  }

  @Test(timeout = 120000)
  public void testLandsatSelect() throws Exception {
    final Path input = getLandsatGZ();
    final Path output = path("testLandsatSelect")
        .makeQualified(fs.getUri(), fs.getWorkingDirectory());

    final Job job = Job.getInstance(conf, "process level count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(WordCount.TokenizerMapper.class);
    job.setCombinerClass(WordCount.IntSumReducer.class);
    job.setReducerClass(WordCount.IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, input);
    FileOutputFormat.setOutputPath(job, output);

    // job with use the staging committer
    final JobConf jobConf = (JobConf) job.getConfiguration();
    jobConf.set(FS_S3A_COMMITTER_NAME, StagingCommitter.NAME);
    jobConf.setBoolean(FS_S3A_COMMITTER_STAGING_UNIQUE_FILENAMES,
        false);

    final String query
        = ITestS3SelectLandsat.SELECT_PROCESSING_LEVEL_NO_LIMIT;
    inputMust(jobConf, SELECT_SQL,
        query);
    inputMust(jobConf, SELECT_INPUT_COMPRESSION, COMPRESSION_OPT_GZIP);

    // input settings
    inputMust(jobConf, SELECT_INPUT_FORMAT, SELECT_FORMAT_CSV);
    inputMust(jobConf, CSV_INPUT_HEADER, CSV_HEADER_OPT_USE);

    // output
    inputMust(jobConf, SELECT_OUTPUT_FORMAT, SELECT_FORMAT_CSV);
    inputMust(jobConf, CSV_OUTPUT_QUOTE_FIELDS,
        CSV_OUTPUT_QUOTE_FIELDS_AS_NEEEDED);

    // disable the gzip codec, so that the record readers do not
    // get confused
    enablePassthroughCodec(jobConf, ".gz");

    checkpoint("AFTER_JOB_SUBMIT");

    try (DurationInfo ignored = new DurationInfo(LOG, "SQL " + query)) {
      int exitCode = job.waitForCompletion(true) ? 0 : 1;
      assertEquals("Returned error code.", 0, exitCode);
    }

    checkpoint("AFTER_JOB_COMPLETE");

    // log the success info
    Path successPath = new Path(output, "_SUCCESS");
    SuccessData success = SuccessData.load(fs, successPath);
    LOG.info("Job _SUCCESS\n{}", success);

    // process the results by ver
    //
    LOG.info("Results for query \n{}", query);
    final AtomicLong parts = new AtomicLong(0);
    S3AUtils.applyLocatedFiles(fs.listFiles(output, false),
        (status) -> {
          Path path = status.getPath();
          // ignore _SUCCESS, any temp files in subdirectories...
          if (path.getName().startsWith("part-")) {
            parts.incrementAndGet();
            String result = readStringFromFile(path);
            LOG.info("{}\n{}", path, result);
            String[] lines = result.split("\n", -1);
            int l = lines.length;
            // add a bit of slack here in case some new processing
            // option was added.
            assertTrue("Wrong number of lines (" + l + ") in " + result,
                l > 0 && l < 15);
          }
        });
    assertEquals("More part files created than expected", 1, parts.get());
  }

  /**
   * Read a file; using Async IO for completeness and to see how
   * well the async IO works in practice.
   * Summary: checked exceptions cripple Async operations.
   */
  private String readStringFromFile(Path path) throws IOException {
    int bytesLen = (int)fs.getFileStatus(path).getLen();
    byte[] buffer = new byte[bytesLen];
    return FutureIO.awaitFuture(
        fs.openFile(path).build().thenApply(in -> {
          try {
            IOUtils.readFully(in, buffer, 0, bytesLen);
            return new String(buffer);
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
        }));
  }

}
