package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPBJVMInterface;

public interface BPServiceActorJVMInterface {
    boolean isAlive();
    void stopCommandProcessingThread();
    java.net.InetSocketAddress getNNSocketAddress();
    DatanodeProtocolClientSideTranslatorPBJVMInterface getNameNodeProxy();
}
