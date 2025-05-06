package org.apache.hadoop.hdfs.server.namenode.ha;

import java.io.IOException;

public interface EditLogTailerJVMInterface {
    void doTailEdits() throws IOException, InterruptedException;
}
