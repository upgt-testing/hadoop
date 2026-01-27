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
package org.apache.hadoop.hdfs.server.namenode;

import static org.apache.hadoop.test.LambdaTestUtils.intercept;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.namenode.JournalSet.JournalAndStream;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.PathUtils;
import org.apache.hadoop.util.ExitUtil.ExitException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

@RunWith(Parameterized.class)
public class TestEditLogJournalFailures_RestartInjected_Randomized_456 {

    private int editsPerformed = 0;

    private MiniDFSCluster cluster;

    private FileSystem fs;

    private boolean useAsyncEdits;

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> params = new ArrayList<Object[]>();
        params.add(new Object[] { Boolean.FALSE });
        params.add(new Object[] { Boolean.TRUE });
        return params;
    }

    public TestEditLogJournalFailures_RestartInjected_Randomized_456(boolean useAsyncEdits) {
        this.useAsyncEdits = useAsyncEdits;
    }

    private Configuration getConf() {
        Configuration conf = new HdfsConfiguration();
        conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_EDITS_ASYNC_LOGGING, useAsyncEdits);
        return conf;
    }

    /**
     * Create the mini cluster for testing and sub in a custom runtime so that
     * edit log journal failures don't actually cause the JVM to exit.
     */
    @Before
    public void setUpMiniCluster() throws IOException {
        setUpMiniCluster(getConf(), true);
    }

    public void setUpMiniCluster(Configuration conf, boolean manageNameDfsDirs) throws IOException {
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(0).manageNameDfsDirs(manageNameDfsDirs).checkExitOnShutdown(false).build();
        cluster.waitActive();
        fs = cluster.getFileSystem();
    }

    @After
    public void shutDownMiniCluster() throws IOException {
        if (fs != null) {
            fs.close();
            fs = null;
        }
        if (cluster != null) {
            try {
                cluster.shutdown();
                cluster = null;
            } catch (ExitException ee) {
                // Ignore ExitExceptions as the tests may result in the
                // NameNode doing an immediate shutdown.
            }
        }
    }

    @Test
    public void testSingleFailedEditsDirOnFlush() throws IOException {
        RestartFramework.at("after_first_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue(doAnEdit());
        invalidateEditsDirAtIndex(0, true, false);
        assertTrue(doAnEdit());
        RestartFramework.at("after_second_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_invalidation").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertFalse(cluster.getNameNode().isInSafeMode());
    }

    @Test
    public void testAllEditsDirsFailOnFlush() throws IOException {
        assertTrue(doAnEdit());
        invalidateEditsDirAtIndex(0, true, false);
        invalidateEditsDirAtIndex(1, true, false);
        RestartFramework.at("after_initial_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_invalidate_all_journals").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            doAnEdit();
            fail("The previous edit could not be synced to any persistent storage, " + "should have halted the NN");
        } catch (RemoteException re) {
            assertTrue(re.getClassName().contains("ExitException"));
            GenericTestUtils.assertExceptionContains("Could not sync enough journals to persistent storage. " + "Unsynced transactions: 1", re);
        }
    }

    @Test
    public void testAllEditsDirFailOnWrite() throws IOException {
        assertTrue(doAnEdit());
        RestartFramework.at("after_initial_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        invalidateEditsDirAtIndex(0, true, true);
        RestartFramework.at("after_invalidate_journals_on_write").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        invalidateEditsDirAtIndex(1, true, true);
        try {
            doAnEdit();
            fail("The previous edit could not be synced to any persistent storage, " + " should have halted the NN");
        } catch (RemoteException re) {
            assertTrue(re.getClassName().contains("ExitException"));
            GenericTestUtils.assertExceptionContains("Could not sync enough journals to persistent storage due to " + "No journals available to flush. " + "Unsynced transactions: 1", re);
        }
    }

    @Test
    public void testSingleFailedEditsDirOnSetReadyToFlush() throws IOException {
        RestartFramework.at("after_second_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue(doAnEdit());
        RestartFramework.at("after_initial_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        invalidateEditsDirAtIndex(0, false, false);
        RestartFramework.at("after_invalidate_single_journal").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue(doAnEdit());
        assertFalse(cluster.getNameNode().isInSafeMode());
    }

    @Test
    public void testSingleRequiredFailedEditsDirOnSetReadyToFlush() throws IOException {
        RestartFramework.at("after_invalidate_required_journal").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        String[] editsDirs = cluster.getConfiguration(0).getTrimmedStrings(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY);
        shutDownMiniCluster();
        Configuration conf = getConf();
        conf.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_REQUIRED_KEY, editsDirs[0]);
        conf.setInt(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_MINIMUM_KEY, 0);
        conf.setInt(DFSConfigKeys.DFS_NAMENODE_CHECKED_VOLUMES_MINIMUM_KEY, 0);
        setUpMiniCluster(conf, true);
        assertTrue(doAnEdit());
        invalidateEditsDirAtIndex(0, false, false);
        JournalAndStream nonRequiredJas = getJournalAndStream(1);
        EditLogFileOutputStream nonRequiredSpy = spyOnStream(nonRequiredJas);
        assertTrue(nonRequiredJas.isActive());
        try {
            doAnEdit();
            fail("A single failure of a required journal should have halted the NN");
        } catch (RemoteException re) {
            assertTrue(re.getClassName().contains("ExitException"));
            GenericTestUtils.assertExceptionContains("setReadyToFlush failed for required journal", re);
        }
        RestartFramework.at("after_cluster_restart").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_first_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Mockito.verify(nonRequiredSpy, Mockito.never()).setReadyToFlush();
        assertFalse(nonRequiredJas.isActive());
    }

    @Test
    public void testMultipleRedundantFailedEditsDirOnSetReadyToFlush() throws IOException {
        shutDownMiniCluster();
        Configuration conf = getConf();
        String[] nameDirs = new String[4];
        for (int i = 0; i < nameDirs.length; i++) {
            File nameDir = new File(PathUtils.getTestDir(getClass()), "name-dir" + i);
            nameDir.mkdirs();
            nameDirs[i] = nameDir.getAbsolutePath();
        }
        conf.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY, StringUtils.join(nameDirs, ","));
        RestartFramework.at("after_invalidate_second_journal").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        conf.setInt(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_MINIMUM_KEY, 2);
        setUpMiniCluster(conf, false);
        RestartFramework.at("after_invalidate_first_journal").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_initial_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue(doAnEdit());
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_invalidate_third_journal").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        invalidateEditsDirAtIndex(0, false, false);
        assertTrue(doAnEdit());
        invalidateEditsDirAtIndex(1, false, false);
        assertTrue(doAnEdit());
        invalidateEditsDirAtIndex(2, false, false);
        try {
            doAnEdit();
            fail("A failure of more than the minimum number of redundant journals " + "should have halted ");
        } catch (RemoteException re) {
            assertTrue(re.getClassName().contains("ExitException"));
            GenericTestUtils.assertExceptionContains("Could not sync enough journals to persistent storage due to " + "setReadyToFlush failed for too many journals. " + "Unsynced transactions: 1", re);
        }
    }

    @Test
    public void testMultipleRedundantFailedEditsDirOnStartLogSegment() throws Exception {
        shutDownMiniCluster();
        Configuration conf = getConf();
        String[] nameDirs = new String[4];
        RestartFramework.at("after_spy_journal").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        for (int i = 0; i < nameDirs.length; i++) {
            File nameDir = new File(PathUtils.getTestDir(getClass()), "name-dir" + i);
            nameDir.mkdirs();
            nameDirs[i] = nameDir.getAbsolutePath();
        }
        conf.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY, StringUtils.join(nameDirs, ","));
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        conf.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_REQUIRED_KEY, StringUtils.join(nameDirs, ",", 0, 3));
        RestartFramework.at("after_initial_edit").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        setUpMiniCluster(conf, false);
        assertTrue(doAnEdit());
        spyOnJASjournal(3);
        RemoteException re = intercept(RemoteException.class, "too few journals successfully started.", () -> ((DistributedFileSystem) fs).rollEdits());
        GenericTestUtils.assertExceptionContains("ExitException", re);
    }

    private JournalManager spyOnJASjournal(int index) throws Exception {
        JournalAndStream jas = getJournalAndStream(index);
        JournalManager manager = jas.getManager();
        JournalManager spyManager = spy(manager);
        jas.setJournalForTests(spyManager);
        doThrow(new IOException("Unable to start log segment ")).when(spyManager).startLogSegment(anyLong(), anyInt());
        return spyManager;
    }

    /**
     * Replace the journal at index <code>index</code> with one that throws an
     * exception on flush.
     *
     * @param index the index of the journal to take offline.
     * @return the original <code>EditLogOutputStream</code> of the journal.
     */
    private void invalidateEditsDirAtIndex(int index, boolean failOnFlush, boolean failOnWrite) throws IOException {
        JournalAndStream jas = getJournalAndStream(index);
        EditLogFileOutputStream spyElos = spyOnStream(jas);
        if (failOnWrite) {
            doThrow(new IOException("fail on write()")).when(spyElos).write((FSEditLogOp) any());
        }
        if (failOnFlush) {
            doThrow(new IOException("fail on flush()")).when(spyElos).flush();
        } else {
            doThrow(new IOException("fail on setReadyToFlush()")).when(spyElos).setReadyToFlush();
        }
    }

    private EditLogFileOutputStream spyOnStream(JournalAndStream jas) {
        EditLogFileOutputStream elos = (EditLogFileOutputStream) jas.getCurrentStream();
        EditLogFileOutputStream spyElos = spy(elos);
        jas.setCurrentStreamForTests(spyElos);
        return spyElos;
    }

    /**
     * Pull out one of the JournalAndStream objects from the edit log.
     */
    private JournalAndStream getJournalAndStream(int index) {
        FSImage fsimage = cluster.getNamesystem().getFSImage();
        FSEditLog editLog = fsimage.getEditLog();
        return editLog.getJournals().get(index);
    }

    /**
     * Do a mutative metadata operation on the file system.
     *
     * @return true if the operation was successful, false otherwise.
     */
    private boolean doAnEdit() throws IOException {
        return fs.mkdirs(new Path("/tmp", Integer.toString(editsPerformed++)));
    }
}
