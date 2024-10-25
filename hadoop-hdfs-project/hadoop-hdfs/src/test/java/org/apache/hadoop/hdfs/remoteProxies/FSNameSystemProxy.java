package org.apache.hadoop.hdfs.remoteProxies;

import java.io.IOException;

public interface FSNameSystemProxy {
    boolean testRMIPrint(String message);
    boolean saveNamespace(final long timeWindow, final long txGap) throws IOException;
    void enterSafeMode(boolean resourcesLow) throws IOException;
    KeyProviderProxy getProvider();
    String getClusterId();
    long getBlocksTotal();
    FSDirectoryProxy getFSDirectory();
    SnapshotManagerProxy getSnapshotManager();
    BlockManagerProxy getBlockManager();
}
