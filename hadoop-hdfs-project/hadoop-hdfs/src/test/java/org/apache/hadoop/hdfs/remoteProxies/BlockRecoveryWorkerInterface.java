package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.BlockRecoveryCommand;

import java.util.*;
import java.io.*;

public interface BlockRecoveryWorkerInterface {

    DaemonInterface recoverBlocks(String who, Collection<BlockRecoveryCommand.RecoveringBlock> blocks);
}
