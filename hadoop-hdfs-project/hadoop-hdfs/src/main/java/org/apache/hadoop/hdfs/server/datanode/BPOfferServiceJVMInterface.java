package org.apache.hadoop.hdfs.server.datanode;

import java.io.IOException;

public interface BPOfferServiceJVMInterface {
    void triggerHeartbeatForTests() throws IOException;
    void triggerBlockReportForTests() throws IOException;
}
