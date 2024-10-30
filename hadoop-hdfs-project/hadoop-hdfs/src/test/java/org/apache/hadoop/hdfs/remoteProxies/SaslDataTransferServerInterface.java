package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.datatransfer.IOStreamPair;
import org.apache.hadoop.hdfs.net.Peer;
import org.apache.hadoop.hdfs.protocol.DatanodeID;

public interface SaslDataTransferServerInterface {

    IOStreamPair receive(Peer peer, OutputStream underlyingOut, InputStream underlyingIn, int xferPort, DatanodeID datanodeId);

    String getNegotiatedQOP();
}
