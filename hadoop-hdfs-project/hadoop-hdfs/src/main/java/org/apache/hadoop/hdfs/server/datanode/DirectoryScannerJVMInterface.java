package org.apache.hadoop.hdfs.server.datanode;

import java.io.IOException;

public interface DirectoryScannerJVMInterface {
    void reconcile() throws IOException;
}
