package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.BlockECReconstructionCommand;

import java.util.*;
import java.io.*;

public interface ErasureCodingWorkerInterface {

    void processErasureCodingTasks(Collection<BlockECReconstructionCommand.BlockECReconstructionInfo> ecTasks);

    void shutDown();

    float getXmitWeight();
}
