package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocolsJVMInterface;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RPCServerJVMInterface;

import java.io.IOException;

public interface NameNodeRpcServerJVMInterface extends NamenodeProtocolsJVMInterface {
    RPCServerJVMInterface getClientRpcServer();
}
