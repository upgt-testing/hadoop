package org.apache.hadoop.hdfs.server.datanode;

import java.io.IOException;
import java.util.List;

public interface BPOfferServiceJVMInterface {
    String getBlockPoolId();
    void triggerHeartbeatForTests() throws IOException;
    void triggerBlockReportForTests() throws IOException;
    boolean isAlive();
    boolean isInitialized();
    List<? extends BPServiceActorJVMInterface> getBPServiceActors();
    void triggerDeletionReportForTests() throws IOException;
}
