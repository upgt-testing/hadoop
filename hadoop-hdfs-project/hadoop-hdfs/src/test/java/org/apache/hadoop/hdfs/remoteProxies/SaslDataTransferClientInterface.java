package org.apache.hadoop.hdfs.remoteProxies;

import java.net.Socket;
import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.protocol.datatransfer.sasl.DataEncryptionKeyFactory;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
//import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.hdfs.net.Peer;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.datatransfer.IOStreamPair;
import org.apache.hadoop.security.token.Token;

import javax.crypto.SecretKey;

public interface SaslDataTransferClientInterface {

    IOStreamPairInterface newSocketSend(Socket socket, OutputStream underlyingOut, InputStream underlyingIn, DataEncryptionKeyFactoryInterface encryptionKeyFactory, Token<BlockTokenIdentifier> accessToken, DatanodeIDInterface datanodeId);

    PeerInterface peerSend(Peer peer, DataEncryptionKeyFactory encryptionKeyFactory, Token<BlockTokenIdentifier> accessToken, DatanodeID datanodeId);

    IOStreamPair socketSend(Socket socket, OutputStream underlyingOut, InputStream underlyingIn, DataEncryptionKeyFactory encryptionKeyFactory, Token<BlockTokenIdentifier> accessToken, DatanodeID datanodeId);

    IOStreamPair socketSend(Socket socket, OutputStream underlyingOut, InputStream underlyingIn, DataEncryptionKeyFactory encryptionKeyFactory, Token<BlockTokenIdentifier> accessToken, DatanodeID datanodeId, SecretKey secretKey);

    String getTargetQOP();
}
